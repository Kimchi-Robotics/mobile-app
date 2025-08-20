package com.kimchi.deliverybot.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import com.kimchi.deliverybot.utils.Point2D

class MarkerBitmap(bitmap: Bitmap, position: Point2D) {
    private var _position = position
    private var _bitmap = bitmap

    fun drawOnCanvas(canvas: Canvas) {
        val matrix = Matrix()
        matrix.postTranslate(_position.x - _bitmap.width / 2f, _position.y - _bitmap.height)
        canvas.drawBitmap(_bitmap, matrix, null)
    }

    fun setNumber(number: Int) {
        _bitmap = drawTextOnBitmap(_bitmap, number.toString())
    }

    fun drawTextOnBitmap(originalBitmap: Bitmap, text: String): Bitmap {
        // Create a mutable copy of the bitmap
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Create paint for text
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        // Draw text at center
        val centerX = mutableBitmap.width / 2f
        val centerY = mutableBitmap.height / 2f
        canvas.drawText(text, centerX, centerY, paint)

        return mutableBitmap
    }
}
