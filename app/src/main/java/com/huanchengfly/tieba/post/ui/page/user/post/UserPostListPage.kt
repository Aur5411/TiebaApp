package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 独立的「我的发帖 / 我的回帖」列表页。
 *
 * 内容复用 [UserPostPage]，`isThread = true` 为主题帖，`false` 为回帖。
 */
@Destination
@Composable
fun UserPostListPage(
    uid: Long = 0,
    isThread: Boolean = true,
    authorName: String = "",
    authorNameShow: String = "",
    authorPortrait: String = "",
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        MyScaffold(
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(
                                id = if (isThread) {
                                    R.string.title_toolbox_threads
                                } else {
                                    R.string.title_toolbox_posts
                                }
                            ),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPaddings)
            ) {
                UserPostPage(
                    uid = uid,
                    isThread = isThread,
                    enablePullRefresh = true,
                    authorName = authorName,
                    authorNameShow = authorNameShow,
                    authorPortrait = authorPortrait
                )
            }
        }
    }
}
