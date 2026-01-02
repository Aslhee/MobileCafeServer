package com.project.mobilecafeserver

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class DeviceAdapter(
    private val deviceList: ArrayList<DeviceModel>,
    private val onAddTime: (DeviceModel) -> Unit, // Action when "Add 1 Hr" is clicked
    private val onLock: (DeviceModel) -> Unit,     // Action when "Lock" is clicked
    private val onPause: (DeviceModel) -> Unit // <--- Add this new action
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    // VARIABLE TO TRACK MODE
    var isSelectionMode = false

    // This class holds the UI elements from item_device.xml
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnAdd: Button = itemView.findViewById(R.id.btnAddTime)
        val btnLock: Button = itemView.findViewById(R.id.btnLock)
        val btnPause: Button = itemView.findViewById(R.id.btnPause) // <--- Bind it

        // NEW CHECKBOX
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
    }

    // 1. Create the view (inflate the XML)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    // 2. Bind the data to the view (This runs for every row)
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]

        // 1. Set Name
        holder.tvName.text = device.name

        // 2. Status Logic (Colors & Text)
        val currentTime = System.currentTimeMillis()

        when (device.status) {
            "ACTIVE" -> {
                val remainingMillis = device.endTime - currentTime
                if (remainingMillis > 0) {
                    val minutes = (remainingMillis / 1000) / 60
                    val seconds = (remainingMillis / 1000) % 60
                    val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                    holder.tvStatus.text = "Status: ACTIVE ($timeString left)"
                    holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
                } else {
                    holder.tvStatus.text = "Status: EXPIRED (Waiting for Lock)"
                    holder.tvStatus.setTextColor(Color.RED)
                }
                holder.btnPause.text = "Pause"
                holder.btnPause.isEnabled = true
            }
            "PAUSED" -> {
                val savedMillis = device.savedTime
                val minutes = (savedMillis / 1000) / 60
                val seconds = (savedMillis / 1000) % 60
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                holder.tvStatus.text = "Status: PAUSED ($timeString frozen)"
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")) // Orange
                holder.btnPause.text = "Resume"
                holder.btnPause.isEnabled = true
            }
            else -> { // LOCKED
                holder.tvStatus.text = "Status: LOCKED"
                holder.tvStatus.setTextColor(Color.RED)
                holder.btnPause.text = "Pause"
                holder.btnPause.isEnabled = false
            }
        }

        // 3. Selection Mode Logic (The Checkbox)
        if (isSelectionMode) {
            holder.cbSelect.visibility = View.VISIBLE
        } else {
            holder.cbSelect.visibility = View.GONE
            device.isSelected = false // Reset if mode is off
        }

        // Prevent weird checkbox behavior when scrolling
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = device.isSelected
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            device.isSelected = isChecked
        }

        // 4. BUTTON CLICK LISTENERS (This is likely what was missing!)
        holder.btnAdd.setOnClickListener { onAddTime(device) }
        holder.btnLock.setOnClickListener { onLock(device) }
        holder.btnPause.setOnClickListener { onPause(device) }
    }

    override fun getItemCount(): Int = deviceList.size
}