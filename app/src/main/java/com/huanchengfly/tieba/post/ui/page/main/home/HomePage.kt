package com.huanchengfly.tieba.post.ui.page.main.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.request.DisplayRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.menuBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.toolboxAccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.persistentListOf

private fun getGridCells(
    context: Context,
    listSingle: Boolean = context.appPreferences.listSingle
): GridCells {
    return if (listSingle) {
        GridCells.Fixed(1)
    } else {
        GridCells.Adaptive(180.dp)
    }
}

private const val SORT_BY_HOT = 0
private const val SORT_BY_NEW = 1
private const val SORT_BY_LEVEL = 2
private const val SORT_BY_NAME = 3

/** 按用户选择的排序方式生成比较器，名称相同时用吧 id 兜底保证顺序稳定。 */
private fun forumComparator(sortType: Int): Comparator<HomeUiState.Forum> =
    when (sortType) {
        SORT_BY_NEW -> compareByDescending<HomeUiState.Forum> { it.forumId.toLongOrNull() ?: 0L }
            .thenBy { it.forumName }
        SORT_BY_LEVEL -> compareByDescending<HomeUiState.Forum> { it.levelId.toIntOrNull() ?: 0 }
            .thenBy { it.forumName }
        SORT_BY_NAME -> compareBy { it.forumName }
        else -> compareByDescending<HomeUiState.Forum> { it.hotNum }.thenBy { it.forumName }
    }

@Preview("SearchBoxPreview")
@Composable
fun SearchBoxPreview() {
    SearchBox(
        backgroundColor = Color(0xFFF8F8F8),
        contentColor = Color(0xFFBFBFBF),
        onClick = {}
    )
}

@Composable
fun SearchBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ExtendedTheme.colors.topBarSurface,
    contentColor: Color = ExtendedTheme.colors.onTopBarSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(ExtendedTheme.colors.topBar)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .debounceClickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .align(CenterVertically)
                        .size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.hint_search),
                    modifier = Modifier.align(CenterVertically),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false
) {
    Chip(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp)
            .then(modifier),
        invertColor = invert
    )
}

@Composable
private fun ForumItemPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Image(
            painter = rememberDrawablePainter(
                drawable = ImageUtil.getPlaceHolder(
                    LocalContext.current,
                    0
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(40.dp)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                color = ExtendedTheme.colors.text,
                text = "",
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(visible = true, color = ExtendedTheme.colors.chip),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                color = ExtendedTheme.colors.text,
                text = "",
                modifier = Modifier
                    .width(64.dp)
                    .placeholder(visible = true, color = ExtendedTheme.colors.chip),
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = ExtendedTheme.colors.chip,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip)
        ) {
            Text(
                text = "0",
                color = ExtendedTheme.colors.onChip,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Center)
            )
        }
    }
}

@Composable
private fun ForumItemMenuContent(
    menuState: MenuState,
    isTopForum: Boolean,
    onDeleteTopForum: () -> Unit,
    onAddTopForum: () -> Unit,
    onCopyName: () -> Unit,
    onUnfollow: () -> Unit,
) {
    DropdownMenuItem(
        onClick = {
            if (isTopForum) {
                onDeleteTopForum()
            } else {
                onAddTopForum()
            }
            menuState.expanded = false
        }
    ) {
        if (isTopForum) {
            Text(text = stringResource(id = R.string.menu_top_del))
        } else {
            Text(text = stringResource(id = R.string.menu_top))
        }
    }
    DropdownMenuItem(
        onClick = {
            onCopyName()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.title_copy_forum_name))
    }
    DropdownMenuItem(
        onClick = {
            onUnfollow()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.button_unfollow))
    }
}

/**
 * 搜索区：搜索框 + 点开后的「最近搜索 / 猜你想搜」快捷面板。
 * visible=false 时整体收起（沉浸式滚动），只保留标题栏。
 */
@Composable
private fun HomeSearchArea(
    visible: Boolean,
    onOpenSearch: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            SearchBox(modifier = Modifier.padding(bottom = 4.dp), onClick = onOpenSearch)
        }
    }
}

