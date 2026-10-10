package com.arbin.panel

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class PanelView(
    ctx: Context,
    private val onChange: (String, Any) -> Unit
) : View(ctx) {

    private data class Item(val label: String, var on: Boolean, val key: String)

    private val items = mutableListOf(
        Item("ESP Box",       false, "esp_box"),
        Item("Aim Assist",    false, "aim"),
        Item("Auto Fire",     false, "auto_fire"),
        Item("Headshot Pull", false, "headshot"),
        Item("Recoil Comp",   false, "recoil"),
        Item("Drop Shot",     false, "droppshot")
    )

    private val bg   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF01A1A1A.toInt() }
    private val txt  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 36f }
    private val onP  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E676.toInt() }
    private val offP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A3A3A.toInt() }
    private val head = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt(); textSize = 44f; isFakeBoldText = true
    }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val W = 780f
    private val rowH = 100f
    private val pad = 30f
    private val headerH = 110f
    private val H = headerH + rowH * items.size + pad

    init { setWillNotDraw(false) }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(W.toInt(), H.toInt())

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawRoundRect(RectF(0f, 0f, W, H), 28f, 28f, bg)
        c.drawText("ARBIN PANEL", pad, 72f, head)

        items.forEachIndexed { i, it ->
            val top = headerH + i * rowH
            c.drawText(it.label, pad, top + 62f, txt)
            val tr = RectF(W - 180f, top + 25f, W - 50f, top + 75f)
            c.drawRoundRect(tr, 25f, 25f, if (it.on) onP else offP)
            val knobX = if (it.on) tr.right - 25f else tr.left + 25f
            c.drawCircle(knobX, tr.centerY(), 20f, knob)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            if (e.y < headerH) return true
            val i = ((e.y - headerH) / rowH).toInt()
            if (i in items.indices) {
                val it = items[i]
                it.on = !it.on
                onChange(it.key, it.on)
                invalidate()
            }
        }
        return true
    }
}
