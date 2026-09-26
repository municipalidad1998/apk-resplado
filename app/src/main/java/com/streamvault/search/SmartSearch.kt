package com.streamvault.search

import java.text.Normalizer

/**
 * Relevance ranking for the music search: an exact artist + title always wins over a fuzzy
 * coincidence, and a song by another artist is never promoted above a real match.
 *
 * The same rules score the phone library and the online providers, so "first the phone, then
 * the internet" keeps the order the user expects. Sources are never merged into one list:
 * each provider ranks its own results and the UI keeps them apart.
 */
object SmartSearch {

    /** Below this a candidate is considered unrelated and disappears. */
    const val MIN_SCORE = 25
    /** From here a match is "strong": weak coincidences are hidden next to it. */
    const val STRONG_SCORE = 60
    /** When a strong match exists, weaker coincidences are hidden instead of mixed in. */
    const val KEEP_WITH_STRONG = 50

    const val EXACT_ARTIST_TITLE = 100
    const val EXACT_TITLE = 95
    /** Exact artist: tolerates the article ("La IBI" == "IBI") but nothing else. */
    const val EXACT_ARTIST = 92
    const val ARTIST_TITLE_PREFIX = 94
    /** The artist is one of the credited artists: "Alex Campos, Marcos Witt". */
    const val ARTIST_CREDITED = 84
    /** A version of the same song: "Al taller del maestro (en vivo)". */
    const val TITLE_PREFIX = 88
    const val BOTH_PARTS = 80
    const val ALBUM_EXACT = 70
    const val ARTIST_PREFIX = 60
    const val FEAT = 58
    const val WORD_MATCH = 52
    const val ALBUM_PREFIX = 50

    /** One item that can be ranked: a local track or an online result. */
    data class Candidate(val id: String, val title: String, val artist: String, val album: String = "",
                         val source: String = "local", val plays: Int = 0, val durationMs: Long = 0)
    data class Ranked(val candidate: Candidate, val score: Int)

    /** The query, split into a possible song title and a possible artist. */
    data class Intent(val raw: String, val text: String, val parts: List<String>) {
        val titleGuess: String? get() = parts.firstOrNull()
        val artistGuess: String? get() = parts.getOrNull(1)
    }

    /** Leading articles, so "La IBI" and "IBI" are the same artist for the user. */
    private val ARTICLES = setOf("la", "el", "los", "las", "lo", "the")

    /** Lower case, no accents, no punctuation: "Tú Poeta" and "tu poeta" become the same text. */
    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** The artist name without a leading article: used for every artist comparison. */
    fun artistKey(value: String): String {
        val words = normalize(value).split(' ').filter { it.isNotBlank() }
        val withoutArticle = if (words.size > 1 && words.first() in ARTICLES) words.drop(1) else words
        return withoutArticle.joinToString(" ")
    }

    /** True when the artist is credited among others: "Alex Campos, Marcos Witt" for "Alex Campos". */
    internal fun credited(artistKey: String, wanted: String): Boolean {
        if (artistKey.isBlank() || wanted.isBlank()) return false
        if (artistKey == wanted) return true
        return artistKey.startsWith("$wanted ") || artistKey.endsWith(" $wanted") || artistKey.contains(" $wanted ")
    }

    /** "Alex Campos - Tu Poeta" and "Tu poeta (de Alex Campos)" give the same two parts. */
    fun parse(query: String): Intent {
        val text = normalize(query)
        // Split on the raw query: normalize() would delete the very dashes that separate the fields.
        val separators = Regex("\\s+(?:-|–|—|:|\\|)\\s+|\\s+(?:de|del|by|feat|ft)\\s+", RegexOption.IGNORE_CASE)
        val parts = query.split(separators).map(::normalize).filter { it.length > 1 }
        return Intent(query, text, (if (parts.isEmpty()) listOf(text) else parts).take(2))
    }

    /** How well one candidate answers the query. 0 means "not related at all". */
    fun score(candidate: Candidate, intent: Intent): Int {
        val title = normalize(candidate.title)
        val artist = normalize(candidate.artist)
        val album = normalize(candidate.album)
        val query = intent.text
        if (title.isBlank() && artist.isBlank()) return 0
        val artistK = artistKey(candidate.artist)
        val queryArtist = artistKey(query)
        val words = query.split(' ').filter { it.length > 1 }
        var best = 0
        fun keep(value: Int) { if (value > best) best = value }

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
            val firstArtist = artistKey(first)
            val secondArtist = artistKey(second)
            if (artistK == firstArtist && title == second) keep(EXACT_ARTIST_TITLE)
            else if (title == first && artistK == secondArtist) keep(EXACT_ARTIST_TITLE)
            else if (artistK == firstArtist && title.startsWith(second)) keep(ARTIST_TITLE_PREFIX)
            else if (title == first && credited(artistK, secondArtist)) keep(ARTIST_TITLE_PREFIX)
            else if (artistK.contains(firstArtist) && title.contains(second)) keep(BOTH_PARTS)
            else if (title.contains(first) && artistK.contains(secondArtist)) keep(BOTH_PARTS)
        }

