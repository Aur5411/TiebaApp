package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.github.panpf.sketch.sketch
import java.io.File
import java.math.BigDecimal
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片缓存工具类
 * Created by Trojx on 2016/10/10 0010.
 */
object ImageCacheUtil {
    private const val TAG = "ImageCacheUtil"

    // 旧版 Glide 的磁盘缓存目录名，仅用于清理历史残留
    const val DEFAULT_DISK_CACHE_DIR = "image_manager_disk_cache"

    /**
     * 清除图片磁盘缓存。主线程调用时切到后台线程执行，结果不上报；
     * 需要结果请用 [clearImageDiskCacheBlocking] 或 [clearImageAllCache]。
     */
    fun clearImageDiskCache(context: Context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            thread(name = "clear-image-cache") { clearImageDiskCacheBlocking(context) }
        } else {
            clearImageDiskCacheBlocking(context)
        }
    }

    /**
     * 同步清除图片磁盘缓存（不要在主线程序列上直接调用），返回是否全部清除成功。
     */
    fun clearImageDiskCacheBlocking(context: Context): Boolean {
        var success = true
        try {
            context.sketch.downloadCache.clear()
        } catch (e: Throwable) {
            Log.w(TAG, "failed to clear sketch download cache", e)
            success = false
        }
        try {
            context.sketch.resultCache.clear()
        } catch (e: Throwable) {
            Log.w(TAG, "failed to clear sketch result cache", e)
            success = false
        }
        // 旧版本把 Sketch 缓存放在外部存储，属主错配时那里可能完全不可写，
        // 顺手做一次改名/删除，让「清除缓存」对老用户也真正生效。
        if (!SketchCacheUtil.cleanupLegacyExternal(context)) {
            Log.w(TAG, "legacy sketch cache directory is still present and not writable")
            success = false
        }
        return success
    }

    /**
     * 清除图片内存缓存
     */
    fun clearImageMemoryCache(context: Context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) { //只能在主线程执行
                context.sketch
                    .memoryCache
                    .clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 清除图片所有缓存，返回是否全部清除成功。
     * 内部会自己切到后台线程，可直接在主线程调用。
     */
    suspend fun clearImageAllCache(context: Context): Boolean = withContext(Dispatchers.IO) {
        var success = clearImageDiskCacheBlocking(context)
        withContext(Dispatchers.Main) { clearImageMemoryCache(context) }
        val legacyDirs = listOfNotNull(
            legacyExternalDir(context, DEFAULT_DISK_CACHE_DIR),
            File(context.cacheDir, ".shareTemp"),
        )
        legacyDirs.forEach { dir ->
            if (dir.exists() && !deleteFolderFile(dir.absolutePath, false)) {
                Log.w(TAG, "failed to clear ${dir.path}")
                success = false
            }
        }
        success
    }

    private fun legacyExternalDir(context: Context, name: String): File? = try {
        context.externalCacheDir?.let { File(it, name) }
    } catch (e: Throwable) {
        null
    }

    /**
     * 获取缓存大小
     *
     * @return CacheSize
     */
    fun getCacheSize(context: Context): String {
        try {
            val glideCacheSize = getFolderSize(
                File(
                    context.cacheDir,
                    DEFAULT_DISK_CACHE_DIR
                )
            ).toDouble()
            val shareCacheSize = getFolderSize(File(context.cacheDir, ".shareTemp")).toDouble()
            val sketchCacheSize = context.sketch.run { downloadCache.size + resultCache.size }
            return getFormatSize(glideCacheSize + shareCacheSize + sketchCacheSize)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 获取指定文件夹内所有文件大小的和
     *
     * @param file file
     * @return size
     */
    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        try {
            val fileList = file.listFiles() ?: return 0
            for (aFileList in fileList) {
                size = if (aFileList.isDirectory) {
                    size + getFolderSize(aFileList)
                } else {
                    size + aFileList.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * 删除指定目录下的文件，这里用于缓存的删除
     *
     * @param filePath       filePath
     * @param deleteThisPath deleteThisPath
     */
    private fun deleteFolderFile(filePath: String, deleteThisPath: Boolean): Boolean {
        if (TextUtils.isEmpty(filePath)) return true
        return try {
            val file = File(filePath)
            var success = true
            if (file.isDirectory) {
                file.listFiles()?.forEach {
                    if (!deleteFolderFile(it.absolutePath, true)) success = false
                }
            }
            if (deleteThisPath) {
                if (!file.isDirectory) {
                    if (!file.delete() && file.exists()) success = false
                } else {
                    if (file.listFiles().isNullOrEmpty()) {
                        if (!file.delete() && file.exists()) success = false
                    }
                }
            }
            success
        } catch (e: Exception) {
            Log.w(TAG, "failed to delete $filePath", e)
            false
        }
    }

    /**
     * 格式化单位
     *
     * @param size size
     * @return size
     */
    fun getFormatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            return size.toString() + "B"
        }
        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result1 = BigDecimal(java.lang.Double.toString(kiloByte))
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result2 = BigDecimal(java.lang.Double.toString(megaByte))
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result3 = BigDecimal(java.lang.Double.toString(gigaByte))
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB"
        }
        val result4 = BigDecimal(teraBytes)
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB"
    }
}