package com.streamvault.util

import com.streamvault.data.model.Channel

object ChannelCache {
    private val cache = mutableListOf<Channel>()
    private var loaded = false

    fun set(channels: List<Channel>) {
        cache.clear()
        cache.addAll(channels)
        loaded = true
    }

    fun get(): List<Channel> = cache.toList()

    fun isLoaded() = loaded && cache.isNotEmpty()

    fun clear() { cache.clear(); loaded = false }
}
