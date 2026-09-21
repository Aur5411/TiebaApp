package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getFloat
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getLong
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_LIGHT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


open class AppPreferencesUtils private constructor(ctx: Context) {
    companion object {
        private var instance: AppPreferencesUtils? = null

        fun getInstance(context: Context): AppPreferencesUtils {
            return instance ?: AppPreferencesUtils(context).also {
                instance = it
            }
        }
    }

    private val contextWeakReference: WeakReference<Context> = WeakReference(ctx)

    private val context: Context
        get() = contextWeakReference.get()!!

    private val preferencesDataStore: DataStore<Preferences>
        get() = context.dataStore

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var userLikeLastRequestUnix by DataStoreDelegates.long(defaultValue = 0L)

    var ignoreBatteryOptimizationsDialog by DataStoreDelegates.boolean(defaultValue = false)

    var appIcon by DataStoreDelegates.string(
        defaultValue = LauncherIcons.DEFAULT_ICON,
        key = AppIconUtil.PREF_KEY_APP_ICON
    )

    var useThemedIcon by DataStoreDelegates.boolean(defaultValue = false)

    var autoSign by DataStoreDelegates.boolean(defaultValue = false, key = "auto_sign")

    var autoSignTime by DataStoreDelegates.string(
        defaultValue = "09:00",
        key = "auto_sign_time"
    )

    var blockVideo by DataStoreDelegates.boolean(defaultValue = false)

    var showFollowedOnly by DataStoreDelegates.boolean(defaultValue = false)

    var checkCIUpdate by DataStoreDelegates.boolean(
        defaultValue = false
    )

