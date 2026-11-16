package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

@Destination
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    val dialogState = rememberDialogState()

    PromptDialog(
        onConfirm = {
            val input = it.trim()
            if (input.isNotEmpty()) {
                val asUid = input.toLongOrNull()
                viewModel.send(
                    BlockListUiIntent.Add(
                        category = Block.CATEGORY_BLACK_LIST,
                        type = Block.TYPE_USER,
                        userName = if (asUid == null) input else null,
                        userId = asUid
                    )
                )
            }
        },
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_add_black)) }
    ) {
        Column {
            Text(text = stringResource(id = R.string.tip_add_black))
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
    val items = remember(blackList) { blackList.filter { it.type == Block.TYPE_USER } }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_black_list),
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
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.title_add_black),
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
                        BlockUserItemPlaceholder()
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
                            clipboardManager.setText(AnnotatedString(item.username.orEmpty()))
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
                        BlockUserItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockUserItemPlaceholder() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_block_settings),
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
    }
}

@Composable
private fun BlockUserItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = stringResource(id = R.string.block_type_user),
            tint = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.username.orEmpty(),
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "UID: ${item.uid}",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
