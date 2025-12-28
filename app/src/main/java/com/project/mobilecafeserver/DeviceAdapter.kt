package com.project.mobilecafeserver

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val deviceList: ArrayList<DeviceModel>,
    private val onAddTime: (DeviceModel) -> Unit, // Action when "Add 1 Hr" is clicked
    private val onLock: (DeviceModel) -> Unit,     // Action when "Lock" is clicked
    private val onPause: (DeviceModel) -> Unit // <--- Add this new action
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    // This class holds the UI elements from item_device.xml
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnAdd: Button = itemView.findViewById(R.id.btnAddTime)
        val btnLock: Button = itemView.findViewById(R.id.btnLock)
        val btnPause: Button = itemView.findViewById(R.id.btnPause) // <--- Bind it
    }

    // 1. Create the view (inflate the XML)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    // 2. Bind the data to the view (This runs for every row)
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]

        // Set Name
        holder.tvName.text = device.name

        // Set Status and Logic
        if (device.status == "ACTIVE") {
            // Calculate remaining time for display
            val remainingMillis = device.endTime - System.currentTimeMillis()
            if (remainingMillis > 0) {
// NEW CODE (Shows 54:30):
                val minutes = (remainingMillis / 1000) / 60
                val seconds = (remainingMillis / 1000) % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                holder.tvStatus.text = "Status: ACTIVE ($timeString left)"
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
            } else {
                holder.tvStatus.text = "Status: EXPIRED (Waiting for Lock)"
                holder.tvStatus.setTextColor(Color.RED)
            }
        } else {
            holder.tvStatus.text = "Status: LOCKED"
            holder.tvStatus.setTextColor(Color.RED)
        }

        // Button Click Listeners (Pass the action back to MainActivity)
        holder.btnAdd.setOnClickListener { onAddTime(device) }
        holder.btnLock.setOnClickListener { onLock(device) }
        holder.btnPause.setOnClickListener { onPause(device) } // <--- Click
    }

    // 3. How many items are there?
    override fun getItemCount(): Int = deviceList.size
}