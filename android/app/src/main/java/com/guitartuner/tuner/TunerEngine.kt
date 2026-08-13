package com.guitartuner.tuner

import android.os.SystemClock
import com.guitartuner.audio.PitchResult
import com.guitartuner.tuning.GuitarString
import com.guitartuner.tuning.Instrument
import com.guitartuner.tuning.Note
import com.guitartuner.tuning.Tuning
import com.guitartuner.tuning.TuningLibrary
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/** 调音模式：智能（自动识弦） / 手动（用户选弦） */
enum class Mode { SMART, MANUAL }

/** 纠错提示类型 */
enum class Hint {
    LISTENING,      // 等待拨弦 / 信号不稳
    IN_TUNE,        // 已调准
    SHARP,          // 偏高，调低
    FLAT,           // 偏低，调高
    WRONG_DIRECTION,// 拧反方向
    WRONG_STRING,   // 可能调错弦
    NO_MATCH        // 未匹配到琴弦
}

data class TunerReading(
    val note: Note,
    val frequency: Float,
    val centsFromNote: Double,   // 对最近半音的偏差
    val targetString: Int?,      // 目标弦号（1 起，低音=1）
    val targetNote: Note?,
    val centsFromTarget: Double, // 对目标弦的偏差
    val matchedString: Int?,     // 检测音高最接近的弦号
    val hint: Hint
)

/**
 * 调音核心：把检测到的频率映射为音符/琴弦/偏差，并产出纠错提示。
 */
class TunerEngine {

    var tuning: Tuning = TuningLibrary.defaultFor(Instrument.GUITAR)
    var mode: Mode = Mode.SMART
    var manualString: Int = 6

    private val directionMonitor = DirectionMonitor()
    private var lastTarget: Int? = null

    fun process(result: PitchResult): TunerReading {
        val f = result.frequency.toDouble()
        val midi = (69.0 + 12.0 * log2(f / 440.0)).roundToInt()
        val note = Note.of(midi)
        val centsFromNote = centsBetween(f, note.frequency)

        // 找检测音高最接近的弦
        var matched: GuitarString? = null
        var matchedDist = Double.MAX_VALUE
        for (s in tuning.strings) {
            val d = abs(centsBetween(f, s.frequency))
            if (d < matchedDist) {
                matchedDist = d
                matched = s
            }
        }

        // 目标弦：智能模式=自动匹配，手动模式=用户选中
        val target: GuitarString? = when (mode) {
            Mode.SMART -> matched
            Mode.MANUAL -> tuning.strings.firstOrNull { it.number == manualString }
        }
        val targetCents = if (target != null) centsBetween(f, target.frequency) else centsFromNote

        // 目标弦变化时重置方向监测；仅在信号稳定时记录偏差
        if (target?.number != lastTarget) {
            directionMonitor.reset()
            lastTarget = target?.number
        }
        if (target != null && abs(targetCents) <= 60.0 && result.confidence >= 0.7f) {
            directionMonitor.update(SystemClock.elapsedRealtime(), targetCents)
        }

        val hint = evaluateHint(result.confidence, f, target, targetCents, matched, matchedDist)

        return TunerReading(
            note = note,
            frequency = result.frequency,
            centsFromNote = centsFromNote,
            targetString = target?.number,
            targetNote = target?.note,
            centsFromTarget = targetCents,
            matchedString = matched?.number,
            hint = hint
        )
    }

    private fun evaluateHint(
        confidence: Float,
        f: Double,
        target: GuitarString?,
        targetCents: Double,
        matched: GuitarString?,
        matchedDist: Double
    ): Hint {
        if (target == null) return Hint.NO_MATCH
        if (confidence < 0.7f) return Hint.LISTENING

        val absC = abs(targetCents)
        if (absC <= 5.0) return Hint.IN_TUNE

        // 错弦：手动模式下，检测音高更接近另一根弦 → 提示“拧错旋钮”
        if (mode == Mode.MANUAL && matched != null &&
            matched.number != target.number && absC > 20.0
        ) {
            val matchedCents = abs(centsBetween(f, matched.frequency))
            if (matchedCents < absC - 30.0) return Hint.WRONG_STRING
        }

        // 拧反方向：偏差在持续增大
        if (matchedDist <= 40.0 && absC in 5.0..60.0) {
            val slopeCps = directionMonitor.slope() * 1000.0 // cents / 秒
            if (slopeCps > 12.0) return Hint.WRONG_DIRECTION
        }

        return if (targetCents > 0) Hint.SHARP else Hint.FLAT
    }

    private fun centsBetween(f: Double, targetFreq: Double): Double =
        1200.0 * log2(f / targetFreq)
}
