package com.appMobi.appMobiLib;

import static android.hardware.SensorManager.DATA_X;
import static android.hardware.SensorManager.DATA_Y;
import static android.hardware.SensorManager.DATA_Z;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorListener;
import android.hardware.SensorManager;

@SuppressWarnings("deprecation")
public class AppMobiAccelerometer extends AppMobiCommand implements SensorListener{
	String mKey;
	int mTime = 10000;
	boolean started = false;
	
	private SensorManager sensorManager;
	
	private long lastUpdate = -1;
	
	AppMobiAccelerometer(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);	
		sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	protected void stopCommand() {
		stop();
	}
	
	public void start(int time)
	{
		mTime = time;
		if (!started)
		{
			sensorManager.registerListener(this, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
		}
	}
	
	public void stop()
	{
		if(started)
			sensorManager.unregisterListener(this);
	}
	
	public void onAccuracyChanged(int sensor, int accuracy) {
		// This should call the FAIL method
	}
	
	public void onSensorChanged(int sensor, float[] values) {
		if (sensor != SensorManager.SENSOR_ACCELEROMETER || values.length < 3)
		      return;
		long curTime = System.currentTimeMillis();

		if (lastUpdate == -1 || (curTime - lastUpdate) > mTime) {
			
			lastUpdate = curTime;
			
			//android reports 0-10 - make it 0-1 for consistency with iPhone
			float x = values[DATA_X]/10;
			float y = values[DATA_Y]/10;
			float z = values[DATA_Z]/10;
			
			//if in landscape, android swaps x and y axes - swap them back for consistency with iPhone
			if(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				float oldx = x, oldy = y;
				y = oldx * -1;
				x = oldy;
			}
			
			super.injectJS("javascript:AppMobi._accel=new AppMobi.Acceleration("+x+","+y+","+z+",false);");
		}
	}	
}
