package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GeneralTabListPage(
    forumId: Long,
    forumName: String,
    navTabInfo: FrsTabInfo,
    viewModel: GeneralTabListViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            GeneralTabListUiIntent.FirstLoad(
                forumId = forumId,
                forumName = forumName,
                navTabInfo = navTabInfo,
                sortType = navTabInfo.sort_menu.firstOrNull()?.source_id ?: -1,
            )
        )
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::currentPage,
        initial = 1
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::threadList,
        initial = persistentListOf()
    )
    val sortType by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::sortType,
        initial = -1
    )

    onGlobalEvent<GeneralTabListUiEvent.BackToTop> {
        lazyListState.animateScrollToItem(0)
    }
    onGlobalEvent<GeneralTabListUiEvent.Refresh> { event ->
        viewModel.send(
            GeneralTabListUiIntent.Refresh(
                forumId = forumId,
                forumName = forumName,
                navTabInfo = navTabInfo,
                sortType = event.sortType.takeIf { it >= 0 } ?: sortType,
            )
        )
    }
    viewModel.onEvent<GeneralTabListUiEvent.AgreeFail> {
        coroutineScope.launch {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = context.getString(
                    R.string.snackbar_agree_fail,
                    it.errorCode,
                    it.errorMsg
                ),
                actionLabel = context.getString(R.string.button_retry)
            )
            if (snackbarResult == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    GeneralTabListUiIntent.Agree(
                        threadId = it.threadId,
                        postId = it.postId,
                        hasAgree = it.hasAgree,
                    )
                )
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.send(
                GeneralTabListUiIntent.Refresh(
                    forumId = forumId,
                    forumName = forumName,
                    navTabInfo = navTabInfo,
                    sortType = sortType,
                )
            )
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (navTabInfo.sub_tab_list.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = navTabInfo.sub_tab_list,
                        key = { it.class_id }
                    ) { menu ->
                        Chip(
                            text = menu.class_name,
                            invertColor = false,
                            onClick = { }
                        )
                    }
                }
            }

            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = {
                    viewModel.send(
                        GeneralTabListUiIntent.LoadMore(
                            forumId = forumId,
                            forumName = forumName,
                            navTabInfo = navTabInfo,
                            currentPage = currentPage,
                            lastThreadId = threadList.lastOrNull()?.thread?.get { id } ?: 0,
                            sortType = sortType,
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = threadList.isEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                ThreadList(
                    state = lazyListState,
                    items = threadList,
                    onItemClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                threadInfo = it
                            )
                        )
                    },
                    onItemReplyClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                scrollToReply = true
                            )
                        )
                    },
                    onAgree = { threadInfo ->
                        viewModel.send(
                            GeneralTabListUiIntent.Agree(
                                threadId = threadInfo.id,
                                postId = threadInfo.firstPostId,
                                hasAgree = threadInfo.agree?.hasAgree ?: 0,
                            )
                        )
                    },
                    onUserClicked = {
                        navigator.navigate(UserProfilePageDestination(it.id))
                    }
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.primary,
        )
    }
}

private enum class ItemType { Top, PlainText, SingleMedia, MultiMedia, Video }

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String = stringResource(id = R.string.content_top),
) {
    Row(
        modifier = modifier
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Chip(
            text = type,
            shape = RoundedCornerShape(3.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
    }
}

@Composable
private fun ThreadList(
    state: androidx.compose.foundation.lazy.LazyListState,
    items: ImmutableList<ThreadItemData>,
    onItemClicked: (ThreadInfo) -> Unit,
    onItemReplyClicked: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    onUserClicked: (User) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val itemFraction = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 0.5f
        else -> 1f
    }
    MyLazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(
            items = items,
            key = { index, (holder) ->
                val (item) = holder
                "${index}_${item.id}"
            },
            contentType = { _, (holder) ->
                val (item) = holder
                if (item.isTop == 1) ItemType.Top
                else {
                    if (item.media.isNotEmpty())
                        if (item.media.size == 1) ItemType.SingleMedia else ItemType.MultiMedia
                    else if (item.videoInfo != null) ItemType.Video
                    else ItemType.PlainText
                }
            }
        ) { index, (holder, blocked) ->
            BlockableContent(
                blocked = blocked,
                blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                val (item) = holder
                Column(
                    modifier = Modifier.fillMaxWidth(itemFraction)
                ) {
                    if (item.isTop == 1) {
                        val title = item.title.takeUnless { it.isBlank() } ?: item.abstractText
                        TopThreadItem(
                            title = title,
                            onClick = { onItemClicked(item) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        if (index > 0) {
                            if (items[index - 1].thread.get { isTop } == 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        FeedCard(
                            item = holder,
                            onClick = onItemClicked,
                            onClickReply = onItemReplyClicked,
                            onAgree = onAgree,
                            onClickUser = onUserClicked,
                        )
                    }
                }
            }
        }
    }
}
