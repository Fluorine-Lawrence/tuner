package com.guitartuner.tuning

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 十二平均律音符，内部用 MIDI 音符号表示（60 = C4 = 中央 C）。
 * 音名统一使用升号（#）记法。
 */
class Note private constructor(val midi: Int) {

    val name: String
        get() = NAMES[Math.floorMod(midi, 12)]

    val octave: Int
        get() = midi / 12 - 1

    val frequency: Double
        get() = frequencyOf(midi)

    val fullName: String
        get() = "$name$octave"

    override fun equals(other: Any?): Boolean = other is Note && other.midi == midi
    override fun hashCode(): Int = midi
    override fun toString(): String = fullName

    companion object {
        private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        fun of(midi: Int): Note = Note(midi)

        fun of(name: String, octave: Int): Note {
            val idx = NAMES.indexOfFirst { it.equals(name, ignoreCase = true) }
            require(idx >= 0) { "未知音名: $name" }
            return Note((octave + 1) * 12 + idx)
        }

        /** 从频率取最近的半音音符 */
        fun fromFrequency(f: Double): Note = of((69.0 + 12.0 * log2(f / 440.0)).roundToInt())

        /** MIDI 音符号 → 频率（A4 = 440Hz） */
        fun frequencyOf(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

        /** 解析 "F#3" / "C-1" 形式的字符串 */
        fun parse(s: String): Note {
            var i = s.length
            while (i > 0 && (s[i - 1].isDigit() || s[i - 1] == '-')) i--
            val name = s.substring(0, i)
            val octave = s.substring(i).toInt()
            return of(name, octave)
        }
    }
}
