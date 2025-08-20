package com.kimchi.deliverybot.draw

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.kimchi.deliverybot.utils.Path
import com.kimchi.deliverybot.utils.Point2D

class RobotPath {
    private var _path = Path.empty()

    fun updatePath(newPath: Path) {
        _path = newPath
    }

    fun drawOnCanvas(canvas: Canvas) {
        var paint = Paint(Color.RED)
        paint.strokeWidth = 1f
        paint.color = Color.RED

        var previusPoint: Point2D? = null
        for (point in _path.points) {
            if (previusPoint == null) {
                previusPoint = point
                continue
            }
            canvas.drawLine(previusPoint.x, previusPoint.y, point.x, point.y, paint)
            previusPoint = point
        }
    }

    fun clear() {
        _path = Path.empty()
    }
}