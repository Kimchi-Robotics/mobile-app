package com.kimchi.deliverybot.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.kimchi.deliverybot.R
import com.kimchi.deliverybot.ui.JoystickView.JoystickListener
import com.kimchi.deliverybot.utils.Velocity2D
import com.kimchi.grpc.Velocity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class UiJoystickFragment: Fragment() {
    private var _sendJoyStickMsgs = false
    private var _currentVelocity = Velocity2D(0f,0f)
    private val _uiViewModel : UiViewModel by activityViewModels()
    private lateinit var _velCoroutine: Job

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_joystick_fragment, container, false)

        val joystick: JoystickView = view.findViewById(R.id.joystick)
        joystick.setJoystickListener(object : JoystickListener {
            override fun onJoystickStarted() {
                _sendJoyStickMsgs = true
                val velocityFlow = flow {
                    do {
                        delay(100)
                        _velCoroutine.join()
                        if (!_sendJoyStickMsgs) {
                            _currentVelocity.linear = 0f
                            _currentVelocity.angular = 0f
                         }

                        // TODO: wait until velocity chages instead of waiting some time
                        val velocity = _currentVelocity.toProtoGrpcVelocity()

                        emit(velocity)
                    } while (_sendJoyStickMsgs)
                }
                _uiViewModel.callMoveService(velocityFlow)
            }

            override fun onJoystickMoved(xPercent: Float, yPercent: Float) {
                // Handle joystick movement
                // xPercent and yPercent are values between -1 and 1
                // representing the position of the joystick
                _velCoroutine = lifecycleScope.async(Dispatchers.IO) {
                    _currentVelocity.linear = yPercent
                    // Important: x positive is for turning to the right. Which is a negative rotation in the z
                    // axis.
                    _currentVelocity.angular = -xPercent
                }
            }

            override fun onJoystickReleased() {
                // Handle joystick release
                // Example: Stop character movement
                _velCoroutine = lifecycleScope.async(Dispatchers.IO) {
                    _sendJoyStickMsgs = false
                    _currentVelocity.linear = 0f
                    _currentVelocity.angular = 0f
                }
            }
        })
        return view
    }
}
