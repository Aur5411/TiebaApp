package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.*
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class BlockListViewModel :
    BaseViewModel<BlockListUiIntent, BlockListPartialChange, BlockListUiState, BlockListUiEvent>() {
    override fun createInitialState(): BlockListUiState = BlockListUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<BlockListUiIntent, BlockListPartialChange, BlockListUiState> =
        BlockSettingsPartialChangeProducer

    override fun dispatchEvent(partialChange: BlockListPartialChange): UiEvent? =
        when (partialChange) {
            is BlockListPartialChange.Load.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Add.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Delete.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Add.Success -> BlockListUiEvent.Success.Add
            is BlockListPartialChange.Delete.Success -> BlockListUiEvent.Success.Delete
            else -> null
        }

    private object BlockSettingsPartialChangeProducer :
        PartialChangeProducer<BlockListUiIntent, BlockListPartialChange, BlockListUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<BlockListUiIntent>): Flow<BlockListPartialChange> =
            merge(
                intentFlow.filterIsInstance<BlockListUiIntent.Load>()
                    .flatMapConcat { produceLoadPartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.Add>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.Delete>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun produceLoadPartialChange(): Flow<BlockListPartialChange.Load> =
            flowOf<BlockListPartialChange.Load>(
                BlockListPartialChange.Load.Success(BlockManager.blackList)
            )
                .onStart { BlockListPartialChange.Load.Start }
                .catch { emit(BlockListPartialChange.Load.Failure(it)) }

        private fun BlockListUiIntent.Add.producePartialChange(): Flow<BlockListPartialChange.Add> =
            flow<BlockListPartialChange.Add> {
                val block = Block(
                    category = category,
                    type = type,
                    keywords = if (type == Block.TYPE_KEYWORD) keywords.toJson() else null,
                    username = userName,
                    uid = userId?.toString(),
                    isRegex = isRegex
                )
                val savedBlock = BlockManager.addBlock(block)
                emit(BlockListPartialChange.Add.Success(savedBlock))
            }.catch { emit(BlockListPartialChange.Add.Failure(it)) }

        private fun BlockListUiIntent.Delete.producePartialChange(): Flow<BlockListPartialChange.Delete> =
            flow<BlockListPartialChange.Delete> {
                BlockManager.removeBlock(id)
                emit(BlockListPartialChange.Delete.Success(id))
            }.catch { emit(BlockListPartialChange.Delete.Failure(it)) }
    }
}

sealed interface BlockListUiIntent : UiIntent {
    object Load : BlockListUiIntent

    data class Add(
        val category: Int,
        val type: Int = Block.TYPE_KEYWORD,
        val keywords: List<String> = emptyList(),
        val isRegex: Boolean = false,
        val userName: String? = null,
        val userId: Long? = null,
    ) : BlockListUiIntent

    data class Delete(
        val id: Long,
    ) : BlockListUiIntent
}

sealed interface BlockListPartialChange : PartialChange<BlockListUiState> {
    sealed class Load : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                Start -> oldState.copy(isLoading = true)
                is Success -> oldState.copy(
                    isLoading = false,
                    blackList = blackList,
                )

                is Failure -> oldState.copy(isLoading = false)
            }

        object Start : Load()

        data class Success(val blackList: List<Block>) : Load()

        data class Failure(val error: Throwable) : Load()
    }

    sealed class Add : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> oldState.copy(blackList = oldState.blackList + item)

                is Failure -> oldState
            }

        data class Success(val item: Block) : Add()

        data class Failure(val error: Throwable) : Add()
    }

    sealed class Delete : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> oldState.copy(
                    blackList = oldState.blackList.filterNot { it.id == deletedId }
                )

                is Failure -> oldState
            }

        data class Success(val deletedId: Long) : Delete()

        data class Failure(val error: Throwable) : Delete()
    }
}

data class BlockListUiState(
    val isLoading: Boolean = true,
    val blackList: List<Block> = emptyList(),
) : UiState

sealed interface BlockListUiEvent : UiEvent {
    sealed interface Success : BlockListUiEvent {
        object Add : Success
        object Delete : Success
    }
}