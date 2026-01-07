package com.project.mobilecafeserver

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.project.mobilecafeserver.adapters.DeviceAdapter
import com.project.mobilecafeserver.models.DeviceModel
import com.project.mobilecafeserver.models.HistoryModel

class MainActivity : AppCompatActivity() {

    // Variables for Firebase and the List
    private lateinit var dbRef: DatabaseReference
    private lateinit var deviceList: ArrayList<DeviceModel>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var cbToggleSelection: android.widget.CheckBox
    private lateinit var btnDeleteSelected: android.widget.ImageView

    // Create the "Runnable" (The Task)
    private val refreshTask = object : Runnable {
        override fun run() {
            // Force the list to re-bind (Recalculate "X mins left")
            if (::adapter.isInitialized) {
                adapter.notifyDataSetChanged()
            }

            // Schedule this same task to run again in 30 seconds (30000ms)
            // You can make this 1000ms if you want to see seconds ticking,
            // but 30 seconds is better for battery.
            handler.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        // Start the loop when app is open
        handler.post(refreshTask)
    }

    override fun onPause() {
        super.onPause()
        // Stop the loop when app is minimized/closed
        handler.removeCallbacks(refreshTask)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Setup Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        // 2. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewDevices)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        // 3. Initialize List & Adapter FIRST (Before setting listeners)
        deviceList = arrayListOf()
        adapter = DeviceAdapter(deviceList,
            onAddTime = { device -> showTimeDialog(device) },
            onLock = { device -> lockDevice(device) },
            onPause = { device -> togglePause(device) }
        )
        recyclerView.adapter = adapter

        // 4. Bind Toolbar Views
        cbToggleSelection = findViewById(R.id.cbToggleSelection)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)
        val btnMenu = findViewById<android.widget.ImageView>(R.id.btnMenu)
        val btnManageApps = findViewById<android.widget.ImageView>(R.id.btnManageApps)

        // 5. Logic: Selection Toggle
        cbToggleSelection.setOnCheckedChangeListener { _, isChecked ->
            // Tell Adapter to switch modes
            adapter.isSelectionMode = isChecked

            // Show/Hide BOTH Action Buttons
            if (isChecked) {
                btnDeleteSelected.visibility = View.VISIBLE
                btnManageApps.visibility = View.VISIBLE
            } else {
                btnDeleteSelected.visibility = View.GONE
                btnManageApps.visibility = View.GONE
            }

            // Refresh list to show/hide checkboxes
            adapter.notifyDataSetChanged()
        }

        // 6. Logic: Manage Apps (Edit Button)
        btnManageApps.setOnClickListener {
            val selectedItems = deviceList.filter { it.isSelected }

            if (selectedItems.size == 1) {
                val device = selectedItems[0]
                val intent = android.content.Intent(this, ManageAppsActivity::class.java)
                intent.putExtra("DEVICE_ID", device.deviceId)
                startActivity(intent)
            } else {
                android.widget.Toast.makeText(this, "Please select EXACTLY ONE device to manage apps.", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        // 7. Logic: Delete Button
        btnDeleteSelected.setOnClickListener {
            deleteSelectedDevices()
        }

        // 8. Logic: History Menu
        btnMenu.setOnClickListener {
            val intent = android.content.Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // 9. Start Data Sync
        getDevicesData()
        createDummyDevice()
    }

    private fun togglePause(device: DeviceModel) {
        val currentTime = System.currentTimeMillis()

        if (device.status == "ACTIVE") {
            // --- PAUSE LOGIC ---
            val timeLeft = device.endTime - currentTime

            if (timeLeft > 0) {
                val updates = mapOf(
                    "status" to "PAUSED",
                    "savedTime" to timeLeft, // Save the time
                    "endTime" to 0
                )
                dbRef.child(device.deviceId).updateChildren(updates)
                Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Time expired", Toast.LENGTH_SHORT).show()
            }

        } else if (device.status == "PAUSED") {
            // --- RESUME LOGIC ---
            var timeToRestore = device.savedTime

            //  SAFETY FIX: If savedTime is 0 (bug), give 1 minute so it doesn't lock instantly
            if (timeToRestore <= 0) {
                timeToRestore = 60000L // 1 Minute default
            }

            // Calculate new End Time
            val newEndTime = currentTime + timeToRestore

            val updates = mapOf(
                "status" to "ACTIVE",
                "endTime" to newEndTime,
                "savedTime" to 0
            )
            dbRef.child(device.deviceId).updateChildren(updates)
            Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedDevices() {
        val itemsToDelete = deviceList.filter { it.isSelected }

        if (itemsToDelete.isEmpty()) {
            Toast.makeText(this, "No devices selected", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Devices?")
            .setMessage("Are you sure you want to remove ${itemsToDelete.size} devices?")
            .setPositiveButton("DELETE") { _, _ ->

                // Delete from Firebase
                for (device in itemsToDelete) {
                    dbRef.child(device.deviceId).removeValue()
                }

                Toast.makeText(this, "Deleted ${itemsToDelete.size} devices", Toast.LENGTH_SHORT).show()

                // Reset Selection Mode
                cbToggleSelection.isChecked = false
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // --- NEW FUNCTION: Show the Dialog ---
    private fun showTimeDialog(device: DeviceModel) {
        // 1. Inflate the XML layout
        val dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_add_time, null)

        // 2. Create the Alert Dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false) // THIS PREVENTS CLICKING OUTSIDE/BACK BUTTON

        val dialog = builder.create()

        // Make background transparent so the CardView rounded corners show up nicely
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // 3. Setup Buttons
        val btnClose = dialogView.findViewById<android.view.View>(R.id.imgClose)
        val btn15 = dialogView.findViewById<android.view.View>(R.id.btn15Min)
        val btn30 = dialogView.findViewById<android.view.View>(R.id.btn30Min)
        val btn60 = dialogView.findViewById<android.view.View>(R.id.btn1Hour)
        val btn120 = dialogView.findViewById<android.view.View>(R.id.btn2Hours)

        // Close Logic
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Time Logic
        btn15.setOnClickListener {
            addTime(device, 1) // Add 15 mins
            dialog.dismiss()
        }
        btn30.setOnClickListener {
            addTime(device, 30) // Add 30 mins
            dialog.dismiss()
        }
        btn60.setOnClickListener {
            addTime(device, 60) // Add 60 mins (1 hr)
            dialog.dismiss()
        }
        btn120.setOnClickListener {
            addTime(device, 120) // Add 120 mins (2 hrs)
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun getDevicesData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceList.clear() // 1. Wipe the old list
                if (snapshot.exists()) {
                    for (deviceSnap in snapshot.children) {
                        val device = deviceSnap.getValue(DeviceModel::class.java)
                        if (device != null) {
                            deviceList.add(device) // 2. Add the updated data (Status: PAUSED)
                        }
                    }
                    // 3. FORCE THE ADAPTER TO REDRAW
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- BUTTON LOGIC ---
    private fun addTime(device: DeviceModel, minutesToAdd: Int) {
        val timeToAddMillis = minutesToAdd * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        // 1. GENERATE HISTORY RECORD FIRST
        val historyRef = dbRef.parent?.child("history")?.push()
        val historyId = historyRef?.key ?: "" // Get the unique key (e.g., "-NXY123...")

        // Save the history data immediately
        val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
        val record = HistoryModel(
            mobileId = device.name,
            timeDuration = "$minutesToAdd Mins",
            amount = calculatePrice(minutesToAdd),
            timestamp = sdf.format(java.util.Date()),
            hasFaceData = false, // Will become true after client uploads
            hasLocationData = false
        )
        historyRef?.setValue(record)

        // 2. CALCULATE TIME (Existing Logic)
        dbRef.child(device.deviceId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentStatus = snapshot.child("status").getValue(String::class.java) ?: "LOCKED"
                val currentEndTime = snapshot.child("endTime").getValue(Long::class.java) ?: 0L
                val currentSavedTime = snapshot.child("savedTime").getValue(Long::class.java) ?: 0L

                var newEndTime: Long = 0
                var newSavedTime: Long = 0

                // CRITICAL CHANGE: Status becomes "UNLOCKING" to trigger camera
                var newStatus = "UNLOCKING"

                if (currentStatus == "ACTIVE" && currentEndTime > currentTime) {
                    newEndTime = currentEndTime + timeToAddMillis
                    newStatus = "ACTIVE" // Already active, no need to photo again
                }
                else if (currentStatus == "PAUSED") {
                    newSavedTime = currentSavedTime + timeToAddMillis
                    newStatus = "PAUSED"
                }
                else {
                    // Locked or Expired -> Trigger Camera
                    newEndTime = currentTime + timeToAddMillis
                    newStatus = "UNLOCKING"
                }

                // 3. SEND COMMAND TO CLIENT
                val updates = mapOf(
                    "status" to newStatus,
                    "endTime" to newEndTime,
                    "savedTime" to newSavedTime,
                    "currentSessionId" to historyId // Pass the ticket!
                )

                dbRef.child(device.deviceId).updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Added $minutesToAdd mins", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun calculatePrice(minutes: Int): String {
        // DEFINE YOUR PRICING HERE
        // Example: 15 pesos per hour (Change logic as needed)
        val price = when (minutes) {
            15 -> 5.00
            30 -> 10.00
            60 -> 20.00
            120 -> 40.00
            else -> 0.00
        }
        return "P $price"
    }


    private fun lockDevice(device: DeviceModel) {
        // Prepare the update (Reset time to 0)
        val updates = mapOf(
            "status" to "LOCKED",
            "endTime" to 0L
        )

        // Send to Firebase
        dbRef.child(device.deviceId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Locked ${device.name}", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper: Creates a "Unit 1" automatically if your database is empty
    private fun createDummyDevice() {
        val id = "mobile_01"
        // Check if it exists first
        dbRef.child(id).get().addOnSuccessListener {
            if (!it.exists()) {
                val newDevice = DeviceModel(id, "Station 1 (Vivo)", "LOCKED", 0)
                dbRef.child(id).setValue(newDevice)
            }
        }
    }
}