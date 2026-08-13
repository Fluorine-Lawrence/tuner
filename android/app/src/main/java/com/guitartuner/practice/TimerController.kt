package com.guitartuner.practice

import android.os.SystemClock

/**
 * 练习计时器核心：完整计时（开始/暂停/继续/结束），用 SystemClock 累计多段时长，
 * 中途暂停不影响总时长。不持有 Android 组件，可单测。
 */
class TimerController {

    enum class State { IDLE, RUNNING, PAUSED }

    private var state = State.IDLE

    /** 当前段开始时的 elapsedRealtime */
    private var segmentStart = 0L

    /** 已累计的时长（不含当前进行中的段） */
    private var accumulated = 0L

    /** 总暂停时长（用于计算净练习时间） */
    private var totalPaused = 0L

    /** 当前段开始时的真实时间戳（用于写 session 的 startTime） */
    private var wallStart = 0L

    val isIdle: Boolean get() = state == State.IDLE
    val isRunning: Boolean get() = state == State.RUNNING
    val isPaused: Boolean get() = state == State.PAUSED

    /** 当前总练习时长（毫秒） */
    val elapsedMs: Long
        get() {
            val seg = if (state == State.RUNNING)
                SystemClock.elapsedRealtime() - segmentStart else 0L
            return accumulated + seg
        }

    /** 开始（从空闲或暂停恢复） */
    fun start() {
        if (state == State.RUNNING) return
        if (state == State.IDLE) {
            accumulated = 0L
            totalPaused = 0L
            wallStart = System.currentTimeMillis()
        }
        segmentStart = SystemClock.elapsedRealtime()
        state = State.RUNNING
    }

    /** 暂停 */
    fun pause() {
        if (state != State.RUNNING) return
        accumulated += SystemClock.elapsedRealtime() - segmentStart
        totalPaused += System.currentTimeMillis() - wallStart
        state = State.PAUSED
    }

    /** 结束并返回结果 */
    fun stop(): Result {
        if (state == State.RUNNING) {
            accumulated += SystemClock.elapsedRealtime() - segmentStart
        }
        val dur = accumulated
        state = State.IDLE
        return Result(
            durationMs = dur,
            wallStart = wallStart,
            wallEnd = System.currentTimeMillis()
        )
    }

    data class Result(val durationMs: Long, val wallStart: Long, val wallEnd: Long)
}
