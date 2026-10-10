package com.arbin.panel

import android.content.Context
import android.graphics.*
import android.media.projection.MediaProjection
import android.view.View

class EspOverlay(ctx: Context) : View(ctx) {

    data class Box(val l: Float, val t: Float, val r: Float, val b: Float) {
        val cx get() = (l + r) / 2f
        val cy get() = (t + b) / 2f
    }

    private val boxP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 5f
    }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN; strokeWidth = 3f
    }

    @Volatile private var boxes: List<Box> = emptyList()
    private var capture: ScreenCapture? = null
    private var projection: MediaProjection? = null
    private var aim: AimController? = null

    fun attachProjection(p: MediaProjection) { projection = p }
    fun setAimController(a: AimController) { aim = a }

    fun start() {
        val p = projection ?: return
        val cap = ScreenCapture(p)
        cap.prepare(resources.displayMetrics)
        cap.onFrame { bmp ->
            val found = Detector.detect(bmp)
            boxes = found
            postInvalidate()
            aim?.updateTargets(found)
            bmp.recycle()
        }
        cap.start()
        capture = cap
    }

    fun stop() { capture?.stop(); capture = null }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawLine(w / 2f, h / 2f - 30f, w / 2f, h / 2f + 30f, lineP)
        c.drawLine(w / 2f - 30f, h / 2f, w / 2f + 30f, h / 2f, lineP)

        for (b in boxes) {
            c.drawRect(b.l, b.t, b.r, b.b, boxP)
            c.drawCircle(b.cx, b.t + (b.r - b.l) / 2f, 6f, dotP)
        }
    }
}
