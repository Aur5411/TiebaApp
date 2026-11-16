package com.huanchengfly.tieba.post.ui.utils

import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

fun getPhotoViewData(
    post: Post,
    picId: String,
    picUrl: String,
    originUrl: String,
    showOriginBtn: Boolean,
    originSize: Int,
    seeLz: Boolean = false
): PhotoViewData? {
    if (post.from_forum == null) return null
    return PhotoViewData(
        data = LoadPicPageData(
            forumId = post.from_forum.id,
            forumName = post.from_forum.name,
            threadId = post.tid,
            postId = post.id,
            objType = "pb",
            picId = picId,
            picIndex = 1,
            seeLz = seeLz,
            originUrl = originUrl,
        ),
        picItems = persistentListOf(
            PicItem(
                picId = picId,
                picIndex = 1,
                url = picUrl,
                originUrl = originUrl,
                showOriginBtn = showOriginBtn,
                originSize = originSize,
                postId = post.id
            )
        )
    )
}

/**
 * 楼中楼（SubPost）图片用。SubPostList proto 里没有 tid / from_forum，
 * 所以 forumId / forumName / threadId 需要由调用方从外层帖子上下文传进来，
 * postId 用楼中楼自身 id。
 */
fun getPhotoViewData(
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long,
    picId: String,
    picUrl: String,
    originUrl: String,
    showOriginBtn: Boolean,
    originSize: Int,
    seeLz: Boolean = false
): PhotoViewData {
    val picItem = PicItem(
        picId = picId,
        picIndex = 1,
        url = picUrl,
        originUrl = originUrl,
        showOriginBtn = showOriginBtn,
        originSize = originSize,
        postId = postId
    )
    return PhotoViewData(
        // forumId 无效时不带 LoadPicPageData，只看单图，避免用 0 去请求图片列表
        data = if (forumId != 0L) LoadPicPageData(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = postId,
            objType = "pb",
            picId = picId,
            picIndex = 1,
            seeLz = seeLz,
            originUrl = originUrl,
        ) else null,
        picItems = persistentListOf(picItem),
    )
}

fun getPhotoViewData(
    threadInfo: ThreadInfo,
    index: Int
): PhotoViewData {
    return getPhotoViewData(
        medias = threadInfo.media,
        forumId = threadInfo.forumId,
        forumName = threadInfo.forumName,
        threadId = threadInfo.threadId,
        index = index
    )
}

fun getPhotoViewData(
    medias: List<Media>,
    forumId: Long,
    forumName: String,
    threadId: Long,
    index: Int
): PhotoViewData {
    val media = medias[index]
    return PhotoViewData(
        data = LoadPicPageData(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = media.postId,
            seeLz = false,
            objType = "index",
            picId = ImageUtil.getPicId(media.originPic),
            picIndex = index + 1,
            originUrl = media.originPic
        ),
        picItems = medias.mapIndexed { mediaIndex, mediaItem ->
            PicItem(
                picId = ImageUtil.getPicId(mediaItem.originPic),
                picIndex = mediaIndex + 1,
                url = mediaItem.bigPic,
                originUrl = mediaItem.originPic,
                showOriginBtn = mediaItem.showOriginalBtn == 1,
                originSize = mediaItem.originSize,
                postId = mediaItem.postId
            )
        }.toImmutableList(),
        index = index
    )
}