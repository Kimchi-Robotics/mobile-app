package com.kimchi.deliverybot.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var baseRadius: Float = 0f
    private var hatRadius: Float = 0f
    private var joystickRadius: Float = 0f

    private val basePaint: Paint = Paint().apply {
        color = Color.GRAY
        alpha = 150
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val hatPaint: Paint = Paint().apply {
        color = Color.DKGRAY
        alpha = 200
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var isPressed: Boolean = false
    private var actualX: Float = 0f
    private var actualY: Float = 0f

    private var joystickListener: JoystickListener? = null

    interface JoystickListener {
        fun onJoystickMoved(xPercent: Float, yPercent: Float)
        fun onJoystickReleased()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate dimensions
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = minOf(w, h) / 3f
        hatRadius = minOf(w, h) / 5f
        joystickRadius = baseRadius

        // Initialize joystick position
        actualX = centerX
        actualY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw joystick base
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)

        // Draw the joystick hat
        canvas.drawCircle(actualX, actualY, hatRadius, hatPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val distance = sqrt(dx * dx + dy * dy)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if the touch is within the base radius
                if (distance <= baseRadius) {
                    isPressed = true
                    actualX = event.x
                    actualY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isPressed) {
                    // Constrain the joystick position to the base circle
                    if (distance > joystickRadius) {
                        val ratio = joystickRadius / distance
                        actualX = centerX + dx * ratio
                        actualY = centerY + dy * ratio
                    } else {
                        actualX = event.x
                        actualY = event.y
                    }

                    // Calculate the percentage of movement in each direction
                    val xPercent = (actualX - centerX) / joystickRadius
                    // actualY positive goes from top to bottom. But we want yPercent positive to be
                    // from bottom to top.
                    val yPercent = -(actualY - centerY) / joystickRadius

                    // Notify the listener about joystick movement
                    joystickListener?.onJoystickMoved(xPercent, yPercent)
                }
            }

            MotionEvent.ACTION_UP -> {
                isPressed = false
                // Reset the joystick position
                actualX = centerX
                actualY = centerY

                // Notify the listener about joystick release
                joystickListener?.onJoystickReleased()
            }
        }

        invalidate()
        return true
    }

    fun setJoystickListener(listener: JoystickListener) {
        this.joystickListener = listener
    }

    // Optional: Methods to customize the joystick appearance
    fun setBaseColor(color: Int) {
        basePaint.color = color
        invalidate()
    }

    fun setHatColor(color: Int) {
        hatPaint.color = color
        invalidate()
    }

    fun setJoystickRadius(radius: Float) {
        this.joystickRadius = radius
        invalidate()
    }
}