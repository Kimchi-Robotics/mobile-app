package com.example.deliverybot.ui.reflow

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlin.math.abs


class ReflowViewModel : ViewModel() {

    private lateinit var _originalBitmap: Bitmap

    private lateinit var _currentBitmap: Bitmap
    private lateinit var _robotBitmap: Bitmap

    private  var _pointsArray = arrayOf<Pair<Float,Float>>()

    private var _robot_step = Pair(0f, 0f)
    private var _robot_path_index = 0

    public fun SetOriginalBipmap(originalBitmap: Bitmap){
        _originalBitmap = originalBitmap
        _currentBitmap = _originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun SetRobotBitmap(robotBitmap: Bitmap) {
        _robotBitmap = robotBitmap
    }
    public fun AddPointToPath(point: Pair<Float, Float>) {
        if(_pointsArray.size == 0) {
            _robot_step = point
        }
        _pointsArray += point
    }

    public fun GetBitmap(): Bitmap {
        if(_pointsArray.size < 2) {
            return _originalBitmap
        }

        val size = _pointsArray.size
        var canvas = Canvas(_currentBitmap)

        var paint = Paint(Color.RED)
        paint.strokeWidth = 10f
        paint.color = Color.RED
        canvas.drawLine(_pointsArray[size-2].first, _pointsArray[size-2].second, _pointsArray[size-1].first, _pointsArray[size-1].second, paint)

        return _currentBitmap
    }

    fun GetBitmapWithRobot(): Bitmap {
        val cs = Bitmap.createBitmap(
            _currentBitmap.getWidth(),
            _currentBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        )

        var canvas = Canvas(cs)
        canvas.drawBitmap(_currentBitmap,0f,0f, null);
        canvas.drawBitmap(_robotBitmap, _robot_step.first, _robot_step.second, null);

        return cs
    }

    fun robotStep() {
        Log.i("Arilow", "robotStep()")
        if(_robot_path_index + 1 >= _pointsArray.size)
            return

        Log.i("Arilow", "robotStep(), _robot_path_index: " + _robot_path_index.toString())

        var step_x = (_pointsArray[_robot_path_index+1].first - _pointsArray[_robot_path_index].first)/10
        var step_y = (_pointsArray[_robot_path_index+1].second - _pointsArray[_robot_path_index].second)/10


        Log.i("Arilow", "robotStep(), _robot_step: " + _robot_step.toString())
        Log.i("Arilow", "robotStep(), step_x: " + step_x.toString())
        Log.i("Arilow", "robotStep(), step_y: " + step_y.toString())

        _robot_step = Pair(_robot_step.first + step_x, _robot_step.second + step_y)

        if(abs(_robot_step.first - _pointsArray[_robot_path_index+1].first) < 0.05 && abs(_robot_step.second - _pointsArray[_robot_path_index+1].second) < 0.05 ) {
            Log.i("Arilow", "robotStep(), _robot_step == _pointsArray[_robot_path_index+1]")
            _robot_path_index++
        }
    }
}