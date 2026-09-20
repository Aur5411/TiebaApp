package com.huanchengfly.tieba.post.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户回帖（TA 的回帖）数据模型。
 *
 * 背景：官方 protobuf 接口 `POST /c/u/feed/userpost?cmd=303002` 的
 * `is_thread=0`（回帖）分支自 2026-09 起在服务端不再返回任何数据
 * （HTTP 200、error_code=0，但 `post_list` 恒为空，已对 5 个不同 uid 复现）。
 * 主题帖分支 `is_thread=1` 正常。
 *
 * 因此回帖改用第三方只读数据源 [UserReplyRemoteDataSource]（无需登录、可分页），
 * 再映射为官方 [com.huanchengfly.tieba.post.api.models.protos.PostInfoList] 供 UI 复用。
 */

/** 单条回帖。字段对应 eztb `/user/posts` 返回的 JSON。 */
@Serializable
data class UserReplyItem(
    @SerialName("forumId") val forumId: Long = 0,
    @SerialName("forumName") val forumName: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("threadId") val threadId: String = "",
    @SerialName("postId") val postId: String = "",
    @SerialName("cid") val cid: String = "",
    @SerialName("createTime") val createTime: Long = 0,
    @SerialName("affiliated") val affiliated: Boolean = false,
    @SerialName("content") val content: String = "",
    @SerialName("replyTo") val replyTo: String? = null,
)

/** 一页回帖。 */
data class UserReplyPage(
    val items: List<UserReplyItem>,
)
