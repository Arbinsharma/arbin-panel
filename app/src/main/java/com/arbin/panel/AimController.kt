package com.arbin.panel

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.math.hypot
import kotlin.random.Random

class AimController {

    @Volatile var smooth: Float = 0.25f
    @Volatile var active = false
    @Volatile var autoFire = false
    @Volatile var headshotPull = false
    @Volatile var recoilComp = false
    @Volatile var dropShot = false

    private val h = Handler(Looper.getMainLooper())
    private var targets: List<EspOverlay.Box> = emptyList()
    private var lastInjection = 0L

    fun updateTargets(t: List<EspOverlay.Box>) { targets = t }

    fun start() {
        if (active) return
        active = true
        h.post(loop)
    }

    fun stop() {
        active = false
        h.removeCallbacks(loop)
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!active) return
            val now = System.currentTimeMillis()
            val cool = (40 + (1f - smooth) * 20f).toLong()

            if (now - lastInjection >= cool) {
                val best = pickTarget()
                if (best != null) {
                    injectToward(best)
                    if (autoFire) ShizukuHelper.tap(best.cx, best.t + 20f)
                    lastInjection = now
                }
            }
            h.postDelayed(this, 16)
        }
    }

    private fun pickTarget(): EspOverlay.Box? {
        if (targets.isEmpty()) return null
        val cx = ScreenSize.w / 2f
        val cy = ScreenSize.h / 2f
        var best: EspOverlay.Box? = null
        var bestD = Float.MAX_VALUE
        for (b in targets) {
            val d = hypot(b.cx - cx, b.cy - cy)
            if (d < bestD) { bestD = d; best = b }
        }
        return if (bestD < ScreenSize.w * 0.35f) best else null
    }

    private fun injectToward(b: EspOverlay.Box) {
        val aimY = if (headshotPull) b.t + (b.r - b.l) / 2f else b.t + (b.b - b.t) * 0.3f
        val cx = ScreenSize.w / 2f
        val cy = ScreenSize.h / 2f

        val dx = (b.cx - cx) * (1f - smooth)
        val dy = (aimY - cy) * (1f - smooth)

        val jx = dx + Random.nextFloat() * 4f - 2f
        val jy = dy + Random.nextFloat() * 4f - 2f

        val tx = (cx + jx).coerceIn(4f, ScreenSize.w - 4f)
        val ty = (cy + jy).coerceIn(4f, ScreenSize.h - 4f)

        ShizukuHelper.injectTouch(tx, ty, MotionEvent.ACTION_DOWN)
        ShizukuHelper.injectTouch(tx, ty, MotionEvent.ACTION_UP)

        if (dropShot) {
            ShizukuHelper.tap(ScreenSize.w - 120f, ScreenSize.h - 120f)
        }
    }
}

object ScreenSize {
    @Volatile var w: Float = 1080f
    @Volatile var h: Float = 2340f
}
