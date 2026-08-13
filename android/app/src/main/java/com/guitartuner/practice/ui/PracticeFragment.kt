package com.guitartuner.practice.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.guitartuner.R
import com.guitartuner.practice.TimerController
import com.guitartuner.practice.db.PracticeDb
import com.guitartuner.practice.model.Piece
import com.guitartuner.practice.model.PieceStatus
import com.guitartuner.practice.model.PracticeSession
import com.guitartuner.tuning.Instrument
import com.guitartuner.ui.Dialogs
import java.util.Locale

/**
 * 练习板块：乐器选择 + 完整计时器 + 当前曲目 + 曲库/统计入口。
 */
class PracticeFragment : Fragment() {

    private lateinit var db: PracticeDb
    private val timer = TimerController()

    private lateinit var instrumentRow: LinearLayout
    private lateinit var timerText: TextView
    private lateinit var currentPieceText: TextView
    private lateinit var todayBriefText: TextView
    private lateinit var startStopBtn: Button
    private lateinit var pauseResumeBtn: Button

    private val instrumentViews = mutableListOf<TextView>()
    private var currentInstrument = Instrument.GUITAR
    private var currentPiece: Piece? = null
    private var pieceChosen = false

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            timerText.text = formatDuration(timer.elapsedMs)
            if (timer.isRunning) tickHandler.postDelayed(this, 500)
        }
    }

    private val primaryColor by lazy { requireContext().getColor(R.color.primary) }
    private val accentColor by lazy { requireContext().getColor(R.color.accent) }
    private val onAccentColor by lazy { requireContext().getColor(R.color.on_accent) }
    private val textPrimaryColor by lazy { requireContext().getColor(R.color.text_primary) }
    private val secondaryColor by lazy { requireContext().getColor(R.color.text_secondary) }
    private val textDimColor by lazy { requireContext().getColor(R.color.text_dim) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_practice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = PracticeDb(requireContext())

        instrumentRow = view.findViewById(R.id.practiceInstrumentRow)
        timerText = view.findViewById(R.id.timerText)
        currentPieceText = view.findViewById(R.id.currentPieceText)
        todayBriefText = view.findViewById(R.id.todayBriefText)
        startStopBtn = view.findViewById(R.id.startStopBtn)
        pauseResumeBtn = view.findViewById(R.id.pauseResumeBtn)

        renderInstruments()

        startStopBtn.setOnClickListener {
            if (timer.isRunning || timer.isPaused) finishSession() else startSession()
        }
        pauseResumeBtn.setOnClickListener {
            if (timer.isRunning) {
                timer.pause()
                showPausedState()
            } else if (timer.isPaused) {
                timer.start()
                showRunningState()
                tickHandler.post(tickRunnable)
            }
        }

        view.findViewById<View>(R.id.repertoireBtn).setOnClickListener {
            startActivity(Intent(requireContext(), RepertoireActivity::class.java))
        }
        view.findViewById<View>(R.id.statsBtn).setOnClickListener {
            startActivity(Intent(requireContext(), StatsActivity::class.java))
        }
        view.findViewById<Button>(R.id.pickPieceBtn).setOnClickListener { showPiecePicker() }
    }

    private fun showPiecePicker() {
        val pieces = db.allPieces(currentInstrument.name)
        val items = mutableListOf<Pair<String, String?>>("自由练习（不选曲目）" to null)
        items += pieces.map { it.name to it.status.displayName }
        Dialogs.pickList(requireContext(), "选择曲目", items) { idx ->
            pieceChosen = true
            if (idx == 0) {
                currentPiece = null
                currentPieceText.text = "自由练习（未选曲目）"
            } else {
                currentPiece = pieces[idx - 1]
                currentPieceText.text = "正在练：${currentPiece!!.name}"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentPiece = null
        pieceChosen = false
        renderInstruments()
        refreshTodayBrief()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tickHandler.removeCallbacks(tickRunnable)
        if (timer.isRunning || timer.isPaused) timer.stop()
        db.close()
    }

    private fun refreshTodayBrief() {
        val min = db.todayDuration(currentInstrument.name) / 60000
        todayBriefText.text = if (min >= 60) "今日 ${min / 60}h${min % 60}m" else "今日 ${min}分"
    }

    private fun renderInstruments() {
        instrumentRow.removeAllViews()
        instrumentViews.clear()
        val ctx = requireContext()
        for (inst in Instrument.values()) {
            val tv = TextView(ctx).apply {
                text = inst.displayName
                gravity = Gravity.CENTER
                textSize = 14f
                val padH = (14 * resources.displayMetrics.density).toInt()
                val padV = (9 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
                setOnClickListener { selectInstrument(inst) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * resources.displayMetrics.density).toInt()
            instrumentRow.addView(tv, lp)
            instrumentViews.add(tv)
        }
        highlightInstrument()
    }

    private fun highlightInstrument() {
        val values = Instrument.values()
        for (i in instrumentViews.indices) {
            val selected = values[i] == currentInstrument
            val tv = instrumentViews[i]
            tv.setBackgroundResource(
                if (selected) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
            )
            tv.setTextColor(if (selected) onAccentColor else secondaryColor)
            tv.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun selectInstrument(inst: Instrument) {
        if (inst == currentInstrument) return
        currentInstrument = inst
        currentPiece = null
        pieceChosen = false
        currentPieceText.text = "未选择曲目"
        highlightInstrument()
        refreshTodayBrief()
    }

    private fun startSession() {
        if (!pieceChosen) currentPiece = pickPiece()
        currentPieceText.text = if (currentPiece != null)
            "正在练：${currentPiece!!.name}" else "自由练习（未选曲目）"
        timer.start()
        showRunningState()
        tickHandler.post(tickRunnable)
    }

    /** 运行中：主按钮变红「结束」，暂停可用，计时数字青绿 */
    private fun showRunningState() {
        startStopBtn.text = "■  结束练习"
        startStopBtn.setBackgroundResource(R.drawable.bg_button_danger)
        startStopBtn.setTextColor(textPrimaryColor)
        pauseResumeBtn.text = "⏸  暂停"
        pauseResumeBtn.isEnabled = true
        pauseResumeBtn.setTextColor(accentColor)
        timerText.setTextColor(primaryColor)
    }

    /** 已暂停：主按钮仍「结束」，暂停按钮变「继续」 */
    private fun showPausedState() {
        startStopBtn.text = "■  结束练习"
        pauseResumeBtn.text = "▶  继续"
        pauseResumeBtn.setTextColor(primaryColor)
        timerText.setTextColor(secondaryColor)
    }

    private fun finishSession() {
        val result = timer.stop()
        tickHandler.removeCallbacks(tickRunnable)
        timerText.text = formatDuration(result.durationMs)

        if (result.durationMs < 10000) {
            Dialogs.confirm(requireContext(), "练习太短", "练习不足 10 秒，不记录本次。", "好", "好") {
                resetAfterFinish()
            }
            resetAfterFinish()
            return
        }
        showLogDialog(result)
    }

    private fun showLogDialog(result: TimerController.Result) {
        Dialogs.input(
            requireContext(),
            "本次练习 ${formatDuration(result.durationMs)}",
            "写点练习日志（可选）"
        ) { note ->
            db.insertSession(PracticeSession(
                instrument = currentInstrument.name,
                pieceId = currentPiece?.id,
                startTime = result.wallStart,
                endTime = result.wallEnd,
                durationMs = result.durationMs,
                note = note
            ))
            resetAfterFinish()
        }
    }

    private fun resetAfterFinish() {
        startStopBtn.text = "▶  开始练习"
        startStopBtn.setBackgroundResource(R.drawable.bg_button_primary)
        startStopBtn.setTextColor(onAccentColor)
        pauseResumeBtn.text = "⏸  暂停"
        pauseResumeBtn.isEnabled = false
        pauseResumeBtn.setTextColor(textDimColor)
        timerText.setTextColor(textPrimaryColor)
        timerText.text = formatDuration(0)
        currentPiece = null
        pieceChosen = false
        currentPieceText.text = "未选择曲目"
        refreshTodayBrief()
    }

    private fun pickPiece(): Piece? =
        db.allPieces(currentInstrument.name)
            .filter { it.status == PieceStatus.LEARNING }
            .firstOrNull()

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
