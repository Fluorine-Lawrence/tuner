package com.guitartuner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.guitartuner.audio.AudioEngine
import com.guitartuner.audio.PitchResult
import com.guitartuner.tuner.Hint
import com.guitartuner.tuner.Mode
import com.guitartuner.tuner.TunerEngine
import com.guitartuner.tuner.TunerReading
import com.guitartuner.tuning.Instrument
import com.guitartuner.tuning.TuningStore
import com.guitartuner.ui.TunerGaugeView
import com.guitartuner.ui.TuningListActivity
import kotlin.math.abs

/**
 * 主界面：乐器切换 + 实时调音 + 模式切换（智能/手动）+ 纠错提示 + 弦选择 + 调弦入口。
 */
class MainActivity : Activity() {

    private lateinit var gauge: TunerGaugeView
    private lateinit var noteText: TextView
    private lateinit var detailText: TextView
    private lateinit var freqText: TextView
    private lateinit var centsText: TextView
    private lateinit var hintText: TextView
    private lateinit var modeButton: Button
    private lateinit var tuningButton: Button
    private lateinit var instrumentRow: LinearLayout
    private lateinit var stringRow: LinearLayout

    private val tuner = TunerEngine()
    private lateinit var audio: AudioEngine
    private val stringViews = mutableListOf<TextView>()
    private val instrumentViews = mutableListOf<TextView>()

    private lateinit var prefs: SharedPreferences
    private var currentInstrument = Instrument.GUITAR

    // 频率指数平滑，抑制拨弦瞬间的高频抖动
    private var smoothedFreq = 0.0
    private var hasSmoothed = false

    private val goodColor by lazy { getColor(R.color.good) }
    private val onAccentColor by lazy { getColor(R.color.on_accent) }
    private val textPrimaryColor by lazy { getColor(R.color.text_primary) }
    private val secondaryColor by lazy { getColor(R.color.text_secondary) }
    private val warnColor by lazy { getColor(R.color.warn) }
    private val dangerColor by lazy { getColor(R.color.danger) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        bindViews()
        audio = AudioEngine(::onPitch)

        loadState()
        renderInstruments()
        renderStrings()
        updateModeButton()
        updateTuningButton()

        modeButton.setOnClickListener { toggleMode() }
        tuningButton.setOnClickListener {
            startActivity(Intent(this, TuningListActivity::class.java))
        }

        requestMicPermission()
    }

    override fun onResume() {
        super.onResume()
        // 从调弦列表/自定义编辑返回后刷新
        currentInstrument = TuningStore.currentInstrument(this)
        tuner.tuning = TuningStore.currentTuning(this)
        renderInstruments()
        renderStrings()
        updateTuningButton()
        if (hasMicPermission()) audio.start()
    }

    override fun onPause() {
        super.onPause()
        audio.stop()
    }

    // ---- 初始化 ----

    private fun bindViews() {
        gauge = findViewById(R.id.gauge)
        noteText = findViewById(R.id.noteText)
        detailText = findViewById(R.id.detailText)
        freqText = findViewById(R.id.freqText)
        centsText = findViewById(R.id.centsText)
        hintText = findViewById(R.id.hintText)
        modeButton = findViewById(R.id.modeButton)
        tuningButton = findViewById(R.id.tuningButton)
        instrumentRow = findViewById(R.id.instrumentRow)
        stringRow = findViewById(R.id.stringRow)
    }

    private fun loadState() {
        currentInstrument = TuningStore.currentInstrument(this)
        tuner.tuning = TuningStore.currentTuning(this)
        tuner.mode = if (prefs.getString("mode", Mode.SMART.name) == Mode.MANUAL.name)
            Mode.MANUAL else Mode.SMART
        val lowest = tuner.tuning.strings.last().number
        val saved = prefs.getInt("manual_string", lowest)
        tuner.manualString = saved.coerceIn(1, lowest)
    }

    // ---- 乐器选择条 ----

