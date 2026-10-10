package com.arbin.panel

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var circle: View? = null
    private var panel: PanelView? = null
    private var esp: EspOverlay? = null
    private var aim: AimController? = null
    private var projection: MediaProjection? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotif())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        aim = AimController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val code = it.getIntExtra("code", 0)
            val data = it.getParcelableExtra<Intent>("data")
            if (code != 0 && data != null) {
                val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projection = mpm.getMediaProjection(code, data)
            }
        }
        val dm = resources.displayMetrics
        ScreenSize.w = dm.widthPixels.toFloat()
        ScreenSize.h = dm.heightPixels.toFloat()

        if (circle == null) addFloatingCircle()
        return START_STICKY
    }

    private fun buildNotif(): Notification {
        val ch = "arbin_panel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(ch, "Arbin Panel", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, ch)
            .setContentTitle("Arbin Panel running")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()
    }

    private fun addFloatingCircle() {
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            alpha = 0.85f
        }
        val size = 160
        val p = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 60; y = 300 }

        var startX = 0; var startY = 0
        var downRawX = 0f; var downRawY = 0f

        iv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y
                    downRawX = e.rawX; downRawY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (e.rawX - downRawX).toInt()
                    p.y = startY + (e.rawY - downRawY).toInt()
                    wm.updateViewLayout(iv, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(e.rawX - downRawX) < 15 &&
                        Math.abs(e.rawY - downRawY) < 15) togglePanel()
                    true
                }
                else -> false
            }
        }
        wm.addView(iv, p)
        circle = iv
    }

    private fun togglePanel() {
        if (panel != null) {
            wm.removeView(panel); panel = null
            esp?.let { it.stop(); wm.removeView(it) }; esp = null
            aim?.stop()
            return
        }

        val pv = PanelView(this) { key, value ->
            when (key) {
                "esp_box"   -> setEspBox(value as Boolean)
                "aim"       -> setAim(value as Boolean)
                "auto_fire" -> aim?.autoFire = value as Boolean
                "headshot"  -> aim?.headshotPull = value as Boolean
                "recoil"    -> aim?.recoilComp = value as Boolean
                "droppshot" -> aim?.dropShot = value as Boolean
            }
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 220; y = 300 }

        var sx = 0; var sy = 0; var drx = 0f; var dry = 0f
        pv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x; sy = p.y; drx = e.rawX; dry = e.rawY; false
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = sx + (e.rawX - drx).toInt()
                    p.y = sy + (e.rawY - dry).toInt()
                    wm.updateViewLayout(pv, p); false
                }
                else -> false
            }
        }
        wm.addView(pv, p); panel = pv
    }

    private fun setEspBox(on: Boolean) {
        if (on && esp == null) {
            val e = EspOverlay(this)
            projection?.let { e.attachProjection(it) }
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            )
            wm.addView(e, p)
            e.setAimController(aim)
            e.start()
            esp = e
        } else if (!on) {
            esp?.let { it.stop(); wm.removeView(it) }; esp = null
        }
    }

    private fun setAim(on: Boolean) {
        if (on) aim?.start() else aim?.stop()
    }

    override fun onDestroy() {
        runCatching { panel?.let { wm.removeView(it) } }
        runCatching { esp?.let { it.stop(); wm.removeView(it) } }
        runCatching { circle?.let { wm.removeView(it) } }
        aim?.stop()
        projection?.stop()
        super.onDestroy()
    }
}
