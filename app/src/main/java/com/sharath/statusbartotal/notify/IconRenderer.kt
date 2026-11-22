package com.sharath.statusbartotal.notify

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

object IconRenderer {
    private const val SIZE = 96
    private const val BOX = 94f
    private const val REF_SIZE = 100f
    private const val MIN_SCALE_X = 0.6f

    fun render(lines: List<String>): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val text = lines.joinToString("")
        if (text.isEmpty()) return bitmap
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        val rect = Rect()
        fun measure() = paint.getTextBounds(text, 0, text.length, rect)

        paint.textScaleX = 1f
        paint.textSize = REF_SIZE
        measure()
        if (rect.height() <= 0 || rect.width() <= 0) return bitmap
        paint.textSize = REF_SIZE * BOX / rect.height()
        measure()
        if (rect.width() > BOX) {
            paint.textScaleX = maxOf(MIN_SCALE_X, BOX / rect.width())
            measure()
            if (rect.width() > BOX) {
                paint.textSize = paint.textSize * BOX / rect.width()
                measure()
            }
        }
        val x = (SIZE - rect.width()) / 2f - rect.left
        val y = (SIZE - rect.height()) / 2f - rect.top
        canvas.drawText(text, x, y, paint)
        return bitmap
    }
}
