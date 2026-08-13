package com.guitartuner.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.guitartuner.R
import com.guitartuner.tuning.Tuning
import com.guitartuner.tuning.TuningStore

/**
 * 调弦选择列表：展示内置预设 + 用户自定义调弦，可新建自定义、长按删除自定义。
 */
class TuningListActivity : Activity() {

    private lateinit var listView: ListView
    private var tunings: List<Tuning> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuning_list)

        listView = findViewById(R.id.listView)

        findViewById<Button>(R.id.btnNew).setOnClickListener {
            startActivity(Intent(this, CustomTuningActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val t = tunings[pos]
            TuningStore.setCurrentTuning(this, t.name)
            finish()
        }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val t = tunings[pos]
            if (t.isCustom) confirmDelete(t)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        tunings = TuningStore.allTunings(this, TuningStore.currentInstrument(this))
        val adapter = object : ArrayAdapter<Tuning>(
            this, R.layout.item_tuning, R.id.tuningName, tunings
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val t = tunings[position]
                v.findViewById<TextView>(R.id.tuningName).text =
                    if (t.isCustom) "${t.name}  ✎" else t.name
                // 按从低音到高音的顺序展示音名（如 E2 A2 D3 G3 B3 E4）
                v.findViewById<TextView>(R.id.tuningNotes).text =
                    t.strings.reversed().joinToString(" ") { it.note.fullName }
                return v
            }
        }
        listView.adapter = adapter
    }

    private fun confirmDelete(t: Tuning) {
        Dialogs.confirm(this, "删除自定义调弦", "确定删除「${t.name}」吗？", "删除", "取消") {
            TuningStore.deleteCustom(this, t.name)
            reload()
        }
    }
}
