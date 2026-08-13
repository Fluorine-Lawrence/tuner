package com.guitartuner.tuning

/**
 * 一套调弦。strings 按从低音弦到高音弦的顺序排列。
 */
data class Tuning(
    val name: String,
    val strings: List<GuitarString>,
    val isCustom: Boolean = false
) {
    val stringCount: Int get() = strings.size

    override fun toString(): String = name
}
