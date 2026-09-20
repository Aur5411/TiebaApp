package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.Manifest
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.OnPermissionInterceptor
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.base.IPermission
import com.hjq.permissions.permission.PermissionLists
import com.huanchengfly.tieba.post.R
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.huanchengfly.tieba.post.toastShort

/**
 * Convert String permission to IPermission object
 */
private fun String.toIPermission(): IPermission {
    return when (this) {
        PermissionUtils.CAMERA, Manifest.permission.CAMERA -> PermissionLists.getCameraPermission()
        PermissionUtils.READ_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> PermissionLists.getReadExternalStoragePermission()
        PermissionUtils.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> PermissionLists.getWriteExternalStoragePermission()
        PermissionUtils.RECORD_AUDIO, Manifest.permission.RECORD_AUDIO -> PermissionLists.getRecordAudioPermission()
        PermissionUtils.POST_NOTIFICATIONS -> PermissionLists.getPostNotificationsPermission()
        PermissionUtils.READ_MEDIA_IMAGES -> PermissionLists.getReadMediaImagesPermission()
        PermissionUtils.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION -> PermissionLists.getAccessFineLocationPermission()
        PermissionUtils.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> PermissionLists.getAccessCoarseLocationPermission()
        else -> throw IllegalArgumentException("Unknown permission: $this")
    }
}

object PermissionUtils {

    const val READ_CALENDAR = "android.permission.READ_CALENDAR"
    const val WRITE_CALENDAR = "android.permission.WRITE_CALENDAR"

    const val CAMERA = "android.permission.CAMERA"

    const val READ_CONTACTS = "android.permission.READ_CONTACTS"
    const val WRITE_CONTACTS = "android.permission.WRITE_CONTACTS"
    const val GET_ACCOUNTS = "android.permission.GET_ACCOUNTS"

    const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
    const val ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"

    const val READ_PHONE_STATE = "android.permission.READ_PHONE_STATE"
    const val CALL_PHONE = "android.permission.CALL_PHONE"
    const val USE_SIP = "android.permission.USE_SIP"
    const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
    const val ANSWER_PHONE_CALLS = "android.permission.ANSWER_PHONE_CALLS"
    const val ADD_VOICEMAIL = "com.android.voicemail.permission.ADD_VOICEMAIL"

    const val READ_CALL_LOG = "android.permission.READ_CALL_LOG"
    const val WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG"
    const val PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS"

    const val BODY_SENSORS = "android.permission.BODY_SENSORS"
    const val ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION"

