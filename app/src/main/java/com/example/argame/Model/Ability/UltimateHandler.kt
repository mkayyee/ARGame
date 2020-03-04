package com.example.argame.Model.Ability

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.sql.Time
import kotlin.math.abs

const val ACCEL_GOAL = 7f
const val ILLUM_GOAL = 50f

class UltimateHandler(
    private val accel: Sensor,
    private val illum: Sensor,
    private val mSensorManager: SensorManager,
    private val observer: UltimateHandlerListener) : SensorEventListener {

    private var measuring = false
    private var xHighest = 0f
    private var yHighest = 0f
    private var zHighest = 0f
    private var illumSmallest = 100f

    interface UltimateHandlerListener {
        fun onMeasured(succeeded: Boolean, ability: PlayerUltimate)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor
        if (measuring) {
            if (sensor == accel) {
                val values = event.values
                val x = abs(values[0])
                val y = abs(values[1])
                val z = abs(values[2])
                updateHighestAccel(x, y, z)
            } else if (sensor == illum) {
                val values = event.values
                Log.d("SENSOR", "Illumation value: ${abs(values[0])}")
                updateHighestIllum(abs(values[0]))
            }
        }
    }

    fun beginMeasuring(ability: PlayerUltimate) {
        measuring = true
        registerSensorListener(ability)
        Log.d("SENSOR", "Measuring began at: ${Time(System.currentTimeMillis())}")
    }

    private fun updateHighestAccel(x: Float, y: Float, z: Float) {
        if (x >= ACCEL_GOAL || y >= ACCEL_GOAL || z >= ACCEL_GOAL) {
            observer.onMeasured(true, PlayerUltimate.KILLALL)
            clearAccelData()
            Log.d("SENSOR", "Measuring stopped at: ${Time(System.currentTimeMillis())}")
        } else {
            if (x > xHighest) xHighest = x
            if (y > yHighest) yHighest = y
            if (z > zHighest) zHighest = z
            observer.onMeasured(false, PlayerUltimate.KILLALL)
        }
    }

    private fun updateHighestIllum(value: Float) {
        clearIllumData()
        if (value <= ILLUM_GOAL) {
            observer.onMeasured(true, PlayerUltimate.SERENITY)
            Log.d("SENSOR", "Measuring stopped at: ${Time(System.currentTimeMillis())}")
        } else {
            if (value < illumSmallest) illumSmallest = value
            observer.onMeasured(false, PlayerUltimate.SERENITY)
        }
    }

    private fun clearAccelData() {
        xHighest = 0f
        yHighest = 0f
        zHighest = 0f
        measuring = false
        unregisterSensorListener()
    }

    private fun clearIllumData() {
        illumSmallest = 100f
        measuring = false
        unregisterSensorListener()
    }

    private fun registerSensorListener(ability: PlayerUltimate) {
        if (ability == PlayerUltimate.KILLALL) {
            mSensorManager.registerListener(
                this,
                accel,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            )
        } else {
            mSensorManager.registerListener(
                this,
                illum,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            )
        }
    }

    private fun unregisterSensorListener() {
        mSensorManager.unregisterListener(this)
    }
}