package com.kimchi.deliverybot.ui

import android.app.Dialog
import com.kimchi.deliverybot.network.NetworkScanner
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kimchi.deliverybot.R
import com.kimchi.deliverybot.network.DeviceAdapter
import com.kimchi.deliverybot.network.NetworkDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkDevicesDialogFragment : DialogFragment(), NetworkScanner.ScanListener, DeviceAdapter.OnDeviceClickListener {

    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var devicesRecyclerView: RecyclerView
    private val _uiViewModel : UiViewModel by activityViewModels()

    private val networkScanner = NetworkScanner()
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a dialog with custom layout
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.ui_network_devices_fragment)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ui_network_devices_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        scanButton = view.findViewById(R.id.scanButton)
        statusTextView = view.findViewById(R.id.statusTextView)
        devicesRecyclerView = view.findViewById(R.id.devicesRecyclerView)
        val closeButton: Button = view.findViewById(R.id.closeButton)

        // Setup RecyclerView
        deviceAdapter = DeviceAdapter(this)
        devicesRecyclerView.adapter = deviceAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Setup scan button
        scanButton.setOnClickListener {
            startNetworkScan()
        }

        // Setup close button
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun startNetworkScan() {
        // Update UI to show scanning state
        statusTextView.text = "Scanning network..."
        deviceAdapter.updateDevices(emptyList())

        // Get local IP address to determine subnet
        val wifiManager = requireContext().applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        val formattedIP = String.format(
            "%d.%d.%d",
            ipAddress and 0xff,
            (ipAddress shr 8) and 0xff,
            (ipAddress shr 16) and 0xff
        )

        // Start scanning
        networkScanner.scanNetwork(formattedIP, this)
    }

    // NetworkScanner.ScanListener implementation
    override fun onDeviceFound(ipAddress: String, hostname: String) {
        activity?.runOnUiThread {
            deviceAdapter.addDevice(NetworkDevice(ipAddress, hostname))
        }
    }

    override fun onScanComplete(devices: List<String>) {
        activity?.runOnUiThread {
            statusTextView.text = "Scan complete. Found ${devices.size} devices."
        }
    }

    override fun onDeviceClick(device: NetworkDevice) {
        lifecycleScope.launch(Dispatchers.IO) {
            val alive = _uiViewModel.tryUri(Uri.parse("http://${device.ipAddress}:${50051}"))

            if (!alive) {
                val msg = "The selected device is not a kimchi robot or is not ready to connect."
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Conected to ${device.hostname}!", Toast.LENGTH_SHORT).show()
            }

            dismiss()
        }
    }
}
