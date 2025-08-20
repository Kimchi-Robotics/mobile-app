package com.kimchi.deliverybot.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.kimchi.deliverybot.draw.MarkerBitmap
import com.kimchi.deliverybot.draw.MarkersBitmapArray
import com.kimchi.deliverybot.draw.RobotBitmap
import com.kimchi.deliverybot.draw.RobotPath
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Path
import com.kimchi.deliverybot.utils.Point2D
import com.kimchi.deliverybot.utils.RobotState
import com.ortiz.touchview.OnTouchCoordinatesListener
import com.ortiz.touchview.TouchImageView

class UiMapFragment: Fragment() {
    private lateinit var _originalBitmap: Bitmap

    private lateinit var _currentBitmap: Bitmap
    private var _markers = MarkersBitmapArray()

    private lateinit var _robotBitmap: RobotBitmap

    private val _uiViewModel : UiViewModel by activityViewModels()
    private val _robotRadius = 30
    private val _markerSize = 15

    private var _robotPath = RobotPath()
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
        _robotBitmap = RobotBitmap(createScaleBitmap(R.drawable.robot_image, _robotRadius, _robotRadius))

        val bitmap = drawBitmap()
        touchImageView.setImageBitmap(bitmap)

        _uiViewModel.pose.observe(viewLifecycleOwner) {
            _robotBitmap.updatePose(_mapInfo.WorldToBitmap(it))
            val mapBitmap = drawBitmap()
            touchImageView.setImageBitmap(mapBitmap)
        }

        _uiViewModel.path.observe(viewLifecycleOwner) {
            if (_uiViewModel.robotState.value != RobotState.NAVIGATION) {
                return@observe
            }
            var newBitmapPath = Path.empty()

            for (point in it.points) {
                newBitmapPath.points += _mapInfo.WorldToBitmap(point)
            }
            _robotPath.updatePath(newBitmapPath)
            _pathUpdated = true
        }

        _uiViewModel.mapInfo.observe(viewLifecycleOwner) {
            _mapInfo = it
            setOriginalBipmap(it.bitmap)
            touchImageView.setImageBitmap(drawBitmap())
        }

        _uiViewModel.robotState.observe(viewLifecycleOwner) {
            if (it == RobotState.IDLE) {
                _markers.clear()
                _robotPath.clear()
            } else if (it != RobotState.NAVIGATION) {
                _robotPath.clear()
            }
            touchImageView.setImageBitmap(drawBitmap())
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
                _markers.addMarker(MarkerBitmap(createScaleBitmap(R.drawable.marker, _markerSize, _markerSize), Point2D(x, y)))
                _uiViewModel.onSingleTouch(x, y)
            }
        })

        return view
    }

    private fun setOriginalBipmap(originalBitmap: Bitmap){
        _originalBitmap = originalBitmap
        _currentBitmap = _originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
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

    private fun drawBitmap(): Bitmap {
        val cs = Bitmap.createBitmap(
            _currentBitmap.getWidth(),
            _currentBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        )

        var canvas = Canvas(cs)
        canvas.drawBitmap(_currentBitmap,0f,0f, null);

        if (_pathUpdated) {
            _robotPath.drawOnCanvas(canvas)
        }

        _markers.drawOnCanvas(canvas)
        _robotBitmap.drawOnCanvas(canvas)
        return cs
    }
}