    private fun renderInstruments() {
        instrumentRow.removeAllViews()
        instrumentViews.clear()
        for (inst in Instrument.values()) {
            val tv = TextView(this).apply {
                text = inst.displayName
                gravity = Gravity.CENTER
                textSize = 14f
                val padH = (14 * resources.displayMetrics.density).toInt()
                val padV = (9 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
                setOnClickListener { selectInstrument(inst) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
        TuningStore.setCurrentInstrument(this, inst)
        tuner.tuning = TuningStore.currentTuning(this)
        tuner.manualString = tuner.tuning.strings.last().number
        highlightInstrument()
        renderStrings()
        updateTuningButton()
        saveMode()
    }

    // ---- 弦选择条 ----

    private fun renderStrings() {
        stringRow.removeAllViews()
        stringViews.clear()
        val many = tuner.tuning.stringCount > 8
        val density = resources.displayMetrics.density
        for (s in tuner.tuning.strings) {
            val tv = TextView(this).apply {
                text = "${s.number}弦\n${s.note.fullName}"
                gravity = Gravity.CENTER
                textSize = if (many) 10f else 12f
                val pad = (if (many) 5 else 8) * density
                val padInt = pad.toInt()
                setPadding(padInt, padInt, padInt, padInt)
                setBackgroundResource(R.drawable.bg_string_cell)
                setTextColor(secondaryColor)
                setOnClickListener {
                    tuner.mode = Mode.MANUAL
                    tuner.manualString = s.number
                    updateModeButton()
                    highlightString(s.number)
                    saveMode()
                }
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginEnd = (4 * density).toInt()
            stringRow.addView(tv, lp)
            stringViews.add(tv)
        }
        highlightString(null)
    }

    private fun highlightString(active: Int?) {
        for (i in stringViews.indices) {
            val num = tuner.tuning.strings[i].number
            val isActive = num == active
            val tv = stringViews[i]
            tv.setBackgroundResource(
                if (isActive) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
            )
            tv.setTextColor(if (isActive) onAccentColor else secondaryColor)
            tv.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    // ---- 模式切换 ----

    private fun updateModeButton() {
        modeButton.text = if (tuner.mode == Mode.SMART) "智能模式" else "手动模式"
    }

    private fun updateTuningButton() {
        tuningButton.text = tuner.tuning.name
    }

    private fun toggleMode() {
        tuner.mode = if (tuner.mode == Mode.SMART) Mode.MANUAL else Mode.SMART
        updateModeButton()
        saveMode()
    }

    private fun saveMode() {
        prefs.edit()
            .putString("mode", tuner.mode.name)
            .putInt("manual_string", tuner.manualString)
            .apply()
    }

    // ---- 实时回调（主线程） ----

    private fun onPitch(result: PitchResult?) {
        if (result == null) {
            hasSmoothed = false
            gauge.setActive(false)
            highlightString(null)
            noteText.setTextColor(textPrimaryColor)
            setHint("请拨动琴弦…", secondaryColor)
            return
        }

        // 指数平滑频率：抑制拨弦瞬间的冲高/抖动，让指针更稳
        val f = result.frequency.toDouble()
        if (!hasSmoothed) {
            smoothedFreq = f
            hasSmoothed = true
        } else {
            smoothedFreq += SMOOTH_ALPHA * (f - smoothedFreq)
        }

        val r = tuner.process(PitchResult(smoothedFreq.toFloat(), result.confidence))
        gauge.setActive(true)

        noteText.text = r.note.name
        detailText.text = if (r.targetString != null && r.targetNote != null) {
            "第${r.targetString}弦 · ${r.targetNote.fullName}"
        } else {
            r.note.fullName
        }
        freqText.text = "%.1f Hz".format(r.frequency)

        val c = r.centsFromTarget
        centsText.text = (if (c >= 0) "+" else "-") + "%.1f".format(abs(c)) + " ¢"

        gauge.setCents(c)
        highlightString(r.targetString)
        noteText.setTextColor(if (r.hint == Hint.IN_TUNE) goodColor else textPrimaryColor)
        renderHint(r)
    }

    private fun renderHint(r: TunerReading) {
        val (text, color) = when (r.hint) {
            Hint.LISTENING -> "请拨动琴弦…" to secondaryColor
            Hint.IN_TUNE -> "✓ 已调准" to goodColor
            Hint.SHARP -> "偏高，请调低" to warnColor
            Hint.FLAT -> "偏低，请调高" to warnColor
            Hint.WRONG_DIRECTION -> "⚠ 拧反方向了，反着拧" to dangerColor
            Hint.WRONG_STRING -> "⚠ 可能调错弦：这听起来更像第 ${r.matchedString} 弦" to dangerColor
            Hint.NO_MATCH -> "未匹配到琴弦，请检查调弦设定" to dangerColor
        }
        setHint(text, color)
    }

    private fun setHint(text: String, color: Int) {
        hintText.text = text
        hintText.setTextColor(color)
        hintText.background.mutate().setTint(withAlpha(color, 34))
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    // ---- 麦克风权限 ----

    private fun requestMicPermission() {
        if (hasMicPermission()) {
            audio.start()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
        }
    }

    private fun hasMicPermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                audio.start()
            } else {
                setHint("需要麦克风权限才能调音", dangerColor)
            }
        }
    }

    companion object {
        private const val REQ_MIC = 100
        private const val SMOOTH_ALPHA = 0.3
    }
}
