package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.utils.BlockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 屏蔽逻辑回归测试（纯 JVM 单元测试，不依赖 Android 运行时）。
 *
 * 覆盖：
 * 1. 屏蔽词普通包含匹配 / 正则匹配 / 空词不匹配 / 非法正则不崩；
 * 2. 屏蔽词只认「黑名单 + TYPE_KEYWORD」条目，用户条目不会被当关键词；
 * 3. blackList / blackUserList / blackKeywordList 三个视图的过滤是否正确；
 * 4. containsUser 命中判定（uid 优先、username 兜底）。
 */
class BlockManagerTest {

    private val blockListField =
        BlockManager::class.java.getDeclaredField("blockList").apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    private fun blocks(): MutableList<Block> = blockListField.get(BlockManager) as MutableList<Block>

    @Before
    fun clearBlocks() {
        blocks().clear()
    }

    private fun keywordBlock(value: String, isRegex: Boolean = false) = Block(
        id = 0L,
        category = Block.CATEGORY_BLACK_LIST,
        type = Block.TYPE_KEYWORD,
        keywords = "[${"\""}$value${"\""}]",
        isRegex = isRegex,
    )

    private fun userBlock(name: String, uid: Long) = Block(
        id = 0L,
        category = Block.CATEGORY_BLACK_LIST,
        type = Block.TYPE_USER,
        username = name,
        uid = uid.toString(),
    )

    @Test
    fun keyword_containsMatch() {
        blocks().add(keywordBlock("广告"))
        assertTrue(BlockManager.shouldBlock("这是一条广告评论"))
        assertFalse(BlockManager.shouldBlock("正常评论"))
        assertFalse(BlockManager.shouldBlock(""))
    }

    @Test
    fun keyword_regexMatch() {
        blocks().add(keywordBlock("^加[Vv]信", isRegex = true))
        assertTrue(BlockManager.shouldBlock("加v信聊"))
        assertFalse(BlockManager.shouldBlock("请加个微信"))
    }

    @Test
    fun keyword_invalidRegexDoesNotCrash() {
        blocks().add(keywordBlock("[", isRegex = true))
        assertFalse(BlockManager.shouldBlock("任意内容"))
    }

    @Test
    fun keyword_blankDoesNotMatch() {
        blocks().add(keywordBlock("   "))
        assertFalse(BlockManager.shouldBlock("任意内容"))
    }

    @Test
    fun keyword_onlyKeywordTypeMatches() {
        // 用户条目里即使 keywords 字段有内容，也不能被当屏蔽词
        blocks().add(
            Block(
                category = Block.CATEGORY_BLACK_LIST,
                type = Block.TYPE_USER,
                username = "张三",
                uid = "12345",
                keywords = "[\"张三\"]",
            )
        )
        assertFalse(BlockManager.shouldBlock("张三在评论里出现"))
    }

    @Test
    fun listViewsSplitByType() {
        blocks().add(keywordBlock("广告"))
        blocks().add(userBlock("张三", 12345L))
        assertEquals(2, BlockManager.blackList.size)
        assertEquals(1, BlockManager.blackUserList.size)
        assertEquals(1, BlockManager.blackKeywordList.size)
        assertEquals(Block.TYPE_USER, BlockManager.blackUserList.first().type)
        assertEquals(Block.TYPE_KEYWORD, BlockManager.blackKeywordList.first().type)
    }

    @Test
    fun containsUser_byUidAndName() {
        blocks().add(userBlock("张三", 12345L))
        assertTrue(BlockManager.containsUser(userId = 12345L))
        assertTrue(BlockManager.containsUser(userName = "张三"))
        assertFalse(BlockManager.containsUser(userId = 999L, userName = "李四"))
    }

    @Test
    fun shouldBlockUser_byUidAndName() {
        blocks().add(userBlock("张三", 12345L))
        assertTrue(BlockManager.shouldBlock(userId = 12345L))
        assertTrue(BlockManager.shouldBlock(userName = "张三"))
        assertFalse(BlockManager.shouldBlock(userId = 1L, userName = "李四"))
    }
}
