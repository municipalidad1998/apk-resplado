package com.streamvault.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Installs the downloaded APK with the PackageInstaller session API instead of a bare
 * ACTION_VIEW intent. The advantage is that Android tells us *why* it refused, so a conflict
 * with a previously installed build can be explained instead of showing a generic failure.
 */
object UpdateInstaller {

    sealed interface Status {
        data object Idle : Status
        data object Pending : Status
        data object Success : Status
        data object NeedsUserAction : Status
        data class Conflict(val detail: String?) : Status
        data class Failed(val detail: String?) : Status
    }

    val status = MutableStateFlow<Status>(Status.Idle)

    /**
     * Returns true when the session was committed (the result arrives later in [status]),
     * false when the caller must fall back to another route.
     */
    fun commit(context: Context, file: File): Boolean {
        status.value = Status.Pending
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_UNSPECIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setInstallReason(PackageManager.INSTALL_REASON_USER)
        }
        val sessionId = try {
            installer.createSession(params)
        } catch (e: Exception) {
            status.value = Status.Failed("No se pudo abrir una sesión de instalación: ${e.localizedMessage}")
            return false
        }
        val session = try {
            installer.openSession(sessionId)
        } catch (e: Exception) {
            status.value = Status.Failed("No se pudo preparar la instalación: ${e.localizedMessage}")
            return false
        }
        return try {
            session.openWrite("lumina-update", 0, file.length()).use { target ->
                file.inputStream().use { input -> input.copyTo(target) }
                session.fsync(target)
            }
            val intent = Intent(context, InstallResultReceiver::class.java).setAction(ACTION_RESULT)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
            true
        } catch (e: Exception) {
            status.value = Status.Failed("No se pudo enviar el APK al instalador: ${e.localizedMessage}")
            try {
                session.abandon()
            } catch (_: Exception) {
            }
            false
        } finally {
            runCatching { session.close() }
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter(ACTION_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(InstallResultReceiver(), filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(InstallResultReceiver(), filter)
        }
    }

    const val ACTION_RESULT = "com.streamvault.INSTALL_RESULT"
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UpdateInstaller.ACTION_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        @Suppress("DEPRECATION")
        val other = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                UpdateInstaller.status.value = UpdateInstaller.Status.NeedsUserAction
                other?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) } }
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateInstaller.status.value = UpdateInstaller.Status.Success
            PackageInstaller.STATUS_FAILURE_CONFLICT -> UpdateInstaller.status.value = UpdateInstaller.Status.Conflict(detail)
            else -> UpdateInstaller.status.value = UpdateInstaller.Status.Failed(detail ?: "Estado $status")
        }
    }
}