    var collectThreadSeeLz by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "collect_thread_see_lz"
    )

    var collectThreadDescSort by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "collect_thread_desc_sort"
    )

    var customPrimaryColor by DataStoreDelegates.string(key = "custom_primary_color")

    var customStatusBarFontDark by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "custom_status_bar_font_dark"
    )

    var toolbarPrimaryColor by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "custom_toolbar_primary_color"
    )

    var defaultSortType by DataStoreDelegates.string(
        key = "default_sort_type",
        defaultValue = "0"
    )

    var darkTheme by DataStoreDelegates.string(key = "dark_theme", defaultValue = "grey_dark")

    var doNotUsePhotoPicker by DataStoreDelegates.boolean(defaultValue = false)

    var useDynamicColorTheme by DataStoreDelegates.boolean(defaultValue = false)

    var followSystemNight by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "follow_system_night"
    )

    /** 用户在权限弹窗里点了「禁止」后置为 true，之后不再重复提示 */
    var notifyPermissionDenied by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "notify_permission_denied"
    )

    var fontScale by DataStoreDelegates.float(defaultValue = 1.0f)

    var forumFabFunction by DataStoreDelegates.string(defaultValue = "post")

    var hideBlockedContent by DataStoreDelegates.boolean(defaultValue = false)

    var hideExplore by DataStoreDelegates.boolean(defaultValue = false)

    var defaultStart by DataStoreDelegates.int(defaultValue = 0)

    var hideForumIntroAndStat by DataStoreDelegates.boolean(defaultValue = false)

    var incognitoMode by DataStoreDelegates.boolean(defaultValue = false)

    var hideMedia by DataStoreDelegates.boolean(defaultValue = false)

    var hideReply by DataStoreDelegates.boolean(defaultValue = false)

    var homePageScroll by DataStoreDelegates.boolean(defaultValue = false)

    var homePageShowHistoryForum by DataStoreDelegates.boolean(defaultValue = true)

    var imageDarkenWhenNightMode by DataStoreDelegates.boolean(defaultValue = true)

    var imageLoadType by DataStoreDelegates.string(
        key = "image_load_type",
        defaultValue = "0"
    )

    var imeHeight by DataStoreDelegates.int(defaultValue = 800)

    var liftUpBottomBar by DataStoreDelegates.boolean(defaultValue = true)

    var listItemsBackgroundIntermixed by DataStoreDelegates.boolean(defaultValue = true)

    // 「进吧」列表默认单列显示（贴吧列表带吧头像，单列更清晰）
    var listSingle by DataStoreDelegates.boolean(defaultValue = true)

    // 「进吧」列表排序方式：0=按热度 1=按发帖时间 2=按等级 3=按名称
    var homeSortType by DataStoreDelegates.int(defaultValue = 0)

    var littleTail by DataStoreDelegates.string(key = "little_tail")

    var loadPictureWhenScroll by DataStoreDelegates.boolean(defaultValue = true)

    var oldTheme by DataStoreDelegates.string(key = "old_theme")

    var oksignSlowMode by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "oksign_slow_mode"
    )

    var oksignUseOfficialOksign by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "oksign_use_official_oksign"
    )

    var oksignFailAutoStop by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "oksign_fail_auto_stop"
    )

    var picWatermarkType by DataStoreDelegates.string(
        defaultValue = "2",
        key = "pic_watermark_type",
    )

    /**
     * 安全模式（只读开关）。
     *
     * 开启后，本 App 会拦截发贴、回贴、签到、点赞、关注等所有写操作，仅保留浏览功能，
     * 用于最大程度规避第三方客户端写操作带来的封号风险。
     * 在「设置 - 习惯设置」中切换。
     */
    var safeMode by DataStoreDelegates.boolean(defaultValue = false)

    /**
     * 是否显示有风险的功能入口（发贴 / 回贴 / 签到 / 点赞 / 关注 / 收藏 / 编辑资料等）。
     *
     * 默认 false：隐藏这些写操作入口，配合安全模式形成只读保护。
     * 在「设置 - 习惯设置」中打开该开关后，会自动重启应用使隐藏的入口恢复显示。
     */
    var showRiskyFeatures by DataStoreDelegates.boolean(defaultValue = false)

    var radius by DataStoreDelegates.int(defaultValue = 8)

    var signDay by DataStoreDelegates.int(defaultValue = -1, key = "sign_day")

    var showBlockTip by DataStoreDelegates.boolean(defaultValue = true)

    var showBothUsernameAndNickname by DataStoreDelegates.boolean(
        defaultValue = false,
        key = "show_both_username_and_nickname"
    )

    var showExperimentalFeatures by DataStoreDelegates.boolean(defaultValue = false)

    var showShortcutInThread by DataStoreDelegates.boolean(defaultValue = true)

    var showTopForumInNormalList by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "show_top_forum_in_normal_list"
    )

    var statusBarDarker by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "status_bar_darker"
    )

    var theme by DataStoreDelegates.string(defaultValue = ThemeUtil.THEME_DEFAULT)

    var translucentBackgroundAlpha by DataStoreDelegates.int(
        defaultValue = 255,
        key = "translucent_background_alpha"
    )

    var translucentBackgroundBlur by DataStoreDelegates.int(key = "translucent_background_blur")

    var translucentBackgroundTheme by DataStoreDelegates.int(
        defaultValue = TRANSLUCENT_THEME_LIGHT,
        key = "translucent_background_theme"
    )

    var translucentThemeBackgroundPath by DataStoreDelegates.string(key = "translucent_theme_background_path")

    var translucentPrimaryColor by DataStoreDelegates.string(key = "translucent_primary_color")

    var useCustomTabs by DataStoreDelegates.boolean(
        defaultValue = true,
        key = "use_custom_tabs"
    )

    var useWebView by DataStoreDelegates.boolean(defaultValue = true, key = "use_webview")

    private object DataStoreDelegates {
        fun int(
            defaultValue: Int = 0,
            key: String? = null
        ) = object : ReadWriteProperty<AppPreferencesUtils, Int> {
            private var prefValue = defaultValue
            private var initialized = false

            override fun getValue(thisRef: AppPreferencesUtils, property: KProperty<*>): Int {
                val finalKey = key ?: property.name
                if (!initialized) {
                    initialized = true
                    prefValue = thisRef.preferencesDataStore.getInt(finalKey, defaultValue)
                    thisRef.coroutineScope.launch {
                        thisRef.preferencesDataStore.data
                            .map { it[intPreferencesKey(finalKey)] }
                            .distinctUntilChanged()
                            .collect {
                                prefValue = it ?: defaultValue
                            }
                    }
                }
                return prefValue
            }

            override fun setValue(
                thisRef: AppPreferencesUtils,
                property: KProperty<*>,
                value: Int
            ) {
                prefValue = value
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.edit {
                        it[intPreferencesKey(key ?: property.name)] = value
                    }
                }
            }
        }

        fun string(
            defaultValue: String? = null,
            key: String? = null
        ) = object : ReadWriteProperty<AppPreferencesUtils, String?> {
            private var prefValue = defaultValue
            private var initialized = false

            override fun getValue(thisRef: AppPreferencesUtils, property: KProperty<*>): String? {
                val finalKey = key ?: property.name
                if (!initialized) {
                    initialized = true
                    prefValue = thisRef.preferencesDataStore.getString(finalKey)
                        ?: defaultValue
                    thisRef.coroutineScope.launch {
                        thisRef.preferencesDataStore.data
                            .map { it[stringPreferencesKey(finalKey)] }
                            .distinctUntilChanged()
                            .collect {
                                prefValue = it ?: defaultValue
                            }
                    }
                }
                return prefValue
            }

            override fun setValue(
                thisRef: AppPreferencesUtils,
                property: KProperty<*>,
                value: String?
            ) {
                prefValue = value
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.edit {
                        if (value == null) {
                            it.remove(stringPreferencesKey(key ?: property.name))
                        } else {
                            it[stringPreferencesKey(key ?: property.name)] = value
                        }
                    }
                }
            }
        }

        fun float(
            defaultValue: Float = 0F,
            key: String? = null
        ) = object : ReadWriteProperty<AppPreferencesUtils, Float> {
            private var prefValue = defaultValue
            private var initialized = false

            override fun getValue(thisRef: AppPreferencesUtils, property: KProperty<*>): Float {
                val finalKey = key ?: property.name
                if (!initialized) {
                    initialized = true
                    prefValue =
                        thisRef.preferencesDataStore.getFloat(finalKey, defaultValue)
                    thisRef.coroutineScope.launch {
                        thisRef.preferencesDataStore.data
                            .map { it[floatPreferencesKey(finalKey)] }
                            .distinctUntilChanged()
                            .collect {
                                prefValue = it ?: defaultValue
                            }
                    }
                }
                return prefValue
            }

            override fun setValue(
                thisRef: AppPreferencesUtils,
                property: KProperty<*>,
                value: Float
            ) {
                prefValue = value
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.edit {
                        it[floatPreferencesKey(key ?: property.name)] = value
                    }
                }
            }
        }

        fun long(
            defaultValue: Long = 0L,
            key: String? = null
        ) = object : ReadWriteProperty<AppPreferencesUtils, Long> {
            private var prefValue = defaultValue
            private var initialized = false

            override fun getValue(thisRef: AppPreferencesUtils, property: KProperty<*>): Long {
                val finalKey = key ?: property.name
                if (!initialized) {
                    initialized = true
                    prefValue =
                        thisRef.preferencesDataStore.getLong(finalKey, defaultValue)
                    thisRef.coroutineScope.launch {
                        thisRef.preferencesDataStore.data
                            .map { it[longPreferencesKey(finalKey)] }
                            .distinctUntilChanged()
                            .collect {
                                prefValue = it ?: defaultValue
                            }
                    }
                }
                return prefValue
            }

            override fun setValue(
                thisRef: AppPreferencesUtils,
                property: KProperty<*>,
                value: Long
            ) {
                prefValue = value
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.edit {
                        it[longPreferencesKey(key ?: property.name)] = value
                    }
                }
            }
        }

        fun boolean(
            defaultValue: Boolean = false,
            key: String? = null
        ) = object : ReadWriteProperty<AppPreferencesUtils, Boolean> {
            private var prefValue = defaultValue
            private var initialized = false

            override fun getValue(thisRef: AppPreferencesUtils, property: KProperty<*>): Boolean {
                val finalKey = key ?: property.name
                if (!initialized) {
                    initialized = true
                    prefValue =
                        thisRef.preferencesDataStore.getBoolean(finalKey, defaultValue)
                    thisRef.coroutineScope.launch {
                        thisRef.preferencesDataStore.data
                            .map { it[booleanPreferencesKey(finalKey)] }
                            .distinctUntilChanged()
                            .collect {
                                prefValue = it ?: defaultValue
                            }
                    }
                }
                return prefValue
            }

            override fun setValue(
                thisRef: AppPreferencesUtils,
                property: KProperty<*>,
                value: Boolean
            ) {
                prefValue = value
                thisRef.coroutineScope.launch {
                    thisRef.preferencesDataStore.edit {
                        it[booleanPreferencesKey(key ?: property.name)] = value
                    }
                }
            }
        }
    }
}

val Context.appPreferences: AppPreferencesUtils
    get() = AppPreferencesUtils.getInstance(this)