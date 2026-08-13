package com.guitartuner.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 自绘滚轮选择器（iOS 风格）：垂直滚动、中间高亮、松手吸附、上下渐变遮罩。
 */
class WheelPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val itemHeight = 44f * density
    private val visibleCount = 5

    private var items: List<String> = emptyList()
    private var selectedIndex = 0
    private var offset = 0f
    private var lastY = 0f
    private var animator: ValueAnimator? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 17f * density
        color = 0xFF9CA3AF.toInt()
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 19f * density
        color = 0xFF2DD4BF.toInt()
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1C242E.toInt()
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A3442.toInt()
        strokeWidth = 1f * density
    }
    private val topMask = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomMask = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setItems(list: List<String>, initial: Int) {
        items = list
        selectedIndex = initial.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        offset = selectedIndex * itemHeight
        invalidate()
    }

    fun getSelectedIndex(): Int = selectedIndex

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (items.isEmpty()) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // 手指上滑 → 内容上移 → 值增大（与 iOS 滚轮一致）
                offset -= (event.y - lastY)
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                snapToNearest()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToNearest() {
        val maxIndex = (items.size - 1).coerceAtLeast(0)
        val target = (offset / itemHeight).roundToInt().coerceIn(0, maxIndex)
        val targetOffset = target * itemHeight
        animator?.cancel()
        animator = ValueAnimator.ofFloat(offset, targetOffset).apply {
            duration = 160
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                offset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        selectedIndex = target
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f
        val cx = w / 2f

        // 中间高亮区
        canvas.drawRoundRect(
            RectF(0f, centerY - itemHeight / 2f, w, centerY + itemHeight / 2f),
            12f * density, 12f * density, highlightPaint
        )

        // 可见项范围
        val firstVisible = floor(offset / itemHeight).toInt() - (visibleCount / 2) - 1
        val lastVisible = firstVisible + visibleCount + 2

        for (i in firstVisible..lastVisible) {
            if (i < 0 || i >= items.size) continue
            // 文本中心 y：i 项对准 centerY（去掉多余的 +itemHeight/2，使选中项与高亮框中心对齐）
            val y = centerY + i * itemHeight - offset
            val dist = abs(y - centerY) / itemHeight
            val alpha = (1f - dist * 0.9f).coerceIn(0.15f, 1f)
            val paint = if (i == selectedIndex) centerTextPaint else textPaint
            paint.alpha = (alpha * 255).toInt()
            val baseline = y - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(items[i], cx, baseline, paint)
        }
        textPaint.alpha = 255
        centerTextPaint.alpha = 255

        // 分隔线
        canvas.drawLine(0f, centerY - itemHeight / 2f, w, centerY - itemHeight / 2f, dividerPaint)
        canvas.drawLine(0f, centerY + itemHeight / 2f, w, centerY + itemHeight / 2f, dividerPaint)

        // 上下渐变遮罩
        ensureMasks()
        canvas.drawRect(0f, 0f, w, centerY - itemHeight / 2f, topMask)
        canvas.drawRect(0f, centerY + itemHeight / 2f, w, h, bottomMask)
    }

    private fun ensureMasks() {
        val h = height.toFloat()
        if (h <= 0) return
        val fade = itemHeight * 1.5f
        val bg = 0xFF141A22.toInt()  // surface 色
        topMask.shader = LinearGradient(0f, 0f, 0f, fade,
            bg, bg and 0x00FFFFFF, Shader.TileMode.CLAMP)
        bottomMask.shader = LinearGradient(0f, h, 0f, h - fade,
            bg, bg and 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
}
