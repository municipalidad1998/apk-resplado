package com.streamvault.search

import java.text.Normalizer

/**
 * Relevance ranking for the music search: an exact artist + title always wins over a fuzzy
 * coincidence, and unrelated results never appear above a real match.
 *
 * Local library and online sources are scored with exactly the same rules, so "first the
 * phone, then the internet" keeps the order the user expects.
 */
object SmartSearch {

    const val MIN_SCORE = 25
    const val STRONG_SCORE = 60
    /** When a strong match exists, weaker coincidences are hidden instead of mixed in. */
    const val KEEP_WITH_STRONG = 50

    /** One item that can be ranked: a local track or an online result. */
    data class Candidate(val id: String, val title: String, val artist: String, val album: String = "",
                         val source: String = "local", val plays: Int = 0, val durationMs: Long = 0)
    data class Ranked(val candidate: Candidate, val score: Int)

    /** The query, split into a possible song title and a possible artist. */
    data class Intent(val raw: String, val text: String, val parts: List<String>) {
        val titleGuess: String? get() = parts.firstOrNull()
        val artistGuess: String? get() = parts.getOrNull(1)
    }

    /** Lower case, no accents, no punctuation: "Tú Poeta" and "tu poeta" become the same text. */
    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** "Alex Campos - Tu Poeta" and "Tu poeta (de Alex Campos)" give the same two parts. */
    fun parse(query: String): Intent {
        val text = normalize(query)
        val separators = Regex("\\s+(?:-|–|—|:|\\||de|del|by|feat|ft)\\s+|\\s+-\\s+")
        var parts = text.split(separators).map { it.trim() }.filter { it.length > 1 }
        if (parts.size == 1) {
            // "tu poeta alex campos": no separator, so the strongest hint is the whole string.
            parts = listOf(text)
        }
        return Intent(query, text, parts.take(2))
    }

    /** How well one candidate answers the query. 0 means "not related at all". */
    fun score(candidate: Candidate, intent: Intent): Int {
        val title = normalize(candidate.title)
        val artist = normalize(candidate.artist)
        val album = normalize(candidate.album)
        val query = intent.text
        if (title.isBlank() && artist.isBlank()) return 0
        var best = 0
        fun keep(value: Int) { if (value > best) best = value }
        val words = query.split(' ').filter { it.length > 1 }

        // 1. Exact artist + title, including queries written without a separator
        //    ("tu poeta alex campos"), where every word boundary is tried as the split.
        val splits = mutableListOf<Pair<String, String>>()
        if (intent.parts.size > 1) { splits += intent.parts[0] to intent.parts[1]; splits += intent.parts[1] to intent.parts[0] }
        if (words.size >= 2) {
            for (index in 1 until words.size) {
                splits += words.subList(0, index).joinToString(" ") to words.subList(index, words.size).joinToString(" ")
            }
        }
        for ((first, second) in splits) {
            if (artist == first && title == second) keep(100)
            else if (title == first && artist == second) keep(100)
            else if (artist == first && title.startsWith(second)) keep(94)
            else if (title == first && artist.startsWith(second)) keep(94)
            else if (artist.contains(first) && title.contains(second)) keep(80)
            else if (title.contains(first) && artist.contains(second)) keep(80)
        }

        // 2. Exact title, exact artist, exact album.
        if (title == query) keep(95)
        if (artist == query) keep(90)
        if (album.isNotBlank() && album == query) keep(70)

        // 3. Prefixes and word-bounded containment.
        if (title.startsWith(query) || artist.startsWith(query)) keep(60)
        if (album.startsWith(query)) keep(50)
        if (containsWord(title, query) || containsWord(artist, query)) keep(52)
        if (album.isNotBlank() && containsWord(album, query)) keep(44)

        // 4. Collaborations: the artist appears next to "feat" in the title.
        if (containsWord(title, "feat") || containsWord(title, "ft")) {
            if (words.any { word -> word.length > 2 && containsWord(title, word) }) keep(58)
        }

        // 5. All the query words, in order, inside the title or the artist.
        if (words.isNotEmpty()) {
            if (wordsInOrder(title, words)) keep(46)
            if (wordsInOrder(artist, words)) keep(40)
            if (words.all { word -> title.contains(word) || artist.contains(word) || album.contains(word) }) keep(34)
            val covered = words.count { word -> title.contains(word) || artist.contains(word) || album.contains(word) }
            if (covered > 0) keep(20 + (covered * 14) / words.size)
        }

        // 6. Typos: "alex campo" still finds "Alex Campos".
        val titleDistance = distance(title, query)
        val artistDistance = distance(artist, query)
        val allowance = (query.length / 6).coerceAtLeast(1)
        if (titleDistance in 1..allowance) keep(38 - titleDistance * 4)
        if (artistDistance in 1..allowance) keep(34 - artistDistance * 4)
        return best
    }

    /**
     * Ranks and drops the noise: if any result is a strong match, weak coincidences no longer
     * appear at all, so the answer is never buried under unrelated songs.
     */
    fun rank(candidates: Iterable<Candidate>, query: String, floor: Int = MIN_SCORE): List<Ranked> {
        val intent = parse(query)
        val ranked = candidates.mapNotNull { candidate ->
            val value = score(candidate, intent)
            if (value >= floor) Ranked(candidate, value) else null
        }.sortedWith(compareByDescending<Ranked> { it.score }.thenByDescending { it.candidate.plays }.thenBy { it.candidate.title.length })
        val hasStrong = ranked.firstOrNull()?.score ?: 0 >= STRONG_SCORE
        return if (hasStrong) ranked.filter { it.score >= KEEP_WITH_STRONG } else ranked
    }

    fun <T> rankItems(items: Iterable<T>, query: String, toCandidate: (T) -> Candidate): List<T> {
        val byId = items.associateBy { toCandidate(it).id }
        return rank(items.map(toCandidate), query).mapNotNull { byId[it.candidate.id] }
    }

    private fun containsWord(haystack: String, needle: String): Boolean =
        haystack.isNotBlank() && needle.isNotBlank() && " $haystack ".contains(" $needle ")

    private fun wordsInOrder(text: String, words: List<String>): Boolean {
        var from = 0
        for (word in words) {
            val index = text.indexOf(word, from)
            if (index < 0) return false
            from = index + word.length
        }
        return true
    }

    /** Levenshtein distance, capped so long texts stay cheap. */
    internal fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        if (kotlin.math.abs(a.length - b.length) > 6) return 99
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
