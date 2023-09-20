package com.example.ubicompapplication

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorSpace.connect
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.eclipse.paho.android.service.MqttAndroidClient


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
    private lateinit var mqttClient: MqttConnection

    @RequiresApi(Build.VERSION_CODES.O)
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

        mqttClient = MqttConnection(applicationContext)
        mqttClient.connect()
//        mqttClient.subscribe("android/test")
//        mqttClient.receiveMessages()

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
}