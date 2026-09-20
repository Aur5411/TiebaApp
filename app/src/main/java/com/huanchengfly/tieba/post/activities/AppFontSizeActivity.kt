package com.huanchengfly.tieba.post.activities

import android.os.Bundle
import android.util.TypedValue
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.adapters.ChatBubbleStyleAdapter
import com.huanchengfly.tieba.post.components.MyLinearLayoutManager
import com.huanchengfly.tieba.post.databinding.ActivityAppFontSizeBinding
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ThemeUtil

class AppFontSizeActivity : BaseActivity<ActivityAppFontSizeBinding>() {
    companion object {
        const val FONT_SCALE_MIN = 0.8f
        const val FONT_SCALE_MAX = 1.3f
        const val FONT_SCALE_STEP = 0.05f

        val SIZE_TEXT_MAPPING = mapOf(
            R.string.text_size_small to (0..1),
            R.string.text_size_little_small to (2..3),
            R.string.text_size_default to (4..4),
            R.string.text_size_little_large to (5..6),
            R.string.text_size_large to (7..8),
            R.string.text_size_very_large to (9..10)
        )
    }

    private var oldFontSize: Float = 0f
    private var finished: Boolean = false

    private val bubblesAdapter: ChatBubbleStyleAdapter by lazy {
        ChatBubbleStyleAdapter(
            this,
            listOf(
                ChatBubbleStyleAdapter.Bubble(
                    getString(R.string.bubble_want_change_font_size),
                    ChatBubbleStyleAdapter.Bubble.POSITION_RIGHT
                ),
                ChatBubbleStyleAdapter.Bubble(getString(R.string.bubble_change_font_size))
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewBinding(ActivityAppFontSizeBinding.inflate(layoutInflater))

        // 背景透明处理
        ThemeUtil.setTranslucentThemeBackground(this, binding?.background)

        setSupportActionBar(binding?.appbar?.collapsingToolbar?.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = this@AppFontSizeActivity.title
        }
        binding?.appbar?.collapsingToolbar?.toolbar?.title = title

        oldFontSize = appPreferences.fontScale

        // 对话气泡 RecyclerView
        binding?.appFontSizeBubbles.apply {
            this?.layoutManager = MyLinearLayoutManager(this@AppFontSizeActivity, LinearLayoutManager.VERTICAL, false)
            this?.adapter = bubblesAdapter
        }

        // SeekBar 初始进度
        val progress = ((appPreferences.fontScale * 1000L - FONT_SCALE_MIN * 1000L).toInt()) /
                ((FONT_SCALE_STEP * 1000L).toInt())
        binding?.appFontSizeSeekbar?.progress = progress
        updateSizeText(progress)

        // 监听字体大小变化
        binding?.appFontSizeSeekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontScale = FONT_SCALE_MIN + progress * FONT_SCALE_STEP
                appPreferences.fontScale = fontScale
                updatePreview(fontScale)
                updateSizeText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun finish() {
        if (!finished && oldFontSize != appPreferences.fontScale) {
            finished = true
            toastShort(R.string.toast_after_change_will_restart)
            App.INSTANCE.removeAllActivity()
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                startActivity(it)
            }
        }
        super.finish()
    }

    private fun updateSizeText(progress: Int) {
        val sizeTexts = SIZE_TEXT_MAPPING.filterValues { progress in it }
        if (sizeTexts.isNotEmpty()) {
            binding?.appFontSizeText?.setText(sizeTexts.entries.first().key)
        }
    }

    private fun updatePreview(fontScale: Float = appPreferences.fontScale) {
        bubblesAdapter.bubblesFontSize = 15f.dpToPxFloat() * fontScale
        binding?.appFontSizeText?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f.dpToPxFloat() * fontScale)
    }
}