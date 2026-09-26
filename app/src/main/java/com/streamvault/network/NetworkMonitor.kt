package com.streamvault.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Monitor de red: 🟢 conectado | 🟡 conexión lenta | 🔴 sin conexión.
 * Sirve para reproducción online y para el modo "solo Wi-Fi".
 */
object NetworkMonitor {

    enum class Status { ONLINE, SLOW, OFFLINE, WIFI }

    private val _status = MutableStateFlow(Status.OFFLINE)
    val status: StateFlow<Status> = _status

    fun start(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        update(cm)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { update(cm) }
            override fun onLost(network: android.net.Network) { update(cm) }
            override fun onCapabilitiesChanged(n: android.net.Network, c: NetworkCapabilities) { update(cm) }
        })
    }

    private fun update(cm: ConnectivityManager) {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        _status.value = when {
            caps == null -> Status.OFFLINE
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> Status.OFFLINE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Status.WIFI
            (caps.linkDownstreamBandwidthKbps ?: 0) < 1500 -> Status.SLOW // <1.5 Mbps ≈ lenta
            else -> Status.ONLINE
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun isWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
