package com.huanchengfly.tieba.post.ui.page.searchuser

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * 「查人」页面的数据源。
 *
 * 贴吧有「tieba_uid」（个人主页 ID）和「user_id」（内部用户 ID）两套不同的 ID。
 * profile 类接口（含本页用到的 cmd=303024）认的是 **user_id**，
 * 因此资料页「复制 ID」按钮复制的也是 user_id —— 两处保持同一套 ID，粘贴即可查询。
 *
 * 历史坑：早先版本试图用 `getUserByTiebaUid`（cmd=309702）把 tieba_uid 转成 user_id，
 * 但该接口在服务端已失效（恒返回 error_code=0 + 空 data），导致 1.6.1~1.6.3 一直
 * 报 "empty user"。现已彻底移除该转换步骤，直接把输入的数字交给 profile 接口。
 */
@HiltViewModel
class SearchUserViewModel @Inject constructor() :
    BaseViewModel<SearchUserUiIntent, SearchUserPartialChange, SearchUserUiState, UiEvent>() {

    override fun createInitialState(): SearchUserUiState = SearchUserUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<SearchUserUiIntent, SearchUserPartialChange, SearchUserUiState> =
        SearchUserPartialChangeProducer

    override fun dispatchEvent(partialChange: SearchUserPartialChange): UiEvent? = null

    private object SearchUserPartialChangeProducer :
        PartialChangeProducer<SearchUserUiIntent, SearchUserPartialChange, SearchUserUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<SearchUserUiIntent>): Flow<SearchUserPartialChange> =
            intentFlow.filterIsInstance<SearchUserUiIntent.Search>()
                .flatMapConcat { it.producePartialChange() }

        private fun SearchUserUiIntent.Search.producePartialChange(): Flow<SearchUserPartialChange.Search> =
            flow {
                emit(userIdFromTiebaUid(tiebaUid))
            }
                .flowOn(Dispatchers.IO)
                .flatMapConcat { userId ->
                    TiebaApi.getInstance()
                        .userProfileFlow(userId)
                        .map<ProfileResponse, SearchUserPartialChange.Search> {
                            val user = it.data_?.user
                            // 只有连昵称/ID 都拿不到时，才算真的「查无此人」。
                            // 服务端对无效 ID 会返回一个字段几乎全空的 User 对象，
                            // 早期直接拿 user == null 判断会漏掉这种情况。
                            if (user == null || (user.id == 0L && user.name.isEmpty() && user.nameShow.isEmpty())) {
                                SearchUserPartialChange.Search.Failure(
                                    IllegalStateException("not found")
                                )
                            } else {
                                SearchUserPartialChange.Search.Success(user)
                            }
                        }
                }
                .onStart { emit(SearchUserPartialChange.Search.Start) }
                .catch { emit(SearchUserPartialChange.Search.Failure(it)) }

        /**
         * 把用户输入的数字解析成可用于 profile 查询的 uid。
         *
         * 实测结论（2026-09-20 抓包验证）：`profile`（cmd=303024）的 `friend_uid`
         * 字段对**任意 8~10 位数字**都能直接返回对应资料，**不需要**先做
         * tieba_uid → user_id 的转换。
         *
         * 而曾经用到的转换接口 `getUserByTiebaUid`（cmd=309702）在服务端已失效：
         * 无论传什么参数都返回 error_code=0 且 data 为空，这正是历史上
         * 三个版本（1.6.1 / 1.6.2 / 1.6.3）一直报 "empty user" 的真正原因。
         *
         * 因此这里直接返回输入值，交给 profile 接口去认。
         */
        private fun userIdFromTiebaUid(tiebaUid: String): Long =
            tiebaUid.toLongOrNull() ?: throw IllegalStateException("invalid uid")
    }
}

sealed interface SearchUserUiIntent : UiIntent {
    /** 查询指定「贴吧 UID」（tieba_uid）的用户资料。 */
    data class Search(val tiebaUid: String) : SearchUserUiIntent
}

sealed interface SearchUserPartialChange : PartialChange<SearchUserUiState> {
    sealed class Search : SearchUserPartialChange {
        override fun reduce(oldState: SearchUserUiState): SearchUserUiState = when (this) {
            is Start -> oldState.copy(
                isSearching = true,
                hasSearched = true,
                errorMessage = null,
            )

            is Success -> oldState.copy(
                isSearching = false,
                user = user.wrapImmutable(),
                errorMessage = null,
                searchedUid = user.id,
            )

            is Failure -> oldState.copy(
                isSearching = false,
                user = null,
                errorMessage = error.message ?: "查询失败",
            )
        }

        data object Start : Search()
        data class Success(val user: User) : Search()
        data class Failure(val error: Throwable) : Search()
    }
}

data class SearchUserUiState(
    val isSearching: Boolean = false,
    /** 是否已经执行过至少一次查询（用于区分「未查询」与「查无结果」）。 */
    val hasSearched: Boolean = false,
    val searchedUid: Long = 0L,
    val user: ImmutableHolder<User>? = null,
    val errorMessage: String? = null,
) : UiState
