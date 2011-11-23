/*
 */
package com.google.android.c2dm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

/**
 * Helper class to handle BroadcastReciver behavior.
 * - can only run for a limited amount of time - it must start a real service 
 * for longer activity
 * - must get the power lock, must make sure it's released when all done.
 * 
 */
public class C2DMBroadcastReceiver extends BroadcastReceiver {
    
    @Override
    public final void onReceive(Context context, Intent intent) {
        String registration = intent.getStringExtra("registration_id"); 
        if(registration!=null) {
        	if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "onReceive intent registration" + registration);
        }
        if (intent.getStringExtra("error") != null) {
            if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "onReceive intent error" + intent.getStringExtra("error"));
        } else if (intent.getStringExtra("unregistered") != null) {
            // unregistration done, new messages from the authorized sender will be rejected
        } else if (registration != null) {
           // Send the registration ID to the 3rd party site that is sending the messages.
           // This should be done in a separate thread.
           // When done, remember that all registration is done. 
        }
        
        // To keep things in one place.
        C2DMBaseReceiver.runIntentInService(context, intent);
        setResult(Activity.RESULT_OK, null /* data */, null /* extra */);        
    }
}
