package com.guitartuner.tuning

/**
 * 支持的乐器类型。每种乐器有自己的一套预设调弦。
 */
enum class Instrument(val displayName: String) {
    GUITAR("吉他"),
    BASS("贝斯"),
    VIOLIN("小提琴"),
    VIOLA("中提琴"),
    CELLO("大提琴"),
    ERHU("二胡"),
    GUZHENG("古筝")
}
