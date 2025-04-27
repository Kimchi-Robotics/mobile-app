package com.kimchi.deliverybot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.kimchi.deliverybot.R
import com.kimchi.deliverybot.utils.RobotState

/**
 * Handles the changes in the UI related to the state of the robot.
 */
class UiStateFragment: Fragment() {
    private var _currentStateFragment: Fragment? = null
    private var _currentState: RobotState = RobotState.IDLE
    private val _uiViewModel : UiViewModel by activityViewModels()

    private lateinit var _robotStateContentTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.ui_state_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _robotStateContentTextView = view.findViewById(R.id.robotStateContentTextView)

        _uiViewModel.robotState.observe(viewLifecycleOwner) {
            _currentState = it
            updateStateTextView()
            updateStateFragment()
        }

        // Initial fragment setup based on current state
        updateStateFragment()
    }

    private fun updateStateTextView() {
        _robotStateContentTextView.text = when(_currentState) {
            RobotState.IDLE -> "Idle"
            RobotState.TELEOP -> "Teleoperation"
            RobotState.NOT_CONNECTED -> "Not connected"
            RobotState.MAPPING_WITH_TELEOP -> "Mapping with teleoperation"
            RobotState.NAVIGATION -> "Navigating"
            RobotState.MAPPING_WITH_EXPLORATION -> "Mapping with exploration"
            RobotState.NO_MAP -> "No map"
        }
    }

    /**
    * Updates the state fragment based on the current state
    */
    private fun updateStateFragment() {
        val newFragment = when (_currentState) {
            RobotState.IDLE -> UiEmptyFragment()
            RobotState.TELEOP -> UiJoystickFragment()
            RobotState.MAPPING_WITH_TELEOP -> UiMappingWithTeleopFragment()
            else -> UiEmptyFragment()
        }

        // Only proceed if the fragment is attached to avoid issues
        if (!isAdded) return

        // Perform the fragment transaction
        childFragmentManager.beginTransaction().apply {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(R.id.fragment_container, newFragment)
            commit()
        }

        _currentStateFragment = newFragment
    }
}