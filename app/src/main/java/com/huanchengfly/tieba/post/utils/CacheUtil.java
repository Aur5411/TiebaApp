package com.huanchengfly.tieba.post.utils;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class CacheUtil {
    private CacheUtil() {
    }

    /**
     * 外部存储的 cache 目录在「保留数据卸载 / 跨分支覆盖安装 / 备份恢复」后可能属主错配
     * （既不可读也不可写），甚至直接拿不到。这里统一回落到内部 cacheDir，
     * 保证缓存读写不会因为外部存储不可用而崩溃或静默失败。
     */
    private static File cacheRoot(Context context) {
        File external = null;
        try {
            external = context.getExternalCacheDir();
        } catch (Throwable ignored) {
        }
        if (external != null && external.isDirectory() && external.canWrite()) {
            return external;
        }
        return context.getCacheDir();
    }

    @Nullable
    public static <T> T getCache(Context context, String cacheId, Class<T> tClass) {
        File cacheDir = cacheRoot(context);
        File cacheFile = new File(cacheDir, MD5Util.toMd5(tClass.getName() + "_" + cacheId));
        if (cacheFile.exists()) {
            // 文件存在但读不出来（例如外部存储只读/权限异常）时 readFile 返回 null，
            // 直接 base64Decode 会 NPE，这里按「无缓存」处理。
            String content = FileUtil.readFile(cacheFile);
            if (content == null) {
                return null;
            }
            try {
                return GsonUtil.getGson().fromJson(base64Decode(content), tClass);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void putCache(Context context, String cacheId, Object object) {
        File cacheDir = cacheRoot(context);
        File cacheFile = new File(cacheDir, MD5Util.toMd5(object.getClass().getName() + "_" + cacheId));
        try {
            if (cacheFile.exists() || cacheFile.createNewFile()) {
                try {
                    FileUtil.writeFile(cacheFile, base64Encode(GsonUtil.getGson().toJson(object)), false);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String base64Encode(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }

    public static String base64Decode(String s) {
        return new String(Base64.decode(s.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT), StandardCharsets.UTF_8);
    }
}
