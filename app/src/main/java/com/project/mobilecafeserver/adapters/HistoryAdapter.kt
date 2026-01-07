package com.project.mobilecafeserver.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.mobilecafeserver.R
import com.project.mobilecafeserver.models.HistoryModel

class HistoryAdapter(
    private val historyList: List<HistoryModel>,
    private val onFaceClick: (String) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMid: TextView = itemView.findViewById(R.id.tvMid)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val ivFace: ImageView = itemView.findViewById(R.id.ivFace)
        val ivLocation: ImageView = itemView.findViewById(R.id.ivLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_record, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = historyList[position]

        holder.tvMid.text = record.mobileId
        holder.tvTime.text = record.timeDuration
        holder.tvAmount.text = record.amount

        // --- FACE ICON LOGIC ---
        if (record.hasFaceData) {
            holder.ivFace.setColorFilter(Color.parseColor("#4CAF50")) // Green
            holder.ivFace.alpha = 1.0f

            // Enable Click
            holder.ivFace.setOnClickListener {
                onFaceClick(record.id) // Pass the ID back to the Activity
            }
        } else {
            holder.ivFace.setColorFilter(Color.LTGRAY) // Gray
            holder.ivFace.alpha = 0.3f
            holder.ivFace.setOnClickListener(null) // Disable Click
        }

        // --- LOCATION ICON LOGIC ---
        if (record.hasLocationData) {
            holder.ivLocation.setColorFilter(Color.parseColor("#2196F3")) // Blue (Active)
            holder.ivLocation.alpha = 1.0f
        } else {
            holder.ivLocation.setColorFilter(Color.LTGRAY) // Gray (No Data)
            holder.ivLocation.alpha = 0.3f
        }
    }

    override fun getItemCount(): Int = historyList.size
}