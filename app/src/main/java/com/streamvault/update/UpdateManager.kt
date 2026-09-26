package com.streamvault.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.streamvault.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * Sistema de actualización de APK.
 * Consulta version.json en el repositorio, compara versionCode y
 * descarga/instala la nueva APK con el mecanismo estándar de Android.
 * La nueva APK SIEMPRE mantiene el mismo applicationId y certificado,
 * por lo que se instala encima sin desinstalar ni perder datos.
 */
object UpdateManager {

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/municipalidad1998/apk-resplado/main/version.json"

    data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String, val sizeBytes: Long)

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val body = client.newCall(Request.Builder().url(VERSION_URL).build()).execute()
                .body?.string() ?: return@withContext null
            val j = JSONObject(body)
            val info = UpdateInfo(
                versionCode = j.getInt("versionCode"),
                versionName = j.getString("versionName"),
                apkUrl = j.getString("apkUrl"),
                notes = j.optString("notes", ""),
                sizeBytes = j.optLong("sizeBytes", 0L)
            )
            if (info.versionCode > BuildConfig.VERSION_CODE) info else null
        } catch (e: Exception) { null }
    }

    /** Descarga la APK y lanza el instalador estándar de Android. */
    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        val dest = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "streamvault-update.apk")
        if (dest.exists()) dest.delete()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("StreamVault ${info.versionName}")
            .setDescription("Descargando actualización…")
            .setDestinationUri(Uri.fromFile(dest))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        dm.enqueue(req)

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                try {
                    ctx.unregisterReceiver(this)
                    val uri = FileProvider.getUriForFile(
                        ctx, "${ctx.packageName}.fileprovider", dest
                    )
                    val install = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(install)
                } catch (e: Exception) { /* usuario canceló o error */ }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            if (Build.VERSION.SDK_INT >= 33) Context.RECEIVER_EXPORTED else 0)
    }
}
