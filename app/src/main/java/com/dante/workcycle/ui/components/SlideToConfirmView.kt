package com.dante.workcycle.ui.components

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.dante.workcycle.R
import com.google.android.material.card.MaterialCardView
import kotlin.math.max

class SlideToConfirmView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val labelView: TextView
    private val leadingIconView: ImageView
    private val handleCard: MaterialCardView
    private val handleIconView: ImageView

    private var slideEnabled = true
    private var onSlideComplete: (() -> Unit)? = null
    private var downRawX = 0f
    private var startTranslationX = 0f
    private var maxTranslationX = 0f
    private var isCompleting = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_slide_to_confirm, this, true)
        clipChildren = false
        clipToPadding = false

        labelView = findViewById(R.id.textSlideLabel)
        leadingIconView = findViewById(R.id.imageSlideLeadingIcon)
        handleCard = findViewById(R.id.cardSlideHandle)
        handleIconView = findViewById(R.id.imageSlideHandleIcon)

        handleCard.setOnTouchListener(::onHandleTouch)
        post { updateMaxTranslation() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { updateMaxTranslation() }
    }

    fun setLabelText(text: CharSequence) {
        labelView.text = text
        contentDescription = text
    }

    fun setLeadingIcon(@DrawableRes iconRes: Int) {
        leadingIconView.setImageResource(iconRes)
    }

    fun setHandleIcon(@DrawableRes iconRes: Int) {
        handleIconView.setImageResource(iconRes)
    }

    fun setOnSlideCompleteListener(listener: (() -> Unit)?) {
        onSlideComplete = listener
    }

    fun resetImmediately() {
        handleCard.animate().cancel()
        handleCard.translationX = 0f
        labelView.alpha = 1f
        leadingIconView.alpha = 1f
        isCompleting = false
    }

    fun setSlideEnabled(enabled: Boolean) {
        slideEnabled = enabled
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.6f
        if (!enabled) {
            resetImmediately()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun onHandleTouch(view: android.view.View, event: MotionEvent): Boolean {
        if (!slideEnabled || isCompleting) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                downRawX = event.rawX
                startTranslationX = handleCard.translationX
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val delta = event.rawX - downRawX
                val translation = (startTranslationX + delta).coerceIn(0f, maxTranslationX)
                handleCard.translationX = translation
                updateProgress(translation)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                if (handleCard.translationX >= maxTranslationX * COMPLETE_THRESHOLD) {
                    completeSlide()
                } else {
                    animateReset()
                }
                return true
            }
        }

        return false
    }

    private fun completeSlide() {
        isCompleting = true
        performHapticFeedback(resolveHapticFeedbackConstant())

        handleCard.animate()
            .translationX(maxTranslationX)
            .setDuration(120L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                performClick()
                onSlideComplete?.invoke()
                post { animateReset() }
            }
            .start()
    }

    private fun animateReset() {
        handleCard.animate()
            .translationX(0f)
            .setDuration(180L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                labelView.alpha = 1f
                leadingIconView.alpha = 1f
                isCompleting = false
            }
            .start()
    }

    private fun updateProgress(translationX: Float) {
        if (maxTranslationX <= 0f) return

        val progress = (translationX / maxTranslationX).coerceIn(0f, 1f)
        val fadedAlpha = 1f - (progress * 0.35f)
        labelView.alpha = fadedAlpha
        leadingIconView.alpha = fadedAlpha
    }

    private fun updateMaxTranslation() {
        val layoutParams = handleCard.layoutParams as MarginLayoutParams
        maxTranslationX = max(
            0,
            width - handleCard.width - layoutParams.marginStart - layoutParams.marginEnd
        ).toFloat()
    }

    private fun resolveHapticFeedbackConstant(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    }

    private companion object {
        const val COMPLETE_THRESHOLD = 0.78f
    }
}
