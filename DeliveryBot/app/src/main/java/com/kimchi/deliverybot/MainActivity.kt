package com.kimchi.deliverybot

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kimchi.deliverybot.ui.UiViewModel
import com.kimchi.deliverybot.utils.RobotState

class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private val _uiViewModel : UiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /** Callback for when settings_menu button is pressed.  */
    fun showSettings(view: View?) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.state_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.state_teleoperation -> {
                Log.i("Arilow", "State teleoperation selected")
                _uiViewModel.handleState(RobotState.TELEOP)
                return true
            }
            R.id.state_navigation -> {
                Log.i("Arilow", "State navigation selected")
                _uiViewModel.handleState(RobotState.NAVIGATION)
                return true
            }
        }
        return false
    }
}

