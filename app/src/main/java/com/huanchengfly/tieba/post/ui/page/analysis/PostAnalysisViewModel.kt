package com.huanchengfly.tieba.post.ui.page.analysis

import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 「TA 的发言分析」数据源。
 *
 * 一次把主题帖与回帖都拉齐（回帖可能走只读镜像），再做四类统计：
 * 按月分布、活跃时段、常逛的吧、高频词云（外加 IP 归属地）。
 *
 * 原始发言列表也一并保留在 [PostAnalysisUiState.posts]，供页面上的关键词搜索直接过滤，
 * 不必为「发言搜索」再拉一遍数据。
 */
@HiltViewModel
class PostAnalysisViewModel @Inject constructor() :
    BaseViewModel<PostAnalysisUiIntent, PostAnalysisPartialChange, PostAnalysisUiState, UiEvent>() {

    override fun createInitialState(): PostAnalysisUiState = PostAnalysisUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<PostAnalysisUiIntent, PostAnalysisPartialChange, PostAnalysisUiState> =
        PostAnalysisPartialChangeProducer

    private object PostAnalysisPartialChangeProducer :
        PartialChangeProducer<PostAnalysisUiIntent, PostAnalysisPartialChange, PostAnalysisUiState> {

        override fun toPartialChangeFlow(intentFlow: Flow<PostAnalysisUiIntent>): Flow<PostAnalysisPartialChange> =
            intentFlow.filterIsInstance<PostAnalysisUiIntent.Analyse>()
                .flatMapConcat { intent ->
                    flow { emit(analyse(intent)) }
                        .map<AnalysisOutcome, PostAnalysisPartialChange> { outcome ->
                            PostAnalysisPartialChange.Success(
                                result = outcome.result,
                                posts = outcome.posts,
                            )
                        }
                        .onStart { emit(PostAnalysisPartialChange.Start) }
                        .catch { emit(PostAnalysisPartialChange.Failure(it)) }
                }
    }
}

private data class AnalysisOutcome(
    val result: PostAnalysisResult,
    val posts: List<PostRecord>,
)

private suspend fun analyse(intent: PostAnalysisUiIntent.Analyse): AnalysisOutcome {
    val threads = AnalysisRepository.fetchUserPosts(
        uid = intent.uid,
        isThread = true,
        maxPages = intent.pages,
    )
    val replies = AnalysisRepository.fetchUserPosts(
        uid = intent.uid,
        isThread = false,
        maxPages = intent.pages,
        authorName = intent.authorName,
        authorNameShow = intent.authorNameShow,
        authorPortrait = intent.authorPortrait,
    )
    // 两个来源可能重复（例如主题帖里也带了自己的回复），按 帖子+楼层 去重。
    val all = (threads + replies).distinctBy { "${it.threadId}_${it.postId}" }

    val monthMap = LinkedHashMap<String, Int>()
    val hourCounts = IntArray(24)
    val forumMap = HashMap<String, Int>()
    val times = ArrayList<Long>()

    for (post in all) {
        if (post.createTimeSec > 0L) {
            times.add(post.createTimeSec)
            val month = monthKeyOf(post.createTimeSec)
            if (month.isNotEmpty()) monthMap[month] = (monthMap[month] ?: 0) + 1
            val hour = hourOf(post.createTimeSec)
            if (hour in 0..23) hourCounts[hour] = hourCounts[hour] + 1
        }
        if (post.forumName.isNotBlank()) {
            forumMap[post.forumName] = (forumMap[post.forumName] ?: 0) + 1
        }
    }

    val result = PostAnalysisResult(
        total = all.size,
        threadCount = threads.size,
        replyCount = replies.size,
        forumCount = forumMap.size,
        firstTimeSec = times.minOrNull() ?: 0L,
        lastTimeSec = times.maxOrNull() ?: 0L,
        monthBuckets = monthMap.entries.map { Bucket(it.key, it.value) }.sortedBy { it.key },
        hourBuckets = hourCounts.mapIndexed { hour, count -> Bucket("%02d时".format(hour), count) },
        forumBuckets = forumMap.entries
            .map { Bucket(it.key, it.value) }
            .sortedByDescending { it.count }
            .take(10),
        wordBuckets = TextAnalyzer.keywords(all.map { "${it.title}\n${it.content}" }),
    )
    return AnalysisOutcome(result = result, posts = all)
}

sealed interface PostAnalysisUiIntent : UiIntent {
    /** 开始分析；[pages] 控制每种发言最多拉多少页（每页约 20~30 条）。 */
    data class Analyse(
        val uid: Long,
        val pages: Int = 5,
        val authorName: String = "",
        val authorNameShow: String = "",
        val authorPortrait: String = "",
    ) : PostAnalysisUiIntent
}

sealed interface PostAnalysisPartialChange : PartialChange<PostAnalysisUiState> {
    override fun reduce(oldState: PostAnalysisUiState): PostAnalysisUiState = when (this) {
        is Start -> oldState.copy(isLoading = true, error = null)

        is Success -> oldState.copy(
            isLoading = false,
            error = null,
            result = result,
            posts = posts,
        )

        is Failure -> oldState.copy(
            isLoading = false,
            error = error.message ?: "分析失败",
        )
    }

    data object Start : PostAnalysisPartialChange

    data class Success(
        val result: PostAnalysisResult,
        val posts: List<PostRecord>,
    ) : PostAnalysisPartialChange

    data class Failure(val error: Throwable) : PostAnalysisPartialChange
}

data class PostAnalysisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val result: PostAnalysisResult? = null,
    /** 原始发言列表，供页面上的关键词搜索过滤。 */
    val posts: List<PostRecord> = emptyList(),
) : UiState
