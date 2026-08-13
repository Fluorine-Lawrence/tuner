package com.guitartuner.practice.model

/**
 * 一次练习会话（或其中的一段）。完整计时会拆成多段 session 记录。
 */
data class PracticeSession(
    val id: Long = 0,
    val instrument: String,
    val pieceId: Long? = null,       // 关联曲目（可空：自由练习）
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val note: String = ""            // 练习日志
) {
    val durationMinutes: Double get() = durationMs / 60000.0
}
