package com.guitartuner.practice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * 自绘圆角进度条（替代原生 ProgressBar），带平滑动画。
 */
class RoundedProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A3442.toInt()
    }
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2DD4BF.toInt()
    }

    /** 进度 0~1 */
    private var target = 0f
    private var current = 0f
    private var animator: ValueAnimator? = null

    fun setProgress(percent: Float) {
        target = percent.coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(current, target).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                current = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val radius = h / 2f

        // 背景
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, bgPaint)
        // 前景
        val fw = w * current
        if (fw > radius) {
            canvas.drawRoundRect(RectF(0f, 0f, fw, h), radius, radius, fgPaint)
        }
    }
}
