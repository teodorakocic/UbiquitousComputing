package com.example.ubicompapplication

import android.app.Activity
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat

class DeviceSelectActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private lateinit var btnRefresh: Button
    private lateinit var lvDevices: ListView

    companion object {
        const val EXTRA_ADDRESS: String = "Device_address"
        private const val PERMISSION_REQUEST_CODE = 2
    }

    data class BluetoothDeviceInfo(val name: String, val address: String) {
        override fun toString(): String {
            return "$name\n$address"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_select)

        btnRefresh = findViewById(R.id.btn_refresh)
        lvDevices = findViewById(R.id.lv_devices)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this@DeviceSelectActivity, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        } else {
            checkBluetoothPermission()
        }

        btnRefresh.setOnClickListener { pairedDeviceList() }
        Log.d("SelectDeviceActivity", "Inside onCreate()")
    }

    private fun pairedDeviceList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            return
        }

        // Permission is already granted, continue with the logic
        Log.d("SelectDeviceActivity", "Bluetooth permission granted")

        pairedDevices = bluetoothAdapter!!.bondedDevices
        val list: ArrayList<BluetoothDeviceInfo> = ArrayList()

        if (pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
                val name = device.name ?: "Unknown Device"
                val address = device.address
                val deviceInfo = BluetoothDeviceInfo(name, address)
                list.add(deviceInfo)
                Log.i("device", deviceInfo.toString())
            }
        } else {
            Toast.makeText(this@DeviceSelectActivity, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show()
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
            } else {
                checkBluetoothPermission()
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        lvDevices.adapter = adapter
        lvDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDeviceInfo = list[position]
            val address: String = device.address

            val intent = Intent(this, BleControlActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }
    }


    private fun checkBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), PERMISSION_REQUEST_CODE)
        } else {
            // Permission is already granted, continue with the logic
            pairedDeviceList()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@DeviceSelectActivity,  "Bluetooth permission granted", Toast.LENGTH_SHORT).show()
                pairedDeviceList()
            } else {
                Toast.makeText(this@DeviceSelectActivity,  "Bluetooth permission denied. This app requires Bluetooth permission to function. Please grant the permission in the app settings.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH){
            if (resultCode == Activity.RESULT_OK){
                if(bluetoothAdapter!!.isEnabled){
                    Toast.makeText(this@DeviceSelectActivity, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@DeviceSelectActivity, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if(resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this@DeviceSelectActivity, "Bluetooth has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}