package com.github.ebrooks2002.buoyfinder.location
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RotationSensor (private val context: Context) {

    private val client = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getRotationUpdates(): Flow<Float> = callbackFlow {
        // create sensor objects
        val accelerometer = client.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = client.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // check whether device actually has these sensors.
        if (accelerometer == null || magnetometer == null) {
            println("lacking proper sensors") //debugging purposes
            close()
            return@callbackFlow
        }

        // create arrays to hold rotation data
        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)

        // make sure arrays are ready for conversion
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val sensorListener = object: SensorEventListener {
            val alpha = 0.99f
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // don't need to implement
            }
            // implement onSensorChanged function
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Apply Low-Pass Filter to Accelerometer
                    for (i in 0..2) {
                        accelerometerReading[i] = alpha * accelerometerReading[i] + (1 - alpha) * event.values[i]
                    }
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    // Apply Low-Pass Filter to Magnetometer
                    for (i in 0..2) {
                        magnetometerReading[i] = alpha * magnetometerReading[i] + (1 - alpha) * event.values[i]
                    }
                }

                val success = SensorManager.getRotationMatrix(
                    rotationMatrix, null, accelerometerReading,
                    magnetometerReading)

                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthInRadians = orientationAngles[0]
                    val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                    val heading = (azimuthInDegrees + 360) % 360
                    trySend(heading)
                }
            }
        }
        client.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        client.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            client.unregisterListener(sensorListener)
        }

    }

}

