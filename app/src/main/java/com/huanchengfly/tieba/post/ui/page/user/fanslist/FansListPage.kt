package com.huanchengfly.tieba.post.ui.page.user.fanslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.FansPageBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@Destination
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FansListPage(
    uid: Long = 0,
    navigator: DestinationsNavigator,
    viewModel: FansListViewModel = pageViewModel(),
) {
    val showSelf = (uid == 0L || uid == AccountUtil.LocalAccount.current?.uid?.toLong())
    val targetUid = if (showSelf) null else uid

    fun refresh() {
        viewModel.send(FansListUiIntent.Refresh(targetUid))
    }

    LazyLoad(loaded = viewModel.initialized) {
        refresh()
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::hasMore,
        initial = false
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::totalCount,
        initial = 0
    )
    val users by viewModel.uiState.collectPartialAsState(
        prop1 = FansListUiState::users,
        initial = persistentListOf()
    )

    val isEmpty by remember { derivedStateOf { users.isEmpty() } }
    val isError by remember { derivedStateOf { error != null } }

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_fans_list),
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        }
    ) { contentPaddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
        ) {
            Text(
                text = stringResource(id = R.string.text_fans_list_count, totalCount),
                style = MaterialTheme.typography.body2,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                StateScreen(
                    modifier = Modifier.fillMaxSize(),
                    isEmpty = isEmpty,
                    isError = isError,
                    isLoading = isRefreshing,
                    onReload = ::refresh,
                    errorScreen = { ErrorScreen(error = error.getOrNull()) },
                ) {
                    val pullRefreshState = rememberPullRefreshState(
                        refreshing = isRefreshing,
                        onRefresh = ::refresh
                    )
                    val lazyListState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
                        LoadMoreLayout(
                            isLoading = isLoadingMore,
                            onLoadMore = {
                                if (hasMore) {
                                    viewModel.send(
                                        FansListUiIntent.LoadMore(currentPage, targetUid)
                                    )
                                }
                            },
                            loadEnd = !hasMore,
                            lazyListState = lazyListState
                        ) {
                            FansList(
                                data = users,
                                onClick = { item ->
                                    navigator.navigate(UserProfilePageDestination(item.id))
                                },
                                lazyListState = lazyListState
                            )
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
            }
        }
    }
}

@Composable
private fun FansList(
    data: List<FansPageBean.FanBean>,
    onClick: (FansPageBean.FanBean) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    MyLazyColumn(state = lazyListState) {
        items(
            items = data,
            key = { it.id }
        ) {
            FansListItem(
                item = it,
                onClick = { onClick(it) }
            )
        }
    }
}

@Composable
private fun FansListItem(
    item: FansPageBean.FanBean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            data = StringUtil.getAvatarUrl(item.cleanPortrait),
            size = Sizes.Small,
            contentDescription = item.name
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    item.name.orEmpty(),
                    item.nameShow
                ),
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