/** 顶栏「更多选项」下拉：列表形态、排序方式、最近访问吧开关。 */
@Composable
private fun HomeMoreMenu(
    expanded: Boolean,
    listSingle: Boolean,
    sortType: Int,
    showHistoryForum: Boolean,
    onDismiss: () -> Unit,
    onToggleListSingle: () -> Unit,
    onSelectSort: (Int) -> Unit,
    onToggleHistoryForum: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(color = ExtendedTheme.colors.menuBackground)
    ) {
        DropdownMenuItem(
            onClick = {
                onToggleListSingle()
                onDismiss()
            }
        ) {
            Text(text = stringResource(id = R.string.title_list_single))
            Spacer(modifier = Modifier.weight(1f, fill = true))
            if (listSingle) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.primary
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.menu_sort_title),
                color = ExtendedTheme.colors.textSecondary,
                fontSize = 12.sp
            )
        }
        listOf(
            SORT_BY_HOT to R.string.menu_sort_by_hot,
            SORT_BY_NEW to R.string.menu_sort_by_new,
            SORT_BY_LEVEL to R.string.menu_sort_by_level,
            SORT_BY_NAME to R.string.menu_sort_by_name,
        ).forEach { (type, labelRes) ->
            DropdownMenuItem(
                onClick = {
                    onSelectSort(type)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = labelRes))
                Spacer(modifier = Modifier.weight(1f, fill = true))
                if (sortType == type) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = ExtendedTheme.colors.primary
                    )
                }
            }
        }
        DropdownMenuItem(
            onClick = {
                onToggleHistoryForum()
                onDismiss()
            }
        ) {
            Text(text = stringResource(id = R.string.menu_show_history_forum))
            Spacer(modifier = Modifier.weight(1f, fill = true))
            if (showHistoryForum) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun ForumAvatar(
    forum: HomeUiState.Forum,
    size: Dp,
) {
    val context = LocalContext.current
    val fallbackColor = remember(forum.forumId) {
        // 按吧 id 稳定地挑一个色相，缺头像时也不会一片灰
        val palette = listOf(
            Color(0xFF4C8DFF), Color(0xFF52C4A0), Color(0xFFFF9F43),
            Color(0xFFE56B8A), Color(0xFF7C6BF0), Color(0xFF3FB6D3),
            Color(0xFF9B8B5A), Color(0xFFBF6BD9)
        )
        palette[(forum.forumId.hashCode().let { if (it < 0) -it else it }) % palette.size]
    }
    val fallbackText = remember(forum.forumName) {
        forum.forumName.trim().take(1).ifEmpty { "吧" }
    }
    val avatarData = forum.avatar.trim()

    // 缺图 / 空串时不发请求，直接画首字占位
    val request = remember(avatarData) {
        if (avatarData.isEmpty()) null
        else DisplayRequest(context, avatarData) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color = fallbackColor.copy(alpha = 0.16f)),
        contentAlignment = Center
    ) {
        Text(
            text = fallbackText,
            color = fallbackColor,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold
        )
        if (request != null) {
            AsyncImage(
                request = request,
                contentDescription = stringResource(R.string.forum_portrait),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ForumItemContent(
    item: HomeUiState.Forum,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically
    ) {
        // 头像：始终显示（官方「进吧」列表带吧头像）
        ForumAvatar(forum = item, size = 40.dp)
        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // 贴吧名称
            Text(
                color = ExtendedTheme.colors.text,
                text = item.forumName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 热度值显示
            Text(
                text = stringResource(
                    R.string.hot_num,
                    item.hotNum.getShortNumString()
                ),
                color = ExtendedTheme.colors.onChip,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        // 第三列：等级
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // 等级显示
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .background(
                        color = ExtendedTheme.colors.chip,
                        shape = RoundedCornerShape(3.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.align(Center),
                ) {
                    Text(
                        text = "Lv.${item.levelId}",
                        color = ExtendedTheme.colors.onChip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(CenterVertically)
                    )
                    if (item.isSign) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(id = R.string.tip_signed),
                            modifier = Modifier
                                .size(12.dp)
                                .align(CenterVertically),
                            tint = ExtendedTheme.colors.onChip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumItem(
    item: HomeUiState.Forum,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onAddTopForum: (HomeUiState.Forum) -> Unit,
    onDeleteTopForum: (HomeUiState.Forum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            ForumItemMenuContent(
                menuState = menuState,
                isTopForum = isTopForum,
                onDeleteTopForum = { onDeleteTopForum(item) },
                onAddTopForum = { onAddTopForum(item) },
                onCopyName = {
                    TiebaUtil.copyText(context, item.forumName)
                },
                onUnfollow = { onUnfollow(item) }
            )
        },
        menuState = menuState,
        onClick = {
            onClick(item)
        }
    ) {
        ForumItemContent(item = item)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel(),
    canOpenExplore: Boolean = false,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::isLoading,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::forums,
        initial = persistentListOf()
    )
    val topForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::topForums,
        initial = persistentListOf()
    )
    val historyForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::historyForums,
        initial = persistentListOf()
    )
    val expandHistoryForum by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::expandHistoryForum,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::error,
        initial = null
    )
    val hasLoaded by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::hasLoaded,
        initial = false
    )
    val isLoggedIn = remember(account) { account != null }
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val showEmptyState by remember {
        derivedStateOf { isEmpty && (!isLoggedIn || hasLoaded) }
    }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }
    var listSingle by remember { mutableStateOf(context.appPreferences.listSingle) }
    var sortType by remember { mutableStateOf(context.appPreferences.homeSortType) }
    var showHistoryForumState by remember {
        mutableStateOf(context.appPreferences.homePageShowHistoryForum)
    }
    val showHistoryForum by remember(showHistoryForumState, historyForums) {
        derivedStateOf { showHistoryForumState && historyForums.isNotEmpty() }
    }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val isError by remember { derivedStateOf { error != null } }
    val gridCells by remember { derivedStateOf { getGridCells(context, listSingle) } }

    // 沉浸式滚动：下滑收起搜索框（只剩标题栏），上滑或回到顶部时恢复
    val gridState = rememberLazyGridState()
    var searchBarVisible by remember { mutableStateOf(true) }
    LaunchedEffect(gridState) {
        // 用「索引 + 偏移」估算绝对滚动量，避免换 item 时偏移归零造成误判
        var lastTotal = 0
        snapshotFlow {
            gridState.firstVisibleItemIndex * 1000 + gridState.firstVisibleItemScrollOffset
        }.collect { total ->
            val delta = total - lastTotal
            if (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset <= 8) {
                // 回到顶部一定展开
                searchBarVisible = true
            } else if (delta > 8) {
                searchBarVisible = false
            } else if (delta < -8) {
                searchBarVisible = true
            }
            lastTotal = total
        }
    }

    // 排序后的列表（置顶吧始终排在最前）
    val sortedForums by remember(forums, sortType) {
        derivedStateOf { forums.sortedWith(forumComparator(sortType)) }
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "explore" }
    ) {
        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
    }

    var unfollowForum by remember { mutableStateOf<HomeUiState.Forum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = {
            unfollowForum?.let {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            }
        },
    ) {
        Text(
            text = stringResource(
                id = R.string.title_dialog_unfollow_forum,
                unfollowForum?.forumName.orEmpty()
            )
        )
    }

    LaunchedEffect(Unit) {
        if (isLoggedIn && !viewModel.initialized) {
            viewModel.send(HomeUiIntent.RefreshHistory)
            viewModel.send(HomeUiIntent.Refresh)
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_explore),
                navigationIcon = toolboxAccountNavIconIfCompact(navigator),
                actions = {
                    // 安全模式下隐藏「一键签到」入口（签到属写操作，会被网络层拦截）
                    if (!context.appPreferences.safeMode) {
                        ActionItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                            contentDescription = stringResource(id = R.string.title_oksign)
                        ) {
                            TiebaUtil.startSign(context)
                        }
                    }
                    // 一个按钮收纳：单列/双列、排序方式、最近访问吧开关
                    Box {
                        ActionItem(
                            icon = Icons.Outlined.ViewAgenda,
                            contentDescription = stringResource(id = R.string.title_more_options)
                        ) {
                            moreMenuExpanded = true
                        }
                        HomeMoreMenu(
                            expanded = moreMenuExpanded,
                            listSingle = listSingle,
                            sortType = sortType,
                            showHistoryForum = showHistoryForumState,
                            onDismiss = { moreMenuExpanded = false },
                            onToggleListSingle = {
                                context.appPreferences.listSingle = !listSingle
                                listSingle = !listSingle
                            },
                            onSelectSort = { type ->
                                context.appPreferences.homeSortType = type
                                sortType = type
                            },
                            onToggleHistoryForum = {
                                context.appPreferences.homePageShowHistoryForum =
                                    !context.appPreferences.homePageShowHistoryForum
                                showHistoryForumState =
                                    context.appPreferences.homePageShowHistoryForum
                            }
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { contentPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isLoading,
            onRefresh = { if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh) }
        )
        Box(
            modifier = Modifier
                .pullRefresh(pullRefreshState)
                .padding(contentPaddings)
        ) {
            Column {
                HomeSearchArea(
                    visible = searchBarVisible,
                    onOpenSearch = { navigator.navigate(SearchPageDestination) }
                )
                StateScreen(
                    isEmpty = showEmptyState,
                    isError = isError,
                    isLoading = isLoading,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onReload = {
                        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
                    },
                    emptyScreen = {
                        EmptyScreen(
                            loggedIn = isLoggedIn,
                            canOpenExplore = canOpenExplore,
                            onOpenExplore = onOpenExplore
                        )
                    },
                    loadingScreen = {
                        HomePageSkeletonScreen(listSingle = listSingle, gridCells = gridCells)
                    },
                    errorScreen = {
                        error?.let { ErrorScreen(error = it) }
                    }
                ) {
                    MyLazyVerticalGrid(
                        columns = gridCells,
                        contentPadding = PaddingValues(bottom = 12.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (showHistoryForum) {
                            item(key = "HistoryForums", span = { GridItemSpan(maxLineSpan) }) {
                                val rotate by animateFloatAsState(
                                    targetValue = if (expandHistoryForum) 90f else 0f,
                                    label = "rotate"
                                )
                                Column {
                                    Row(
                                        verticalAlignment = CenterVertically,
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                viewModel.send(
                                                    HomeUiIntent.ToggleHistory(
                                                        expandHistoryForum
                                                    )
                                                )
                                            }
                                            .padding(vertical = 8.dp)
                                            .padding(end = 16.dp)
                                    ) {
                                        Header(
                                            text = stringResource(id = R.string.title_history_forum),
                                            invert = false
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = stringResource(id = R.string.desc_show),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .rotate(rotate)
                                        )
                                    }
                                    AnimatedVisibility(visible = expandHistoryForum) {
                                        LazyRow(
                                            contentPadding = PaddingValues(bottom = 8.dp),
                                        ) {
                                            item(key = "Spacer1") {
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            items(
                                                historyForums,
                                                key = { it.data },
                                                contentType = { "history" }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .height(IntrinsicSize.Min)
                                                        .clip(RoundedCornerShape(100))
                                                        .background(color = ExtendedTheme.colors.chip)
                                                        .debounceClickable(onClick = {
                                                            navigator.navigate(
                                                                ForumPageDestination(
                                                                    it.data
                                                                )
                                                            )
                                                        })
                                                        .padding(4.dp),
                                                    verticalAlignment = CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Avatar(
                                                        data = it.avatar,
                                                        contentDescription = null,
                                                        size = 24.dp,
                                                        shape = CircleShape
                                                    )
                                                    Text(
                                                        text = it.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                }
                                            }
                                            item(key = "Spacer2") {
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (hasTopForum) {
                            item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Header(
                                        text = stringResource(id = R.string.title_top_forum),
                                        invert = true
                                    )
                                }
                            }
                            items(
                                items = topForums,
                                key = { "Top${it.forumId}" },
                                contentType = { "topForum" }
                            ) { item ->
                                ForumItem(
                                    item,
                                    onClick = {
                                        navigator.navigate(ForumPageDestination(it.forumName))
                                    },
                                    onUnfollow = {
                                        unfollowForum = it
                                        confirmUnfollowDialog.show()
                                    },
                                    onAddTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Add(it))
                                    },
                                    onDeleteTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                    },
                                    isTopForum = true
                                )
                            }
                        }
                        if (showHistoryForum || hasTopForum) {
                            item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Header(text = stringResource(id = R.string.forum_list_title))
                                }
                            }
                        }
                        items(
                            items = sortedForums,
                            key = { it.forumId },
                            contentType = { "forum" }
                        ) { item ->
                            ForumItem(
                                item,
                                onClick = {
                                    navigator.navigate(ForumPageDestination(it.forumName))
                                },
                                onUnfollow = {
                                    unfollowForum = it
                                    confirmUnfollowDialog.show()
                                },
                                onAddTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Add(it))
                                },
                                onDeleteTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                }
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun HomePageSkeletonScreen(
    listSingle: Boolean,
    gridCells: GridCells
) {
    MyLazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 12.dp),
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item(key = "TopForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Header(
                    text = stringResource(id = R.string.title_top_forum),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip
                    ),
                    invert = true
                )
            }
        }
        items(6, key = { "TopPlaceholder$it" }) {
            ForumItemPlaceholder()
        }
        item(
            key = "Spacer",
            span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(
                    16.dp
                )
            )
        }
        item(key = "ForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(
                    text = stringResource(id = R.string.forum_list_title),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip
                    ),
                    invert = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(12, key = { "Placeholder$it" }) {
            ForumItemPlaceholder()
        }
    }
}

@Composable
fun EmptyScreen(
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavigator.current
    TipScreen(
        title = {
            if (!loggedIn) {
                Text(text = stringResource(id = R.string.title_empty_login))
            } else {
                Text(text = stringResource(id = R.string.title_empty))
            }
        },
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = MaterialTheme.typography.body1,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (!loggedIn) {
                Button(
                    onClick = {
                        navigator.navigate(LoginPageDestination)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                TextButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}