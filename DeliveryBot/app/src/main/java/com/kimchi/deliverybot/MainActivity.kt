package com.kimchi.deliverybot

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kimchi.deliverybot.storage.DataStoreRepository
import com.kimchi.deliverybot.ui.UiViewModel
import com.kimchi.deliverybot.utils.RobotState

class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private val TAG = MainActivity::class.qualifiedName
    private val _uiViewModel : UiViewModel by viewModels()
    private lateinit var _startMappingDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init with splash screen.
        setupAndRunSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStartMappingDialog()
    }

    override fun onResume() {
        super.onResume()
        // Then handle your robot state logic
        _uiViewModel.robotState.observe(this) {
            Log.i(TAG, "Observing robot state $it")
            if (it == RobotState.NO_MAP) {
                if (!_startMappingDialog.isShowing) {
                    Log.i(TAG, "Showing dialog")
                    _startMappingDialog.show()
                }
            }
        }
        _uiViewModel.setDataStoreRepository(DataStoreRepository(applicationContext))
        _uiViewModel.initRobotState()
    }

    private fun setupStartMappingDialog() {
        _startMappingDialog = Dialog(this)
        _startMappingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        _startMappingDialog.setCancelable(false)
        _startMappingDialog.setContentView(R.layout.no_map_dialog)

        val startMappingButton: Button = _startMappingDialog.findViewById(R.id.start_mapping_button)
        startMappingButton.setOnClickListener {
            _startMappingDialog.dismiss()
            _uiViewModel.callStartMappingService()
        }
    }

    /** Callback for when settings_menu button is pressed.  */
    fun showSettings(view: View?) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        when (_uiViewModel.robotState.value) {
            RobotState.MAPPING_WITH_TELEOP -> {
                inflater.inflate(R.menu.state_menu_mapping, popup.menu)
            }
            RobotState.MAPPING_WITH_EXPLORATION -> {
                inflater.inflate(R.menu.state_menu_mapping, popup.menu)
            }
            RobotState.TELEOP -> {
                inflater.inflate(R.menu.state_menu_teleop, popup.menu)
            }
            else -> {
                inflater.inflate(R.menu.state_menu_default, popup.menu)
            }
        }

        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.state_teleoperation -> {
                Log.i(TAG, "State teleoperation selected")
                _uiViewModel.startTeleoperation()
                return true
            }
            R.id.state_navigation -> {
                Log.i(TAG, "State navigation selected")
                //_uiViewModel.startNavigatio()?
                return true
            }
            R.id.state_mapping -> {
                Log.i(TAG, "State navigation selected")
                //_uiViewModel.startMapping()?
                return true
            }
            R.id.scan_network -> {
                Log.i(TAG, "Scan network selected")
                launchNetworkScannerActivity()
                return true
            }
        }
        return false
    }

    private fun launchNetworkScannerActivity() {
        startActivity(Intent(this, NetworkScannerActivity::class.java))
    }

    private fun setupAndRunSplashScreen() {
        // Track whether we've finished the initial animation
        var keepSplashScreenVisible = true

        val splashScreen = installSplashScreen()
        // This keeps the splash screen visible until the animation finishes and a bit more.
        splashScreen.setKeepOnScreenCondition { keepSplashScreenVisible }

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            if  (_uiViewModel.robotState.value == RobotState.NOT_CONNECTED) {
                launchNetworkScannerActivity()
            }
            splashScreenViewProvider.remove()
        }

        // Use a handler to set flag to false after initial animation duration
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashScreenVisible = false
        }, 1500) // Duration is the value of windowSplashScreenAnimationDuration in theme + 500ms more.

    }

}
