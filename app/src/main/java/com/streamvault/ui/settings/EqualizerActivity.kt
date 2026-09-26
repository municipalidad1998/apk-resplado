package com.streamvault.ui.settings

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.streamvault.R

/**
 * 🎚 Ecualizador de 5+ bandas aplicado a la sesión de audio del
 * reproductor (android.media.audiofx). No modifica los archivos.
 */
class EqualizerActivity : AppCompatActivity() {

    companion object { const val EXTRA_SESSION_ID = "session_id" }

    private var equalizer: Equalizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)
        findViewById<View>(R.id.btnBackEq).setOnClickListener { finish() }

        val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, 0)
        try {
            equalizer = Equalizer(0, sessionId)
        } catch (e: Exception) {
            Toast.makeText(this, "Ecualizador no disponible en esta sesión", Toast.LENGTH_LONG).show()
        }
        val eq = equalizer ?: return

        val swEnable = findViewById<Switch>(R.id.swEqEnable)
        swEnable.isChecked = try { eq.enabled } catch (e: Exception) { false }
        swEnable.setOnCheckedChangeListener { _, on -> eq.enabled = on }

        val container = findViewById<LinearLayout>(R.id.eqBands)
        val bands = eq.numberOfBands
        val range = eq.bandLevelRange // [min, max] en milibels
        for (b in 0 until bands) {
            val band = b.toShort()
            val freq = eq.getCenterFreq(band) / 1000
            val label = TextView(this).apply {
                text = if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz"
                setTextColor(0xFF8888AA.toInt()); textSize = 12f
            }
            val seek = SeekBar(this).apply {
                max = (range[1] - range[0]).toInt()
                progress = (eq.getBandLevel(band) - range[0]).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) eq.setBandLevel(band, (range[0] + p).toShort())
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            container.addView(label)
            container.addView(seek)
        }
    }

    override fun onDestroy() {
        equalizer?.release()
        super.onDestroy()
    }
}
