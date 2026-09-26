package com.streamvault.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streamvault.BuildConfig
import kotlinx.coroutines.launch
import com.streamvault.R
import com.streamvault.playback.LoudnessNormalizer
import com.streamvault.web.AdBlocker

/**
 * ⚙ Configuración general:
 * - Bloqueador de anuncios/trackers del WebView.
 * - Normalización de volumen (objetivo LUFS).
 * - Crossfade entre canciones (configurable; el DSP se aplica en
 *   MusicPlayerService).
 * - Sistema de actualización de APK (muestra versión actual).
 */
class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<View>(R.id.btnBackSettings).setOnClickListener { finish() }

        // Bloqueador + estadísticas
        val swAds = findViewById<Switch>(R.id.swAdBlocker)
        swAds.isChecked = AdBlocker.isEnabled(this)
        swAds.setOnCheckedChangeListener { _, checked -> AdBlocker.setEnabled(this, checked) }

        val swTrack = findViewById<Switch>(R.id.swTrackers)
        swTrack.isChecked = AdBlocker.trackersEnabled(this)
        swTrack.setOnCheckedChangeListener { _, checked -> AdBlocker.setTrackersEnabled(this, checked) }

        val (adsBlocked, trackersBlocked) = AdBlocker.getStats(this)
        findViewById<TextView>(R.id.tvAdStats).text =
            "Anuncios bloqueados: $adsBlocked  ·  Rastreadores bloqueados: $trackersBlocked"

        // Calidad de streaming
        val spQuality = findViewById<Spinner>(R.id.spQuality)
        val qualityOptions = listOf("Automática", "Baja", "Normal", "Alta")
        spQuality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, qualityOptions)
        spQuality.setSelection(prefs.getInt("stream_quality", 0).coerceIn(0, 3))
        spQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.edit().putInt("stream_quality", pos).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val swWifi = findViewById<Switch>(R.id.swOnlyWifi)
        swWifi.isChecked = prefs.getBoolean("only_wifi", false)
        swWifi.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("only_wifi", checked).apply() }

        // Normalización
        val swNorm = findViewById<Switch>(R.id.swNormalization)
        swNorm.isChecked = LoudnessNormalizer.isEnabled(this)
        swNorm.setOnCheckedChangeListener { _, checked -> LoudnessNormalizer.setEnabled(this, checked) }

        // Objetivo LUFS
        val spLufs = findViewById<Spinner>(R.id.spLufs)
        val lufsOptions = listOf("-16 LUFS", "-14 LUFS (recomendado)", "-12 LUFS", "Desactivado")
        spLufs.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lufsOptions)
        spLufs.setSelection(
            when (LoudnessNormalizer.getTargetLufs(this)) {
                -16 -> 0; -12 -> 2; else -> 1
            }
        )
        spLufs.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                when (pos) {
                    0 -> LoudnessNormalizer.setTargetLufs(this@SettingsActivity, -16)
                    1 -> LoudnessNormalizer.setTargetLufs(this@SettingsActivity, -14)
                    2 -> LoudnessNormalizer.setTargetLufs(this@SettingsActivity, -12)
                    else -> LoudnessNormalizer.setEnabled(this@SettingsActivity, false)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Crossfade (5/10/15/20s según especificación)
        val spCross = findViewById<Spinner>(R.id.spCrossfade)
        val crossSecs = listOf(0, 5, 10, 15, 20)
        val crossOptions = listOf("Desactivado", "5 segundos", "10 segundos", "15 segundos", "20 segundos")
        spCross.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, crossOptions)
        spCross.setSelection(crossSecs.indexOf(prefs.getInt("crossfade_sec", 0)).coerceAtLeast(0))
        spCross.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs.edit().putInt("crossfade_sec", crossSecs[pos]).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Modo de normalización: LUFS o ReplayGain
        val spNorm = findViewById<Spinner>(R.id.spNormMode)
        val normOptions = listOf("LUFS Normalization (-14 predeterminado)", "ReplayGain (etiquetas del archivo)")
        spNorm.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, normOptions)
        spNorm.setSelection(if (
            com.streamvault.playback.PlaybackSettings.getNormMode(this) ==
            com.streamvault.playback.PlaybackSettings.NormMode.REPLAYGAIN
        ) 1 else 0)
        spNorm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                com.streamvault.playback.PlaybackSettings.setNormMode(
                    this@SettingsActivity,
                    if (pos == 1) com.streamvault.playback.PlaybackSettings.NormMode.REPLAYGAIN
                    else com.streamvault.playback.PlaybackSettings.NormMode.LUFS
                )
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Gapless
        val swGapless = findViewById<Switch>(R.id.swGapless)
        swGapless.isChecked = com.streamvault.playback.PlaybackSettings.isGaplessEnabled(this)
        swGapless.setOnCheckedChangeListener { _, checked ->
            com.streamvault.playback.PlaybackSettings.setGapless(this, checked)
        }

        // Actualización
        findViewById<TextView>(R.id.tvCurrentVersion).text =
            "Versión actual: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})"
        findViewById<Button>(R.id.btnCheckUpdate).setOnClickListener {
            val btn = it as Button
            btn.isEnabled = false
            btn.text = "Buscando…"
            lifecycleScope.launch {
                val update = com.streamvault.update.UpdateManager.checkForUpdate(this@SettingsActivity)
                btn.isEnabled = true
                btn.text = "Buscar actualización"
                if (update == null) {
                    Toast.makeText(this@SettingsActivity, "Ya tienes la última versión (${BuildConfig.VERSION_NAME})", Toast.LENGTH_LONG).show()
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("🔄 Nueva versión ${update.versionName}")
                        .setMessage(
                            "Versión actual: ${BuildConfig.VERSION_NAME}\n" +
                            "Cambios: ${update.notes}\n" +
                            if (update.sizeBytes > 0) "Tamaño: ${update.sizeBytes / 1024 / 1024} MB\n" else "" +
                            "\nSe instala encima sin perder tus datos."
                        )
                        .setPositiveButton("Actualizar") { _, _ ->
                            com.streamvault.update.UpdateManager.downloadAndInstall(this@SettingsActivity, update)
                        }
                        .setNegativeButton("Después", null)
                        .show()
                }
            }
        }
    }
}
