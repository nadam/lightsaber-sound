/*
 * Copyright (C) Adam Nybäck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.anyro.lss;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;

public class MainActivity extends Activity implements OnLoadCompleteListener {

    private static final int PRIORITY_HIGH = 2;
    private static final int PRIORITY_LOW = 1;
    private static final int PLAY_ONCE = 0;
    private static final int LOOP = -1;
    private static final float RATE_NORMAL = 1f;

    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mProximitySensor;

    private SoundPool mSoundPool;
    private int mSoundOn;
    private int mSoundHit;
    private int mSoundHum = 0;
    private int mDarkHumId = 0;
    private int mLightHumId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(this);
        mSoundHum = mSoundPool.load(this, R.raw.saberftn2, 1);
        mSoundHit = mSoundPool.load(this, R.raw.lswall01, 1);
        mSoundOn = mSoundPool.load(this, R.raw.lightsaber_ignites, 1);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    private SensorEventListener mGyroListener = new SensorEventListener() {

        private boolean mHitStarted = false;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            // Rotational speed
            float[] values = event.values;
            float x = values[0];
            float z = values[2];

            float zStrength = z * z;
            if (zStrength > 150f && !mHitStarted) {
                mSoundPool.play(mSoundHit, 0.5f, 0.5f, PRIORITY_LOW, PLAY_ONCE, RATE_NORMAL);
                mHitStarted = true;
            } else {
                mHitStarted = false;
            }

            float strength = (zStrength + x * x) / 145f;
            float lightVolume = Math.min(strength + 0.1f, 1f); // 0.1 to 1.0
            float darkVolume = Math.max(0.4f - strength, 0.1f); // 0.4 to 0.1
            float rate = Math.min(strength / 2f + 1f, 1.2f); // 1.0 to 1.2

            if (mLightHumId != 0) {
                mSoundPool.setVolume(mLightHumId, lightVolume, lightVolume);
                mSoundPool.setRate(mDarkHumId, rate);
                mSoundPool.setVolume(mDarkHumId, darkVolume, darkVolume);
            }
        }
    };

    private SensorEventListener mProximityListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];

            if (mLightHumId != 0 && distance < 1f) {
                mSoundPool.play(mSoundHit, 0.5f, 0.5f, PRIORITY_LOW, PLAY_ONCE, RATE_NORMAL);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mGyroListener, mGyroSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mProximityListener, mProximitySensor,
                SensorManager.SENSOR_DELAY_UI);
        if (mDarkHumId == 0) {
            startSound();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mGyroListener, mGyroSensor);
        mSensorManager.unregisterListener(mProximityListener, mProximitySensor);
        stopSound();
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        if (mDarkHumId == 0 && sampleId == mSoundOn) {
            startSound();
        }
    }

    private void startSound() {
        mSoundPool.play(mSoundOn, 0.5f, 0.5f, PRIORITY_HIGH, PLAY_ONCE, RATE_NORMAL);
        mDarkHumId = mSoundPool.play(mSoundHum, 0.4f, 0.4f, PRIORITY_HIGH, LOOP, RATE_NORMAL);
        mLightHumId = mSoundPool.play(mSoundHum, 0.1f, 0.1f, PRIORITY_HIGH, LOOP, 1.2f);
    }

    private void stopSound() {
        mSoundPool.stop(mDarkHumId);
        mSoundPool.stop(mLightHumId);
        mDarkHumId = 0;
        mLightHumId = 0;
    }
}