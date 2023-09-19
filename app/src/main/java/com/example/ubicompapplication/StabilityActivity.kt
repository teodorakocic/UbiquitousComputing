package com.example.ubicompapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.ubicompapplication.Constants.Companion.NOTIFICATION_CODE
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.w3c.dom.Text
import kotlin.math.sqrt

class StabilityActivity : AppCompatActivity() {

    private lateinit var tvAccelerationValue: TextView
    private lateinit var tvGyroscopeValue: TextView
    private lateinit var tvRoadPrediction: TextView
    private lateinit var tvESC: TextView
    private lateinit var progressRoadCondition: ProgressBar
    private lateinit var progressESC: ProgressBar
    private lateinit var bottomMenu: BottomNavigationView
    private var acceleration = 2.0
    private var gyro = 0.2

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stability)

        tvAccelerationValue = findViewById(R.id.tv_acceleration_value)
        tvGyroscopeValue = findViewById(R.id.tv_gyroscope_value)
        tvRoadPrediction = findViewById(R.id.tv_predict_road_value)
        tvESC = findViewById(R.id.tv_esc_value)
        progressRoadCondition = findViewById(R.id.progress_road_prediction)
        progressESC = findViewById(R.id.progress_esc)
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

        val preferences = this.getSharedPreferences(Constants.PREFERENCE_SMART_CAR, Context.MODE_PRIVATE)

//        acceleration = calculateAcceleration()
//        gyro = calculateGyro()

        tvAccelerationValue.text = acceleration.toString()
        tvGyroscopeValue.text = gyro.toString()

        if(preferences.getBoolean("engine", false)) {
            if(!preferences.getBoolean("trunk", false)) {
                Toast.makeText(this@StabilityActivity, "Close the trunk in order to enable stability system!", Toast.LENGTH_SHORT).show()
            } else {
                startForegroundService()
                checkPermissions(Manifest.permission.POST_NOTIFICATIONS, NOTIFICATION_CODE)
                if (acceleration > 1.5 && gyro < 0.5) {
                    tvRoadPrediction.text = "activated"
                    progressRoadCondition.visibility = View.VISIBLE
                    tvESC.text = "activated"
                    progressESC.visibility = View.VISIBLE
                } else {
                    tvRoadPrediction.text = "deactivated"
                    progressRoadCondition.visibility = View.INVISIBLE
                    tvESC.text = "deactivated"
                    progressESC.visibility = View.INVISIBLE
                }
            }
        } else {
            Toast.makeText(this@StabilityActivity, "Start the vehicle so that the stability system can be turned on!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAcceleration(aX: Double, aY: Double, aZ: Double): Double {
        return sqrt(aX * aX + aY * aY + aZ * aZ)
    }

    private fun calculateGyro(gX: Double, gY: Double, gZ: Double): Double {
        return sqrt(gX * gX + gY * gY + gZ * gZ)
    }

    override fun onDestroy() {
        super.onDestroy()
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
}