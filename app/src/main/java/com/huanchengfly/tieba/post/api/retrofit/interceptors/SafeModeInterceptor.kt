package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.appPreferences
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 安全模式拦截器：开启安全模式后，在网络层拦截所有写操作
 * （发贴 / 回贴 / 签到 / 一键签到 / 点赞 / 关注 / 收藏 / 吧务 / 资料修改 / 图片上传等），
 * 仅放行读请求，最大程度规避第三方客户端写操作带来的封号风险。
 *
 * 安全模式默认开启。UI 层已同步隐藏发贴 / 回贴 / 一键签到等入口，
 * 本拦截器作为兜底，覆盖其余写操作入口。
 *
 * **登录不在拦截范围内**：登录属于「身份建立」而非向社区提交内容，
 * `/c/s/login`、`/c/s/initNickname` 等接口有意不列入 [WRITE_ENDPOINTS]，
 * 并由 [LOGIN_ENDPOINTS] 白名单显式兜底，确保安全模式下也能正常登录 / 切换账号。
 */
class SafeModeInterceptor : Interceptor {

    companion object {
        /**
         * 会产生写操作的接口路径（包含匹配），安全模式下全部拦截。
         *
         * 注意：**不要**把登录相关路径（如 `/c/s/login`）加进来，
         * 否则安全模式下将无法登录。
         */
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
            "/c/c/img/portrait",         // 修改头像
            "/c/s/uploadPicture",        // 上传图片
            "/mo/q/cooluploadpic",       // web 上传图片
            "/c/f/ueg/checkjubao",       // 举报预检
        )

        /**
         * 登录相关路径白名单：即使未来有人往 [WRITE_ENDPOINTS] 里误加了包含 `login`
         * 之类的宽泛片段，这里也会兜底放行，保证登录永远可用。
         */
        private val LOGIN_ENDPOINTS = listOf(
            "/c/s/login",        // 登录 / 校验登录态
            "/c/s/initNickname", // 首次登录初始化昵称（属登录流程一环）
        )

        private fun isWriteEndpoint(path: String): Boolean {
            if (LOGIN_ENDPOINTS.any { path.contains(it) }) return false
            return WRITE_ENDPOINTS.any { path.contains(it) }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (App.INSTANCE.appPreferences.safeMode
            && isWriteEndpoint(request.url.encodedPath)
        ) {
            throw IOException(App.INSTANCE.getString(R.string.error_safe_mode_blocked))
        }
        return chain.proceed(request)
    }
}
