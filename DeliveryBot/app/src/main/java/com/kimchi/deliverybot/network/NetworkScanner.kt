package com.kimchi.deliverybot.network

import java.io.IOException
import java.net.InetAddress

class NetworkScanner {
    private val timeoutMs = 50
    private val connectedDevices = mutableListOf<String>()

    fun scanNetwork(subnet: String, listener: ScanListener) {
        connectedDevices.clear()
        Thread {
            for (i in 1..254) {
                val host = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(host)
                    if (address.isReachable(timeoutMs)) {
                        val deviceName = address.hostName
                        val deviceInfo = "$host ($deviceName)"
                        connectedDevices.add(deviceInfo)
                        listener.onDeviceFound(host, deviceName)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            listener.onScanComplete(connectedDevices)
        }.start()
    }

    interface ScanListener {
        fun onDeviceFound(ipAddress: String, hostname: String)
        fun onScanComplete(devices: List<String>)
    }
}