package com.example.mobilevideoplayer;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

public class SensorService extends Service {

    private float gravityZ = 0;
    private float proximity = 0;

    private SensorManager sensorManager;
    private Intent broadcastIntent = new Intent();

    //listeners
    private AccelerometerSensorListener accelerometerListener = new AccelerometerSensorListener();
    private ProximitySensorListener proximityListener = new ProximitySensorListener();

    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {

        broadcastIntent.setAction("GET_PROXIMITY_GRAVITY_ACTION");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Register sensors, if they exist
        if (sensorManager != null) {
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null)
                sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor != null)
                sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Unregister the sensors to save battery
        super.onDestroy();
        sensorManager.unregisterListener(accelerometerListener);
        sensorManager.unregisterListener(proximityListener);
    }

    /**
     * Sends message notifying that the phone is facing down on a surface and video should pause.
     */
    private void notifyPause() {
        broadcastIntent.putExtra("PAUSE_VIDEO", true);
        sendBroadcast(broadcastIntent);
    }

    private class AccelerometerSensorListener implements SensorEventListener {

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            gravityZ = sensorEvent.values[2];

            // Check proximity sensor
            if (proximity < 0.5 && gravityZ < -9.5) notifyPause();
        }
    }

    private class ProximitySensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            proximity = event.values[0];

            // Check proximity
            if (gravityZ < -9.5 && proximity < 0.5) notifyPause();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
