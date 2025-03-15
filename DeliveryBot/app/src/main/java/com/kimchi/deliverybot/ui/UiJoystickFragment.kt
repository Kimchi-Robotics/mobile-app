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
import com.kimchi.deliverybot.R
import com.kimchi.deliverybot.ui.JoystickView.JoystickListener

class UiJoystickFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_joystick_fragment, container, false)

        val joystick: JoystickView = view.findViewById(R.id.joystick)
        joystick.setJoystickListener(object : JoystickListener {
            override fun onJoystickMoved(xPercent: Float, yPercent: Float) {
                // Handle joystick movement
                // xPercent and yPercent are values between -1 and 1
                // representing the position of the joystick
                Log.i("Arilow", "joystick moved: x: ${xPercent}, y: ${yPercent}")
            }

            override fun onJoystickReleased() {
                // Handle joystick release
                // Example: Stop character movement
                Log.i("Arilow", "joystick stop: x: ${0}, y: ${0}")
            }
        })

        return view

    }

}