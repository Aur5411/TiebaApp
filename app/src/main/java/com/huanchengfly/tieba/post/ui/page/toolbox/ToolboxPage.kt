package com.huanchengfly.tieba.post.ui.page.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.FansListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.FollowListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HistoryPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadStorePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserLikeForumListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserPostListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 「我的贴吧」聚合页。
 *
 * 从主页左上角头像进入，聚合了个人资料卡与常用功能入口：
 * 社交（粉丝 / 关注）、我的内容（发帖 / 回帖 / 收藏 / 浏览记录）、
 * 贴吧（关注的吧 / 一键签到）、应用（设置 / 账号管理）。
 */
@Destination
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ToolboxPage(
    navigator: DestinationsNavigator,
    viewModel: ToolboxViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val account = AccountUtil.LocalAccount.current
    val selfUid = remember(account) { account?.uid?.toLongOrNull() ?: 0L }
    val isLoggedIn = selfUid != 0L

    if (isLoggedIn) {
        LazyLoad(loaded = viewModel.initialized) {
            viewModel.send(ToolboxUiIntent.Refresh(selfUid))
            viewModel.initialized = true
        }
    }

    val user by viewModel.uiState.collectPartialAsState(
        prop1 = ToolboxUiState::user,
        initial = null
    )

    fun requireLogin(action: () -> Unit) {
        if (!isLoggedIn) {
            context.toastShort(R.string.toast_toolbox_need_login)
            navigator.navigate(LoginPageDestination)
        } else {
            action()
        }
    }

    val socialEntries = listOf(
        ToolboxEntry(
            icon = Icons.Outlined.Groups,
            title = stringResource(id = R.string.title_toolbox_fans),
            summary = stringResource(id = R.string.summary_toolbox_fans),
        ) {
            requireLogin { navigator.navigate(FansListPageDestination(selfUid)) }
        },
        ToolboxEntry(
            icon = Icons.Outlined.Person,
            title = stringResource(id = R.string.title_toolbox_follow),
            summary = stringResource(id = R.string.summary_toolbox_follow),
        ) {
            requireLogin { navigator.navigate(FollowListPageDestination(selfUid)) }
        },
    )

    val contentEntries = listOf(
        ToolboxEntry(
            icon = Icons.Outlined.Article,
            title = stringResource(id = R.string.title_toolbox_threads),
            summary = stringResource(id = R.string.summary_toolbox_threads),
        ) {
            requireLogin {
                navigator.navigate(UserPostListPageDestination(uid = selfUid, isThread = true))
            }
        },
        ToolboxEntry(
            icon = Icons.Outlined.Reply,
            title = stringResource(id = R.string.title_toolbox_posts),
            summary = stringResource(id = R.string.summary_toolbox_posts),
        ) {
            requireLogin {
                navigator.navigate(UserPostListPageDestination(uid = selfUid, isThread = false))
            }
        },
        ToolboxEntry(
            icon = Icons.Outlined.CollectionsBookmark,
            title = stringResource(id = R.string.title_toolbox_collect),
            summary = stringResource(id = R.string.summary_toolbox_collect),
        ) {
            requireLogin { navigator.navigate(ThreadStorePageDestination) }
        },
        ToolboxEntry(
            icon = Icons.Outlined.History,
            title = stringResource(id = R.string.title_toolbox_history),
            summary = stringResource(id = R.string.summary_toolbox_history),
        ) {
            navigator.navigate(HistoryPageDestination)
        },
    )

    val forumEntries = listOf(
        ToolboxEntry(
            icon = Icons.Outlined.Forum,
            title = stringResource(id = R.string.title_toolbox_like_forum),
            summary = stringResource(id = R.string.summary_toolbox_like_forum),
        ) {
            requireLogin { navigator.navigate(UserLikeForumListPageDestination(selfUid)) }
        },
        ToolboxEntry(
            icon = Icons.Rounded.CheckCircle,
            title = stringResource(id = R.string.title_toolbox_oksign),
            summary = stringResource(id = R.string.summary_toolbox_oksign),
        ) {
            requireLogin { TiebaUtil.startSign(context) }
        },
    )

    val appEntries = listOf(
        ToolboxEntry(
            icon = Icons.Outlined.Settings,
            title = stringResource(id = R.string.title_toolbox_settings),
            summary = stringResource(id = R.string.summary_toolbox_settings),
        ) {
            navigator.navigate(SettingsPageDestination)
        },
        ToolboxEntry(
            icon = Icons.Outlined.ManageAccounts,
            title = stringResource(id = R.string.title_toolbox_account),
            summary = stringResource(id = R.string.summary_toolbox_account),
        ) {
            navigator.navigate(AccountManagePageDestination)
        },
    )

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_toolbox_center),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ToolboxProfileCard(
                    user = user?.getOrNull(),
                    fallbackAccount = account,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    onClick = {
                        requireLogin {
                            navigator.navigate(UserProfilePageDestination(selfUid))
                        }
                    },
                )
            }

            item { ToolboxGroupTitle(stringResource(id = R.string.title_toolbox_group_social)) }
            items(items = socialEntries) { ToolboxEntryRow(entry = it) }

            item { ToolboxGroupTitle(stringResource(id = R.string.title_toolbox_group_content)) }
            items(items = contentEntries) { ToolboxEntryRow(entry = it) }

            item { ToolboxGroupTitle(stringResource(id = R.string.title_toolbox_group_forum)) }
            items(items = forumEntries) { ToolboxEntryRow(entry = it) }

            item { ToolboxGroupTitle(stringResource(id = R.string.title_toolbox_group_app)) }
            items(items = appEntries) { ToolboxEntryRow(entry = it) }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private class ToolboxEntry(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val onClick: () -> Unit,
)