        // 2. Exact title, exact artist, exact album.
        if (title == query) keep(EXACT_TITLE)
        if (artistK == queryArtist) keep(EXACT_ARTIST)
        if (album.isNotBlank() && album == query) keep(ALBUM_EXACT)

        // 3. A version of the same song keeps its place right behind the original.
        if (query.length >= 6 && title.startsWith(query)) keep(TITLE_PREFIX)
        if (queryArtist.length >= 3 && artistK.startsWith("$queryArtist ")) keep(ARTIST_PREFIX)
        if (queryArtist.length >= 3 && credited(artistK, queryArtist) && artistK != queryArtist) keep(ARTIST_CREDITED)

        // 4. Collaborations: the artist appears next to "feat" in the title.
        if (containsWord(title, "feat") || containsWord(title, "ft")) {
            if (words.any { word -> word.length > 2 && containsWord(title, word) }) keep(FEAT)
        }

        // 5. Whole words inside the title, the artist or the album.
        if (containsWord(title, query) || containsWord(artist, query)) keep(WORD_MATCH)
        if (album.isNotBlank() && album.startsWith(query)) keep(ALBUM_PREFIX)
        if (album.isNotBlank() && containsWord(album, query)) keep(44)

        // 6. All the query words, in order, inside the title or the artist.
        if (words.isNotEmpty()) {
            if (wordsInOrder(title, words)) keep(46)
            if (wordsInOrder(artist, words)) keep(40)
            // A single shared word is noise ("Alex" for "Alex Campos"), so a multi-word query
            // only counts when at least two of its words really appear.
            val covered = words.count { word -> containsWord(title, word) || containsWord(artist, word) || containsWord(album, word) }
            if (covered > 0 && (words.size == 1 || covered >= 2)) keep(20 + (covered * 14) / words.size)
        }

        // 7. Typos: "alex campo" still finds "Alex Campos".
        val titleDistance = distance(title, query)
        val artistDistance = distance(artistK, queryArtist)
        val allowance = (query.length / 6).coerceAtLeast(1)
        if (titleDistance in 1..allowance) keep(38 - titleDistance * 4)
        if (artistDistance in 1..allowance) keep(34 - artistDistance * 4)
        return best
    }

    /**
     * Ranks and drops the noise. Two extra rules make the answer trustworthy:
     *
     *  - an exact artist wins: when the artist exists in the library, nothing from a different
     *    artist is shown, not even above it;
     *  - an exact title wins: only that song and its versions stay.
     *
     * If both kinds of exact match exist (a song called "Alex Campos" sung by somebody else),
     * both families are kept and the score decides the order.
     */
    fun rank(candidates: Iterable<Candidate>, query: String, floor: Int = MIN_SCORE): List<Ranked> {
        val intent = parse(query)
        val scored = candidates.mapNotNull { candidate ->
            val value = score(candidate, intent)
            if (value >= floor) Ranked(candidate, value) else null
        }
        if (scored.isEmpty()) return emptyList()
        val queryArtist = artistKey(query)
        val exactArtists = scored.filter { artistKey(it.candidate.artist) == queryArtist && it.score >= KEEP_WITH_STRONG }
            .map { artistKey(it.candidate.artist) }.toSet()
        val creditedArtists = scored.filter { credited(artistKey(it.candidate.artist), queryArtist) && it.score >= KEEP_WITH_STRONG }
            .map { artistKey(it.candidate.artist) }.toSet()
        val artistFamily = if (exactArtists.isNotEmpty()) exactArtists else creditedArtists
        val artistMatches = scored.filter { artistKey(it.candidate.artist) in artistFamily && it.score >= KEEP_WITH_STRONG }
        val titleMatches = scored.filter { it.score >= EXACT_TITLE }.let { exact ->
            if (exact.isEmpty()) emptyList() else {
                val wanted = normalize(exact.first().candidate.title)
                scored.filter { val title = normalize(it.candidate.title); title == wanted || title.startsWith("$wanted ") }
            }
        }
        val top = scored.maxOf { it.score }
        val filtered = when {
            artistMatches.isNotEmpty() && titleMatches.isNotEmpty() -> (artistMatches + titleMatches).distinctBy { it.candidate.id }
            artistMatches.isNotEmpty() -> artistMatches
            titleMatches.isNotEmpty() -> titleMatches
            top >= STRONG_SCORE -> scored.filter { it.score >= KEEP_WITH_STRONG }
            else -> scored
        }
        val result = filtered.ifEmpty { scored }
        return result.sortedWith(compareByDescending<Ranked> { it.score }.thenByDescending { it.candidate.plays }.thenBy { it.candidate.title.length })
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
