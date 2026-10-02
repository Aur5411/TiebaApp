package com.huanchengfly.tieba.post.ui.page.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 「TA 的发言分析」页。
 *
 * 顶部搜索框即 eztb 的「发言搜索」：分析时已经把发言列表拉全了，
 * 这里直接本地过滤，不必为搜索再发一次请求。
 * 清空关键词即回到统计视图（按月/时段/贴吧/IP/词云），底部可导出 JSON 或 CSV。
 */
@Destination
@Composable
fun PostAnalysisPage(
    uid: Long,
    navigator: DestinationsNavigator,
    authorName: String = "",
    authorNameShow: String = "",
    authorPortrait: String = "",
    pages: Int = 5,
    viewModel: PostAnalysisViewModel = pageViewModel(),
) {
    var keyword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uid, pages) {
        viewModel.send(
            PostAnalysisUiIntent.Analyse(
                uid = uid,
                pages = pages,
                authorName = authorName,
                authorNameShow = authorNameShow,
                authorPortrait = authorPortrait,
            )
        )
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = PostAnalysisUiState::isLoading,
        initial = false,
    )
    val errorText by viewModel.uiState.collectPartialAsState(
        prop1 = PostAnalysisUiState::error,
        initial = null,
    )
    val result by viewModel.uiState.collectPartialAsState(
        prop1 = PostAnalysisUiState::result,
        initial = null,
    )
    val posts by viewModel.uiState.collectPartialAsState(
        prop1 = PostAnalysisUiState::posts,
        initial = emptyList<PostRecord>(),
    )

    val matched = remember(keyword, posts) {
        val kw = keyword.trim()
        if (kw.isEmpty()) {
            emptyList()
        } else {
            posts.filter { it.searchableText().contains(kw, ignoreCase = true) }
        }
    }

    ProvideNavigator(navigator = navigator) {
        MyScaffold(
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_post_analysis),
                            style = MaterialTheme.typography.h6,
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    },
                )
            },
        ) { contentPaddings ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPaddings),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = true,
                        label = { Text(text = stringResource(id = R.string.hint_analysis_keyword)) },
                    )
                }

                if (keyword.isNotBlank()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.label_analysis_hit, matched.size),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.text,
                        )
                    }
                    // 列表可能很长，只展示前 100 条命中，避免一次性铺开卡 UI。
                    items(items = matched.take(100)) { post ->
                        PostHitRow(post = post) {
                            navigator.navigate(ThreadPageDestination(threadId = post.threadId))
                        }
                    }
                } else {
                    when {
                        isLoading -> item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorText != null -> item {
                            Text(
                                text = errorText.orEmpty(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colors.error,
                            )
                        }

                        result != null -> {
                            val r = result ?: PostAnalysisResult()
                            item { OverviewSection(result = r) }
                            item {
                                AnalysisSection(title = stringResource(id = R.string.title_analysis_month)) {
                                    BarChart(
                                        buckets = r.monthBuckets,
                                        axisLabel = "${r.monthBuckets.firstOrNull()?.key.orEmpty()} → " +
                                            r.monthBuckets.lastOrNull()?.key.orEmpty(),
                                    )
                                }
                            }
                            item {
                                AnalysisSection(title = stringResource(id = R.string.title_analysis_hour)) {
                                    BarChart(buckets = r.hourBuckets, axisLabel = "0 时 → 23 时")
                                }
                            }
                            item {
                                AnalysisSection(title = stringResource(id = R.string.title_analysis_forum_top)) {
                                    RankList(buckets = r.forumBuckets)
                                }
                            }
                            item {
                                AnalysisSection(title = stringResource(id = R.string.title_analysis_words)) {
                                    WordCloud(words = r.wordBuckets)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(result: PostAnalysisResult) {
    AnalysisSection(title = stringResource(id = R.string.title_analysis_overview)) {
        StatRow(
            label = stringResource(id = R.string.label_analysis_total),
            value = result.total.toString(),
        )
        StatRow(
            label = stringResource(id = R.string.label_analysis_threads),
            value = result.threadCount.toString(),
        )
        StatRow(
            label = stringResource(id = R.string.label_analysis_replies),
            value = result.replyCount.toString(),
        )
        StatRow(
            label = stringResource(id = R.string.label_analysis_forum_count),
            value = result.forumCount.toString(),
        )
        StatRow(
            label = stringResource(id = R.string.label_analysis_range),
            value = if (result.firstTimeSec > 0L) {
                "${formatDateTime(result.firstTimeSec)} ~ ${formatDateTime(result.lastTimeSec)}"
            } else {
                "—"
            },
        )
    }
}

@Composable
private fun PostHitRow(post: PostRecord, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = buildString {
                if (post.forumName.isNotBlank()) append(post.forumName).append(" · ")
                append(if (post.isThread) "主题帖" else "回帖")
                if (post.createTimeSec > 0L) append(" · ").append(formatDateTime(post.createTimeSec))
            },
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.text,
        )
        val preview = listOf(post.title, post.content)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .replace("\n", " ")
        Text(
            text = preview,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.text,
        )
    }
}

