package com.huanchengfly.tieba.post.ui.page.searchuser

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.FansListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.FollowListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserLikeForumListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserPostListPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 用户 ID：5~12 位纯数字。
 *
 * 注意：查人接口认的是「用户 ID」(user_id)，它的位数并不固定 ——
 * 老用户可能只有 7 位（如 5089363），新用户则长达 10 位。
 * 早先写死 8~10 位会把合法的 7 位 ID 直接挡掉，因此放宽到 5~12 位。
 */
private val UID_REGEX = Regex("^\\d{5,12}$")

/** 从分享文案中提取 UID，如「#123456789#」。 */
private val SHARE_UID_REGEX = Regex("#(\\d{5,12})#")

/** 从任意文本中宽松提取一串 5~12 位数字，作为兜底。 */
private val LOOSE_UID_REGEX = Regex("(\\d{5,12})")

/**
 * 尽力从一段文本里解析出贴吧 UID。
 *
 * 优先识别分享文案里的 `#数字#`；若整段就是纯数字则直接采用；最后宽松匹配长数字串。
 */
private fun extractUid(raw: String?): Long? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    if (UID_REGEX.matches(text)) return text.toLongOrNull()
    SHARE_UID_REGEX.find(text)?.groupValues?.getOrNull(1)?.let {
        return it.toLongOrNull()
    }
    return LOOSE_UID_REGEX.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
}

private fun Context.readClipboardText(): String? {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val item = manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
    return item.coerceToText(this)?.toString()
}

/**
 * 「查人」页。
 *
 * 输入 8~10 位贴吧 UID（或直接粘贴贴吧分享文案，`#数字#` 会被自动提取）即可查询吧友资料，
 * 并在资料卡下方提供主题帖 / 回帖 / 关注 / 粉丝 / 关注的吧等快捷入口。
 *
 * 注意：[ProvideNavigator] 必须包裹页面内容 —— 下方入口列表会读取 [LocalNavigator]。
 */
@Destination
@Composable
fun SearchUserPage(
    navigator: DestinationsNavigator,
    viewModel: SearchUserViewModel = pageViewModel(),
) {
    ProvideNavigator(navigator = navigator) {
        MyScaffold(
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_search_user),
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
                )
            },
        ) { contentPaddings ->
            SearchUserContent(
                viewModel = viewModel,
                contentPadding = contentPaddings,
            )
        }
    }
}

/**
 * 「查人」Tab 的嵌入式版本。
 *
 * 用于主页 [com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage] 的 Tab 排 —— 不需要
 * 自己的 TopBar 与返回键（主页已经提供了外层 Toolbar）。
 *
 * 注意：该 Composable 会被放在主页的 `ProvideNavigator` 作用域之内，因此可以直接读取
 * [LocalNavigator]；若被放到其它上下文，调用方需保证 [ProvideNavigator] 已提供。
 */
@Composable
fun SearchUserTabPage() {
    val viewModel: SearchUserViewModel = pageViewModel()
    SearchUserContent(
        viewModel = viewModel,
        contentPadding = PaddingValues(0.dp),
    )
}

