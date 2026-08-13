package com.guitartuner.practice.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.guitartuner.R
import com.guitartuner.practice.db.PracticeDb
import com.guitartuner.tuning.Instrument
import com.guitartuner.ui.Dialogs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 统计：天/周/月/年柱状图 + 累计/今日/连续天数 + 每日目标进度。
 */
class StatsActivity : Activity() {

    private lateinit var db: PracticeDb
    private lateinit var instrumentRow: LinearLayout
    private lateinit var granularityRow: LinearLayout
    private lateinit var barChart: BarChartView
    private lateinit var totalText: TextView
    private lateinit var todayText: TextView
    private lateinit var streakText: TextView
    private lateinit var goalText: TextView
    private lateinit var goalProgress: RoundedProgressView

    private var instrument: Instrument? = null   // null = 所有乐器
    private var granularity = PracticeDb.Granularity.DAY

    private val instrumentViews = mutableListOf<TextView>()
    private val granViews = mutableListOf<TextView>()

    private val onAccentColor by lazy { getColor(R.color.on_accent) }
    private val textPrimaryColor by lazy { getColor(R.color.text_primary) }
    private val secondaryColor by lazy { getColor(R.color.text_secondary) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        db = PracticeDb(this)

        instrumentRow = findViewById(R.id.statsInstrumentRow)
        granularityRow = findViewById(R.id.granularityRow)
        barChart = findViewById(R.id.barChart)
        totalText = findViewById(R.id.totalText)
        todayText = findViewById(R.id.todayText)
        streakText = findViewById(R.id.streakText)
        goalText = findViewById(R.id.goalText)
        goalProgress = findViewById(R.id.goalProgress)

        findViewById<TextView>(R.id.goalEditBtn).setOnClickListener { editGoal() }

        renderInstruments()
        renderGranularity()
        refresh()
    }

    private fun renderInstruments() {
        instrumentRow.removeAllViews()
        instrumentViews.clear()
        val items = listOf<Pair<String, Instrument?>>("全部" to null) +
            Instrument.values().map { it.displayName to it }
        for ((label, inst) in items) {
            val tv = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                val padH = (12 * resources.displayMetrics.density).toInt()
                val padV = (7 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
                setOnClickListener {
                    instrument = inst
                    highlightInstruments()
                    refresh()
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * resources.displayMetrics.density).toInt()
            instrumentRow.addView(tv, lp)
            instrumentViews.add(tv)
        }
        highlightInstruments()
    }

    private fun highlightInstruments() {
        val sel = if (instrument == null) 0
        else Instrument.values().indexOf(instrument) + 1
        for (i in instrumentViews.indices) {
            val tv = instrumentViews[i]
            val on = i == sel
            tv.setBackgroundResource(
                if (on) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
            )
            tv.setTextColor(if (on) onAccentColor else secondaryColor)
        }
    }

    private fun renderGranularity() {
        granularityRow.removeAllViews()
        granViews.clear()
        val items = listOf(
            "天" to PracticeDb.Granularity.DAY,
            "周" to PracticeDb.Granularity.WEEK,
            "月" to PracticeDb.Granularity.MONTH,
            "年" to PracticeDb.Granularity.YEAR
        )
        for ((label, g) in items) {
            val tv = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 15f
                val pad = (14 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    granularity = g
                    highlightGranularity()
                    refresh()
                }
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            granularityRow.addView(tv, lp)
            granViews.add(tv)
        }
        highlightGranularity()
    }

    private fun highlightGranularity() {
        val order = listOf(
            PracticeDb.Granularity.DAY, PracticeDb.Granularity.WEEK,
            PracticeDb.Granularity.MONTH, PracticeDb.Granularity.YEAR
        )
        val sel = order.indexOf(granularity)
        for (i in granViews.indices) {
            val tv = granViews[i]
            val on = i == sel
            tv.setBackgroundResource(
                if (on) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
            )
            tv.setTextColor(if (on) onAccentColor else secondaryColor)
            tv.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun refresh() {
        val instName = instrument?.name

        // 激励数字
        totalText.text = fmtDuration(db.totalDuration(instName))
        todayText.text = fmtDuration(db.todayDuration(instName))
        streakText.text = "${db.streakDays(instName)}天"

        // 目标进度
        val goal = if (instName != null) db.getGoalMinutes(instName) else 0
        if (goal > 0) {
            val todayMin = db.todayDuration(instName) / 60000.0
            goalText.text = "今日目标：${todayMin.toInt()}/${goal} 分钟"
            goalProgress.setProgress((todayMin / goal).toFloat())
        } else {
            goalText.text = "今日目标：未设置"
            goalProgress.setProgress(0f)
        }

        // 柱状图
        val buckets = when (granularity) {
            PracticeDb.Granularity.DAY -> 30
            PracticeDb.Granularity.WEEK -> 12
            PracticeDb.Granularity.MONTH -> 12
            PracticeDb.Granularity.YEAR -> 5
        }
        val stats = db.stats(instName, granularity, buckets)
        val items = stats.map { b ->
            BarChartView.Item(label = bucketLabel(b.startTime, granularity), durationMs = b.durationMs)
        }
        barChart.setData(items)
    }

    private fun bucketLabel(startTime: Long, g: PracticeDb.Granularity): String {
        val d = Date(startTime)
        return when (g) {
            PracticeDb.Granularity.DAY -> SimpleDateFormat("MM/dd", Locale.US).format(d)
            PracticeDb.Granularity.WEEK -> "W" + SimpleDateFormat("ww", Locale.US).format(d)
            PracticeDb.Granularity.MONTH -> SimpleDateFormat("yy/MM", Locale.US).format(d)
            PracticeDb.Granularity.YEAR -> SimpleDateFormat("yyyy", Locale.US).format(d)
        }
    }

    private fun editGoal() {
        if (instrument == null) {
            Toast.makeText(this, "请先选择一个具体乐器再设置目标", Toast.LENGTH_SHORT).show()
            return
        }
        val inst = instrument!!
        // 分钟选项：10 ~ 180，步进 10
        val minutes = (1..18).map { it * 10 }
        val labels = minutes.map { "${it} 分钟" }
        val current = db.getGoalMinutes(inst.name)
        val currentIndex = minutes.indexOf(current).takeIf { it >= 0 } ?: 2 // 默认 30 分钟
        Dialogs.pickWheel(this, "每日练习目标（${inst.displayName}）", labels, currentIndex) { idx ->
            db.setGoalMinutes(inst.name, minutes[idx])
            refresh()
        }
    }

    private fun fmtDuration(ms: Long): String {
        val totalMin = ms / 60000
        return if (totalMin >= 60) "${totalMin / 60}h${totalMin % 60}m" else "${totalMin}分"
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}
