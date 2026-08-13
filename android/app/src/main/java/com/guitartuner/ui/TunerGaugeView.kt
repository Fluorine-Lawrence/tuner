package com.guitartuner.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 调音指针仪表盘：深底发光刻度，半圆形量程（±100 音分），
 * 红→琥珀→青绿→琥珀→红 彩色分区，指针平滑摆动，调准时青绿发光。
 */
class TunerGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetCents = 0.0
    private var currentCents = 0.0
    private var active = false
    private var animator: ValueAnimator? = null

    private val density = resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f * density
        color = 0xFF1C242E.toInt()
    }
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f * density
        color = 0xFF3A4656.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
        color = 0xFF9CA3AF.toInt()
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE5E7EB.toInt()
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF0B0F14.toInt()
    }

    fun setCents(c: Double) {
        targetCents = c.coerceIn(-RANGE, RANGE)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(currentCents.toFloat(), targetCents.toFloat()).apply {
            duration = 120
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                currentCents = (it.animatedValue as Float).toDouble()
                invalidate()
            }
            start()
        }
    }

    fun setActive(a: Boolean) {
        active = a
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h * 0.94f
        val r = min(w, h) * 0.80f
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)

        // 底部轨道
        canvas.drawArc(rect, 180f, 180f, false, trackPaint)

        // 彩色分区（无信号时压暗）
        val zoneAlpha = if (active) 255 else 50
        for (z in zones()) {
            zonePaint.color = z.color
            zonePaint.alpha = zoneAlpha
            canvas.drawArc(rect, z.startAngle, z.sweepAngle, false, zonePaint)
        }
        zonePaint.alpha = 255

        // 刻度线
        for (c in -RANGE.toInt()..RANGE.toInt() step 5) {
            val isMajor = c % 20 == 0
            val r1 = r * if (isMajor) 0.80f else 0.86f
            val r2 = r * 0.94f
            val rad = angleRad(c.toDouble())
            canvas.drawLine(
                cx + (cos(rad) * r1).toFloat(), cy + (sin(rad) * r1).toFloat(),
                cx + (cos(rad) * r2).toFloat(), cy + (sin(rad) * r2).toFloat(),
                tickPaint
            )
        }

        // 刻度标签
        for (c in listOf(-100, -50, 0, 50, 100)) {
            val rad = angleRad(c.toDouble())
            val lr = r * 0.62f
            val lx = cx + (cos(rad) * lr).toFloat()
            val ly = cy + (sin(rad) * lr).toFloat() + labelPaint.textSize / 2f
            val text = when {
                c == 0 -> "0"
                c > 0 -> "+$c"
                else -> "$c"
            }
            canvas.drawText(text, lx, ly, labelPaint)
        }

        // 指针
        val inTune = active && abs(currentCents) <= 5.0
        needlePaint.color = if (active) colorForCents(currentCents) else 0xFF3A4656.toInt()
        if (inTune) {
            needlePaint.setShadowLayer(20f * density, 0f, 0f, 0xAA_2DD4BF.toInt())
        } else {
            needlePaint.clearShadowLayer()
        }
        val needleRad = angleRad(currentCents)
        val needleLen = r * 0.74f
        canvas.drawLine(
            cx, cy,
            cx + (cos(needleRad) * needleLen).toFloat(),
            cy + (sin(needleRad) * needleLen).toFloat(),
            needlePaint
        )
        needlePaint.clearShadowLayer()

        // 中心
        canvas.drawCircle(cx, cy, r * 0.075f, pivotPaint)
        canvas.drawCircle(cx, cy, r * 0.04f, centerPaint)
    }

    private fun angleRad(cents: Double): Double =
        Math.toRadians(180.0 + (cents + RANGE) / (2.0 * RANGE) * 180.0)

    private fun colorForCents(c: Double): Int {
        val a = abs(c)
        return when {
            a <= 5 -> 0xFF2DD4BF.toInt()  // 青绿（已调准）
            a <= 30 -> 0xFFF59E0B.toInt() // 琥珀（接近）
            else -> 0xFFF87171.toInt()    // 红（偏离较大）
        }
    }

    private data class Zone(val startAngle: Float, val sweepAngle: Float, val color: Int)

    private fun zones(): List<Zone> {
        val red = 0xFFF87171.toInt()
        val amber = 0xFFF59E0B.toInt()
        val teal = 0xFF2DD4BF.toInt()

        fun zone(start: Double, end: Double, c: Int) = Zone(
            (180.0 + (start + RANGE) / (2.0 * RANGE) * 180.0).toFloat(),
            ((end - start) / (2.0 * RANGE) * 180.0).toFloat(),
            c
        )
        return listOf(
            zone(-100.0, -30.0, red),
            zone(-30.0, -5.0, amber),
            zone(-5.0, 5.0, teal),
            zone(5.0, 30.0, amber),
            zone(30.0, 100.0, red)
        )
    }

    companion object {
        private const val RANGE = 100.0
    }
}
