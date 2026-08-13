package com.guitartuner.audio

import kotlin.math.abs

/** 音高检测结果：检测到的基频（Hz）与置信度（0~1） */
data class PitchResult(val frequency: Float, val confidence: Float)

/**
 * 基于 YIN 算法的音高检测器（自相关差分函数 + 累积均值归一化 + 抛物线插值）。
 * 对吉他 E2(82Hz)~E4(330Hz) 及其泛音鲁棒，比朴素过零率/FFT 更稳定。
 */
class PitchDetector(private val sampleRate: Int, private val bufferSize: Int) {

    private val yin = FloatArray(bufferSize / 2)
    private val threshold = 0.10f

    fun detect(buffer: FloatArray): PitchResult? {
        val w = buffer.size / 2
        if (w < 8) return null

        // 1) 差分函数 d(tau) = Σ (x[i] - x[i+tau])^2
        for (tau in 0 until w) {
            var sum = 0.0
            for (i in 0 until w) {
                val d = buffer[i] - buffer[i + tau]
                sum += d * d
            }
            yin[tau] = sum.toFloat()
        }

        // 2) 累积均值归一化差
        yin[0] = 1f
        var runningSum = 0.0
        for (tau in 1 until w) {
            runningSum += yin[tau]
            yin[tau] = (yin[tau] * tau / runningSum).toFloat()
        }

        // 3) 绝对阈值：找第一个低于阈值的局部极小
        var tauEstimate = -1
        var tau = 2
        while (tau < w) {
            if (yin[tau] < threshold) {
                while (tau + 1 < w && yin[tau + 1] < yin[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return null

        // 4) 抛物线插值提高精度
        val betterTau = parabolicInterpolation(tauEstimate)
        if (betterTau <= 0) return null

        val frequency = sampleRate.toFloat() / betterTau
        val confidence = (1f - yin[tauEstimate]).coerceIn(0f, 1f)
        return PitchResult(frequency, confidence)
    }

    private fun parabolicInterpolation(tau: Int): Float {
        if (tau <= 0 || tau + 1 >= yin.size) return tau.toFloat()
        val s0 = yin[tau - 1]
        val s1 = yin[tau]
        val s2 = yin[tau + 1]
        val denom = 2f * (2f * s1 - s2 - s0)
        if (abs(denom) < 1e-9f) return tau.toFloat()
        return tau + (s2 - s0) / denom
    }
}
