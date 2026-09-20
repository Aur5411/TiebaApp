package com.huanchengfly.tieba.post.ui.page.user.likeforum

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
 * 独立的「关注的吧」列表页。
 *
 * 内容复用 [UserLikeForumPage]，该组件内部通过 `LocalNavigator.current`
 * 获取导航器，因此这里必须用 [ProvideNavigator] 包裹，否则会抛
 * 「No navigator is available」崩溃。
 */
@Destination
@Composable
fun UserLikeForumListPage(
    uid: Long = 0,
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        MyScaffold(
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_toolbox_like_forum),
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
                UserLikeForumPage(
                    uid = uid,
                    enablePullRefresh = true
                )
            }
        }
    }
}
