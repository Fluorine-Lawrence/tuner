package com.guitartuner.practice.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.guitartuner.R
import com.guitartuner.practice.db.PracticeDb
import com.guitartuner.practice.model.Piece
import com.guitartuner.practice.model.PieceStatus
import com.guitartuner.ui.Dialogs
import com.guitartuner.tuning.Instrument

/**
 * 曲库：曲目增删 + 三状态切换（点击切换）+ 状态筛选 + 删除。
 */
class RepertoireActivity : Activity() {

    private lateinit var db: PracticeDb
    private lateinit var listView: ListView
    private lateinit var newPieceInput: EditText
    private lateinit var statusFilterRow: LinearLayout

    private var instrument = Instrument.GUITAR
    private var filter: PieceStatus? = null   // null = 全部
    private var pieces: List<Piece> = emptyList()

    private val filterViews = mutableListOf<TextView>()

    private val onAccentColor by lazy { getColor(R.color.on_accent) }
    private val accentColor by lazy { getColor(R.color.accent) }
    private val textPrimaryColor by lazy { getColor(R.color.text_primary) }
    private val secondaryColor by lazy { getColor(R.color.text_secondary) }
    private val wantColor by lazy { getColor(R.color.accent) }      // 想演奏 = 橙
    private val learningColor by lazy { getColor(R.color.primary) }  // 正在练 = 蓝
    private val masteredColor by lazy { getColor(R.color.good) }     // 已拿下 = 绿

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repertoire)
        db = PracticeDb(this)

        listView = findViewById(R.id.pieceList)
        newPieceInput = findViewById(R.id.newPieceInput)
        statusFilterRow = findViewById(R.id.statusFilterRow)

        findViewById<Button>(R.id.addPieceBtn).setOnClickListener { addPiece() }
        renderFilters()
        listView.setOnItemClickListener { _, _, pos, _ -> cycleStatus(pieces[pos]) }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            confirmDelete(pieces[pos]); true
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun renderFilters() {
        statusFilterRow.removeAllViews()
        filterViews.clear()
        val filters = listOf<Pair<String, PieceStatus?>>(
            "全部" to null,
            "想演奏" to PieceStatus.WANT,
            "正在练" to PieceStatus.LEARNING,
            "已拿下" to PieceStatus.MASTERED
        )
        for ((label, status) in filters) {
            val tv = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                val padH = (12 * resources.displayMetrics.density).toInt()
                val padV = (7 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
                setOnClickListener {
                    filter = status
                    highlightFilters()
                    reload()
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * resources.displayMetrics.density).toInt()
            statusFilterRow.addView(tv, lp)
            filterViews.add(tv)
        }
        highlightFilters()
    }

    private fun highlightFilters() {
        val selected = when (filter) {
            null -> 0
            PieceStatus.WANT -> 1
            PieceStatus.LEARNING -> 2
            PieceStatus.MASTERED -> 3
        }
        for (i in filterViews.indices) {
            val tv = filterViews[i]
            val on = i == selected
            tv.setBackgroundResource(
                if (on) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
            )
            tv.setTextColor(if (on) onAccentColor else secondaryColor)
        }
    }

    private fun addPiece() {
        val name = newPieceInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入曲目名称", Toast.LENGTH_SHORT).show()
            return
        }
        db.insertPiece(Piece(name = name, instrument = instrument.name))
        newPieceInput.text.clear()
        reload()
    }

    private fun cycleStatus(p: Piece) {
        val next = p.status.next()
        db.updatePieceStatus(p.id, next)
        reload()
    }

    private fun confirmDelete(p: Piece) {
        Dialogs.confirm(this, "删除曲目", "确定删除「${p.name}」吗？", "删除", "取消") {
            db.deletePiece(p.id)
            reload()
        }
    }

    private fun reload() {
        pieces = db.allPieces(instrument.name)
            .filter { filter == null || it.status == filter }
        val adapter = object : ArrayAdapter<Piece>(
            this, R.layout.item_piece, R.id.pieceName, pieces
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val p = pieces[position]
                v.findViewById<TextView>(R.id.pieceName).text = p.name
                val statusTv = v.findViewById<TextView>(R.id.pieceStatus)
                statusTv.text = p.status.displayName
                statusTv.setTextColor(statusColor(p.status))
                return v
            }
        }
        listView.adapter = adapter
    }

    private fun statusColor(s: PieceStatus): Int = when (s) {
        PieceStatus.WANT -> wantColor
        PieceStatus.LEARNING -> learningColor
        PieceStatus.MASTERED -> masteredColor
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}