@Composable
private fun SearchUserContent(
    viewModel: SearchUserViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val loggedIn = AccountUtil.LocalAccount.current != null

    val isSearching by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::isSearching,
        initial = false,
    )
    val hasSearched by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::hasSearched,
        initial = false,
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::user,
        initial = null,
    )
    val errorMessage by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::errorMessage,
        initial = null,
    )

    var uidInput by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val emptyHint = stringResource(id = R.string.error_search_user_uid_empty)
    val invalidHint = stringResource(id = R.string.error_search_user_uid_invalid)
    val pasteEmptyHint = stringResource(id = R.string.text_search_user_paste_empty)
    val pasteDoneHint = stringResource(id = R.string.text_search_user_paste_done)
    val pasteFailedHint = stringResource(id = R.string.text_search_user_paste_failed)
    val notFoundHint = stringResource(id = R.string.error_search_user_not_found)

    fun submit(raw: String = uidInput) {
        val text = raw.trim()
        if (text.isEmpty()) {
            localError = emptyHint
            return
        }
        if (!UID_REGEX.matches(text)) {
            localError = invalidHint
            return
        }
        localError = null
        viewModel.send(SearchUserUiIntent.Search(text))
    }

    fun pasteFromClipboard() {
        val clipped = context.readClipboardText()
        if (clipped.isNullOrBlank()) {
            context.toastShort(pasteEmptyHint)
            return
        }
        val uid = extractUid(clipped)
        if (uid == null) {
            context.toastShort(pasteFailedHint)
        } else {
            uidInput = uid.toString()
            localError = null
            context.toastShort(pasteDoneHint)
            viewModel.send(SearchUserUiIntent.Search(uid.toString()))
        }
    }

    // 注意：quickEntries 是 @Composable，必须在 LazyColumn 的无可组合上下文中先取出来，
    // 直接写在 LazyListScope lambda 里会报「@Composable invocations can only happen ...」。
    val currentUser = user?.getOrNull()
    val entries = if (currentUser != null) {
        quickEntries(
            context = context,
            navigator = navigator,
            user = currentUser,
            loggedIn = loggedIn,
        )
    } else {
        emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            UidInputCard(
                value = uidInput,
                onValueChange = {
                    uidInput = it.filter { c -> c.isDigit() }.take(10)
                    localError = null
                },
                isSearching = isSearching,
                errorText = localError,
                onQuery = { submit() },
                onClear = {
                    uidInput = ""
                    localError = null
                },
                onPaste = { pasteFromClipboard() },
            )
        }

        item {
            SearchUserResultArea(
                isSearching = isSearching,
                hasSearched = hasSearched,
                user = currentUser,
                errorMessage = errorMessage,
                notFoundHint = notFoundHint,
            )
        }

        if (entries.isNotEmpty()) {
            item { SearchUserGroupTitle(stringResource(id = R.string.title_search_user_quick)) }
            items(items = entries) { entry -> SearchUserEntryRow(entry = entry) }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  输入区                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun UidInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    isSearching: Boolean,
    errorText: String?,
    onQuery: () -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.title_search_user_id_label)) },
            placeholder = { Text(text = stringResource(id = R.string.hint_search_user_uid)) },
            singleLine = true,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { onQuery() }),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.PersonSearch,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textSecondary,
                )
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.action_search_user_clear),
                            tint = ExtendedTheme.colors.textSecondary,
                        )
                    }
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onPaste, enabled = !isSearching) {
                Text(text = stringResource(id = R.string.action_search_user_paste))
            }

            SearchUserPrimaryButton(
                text = stringResource(id = R.string.action_search_user_query),
                icon = Icons.Outlined.PersonSearch,
                enabled = !isSearching,
                onClick = onQuery,
                modifier = Modifier.weight(1f),
            )
        }

        val hintText = errorText
        if (hintText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = hintText,
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.accent,
                )
            }
        } else {
            Text(
                text = stringResource(id = R.string.summary_search_user_uid_hint),
                fontSize = 12.sp,
                color = ExtendedTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SearchUserPrimaryButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (enabled) ExtendedTheme.colors.primary else ExtendedTheme.colors.chip
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ExtendedTheme.colors.onPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.button,
            color = ExtendedTheme.colors.onPrimary,
            maxLines = 1,
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                                  结果区                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun SearchUserResultArea(
    isSearching: Boolean,
    hasSearched: Boolean,
    user: User?,
    errorMessage: String?,
    notFoundHint: String,
) {
    when {
        isSearching && user == null -> {
            StatusCard {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = ExtendedTheme.colors.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.desc_search_user) + "…",
                    fontSize = 13.sp,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
        }

        errorMessage != null && user == null -> {
            StatusCard {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.accent,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        user != null -> {
            SearchUserProfileCard(user = user)
        }

        hasSearched -> {
            StatusCard {
                Icon(
                    imageVector = Icons.Outlined.PersonSearch,
                    contentDescription = null,
                    tint = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notFoundHint,
                    fontSize = 13.sp,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun SearchUserProfileCard(
    user: User,
    modifier: Modifier = Modifier,
) {
    val name = user.nameShow.takeUnless { it.isEmpty() }
        ?: user.name.takeUnless { it.isEmpty() }
        ?: user.name
    val portrait = user.portrait.takeUnless { it.isEmpty() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(56.dp)) {
                if (portrait.isNullOrEmpty()) {
                    AvatarPlaceholder(size = 56.dp)
                } else {
                    Avatar(
                        data = StringUtil.getAvatarUrl(portrait),
                        size = 56.dp,
                        contentDescription = stringResource(id = R.string.user_portrait),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = ExtendedTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subText = buildString {
                    user.tieba_uid.takeUnless { it.isEmpty() }?.let {
                        append(stringResource(id = R.string.text_profile_user_id, it))
                    }
                    user.tb_age.takeUnless { it.isEmpty() }?.let {
                        if (isNotEmpty()) append("  ·  ")
                        append(stringResource(id = R.string.text_profile_tb_age, it))
                    }
                }
                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        fontSize = 12.sp,
                        color = ExtendedTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (user.intro.isNotEmpty()) {
            Text(
                text = user.intro,
                fontSize = 13.sp,
                color = ExtendedTheme.colors.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SearchUserStat(
                value = shortNum(user.concern_num),
                label = stringResource(id = R.string.text_stat_follow),
            )
            SearchUserStat(
                value = shortNum(user.fans_num),
                label = stringResource(id = R.string.text_stat_fans),
            )
            SearchUserStat(
                value = shortNum(user.thread_num),
                label = stringResource(id = R.string.text_toolbox_stat_threads),
            )
            SearchUserStat(
                value = shortNum(user.post_num),
                label = stringResource(id = R.string.text_toolbox_stat_posts),
            )
            SearchUserStat(
                value = shortNum(user.total_agree_num),
                label = stringResource(id = R.string.text_stat_agrees),
            )
        }
    }
}

@Composable
private fun SearchUserStat(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = ExtendedTheme.colors.textSecondary,
        )
    }
}

/* -------------------------------------------------------------------------- */
/*                                  入口列表                                    */
/* -------------------------------------------------------------------------- */

private class SearchUserEntry(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val onClick: () -> Unit,
)

@Composable
private fun SearchUserGroupTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = ExtendedTheme.colors.textSecondary,
    )
}

@Composable
private fun SearchUserEntryRow(
    entry: SearchUserEntry,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = ExtendedTheme.colors.card)
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = entry.title,
            tint = ExtendedTheme.colors.primary,
            modifier = Modifier.size(Sizes.Tiny),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                color = ExtendedTheme.colors.text,
                maxLines = 1,
            )
            Text(
                text = entry.summary,
                fontSize = 12.sp,
                color = ExtendedTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = ExtendedTheme.colors.textDisabled,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 构建资料卡下方的快捷入口。
 *
 * 关注 / 粉丝列表接口需要登录，未登录时统一跳转登录页；主题帖 / 回帖 / 关注的吧
 * 无需登录即可查看他人数据。
 */
@Composable
private fun quickEntries(
    context: Context,
    navigator: DestinationsNavigator,
    user: User,
    loggedIn: Boolean,
): List<SearchUserEntry> {
    val uid = user.id
    // 回帖数据源（eztb）不返回发帖人昵称/头像；被查看的这一位就是发帖人，回填给列表头部。
    val authorName = user.name
    val authorNameShow = user.nameShow
    val authorPortrait = user.portrait
    return remember(uid, loggedIn, authorName, authorNameShow, authorPortrait) {
        val requireLogin: (() -> Unit) -> Unit = { action ->
            if (loggedIn) {
                action()
            } else {
                context.toastShort(R.string.toast_toolbox_need_login)
                navigator.navigate(LoginPageDestination)
            }
        }

        listOf(
            SearchUserEntry(
                icon = Icons.Outlined.Article,
                title = context.getString(R.string.title_search_user_threads),
                summary = context.getString(R.string.summary_search_user_threads),
                onClick = {
                    navigator.navigate(UserPostListPageDestination(uid = uid, isThread = true))
                },
            ),
            SearchUserEntry(
                icon = Icons.Outlined.Reply,
                title = context.getString(R.string.title_search_user_posts),
                summary = context.getString(R.string.summary_search_user_posts),
                onClick = {
                    navigator.navigate(
                        UserPostListPageDestination(
                            uid = uid,
                            isThread = false,
                            authorName = authorName,
                            authorNameShow = authorNameShow,
                            authorPortrait = authorPortrait,
                        )
                    )
                },
            ),
            SearchUserEntry(
                icon = Icons.Outlined.Groups,
                title = context.getString(R.string.title_search_user_follow),
                summary = context.getString(R.string.summary_search_user_follow),
                onClick = { requireLogin { navigator.navigate(FollowListPageDestination(uid)) } },
            ),
            SearchUserEntry(
                icon = Icons.Outlined.Person,
                title = context.getString(R.string.title_search_user_fans),
                summary = context.getString(R.string.summary_search_user_fans),
                onClick = { requireLogin { navigator.navigate(FansListPageDestination(uid)) } },
            ),
            SearchUserEntry(
                icon = Icons.Outlined.Forum,
                title = context.getString(R.string.title_search_user_like_forum),
                summary = context.getString(R.string.summary_search_user_like_forum),
                onClick = { navigator.navigate(UserLikeForumListPageDestination(uid)) },
            ),
        )
    }
}

/**
 * 数字缩写（1.2W / 3.4KW）。
 *
 * [StringUtil.getShortNumString] 是 `object StringUtil` 的成员扩展函数，且未标注
 * `@JvmStatic`，无法作为顶层扩展导入使用，因此在此本地包装一层。
 */
private fun shortNum(value: Int): String = with(StringUtil) { value.getShortNumString() }
