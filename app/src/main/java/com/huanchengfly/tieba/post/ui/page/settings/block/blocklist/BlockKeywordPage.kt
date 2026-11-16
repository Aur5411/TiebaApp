package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import com.huanchengfly.tieba.post.ui.page.main.BottomNavigationDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 屏蔽词管理：独立页面，与「黑名单」分开。
 *
 * 添加的词组只作用于评论（2 楼及以后的楼层、楼中楼），命中后该条评论直接不渲染；
 * 帖子本身（1 楼正文、帖子列表标题/摘要）不受屏蔽词影响。
 */
@Destination
@Composable
fun BlockKeywordPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    val dialogState = rememberDialogState()
    var isRegex by remember { mutableStateOf(false) } // 记录是否为正则

    PromptDialog(
        onConfirm = {
            viewModel.send(
                BlockListUiIntent.Add(
                    category = Block.CATEGORY_BLACK_LIST,
                    type = Block.TYPE_KEYWORD,
                    keywords = if (isRegex) listOf(it) else it.split(" "), // 避免正则中的空格导致正则失效
                    isRegex = isRegex
                )
            )
            isRegex = false // 重置
        },
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_add_keyword)) }
    ) {
        Column {
            Text(text = stringResource(id = R.string.tip_add_keyword))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isRegex,
                    onCheckedChange = { isRegex = it }
                )
                Text(text = "作为正则表达式")
            }
        }
    }

    val context = LocalContext.current
    val blackList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::blackList,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::isLoading,
        initial = false
    )
    val items = remember(blackList) { blackList.filter { it.type == Block.TYPE_KEYWORD } }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_keyword_list_manage),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
            )
        },
        bottomBar = {
            Column {
                BottomNavigationDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { dialogState.show() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.title_add_keyword),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val snackBarHostState = LocalSnackbarHostState.current
        viewModel.onEvent<BlockListUiEvent.Success> {
            snackBarHostState.showSnackbar(
                when (it) {
                    is BlockListUiEvent.Success.Add -> context.getString(R.string.toast_add_success)
                    is BlockListUiEvent.Success.Delete -> context.getString(R.string.toast_delete_success)
                }
            )
        }
        StateScreen(
            isEmpty = items.isEmpty(),
            isError = false,
            isLoading = isLoading,
            loadingScreen = {
                MyLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(4) {
                        BlockKeywordItemPlaceholder()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val clipboardManager = LocalClipboardManager.current
            MyLazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    LongClickMenu(menuContent = {
                        DropdownMenuItem(onClick = {
                            val copied = keywordsOf(item).let { list ->
                                if (item.isRegex) list.joinToString(separator = "")
                                else list.joinToString(separator = " ")
                            }
                            clipboardManager.setText(AnnotatedString(copied))
                            Toast.makeText(context, R.string.toast_copy_success, Toast.LENGTH_SHORT).show()
                        }) {
                            Text(text = stringResource(id = R.string.title_copy))
                        }
                        DropdownMenuItem(onClick = {
                            viewModel.send(BlockListUiIntent.Delete(item.id))
                        }) {
                            Text(text = stringResource(id = R.string.title_delete))
                        }
                    }) {
                        BlockKeywordItem(item = item)
                    }
                }
            }
        }
    }
}

private fun keywordsOf(item: Block): List<String> = item.getKeywords()

@Composable
private fun BlockKeywordItemPlaceholder() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.TextFields,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_keyword_list),
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockKeywordItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
            contentDescription = stringResource(id = R.string.block_type_keywords),
            tint = if (item.isRegex) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = keywordsOf(item).joinToString(" "),
                    style = MaterialTheme.typography.subtitle1,
                    color = if (item.isRegex) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.onSurface,
                    maxLines = Int.MAX_VALUE, // 支持多行
                    overflow = TextOverflow.Visible
                )
                if (item.isRegex) {
                    Text(
                        text = "正则",
                        color = MaterialTheme.colors.secondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.secondary.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
