package com.guitartuner.practice.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.guitartuner.practice.model.Piece
import com.guitartuner.practice.model.PieceStatus
import com.guitartuner.practice.model.PracticeSession
import java.util.Calendar

/**
 * 练习数据存储：SQLite，三张表。
 *  - piece：曲目库
 *  - session：练习会话（完整计时拆成多段）
 *  - goal：每日练习目标（按乐器）
 * 提供 DAO + 天/周/月/年聚合统计查询。
 */
class PracticeDb(context: Context) :
    SQLiteOpenHelper(context, "practice.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE piece(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                instrument TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE session(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                instrument TEXT NOT NULL,
                piece_id INTEGER,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT ''
            )"""
        )
        db.execSQL(
            """CREATE TABLE goal(
                instrument TEXT PRIMARY KEY,
                target_minutes INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 无升级逻辑
    }

    // ================= 曲目 =================

    fun insertPiece(p: Piece): Long {
        val cv = ContentValues().apply {
            put("name", p.name)
            put("instrument", p.instrument)
            put("status", p.status.name)
            put("created_at", p.createdAt)
        }
        return writableDatabase.insertOrThrow("piece", null, cv)
    }

    fun updatePieceStatus(id: Long, status: PieceStatus) {
        val cv = ContentValues().apply { put("status", status.name) }
        writableDatabase.update("piece", cv, "id=?", arrayOf(id.toString()))
    }

    fun updatePiece(p: Piece) {
        val cv = ContentValues().apply {
            put("name", p.name)
            put("instrument", p.instrument)
            put("status", p.status.name)
        }
        writableDatabase.update("piece", cv, "id=?", arrayOf(p.id.toString()))
    }

    fun deletePiece(id: Long) {
        writableDatabase.delete("piece", "id=?", arrayOf(id.toString()))
    }

    fun allPieces(instrument: String? = null): List<Piece> {
        val sql = if (instrument != null)
            "SELECT * FROM piece WHERE instrument=? ORDER BY created_at DESC"
        else "SELECT * FROM piece ORDER BY created_at DESC"
        val args = if (instrument != null) arrayOf(instrument) else null
        return queryPieces(sql, args)
    }

    private fun queryPieces(sql: String, args: Array<String>?): List<Piece> {
        val list = mutableListOf<Piece>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                list.add(Piece(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    instrument = c.getString(c.getColumnIndexOrThrow("instrument")),
                    status = PieceStatus.fromName(c.getString(c.getColumnIndexOrThrow("status"))),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                ))
            }
        }
        return list
    }

    // ================= 练习会话 =================

    fun insertSession(s: PracticeSession): Long {
        val cv = ContentValues().apply {
            put("instrument", s.instrument)
            if (s.pieceId != null) put("piece_id", s.pieceId) else putNull("piece_id")
            put("start_time", s.startTime)
            put("end_time", s.endTime)
            put("duration_ms", s.durationMs)
            put("note", s.note)
        }
        return writableDatabase.insertOrThrow("session", null, cv)
    }

    fun recentSessions(limit: Int = 50): List<PracticeSession> {
        val list = mutableListOf<PracticeSession>()
        readableDatabase.rawQuery(
            "SELECT * FROM session ORDER BY start_time DESC LIMIT ?", arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(readSession(c))
            }
        }
        return list
    }

    private fun readSession(c: android.database.Cursor): PracticeSession {
        val pieceIdx = c.getColumnIndex("piece_id")
        return PracticeSession(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            instrument = c.getString(c.getColumnIndexOrThrow("instrument")),
            pieceId = if (c.isNull(pieceIdx)) null else c.getLong(pieceIdx),
            startTime = c.getLong(c.getColumnIndexOrThrow("start_time")),
            endTime = c.getLong(c.getColumnIndexOrThrow("end_time")),
            durationMs = c.getLong(c.getColumnIndexOrThrow("duration_ms")),
            note = c.getString(c.getColumnIndexOrThrow("note"))
        )
    }

    // ================= 每日目标 =================

    fun getGoalMinutes(instrument: String): Int {
        readableDatabase.rawQuery(
            "SELECT target_minutes FROM goal WHERE instrument=?", arrayOf(instrument)
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun setGoalMinutes(instrument: String, minutes: Int) {
        val cv = ContentValues().apply {
            put("instrument", instrument)
            put("target_minutes", minutes)
        }
        writableDatabase.insertWithOnConflict("goal", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ================= 统计聚合 =================

    /** 统计粒度 */
    enum class Granularity { DAY, WEEK, MONTH, YEAR }

    /** 一条聚合结果：bucket 起始时间戳 + 该粒度内的总时长(ms) */
    data class StatBucket(val startTime: Long, val durationMs: Long)

    /**
     * 按粒度聚合练习时长。
     * @param instrument 乐器（null = 所有）
     * @param granularity 天/周/月/年
     * @param buckets 返回的桶数量
     */
    fun stats(
        instrument: String?,
        granularity: Granularity,
        buckets: Int
    ): List<StatBucket> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

        // 根据粒度回退到「最近 buckets 个」的起点
        when (granularity) {
            Granularity.DAY -> cal.add(Calendar.DAY_OF_YEAR, -(buckets - 1))
            Granularity.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.add(Calendar.WEEK_OF_YEAR, -(buckets - 1))
            }
            Granularity.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -(buckets - 1))
            }
            Granularity.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.YEAR, -(buckets - 1))
            }
        }

        val results = mutableListOf<StatBucket>()
        for (i in 0 until buckets) {
            val bucketStart = cal.timeInMillis
            val bucketEnd = nextBucketStart(cal, granularity)
            val dur = sumDuration(instrument, bucketStart, bucketEnd)
            results.add(StatBucket(bucketStart, dur))
            cal.timeInMillis = bucketEnd
        }
        return results
    }

    /** 累计总时长（可选按乐器过滤） */
    fun totalDuration(instrument: String? = null): Long {
        val sql = if (instrument != null)
            "SELECT COALESCE(SUM(duration_ms),0) FROM session WHERE instrument=?"
        else "SELECT COALESCE(SUM(duration_ms),0) FROM session"
        val args = if (instrument != null) arrayOf(instrument) else null
        readableDatabase.rawQuery(sql, args).use { c ->
            c.moveToFirst()
            return c.getLong(0)
        }
    }

    /** 今日总时长（可选按乐器过滤） */
    fun todayDuration(instrument: String? = null): Long {
        val start = dayStart(System.currentTimeMillis())
        val end = start + 86400000L
        return sumDuration(instrument, start, end)
    }

    /** 连续练习天数（从今天往前，连续有记录的） */
    fun streakDays(instrument: String? = null): Int {
        val where = if (instrument != null) "instrument=?" else "1=1"
        val args = if (instrument != null) arrayOf(instrument) else null
        val days = sortedSetOf<Long>()
        readableDatabase.rawQuery("SELECT start_time FROM session WHERE $where", args).use { c ->
            while (c.moveToNext()) {
                days.add(dayStart(c.getLong(0)))
            }
        }
        var streak = 0
        var cursor = dayStart(System.currentTimeMillis())
        while (days.contains(cursor)) {
            streak++
            cursor -= 86400000L
        }
        return streak
    }

    // ---- 内部工具 ----

    private fun sumDuration(instrument: String?, start: Long, end: Long): Long {
        val where = StringBuilder("start_time >= ? AND start_time < ?")
        val args = mutableListOf(start.toString(), end.toString())
        if (instrument != null) {
            where.append(" AND instrument=?")
            args.add(instrument)
        }
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(duration_ms),0) FROM session WHERE $where",
            args.toTypedArray()
        ).use { c ->
            c.moveToFirst()
            return c.getLong(0)
        }
    }

    private fun nextBucketStart(cal: Calendar, g: Granularity): Long {
        return when (g) {
            Granularity.DAY -> cal.timeInMillis + 86400000L
            Granularity.WEEK -> cal.timeInMillis + 7L * 86400000L
            Granularity.MONTH -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.MONTH, 1)
                c.timeInMillis
            }
            Granularity.YEAR -> {
                val c = cal.clone() as Calendar
                c.add(Calendar.YEAR, 1)
                c.timeInMillis
            }
        }
    }

    private fun dayStart(ts: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
