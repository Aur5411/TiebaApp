package com.huanchengfly.tieba.post.ui.page.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

/** 一个分析卡片：标题 + 内容。 */
@Composable
internal fun AnalysisSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(color = ExtendedTheme.colors.card, shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text,
        )
        content()
    }
}

/** 竖向柱状图：横轴为分桶顺序，高度按最大值归一化。 */
@Composable
internal fun BarChart(
    buckets: List<Bucket>,
    modifier: Modifier = Modifier,
    axisLabel: String = "",
) {
    if (buckets.isEmpty()) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.text,
        )
        return
    }
    val max = buckets.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                val heightDp = (bucket.count.toFloat() / max * 100f).coerceAtLeast(3f).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(heightDp)
                        .background(
                            color = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
            }
        }
        if (axisLabel.isNotEmpty()) {
            Text(
                text = axisLabel,
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.text,
            )
        }
    }
}

/**
 * 横向排行条：左侧名称、中间占比条、右侧次数。
 *
 * [onItemClick] 非空时整行可点击（例如点贴吧名跳到该吧、点用户 ID 跳去查人）。
 */
@Composable
internal fun RankList(
    buckets: List<Bucket>,
    modifier: Modifier = Modifier,
    onItemClick: ((String) -> Unit)? = null,
) {
    if (buckets.isEmpty()) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.text,
        )
        return
    }
    val max = buckets.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buckets.forEach { bucket ->
            val ratio = (bucket.count.toFloat() / max).coerceIn(0f, 1f)
            val rowModifier = if (onItemClick != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(bucket.key) }
            } else {
                Modifier.fillMaxWidth()
            }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = bucket.key,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.text,
                )
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(10.dp)
                        .background(
                            color = MaterialTheme.colors.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(5.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(10.dp)
                            .background(
                                color = MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(5.dp),
                            ),
                    )
                }
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.text,
                )
            }
        }
    }
}

/** 词云：字号随频次线性增大，透明度也随之提高。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordCloud(
    words: List<Bucket>,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) {
        Text(
            text = "暂无数据",
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.text,
        )
        return
    }
    val max = words.maxOf { it.count }
    val min = words.minOf { it.count }
    val span = (max - min).coerceAtLeast(1)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        words.forEach { word ->
            val t = (word.count - min).toFloat() / span
            Text(
                text = word.key,
                fontSize = (14f + t * 16f).sp,
                color = MaterialTheme.colors.primary.copy(alpha = (0.55f + t * 0.45f)),
                fontWeight = if (t > 0.6f) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/** 一行「标签：值」。 */
@Composable
internal fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = ExtendedTheme.colors.text,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text,
        )
    }
}
