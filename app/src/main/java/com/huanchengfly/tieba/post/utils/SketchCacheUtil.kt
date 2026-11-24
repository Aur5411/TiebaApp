package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.cache.internal.LruDiskCache
import java.io.File

/**
 * Sketch 磁盘缓存目录的选择，以及旧版本残留目录的清理。
 *
 * Sketch 3.x 默认把磁盘缓存放在 `externalCacheDir/sketch3`。外部存储里
 * `/sdcard/Android/data/<pkg>/` 的属主在「保留数据卸载 / 跨分支覆盖安装 / 备份恢复」
 * 之后可能仍是旧 UID，新进程对它既不能读也不能写，DiskCache 初始化直接抛异常，
 * 之后每个图片请求都在读写磁盘缓存这一步失败并要求重试 —— 表现为全 App 图片
 * 静默不显示（头像变灰底首字母、帖子配图全挂），且没有明显日志。
 *
 * 因此缓存根目录固定使用内部 `cacheDir`（由系统随 UID 管理，不会出现属主错配），
 * 老的外部目录只做尽力清理。
 */
object SketchCacheUtil {
    private const val TAG = "SketchCacheUtil"
    private const val DIR_NAME = "sketch3"

    /**
     * 内部缓存根目录 `cacheDir/sketch3`。
     */
    fun internalRoot(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { mkdirs() }

    /**
     * 建一个「失败即降级」的下载磁盘缓存。目录不可用时只丢缓存，不影响图片加载。
     */
    fun downloadCache(context: Context): DiskCache {
        val dir = File(internalRoot(context), "download")
        return ResilientDiskCache(
            LruDiskCache.ForDownloadBuilder(context).directory(dir).build(),
            "download",
            dir
        )
    }

    /**
     * 建一个「失败即降级」的结果磁盘缓存。
     */
    fun resultCache(context: Context): DiskCache {
        val dir = File(internalRoot(context), "result")
        return ResilientDiskCache(
            LruDiskCache.ForResultBuilder(context).directory(dir).build(),
            "result",
            dir
        )
    }

    /**
     * 旧版本残留在外部存储的缓存根目录，取不到外部存储时返回 null。
     */
    fun legacyExternalRoot(context: Context): File? = try {
        context.externalCacheDir?.let { File(it, DIR_NAME) }
    } catch (e: Throwable) {
        null
    }

    /**
     * 目录是否真的可写。`exists()`/`canWrite()` 在部分 ROM 上会给出误判，
     * 这里直接实际创建探针文件来判断。
     */
    fun isWritable(dir: File): Boolean {
        if (!dir.isDirectory) return false
        return try {
            val probe = File(dir, ".write_probe_${System.nanoTime()}")
            if (probe.createNewFile()) {
                probe.delete()
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * 尽力删除旧的外部缓存目录，返回是否已经彻底清理干净。
     *
     * 属主错误时 `deleteRecursively()` 删不掉子目录（子目录同样没有写权限），
     * 但对 cache 父目录做 `renameTo` 只需要父目录的写权限，所以先把整个目录
     * 改名让位，再删改名后的目录。两步都失败就放弃，不能影响启动。
     */
    fun cleanupLegacyExternal(context: Context): Boolean {
        val legacy = legacyExternalRoot(context) ?: return true
        if (!legacy.exists()) return true
        if (isWritable(legacy)) {
            return try {
                val deleted = legacy.deleteRecursively()
                if (!deleted) {
                    Log.w(TAG, "failed to delete legacy sketch cache: ${legacy.path}")
                }
                deleted
            } catch (e: Throwable) {
                Log.w(TAG, "failed to delete legacy sketch cache: ${legacy.path}", e)
                false
            }
        }
        val parent = legacy.parentFile
        if (parent == null || !parent.isDirectory) {
            Log.w(TAG, "legacy sketch cache parent missing: ${legacy.path}")
            return false
        }
        val quarantine = File(parent, "$DIR_NAME.broken.${System.currentTimeMillis()}")
        val renamed = try {
            legacy.renameTo(quarantine)
        } catch (e: Throwable) {
            Log.w(TAG, "failed to rename legacy sketch cache: ${legacy.path}", e)
            false
        }
        if (!renamed) {
            Log.w(
                TAG,
                "legacy sketch cache is not writable and cannot be renamed: ${legacy.path}"
            )
            return false
        }
        return try {
            quarantine.deleteRecursively()
        } catch (e: Throwable) {
            Log.w(TAG, "failed to delete quarantined sketch cache: ${quarantine.path}", e)
            false
        }
    }
}
