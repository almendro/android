package com.appMobi.applab;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;

import com.appMobi.appMobiLib.AppConfigData;
import com.appMobi.appMobiLib.R;

public class MainActivity extends com.appMobi.appMobiLib.AppMobiActivity
{
	public void onCreate(Bundle savedInstanceState)
	{
		isTestContainer = false;
        super.onCreate(savedInstanceState);
	}

	//do not check in
    @Override
    protected void onStop() {
        super.onStop();        

        //so activity wont stop if child tasks are active
        if( appView!=null && appView.player != null && appView.player.isPlayingPodcast() ) return;
        if(isLaunchedChildActivity()) return;
        
        //should only be true for test container
		boolean shouldExitOnStop = Boolean.parseBoolean(getResources().getString(R.string.shouldExitOnStop));
        if(shouldExitOnStop) {
            try {
                if(flyCastPlayer!=null)flyCastPlayer.ShutDownAppMobi();
            } catch(Exception e){
                e.printStackTrace();
            }
            finish();
            System.exit(0);
        }
    }

	@Override
	protected boolean extractBundledApp() {
		return super.extractBundledApp();
	}
    
	@Override
	protected boolean getJS(AppConfigData config, boolean useBundledJS)
			throws FileNotFoundException, IOException {
		boolean shouldUseBundleJS = Boolean.parseBoolean(getResources().getString(R.string.useBundledJS));
		return super.getJS(config, shouldUseBundleJS);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			Class<?> serviceClass = null;
			serviceClass = Class.forName(getString(R.string.remote_service_action));
			stopService(new Intent(this, serviceClass));
		} catch( Exception e ) { }
	}	

}