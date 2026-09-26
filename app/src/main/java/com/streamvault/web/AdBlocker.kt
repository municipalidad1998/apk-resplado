package com.streamvault.web

import android.content.Context
import android.net.Uri

/**
 * Filtro de contenido para el WebView:
 * - Dominios publicitarios y de tracking conocidos.
 * - Bloqueo de pop-ups y redirecciones maliciosas.
 * - Lista actualizable (persistida en SharedPreferences) y
 *   excepciones configurables.
 * NUNCA intenta vulnerar DRM ni mecanismos de seguridad de plataformas.
 */
object AdBlocker {

    private const val PREFS = "adblocker_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TRACKERS = "block_trackers"
    private const val KEY_EXCEPTIONS = "exceptions"
    private const val KEY_CUSTOM = "custom_domains"
    private const val KEY_ADS_BLOCKED = "ads_blocked"
    private const val KEY_TRACKERS_BLOCKED = "trackers_blocked"

    /** Dominios de rastreo (se cuentan aparte en las estadísticas). */
    private val trackerDomains = setOf(
        "analytics.google.com", "google-analytics.com", "scorecardresearch.com",
        "quantserve.com", "doubleverify.com", "moatads.com",
        "telemetry.microsoft.com", "browser-intake-datadoghq.com"
    )

    /** Dominios publicitarios/tracker conocidos (lista base). */
    private val baseBlockedDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googletagservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com",
        "admob.com", "adsense.google.com", "analytics.google.com",
        "google-analytics.com", "scorecardresearch.com", "quantserve.com",
        "taboola.com", "outbrain.com", "ads.yahoo.com", "advertising.com",
        "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net",
        "facebook.net", "ads-twitter.com", "static.ads-twitter.com",
        "tracking.twitter.com", "moatads.com", "doubleverify.com",
        "amazon-adsystem.com", "adsystem.amazon.com",
        "pubmatic.com", "rubiconproject.com", "openx.net",
        "youtube.com/api/ads", "youtube.com/pagead", "googleads.g.doubleclick.net",
        "adservice.google.com", "googlesyndication.com", "pagead2.googlesyndication.com",
        "sharethrough.com", "zedo.com", "adform.net",
        "smartadserver.com", "yieldmo.com", "spotxchange.com",
        "telemetry.microsoft.com", "browser-intake-datadoghq.com"
    )

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getExceptions(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_EXCEPTIONS, emptySet()) ?: emptySet()

    fun addException(context: Context, domain: String) {
        val cur = getExceptions(context).toMutableSet(); cur.add(domain.lowercase())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_EXCEPTIONS, cur).apply()
    }

    fun getCustomBlocked(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_CUSTOM, emptySet()) ?: emptySet()

    fun trackersEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_TRACKERS, true)

    fun setTrackersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_TRACKERS, enabled).apply()
    }

    /** 📊 Estadísticas del bloqueador (anuncios / rastreadores). */
    fun getStats(context: Context): Pair<Int, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getInt(KEY_ADS_BLOCKED, 0) to p.getInt(KEY_TRACKERS_BLOCKED, 0)
    }

    private fun count(context: Context, key: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }

    /** Sustituye la lista con una versión actualizada (p. ej. descargada). */
    fun updateBlockedList(context: Context, domains: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_CUSTOM, domains.map { it.lowercase() }.toSet()).apply()
    }

    fun shouldBlock(context: Context, url: String?): Boolean {
        if (!isEnabled(context) || url.isNullOrBlank()) return false
        val host = try { Uri.parse(url).host?.lowercase() ?: return false } catch (e: Exception) { return false }
        if (getExceptions(context).any { host == it || host.endsWith(".$it") }) return false
        val isTracker = trackerDomains.any { host == it || host.endsWith(".$it") }
        if (isTracker) {
            if (trackersEnabled(context)) { count(context, KEY_TRACKERS_BLOCKED); return true }
        }
        val blocked = baseBlockedDomains + getCustomBlocked(context)
        val hit = blocked.any { host == it || host.endsWith(".$it") }
        if (hit && !isTracker) count(context, KEY_ADS_BLOCKED)
        return hit
    }
}
