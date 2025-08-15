package com.kimchi.deliverybot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimchi.deliverybot.R

class UiGoalReachedFragment: Fragment() {
    private val _uiViewModel : UiViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_goal_reached_fragment, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonContinue = view.findViewById<Button>(R.id.continue_path_button)
        val buttonCancelMission = view.findViewById<Button>(R.id.cancel_mission_button)
        buttonContinue.setOnClickListener {
            _uiViewModel.callNavigationContinuePathService()
        }
        buttonCancelMission.setOnClickListener {
            _uiViewModel.callNavigationCancelMissionService()
        }

    }
}
