package com.arbin.panel

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuHelper {

    private var appCtx: Context? = null

    @Volatile private var inputManager: Any? = null
    @Volatile private var appOps: Any? = null

    private const val OP_SYSTEM_ALERT_WINDOW = 24
    private const val MODE_ALLOWED = 0

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    fun isRunning(): Boolean =
        try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasPermission(): Boolean =
        isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission() {
        if (isRunning() && !hasPermission()) {
            Shizuku.requestPermission(1001)
        }
    }

    fun grantOverlay(): Boolean {
        if (!hasPermission()) return false
        val ctx = appCtx ?: return false
        return try {
            val ops = getAppOps() ?: return false
            val uid = ctx.applicationInfo.uid
            val pkg = ctx.packageName
            val setMode = ops.javaClass.getMethod(
                "setMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            setMode.invoke(ops, OP_SYSTEM_ALERT_WINDOW, uid, pkg, MODE_ALLOWED)
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    @Synchronized
    private fun getAppOps(): Any? {
        if (appOps != null) return appOps
        return try {
            val binder: IBinder = SystemServiceHelper.getSystemService("appops")
            val wrapped = ShizukuBinderWrapper(binder)
            val stub = Class.forName("android.app.IAppOpsService\$Stub")
            appOps = stub.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapped)
            appOps
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    @Synchronized
    private fun getInput(): Any? {
        if (inputManager != null) return inputManager
        if (!hasPermission()) return null
        return try {
            val binder: IBinder = SystemServiceHelper.getSystemService("input")
            val wrapped = ShizukuBinderWrapper(binder)
            val stub = Class.forName("android.hardware.input.IInputManager\$Stub")
            inputManager = stub.getMethod("asInterface", IBinder::class.java)
                .invoke(null, wrapped)
            inputManager
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun injectTouch(x: Float, y: Float, action: Int) {
        val im = getInput() ?: return
        try {
            val now = SystemClock.uptimeMillis()
            val ev = MotionEvent.obtain(now, now, action, x, y, 0)
            ev.source = InputDevice.SOURCE_TOUCHSCREEN
            val m = im.javaClass.getMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
            )
            m.invoke(im, ev, 0)
            ev.recycle()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun tap(x: Float, y: Float) {
        injectTouch(x, y, MotionEvent.ACTION_DOWN)
        injectTouch(x, y, MotionEvent.ACTION_UP)
    }

    fun shell(cmd: String): String? {
        if (!hasPermission()) return null
        return try {
            val p = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            p.inputStream.bufferedReader().use { it.readText() }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
