package com.guitartuner.tuner

import kotlin.math.abs

/**
 * 音高偏差趋势监测：维护最近一段时间内的“偏差（cents）”历史，
 * 用线性回归求 |偏差| 随时间变化的斜率，判断用户是否在“越拧越偏”。
 */
class DirectionMonitor(private val windowMs: Long = 1200L) {

    private data class Sample(val timeMs: Long, val cents: Double)

    private val samples = ArrayDeque<Sample>()

    fun reset() {
        samples.clear()
    }

    fun update(nowMs: Long, cents: Double) {
        samples.addLast(Sample(nowMs, cents))
        while (samples.isNotEmpty() && nowMs - samples.first().timeMs > windowMs) {
            samples.removeFirst()
        }
    }

    /** 返回 |偏差| 对时间的斜率（cents / 毫秒）。正值 = 越拧越偏。 */
    fun slope(): Double {
        val n = samples.size
        if (n < 3) return 0.0
        val meanX = samples.sumOf { it.timeMs.toDouble() } / n
        val meanY = samples.sumOf { abs(it.cents) } / n
        var num = 0.0
        var den = 0.0
        for (s in samples) {
            val dx = s.timeMs - meanX
            num += dx * (abs(s.cents) - meanY)
            den += dx * dx
        }
        if (den < 1e-6) return 0.0
        return num / den
    }
}
