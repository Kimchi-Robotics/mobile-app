package com.kimchi.deliverybot.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.kimchi.deliverybot.R
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking

class UiControlPanelFragment: Fragment() {
    val uiViewModel = UiViewModel()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_control_panel_fragment, container, false)
        var button = view.findViewById<Button>(R.id.button_service)
        button.setOnClickListener {
            uiViewModel.callService()
        }


        return view
    }
}