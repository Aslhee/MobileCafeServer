package com.project.mobilecafeserver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyList: ArrayList<HistoryModel>
    private lateinit var dbRef: DatabaseReference
    private lateinit var tvCurrentDate: android.widget.TextView // New


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        tvCurrentDate = findViewById(R.id.tvCurrentDate) // Bind the new textview
        // 1. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // This makes new items appear at the top (optional)
        (recyclerView.layoutManager as LinearLayoutManager).reverseLayout = true
        (recyclerView.layoutManager as LinearLayoutManager).stackFromEnd = true

        historyList = arrayListOf()
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter

        // 2. Get TODAY'S DATE
        // We use the same format as your timestamp: "MMM dd" (e.g. Jan 03)
        val sdfDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val sdfFilter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()) // Used for matching

        val currentDate = java.util.Date()
        val dateStringDisplay = sdfDate.format(currentDate) // Shows "Jan 03, 2026"
        val filterString = sdfFilter.format(currentDate)    // Shows "Jan 03"

        // Set the label text
        tvCurrentDate.text = "Date: $dateStringDisplay"

        // 2. Connect to Firebase "history" node
        dbRef = FirebaseDatabase.getInstance().getReference("history")

        val tvTotalAmount = findViewById<android.widget.TextView>(R.id.tvTotalAmount)

        // 3. Fetch Data
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                var totalSum = 0.0 // Variable to hold sum

                if (snapshot.exists()) {
                    for (recordSnap in snapshot.children) {
                        val record = recordSnap.getValue(HistoryModel::class.java)

                        if (record != null) {
                            // Filter by Today's Date
                            if (record.timestamp.contains(filterString)) {
                                historyList.add(record)

                                // --- CALCULATION LOGIC ---
                                // Convert "P 20.00" -> 20.00
                                try {
                                    val cleanAmount = record.amount
                                        .replace("P", "")
                                        .replace(" ", "")
                                        .trim()

                                    val value = cleanAmount.toDouble()
                                    totalSum += value
                                } catch (e: Exception) {
                                    // Ignore bad data
                                }
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()

                    // --- UPDATE TOTAL TEXT ---
                    tvTotalAmount.text = String.format("P %.2f", totalSum)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}