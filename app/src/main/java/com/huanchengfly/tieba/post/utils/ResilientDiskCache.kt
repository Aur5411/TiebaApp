package com.huanchengfly.tieba.post.utils

import android.util.Log
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.util.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex

/**
 * 给 [DiskCache] 套一层「失败即降级」的保护。
 *
 * 背景见 [SketchCacheUtil]：Sketch 3.x 的 `LruDiskCache.edit()/get()` 内部会惰性
 * `openDiskLruCache()`，这一步抛出的 IOException 不被捕获，会一路冒泡到
 * `HttpUriFetcher.fetch()`，导致连网络请求都发不出去，所有图片一起挂掉。
 *
 * 这里把磁盘缓存收敛成「纯加速层」：任何一次读写抛异常，只记一条 WARNING 日志，
 * 对上层表现为「缓存未命中」或「写入失败」——
 *   - `get()` 返回 null  → `readCache()` 返回 null → 继续走网络抓取；
 *   - `edit()` 返回 null → `writeCache()` 返回 null → 回落到 `readBytes()` 内存路径。
 * 于是图片依然能正常显示，只是不落盘，正是 issue 里期望的降级行为。
 */
class ResilientDiskCache(
    private val delegate: DiskCache,
    private val name: String,
    /** `delegate.directory` 自身抛异常时用来兜底返回的目录。 */
    private val fallbackDirectory: File,
) : DiskCache {

    private companion object {
        const val TAG = "ResilientDiskCache"
    }

    private val degraded = AtomicBoolean(false)
    private val fallbackLocks = ConcurrentHashMap<String, Mutex>()

    val isDegraded: Boolean
        get() = degraded.get()

    private fun warn(action: String, e: Throwable, degrade: Boolean = true) {
        // 只在第一次失败时标记降级，但每次都留日志便于排查。
        if (degrade) degraded.set(true)
        try {
            Log.w(TAG, "$name disk cache $action failed, degraded to memory only", e)
        } catch (t: Throwable) {
            // 日志本身绝不能再把图片加载带崩。
        }
    }

    /**
     * 已经确认这块磁盘缓存不可用时，后续调用直接短路，
     * 避免每个图片请求都去撞一次异常（一个页面几十张图会刷满日志）。
     */
    private inline fun <T> skipWhenDegraded(fallback: T, block: () -> T): T =
        if (degraded.get()) fallback else block()

    override var logger: Logger?
        get() = delegate.logger
        set(value) {
            delegate.logger = value
        }

    override val directory: File
        get() = skipWhenDegraded(fallbackDirectory) {
            try {
                delegate.directory
            } catch (e: Throwable) {
                warn("directory", e)
                fallbackDirectory
            }
        }

    override val maxSize: Long
        get() = try {
            delegate.maxSize
        } catch (e: Throwable) {
            0L
        }

    override val size: Long
        get() = skipWhenDegraded(0L) {
            try {
                delegate.size
            } catch (e: Throwable) {
                warn("size", e)
                0L
            }
        }

    override fun edit(key: String): DiskCache.Editor? = skipWhenDegraded(null) {
        try {
            delegate.edit(key)
        } catch (e: Throwable) {
            warn("edit", e)
            null
        }
    }

    override fun remove(key: String): Boolean = skipWhenDegraded(false) {
        try {
            delegate.remove(key)
        } catch (e: Throwable) {
            warn("remove", e)
            false
        }
    }

    override fun get(key: String): DiskCache.Snapshot? = skipWhenDegraded(null) {
        try {
            delegate[key]
        } catch (e: Throwable) {
            warn("get", e)
            null
        }
    }

    override fun exist(key: String): Boolean = skipWhenDegraded(false) {
        try {
            delegate.exist(key)
        } catch (e: Throwable) {
            warn("exist", e)
            false
        }
    }

    override fun clear() {
        try {
            delegate.clear()
        } catch (e: Throwable) {
            warn("clear", e)
        }
        // 清缓存也是给用户的一次「自愈」机会：内存里的降级标记先复位，
        // 下次真正用到磁盘缓存时会重新尝试初始化。
        degraded.set(false)
    }

    override fun editLock(key: String): Mutex {
        val fallback = fallbackLocks.getOrPut(key) { Mutex() }
        if (degraded.get()) return fallback
        val lock = try {
            delegate.editLock(key)
        } catch (e: Throwable) {
            warn("editLock", e)
            null
        }
        return lock ?: fallback
    }

    override fun close() {
        try {
            delegate.close()
        } catch (e: Throwable) {
            warn("close", e, degrade = false)
        }
    }
}
