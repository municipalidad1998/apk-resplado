package com.streamvault.ui.telegram

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.streamvault.R
import com.streamvault.StreamVaultApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ☁ Telegram Cloud — configuración MTProto (API ID, API Hash,
 * número telefónico, código de verificación y contraseña 2FA).
 * Los datos se guardan localmente. La integración MTProto completa
 * (creación de canal de almacenamiento, subida/descarga con
 * fragmentación) llega con el motor TDLib en la fase siguiente.
 */
class TelegramConfigActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_config)
        findViewById<View>(R.id.btnBackTelegram).setOnClickListener { finish() }

        val etApiId = findViewById<EditText>(R.id.etApiId)
        val etApiHash = findViewById<EditText>(R.id.etApiHash)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etCode = findViewById<EditText>(R.id.etCode)
        val etPassword = findViewById<EditText>(R.id.et2fa)

        etApiId.setText(prefs.getString("api_id", ""))
        etApiHash.setText(prefs.getString("api_hash", ""))
        etPhone.setText(prefs.getString("phone", ""))

        findViewById<Button>(R.id.btnSaveTelegram).setOnClickListener {
            val apiId = etApiId.text.toString().trim()
            val apiHash = etApiHash.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val code = etCode.text.toString().trim()
            val pass = etPassword.text.toString()
            if (apiId.isEmpty() || apiHash.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Completa API ID, API Hash y teléfono", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("api_id", apiId)
                .putString("api_hash", apiHash)
                .putString("phone", phone)
                .apply()

            // Flujo de autenticación MTProto (TDLib)
            val client = StreamVaultApp.telegramClient
            client.start()
            lifecycleScope.launch {
                client.authState.collectLatest { state ->
                    when (state) {
                        is com.streamvault.data.cloud.TelegramClient.AuthState.WaitPhone ->
                            client.sendPhoneNumber(phone)
                        is com.streamvault.data.cloud.TelegramClient.AuthState.WaitCode ->
                            if (code.isNotEmpty()) client.sendCode(code)
                        is com.streamvault.data.cloud.TelegramClient.AuthState.WaitPassword ->
                            if (pass.isNotEmpty()) client.sendPassword(pass)
                        is com.streamvault.data.cloud.TelegramClient.AuthState.Ready -> {
                            Toast.makeText(this@TelegramConfigActivity, "☁ Telegram Cloud conectado", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@TelegramConfigActivity, TelegramCloudActivity::class.java))
                            finish()
                        }
                        is com.streamvault.data.cloud.TelegramClient.AuthState.Error ->
                            Toast.makeText(this@TelegramConfigActivity, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        else -> {}
                    }
                }
            }
            Toast.makeText(this, "Conectando con Telegram…", Toast.LENGTH_SHORT).show()
        }
    }
}
