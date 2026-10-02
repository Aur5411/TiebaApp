package com.huanchengfly.tieba.post.ui.page.analysis

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.UserReplyMapper
import com.huanchengfly.tieba.post.api.models.UserReplyRemoteDataSource
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 分析模块的取数入口。
 *
 * 沿用仓库既有约定（与 `UserPostViewModel` 完全一致）：
 * - 主题帖走官方 `userpost`（`is_thread=1`），工作正常；
 * - 回帖的官方 `is_thread=0` 自 2026-09 起恒返回空列表（服务端缺陷），
 *   因此官方为空时降级到只读镜像 [UserReplyRemoteDataSource]。
 */
internal object AnalysisRepository {

    /** 拉取某用户的发言（主题帖或回帖），最多 [maxPages] 页。 */
    suspend fun fetchUserPosts(
        uid: Long,
        isThread: Boolean,
        maxPages: Int,
        authorName: String = "",
        authorNameShow: String = "",
        authorPortrait: String = "",
    ): List<PostRecord> = withContext(Dispatchers.IO) {
        val result = ArrayList<PostRecord>()
        for (page in 1..maxPages) {
            val list = loadUserPostPage(
                uid = uid,
                page = page,
                isThread = isThread,
                authorName = authorName,
                authorNameShow = authorNameShow,
                authorPortrait = authorPortrait,
            )
            if (list.isEmpty()) break
            result.addAll(list.map { it.toRecord() })
        }
        result
    }

    private suspend fun loadUserPostPage(
        uid: Long,
        page: Int,
        isThread: Boolean,
        authorName: String,
        authorNameShow: String,
        authorPortrait: String,
    ): List<PostInfoList> {
        val official = TiebaApi.getInstance()
            .userPostFlow(uid, page, isThread)
            .first()
            .data_?.post_list.orEmpty()
        if (official.isNotEmpty()) return official
        if (isThread) return emptyList()

        // 官方回帖接口恒空 → 走只读镜像；镜像不返回昵称/头像，用调用方传入的兜底。
        val replies = UserReplyRemoteDataSource.fetch(uid, page).items
        return UserReplyMapper.toPostInfoListList(
            items = replies,
            authorName = authorName,
            authorNameShow = authorNameShow,
            authorPortrait = authorPortrait,
            authorId = uid,
        )
    }

    private fun PostInfoList.toRecord(): PostRecord {
        val abstractText = content.joinToString("") { it.post_content.abstractText }
        val text = listOf(title.orEmpty(), abstractText, content_thread.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("\n")
        return PostRecord(
            threadId = thread_id,
            postId = post_id,
            isThread = is_thread == 1,
            createTimeSec = create_time.toLong(),
            forumName = forum_name.orEmpty(),
            title = title.orEmpty(),
            content = text,
        )
    }

}
