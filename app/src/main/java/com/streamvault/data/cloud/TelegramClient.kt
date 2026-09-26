package com.streamvault.data.cloud

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.JsonClient
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cliente MTProto de Telegram usando la interfaz JSON oficial de TDLib
 * (JsonClient). NO usa Bot API. Soporta:
 * - Login con número + código SMS + contraseña 2FA.
 * - Canal privado "StreamVault Cloud" como almacenamiento.
 * - Subida y descarga de archivos; los archivos permanecen en Telegram
 *   aunque se eliminen del dispositivo (sección 19).
 */
class TelegramClient(private val context: Context) {

    sealed class AuthState {
        object Idle : AuthState()
        object WaitPhone : AuthState()
        object WaitCode : AuthState()
        object WaitPassword : AuthState()
        object Ready : AuthState()
        data class Error(val message: String) : AuthState()
    }

    val authState = MutableStateFlow<AuthState>(AuthState.Idle)

    private val prefs by lazy { context.getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clientId: Long = 0L
    private var started = AtomicBoolean(false)
    private var storageChatId: Long
        get() = prefs.getLong("storage_chat_id", 0L)
        set(v) { prefs.edit().putLong("storage_chat_id", v).apply() }

    /** Inicia TDLib y el bucle de actualizaciones. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            System.loadLibrary("tdjni")
        } catch (e: UnsatisfiedLinkError) {
            authState.value = AuthState.Error("Librería nativa TDLib no disponible")
            return
        }
        clientId = JsonClient.create()
        // Configuración del nivel de log (silenciar)
        JsonClient.execute(clientId, JSONObject().put("@type", "setLogVerbosityLevel")
            .put("new_verbosity_level", 0).toString())
        requestTdlibParams()
        scope.launch {
            while (true) {
                val raw = JsonClient.receive(clientId, 60.0)
                if (raw.isNullOrEmpty()) continue
                try { handleUpdate(JSONObject(raw)) } catch (e: Exception) { /* ignorar */ }
            }
        }
    }

    private fun requestTdlibParams() {
        val p = JSONObject()
            .put("@type", "setTdlibParameters")
            .put("use_test_dc", false)
            .put("database_directory", File(context.filesDir, "tdlib").absolutePath)
            .put("use_message_database", true)
            .put("use_file_database", true)
            .put("api_id", prefs.getString("api_id", "")?.toIntOrNull() ?: 0)
            .put("api_hash", prefs.getString("api_hash", "") ?: "")
            .put("system_language_code", "es")
            .put("device_model", Build.MODEL ?: "Android")
            .put("application_version", "2.0")
            .put("enable_storage_optimizer", true)
        send(p)
    }

    private fun handleUpdate(u: JSONObject) {
        when (u.optString("@type")) {
            "updateAuthorizationState" -> {
                when (u.optJSONObject("authorization_state")?.optString("@type")) {
                    "authorizationStateWaitPhoneNumber" -> authState.value = AuthState.WaitPhone
                    "authorizationStateWaitCode" -> authState.value = AuthState.WaitCode
                    "authorizationStateWaitPassword" -> authState.value = AuthState.WaitPassword
                    "authorizationStateReady" -> {
                        authState.value = AuthState.Ready
                        ensureStorageChat()
                    }
                    "authorizationStateLoggingOut", "authorizationStateClosed" ->
                        authState.value = AuthState.WaitPhone
                }
            }
            "error" -> authState.value = AuthState.Error(u.optString("message", "Error de Telegram"))
        }
    }

    fun sendPhoneNumber(phone: String) {
        send(JSONObject().put("@type", "setAuthenticationPhoneNumber").put("phone_number", phone))
    }

    fun sendCode(code: String) {
        send(JSONObject().put("@type", "checkAuthenticationCode").put("code", code))
    }

    fun sendPassword(password: String) {
        send(JSONObject().put("@type", "checkAuthenticationPassword").put("password", password))
    }

    /** Crea (una vez) el supergrupo privado de almacenamiento. */
    private fun ensureStorageChat() {
        if (storageChatId != 0L) return
        val result = JsonClient.execute(clientId, 
            JSONObject().put("@type", "createNewSupergroupChat")
                .put("title", "StreamVault Cloud")
                .put("is_forum", false)
                .put("description", "Almacenamiento personal de StreamVault")
                .toString()
        )
        val chat = JSONObject(result)
        if (chat.optString("@type") == "chat") storageChatId = chat.getLong("id")
    }

    /** Sube un archivo al canal. Devuelve (messageId, remoteFileId) o null. */
    fun uploadFile(file: File): Pair<Long, String>? {
        if (storageChatId == 0L) return null
        val content = JSONObject()
            .put("@type", "inputMessageDocument")
            .put("document", JSONObject().put("@type", "inputFileLocal").put("path", file.absolutePath))
            .put("disable_content_type_detection", false)
        val msg = JSONObject().put("@type", "sendMessage")
            .put("chat_id", storageChatId)
            .put("message_thread_id", 0)
            .put("options", JSONObject())
            .put("input_message_content", content)
        val result = JSONObject(JsonClient.execute(clientId, msg.toString()))
        if (result.optString("@type") != "message") return null
        val doc = result.optJSONObject("content")?.optJSONObject("document")?.optJSONObject("document")
        return result.getLong("id") to (doc?.optString("id") ?: "")
    }

    private fun send(query: JSONObject) {
        if (clientId != 0L) JsonClient.send(clientId, query.toString())
    }
}
