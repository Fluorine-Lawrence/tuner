package com.guitartuner.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 调音指针仪表盘：半圆形刻度（±100 音分），红→黄→绿→黄→红 彩色分区，
 * 指针随偏差摆动，调准时指针发光；无信号时彩色区变暗、指针变灰。
 */
class TunerGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cents = 0.0
    private var active = false

    private val density = resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 13f * density
        color = 0xFF232A36.toInt()
    }
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 13f * density
        strokeCap = Paint.Cap.BUTT
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f * density
        color = 0xFF5A6478.toInt()
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * density
        color = 0xFF6B7280.toInt()
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF9AA3B2.toInt()
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    fun setCents(c: Double) {
        cents = c.coerceIn(-RANGE, RANGE)
        invalidate()
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
        val zoneAlpha = if (active) 255 else 70
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
        val inTune = active && abs(cents) <= 5.0
        needlePaint.color = if (active) colorForCents(cents) else 0xFF4A5262.toInt()
        if (inTune) {
            needlePaint.setShadowLayer(16f * density, 0f, 0f, 0x88_22C55E.toInt())
        } else {
            needlePaint.clearShadowLayer()
        }
        val needleRad = angleRad(cents)
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
            a <= 5 -> 0xFF22C55E.toInt()  // 绿（已调准）
            a <= 30 -> 0xFFF59E0B.toInt() // 黄（接近）
            else -> 0xFFEF4444.toInt()    // 红（偏离较大）
        }
    }

    private data class Zone(val startAngle: Float, val sweepAngle: Float, val color: Int)

    private fun zones(): List<Zone> {
        val red = 0xFFEF4444.toInt()
        val amber = 0xFFF59E0B.toInt()
        val green = 0xFF22C55E.toInt()

        fun zone(start: Double, end: Double, c: Int) = Zone(
            (180.0 + (start + RANGE) / (2.0 * RANGE) * 180.0).toFloat(),
            ((end - start) / (2.0 * RANGE) * 180.0).toFloat(),
            c
        )
        return listOf(
            zone(-100.0, -30.0, red),
            zone(-30.0, -5.0, amber),
            zone(-5.0, 5.0, green),
            zone(5.0, 30.0, amber),
            zone(30.0, 100.0, red)
        )
    }

    companion object {
        private const val RANGE = 100.0
    }
}
