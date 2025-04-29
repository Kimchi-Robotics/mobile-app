package com.kimchi.deliverybot

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kimchi.deliverybot.network.DeviceAdapter
import com.kimchi.deliverybot.network.NetworkDevice
import com.kimchi.deliverybot.network.NetworkScanner
import com.kimchi.deliverybot.network.NetworkScannerViewModel
import com.kimchi.deliverybot.storage.DataStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkScannerActivity: AppCompatActivity(), NetworkScanner.ScanListener, DeviceAdapter.OnDeviceClickListener {
    private lateinit var _scanButton: Button
    private lateinit var _statusTextView: TextView
    private lateinit var _devicesRecyclerView: RecyclerView
    private val _networkScannerViewModel : NetworkScannerViewModel by viewModels()

    private val _networkScanner = NetworkScanner()
    private lateinit var _deviceAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.ui_network_devices_fragment)
        // Initialize views
        _scanButton = findViewById(R.id.scanButton)
        _statusTextView = findViewById(R.id.statusTextView)
        _devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        val closeButton: Button = findViewById(R.id.closeButton)

        // Setup RecyclerView
        _deviceAdapter = DeviceAdapter(this)
        _devicesRecyclerView.adapter = _deviceAdapter
        _devicesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup scan button
        _scanButton.setOnClickListener {
            startNetworkScan()
        }

        // Setup close button
        closeButton.setOnClickListener {
            finish()
        }

        _networkScannerViewModel.setDataStoreRepository(DataStoreRepository(applicationContext))
    }

    @SuppressLint("DefaultLocale")
    private fun startNetworkScan() {
        // Update UI to show scanning state
        _statusTextView.text = "Scanning network..."
        _deviceAdapter.updateDevices(emptyList())

        // Get local IP address to determine subnet
        val wifiManager = applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        val formattedIP = String.format(
            "%d.%d.%d",
            ipAddress and 0xff,
            (ipAddress shr 8) and 0xff,
            (ipAddress shr 16) and 0xff
        )

        // Start scanning
        _networkScanner.scanNetwork(formattedIP, this)
    }

    // NetworkScanner.ScanListener implementation
    override fun onDeviceFound(ipAddress: String, hostname: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var hostnameInput = hostname
            // TODO This code would let the user see in the list if the ip from a kimchi robot or not
            //      the  problem is that when you try to connect many time to the gRPC server, it kind of fail.
            val alive = _networkScannerViewModel.tryUri(Uri.parse("http://${ipAddress}:${50051}"))
            if (alive) {
                hostnameInput = "$hostnameInput <- Kimchi Robot."
            } else {
                hostnameInput = "$hostnameInput <- Couldn't establish connection."
            }

            withContext(Dispatchers.Main) {
                _deviceAdapter.addDevice(NetworkDevice(ipAddress, hostnameInput))
            }
        }
    }

    override fun onScanComplete(devices: List<String>) {
        runOnUiThread {
            _statusTextView.text = "Scan complete. Found ${devices.size} devices."
        }
    }

    override fun onDeviceClick(device: NetworkDevice) {
        lifecycleScope.launch(Dispatchers.IO) {
            val alive = _networkScannerViewModel.tryUri(Uri.parse("http://${device.ipAddress}:${50051}"))
            if (!alive) {
                val msg = "The selected device is not a kimchi robot or is not ready to connect."
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Conected to ${device.hostname}!", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }
}