    const val SEND_SMS = "android.permission.SEND_SMS"
    const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
    const val READ_SMS = "android.permission.READ_SMS"
    const val RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH"
    const val RECEIVE_MMS = "android.permission.RECEIVE_MMS"

    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"

    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    /**
     * Turn permissions into text.
     */
    fun transformText(context: Context, permissions: List<String>): List<String> {
        val permissionNames: MutableList<String> = mutableListOf()
        if (context == null) {
            return permissionNames
        }
        if (permissions == null) {
            return permissionNames
        }
        for (permission in permissions) {
            when (permission) {
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    val hint = context.getString(R.string.common_permission_storage)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hint = context.getString(R.string.common_permission_image_and_video)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                "android.permission.READ_MEDIA_AUDIO" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hint = context.getString(R.string.common_permission_music_and_audio)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                Manifest.permission.CAMERA -> {
                    val hint = context.getString(R.string.common_permission_camera)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.RECORD_AUDIO -> {
                    val hint = context.getString(R.string.common_permission_microphone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, "android.permission.ACCESS_BACKGROUND_LOCATION" -> {
                    val hint: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        !permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        !permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) {
                        context.getString(R.string.common_permission_location_background)
                    } else {
                        context.getString(R.string.common_permission_location)
                    }
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.BODY_SENSORS, "android.permission.BODY_SENSORS_BACKGROUND" -> {
                    val hint: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !permissions.contains(Manifest.permission.BODY_SENSORS)
                    ) {
                        context.getString(R.string.common_permission_body_sensors_background)
                    } else {
                        context.getString(R.string.common_permission_body_sensors)
                    }
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val hint = context.getString(R.string.common_permission_nearby_devices)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                "android.permission.NEARBY_WIFI_DEVICES" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hint = context.getString(R.string.common_permission_nearby_devices)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, "com.android.voicemail.permission.ADD_VOICEMAIL", Manifest.permission.USE_SIP, "android.permission.READ_PHONE_NUMBERS", "android.permission.ANSWER_PHONE_CALLS" -> {
                    val hint = context.getString(R.string.common_permission_phone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS -> {
                    val hint = context.getString(R.string.common_permission_contacts)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> {
                    val hint = context.getString(R.string.common_permission_calendar)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG, "android.permission.PROCESS_OUTGOING_CALLS" -> {
                    val hint =
                        context.getString(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.common_permission_call_logs else R.string.common_permission_phone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.ACTIVITY_RECOGNITION" -> {
                    val hint =
                        context.getString(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) R.string.common_permission_activity_recognition_api30 else R.string.common_permission_activity_recognition_api29)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.ACCESS_MEDIA_LOCATION" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val hint =
                            context.getString(R.string.common_permission_access_media_location)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_WAP_PUSH, Manifest.permission.RECEIVE_MMS -> {
                    val hint = context.getString(R.string.common_permission_sms)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val hint = context.getString(R.string.common_permission_all_file_access)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                "android.permission.REQUEST_INSTALL_PACKAGES" -> {
                    val hint = context.getString(R.string.common_permission_install_unknown_apps)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.SYSTEM_ALERT_WINDOW" -> {
                    val hint = context.getString(R.string.common_permission_display_over_other_apps)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.WRITE_SETTINGS" -> {
                    val hint = context.getString(R.string.common_permission_modify_system_settings)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.NOTIFICATION_SERVICE" -> {
                    val hint = context.getString(R.string.common_permission_allow_notifications)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.POST_NOTIFICATIONS" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hint = context.getString(R.string.common_permission_post_notifications)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }

                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" -> {
                    val hint =
                        context.getString(R.string.common_permission_allow_notifications_access)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.PACKAGE_USAGE_STATS" -> {
                    val hint = context.getString(R.string.common_permission_apps_with_usage_access)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.SCHEDULE_EXACT_ALARM" -> {
                    val hint = context.getString(R.string.common_permission_alarms_reminders)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.ACCESS_NOTIFICATION_POLICY" -> {
                    val hint = context.getString(R.string.common_permission_do_not_disturb_access)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> {
                    val hint = context.getString(R.string.common_permission_ignore_battery_optimize)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.BIND_VPN_SERVICE" -> {
                    val hint = context.getString(R.string.common_permission_vpn)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                "android.permission.PICTURE_IN_PICTURE" -> {
                    val hint = context.getString(R.string.common_permission_picture_in_picture)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }

                else -> {}
            }
        }

        return permissionNames
    }

    fun askPermission(context: Context, permissionData: PermissionData, onGranted: () -> Unit) {
        askPermission(context, permissionData, R.string.tip_no_permission, onGranted)
    }

    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: Int,
        onGranted: () -> Unit
    ) {
        askPermission(context, permissionData, context.getString(deniedToast), onGranted, null)
    }

    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: Int,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)?
    ) {
        askPermission(context, permissionData, context.getString(deniedToast), onGranted, onDenied)
    }

    @JvmOverloads
    fun askPermission(
        context: Context,
        permissionData: PermissionData,
        deniedToast: String,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        val permissionList = permissionData.permissions.map { it.toIPermission() }
        if (XXPermissions.isGrantedPermissions(context, permissionList)) {
            onGranted()
        } else {
            val grantedCb = onGranted
            val deniedCb = onDenied
            PermissionRequester(context).apply {
                this.permissions = permissionData.permissions
                this.description = permissionData.desc
                this.onGranted = grantedCb
                this.onDenied = {
                    context.toastShort(deniedToast)
                    deniedCb?.invoke()
                }
            }.start()
        }
    }

    /**
     * 部分权限没有运行时弹窗、只能在系统设置页里开启（悬浮窗 / 修改系统设置 / 电池优化 / 使用情况访问），
     * 以及 Android 12 及以下的通知权限。命中则直接打开对应系统页面并返回 true。
     */
    fun openSystemPermissionSetting(context: Context, permissions: List<String>): Boolean {
        val pkg = context.packageName
        val target: Intent? = when {
            permissions.contains("android.permission.SYSTEM_ALERT_WINDOW") ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", pkg, null))

            permissions.contains("android.permission.WRITE_SETTINGS") ->
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", pkg, null))

            permissions.contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") ->
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.fromParts("package", pkg, null)
                )

            permissions.contains("android.permission.PACKAGE_USAGE_STATS") ->
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

            permissions.contains(POST_NOTIFICATIONS) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)

            else -> null
        }
        if (target == null) return false
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(target)
            true
        }.getOrDefault(false)
    }

    /**
     * 通知权限是否已授予。Android 13+ 用运行时权限判断，12 及以下看通知开关。
     */
    fun isNotificationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                XXPermissions.isGrantedPermission(
                    context,
                    PermissionLists.getPostNotificationsPermission()
                )
            }.getOrDefault(false)
        } else {
            runCatching {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }.getOrDefault(true)
        }
    }

    data class PermissionData(val permissions: List<String>, val desc: String)
}

class PermissionRequester(val context: Context) {
    var permissions: List<String> = emptyList()
    var description: String = ""
    var unchecked: Boolean = false
    var onGranted: (() -> Unit)? = null
    var onDenied: (() -> Unit)? = null

    /**
     * 统一权限入口：直接拉起「系统权限弹窗」，不再弹应用内的说明框。
     *
     * 流程：
     *  已授予            -> onGranted
     *  特殊权限(无系统弹窗) -> 打开对应系统设置页
     *  普通权限           -> 系统权限弹窗 -> 用户点「允许」即生效
     *  已被永久拒绝        -> 系统弹窗不再出现，提示并跳该权限的设置页
     */
    fun start() {
        val permissionList = permissions.map { it.toIPermission() }
        if (XXPermissions.isGrantedPermissions(context, permissionList)) {
            onGranted?.invoke()
            return
        }
        startSystemRequest(permissionList)
    }

    /**
     * 「允许」的实际动作：能走系统设置页的权限先开页面，其余走标准运行时权限弹窗。
     * 用户在系统侧确认后，手机设置里的权限即同步。
     */
    private fun startSystemRequest(permissionList: List<IPermission>) {
        if (PermissionUtils.openSystemPermissionSetting(context, permissions)) return

        // XXPermissions 要求传 Activity 才能拉起系统弹窗；这里是 Context 时尝试自动取 Activity
        val activity = context.findActivity()
        if (activity == null) {
            // 拿不到 Activity 就没法弹系统框，退化为打开该应用的权限设置页
            XXPermissions.startPermissionActivity(context, permissionList)
            onDenied?.invoke()
            return
        }

        XXPermissions.with(activity)
            .permissions(permissionList)
            .apply {
                if (unchecked) {
                    unchecked()
                }
                // 拦截器：压掉 XXPermissions 自带的说明框，直接进系统弹窗
                interceptor(SilentPermissionInterceptor)
            }
            .request(object : OnPermissionCallback {
                override fun onResult(
                    grantedList: MutableList<IPermission>,
                    deniedList: MutableList<IPermission>
                ) {
                    if (deniedList.isEmpty()) {
                        onGranted?.invoke()
                        return
                    }
                    // 被永久拒绝（系统弹窗不会再出现）时，引导到系统设置页手动开启
                    if (XXPermissions.isDoNotAskAgainPermissions(activity, deniedList)) {
                        runCatching {
                            context.toastShort(R.string.tip_no_permission)
                        }
                    }
                    onDenied?.invoke()
                }
            })
    }
}

/**
 * 静默拦截器：不做任何 UI，直接放行到系统权限弹窗。
 * XXPermissions 默认会先弹自己的说明框，这里覆盖掉以实现「一键直达系统弹窗」。
 */
object SilentPermissionInterceptor : OnPermissionInterceptor

/**
 * 从 Context 中取出宿主 Activity（ContextWrapper 逐层解开）。
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Context.requestPermission(
    builder: PermissionRequester.() -> Unit
) {
    PermissionRequester(this).apply(builder).start()
}