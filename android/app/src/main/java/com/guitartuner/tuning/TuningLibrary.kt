package com.guitartuner.tuning

/**
 * 内置调弦预设库，按乐器分组。每套调弦的 strings 按标准弦号升序排列
 * （1 弦 = 最高音、最细；N 弦 = 最低音、最粗）。
 */
object TuningLibrary {

    // ===== 吉他 =====
    val guitarPresets: List<Tuning> = listOf(
        preset("标准 EADGBE", "E" to 2, "A" to 2, "D" to 3, "G" to 3, "B" to 3, "E" to 4),
        preset("降半音 Eb", "D#" to 2, "G#" to 2, "C#" to 3, "F#" to 3, "A#" to 3, "D#" to 4),
        preset("降全音 D", "D" to 2, "G" to 2, "C" to 3, "F" to 3, "A" to 3, "D" to 4),
        preset("Drop D", "D" to 2, "A" to 2, "D" to 3, "G" to 3, "B" to 3, "E" to 4),
        preset("Drop C#", "C#" to 2, "G#" to 2, "C#" to 3, "F#" to 3, "A#" to 3, "D#" to 4),
        preset("Drop C", "C" to 2, "G" to 2, "C" to 3, "F" to 3, "A" to 3, "D" to 4),
        preset("Double Drop D", "D" to 2, "A" to 2, "D" to 3, "G" to 3, "B" to 3, "D" to 4),
        preset("DADGAD", "D" to 2, "A" to 2, "D" to 3, "G" to 3, "A" to 3, "D" to 4),
        preset("Open D", "D" to 2, "A" to 2, "D" to 3, "F#" to 3, "A" to 3, "D" to 4),
        preset("Open E", "E" to 2, "B" to 2, "E" to 3, "G#" to 3, "B" to 3, "E" to 4),
        preset("Open G", "D" to 2, "G" to 2, "D" to 3, "G" to 3, "B" to 3, "D" to 4),
        preset("Open A", "E" to 2, "A" to 2, "E" to 3, "A" to 3, "C#" to 4, "E" to 4),
        preset("Open C", "C" to 2, "G" to 2, "C" to 3, "G" to 3, "C" to 4, "E" to 4),
        preset("All Fourths", "E" to 2, "A" to 2, "D" to 3, "G" to 3, "C" to 4, "F" to 4),
        preset("New Standard (NST)", "C" to 2, "G" to 2, "D" to 3, "A" to 3, "E" to 4, "G" to 4)
    )

    // ===== 贝斯 =====
    val bassPresets: List<Tuning> = listOf(
        preset("标准 EADG", "E" to 1, "A" to 1, "D" to 2, "G" to 2),
        preset("Drop D", "D" to 1, "A" to 1, "D" to 2, "G" to 2),
        preset("降半音", "D#" to 1, "G#" to 1, "C#" to 2, "F#" to 2),
        preset("五弦 B 标准", "B" to 0, "E" to 1, "A" to 1, "D" to 2, "G" to 2)
    )

    // ===== 小提琴 =====
    val violinPresets: List<Tuning> = listOf(
        preset("标准 GDAE", "G" to 3, "D" to 4, "A" to 4, "E" to 5),
        preset("降半音", "F#" to 3, "C#" to 4, "G#" to 4, "D#" to 5)
    )

    // ===== 中提琴 =====
    val violaPresets: List<Tuning> = listOf(
        preset("标准 CGDA", "C" to 3, "G" to 3, "D" to 4, "A" to 4)
    )

    // ===== 大提琴 =====
    val celloPresets: List<Tuning> = listOf(
        preset("标准 CGDA", "C" to 2, "G" to 2, "D" to 3, "A" to 3)
    )

    // ===== 二胡 =====
    val erhuPresets: List<Tuning> = listOf(
        preset("D-A（标准）", "D" to 4, "A" to 4),
        preset("C-G", "C" to 4, "G" to 4),
        preset("G-D", "G" to 3, "D" to 4)
    )

    // ===== 古筝（21 弦，五声音阶）=====
    val guzhengPresets: List<Tuning> = listOf(
        pentatonic("D调（标准）", Note.of("D", 2), 21),
        pentatonic("G调", Note.of("G", 2), 21)
    )

    fun presetsFor(instrument: Instrument): List<Tuning> = when (instrument) {
        Instrument.GUITAR -> guitarPresets
        Instrument.BASS -> bassPresets
        Instrument.VIOLIN -> violinPresets
        Instrument.VIOLA -> violaPresets
        Instrument.CELLO -> celloPresets
        Instrument.ERHU -> erhuPresets
        Instrument.GUZHENG -> guzhengPresets
    }

    fun defaultFor(instrument: Instrument): Tuning = presetsFor(instrument).first()

    /** 便捷构造：按顺序给定每根弦的音名与八度（从低音到高音），内部转为标准弦号。 */
    private fun preset(name: String, vararg notes: Pair<String, Int>): Tuning {
        val n = notes.size
        val strings = notes.mapIndexed { i, (nm, o) -> GuitarString(n - i, Note.of(nm, o)) }.reversed()
        return Tuning(name, strings)
    }

    /** 五声音阶调弦（古筝）：大调五声 = 根音上方 0,2,4,7,9 半音，从低到高铺满 count 根弦。 */
    private fun pentatonic(name: String, root: Note, count: Int): Tuning {
        val intervals = intArrayOf(0, 2, 4, 7, 9)
        val notes = mutableListOf<Note>()
        var base = root.midi
        while (notes.size < count) {
            for (iv in intervals) {
                if (notes.size < count) notes.add(Note.of(base + iv))
            }
            base += 12
        }
        val n = notes.size
        val strings = notes.mapIndexed { i, note -> GuitarString(n - i, note) }.reversed()
        return Tuning(name, strings)
    }
}
