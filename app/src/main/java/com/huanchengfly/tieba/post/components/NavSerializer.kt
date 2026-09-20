package com.huanchengfly.tieba.post.components

import android.util.LruCache
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.ramcosta.composedestinations.navargs.DestinationsNavTypeSerializer
import com.ramcosta.composedestinations.navargs.NavTypeSerializer


object ThreadNavBridge {
    private const val MAX_CACHE_SIZE = 4

    private val cache = LruCache<Long, ThreadInfo>(MAX_CACHE_SIZE)

    fun put(data: ThreadInfo): String {
        val id = data.threadId
        cache.put(id, data)
        return id.toString()
    }

    fun get(key: String): ThreadInfo? {
        val id = key.toLongOrNull() ?: return null
        return cache.get(id)
    }
}

@NavTypeSerializer
class ThreadInfoSerializer : DestinationsNavTypeSerializer<ThreadInfo> {
    override fun toRouteString(value: ThreadInfo): String {
        return ThreadNavBridge.put(value)
    }

    override fun fromRouteString(routeStr: String): ThreadInfo {
        return ThreadNavBridge.get(routeStr) ?: ThreadInfo()
    }
}