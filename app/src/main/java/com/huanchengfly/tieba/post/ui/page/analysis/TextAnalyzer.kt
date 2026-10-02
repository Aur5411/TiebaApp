package com.huanchengfly.tieba.post.ui.page.analysis

/**
 * 轻量中文关键词提取（用于词云）。
 *
 * 不引分词库（省掉一个体积不小的字典依赖），改用「二元组 + 三元组」词频统计近似分词：
 * 统计文本里所有相邻 2 / 3 字组合；若某个 3 字组合的频次不低于其包含的
 * 最佳 2 字片段的 70%，说明它是更完整的词，就保留 3 字并抑制那两个 2 字，
 * 避免词云里塞满「的地得」之类的碎片。
 */
internal object TextAnalyzer {

    /** 常见虚词 / 停用单字；n-gram 里只要含一个就丢弃。 */
    private val STOP_CHARS = setOf(
        '的', '了', '是', '我', '你', '他', '她', '它', '在', '有', '和', '就', '不', '也',
        '都', '要', '会', '说', '这', '那', '吗', '吧', '啊', '呢', '很', '还', '被', '把',
        '给', '对', '与', '及', '个', '们', '为', '以', '之', '而', '且', '或', '于', '上',
        '下', '中', '里', '后', '前', '时', '么', '什', '怎', '如', '但', '并', '去', '过',
    )

    private val CHINESE_RUN = Regex("[\\u4e00-\\u9fa5]+")

    /** 从多段文本里提取高频词，返回按频次降序的结果（最多 [limit] 个）。 */
    fun keywords(texts: List<String>, limit: Int = 50): List<Bucket> {
        val counts = HashMap<String, Int>()
        for (text in texts) {
            if (text.isEmpty()) continue
            for (run in CHINESE_RUN.findAll(text)) {
                val s = run.value
                if (s.length < 2) continue
                for (i in 0..s.length - 2) {
                    val g2 = s.substring(i, i + 2)
                    if (!isStop(g2)) counts[g2] = (counts[g2] ?: 0) + 1
                }
                for (i in 0..s.length - 3) {
                    val g3 = s.substring(i, i + 3)
                    if (!isStop(g3)) counts[g3] = (counts[g3] ?: 0) + 1
                }
            }
        }
        if (counts.isEmpty()) return emptyList()

        // 3 字组合优先：够频（>=2）且不弱于其 2 字子片段的 70%，认为是更完整的词。
        val suppressed = HashSet<String>()
        for ((gram, count) in counts) {
            if (gram.length != 3 || count < 2) continue
            val subs = listOf(gram.substring(0, 2), gram.substring(1, 3))
            val bestSub = subs.maxOf { counts[it] ?: 0 }
            if (count.toFloat() >= bestSub * 0.7f) suppressed.addAll(subs)
        }

        val result = ArrayList<Bucket>(counts.size)
        for ((gram, count) in counts) {
            if (gram.length == 3 && count >= 2) result.add(Bucket(gram, count))
        }
        for ((gram, count) in counts) {
            if (gram.length == 2 && count >= 2 && gram !in suppressed) result.add(Bucket(gram, count))
        }
        return result.sortedByDescending { it.count }.take(limit)
    }

    private fun isStop(gram: String): Boolean = gram.any { it in STOP_CHARS }
}
