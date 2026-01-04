package com.project.mobilecafeserver.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.mobilecafeserver.R
import com.project.mobilecafeserver.models.AppSelectionModel

class AppSelectionAdapter(private val appList: List<AppSelectionModel>) :
    RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvPackage: TextView = itemView.findViewById(R.id.tvPackageName)
        val cbAllow: CheckBox = itemView.findViewById(R.id.cbAllow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_selection, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.tvName.text = app.appName
        holder.tvPackage.text = app.packageName

        // Remove listener before setting state to avoid bugs
        holder.cbAllow.setOnCheckedChangeListener(null)
        holder.cbAllow.isChecked = app.isAllowed

        // Add listener back
        holder.cbAllow.setOnCheckedChangeListener { _, isChecked ->
            app.isAllowed = isChecked
        }
    }

    override fun getItemCount(): Int = appList.size
}