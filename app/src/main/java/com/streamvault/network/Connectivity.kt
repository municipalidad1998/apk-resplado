package com.streamvault.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetState(
    val online: Boolean = false,
    val metered: Boolean = false,
    val slow: Boolean = false,
    val wifi: Boolean = false
) {
    val label: String get() = when {
        !online -> "Sin conexión"
        slow -> "Conexión lenta"
        else -> "Conectado"
    }
}

/**
 * Connectivity state for the quality chooser and the automatic reconnection of the player.
 * "Slow" is taken from the bandwidth the system reports, not from a speed test.
 */
class ConnectivityMonitor(context: Context) {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mutable = MutableStateFlow(current())
    val state: StateFlow<NetState> = mutable.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { mutable.value = current() }
        override fun onLost(network: Network) { mutable.value = current() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { mutable.value = current() }
    }

    fun start() {
        runCatching {
            manager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        }
    }

    fun stop() { runCatching { manager.unregisterNetworkCallback(callback) } }

    fun current(): NetState {
        val network = manager.activeNetwork ?: return NetState()
        val caps = runCatching { manager.getNetworkCapabilities(network) }.getOrNull() ?: return NetState()
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val downKbps = caps.linkDownstreamBandwidthKbps
        return NetState(
            online = online,
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            // Below ~512 kbps a FLAC stream will stutter, so it counts as a slow link.
            slow = downKbps in 1..511,
            wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        )
    }
}
