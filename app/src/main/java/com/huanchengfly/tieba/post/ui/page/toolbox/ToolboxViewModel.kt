package com.huanchengfly.tieba.post.ui.page.toolbox

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.profile.ProfileResponse
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 「我的贴吧」聚合页的资料卡数据源。
 *
 * 只负责拉取当前登录用户的 [User]（昵称、头像、关注/粉丝/获赞数等），
 * 供页面顶部的资料卡展示。
 */
@HiltViewModel
class ToolboxViewModel @Inject constructor() :
    BaseViewModel<ToolboxUiIntent, ToolboxPartialChange, ToolboxUiState, UiEvent>() {
    override fun createInitialState(): ToolboxUiState = ToolboxUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ToolboxUiIntent, ToolboxPartialChange, ToolboxUiState> =
        ToolboxPartialChangeProducer

    override fun dispatchEvent(partialChange: ToolboxPartialChange): UiEvent? = null

    private object ToolboxPartialChangeProducer :
        PartialChangeProducer<ToolboxUiIntent, ToolboxPartialChange, ToolboxUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<ToolboxUiIntent>): Flow<ToolboxPartialChange> =
            intentFlow.filterIsInstance<ToolboxUiIntent.Refresh>()
                .flatMapConcat { it.producePartialChange() }

        private fun ToolboxUiIntent.Refresh.producePartialChange(): Flow<ToolboxPartialChange.Refresh> =
            TiebaApi.getInstance()
                .userProfileFlow(uid)
                .map<ProfileResponse, ToolboxPartialChange.Refresh> {
                    val user = it.data_?.user
                    if (user == null) {
                        ToolboxPartialChange.Refresh.Failure(IllegalStateException("empty user"))
                    } else {
                        ToolboxPartialChange.Refresh.Success(user)
                    }
                }
                .onStart { emit(ToolboxPartialChange.Refresh.Start) }
                .catch { emit(ToolboxPartialChange.Refresh.Failure(it)) }
    }
}

sealed interface ToolboxUiIntent : UiIntent {
    data class Refresh(val uid: Long) : ToolboxUiIntent
}

sealed interface ToolboxPartialChange : PartialChange<ToolboxUiState> {
    sealed class Refresh : ToolboxPartialChange {
        override fun reduce(oldState: ToolboxUiState): ToolboxUiState = when (this) {
            is Start -> oldState.copy(isRefreshing = true)

            is Success -> oldState.copy(
                isRefreshing = false,
                user = user.wrapImmutable(),
            )

            is Failure -> oldState.copy(isRefreshing = false)
        }

        data object Start : Refresh()
        data class Success(val user: User) : Refresh()
        data class Failure(val error: Throwable) : Refresh()
    }
}

data class ToolboxUiState(
    val isRefreshing: Boolean = false,
    val user: ImmutableHolder<User>? = null,
) : UiState
