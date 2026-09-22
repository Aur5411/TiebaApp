package com.huanchengfly.tieba.post.capabilities

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.interceptors.SafeModeInterceptor
import com.huanchengfly.tieba.post.utils.appPreferences
import okhttp3.Interceptor
import java.io.IOException

/**
 * 描述一个「可能受安全模式影响」的能力，例如回贴、签到、关注吧、修改资料等。
 *
 * 本 App 的**安全模式**核心不是「拦截某个 API」，而是「只允许浏览」：
 * 开启后所有写操作统一被拦截。因此引入本枚举，把所有受影响的入口显式登记出来，
 * 避免以后再出现「某处入口忘记判断」的遗漏。
 *
 * 使用方式：
 * ```
 * // 1) UI 层隐藏 / 禁用入口
 * if (Capability.REPLY.isAvailable()) { ... }
 *
 * // 2) 入口保留但操作无效时，给出统一提示
 * Capability.SIGN.checkDenyReason()?.let { context.toastShort(it) }
 * ```
 */
enum class Capability(
    /** 是否属于写操作（会被安全模式拦截）。false 表示读操作 / 身份建立，永远可用。 */
    val write: Boolean,
) {
    /** 浏览帖子（读操作） */
    BROWSE(write = false),

    /**
     * 登录 / 切换账号。
     *
     * **明确不属于写操作**，安全模式下必须始终放行——登录只是建立身份，
     * 并不向社区提交内容；若被拦截会导致安全模式下完全无法登录。
     */
    LOGIN(write = false),

    /** 回贴 / 发贴 */
    REPLY(write = true),

    /** 一键签到 / 自动签到 */
    SIGN(write = true),

    /** 关注吧 / 取消关注吧 */
    FOLLOW_FORUM(write = true),

    /** 关注用户 / 取消关注用户 */
    FOLLOW_USER(write = true),

    /** 收藏 / 取消收藏帖子 */
    COLLECT(write = true),

    /** 点赞 / 点踩 */
    AGREE(write = true),

    /** 吧务操作（删帖 / 封禁 / 加精等） */
    BAWU(write = true),

    /** 修改个人资料 / 头像 / 昵称 */
    PROFILE_MODIFY(write = true),

    /** 上传图片 */
    UPLOAD_IMAGE(write = true);

    /** 当前是否可用：读操作恒为 true；写操作仅在安全模式关闭时可用。 */
    fun isAvailable(): Boolean = !write || !App.INSTANCE.appPreferences.safeMode

    /**
     * 若当前不可用，返回给用户看的原因文案；可用时返回 null。
     *
     * [LOGIN] 属于读操作，永远返回 null —— 安全模式下必须能正常登录。
     */
    fun checkDenyReason(): String? =
        if (isAvailable()) null else App.INSTANCE.getString(R.string.error_safe_mode_blocked)

    // ---- 语义化别名：让调用点读起来更贴近业务，避免每次都写 !write 之类的判断 ----

    /** 是否需要对用户提示「安全模式下该操作不可用」 */
    val isBlockedBySafeMode: Boolean get() = !isAvailable()

    companion object {
        /**
         * 当前应该使用哪个安全模式拦截器实例。
         *
         * - 安全模式开启 → [SafeModeInterceptor]（拦截所有写操作）
         * - 安全模式关闭 → 放行全部请求
         *
         * 之所以按开关返回不同实例，而不是在拦截器内部读 `safeMode`：
         * 设置页切换安全模式后 App 会立即重启，因此「哪条链生效」由进程启动时的开关状态决定。
         * 注意 [com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi] 中所有 OkHttpClient
         * 都是 `by lazy`、创建后固化，所以实例切换必须发生在首次使用之前（重启天然满足）。
         */
        fun currentInterceptor(): Interceptor {
            return if (App.INSTANCE.appPreferences.safeMode) {
                SafeModeInterceptor()
            } else {
                PassThroughInterceptor
            }
        }
    }
}

/**
 * 安全模式关闭时使用的空拦截器：什么都不做，原样放行。
 *
 * 与 [SafeModeInterceptor] 成对存在，让「安全模式开 / 关」在代码层面各自对应一个明确的实例。
 */
object PassThroughInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(chain.request())
}

/**
 * 在 ViewModel / UI 层主动拦截写操作时抛出的异常。
 *
 * 与 [SafeModeInterceptor] 在网络层抛出的 `IOException` 语义一致：都是「安全模式阻止了写操作」，
 * 区别只在于拦截发生的位置（网络层 vs 业务层）。统一继承 [IOException]，
 * 便于既有的错误提示逻辑（读取 `getErrorMessage()`）直接复用 [R.string.error_safe_mode_blocked]。
 */
class SafeModeBlockedException : IOException(
    App.INSTANCE.getString(R.string.error_safe_mode_blocked)
)
