package com.kimchi.deliverybot.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimchi.deliverybot.R
import com.kimchi.deliverybot.ui.JoystickView.JoystickListener


class UiControlPanelFragment: Fragment() {
    private val uiViewModel : UiViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_control_panel_fragment, container, false)
        val pose_button = view.findViewById<Button>(R.id.button_service)
        val map_button = view.findViewById<Button>(R.id.map_button_service)
        val positionContentTextView = view.findViewById<TextView>(R.id.positionContentTextView)

        uiViewModel.pose.observe(viewLifecycleOwner) {
            val x = it.x
            val y = it.y
            val positionContentString = java.lang.String("$x, $y")
            positionContentTextView.text = positionContentString
        }

        pose_button.setOnClickListener {
            uiViewModel.callPoseService()
        }

        map_button.setOnClickListener {
            uiViewModel.callMapService()
        }
        return view
    }
}