package com.example.ubicompapplication

open class Constants {

    companion object {
        const val PREFERENCE_SMART_CAR = "smart car preference"
        const val CHANNEL_ID = "7"
        const val notificationId = 1
        const val NOTIFICATION_CODE = 100
        const val TOPIC = "ubicomp/#"
        const val CLIENT_ID = "mqtt_project2"
        const val SERVER_URI = "tcp://192.168.1.104:1884"
        const val TEMPERATURE_HIGH = 35.2
        const val TEMPERATURE_LOW = 27.5
        const val AIRWAVE = 20
        const val PRESSURE_HIGH = 103.5
        const val PRESSURE_LOW = 98.8
        const val TEMP_STREAM = "temp"
        const val PRESSURE_STREAM = "press"
        const val LOW_TEMP_ALARM = "alarmLowTemp"
        const val HIGH_TEMP_ALARM = "alarmHighTemp"
        const val PRESS_LIMITS_ALARM = "alarmPress"
        const val ACC_GYRO_STREAM = "motion"
        const val COLOR_STREAM = "alarmLight"
        const val GESTURE_STREAM = "gesture"
        const val LPS_STREAM = "lps"
        const val PROXIMITY_STREAM = "alarmProximity"
        const val ROAD_CURVE_LIMIT = 0.5
        const val ACC_CURVE_LIMIT = 2.1
    }

}