package com.guitartuner.practice.model

/**
 * 曲目状态：想演奏 → 正在练 → 已拿下。
 * 按 index 顺序循环切换。
 */
enum class PieceStatus(val displayName: String) {
    WANT("想演奏"),
    LEARNING("正在练"),
    MASTERED("已拿下");

    fun next(): PieceStatus = values()[(ordinal + 1) % values().size]

    companion object {
        fun fromName(name: String): PieceStatus =
            values().firstOrNull { it.name == name } ?: WANT
    }
}
