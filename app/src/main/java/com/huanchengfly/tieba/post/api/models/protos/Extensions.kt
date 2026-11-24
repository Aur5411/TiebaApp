package com.huanchengfly.tieba.post.api.models.protos

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender.Companion.appendText
import com.huanchengfly.tieba.post.ui.common.VideoContentRender
import com.huanchengfly.tieba.post.ui.common.VoiceContentRender
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.thread.SubPostItemData
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

val List<Abstract>.abstractText: String
    get() = joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            4 -> it.text

            else -> ""
        }
    }

@OptIn(ExperimentalTextApi::class)
val ThreadInfo.abstractText: String
    get() = richAbstract.joinToString(separator = "") {
        when (it.type) {
            0,40 -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }
            else -> ""
        }
    }

val PostInfoList.abstractText: String
    get() = rich_abstract.joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }

            else -> ""
        }
    }

val ThreadInfo.hasAgree: Int
    get() = agree?.hasAgree ?: 0
val ThreadInfo.hasAgreed: Boolean
    get() = hasAgree == 1
val ThreadInfo.hasAbstract: Boolean
    get() = richAbstract.any { (it.type == 0 && it.text.isNotBlank()) || it.type == 2 }

fun ThreadInfo.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agreeNum = agreeNum + 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agreeNum = agreeNum - 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    copy(
        agreeNum = if (hasAgree == 1) agreeNum + 1 else agreeNum - 1
    )
}

fun ThreadInfo.updateCollectStatus(
    newStatus: Int,
    markPostId: Long
) = if (collectStatus != newStatus) {
    this.copy(
        collectStatus = newStatus,
        collectMarkPid = markPostId.toString()
    )
} else {
    this
}

/**
 * 投票成功后本地刷新投票数据。
 *
 * 服务端的 addPollPost 只返回一个空壳 result，不会把最新的票数下发回来，
 * 而 UI 直接渲染 [PollInfo.total_poll] / [PollOption.num] / [PollInfo.total_num]，
 * 所以必须在这里做一次本地增量，否则用户投完票看到的还是投票前的数字
 * （之前只改了 is_polled，导致「已参与投票」文案生效但票数一动不动）。
 */
fun PollInfo.updatePollStatus(
    polledOptionIds: Collection<Int>
): PollInfo {
    val ids = polledOptionIds.toSet()
    if (ids.isEmpty()) {
        return copy(is_polled = 1)
    }
    val newOptions = options.map { option ->
        if (ids.contains(option.id)) option.copy(num = option.num + 1) else option
    }
    return copy(
        is_polled = 1,
        options = newOptions,
        // 多选时一次提交会加多票，参与人数只 +1（一个人）
        total_poll = total_poll + ids.size,
        total_num = total_num + 1
    )
}

/**
 * 投票成功后刷新整条主题的投票数据。
 *
 * 注意 [ThreadInfo.poll_info] 与 [ThreadInfo.origin_thread_info]?.poll_info 是两个字段，
 * 而 `resolvePollInfo` 优先使用 origin_thread_info 里的那份，
 * 只改 poll_info 的话票数区依旧不会变。
 */
fun ThreadInfo.updatePollStatus(
    polledOptionIds: Collection<Int>
): ThreadInfo = copy(
    poll_info = poll_info?.updatePollStatus(polledOptionIds),
    origin_thread_info = origin_thread_info?.let { origin ->
        origin.copy(
            poll_info = origin.poll_info?.updatePollStatus(polledOptionIds) ?: origin.poll_info
        )
    }
)

fun Post.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    this
}

fun SubPostList.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    this
}

fun PostInfoList.updateAgreeStatus(
    hasAgree: Int,
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                ),
                agree_num = agree_num + 1
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                ),
                agree_num = agree_num - 1
            )
        }
    } else {
        this
    }
} else {
    this
}

private val PbContent.picUrl: String
    get() =
        ImageUtil.getUrl(
            App.INSTANCE,
            true,
            originSrc,
            bigCdnSrc,
            bigSrc,
            dynamic_,
            cdnSrc,
            cdnSrcActive,
            src
        )

val List<PbContent>.plainText: String
    get() = renders.joinToString("\n") { it.toString() }

