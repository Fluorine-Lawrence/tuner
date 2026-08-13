package com.guitartuner.practice.model

/**
 * 曲目：用户目标曲库中的一首曲目。
 */
data class Piece(
    val id: Long = 0,
    val name: String,
    val instrument: String,          // 乐器 key（如 "guitar"）
    val status: PieceStatus = PieceStatus.WANT,
    val createdAt: Long = System.currentTimeMillis()
)
