package com.project.mobilecafeserver

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.project.mobilecafeserver.adapters.AppSelectionAdapter
import com.project.mobilecafeserver.models.AppSelectionModel

class ManageAppsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var adapter: AppSelectionAdapter
    private lateinit var appList: ArrayList<AppSelectionModel>
    private lateinit var dbRef: DatabaseReference
    private var deviceId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_apps)

        // Get the Device ID passed from MainActivity
        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Error: No Device ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup UI
        recyclerView = findViewById(R.id.recyclerApps)
        btnSave = findViewById(R.id.btnSaveApps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        appList = arrayListOf()
        adapter = AppSelectionAdapter(appList)
        recyclerView.adapter = adapter

        // Setup Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("devices").child(deviceId)

        loadAppsFromFirebase()

        btnSave.setOnClickListener {
            saveWhitelist()
        }
    }

    private fun loadAppsFromFirebase() {
        // 1. Get the list of ALL installed apps (uploaded by Client)
        dbRef.child("installed_apps").get().addOnSuccessListener { installedSnap ->
            if (!installedSnap.exists()) {
                Toast.makeText(this, "Client hasn't reported apps yet. Run Client app first.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            // 2. Get the current WHITELIST
            dbRef.child("whitelist").get().addOnSuccessListener { whitelistSnap ->

                // Create a Set of allowed packages for easy lookup
                val allowedSet = HashSet<String>()
                for (child in whitelistSnap.children) {
                    val pkg = child.getValue(String::class.java)
                    if (pkg != null) allowedSet.add(pkg)
                }

                appList.clear()

                // 3. Merge Lists
                for (appSnap in installedSnap.children) {
                    // Firebase keys replace '.' with '_', so we just need the Value (Name)
                    // and we can reconstruct package name or store it differently.
                    // IMPORTANT: In Client, we saved it as Map<PackageName_Safe, AppName>

                    val safePackageName = appSnap.key ?: ""
                    val appName = appSnap.value.toString()

                    // Restore '.' for the real package name
                    val realPackageName = safePackageName.replace("_", ".")

                    val isAllowed = allowedSet.contains(realPackageName)

                    appList.add(AppSelectionModel(appName, realPackageName, isAllowed))
                }

                // Sort alphabetically
                appList.sortBy { it.appName }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun saveWhitelist() {
        val allowedList = ArrayList<String>()

        // Collect checked items
        for (app in appList) {
            if (app.isAllowed) {
                allowedList.add(app.packageName)
            }
        }

        // Save to Firebase
        dbRef.child("whitelist").setValue(allowedList)
            .addOnSuccessListener {
                Toast.makeText(this, "Whitelist Updated!", Toast.LENGTH_SHORT).show()
                finish() // Close screen
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
            }
    }
}