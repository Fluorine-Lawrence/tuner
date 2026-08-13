package com.guitartuner.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.guitartuner.R

/**
 * 自绘弹窗工具：替代系统 AlertDialog / Spinner，深色圆角卡片风格，无安卓原生痕迹。
 */
object Dialogs {

    private val primaryColor = R.color.primary

    /** 确认弹窗 */
    fun confirm(
        context: Context,
        title: String,
        message: String?,
        positive: String = "确认",
        negative: String = "取消",
        onPositive: () -> Unit
    ) {
        val dialog = baseDialog(context)
        val root = column(context)

        root.addView(titleView(context, title))
        if (message != null) {
            root.addView(TextView(context).apply {
                text = message
                textSize = 15f
                setTextColor(context.getColor(R.color.text_secondary))
                setPadding(0, 12, 0, 4)
            })
        }
        root.addView(buttonRow(context,
            negative to { dialog.dismiss() },
            positive to { dialog.dismiss(); onPositive() }
        ))
        dialog.setContentView(root)
        dialog.show()
    }

    /** 输入弹窗 */
    fun input(
        context: Context,
        title: String,
        hint: String,
        onOk: (String) -> Unit
    ) {
        val dialog = baseDialog(context)
        val root = column(context)
        root.addView(titleView(context, title))

        val edit = EditText(context).apply {
            this.hint = hint
            setHintTextColor(context.getColor(R.color.text_dim))
            setTextColor(context.getColor(R.color.text_primary))
            textSize = 16f
            setBackgroundResource(R.drawable.bg_input)
            setPadding(48, 36, 48, 36)
            setSingleLine(false)
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 16
        root.addView(edit, lp)

        root.addView(buttonRow(context,
            "取消" to { dialog.dismiss() },
            "确认" to { dialog.dismiss(); onOk(edit.text.toString()) }
        ))
        dialog.setContentView(root)
        dialog.show()
    }

    /** 选择弹窗（替代 Spinner） */
    fun pick(
        context: Context,
        title: String,
        options: List<String>,
        currentIndex: Int,
        onPick: (Int) -> Unit
    ) {
        val dialog = baseDialog(context)
        val root = column(context)
        root.addView(titleView(context, title))

        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        options.forEachIndexed { i, opt ->
            val cell = TextView(context).apply {
                text = opt
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(16, 26, 16, 26)
                setTextColor(
                    context.getColor(if (i == currentIndex) R.color.on_accent else R.color.text_primary)
                )
                setBackgroundResource(
                    if (i == currentIndex) R.drawable.bg_string_cell_selected else R.drawable.bg_string_cell
                )
                setOnClickListener {
                    dialog.dismiss()
                    onPick(i)
                }
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 8
            grid.addView(cell, lp)
        }
        val glp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        glp.topMargin = 12
        root.addView(grid, glp)

        root.addView(buttonRow(context,
            "取消" to { dialog.dismiss() }
        ))
        dialog.setContentView(root)
        dialog.show()
    }

    // ---- 内部构建 ----

    private fun baseDialog(context: Context): Dialog {
        val d = Dialog(context)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.window?.setDimAmount(0.6f)
        return d
    }

    private fun column(context: Context): LinearLayout {
        val pad = (24 * context.resources.displayMetrics.density).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, pad, pad, pad)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginStart = pad
            lp.marginEnd = pad
            layoutParams = lp
        }
    }

    private fun titleView(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 18f
            setTextColor(context.getColor(R.color.text_primary))
        }

    /** 底部按钮行 */
    private fun buttonRow(
        context: Context,
        vararg buttons: Pair<String, () -> Unit>
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 20
        row.layoutParams = lp

        buttons.forEachIndexed { i, (label, action) ->
            val primary = i == buttons.size - 1
            val btn = TextView(context).apply {
                text = label
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(28, 20, 28, 20)
                setTextColor(
                    context.getColor(if (primary) R.color.on_accent else R.color.text_secondary)
                )
                setBackgroundResource(
                    if (primary) R.drawable.bg_button_primary else R.drawable.bg_button_secondary
                )
                setOnClickListener { action() }
            }
            val blp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            if (i > 0) blp.marginStart = 12
            row.addView(btn, blp)
        }
        return row
    }

    /** 滚轮选择弹窗（iOS 风格轮盘） */
    fun pickWheel(
        context: Context,
        title: String,
        items: List<String>,
        currentIndex: Int,
        onPick: (Int) -> Unit
    ) {
        val dialog = baseDialog(context)
        val root = column(context)
        root.addView(titleView(context, title))

        val wheel = WheelPickerView(context)
        val density = context.resources.displayMetrics.density
        val wlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (44f * 5 * density).toInt()
        )
        wlp.topMargin = 16
        root.addView(wheel, wlp)
        wheel.setItems(items, currentIndex)

        root.addView(buttonRow(context,
            "取消" to { dialog.dismiss() },
            "确认" to { dialog.dismiss(); onPick(wheel.getSelectedIndex()) }
        ))
        dialog.setContentView(root)
        dialog.show()
    }

    /** 两行列表弹窗（用于曲目选择等：主标题 + 副标题） */
    fun pickList(
        context: Context,
        title: String,
        items: List<Pair<String, String?>>,
        onPick: (Int) -> Unit
    ) {
        val dialog = baseDialog(context)
        val root = column(context)
        root.addView(titleView(context, title))

        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        items.forEachIndexed { i, (main, sub) ->
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.bg_string_cell)
                setOnClickListener { dialog.dismiss(); onPick(i) }
            }
            val mainTv = TextView(context).apply {
                text = main
                textSize = 16f
                setTextColor(context.getColor(R.color.text_primary))
            }
            cell.addView(mainTv)
            if (sub != null) {
                val subTv = TextView(context).apply {
                    text = sub
                    textSize = 13f
                    setTextColor(context.getColor(R.color.text_secondary))
                }
                cell.addView(subTv)
            }
            val clp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            clp.topMargin = 8
            list.addView(cell, clp)
        }
        val llp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llp.topMargin = 12
        root.addView(list, llp)

        root.addView(buttonRow(context, "取消" to { dialog.dismiss() }))
        dialog.setContentView(root)
        dialog.show()
    }
}
