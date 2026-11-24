package com.huanchengfly.tieba.post.utils

import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.util.Logger
import java.io.File
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 复现 issue #14 的核心故障模式：磁盘缓存目录不可用时，Sketch 的
 * `edit()` / `get()` 会直接抛异常并冒泡打断整个图片请求。
 *
 * [ResilientDiskCache] 必须把这类异常吞掉，让上层看到「缓存未命中 / 写入失败」，
 * 从而继续走网络加载 —— 也就是 issue 里期望的「降级为仅网络加载」。
 */
class ResilientDiskCacheTest {

    /** 模拟 externalCacheDir/sketch3 属主错配：任何磁盘访问都抛 IOException。 */
    private class BrokenDiskCache : DiskCache {
        override var logger: Logger? = null
        override val directory: File
            get() = throw IOException("EACCES: legacy sketch cache owned by another uid")
        override val maxSize: Long
            get() = throw IOException("EACCES")
        override val size: Long
            get() = throw IOException("EACCES")
        override fun edit(key: String): DiskCache.Editor? = throw IOException("EACCES")
        override fun remove(key: String): Boolean = throw IOException("EACCES")
        override fun get(key: String): DiskCache.Snapshot? = throw IOException("EACCES")
        override fun exist(key: String): Boolean = throw IOException("EACCES")
        override fun clear() = throw IOException("EACCES")
        override fun editLock(key: String): Mutex = throw IOException("EACCES")
        override fun close() = throw IOException("EACCES")
    }

    @Test
    fun brokenCacheNeverThrowsAndBehavesAsCacheMiss() {
        val fallback = File(System.getProperty("java.io.tmpdir"), "sketch3-fallback")
        val cache = ResilientDiskCache(BrokenDiskCache(), "download", fallback)

        // 关键：全部方法都不得抛异常
        assertNull("get 必须是缓存未命中", cache.get("k"))
        assertFalse("exist 必须是 false", cache.exist("k"))
        assertNull("edit 必须返回 null，让 HttpUriFetcher 回落 readBytes", cache.edit("k"))
        assertFalse(cache.remove("k"))
        assertEquals(0L, cache.size)
        assertEquals(fallback, cache.directory)
        assertTrue("必须进入降级标记", cache.isDegraded)

        cache.clear()
        cache.close()
    }

    @Test
    fun clearResetsDegradedFlag() {
        val fallback = File(System.getProperty("java.io.tmpdir"), "sketch3-fallback")
        val cache = ResilientDiskCache(BrokenDiskCache(), "result", fallback)
        assertNull(cache.get("k"))
        assertTrue(cache.isDegraded)
        // 「清除图片缓存」是给用户的自愈入口，清完应复位标记以便重新尝试建缓存
        cache.clear()
        assertFalse(cache.isDegraded)
    }

    @Test
    fun degradedCacheShortCircuits() {
        var hits = 0
        val fallback = File(System.getProperty("java.io.tmpdir"), "sketch3-fallback")
        // 第一次正常返回未命中，第二次开始像真实故障那样抛异常
        val counting = object : DiskCache {
            override var logger: Logger? = null
            override val directory: File get() = File("/nonexistent")
            override val maxSize: Long get() = 0L
            override val size: Long get() = 0L
            override fun edit(key: String): DiskCache.Editor? {
                hits++
                throw IOException("EACCES")
            }
            override fun remove(key: String) = false
            override fun get(key: String): DiskCache.Snapshot? {
                hits++
                return null
            }
            override fun exist(key: String) = false
            override fun clear() {}
            override fun editLock(key: String) = Mutex()
            override fun close() {}
        }
        val cache = ResilientDiskCache(counting, "download", fallback)
        assertNull(cache.get("a"))
        assertEquals(1, hits)
        assertFalse("未抛异常时不应降级", cache.isDegraded)

        // 这次 edit 抛异常 → 进入降级
        assertNull(cache.edit("b"))
        assertEquals(2, hits)
        assertTrue(cache.isDegraded)

        // 降级后不再去撞磁盘，避免一屏几十张图刷爆日志
        assertNull(cache.get("c"))
        assertNull(cache.edit("d"))
        assertEquals("降级后不应再访问底层磁盘缓存", 2, hits)
    }
}
