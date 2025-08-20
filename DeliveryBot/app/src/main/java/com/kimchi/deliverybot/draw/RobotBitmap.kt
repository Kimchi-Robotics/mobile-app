package com.kimchi.deliverybot.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.kimchi.deliverybot.utils.Pose2D

class RobotBitmap(bitmap: Bitmap) {
    private var _pose = Pose2D(-100f,-100f,0f)
    private val _bitmap = bitmap

    fun drawOnCanvas(canvas: Canvas) {
        val matrix = Matrix()
        matrix.postRotate(radsToDegrees(_pose.theta), _bitmap.width / 2f, _bitmap.height / 2f)
        matrix.postTranslate(_pose.x - _bitmap.width / 2f, _pose.y - _bitmap.height / 2f)
        canvas.drawBitmap(_bitmap, matrix, null)
    }

    fun updatePose(newPose: Pose2D) {
        _pose = newPose
    }

    private fun radsToDegrees(rads: Float): Float {
        return rads * 180.0f / 3.1415f
    }

}
