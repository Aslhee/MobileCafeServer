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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 1. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // This makes new items appear at the top (optional)
        (recyclerView.layoutManager as LinearLayoutManager).reverseLayout = true
        (recyclerView.layoutManager as LinearLayoutManager).stackFromEnd = true

        historyList = arrayListOf()
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter

        // 2. Connect to Firebase "history" node
        dbRef = FirebaseDatabase.getInstance().getReference("history")

        // 3. Fetch Data
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                if (snapshot.exists()) {
                    for (recordSnap in snapshot.children) {
                        val record = recordSnap.getValue(HistoryModel::class.java)
                        if (record != null) {
                            historyList.add(record)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}