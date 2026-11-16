package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

object BlockManager {
    private val blockList: MutableList<Block> = mutableListOf()

    val blackList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_BLACK_LIST }

    /** 黑名单里的用户条目（「黑名单」页面显示这些） */
    val blackUserList: List<Block>
        get() = blackList.filter { it.type == Block.TYPE_USER }

    /** 黑名单里的屏蔽词条目（「屏蔽词管理」页面显示这些） */
    val blackKeywordList: List<Block>
        get() = blackList.filter { it.type == Block.TYPE_KEYWORD }

    /**
     * 判断某个用户是否已经在黑名单中。
     * 供「添加到黑名单」等入口做重复添加提示，避免同一个人被反复写入。
     */
    fun containsUser(
        userId: Long = 0L,
        userName: String? = null,
    ): Boolean = blackList.any { block ->
        block.type == Block.TYPE_USER &&
                ((userId != 0L && block.uid == userId.toString()) ||
                        (!userName.isNullOrEmpty() && block.username == userName))
    }

    suspend fun addBlock(block: Block): Block {
        val id = DatabaseUtil.insertBlock(block)
        val savedBlock = block.copy(id = id)
        blockList.add(savedBlock)
        return savedBlock
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val id = DatabaseUtil.insertBlock(block)
            val savedBlock = block.copy(id = id)
            blockList.add(savedBlock)
            callback?.invoke(true)
        }
    }

    suspend fun removeBlock(id: Long) {
        DatabaseUtil.deleteBlockById(id)
        blockList.removeAll { it.id == id }
    }

    suspend fun init() {
        blockList.addAll(DatabaseUtil.getAllBlocks())
    }

    private fun matchesKeyword(block: Block, content: String): Boolean =
        block.getKeywords().any { keyword ->
            if (keyword.isBlank()) {
                false
            } else if (block.isRegex) {
                try {
                    Pattern.compile(keyword).matcher(content).find()
                } catch (_: Exception) {
                    false
                }
            } else {
                content.contains(keyword)
            }
        }

    /**
     * 内容是否命中屏蔽词。
     *
     * 注意：屏蔽词只用于评论，帖子本身不吃屏蔽词，见 [ThreadInfo.shouldBlock] 与 [Post.shouldBlock]。
     */
    fun shouldBlock(content: String): Boolean {
        if (content.isEmpty()) return false
        return blackList.any { it.type == Block.TYPE_KEYWORD && matchesKeyword(it, content) }
    }

    /** 用户是否被拉黑 */
    fun shouldBlock(userId: Long = 0L, userName: String? = null): Boolean {
        val matched = { block: Block ->
            !block.isRegex &&
                    block.type == Block.TYPE_USER &&
                    ((userId != 0L && block.uid == userId.toString()) ||
                            (!userName.isNullOrEmpty() && block.username == userName))
        }
        return blackList.any(matched)
    }

    /**
     * 主题帖只按作者屏蔽。
     * 屏蔽词不作用于帖子，避免加了个词整个帖子在列表里凭空消失。
     */
    fun ThreadInfo.shouldBlock(): Boolean =
        shouldBlock(
            authorId.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name?.ifEmpty { author.nameShow }
        )

    /** 楼层作者是否被拉黑 */
    fun Post.shouldBlockByAuthor(): Boolean =
        shouldBlock(
            author_id.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name?.ifEmpty { author.nameShow }
        )

    /**
     * 楼层：1 楼是楼主正文（等同于帖子本身），屏蔽词不生效；
     * 2 楼及以后是回复，属于「评论」，屏蔽词在这里生效。
     */
    fun Post.shouldBlock(): Boolean =
        shouldBlockByAuthor() || (floor > 1 && shouldBlock(content.plainText))

    /** 这条楼层是否只是「命中屏蔽词」而被拦（用于自动隐藏，而不是显示一条屏蔽提示） */
    fun Post.shouldBlockByKeyword(): Boolean =
        floor > 1 && shouldBlock(content.plainText)

    /** 命中屏蔽词的楼层直接不渲染（按作者屏蔽的仍然显示"已屏蔽"提示） */
    fun Post.autoHidden(): Boolean =
        !shouldBlockByAuthor() && shouldBlockByKeyword()

    /** 楼中楼作者是否被拉黑 */
    fun SubPostList.shouldBlockByAuthor(): Boolean =
        shouldBlock(
            author_id.takeIf { it != 0L } ?: (author?.id ?: -1),
            author?.name?.ifEmpty { author.nameShow }
        )

    fun SubPostList.shouldBlock(): Boolean =
        shouldBlockByAuthor() || shouldBlock(content.plainText)

    /** 楼中楼回复是否只是「命中屏蔽词」而被拦 */
    fun SubPostList.shouldBlockByKeyword(): Boolean =
        shouldBlock(content.plainText)

    /** 命中屏蔽词的楼中楼直接不渲染 */
    fun SubPostList.autoHidden(): Boolean =
        !shouldBlockByAuthor() && shouldBlockByKeyword()

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean =
        shouldBlock(content.orEmpty()) || shouldBlock(
            this.replyer?.id?.toLongOrNull() ?: -1,
            this.replyer?.name?.ifEmpty { this.replyer.nameShow }
        )
}
