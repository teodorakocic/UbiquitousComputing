package com.example.ubicompapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class ControlsActivity : AppCompatActivity() {

    private lateinit var tvCurrentTemperature: TextView
    private lateinit var tvCurrentPressure: TextView
    private lateinit var tvHeatingValue: TextView
    private lateinit var tvCoolingValue: TextView
    private lateinit var tvAirwaveValue: TextView
    private lateinit var tvFanValue: TextView
    private lateinit var tvCoolerValue: TextView
    private lateinit var clHeating: ConstraintLayout
    private lateinit var clCooling: ConstraintLayout
    private lateinit var clVentilation: ConstraintLayout
    private lateinit var bottomMenu: BottomNavigationView
    private var currentTemperature: Double = 32.25
    private var currentPressure: Double = 104.21
    private lateinit var mqttClient: MqttAndroidClient

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controls)

        tvCurrentTemperature = findViewById(R.id.tv_temperature_value)
        tvCurrentPressure = findViewById(R.id.tv_pressure_value)
        tvHeatingValue = findViewById(R.id.tv_heating_value)
        tvCoolingValue = findViewById(R.id.tv_cooling_value)
        tvAirwaveValue = findViewById(R.id.tv_airwave_value)
        tvFanValue = findViewById(R.id.tv_fan_value)
        tvCoolerValue = findViewById(R.id.tv_cooler_value)
        clHeating = findViewById(R.id.cl_heating)
        clCooling = findViewById(R.id.cl_cooling)
        clVentilation = findViewById(R.id.cl_airwave)
        bottomMenu = findViewById(R.id.nav_bottom)

        bottomMenu.menu.findItem(R.id.controlsFragment).isChecked = true

        bottomMenu.menu.findItem(R.id.homeFragment).setOnMenuItemClickListener {
            val intent = Intent(this@ControlsActivity, HomeActivity::class.java)
            startActivity(intent)
            return@setOnMenuItemClickListener false
        }

        bottomMenu.menu.findItem(R.id.stabilityFragment).setOnMenuItemClickListener {
            val intent = Intent(this@ControlsActivity, StabilityActivity::class.java)
            startActivity(intent)
            return@setOnMenuItemClickListener false
        }

        tvCurrentTemperature.text = "$currentTemperature C"
        tvCurrentPressure.text = "$currentPressure kPa"

        val preferences = this.getSharedPreferences(Constants.PREFERENCE_SMART_CAR, Context.MODE_PRIVATE)
        val edit = preferences.edit()

        connect(applicationContext)
//        subscribe("test-android")
//        receiveMessages()

        if(currentTemperature in 18.0..24.0) {
            clHeating.setBackgroundResource(R.drawable.controls_shape)
            clCooling.setBackgroundResource(R.drawable.controls_shape)
            tvCoolerValue.text = "Off"
            tvCoolerValue.setTextColor(Color.parseColor("#342675"))
        } else {
            if(preferences.getBoolean("engine", false))
            {
                if(!preferences.getBoolean("doors", false)) {
                    Toast.makeText(
                        this@ControlsActivity,
                        "Close the door to make the air conditioner/fan work more efficiently!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if(currentTemperature < 18) {
                    clHeating.setBackgroundResource(R.drawable.controls_shape_heating_on)
                    tvCoolerValue.text = "Heat"
                    tvCoolerValue.setTextColor(Color.parseColor("#E3242B"))
                }
                if(currentTemperature > 24) {
                    clCooling.setBackgroundResource(R.drawable.controls_shape_cooling_on)
                    tvCoolerValue.text = "Cold"
                    tvCoolerValue.setTextColor(Color.parseColor("#00CCFF"))
                }
                if(currentPressure in 98.0 .. 103.5) {
                    clVentilation.setBackgroundResource(R.drawable.controls_shape)
                    tvFanValue.text = "Off"
                    tvFanValue.setTextColor(Color.parseColor("#342675"))
                } else {
                    clVentilation.setBackgroundResource(R.drawable.controls_shape_ventilation_on)
                    tvFanValue.text = "On"
                    tvFanValue.setTextColor(Color.parseColor("#90EE90"))
                }
                edit.putBoolean("climate", true)
                edit.commit()
            }
        }
    }

    fun connect(context: Context) {
        val serverURI = "mqtt://broker.emqx.io:1883"
        mqttClient = MqttAndroidClient(context, serverURI, "mqtt-android")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("AndroidMqtt", "Receive message: ${message.toString()} from topic: $topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d("AndroidMqtt", "Connection lost ${cause.toString()}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
//        options.userName = "android"
//        options.password = "mqtt-android".toCharArray()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("AndroidMqtt", "Connection success")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("AndroidMqtt", "Connection failure")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }

    fun subscribe(topic: String) {
        val qos = 0 // Mention your qos value
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on Subscription here
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give your subscription failure callback here
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
        }
    }

    fun receiveMessages() {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                //connectionStatus = false
                // Give your callback on failure here
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    val data = String(message.payload, charset("UTF-8"))
                    // data is the desired received message
                    // Give your callback on message received here
                } catch (e: Exception) {
                    // Give your callback on error here
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }

    fun disconnect() {
        try {
            val disconnectToken = mqttClient.disconnect()
            disconnectToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    //connectionStatus = false
                    // Give Callback on disconnection here
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give Callback on error here
                }
            }
        } catch (e: MqttException) {
            // Give Callback on error here
        }
    }
}