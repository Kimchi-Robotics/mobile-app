package com.kimchi.deliverybot.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.kimchi.deliverybot.utils.Pose2D

class RobotBitmap(bitmap: Bitmap) {
    private var _pose = Pose2D(-100f,-100f,0f)
    private val _bitmap = bitmap

    fun drawOnCanvas(canvas: Canvas) {
//        drawRotatedBitmapWithMatrix(canvas, _bitmap, _pose.x, _pose.y, radsToDegrees(_pose.theta))
        val matrix = Matrix()
        matrix.postRotate(radsToDegrees(_pose.theta), _bitmap.width / 2f, _bitmap.height / 2f)
        matrix.postTranslate(_pose.x - _bitmap.width / 2f, _pose.y - _bitmap.height / 2f)
        canvas.drawBitmap(_bitmap, matrix, null)
    }

    fun updatePose(newPose: Pose2D) {
        _pose = newPose
    }

//    private fun drawRotatedBitmapWithMatrix(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, degrees: Float) {
//        val matrix = Matrix()
//        matrix.postRotate(degrees, bitmap.width / 2f, bitmap.height / 2f)
//        matrix.postTranslate(x - bitmap.width / 2f, y - bitmap.height / 2f)
//        canvas.drawBitmap(bitmap, matrix, null)
//    }

    private fun radsToDegrees(rads: Float): Float {
        return rads * 180.0f / 3.1415f
    }

}