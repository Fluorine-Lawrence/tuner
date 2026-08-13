package com.guitartuner.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.guitartuner.R
import com.guitartuner.tuning.GuitarString
import com.guitartuner.tuning.Note
import com.guitartuner.tuning.Tuning
import com.guitartuner.tuning.TuningStore

/**
 * 自定义调弦编辑器：设置名称，逐弦选择音名 + 八度（第1弦为最低音），支持 3~8 根弦。
 */
class CustomTuningActivity : Activity() {

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val OCTAVES = arrayOf("1", "2", "3", "4", "5")

    private lateinit var nameInput: EditText
    private lateinit var container: LinearLayout
    private val rows = mutableListOf<Pair<Spinner, Spinner>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_tuning)

        nameInput = findViewById(R.id.nameInput)
        container = findViewById(R.id.stringContainer)

        // 编辑已有自定义调弦（可选）
        val editName = intent.getStringExtra("edit")
        val existing = editName?.let { TuningStore.loadCustom(this).firstOrNull { t -> t.name == it } }

        if (existing != null) {
            nameInput.setText(existing.name)
            for (s in existing.strings) addRow(s.note)
        } else {
            nameInput.setText("自定义调弦")
            // 默认 6 弦标准调弦（按标准弦号：1弦最高 E4 → 6弦最低 E2）
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
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val label = TextView(this).apply {
            text = "第${rows.size + 1}弦"
            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
        }
        val noteSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CustomTuningActivity, android.R.layout.simple_spinner_item, NOTE_NAMES
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(NOTE_NAMES.indexOf(note.name).coerceAtLeast(0))
        }
        val octaveSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CustomTuningActivity, android.R.layout.simple_spinner_item, OCTAVES
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection((note.octave - 1).coerceIn(0, OCTAVES.size - 1))
        }

        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(noteSpinner, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(octaveSpinner, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        container.addView(row)
        rows.add(noteSpinner to octaveSpinner)
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
            val noteName = NOTE_NAMES[pair.first.selectedItemPosition]
            val octave = OCTAVES[pair.second.selectedItemPosition].toInt()
            strings.add(GuitarString(i + 1, Note.of(noteName, octave)))
        }
        TuningStore.addOrUpdateCustom(this, Tuning(name, strings, isCustom = true))
        TuningStore.setCurrentTuning(this, name)
        Toast.makeText(this, "已保存「$name」", Toast.LENGTH_SHORT).show()
        finish()
    }
}
