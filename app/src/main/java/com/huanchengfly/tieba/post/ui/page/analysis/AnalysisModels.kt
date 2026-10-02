package com.huanchengfly.tieba.post.ui.page.analysis

import java.util.Calendar

/**
 * 分析模块的公共数据模型。
 *
 * 归一化之后，统计层与展示层就跟数据来源无关了 ——
 * 主题帖走官方接口、回帖可能因官方缺陷降级到只读镜像，但对上层都是同一份 [PostRecord]。
 */

/** 归一化后的「一条发言」。 */
data class PostRecord(
    val threadId: Long,
    val postId: Long,
    val isThread: Boolean,
    val createTimeSec: Long,
    val forumName: String,
    val title: String,
    val content: String,
) {
    /** 供关键词搜索匹配的全文。 */
    fun searchableText(): String = "$title\n$content\n$forumName"
}

/** 一个统计分桶：[key] 是分桶名（月份 / 小时 / 吧名 / 词），[count] 是命中次数。 */
data class Bucket(val key: String, val count: Int)

/** 用户发言分析结果。 */
data class PostAnalysisResult(
    val total: Int = 0,
    val threadCount: Int = 0,
    val replyCount: Int = 0,
    val forumCount: Int = 0,
    val firstTimeSec: Long = 0L,
    val lastTimeSec: Long = 0L,
    val monthBuckets: List<Bucket> = emptyList(),
    val hourBuckets: List<Bucket> = emptyList(),
    val forumBuckets: List<Bucket> = emptyList(),
    val wordBuckets: List<Bucket> = emptyList(),
)

/** 秒级时间戳 → 「年-月」，如 `2026-09`；非法时间戳返回空串。 */
internal fun monthKeyOf(timeSec: Long): String {
    if (timeSec <= 0L) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = timeSec * 1000L }
    return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

/** 秒级时间戳 → 一天中的小时（0~23）；非法时间戳返回 -1。 */
internal fun hourOf(timeSec: Long): Int {
    if (timeSec <= 0L) return -1
    return Calendar.getInstance().apply { timeInMillis = timeSec * 1000L }
        .get(Calendar.HOUR_OF_DAY)
}

internal fun formatDateTime(timeSec: Long): String {
    if (timeSec <= 0L) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = timeSec * 1000L }
    return "%04d-%02d-%02d %02d:%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
    )
}
