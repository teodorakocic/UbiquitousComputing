package com.example.ubicompapplication

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.getDefaultAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.ubicompapplication.Constants.Companion.NOTIFICATION_CODE
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.IOException
import java.util.UUID
import kotlin.math.sqrt

class StabilityActivity : AppCompatActivity() {

    companion object {
        var serviceUUID: UUID = UUID.fromString(Constants.BLE_SERVICE)
        var bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        var address: String? = null
    }

    private lateinit var tvAccelerationValue: TextView
    private lateinit var tvGyroscopeValue: TextView
    private lateinit var tvParkingSensor: TextView
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvESC: TextView
    private lateinit var ivParkingSensorsActive: ImageView
    private lateinit var ivESCActive: ImageView
    private lateinit var bottomMenu: BottomNavigationView
    private var acceleration = 0.0
    private var gyro = 0.0
    private lateinit var mqttClient: MqttConnection
    private lateinit var preferences: SharedPreferences
    private lateinit var edit: SharedPreferences.Editor

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stability)

        tvAccelerationValue = findViewById(R.id.tv_acceleration_value)
        tvGyroscopeValue = findViewById(R.id.tv_gyroscope_value)
        tvParkingSensor = findViewById(R.id.tv_parking_sensor_value)
        tvSpeedValue = findViewById(R.id.tv_speed_value)
        tvESC = findViewById(R.id.tv_esc_value)
        ivParkingSensorsActive = findViewById(R.id.iv_active_parking_sensor)
        ivESCActive = findViewById(R.id.iv_active_esc)
        bottomMenu = findViewById(R.id.nav_bottom)

        bottomMenu.menu.findItem(R.id.stabilityFragment).isChecked = true

        bottomMenu.menu.findItem(R.id.homeFragment).setOnMenuItemClickListener {
            val intent = Intent(this@StabilityActivity , HomeActivity::class.java)
            startActivity(intent)
            return@setOnMenuItemClickListener false
        }

        bottomMenu.menu.findItem(R.id.controlsFragment).setOnMenuItemClickListener {
            val intent = Intent(this@StabilityActivity, ControlsActivity::class.java)
            startActivity(intent)
            return@setOnMenuItemClickListener false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(address != null) {
                address = intent.getStringExtra(DeviceSelectActivity.EXTRA_ADDRESS).toString()
                connectToDevice()
                sendCommand("x")
            }
        }

        preferences = this.getSharedPreferences(Constants.PREFERENCE_SMART_CAR, Context.MODE_PRIVATE)
        edit = preferences.edit()

        tvAccelerationValue.text = acceleration.toString()
        tvGyroscopeValue.text = gyro.toString()

        if(preferences.getBoolean("engine", false)) {
            tvParkingSensor.text = "deactivated"
            ivParkingSensorsActive.visibility = View.INVISIBLE
            tvESC.text = "deactivated"
            ivESCActive.visibility = View.INVISIBLE
            mqttClient = MqttConnection(applicationContext, arrayOf(Constants.TOPIC), IntArray(1) { 0 })
            mqttClient.init()
            initMqttStatusListener()
            mqttClient.connect()
        } else {
            engineStoppedUI()
            Toast.makeText(this@StabilityActivity, "Start the vehicle so that the stability system can be turned on!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initMqttStatusListener() {
        mqttClient.mqttStatusListener = object : MqttStatusListener {

            override fun onConnectFailure(exception: Throwable) {
                displayInDebugLog("Failed to connect")
            }

            override fun onConnectionLost(exception: Throwable) {
                displayInDebugLog("The Connection was lost.")
            }

            @SuppressLint("SetTextI18n")
            override fun onMessageArrived(topic: String, message: MqttMessage) {
                if(topic.contains("alarmLight")) {
                    receivedLightAlarm(message)
                }
                if(topic.contains("motion")) {
                    receivedMovement(message)
                }
                if(topic.contains("alarmProximity")) {
                    receivedProximityAlarm(message)
                }
                displayInMessagesList(String(message.payload))
            }

            override fun onDisconnectService() {
                displayInDebugLog("The Connection was stopped.")
            }

            override fun onTopicSubscriptionSuccess() {
                displayInDebugLog("Subscribed!")
            }

            override fun onTopicSubscriptionError(exception: Throwable) {
                displayInDebugLog("Failed to subscribe")
            }
        }
    }

    private fun displayInMessagesList(message: String) {
        Log.d("new received message", message)
    }

    private fun displayInDebugLog(message: String) {
        Log.i("display in debug", message)
    }

    @SuppressLint("SetTextI18n")
    private fun receivedMovement(message: MqttMessage) {
        val currentAt = readRuleValue(String(message.payload), Constants.ACC_GYRO_STREAM)[1]
        val currentAn = readRuleValue(String(message.payload), Constants.ACC_GYRO_STREAM)[0]
        val omega = readRuleValue(String(message.payload), Constants.ACC_GYRO_STREAM)[5]

        val currentRadius = currentAn.toDouble() / (omega.toDouble() * omega.toDouble())

        tvAccelerationValue.text = "$currentAt m/s^2"
        if(currentAt.toDouble() < 0.0) {
            tvSpeedValue.text = "speeding up"
        } else if (currentAt.toDouble() > 0.0) {
            tvSpeedValue.text = "slowing down"
        } else {
            tvSpeedValue.text = "constant speed"
        }
        tvGyroscopeValue.text = "${String.format("%.2f", currentRadius)} m"

        if(currentRadius > Constants.ROAD_CURVE_LIMIT && currentAt.toDouble() > Constants.ACC_CURVE_LIMIT) {
            tvESC.text = "activated"
            ivESCActive.visibility = View.VISIBLE
            sendBrightnessCommand(75)
            startForegroundService()
            val animation = AlphaAnimation(0.5f, 0f)
            animation.duration = 500
            animation.interpolator = LinearInterpolator()
            animation.repeatCount = 4
            animation.repeatMode = Animation.REVERSE
            ivESCActive.startAnimation(animation)
            Handler(Looper.getMainLooper()).postDelayed({
                ivESCActive.clearAnimation()
                ivESCActive.visibility = View.INVISIBLE
                tvESC.text = "deactivated"
            }, 2_000)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun receivedProximityAlarm(message: MqttMessage) {
        if(readSingleRuleValue(String(message.payload), Constants.PROXIMITY_STREAM) == "on") {
            tvParkingSensor.text = "activated"
            ivParkingSensorsActive.visibility = View.VISIBLE
            sendBrightnessCommand(25)
            val animation = AlphaAnimation(0.5f, 0f)
            animation.duration = 500
            animation.interpolator = LinearInterpolator()
            animation.repeatCount = 4
            animation.repeatMode = Animation.REVERSE
            ivParkingSensorsActive.startAnimation(animation)
            Handler(Looper.getMainLooper()).postDelayed({
                ivParkingSensorsActive.clearAnimation()
                ivParkingSensorsActive.visibility = View.INVISIBLE
                tvParkingSensor.text = "deactivated"
            }, 2_000)
        } else {
            tvParkingSensor.text = "deactivated"
            ivParkingSensorsActive.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun receivedLightAlarm(message: MqttMessage) {
        if(readSingleRuleValue(String(message.payload), Constants.COLOR_STREAM).contains("on")) {
            edit.putBoolean("lights", true)
            edit.commit()
            sendCommand("z")
            Toast.makeText(this@StabilityActivity, "Lights turned on!", Toast.LENGTH_SHORT).show()
        } else {
            edit.putBoolean("lights", false)
            edit.commit()
            Toast.makeText(this@StabilityActivity, "Lights turned off!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
        sendCommand("y")
        stopForegroundService()
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, StabilityService::class.java)
        startService(serviceIntent)
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(this, StabilityService::class.java)
        stopService(serviceIntent)
    }

    private fun readRuleValue(payload: String, key: String): ArrayList<String> {
        val listOfValues = arrayListOf<String>()
        val jsonObject: JsonObject = Json.decodeFromString(payload)
        val values = jsonObject[key].toString()
        listOfValues.add(values.split(',').toString().replace(" ", ""))
        return listOfValues
    }

    private fun readSingleRuleValue(payload: String, key: String): String {
        val jsonObject: JsonObject = Json.decodeFromString(payload)
        return jsonObject[key].toString()
    }

    private fun engineStoppedUI() {
        tvParkingSensor.text = "--"
        ivParkingSensorsActive.visibility = View.INVISIBLE
        tvESC.text = "--"
        ivESCActive.visibility = View.INVISIBLE
        tvSpeedValue.text = "--"
    }

    private fun checkPermissions(permission: String, requestCode: Int) {
        if(checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(permission), requestCode)
        } else {
//            Toast.makeText(this, "Permission already granted!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == NOTIFICATION_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendCommand(input: String) {
        if (BleControlActivity.bluetoothSocket != null) {
            try {
                BleControlActivity.bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendIntCommand(input: Int) {
        if (BleControlActivity.bluetoothSocket != null) {
            try {
                BleControlActivity.bluetoothSocket!!.outputStream.write(input)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (BleControlActivity.bluetoothSocket != null) {
            try {
                BleControlActivity.bluetoothSocket!!.close()
                BleControlActivity.bluetoothSocket = null
                BleControlActivity.isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private fun sendBrightnessCommand(valueSend: Int) {
        sendIntCommand(valueSend)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        progress = ProgressDialog.show(this, "Connecting...", "please wait")
        GlobalScope.launch(Dispatchers.Main) {
            var connectSuccess = true
            try {
                if (bluetoothSocket == null || !isConnected) {
                    bluetoothAdapter = getDefaultAdapter()
                    val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(
                        address
                    )
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(
                        serviceUUID
                    )
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
                    Toast.makeText(this@StabilityActivity, "Connected to device: $deviceName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@StabilityActivity, "Could not connect to device", Toast.LENGTH_SHORT).show()
                    finish()
                }
                isConnected = connectSuccess
            }
        }
    }
}