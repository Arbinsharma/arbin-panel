package com.arbin.panel

import android.graphics.Bitmap
import android.graphics.Color

object Detector {

    private const val DOWNSAMPLE = 4
    private const val MIN_CLUSTER = 8

    private const val R_MIN = 160
    private const val G_MAX = 90
    private const val B_MAX = 90

    fun detect(frame: Bitmap): List<EspOverlay.Box> {
        val sw = frame.width / DOWNSAMPLE
        val sh = frame.height / DOWNSAMPLE
        if (sw <= 0 || sh <= 0) return emptyList()

        val small = Bitmap.createScaledBitmap(frame, sw, sh, false)
        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        val visited = BooleanArray(sw * sh)
        val out = ArrayList<EspOverlay.Box>()

        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val idx = y * sw + x
                if (visited[idx] || !isTarget(pixels[idx])) continue

                var minX = x; var maxX = x
                var minY = y; var maxY = y
                var count = 0
                val stack = ArrayDeque<Int>()
                stack.addLast(idx)
                visited[idx] = true

                while (stack.isNotEmpty()) {
                    val cur = stack.removeLast()
                    val cx = cur % sw
                    val cy = cur / sw
                    count++
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    for (dy in -1..1) for (dx in -1..1) {
                        val nx = cx + dx; val ny = cy + dy
                        if (nx < 0 || ny < 0 || nx >= sw || ny >= sh) continue
                        val ni = ny * sw + nx
                        if (!visited[ni] && isTarget(pixels[ni])) {
                            visited[ni] = true
                            stack.addLast(ni)
                        }
                    }
                }

                if (count < MIN_CLUSTER) continue

                val l = (minX * DOWNSAMPLE).toFloat()
                val t = (minY * DOWNSAMPLE).toFloat()
                val r = ((maxX + 1) * DOWNSAMPLE).toFloat()

                val cxCenter = (l + r) / 2f
                val width = (r - l).coerceAtLeast(40f)
                val headH = width
                val bodyH = headH * 3.6f

                out.add(EspOverlay.Box(cxCenter - width, t, cxCenter + width, t + headH + bodyH))
            }
        }
        small.recycle()
        return out
    }

    private fun isTarget(px: Int): Boolean {
        val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
        return r >= R_MIN && g <= G_MAX && b <= B_MAX
    }
}
