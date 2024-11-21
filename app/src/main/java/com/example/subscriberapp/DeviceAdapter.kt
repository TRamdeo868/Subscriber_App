package com.example.subscriberapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceClick: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val studentId: TextView = view.findViewById(R.id.studentId)
        val minSpeed: TextView = view.findViewById(R.id.minSpeed)
        val maxSpeed: TextView = view.findViewById(R.id.maxSpeed)
        val viewMoreButton: Button = view.findViewById(R.id.viewMoreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.studentId.text = "ID: ${device.studentId}"
        holder.minSpeed.text = "Min Speed: ${device.minSpeed}"
        holder.maxSpeed.text = "Max Speed: ${device.maxSpeed}"
        holder.viewMoreButton.setOnClickListener {
            onDeviceClick(device.studentId)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}
