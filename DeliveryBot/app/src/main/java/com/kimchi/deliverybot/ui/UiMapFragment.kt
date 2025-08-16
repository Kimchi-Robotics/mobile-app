package com.kimchi.deliverybot.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimchi.deliverybot.R
import androidx.fragment.app.activityViewModels
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Path
import com.kimchi.deliverybot.utils.Pose2D
import com.ortiz.touchview.OnTouchCoordinatesListener
import com.ortiz.touchview.TouchImageView

class UiMapFragment: Fragment() {
    private lateinit var _originalBitmap: Bitmap

    private lateinit var _currentBitmap: Bitmap
    private lateinit var _robotBitmap: Bitmap

    private val _uiViewModel : UiViewModel by activityViewModels()
    private val _robotRadius = 30

    private var _path = Path.empty()
    private var _pathUpdated = false
    private var _mapInfo = MapInfo.empty()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_map_fragment, container, false)
        var touchImageView = view.findViewById<TouchImageView>(R.id.imageSingle)

        setOriginalBipmap(createMutableBitmap(R.drawable.map))
        setRobotBitmap(createScaleBitmap(R.drawable.robot_image, _robotRadius, _robotRadius))
        val bitmap = getBitmapWithRobot(Pose2D(0f, 0f, 0f))
        touchImageView.setImageBitmap(bitmap)

        _uiViewModel.pose.observe(viewLifecycleOwner) {
            val mapBitmap = getBitmapWithRobot(it)
            view.findViewById<TouchImageView>(R.id.imageSingle).setImageBitmap(mapBitmap)
        }

        _uiViewModel.path.observe(viewLifecycleOwner) {
            var newBitmapPath = Path.empty()

            for (point in it.points) {
                newBitmapPath.points += _mapInfo.WorldToBitmap(point)
            }
            _path = newBitmapPath
            _pathUpdated = true
        }

        _uiViewModel.mapInfo.observe(viewLifecycleOwner) {
            _mapInfo = it
            setOriginalBipmap(it.bitmap)
        }

        touchImageView.setOnTouchCoordinatesListener(object: OnTouchCoordinatesListener {
            private val kDoubleTouchTimeMs = 300
            private val kLongTouchTimeMs = 150
            private var lastActionUpTimeMs: Long = 0
            private var lastActionDownTimeMs: Long = 0

            private var lastAction = MotionEvent.ACTION_DOWN
            private var countDownTimer: CountDownTimer? = null
            private var shouldKillTimer: Boolean = false // Your boolean flag
            override fun onTouchCoordinate(v: View, event: MotionEvent, bitmapPoint: PointF) {
                when(event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.e("Arilow", "ACTION_DOWN")
                        lastAction = MotionEvent.ACTION_DOWN
                        lastActionDownTimeMs = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.e("Arilow", "ACTION_UP")

                        maybeCallOnSingleTouchCoordEvent(bitmapPoint.x, bitmapPoint.y)
                        lastAction = MotionEvent.ACTION_UP
                        lastActionUpTimeMs = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // One of multiple fingers is lifted
                        lastAction = MotionEvent.ACTION_POINTER_UP
                        Log.e("Arilow", "ACTION_POINTER_UP")
                    }
                    MotionEvent.ACTION_SCROLL -> Log.e("Arilow", "ACTION_SCROLL")
                }
            }


            // A single touch is a touch that
            // - Is not the first touch of a double touch: A touch that is followed by another touch in the next 200ms
            // - Is not the second touch of a double touch: A touch that follows another touch after less that 200ms
            // - Is not a long touch: A touch that is maintained for more than 100ms
            // - Is not part of a touch with multiple fingers
            fun maybeCallOnSingleTouchCoordEvent(x: Float, y: Float) {
                val currentTime = System.currentTimeMillis()
                val timeSinceDown = currentTime - lastActionDownTimeMs
                val timeBetweenTouches = currentTime - lastActionUpTimeMs

                if (timeBetweenTouches <= kDoubleTouchTimeMs) {
                    shouldKillTimer = true
                }

                // Check that is not a long touch: timeSinceDown <= kLongTouchTimeMs
                // Check that is not the second touch of a double touch: timeBetweenTouches >= kDoubleTouchTimeMs
                // Check that is not part of a touch with multiple finger: lastAction != MotionEvent.ACTION_POINTER_UP
                if (timeSinceDown <= kLongTouchTimeMs && timeBetweenTouches >= kDoubleTouchTimeMs && lastAction != MotionEvent.ACTION_POINTER_UP) {
                    shouldKillTimer = false

                    // Check that is not the first touch of a double touch.
                    countDownTimer = object : CountDownTimer(300, 50) { // Check every 50ms
                        override fun onTick(millisUntilFinished: Long) {
                            if (shouldKillTimer) {
                                Log.e("Arilow", "Timer killed by boolean flag")
                                this.cancel()
                                return
                            }
                        }

                        override fun onFinish() {
                            if (!shouldKillTimer) {
                                Log.e("Arilow", "Timer finished after 300ms")
                                onSingleTouchCoordEvent(x, y)
                            } else {
                                Log.e("Arilow", "Timer was flagged to be killed, not executing action")
                            }
                        }
                    }.start()
                }

            }
            fun onSingleTouchCoordEvent(x: Float, y: Float) {
                Log.e("Arilow", "onSingleTouchEvent")

                _uiViewModel.onSingleTouch(x, y)
            }
        })

        return view
    }

    private fun setOriginalBipmap(originalBitmap: Bitmap){
        _originalBitmap = originalBitmap
        _currentBitmap = _originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun setRobotBitmap(robotBitmap: Bitmap) {
        _robotBitmap = robotBitmap
    }

    private fun createMutableBitmap(drawableId: Int): Bitmap {
        var myBitmap = BitmapFactory.decodeResource(getResources(), drawableId)
        val mutableBitmap: Bitmap = myBitmap.copy(Bitmap.Config.ARGB_8888, true)
        return mutableBitmap
    }

    private fun createScaleBitmap(drawableId: Int, newHeight: Int, newWidth: Int): Bitmap {
        var myBitmap = BitmapFactory.decodeResource(resources, drawableId)
        myBitmap = Bitmap.createScaledBitmap(myBitmap!!, newWidth, newHeight, false)
        return myBitmap
    }

    private fun getBitmapWithRobot(pose: Pose2D): Bitmap {
        val cs = Bitmap.createBitmap(
            _currentBitmap.getWidth(),
            _currentBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        )

        var canvas = Canvas(cs)
        canvas.drawBitmap(_currentBitmap,0f,0f, null);

        if (!_path.isEmpty() && _pathUpdated) {
            drawPath(canvas)
        }

        val mapCoords = _mapInfo.WorldToBitmap(pose)
//        canvas.drawBitmap(_robotBitmap, mapCoords.x - _robotRadius/2, mapCoords.y - _robotRadius/2, null);
        drawRotatedBitmapWithMatrix(canvas, _robotBitmap, mapCoords.x, mapCoords.y, radsToDegrees(mapCoords.theta))
        return cs
    }
    private fun drawRotatedBitmapWithMatrix(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, degrees: Float) {
        val matrix = Matrix()
        Log.e("Arilow", "degrees: ${degrees}")
        matrix.postRotate(degrees, bitmap.width / 2f, bitmap.height / 2f)
        matrix.postTranslate(x - bitmap.width / 2f, y - bitmap.height / 2f)
        canvas.drawBitmap(bitmap, matrix, null)
    }
    private fun drawPath(canvas: Canvas) {
        var paint = Paint(Color.RED)
        paint.strokeWidth = 1f
        paint.color = Color.RED

        var previusPoint: Path.Point2D? = null
        for (point in _path.points) {
            if (previusPoint == null) {
                previusPoint = point
                continue
            }
            canvas.drawLine(previusPoint.x, previusPoint.y, point.x, point.y, paint)
            previusPoint = point
        }
    }

    private fun radsToDegrees(rads: Float): Float {
        return rads * 180.0f / 3.1415f
    }
}
