package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.appPreferences
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 只读保护拦截器，两级防护：
 * - 开启安全模式：拦截全部写操作（发贴 / 回贴 / 签到 / 点赞 / 关注 / 收藏 / 吧务 / 资料修改 / 图片上传等）；
 * - 未开启「显示有风险的功能」开关（且未开安全模式）：仅拦截高风险的发贴 / 回贴接口，
 *   点赞 / 关注 / 收藏 / 签到等低风险操作正常放行。
 *
 * 发贴 / 回贴入口在 UI 层（ReplyPage）已提前弹窗拦截，本拦截器作为兜底。
 */
class SafeModeInterceptor : Interceptor {

    companion object {
        /** 高风险接口：发贴 / 回贴（自创内容 + 内容审核关联，封号风险最高） */
        private val POST_ENDPOINTS = listOf(
            "/c/c/post/add",             // 回贴
            "/c/c/post/addPollPost",     // 投票回贴
            "/c/c/thread/add",           // 发贴
            "/mo/q/apubpost",            // web 发贴/回贴
        )

        /** 全部写操作接口（安全模式下全拦） */
        private val WRITE_ENDPOINTS = listOf(
            "/c/c/post/add",             // 回贴
            "/c/c/post/addPollPost",     // 投票回贴
            "/c/c/thread/add",           // 发贴
            "/mo/q/apubpost",            // web 发贴/回贴
            "/c/c/forum/sign",           // 签到
            "/c/c/forum/msign",          // 一键签到
            "/c/c/agree/opAgree",        // 点赞/点踩
            "/c/c/forum/like",           // 关注吧
            "/c/c/forum/unlike",         // 取消关注吧
            "/c/c/forum/unfavolike",     // 取消收藏
            "/c/f/forum/like",           // 关注吧（旧）
            "/c/c/user/follow",          // 关注用户
            "/c/c/user/unfollow",        // 取消关注用户
            "/c/c/user/setUserBlack",    // 拉黑
            "/c/c/post/addstore",        // 收藏帖子
            "/c/c/post/rmstore",         // 取消收藏
            "/c/c/bawu/",                // 吧务操作（删帖/封禁/加精等）
            "/c/c/excellent/submitDislike", // 吐槽/负反馈
            "/c/c/profile/modify",       // 修改资料
            "/c/s/initNickname",         // 初始化昵称
            "/c/c/img/portrait",         // 修改头像
            "/c/s/uploadPicture",        // 上传图片
            "/mo/q/cooluploadpic",       // web 上传图片
            "/c/f/ueg/checkjubao",       // 举报预检
        )

        private fun isWriteEndpoint(path: String): Boolean =
            WRITE_ENDPOINTS.any { path.contains(it) }

        private fun isPostEndpoint(path: String): Boolean =
            POST_ENDPOINTS.any { path.contains(it) }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val prefs = App.INSTANCE.appPreferences
        val path = request.url.encodedPath
        if (prefs.safeMode && isWriteEndpoint(path)) {
            throw IOException(App.INSTANCE.getString(R.string.error_safe_mode_blocked))
        }
        if (!prefs.safeMode && !prefs.showRiskyFeatures && isPostEndpoint(path)) {
            throw IOException(App.INSTANCE.getString(R.string.error_post_hidden_blocked))
        }
        return chain.proceed(request)
    }
}
