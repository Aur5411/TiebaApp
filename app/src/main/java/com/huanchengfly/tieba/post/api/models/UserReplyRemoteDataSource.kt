package com.huanchengfly.tieba.post.api.models

import com.huanchengfly.tieba.post.api.getUserAgent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 「TA 的回帖」只读数据源。
 *
 * ### 为什么不用官方接口
 * 官方 protobuf 接口 `POST /c/u/feed/userpost?cmd=303002` 在 2026-09 之后：
 * - `is_thread=1`（主题帖）→ 正常返回 `post_list`；
 * - `is_thread=0`（回帖）→ HTTP 200、`error_code=0`，但 `post_list` **恒为空**（服务端缺陷）。
 *
 * 该结论已通过对 5 个不同 uid、以及 subtype/offset/begin_time/need_content/rn 等
 * 十余种参数组合的探测复现，官方 web / mini 路由（403 / 需登录 / JS 空壳）均不可用。
 *
 * ### 本数据源
 * 使用社区维护的只读镜像 `eztb`：`GET {host}/user/posts?method=id&id=<user_id>&page=<n>`。
 * - 无需登录，游客可用；
 * - 每页 30 条，`page` 从 1 开始，超出末页返回 `[]`；
 * - 主/备双域名，主域名失败自动切换（CF Worker 兜底）。
 *
 * 注意：`id` 必须是 **user_id**（不是 tieba_uid），与官方 userpost 一致。
 */
object UserReplyRemoteDataSource {

    private val HOSTS = listOf(
        "https://zwrcjbwbskoa.sealosbja.site",
        "https://cf.eztb.org",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * 拉取某用户第 [page] 页回帖（page 从 1 开始）。
     *
     * @return [UserReplyPage]；请求失败或数据源不可用时抛出异常，由上层决定降级策略。
     */
    @Throws(Exception::class)
    fun fetch(uid: Long, page: Int): UserReplyPage {
        require(uid > 0) { "invalid uid: $uid" }
        val safePage = if (page < 1) 1 else page

        var lastError: Exception? = null
        for (host in HOSTS) {
            try {
                return UserReplyPage(items = fetchFrom(host, uid, safePage))
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("all hosts failed")
    }

    private fun fetchFrom(host: String, uid: Long, page: Int): List<UserReplyItem> {
        val url = "$host/user/posts?method=id&id=$uid&page=$page"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", getUserAgent("tieba/12.52.1.0"))
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://www.eztb.org/")
            .header("Origin", "https://www.eztb.org")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // 404 = 用户不存在；其它视为源故障
                throw IllegalStateException("HTTP ${response.code}: ${body.take(120)}")
            }
            if (body.isBlank()) return emptyList()
            // 末页返回 "[]"；异常时返回 JSON 对象（含 error），也按空处理
            val trimmed = body.trim()
            if (trimmed.startsWith("{")) return emptyList()
            return json.decodeFromString<List<UserReplyItem>>(trimmed)
        }
    }
}
