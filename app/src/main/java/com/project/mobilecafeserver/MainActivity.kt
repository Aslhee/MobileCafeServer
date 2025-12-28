package com.project.mobilecafeserver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    // Variables for Firebase and the List
    private lateinit var dbRef: DatabaseReference
    private lateinit var deviceList: ArrayList<DeviceModel>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

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
        // We look for a folder named "devices" in your database
        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        // 2. Setup the RecyclerView (The UI List)
        recyclerView = findViewById(R.id.recyclerViewDevices)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        deviceList = arrayListOf()

        // 3. Connect the Adapter
        // We pass two functions (lambdas) here: one for adding time, one for locking
        adapter = DeviceAdapter(deviceList,
            onAddTime = { device ->
                // Instead of adding time directly, SHOW THE DIALOG
                showTimeDialog(device)
            },
            onLock = { device -> lockDevice(device) },
            onPause = { device -> togglePause(device) } // <--- New Link

        )
        recyclerView.adapter = adapter

        // 4. Start Listening for Data Changes
        getDevicesData()

        // 5. Create a fake device so you have something to test immediately
        createDummyDevice()
    }

    private fun togglePause(device: DeviceModel) {
        val currentTime = System.currentTimeMillis()

        if (device.status == "ACTIVE") {
            // --- LOGIC: PAUSE THE DEVICE ---
            // 1. Calculate how much time is left right now
            val timeLeft = device.endTime - currentTime

            if (timeLeft > 0) {
                val updates = mapOf(
                    "status" to "PAUSED",
                    "savedTime" to timeLeft, // Save the remaining time
                    "endTime" to 0 // Stop the clock
                )
                dbRef.child(device.deviceId).updateChildren(updates)
                Toast.makeText(this, "Paused ${device.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Time already expired", Toast.LENGTH_SHORT).show()
            }

        } else if (device.status == "PAUSED") {
            // --- LOGIC: RESUME THE DEVICE ---
            // 1. Get the time we saved earlier
            val timeToRestore = device.savedTime

            // 2. Add it to the CURRENT time to get a NEW end time
            val newEndTime = currentTime + timeToRestore

            val updates = mapOf(
                "status" to "ACTIVE",
                "endTime" to newEndTime,
                "savedTime" to 0
            )
            dbRef.child(device.deviceId).updateChildren(updates)
            Toast.makeText(this, "Resumed ${device.name}", Toast.LENGTH_SHORT).show()
        }
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
            addTime(device, 15) // Add 15 mins
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
        // This function runs every time something changes in Firebase
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceList.clear() // Clear old list
                if (snapshot.exists()) {
                    // Loop through all devices in database
                    for (deviceSnap in snapshot.children) {
                        val device = deviceSnap.getValue(DeviceModel::class.java)
                        if (device != null) {
                            deviceList.add(device)
                        }
                    }
                    // Tell the list to refresh
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- BUTTON LOGIC ---

    // --- UPDATED LOGIC: Accepts 'minutes' as parameter ---
    private fun addTime(device: DeviceModel, minutesToAdd: Int) {
        val currentTime = System.currentTimeMillis()

        // Calculate milliseconds based on the button clicked
        val timeToAddMillis = minutesToAdd * 60 * 1000L

        // Logic: Add to existing time OR start fresh
        val newEndTime = if (device.status == "ACTIVE" && device.endTime > currentTime) {
            device.endTime + timeToAddMillis
        } else {
            currentTime + timeToAddMillis
        }

        val updates = mapOf(
            "status" to "ACTIVE",
            "endTime" to newEndTime
        )

        dbRef.child(device.deviceId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Added $minutesToAdd mins to ${device.name}", Toast.LENGTH_SHORT).show()
            }
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