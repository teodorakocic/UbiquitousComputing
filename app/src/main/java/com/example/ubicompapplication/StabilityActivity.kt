package com.example.ubicompapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ubicompapplication.Constants.Companion.LED_SERVICE
import com.example.ubicompapplication.Constants.Companion.NOTIFICATION_CODE
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.IOException
import java.math.BigInteger
import java.util.UUID


@SuppressLint("MissingPermission")
class StabilityActivity : AppCompatActivity() {
    private lateinit var tvAccelerationValue: TextView
    private lateinit var tvGyroscopeValue: TextView
    private lateinit var tvParkingSensor: TextView
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvESC: TextView
    private lateinit var ivParkingSensorsActive: ImageView
    private lateinit var ivESCActive: ImageView
    private lateinit var llTopControls: LinearLayout
    private lateinit var llBottomControls: LinearLayout
    private lateinit var ivBottom: ImageView
    private lateinit var bottomMenu: BottomNavigationView
    private var acceleration = 0.0
    private var gyro = 0.0
    private lateinit var mqttClient: MqttConnection
    private lateinit var preferences: SharedPreferences
    private lateinit var edit: SharedPreferences.Editor
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var btnScan: ImageView
    private lateinit var tvScan: TextView
    private lateinit var svScan: ScrollView
    private lateinit var rvScannedDevices: RecyclerView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var scanResults =  mutableListOf<ScanResult>()
    private val scanResultAdapter: BleDeviceAdapter by lazy {
        BleDeviceAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device){
                Log.w("ScanResultAdapter", "Connecting to $address")
                connectGatt(this@StabilityActivity, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SetTextI18n", "MissingPermission")
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
        llTopControls = findViewById(R.id.ll_activate)
        llBottomControls = findViewById(R.id.ll_activate_values)
        ivBottom = findViewById(R.id.iv_zigzag_2)
        bottomMenu = findViewById(R.id.nav_bottom)
        btnScan = findViewById(R.id.iv_ble)
        tvScan = findViewById(R.id.tv_tap_to_connect)
        svScan = findViewById(R.id.sv_scanned_devices)
        rvScannedDevices = findViewById(R.id.rv_scanned_devices)

        if (!isPermissionsGranted(this@StabilityActivity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permissions = mutableSetOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                }
                ActivityCompat.requestPermissions(this@StabilityActivity, permissions.toTypedArray(), 600)
            }
        }

        btnScan.setOnClickListener {
            if(bluetoothAdapter.isEnabled)
            {
                if(isScanning){
                    llTopControls.visibility = View.GONE
                    llBottomControls.visibility = View.GONE
                    ivBottom.visibility = View.GONE
                    svScan.visibility = View.VISIBLE
                    rvScannedDevices.visibility = View.VISIBLE
                    tvScan.text = "Tap to stop scan"
                    rvScannedDevices.adapter = scanResultAdapter
                    rvScannedDevices.layoutManager = LinearLayoutManager(this@StabilityActivity)
                    scanResultAdapter.setOnItemClickListener(object : BleDeviceAdapter.OnItemClickListener {
                        override fun onItemClick(position: Int) {
                            Log.w("ScanResultAdapter", "Connecting to ${scanResults[position].device.address}")
                            scanResults[position].device.connectGatt(this@StabilityActivity, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                            Toast.makeText(this@StabilityActivity, "Successfully connected ${scanResults[position].device.name} [${scanResults[position].device.address}]", Toast.LENGTH_SHORT).show()
                        }
                    })
                    stopBleScan()
                }
                else {
                    tvScan.text = "Tap to connect sensors"
                    llTopControls.visibility = View.VISIBLE
                    llBottomControls.visibility = View.VISIBLE
                    ivBottom.visibility = View.VISIBLE
                    svScan.visibility = View.GONE
                    rvScannedDevices.visibility = View.GONE
                    startBleScan()
                }
            }
        }

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

        val bluetoothManager: BluetoothManager? = ContextCompat.getSystemService(applicationContext, BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager!!.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Wrote to characteristic ${this?.uuid} | value: ${this?.value?.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for ${this?.uuid}!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed for ${this?.uuid}, error: $status")
                    }
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt.discoverServices()
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        // send 2 for blinking LED to notify user that connection was successful
                        sendIntCommand(bluetoothGatt, 2)
                    }, 3000)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("GATT", "$newState")
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            val characteristic: BluetoothGattCharacteristic = gatt.getService(serviceUUID).getCharacteristic(serviceUUID)
//        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                bluetoothGatt = gatt
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable()
                readChValue()
            }
        }
    }

    private lateinit var bleScanner: BluetoothLeScanner

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) {
                scanResults[indexQuery] = result
            } else {
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${result.device.name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }
    private var isScanning: Boolean = false

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    private fun hasPermission(permissionType: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBleScan() {
        if (!isLocationPermissionGranted) {
            requestLocationPermission()
        }
        else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if(isLocationPermissionGranted)
            return
        ActivityCompat.requestPermissions(this@StabilityActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            Constants.LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    private fun readChValue() {
//        val charUuid = convertFromInteger(Constants.BLE_CHARACTERISTIC)
        val characteristic = bluetoothGatt.getService(UUID.fromString(LED_SERVICE))?.getCharacteristic(UUID.fromString(Constants.BUTTON_CHARACTERISTIC))
        if (characteristic?.isReadable() == true) {
            bluetoothGatt.readCharacteristic(characteristic)
        }
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, payload, writeType)
            } else {
                gatt.writeCharacteristic(characteristic)?: error("Not connected to a BLE device!")
            }
        }
    }

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

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
            //send command for ESC notification
            sendIntCommand(bluetoothGatt, 3)
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
            //parking sensors notification
            sendIntCommand(bluetoothGatt, 4)
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
            //notify user to turn on the lights
            sendIntCommand(bluetoothGatt, 5)
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
        sendIntCommand(bluetoothGatt, 0)
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
        when (requestCode) {
            Constants.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
            NOTIFICATION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendIntCommand(bleGatt: BluetoothGatt, input: Int) {
        writeCharacteristic(bleGatt.getService(UUID.fromString(LED_SERVICE)).getCharacteristic(UUID.fromString(Constants.LED_CHARACTERISTIC)), BigInteger.valueOf(input.toLong()).toByteArray())
    }

    private fun sendBrightnessCommand(bleGatt: BluetoothGatt, valueSend: Int) {
        sendIntCommand(bleGatt, valueSend)
    }

    private fun convertFromInteger(i: Int): UUID {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and -0x1).toLong()
        return UUID(msb or (value shl 32), lsb)
    }
}