package com.arbin.panel

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics

class ScreenCapture(private val projection: MediaProjection) {

    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var ht: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile private var running = false
    private var frameCb: ((Bitmap) -> Unit)? = null
    private var w = 0; private var h = 0; private var dpi = 0

    fun prepare(metrics: DisplayMetrics) {
        w = metrics.widthPixels
        h = metrics.heightPixels
        dpi = metrics.densityDpi

        ht = HandlerThread("arbin-cap").apply { start() }
        handler = Handler(ht!!.looper)

        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader!!.setOnImageAvailableListener({ r ->
            val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (running) frameCb?.invoke(toBitmap(img))
            } finally {
                img.close()
            }
        }, handler)

        display = projection.createVirtualDisplay(
            "arbin-cap",
            w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, handler
        )
    }

    fun onFrame(cb: (Bitmap) -> Unit) { frameCb = cb }

    fun start() { running = true }

    fun stop() {
        running = false
        display?.release(); display = null
        reader?.close(); reader = null
        ht?.quitSafely(); ht = null
    }

    private fun toBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(plane.buffer)
        return bmp
    }
}