@Composable
private fun ToolboxGroupTitle(
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

/**
 * 顶部资料卡：头像 + 昵称 + ID + 统计数据（关注 / 粉丝 / 获赞）。
 * 未登录时展示提示并引导登录。
 */
@Composable
private fun ToolboxProfileCard(
    user: com.huanchengfly.tieba.post.api.models.protos.User?,
    fallbackAccount: com.huanchengfly.tieba.post.models.database.Account?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val name = user?.nameShow?.takeUnless { it.isEmpty() }
        ?: user?.name?.takeUnless { it.isEmpty() }
        ?: fallbackAccount?.nameShow?.takeUnless { it.isNullOrEmpty() }
        ?: fallbackAccount?.name
        ?: stringResource(id = R.string.title_toolbox_guest)

    val portrait = user?.portrait?.takeUnless { it.isEmpty() } ?: fallbackAccount?.portrait
    val summary = if (user == null) {
        stringResource(id = R.string.summary_toolbox_guest)
    } else {
        buildString {
            user.tieba_uid.takeUnless { it.isEmpty() }?.let {
                append(stringResource(id = R.string.text_profile_user_id, it))
            }
            user.tb_age.takeUnless { it.isEmpty() }?.let {
                if (isNotEmpty()) append("  ·  ")
                append(stringResource(id = R.string.text_profile_tb_age, it))
            }
        }.ifEmpty { stringResource(id = R.string.summary_toolbox_guest) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color = ExtendedTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = ExtendedTheme.colors.textDisabled,
                modifier = Modifier.size(Sizes.Small),
            )
        }

        if (user != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolboxStat(
                    value = shortNum(user.concern_num),
                    label = stringResource(id = R.string.text_stat_follow),
                )
                ToolboxStat(
                    value = shortNum(user.fans_num),
                    label = stringResource(id = R.string.text_stat_fans),
                )
                ToolboxStat(
                    value = shortNum(user.thread_num),
                    label = stringResource(id = R.string.text_toolbox_stat_threads),
                )
                ToolboxStat(
                    value = shortNum(user.total_agree_num),
                    label = stringResource(id = R.string.text_stat_agrees),
                )
            }
        }
    }
}

@Composable
private fun ToolboxStat(
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

@Composable
private fun ToolboxEntryRow(
    entry: ToolboxEntry,
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
            modifier = Modifier.size(Sizes.Tiny)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                color = ExtendedTheme.colors.text,
                maxLines = 1
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
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 数字缩写（1.2W / 3.4KW）。
 *
 * [StringUtil.getShortNumString] 是 `object StringUtil` 的成员扩展函数，
 * 且未标注 `@JvmStatic`，无法作为顶层扩展导入使用，因此在此本地包装一层。
 */
private fun shortNum(value: Int): String = with(StringUtil) { value.getShortNumString() }
