package com.example.ubicompapplication

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class BleControlActivity : AppCompatActivity() {

    companion object {
        var serviceUUID: UUID = UUID.fromString(Constants.BLE_SERVICE)
        var bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String
    }

    private lateinit var etBrightness: EditText
    private lateinit var btnBlink: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnDisconnect: Button
    private lateinit var sbBrightness: SeekBar
    private lateinit var tvBrightnessValue: TextView
    private lateinit var swLED: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_control)

        etBrightness = findViewById(R.id.et_brightness_input)
        btnBlink = findViewById(R.id.btn_control_led)
        btnSubmit = findViewById(R.id.btn_submit)
        btnDisconnect = findViewById(R.id.btn_disconnect)
        sbBrightness = findViewById(R.id.brightness_slider)
        tvBrightnessValue = findViewById(R.id.tv_brightness)
        swLED = findViewById(R.id.sw_control_led)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(address != null) {
                address = intent.getStringExtra(DeviceSelectActivity.EXTRA_ADDRESS).toString()
                connectToDevice()
            }


            swLED.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Turn on the LED (Send the "x" command to the Bluetooth device)
                    sendCommand("x")
                } else {
                    // Turn off the LED (Send the "y" command to the Bluetooth device)
                    sendCommand("y")
                }
            }

            btnBlink.setOnClickListener { sendCommand("z") }
            btnDisconnect.setOnClickListener { disconnect() }
            btnSubmit.setOnClickListener { sendBrightnessCommand() }

            sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val brightnessText = "Brightness: $progress"
                    tvBrightnessValue.text = brightnessText
                    sendIntCommand(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun sendCommand(input: String) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendIntCommand(input: Int) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.outputStream.write(input)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.close()
                bluetoothSocket = null
                isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private fun sendBrightnessCommand() {
        val brightnessText = etBrightness.text.toString()
        if (brightnessText.isNotEmpty()) {
            val brightnessValue = brightnessText.toIntOrNull()
            if ((brightnessValue != null) && (brightnessValue >= 0) && (brightnessValue <= 100)) {
                sendIntCommand(brightnessValue)
            } else {
                Toast.makeText(this@BleControlActivity, "Invalid input; must be between 0-100!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@BleControlActivity, "Please enter a brightness value", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        progress = ProgressDialog.show(this, "Connecting...", "please wait")
        GlobalScope.launch(Dispatchers.Main) {
            var connectSuccess = true
            try {
                if (bluetoothSocket == null || !isConnected) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(serviceUUID)
                    bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }

            launch(Dispatchers.Main) {
                progress.dismiss()
                if (connectSuccess) {
                    val deviceName = bluetoothSocket!!.remoteDevice.name
                    Toast.makeText(this@BleControlActivity, "Connected to device: $deviceName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@BleControlActivity, "Could not connect to device", Toast.LENGTH_SHORT).show()
                    finish()
                }
                isConnected = connectSuccess
            }
        }
    }
}