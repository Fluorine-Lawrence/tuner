package com.guitartuner.practice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * 自绘柱状图：展示练习时长分布。柱为圆角矩形，加载/更新时柱子平滑生长。
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2DD4BF.toInt()
    }
    private val barDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A3442.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 10f * density
        color = 0xFF6B7280.toInt()
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 9f * density
        color = 0xFF9CA3AF.toInt()
    }

    data class Item(val label: String, val durationMs: Long)

    private var items: List<Item> = emptyList()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    fun setData(data: List<Item>) {
        items = data
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) {
            canvas.drawText("暂无数据", width / 2f, height / 2f, labelPaint)
            return
        }
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 8f * density
        val labelH = 20f * density
        val chartTop = pad
        val chartBottom = h - labelH - pad
        val chartH = chartBottom - chartTop

        val maxMs = max(items.maxOf { it.durationMs }, 1L)
        val slot = w / items.size
        val barW = slot * 0.6f

        items.forEachIndexed { i, item ->
            val cx = slot * i + slot / 2f
            val fullH = if (item.durationMs <= 0) 2f * density
            else (item.durationMs.toFloat() / maxMs * chartH).coerceAtLeast(3f * density)
            val barH = fullH * progress
            val rect = RectF(
                cx - barW / 2f,
                chartBottom - barH,
                cx + barW / 2f,
                chartBottom
            )
            val p = if (item.durationMs > 0) barPaint else barDimPaint
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, p)
            canvas.drawText(item.label, cx, h - 6f * density, labelPaint)
            if (item.durationMs > 0 && progress >= 0.98f) {
                canvas.drawText(fmt(item.durationMs), cx, rect.top - 3f * density, valuePaint)
            }
        }
    }

    private fun fmt(ms: Long): String {
        val m = ms / 60000
        return if (m >= 60) "${m / 60}h${m % 60}m" else "${m}分"
    }
}
