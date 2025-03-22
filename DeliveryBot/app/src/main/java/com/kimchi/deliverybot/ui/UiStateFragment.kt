package com.kimchi.deliverybot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var _currentState: RobotState = RobotState.WAITING

    private val _uiViewModel : UiViewModel by activityViewModels()

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
        _uiViewModel.robotState.observe(viewLifecycleOwner) {
            _currentState = it
            updateStateFragment()
        }

        // Initial fragment setup based on current state
        updateStateFragment()
    }

    /**
    * Updates the state fragment based on the current state
    */
    private fun updateStateFragment() {
        val newFragment = when (_currentState) {
            RobotState.WAITING -> UiEmptyFragment()
            RobotState.TELEOP -> UiJoystickFragment()
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