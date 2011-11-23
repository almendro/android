package com.appMobi.appMobiLib;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

public abstract class C2DMReceiver extends C2DMBaseReceiver {
    //static final String TAG = Config.makeLogTag(MainReceiver.class);
    //public static final String C2DM_SENDER = "ac2dm.appMobi@gmail.com";
    public static final String C2DM_ACCOUNT_EXTRA = "account_name";
    public static final String C2DM_MESSAGE_EXTRA = "message";
    public static final String C2DM_DATA_EXTRA = "data";
    public static final String C2DM_BADGE_EXTRA = "badge";
    public static final String C2DM_USERKEY_EXTRA = "userkey";
    static final int PUSH_NOTIFICATION_ID = 1;//notification id
	static final String FROM_NOTIFICATION = "fromTray";
	
	//hack to differentiate between user initiated and system initiated registration
	private static boolean bShouldFireJSEventWithUpdateToken = true;
	
    
    public C2DMReceiver() {
        super();
    }
    
    @Override
    public void onError(Context context, String errorId) {
//    	System.out.println(errorId);
//        Toast.makeText(context, "Messaging registration error: " + errorId,
//                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        String accountName = intent.getExtras().getString(C2DM_ACCOUNT_EXTRA);
        String message = intent.getExtras().getString(C2DM_MESSAGE_EXTRA);
        String data = intent.getExtras().getString(C2DM_DATA_EXTRA);
        String badge = intent.getExtras().getString(C2DM_BADGE_EXTRA);
        final String userKey = intent.getExtras().getString(C2DM_USERKEY_EXTRA);
        try {
        	//System.out.println("*******received push notification: " + accountName + "," + message + "," + data + ", " + badge + "********");
        	C2DMReceiver.showTrayNotification(context, message, data, badge, userKey);
			// get notifications on the server.
			new Thread("C2DMReceiver:onMessage") {
				public void run() {
					//check if the app is running - if it is tell it to update mesages and log an event
					if(AppMobiActivity.sharedActivity!=null && AppMobiActivity.sharedActivity.appView!=null && AppMobiActivity.sharedActivity.appView.notification!=null) {
						AppMobiActivity.sharedActivity.appView.notification.updatePushNotifications();
						AppMobiActivity.sharedActivity.appView.autoLogEvent("/notification/push/interact.event", userKey);
					}
				}
			}.start();
        	
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }

    @Override
	public void onRegistered(Context context, String registrationId)
			throws IOException {
		super.onRegistered(context, registrationId);
		AppMobiActivity.sharedActivity.appView.notification.registerDevice(registrationId, bShouldFireJSEventWithUpdateToken);
		//always reset to default
		bShouldFireJSEventWithUpdateToken = true;
	}
    
    public static void refreshAppC2DMRegistrationState(Context context, boolean bShouldFireJSEventWithUpdateToken) {
    	C2DMReceiver.bShouldFireJSEventWithUpdateToken = bShouldFireJSEventWithUpdateToken;
    	
    	C2DMessaging.register(context, context.getResources().getString(R.string.c2dm_sender));
	    
	    //currently we just register, never unregister
//      C2DMessaging.unregister(context);
    }
    
    protected static void showTrayNotification(Context context, String message, String data, String badgeNumber, String userKey) {    	
    	//get a reference to the service
    	String ns = Context.NOTIFICATION_SERVICE;
    	final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);    
    	//parse the message for test mode
    	String testModeApp = null;
    	if(message.indexOf('[')==0) {
    		testModeApp = message.substring(1, message.indexOf(']'));
    		message = message.substring(message.indexOf(']')+1).trim();
    	}
    	if(testModeApp==null && AppMobiActivity.sharedActivity!=null) {
    		//check if we are running in TestAnywhere mode
    		if(AppMobiActivity.sharedActivity.configData!=null && context.getPackageName().endsWith(AppMobiActivity.sharedActivity.configData.appName)) {
    			testModeApp = AppMobiActivity.sharedActivity.configData.appName;
    		}
    	}
    	//create the notification instance
    	int icon = R.drawable.icon;
    	CharSequence tickerText = message;
    	long when = System.currentTimeMillis();
    	final Notification notification = new Notification(icon, tickerText, when);
    	//initialize latest event info
    	CharSequence contentTitle = context.getString(R.string.app_name) + ": " + badgeNumber + " unread messages.";
    	CharSequence contentText = message;
    	Intent notificationIntent = null;
//    	if(MainActivity.sharedActivity!=null) { 
//    		notificationIntent = new Intent(MainActivity.sharedActivity, MainActivity.class);
//    	} else {
			try {
				//notificationIntent = new Intent(context, getClassForPendingIntent(context));
				notificationIntent = new Intent(context, Class.forName(context.getPackageName() + ".MainActivity"));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				if(Debug.isDebuggerConnected()) Log.e("[appMobi]", "getClassForPendingIntent returned null, unable to show tray notification");
			}
			notificationIntent.setAction(Intent.ACTION_MAIN);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//    	}
    	//add extra so we can know we were started from tray - set value to test mode app, if any
    	notificationIntent.putExtra(FROM_NOTIFICATION, testModeApp!=null?testModeApp:"");
    	notificationIntent.putExtra(C2DM_USERKEY_EXTRA, userKey);

    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
    	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	//make notification non-cancellable
    	notification.flags = notification.flags|Notification.FLAG_NO_CLEAR; 
    	//show in status bar
    	mNotificationManager.notify(PUSH_NOTIFICATION_ID, notification);
    	//this would remove notification from status bar
    	//mNotificationManager.cancel(BUSY_INDICATOR);
    }
    
    //abstract protected Class<?> getClassForPendingIntent(Context context) throws ClassNotFoundException;

	public static void removeTrayNotification(Context context) {    	
		//check if launched from notification tray - push notification
		//boolean wasStartedFromTray = intent.hasExtra(MainReceiver.FROM_NOTIFICATION);
		//System.out.println("was started from notification tray: " + wasStartedFromTray);
		String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		mNotificationManager.cancel(C2DMReceiver.PUSH_NOTIFICATION_ID);
    }    
}