@OptIn(ExperimentalTextApi::class)
val List<PbContent>.renders: ImmutableList<PbContentRender>
    get() {
        val renders = mutableListOf<PbContentRender>()

        forEach {
            when (it.type) {
                0, 9, 27, 35, 40 -> {
                    renders.appendText(it.text)
                }

                1 -> {
                    val text = buildAnnotatedString {
                        appendInlineContent("link_icon", alternateText = "🔗")
                        withAnnotation(tag = "url", annotation = it.link) {
                            withStyle(
                                SpanStyle(
                                    color = Color(
                                        ThemeUtils.getColorByAttr(
                                            App.INSTANCE,
                                            R.attr.colorNewPrimary
                                        )
                                    )
                                )
                            ) {
                                append(it.text)
                            }
                        }
                    }
                    renders.appendText(text)
                }

                2 -> {
                    EmoticonManager.registerEmoticon(
                        it.text,
                        it.c
                    )
                    val emoticonText = "#(${it.c})".emoticonString
                    renders.appendText(emoticonText)
                }

                3 -> {
                    val width = it.bsize.split(",")[0].toInt()
                    val height = it.bsize.split(",")[1].toInt()
                    renders.add(
                        PicContentRender(
                            picUrl = it.picUrl,
                            originUrl = it.originSrc,
                            showOriginBtn = it.showOriginalBtn == 1,
                            originSize = it.originSize,
                            picId = ImageUtil.getPicId(it.originSrc),
                            width = width,
                            height = height
                        )
                    )
                }

                4 -> {
                    val text = buildAnnotatedString {
                        withAnnotation(tag = "user", annotation = "${it.uid}") {
                            withStyle(
                                SpanStyle(
                                    color = Color(
                                        ThemeUtils.getColorByAttr(
                                            App.INSTANCE,
                                            R.attr.colorNewPrimary
                                        )
                                    )
                                )
                            ) {
                                append(it.text)
                            }
                        }
                    }
                    renders.appendText(text)
                }

                5 -> {
                    if (it.src.isNotBlank()) {
                        val width = it.bsize.split(",")[0].toInt()
                        val height = it.bsize.split(",")[1].toInt()
                        renders.add(
                            VideoContentRender(
                                videoUrl = it.link,
                                picUrl = it.src,
                                webUrl = it.text,
                                width = width,
                                height = height
                            )
                        )
                    } else {
                        val text = buildAnnotatedString {
                            appendInlineContent("video_icon", alternateText = "🎥")
                            withAnnotation(tag = "url", annotation = it.text) {
                                withStyle(
                                    SpanStyle(
                                        color = Color(
                                            ThemeUtils.getColorByAttr(
                                                App.INSTANCE,
                                                R.attr.colorNewPrimary
                                            )
                                        )
                                    )
                                ) {
                                    append(App.INSTANCE.getString(R.string.tag_video))
                                    append(it.text)
                                }
                            }
                        }
                        renders.appendText(text)
                    }
                }

                10 -> {
                    renders.add(VoiceContentRender(it.voiceMD5, it.duringTime))
                }

                20 -> {
                    val width = it.bsize.split(",")[0].toInt()
                    val height = it.bsize.split(",")[1].toInt()
                    renders.add(
                        PicContentRender(
                            picUrl = it.src,
                            originUrl = it.src,
                            showOriginBtn = it.showOriginalBtn == 1,
                            originSize = it.originSize,
                            picId = ImageUtil.getPicId(it.src),
                            width = width,
                            height = height
                        )
                    )
                }
            }
        }

        return renders.toImmutableList()
    }

val Post.contentRenders: ImmutableList<PbContentRender>
    get() {
        val renders = content.renders

        return renders.map {
            if (it is PicContentRender) {
                it.copy(
                    photoViewData = getPhotoViewData(
                        this,
                        it.picId,
                        it.picUrl,
                        it.originUrl,
                        it.showOriginBtn,
                        it.originSize
                    )
                )
            } else it
        }.toImmutableList()
    }

val User.bawuType: String?
    get() = if (is_bawu == 1) {
        if (bawu_type == "manager") "吧主" else "小吧主"
    } else null

val Post.subPostContents: ImmutableList<AnnotatedString>
    get() = sub_post_list?.sub_post_list?.map { it.getContentText(origin_thread_info?.author?.id) }
        ?.toImmutableList()
        ?: persistentListOf()

val Post.subPosts: ImmutableList<SubPostItemData>
    get() = sub_post_list?.sub_post_list?.map {
        SubPostItemData(
            it.wrapImmutable(),
            it.getContentText(origin_thread_info?.author?.id)
        )
    }?.toImmutableList() ?: persistentListOf()

@OptIn(ExperimentalTextApi::class)
fun SubPostList.getContentText(threadAuthorId: Long? = null): AnnotatedString {
    val context = App.INSTANCE
    val accentColor = Color(ThemeUtils.getColorByAttr(context, R.attr.colorNewPrimary))

    val userNameString = buildAnnotatedString {
        withAnnotation("user", "${author?.id}") {
            withStyle(
                SpanStyle(
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(
                    StringUtil.getUsernameAnnotatedString(
                        context,
                        author?.name ?: "",
                        author?.nameShow
                    )
                )
            }
            if (author?.id == threadAuthorId) {
                appendInlineContent("Lz")
            }
            append(": ")
        }
    }

    val contentStrings = content.renders.map { it.toAnnotationString() }

    return userNameString + contentStrings.reduce { acc, annotatedString -> acc + annotatedString }
}