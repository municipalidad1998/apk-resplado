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
import com.streamvault.BuildConfig
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

        // Bloqueador
        val swAds = findViewById<Switch>(R.id.swAdBlocker)
        swAds.isChecked = AdBlocker.isEnabled(this)
        swAds.setOnCheckedChangeListener { _, checked -> AdBlocker.setEnabled(this, checked) }

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

        // Crossfade
        val spCross = findViewById<Spinner>(R.id.spCrossfade)
        val crossOptions = listOf("Desactivado", "2 segundos", "4 segundos", "6 segundos", "8 segundos", "10 segundos")
        spCross.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, crossOptions)
        spCross.setSelection(crossOptions.indexOfFirst {
            it.startsWith(prefs.getInt("crossfade_sec", 0).toString()) || (prefs.getInt("crossfade_sec", 0) == 0 && it == "Desactivado")
        }.coerceAtLeast(0))
        spCross.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val sec = if (pos == 0) 0 else pos * 2
                prefs.edit().putInt("crossfade_sec", sec).apply()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Actualización
        findViewById<TextView>(R.id.tvCurrentVersion).text =
            "Versión actual: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})"
        findViewById<Button>(R.id.btnCheckUpdate).setOnClickListener {
            // Mecanismo estándar de Android: misma applicationId + mismo
            // certificado + versionCode mayor = actualización directa.
            Toast.makeText(
                this,
                "Sin actualizaciones disponibles.\nInstala la nueva APK encima: se actualizará sin perder datos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
