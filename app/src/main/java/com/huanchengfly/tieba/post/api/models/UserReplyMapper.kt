package com.huanchengfly.tieba.post.api.models

import com.huanchengfly.tieba.post.api.models.protos.Abstract
import com.huanchengfly.tieba.post.api.models.protos.PostInfoContent
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList

/**
 * 把第三方回帖数据 [UserReplyItem] 映射为官方 protobuf 模型 [PostInfoList]，
 * 让「TA 的回帖」页面无需任何 UI 改动即可复用现有列表渲染逻辑。
 *
 * ### 字段语义（已实测确认）
 * 镜像返回的字段是「楼层」视角：
 * - [UserReplyItem.threadId]  —— 主题帖 id（点击进帖用）
 * - [UserReplyItem.postId]    —— 主题帖「一楼」的 post id（同一主题下所有回帖都一样）
 * - [UserReplyItem.cid]       —— **该条回帖本身的 post id**（定位具体楼层用）
 *
 * 实测 30 条里 `postId != cid` 占 27 条，均为**普通回帖**（不是楼中楼），
 * 所以不能拿 `cid != postId` 当楼中楼判据；[PostInfoList.post_id] 应取 `cid`。
 *
 * 映射要点（对照 `UserPostPage` 里 `post.isThread == false` 分支的取值）：
 * - 头部：`user_name` / `name_show` / `user_portrait` / `user_id`
 * - 正文：`content`（repeated [PostInfoContent]）→ 每条含 `post_content`（repeated [Abstract]）、
 *   `create_time`、`post_id`、`post_type`
 * - 原帖入口：`thread_id`（点击跳帖子）、`title`（底部卡片显示原帖标题）、`forum_id`
 *
 * 第三方源不返回发帖人的昵称/头像；而在「TA 的回帖」语境下，发帖人就是被查看的用户，
 * 因此由调用方把该用户的昵称/头像传进来填充头部。
 */
object UserReplyMapper {

    fun toPostInfoList(
        reply: UserReplyItem,
        authorName: String = "",
        authorNameShow: String = "",
        authorPortrait: String = "",
        authorId: Long = 0L,
    ): PostInfoList {
        val threadId = reply.threadId.toLongOrNull() ?: 0L
        val postId = reply.postId.toLongOrNull() ?: 0L
        val cid = reply.cid.toLongOrNull() ?: 0L
        // 楼层 id：优先 cid（该条回帖自身 id），缺失时退回 postId
        val floorPostId = if (cid != 0L) cid else postId

        val text = buildString {
            append(reply.content)
            // 楼中楼：镜像用 replyTo 给出被回复者昵称
            reply.replyTo?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append("\n")
                append("回复 ")
                append(it)
                append("：")
            }
        }.ifBlank { "（无文字内容）" }

        val content = PostInfoContent(
            create_time = reply.createTime,
            // 普通回帖：post_type 0（UI 走 ThreadPage + scrollToReply 定位楼层）
            post_type = 0L,
            post_id = floorPostId,
            post_content = listOf(Abstract(type = 0, text = text)),
        )

        val title = reply.title.ifBlank { reply.forumName }

        return PostInfoList(
            forum_id = reply.forumId,
            thread_id = threadId,
            post_id = postId,
            is_thread = 0,
            create_time = reply.createTime.toInt(),
            forum_name = reply.forumName,
            title = title,
            user_name = authorName,
            user_id = authorId,
            user_portrait = authorPortrait,
            name_show = authorNameShow,
            // 回帖分支 UI 不读 agree；置 null 避免误显示点赞按钮
            agree = null,
            content = listOf(content),
        )
    }

    fun toPostInfoListList(
        items: List<UserReplyItem>,
        authorName: String = "",
        authorNameShow: String = "",
        authorPortrait: String = "",
        authorId: Long = 0L,
    ): List<PostInfoList> = items.map {
        toPostInfoList(
            reply = it,
            authorName = authorName,
            authorNameShow = authorNameShow,
            authorPortrait = authorPortrait,
            authorId = authorId,
        )
    }
}
