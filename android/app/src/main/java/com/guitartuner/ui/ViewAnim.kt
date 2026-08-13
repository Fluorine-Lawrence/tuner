package com.guitartuner.ui

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * 轻量动效工具（零依赖，纯属性动画）。
 */
object ViewAnim {

    /** 给按钮加按压缩放反馈（不拦截点击） */
    fun pressable(vararg views: View) {
        for (v in views) {
            v.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
                }
                false
            }
        }
    }

    /** 根布局淡入 + 上移入场 */
    fun fadeIn(root: ViewGroup) {
        root.alpha = 0f
        root.translationY = 24f
        root.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }
}
