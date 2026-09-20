package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.RelateForumBean
import com.huanchengfly.tieba.post.api.models.ThreadBean
import com.huanchengfly.tieba.post.api.models.TopicDetailBean
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.api.models.protos.updateAgreeStatus
import com.huanchengfly.tieba.post.api.models.protos.userLike.ConcernData
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPartialChange
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernUiIntent
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernUiState
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPartialChange
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.collections.map

@Stable
@HiltViewModel
class TopicDetailViewModel @Inject constructor() :
    BaseViewModel<TopicDetailUiIntent, TopicDetailPartialChange, TopicDetailUiState, UiEvent>() {
    override fun createInitialState(): TopicDetailUiState = TopicDetailUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<TopicDetailUiIntent, TopicDetailPartialChange, TopicDetailUiState> =
        TopicDetailPartialChangeProducer

    private object TopicDetailPartialChangeProducer :
        PartialChangeProducer<TopicDetailUiIntent, TopicDetailPartialChange, TopicDetailUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<TopicDetailUiIntent>): Flow<TopicDetailPartialChange> =
            merge(
                intentFlow.filterIsInstance<TopicDetailUiIntent.LoadMore>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<TopicDetailUiIntent.Refresh>()
                    .flatMapConcat { it.produceLoadPartialChange() },
                intentFlow.filterIsInstance<TopicDetailUiIntent.Agree>()
                    .flatMapConcat { it.producePartialChange() }
            )

        private fun TopicDetailUiIntent.LoadMore.producePartialChange(): Flow<TopicDetailPartialChange.LoadMore> =
            TiebaApi.getInstance().topicDetailFlow(
                topicId.toString(),
                topicName,
                1,
                1,
                page,
                pageSize,
                (page - 1) * pageSize,
                lastId.toString()
            )
                .map<TopicDetailBean, TopicDetailPartialChange.LoadMore> {
                    TopicDetailPartialChange.LoadMore.Success(
                        it.data.hasMore,
                        it.data.wreq.page,
                        it.data.topicInfo,
                        it.data.relateForum,
                        it.data.relateThread.threadList,
                    )
                }
                .onStart { emit(TopicDetailPartialChange.LoadMore.Start) }
                .catch { emit(TopicDetailPartialChange.LoadMore.Failure(it)) }

        private fun TopicDetailUiIntent.Refresh.produceLoadPartialChange(): Flow<TopicDetailPartialChange.Refresh> =
            TiebaApi.getInstance().topicDetailFlow(
                topicId.toString(),
                topicName,
                1,
                1,
                1,
                pageSize,
                0,
                ""
            )
                .map<TopicDetailBean, TopicDetailPartialChange.Refresh> {
                    TopicDetailPartialChange.Refresh.Success(
                        it.data.hasMore,
                        it.data.topicInfo,
                        it.data.relateForum,
                        it.data.relateThread.threadList,
                    )
                }
                .onStart { emit(TopicDetailPartialChange.Refresh.Start) }
                .catch { emit(TopicDetailPartialChange.Refresh.Failure(it)) }

        private fun TopicDetailUiIntent.Agree.producePartialChange(): Flow<TopicDetailPartialChange.Agree> =
            TiebaApi.getInstance().opAgreeFlow(
                threadId.toString(), postId.toString(), hasAgree, objType = 3
            ).map<AgreeBean, TopicDetailPartialChange.Agree> {
                TopicDetailPartialChange.Agree.Success(
                    threadId,
                    hasAgree xor 1
                )
            }
                .catch { emit(TopicDetailPartialChange.Agree.Failure(threadId, hasAgree, it)) }
                .onStart { emit(TopicDetailPartialChange.Agree.Start(threadId, hasAgree xor 1)) }
    }
}

sealed interface TopicDetailUiIntent : UiIntent {
    data class Refresh(
        val topicId: Long,
        val topicName: String,
        val pageSize: Int
    ) : TopicDetailUiIntent

    data class LoadMore(
        val topicId: Long,
        val topicName: String,
        val page: Int,
        val pageSize: Int,
        val lastId: Long,
    ) : TopicDetailUiIntent

    data class Agree(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Int,
    ) : TopicDetailUiIntent
}

sealed interface TopicDetailPartialChange : PartialChange<TopicDetailUiState> {
    sealed class Agree private constructor() : TopicDetailPartialChange {
        private fun List<ThreadBean>.updateAgreeStatus(
            threadId: Long,
            hasAgree: Int,
        ): ImmutableList<ThreadBean> {
            return map {
                if (it.feedId == threadId) {
                    it.copy(
                        threadInfo = it.threadInfo.copy(
                            userAgree = hasAgree
                        ),
                        userAgree = hasAgree
                    )
                } else {
                    it
                }
            }.toImmutableList()
        }

        override fun reduce(oldState: TopicDetailUiState): TopicDetailUiState =
            when (this) {
                is Start -> {
                    oldState.copy(
                        relateThread = oldState.relateThread.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Success -> {
                    oldState.copy(
                        relateThread = oldState.relateThread.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Failure -> {
                    oldState.copy(
                        relateThread = oldState.relateThread.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }
            }

        data class Start(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Success(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Failure(
            val threadId: Long,
            val hasAgree: Int,
            val error: Throwable
        ) : Agree()
    }

    sealed class LoadMore : TopicDetailPartialChange {
        override fun reduce(oldState: TopicDetailUiState): TopicDetailUiState = when (this) {
            Start -> oldState.copy(isLoadingMore = true)
            is Success -> oldState.copy(
                isLoadingMore = false,
                currentPage = currentPage,
                hasMore = hasMore,
                topicInfo = topicInfo,
                relateForum = (oldState.relateForum + relateForum).distinctBy { it.forumId }
                    .toImmutableList(),
                relateThread = (oldState.relateThread + relateThread).distinctBy { it.feedId }
                    .toImmutableList(),
            )

            is Failure -> oldState.copy(isLoadingMore = false)
        }

        object Start : LoadMore()

        data class Success(
            val hasMore: Boolean,
            val currentPage: Int,
            val topicInfo: TopicInfoBean,
            val relateForum: List<RelateForumBean>,
            val relateThread: List<ThreadBean>
        ) : LoadMore()

        data class Failure(
            val error: Throwable
        ) : LoadMore()
    }


    sealed class Refresh : TopicDetailPartialChange {
        override fun reduce(oldState: TopicDetailUiState): TopicDetailUiState = when (this) {
            Start -> oldState.copy(isRefreshing = true)
            is Success -> oldState.copy(
                isRefreshing = false,
                currentPage = 1,
                hasMore = hasMore,
                topicInfo = topicInfo,
                relateForum = relateForum.distinctBy { it.forumId }.toImmutableList(),
                relateThread = relateThread.distinctBy { it.feedId }.toImmutableList(),
            )

            is Failure -> oldState.copy(isRefreshing = false)
        }

        object Start : Refresh()

        data class Success(
            val hasMore: Boolean,
            val topicInfo: TopicInfoBean,
            val relateForum: List<RelateForumBean>,
            val relateThread: List<ThreadBean>
        ) : Refresh()

        data class Failure(
            val error: Throwable
        ) : Refresh()
    }
}

data class TopicDetailUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isError: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val topicInfo: TopicInfoBean? = null,
    val relateForum: ImmutableList<RelateForumBean> = persistentListOf(),
    val relateThread: ImmutableList<ThreadBean> = persistentListOf()
) : UiState
