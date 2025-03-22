package com.kimchi.deliverybot.network

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kimchi.deliverybot.R

class DeviceAdapter(private val onDeviceClickListener: OnDeviceClickListener): RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    interface OnDeviceClickListener {
        fun onDeviceClick(device: NetworkDevice)
    }

    private val devices = mutableListOf<NetworkDevice>()

    fun updateDevices(newDevices: List<NetworkDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    fun addDevice(device: NetworkDevice) {
        devices.add(device)
        notifyItemInserted(devices.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)

        // Set click listener for the entire item view
        holder.itemView.setOnClickListener {
            onDeviceClickListener.onDeviceClick(device)
        }
    }

    override fun getItemCount() = devices.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ipTextView: TextView = itemView.findViewById(R.id.deviceIpTextView)
        private val hostnameTextView: TextView = itemView.findViewById(R.id.deviceHostnameTextView)

        fun bind(device: NetworkDevice) {
            ipTextView.text = device.ipAddress
            hostnameTextView.text = device.hostname
        }
    }
}