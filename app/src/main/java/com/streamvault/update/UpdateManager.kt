package com.streamvault.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to GitHub Releases to detect a newer build of this app and installs it with the
 * official Android installer. Nothing is installed silently: Android always shows its own
 * confirmation screen, and the user must have allowed installing from this source once.
 */
class UpdateManager(private val context: Context) {

    @Volatile private var downloaded: File? = null

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(UpdateConfig.LATEST).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 12_000
                connection.readTimeout = 12_000
                if (connection.responseCode !in 200..299) return@withContext null
                UpdateParser.parse(connection.inputStream.bufferedReader().readText())
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    suspend fun download(info: ReleaseInfo, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { if (it.extension == "apk") it.delete() }
        val target = File(directory, info.apkName.substringAfterLast('/').ifBlank { "lumina-update.apk" })
        val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            val code = connection.responseCode
            if (code !in 200..299) error("El servidor respondió $code al descargar la actualización")
            val total = connection.contentLengthLong.coerceAtLeast(1)
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    var last = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        val percent = (written * 100 / total).toInt().coerceIn(0, 100)
                        if (percent != last) { last = percent; withContext(Dispatchers.Main) { progress(percent) } }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (target.length() == 0L) error("La descarga quedó vacía. Revisa tu conexión e inténtalo otra vez.")
        downloaded = target
        target
    }

    fun pending(): File? = downloaded?.takeIf { it.exists() }

    /** Opens the official Android installer for an already downloaded APK. */
    fun install(file: File) {
        if (UpdateInstaller.commit(context, file)) return
        // Older Android versions or a rejected session: fall back to the classic intent.
        val authority = "${context.packageName}.files"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Android refuses an update signed with a different key. Detecting it before showing the
     * installer lets us warn the user instead of failing with a cryptic system message.
     * Returns null when the comparison is not possible on this device.
     */
    @Suppress("DEPRECATION")
    fun signatureMatches(file: File): Boolean? = runCatching {
        val archived = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)?.signatures
        val installed = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        val mine = installed?.map { it.toCharsString() }?.toSet().orEmpty()
        val theirs = archived?.map { it.toCharsString() }?.toSet().orEmpty()
        if (mine.isEmpty() || theirs.isEmpty()) null else mine.intersect(theirs).isNotEmpty()
    }.getOrNull()

    /** Asks Android to remove the currently installed build, so a differently signed APK can take its place. */
    fun requestUninstall() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE).setData(Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Android 8+ requires an explicit allowance before an app may trigger installs. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
