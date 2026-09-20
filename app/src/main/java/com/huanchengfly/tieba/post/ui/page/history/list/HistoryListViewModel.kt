package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.HistoryUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

abstract class HistoryListViewModel :
    BaseViewModel<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState, HistoryListUiEvent>() {
    override fun createInitialState(): HistoryListUiState = HistoryListUiState()

    override fun dispatchEvent(partialChange: HistoryListPartialChange): UiEvent? {
        return when (partialChange) {
            is HistoryListPartialChange.Delete.Success -> HistoryListUiEvent.Delete.Success
            is HistoryListPartialChange.Delete.Failure -> HistoryListUiEvent.Delete.Failure(
                partialChange.error.getErrorMessage()
            )

            else -> null
        }
    }
}

@Stable
@HiltViewModel
class ThreadHistoryListViewModel @Inject constructor() : HistoryListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> =
        HistoryListPartialChangeProducer(HistoryUtil.TYPE_THREAD)
}

@Stable
@HiltViewModel
class ForumHistoryListViewModel @Inject constructor() : HistoryListViewModel() {
    override fun createPartialChangeProducer(): PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> =
        HistoryListPartialChangeProducer(HistoryUtil.TYPE_FORUM)
}

private class HistoryListPartialChangeProducer(val type: Int) :
    PartialChangeProducer<HistoryListUiIntent, HistoryListPartialChange, HistoryListUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<HistoryListUiIntent>): Flow<HistoryListPartialChange> =
        merge(
            intentFlow.filterIsInstance<HistoryListUiIntent.Refresh>()
                .flatMapConcat { produceRefreshPartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.LoadMore>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.Delete>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<HistoryListUiIntent.DeleteAll>()
                .flatMapConcat { produceDeleteAllPartialChange() },
        )

    private fun produceDeleteAllPartialChange() = flowOf(HistoryListPartialChange.DeleteAll)

    private fun produceRefreshPartialChange(): Flow<HistoryListPartialChange.Refresh> =
        flow<HistoryListPartialChange.Refresh> {
            val histories = HistoryUtil.get(type, 0)
            emit(
                HistoryListPartialChange.Refresh.Success(
                    todayHistoryData = histories.filter { DateTimeUtils.isToday(it.timestamp) },
                    beforeHistoryData = histories.filterNot { DateTimeUtils.isToday(it.timestamp) },
                    hasMore = histories.size == HistoryUtil.PAGE_SIZE,
                )
            )
        }
            .flowOn(Dispatchers.IO)
            .catch { emit(HistoryListPartialChange.Refresh.Failure(it)) }

    private fun HistoryListUiIntent.LoadMore.producePartialChange(): Flow<HistoryListPartialChange.LoadMore> =
        flow<HistoryListPartialChange.LoadMore> {
            val histories = HistoryUtil.get(type, page)
            emit(
                HistoryListPartialChange.LoadMore.Success(
                    todayHistoryData = histories.filter { DateTimeUtils.isToday(it.timestamp) },
                    beforeHistoryData = histories.filterNot { DateTimeUtils.isToday(it.timestamp) },
                    hasMore = histories.size == HistoryUtil.PAGE_SIZE,
                    currentPage = page
                )
            )
        }
            .onStart {
                emit(HistoryListPartialChange.LoadMore.Start)
            }
            .flowOn(Dispatchers.IO)
            .catch {
                emit(HistoryListPartialChange.LoadMore.Failure(it))
            }

    private fun HistoryListUiIntent.Delete.producePartialChange() =
        flow<HistoryListPartialChange.Delete> {
            DatabaseUtil.deleteHistoryById(id)
            emit(HistoryListPartialChange.Delete.Success(id))
        }
            .flowOn(Dispatchers.IO)
            .catch { emit(HistoryListPartialChange.Delete.Failure(it)) }
}

sealed interface HistoryListUiIntent : UiIntent {
    object Refresh : HistoryListUiIntent

    data class LoadMore(val page: Int) : HistoryListUiIntent

    data class Delete(val id: Long) : HistoryListUiIntent

    object DeleteAll : HistoryListUiIntent
}

sealed interface HistoryListPartialChange : PartialChange<HistoryListUiState> {
    object DeleteAll : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = oldState.copy(
            todayHistoryData = emptyList(),
            beforeHistoryData = emptyList(),
            currentPage = 0,
            hasMore = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    sealed class Refresh : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                todayHistoryData = todayHistoryData,
                beforeHistoryData = beforeHistoryData,
                currentPage = 0,
                hasMore = hasMore
            )
        }

        data class Success(
            val todayHistoryData: List<History>,
            val beforeHistoryData: List<History>,
            val hasMore: Boolean
        ) : Refresh()

        data class Failure(
            val error: Throwable
        ) : Refresh()
    }

    sealed class LoadMore : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState.copy(isLoadingMore = false)
            Start -> oldState.copy(isLoadingMore = true)
            is Success -> oldState.copy(
                isLoadingMore = false,
                todayHistoryData = oldState.todayHistoryData + todayHistoryData,
                beforeHistoryData = oldState.beforeHistoryData + beforeHistoryData,
                currentPage = currentPage,
                hasMore = hasMore
            )
        }

        object Start : LoadMore()

        data class Success(
            val todayHistoryData: List<History>,
            val beforeHistoryData: List<History>,
            val hasMore: Boolean,
            val currentPage: Int
        ) : LoadMore()

        data class Failure(
            val error: Throwable
        ) : LoadMore()
    }

    sealed class Delete : HistoryListPartialChange {
        override fun reduce(oldState: HistoryListUiState): HistoryListUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                todayHistoryData = oldState.todayHistoryData.filterNot { it.id == id },
                beforeHistoryData = oldState.beforeHistoryData.filterNot { it.id == id })
        }

        data class Success(
            val id: Long
        ) : Delete()

        data class Failure(
            val error: Throwable
        ) : Delete()
    }
}

data class HistoryListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val todayHistoryData: List<History> = emptyList(),
    val beforeHistoryData: List<History> = emptyList(),
) : UiState

sealed interface HistoryListUiEvent : UiEvent {
    sealed interface Delete : HistoryListUiEvent {
        object Success : Delete

        data class Failure(
            val errorMsg: String
        ) : Delete
    }

    object DeleteAll : HistoryListUiEvent
}