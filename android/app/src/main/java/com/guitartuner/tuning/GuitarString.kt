package com.guitartuner.tuning

/**
 * 一根弦。number 采用标准吉他弦号：
 * 第 1 弦 = 最高音、最细的那根；第 N 弦 = 最低音、最粗的那根。
 * （标准 6 弦吉他：1弦=E4 最高，6弦=E2 最低。）
 */
data class GuitarString(val number: Int, val note: Note) {
    val frequency: Double get() = note.frequency
}
