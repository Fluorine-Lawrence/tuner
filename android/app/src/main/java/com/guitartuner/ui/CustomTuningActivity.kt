package com.guitartuner.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.guitartuner.R
import com.guitartuner.tuning.GuitarString
import com.guitartuner.tuning.Note
import com.guitartuner.tuning.Tuning
import com.guitartuner.tuning.TuningStore

/**
 * 自定义调弦编辑器：设置名称，逐弦选择音名 + 八度（第1弦为最低音），支持 3~8 根弦。
 * 音名/八度用自绘选择弹窗（无 Spinner）。
 */
class CustomTuningActivity : Activity() {

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val OCTAVES = arrayOf("1", "2", "3", "4", "5")

    private lateinit var nameInput: EditText
    private lateinit var container: LinearLayout
    /** 每行：音名索引、八度索引 */
    private val rows = mutableListOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_tuning)

        nameInput = findViewById(R.id.nameInput)
        container = findViewById(R.id.stringContainer)

        val editName = intent.getStringExtra("edit")
        val existing = editName?.let { TuningStore.loadCustom(this).firstOrNull { t -> t.name == it } }

        if (existing != null) {
            nameInput.setText(existing.name)
            for (s in existing.strings) addRow(s.note)
        } else {
            nameInput.setText("自定义调弦")
            val standard = listOf(
                Note.of("E", 4), Note.of("B", 3), Note.of("G", 3),
                Note.of("D", 3), Note.of("A", 2), Note.of("E", 2)
            )
            for (n in standard) addRow(n)
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { addRow(Note.of("E", 2)) }
        findViewById<Button>(R.id.btnRemove).setOnClickListener { removeRow() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }
    }

    private fun addRow(note: Note) {
        val noteIndex = NOTE_NAMES.indexOf(note.name).coerceAtLeast(0)
        val octIndex = (note.octave - 1).coerceIn(0, OCTAVES.size - 1)
        val rowIndex = rows.size

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val label = TextView(this).apply {
            text = "第${rowIndex + 1}弦"
            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
        }

        val noteView = pickerCell(NOTE_NAMES[noteIndex])
        val octaveView = pickerCell(OCTAVES[octIndex])

        noteView.setOnClickListener {
            Dialogs.pick(this, "第${rowIndex + 1}弦 · 音名", NOTE_NAMES.toList(), rows[rowIndex].first) { idx ->
                rows[rowIndex] = idx to rows[rowIndex].second
                noteView.text = NOTE_NAMES[idx]
            }
        }
        octaveView.setOnClickListener {
            Dialogs.pick(this, "第${rowIndex + 1}弦 · 八度", OCTAVES.toList(), rows[rowIndex].second) { idx ->
                rows[rowIndex] = rows[rowIndex].first to idx
                octaveView.text = OCTAVES[idx]
            }
        }

        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(noteView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(octaveView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        container.addView(row)
        rows.add(noteIndex to octIndex)
    }

    private fun pickerCell(text: String): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
            setTextColor(getColor(R.color.text_primary))
            setBackgroundResource(R.drawable.bg_string_cell)
        }
    }

    private fun removeRow() {
        if (rows.isEmpty()) return
        rows.removeAt(rows.size - 1)
        container.removeViewAt(container.childCount - 1)
    }

    private fun save() {
        if (rows.size < 3) {
            Toast.makeText(this, "至少需要 3 根弦", Toast.LENGTH_SHORT).show()
            return
        }
        val name = nameInput.text.toString().trim().ifEmpty { "自定义调弦" }
        val strings = mutableListOf<GuitarString>()
        for ((i, pair) in rows.withIndex()) {
            val noteName = NOTE_NAMES[pair.first]
            val octave = OCTAVES[pair.second].toInt()
            strings.add(GuitarString(i + 1, Note.of(noteName, octave)))
        }
        TuningStore.addOrUpdateCustom(this, Tuning(name, strings, isCustom = true))
        TuningStore.setCurrentTuning(this, name)
        Toast.makeText(this, "已保存「$name」", Toast.LENGTH_SHORT).show()
        finish()
    }
}
