package com.appMobi.appMobiLib;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.appMobi.appMobiLib.AppMobiActivity.AppInfo;
import com.appMobi.appMobiLib.util.Base64;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import com.appMobi.appMobiLib.util.Debug;

import android.os.Build;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsoluteLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AppMobiNotification extends AppMobiCommand implements AppMobiActivity.ConfigurationChangeListener {
	private static final int BUSY_INDICATOR = 1;//notification id
    private volatile Thread spinner = null;//for show/hideBusyIndicator

	private static final int SOUND_BEEP = 1;
	
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundPoolMap;
    
    //push notifications support
    public String strPushUser = null;
	String strPushPass = null, strPushEmail;
    private static final String staticPushPrefs = "AM_PUSH_PREFS";
    private String pushPrefs;
    private HashMap<String, AMSNotification> pushUserNotifications;
    
    //rich media push support
	private AbsoluteLayout rmpLayout;
	private ImageButton rmpClose;
	private AppMobiWebView rmpView;
	private boolean isShowingRMP = false;
	private String currentlyShownRMPId = "";
	private TextView rmpPreviewText;
	private ProgressBar rmpPreviewSpinner;
	private int rmpCloseXPort=0, rmpCloseYPort=0, rmpCloseXLand=0, rmpCloseYLand=0, rmpCloseH=0, rmpCloseW=0;
	private int minPreviewDisplayLength = 2000, maxPreviewDisplayLength = 25000;

	DisplayMetrics metrics = new DisplayMetrics();
	
	private boolean isPreFroyo =  Integer.parseInt(Build.VERSION.SDK) < 8 /*Build.VERSION_CODES.FROYO*/;
	
	public AppMobiNotification(final AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
		
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		activity.addConfigurationChangeListener(this);
		
        initSounds();
        
        pushUserNotifications = new HashMap<String, AMSNotification>();
        
        pushPrefs = String.format("%s.%s", webview.config.appName, staticPushPrefs);
    	
		SharedPreferences prefs = activity.getSharedPreferences(pushPrefs, 0);
    	strPushUser = prefs.getString("pushUser", "");
    	strPushPass = prefs.getString("pushPass", "");
    	strPushEmail = prefs.getString("pushEmail", "");
    	
    	//rich media push support
		//remote site support
		rmpLayout = new AbsoluteLayout(activity);
		rmpLayout.setBackgroundColor(Color.BLACK);
		//hide the remote site display until needed
		rmpLayout.setVisibility(View.GONE);
		
		//create the preview text box
		rmpPreviewText = new TextView(activity);
		rmpPreviewText.setGravity(Gravity.CENTER);
		rmpLayout.addView(rmpPreviewText);
		
		//create the preview spinner
		rmpPreviewSpinner = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
		rmpPreviewSpinner.setIndeterminate(true);
		rmpLayout.addView(rmpPreviewSpinner);
		
		//create the close button
		rmpClose = new ImageButton(activity);
//		Drawable remoteCloseImage = null;
//		//set the button image
//		File remoteCloseImageFile = new File(activity.appDir(), "_appMobi/rich_close.png");
//		if(remoteCloseImageFile.exists()) {
//			remoteCloseImage = (Drawable.createFromPath(remoteCloseImageFile.getAbsolutePath()));
//		}
//		else {
//			remoteCloseImage = ( activity.getResources().getDrawable(R.drawable.rich_close));
//		}
//		rmpClose.setImageDrawable(remoteCloseImage);
		rmpClose.setBackgroundResource(0);
		//set up the button click action
		rmpClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				closeRichPushViewer();
			}
		});
		rmpLayout.addView(rmpClose);
		activity.runOnUiThread(new Runnable() {
			public void run() {
				//hack for mobius
				if(activity.root != null) {
					//add layout to activity root layout
					activity.root.addView(rmpLayout);
				}
			}
		});
		
    }
    
    private void initSounds() {
         soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
         soundPoolMap = new HashMap<Integer, Integer>();
         soundPoolMap.put(SOUND_BEEP, soundPool.load(activity, R.raw.beep, 1));
    }
    
    public void playSound(int sound, int loops) {
         AudioManager mgr = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
         int streamVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
         soundPool.play(soundPoolMap.get(sound), streamVolume, streamVolume, 1, loops, 1f);
    }
    
    public void alert(String message, String title, String button) {
        AlertDialog.Builder alertBldr = new AlertDialog.Builder(activity);
        alertBldr.setMessage(message);
        alertBldr.setTitle((title!=null&&title.length()>0)?title:"Alert");
        alertBldr.setPositiveButton((button!=null&&button.length()>0)?button:"OK", 
                        new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {} }
        );
        alertBldr.show();
    }
	
	public void vibrate(){
        Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);
	}

	public void beep(long count)
	{
		count = (count > 0 )?count-1 : 0;
		playSound(AppMobiNotification.SOUND_BEEP, (int)count);
	}
    
    public void showBusyIndicator() {    	
    	if( spinner != null ) return;
    	
    	//get a reference to the service
    	String ns = Context.NOTIFICATION_SERVICE;
    	final NotificationManager mNotificationManager = (NotificationManager) activity.getSystemService(ns);    
    	//create the notification instance
    	int icon = R.drawable.spinner_n;
    	CharSequence tickerText = activity.getString(R.string.app_name) + " is busy...";
    	long when = System.currentTimeMillis();
    	final Notification notification = new Notification(icon, tickerText, when);
    	//initialize latest event info
    	Context context = activity.getApplicationContext();
    	CharSequence contentTitle = activity.getString(R.string.app_name) + " is busy...";
    	CharSequence contentText = "...just a moment please.";
    	Intent notificationIntent = new Intent(activity, activity.getClass());
    	PendingIntent contentIntent = PendingIntent.getActivity(activity, 0, notificationIntent, 0);
    	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	//make notification non-cancellable
    	notification.flags = notification.flags|Notification.FLAG_NO_CLEAR; 
    	//show in status bar
    	mNotificationManager.notify(BUSY_INDICATOR, notification);
    	//animate the icon
    	spinner = new Thread("AppMobiNotification:showBusyIndicator") {
    		public void run() {
    			//frame pointer
    			int currentFrame = 0;
    			//frame array
    			int[] frames = new int[]{R.drawable.spinner_ne, R.drawable.spinner_e, R.drawable.spinner_se, R.drawable.spinner_s, R.drawable.spinner_sw, R.drawable.spinner_w, R.drawable.spinner_nw, R.drawable.spinner_n};
    			Thread thisThread = Thread.currentThread();
    			while(spinner == thisThread) {
    				//loop over the frames, updating the icon every 200 ms
    				currentFrame++;
    				currentFrame %= frames.length;
    				notification.icon = frames[currentFrame];
    				mNotificationManager.notify(BUSY_INDICATOR, notification);
    				try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
    			}
    			//when looping ends, remove notification from status bar
    	    	mNotificationManager.cancel(BUSY_INDICATOR);
    		}
			@Override
			protected void finalize() throws Throwable {
				//in case the process crashes, try to remove notification from status bar
				super.finalize();
    	    	mNotificationManager.cancel(BUSY_INDICATOR);
			}
    	};
    	spinner.start();
    }
    
    public void hideBusyIndicator() {
    	//setting spinner to null makes the worker thread stop looping
    	spinner = null;
    }
    
    public AMSResponse getPushServerResponse(String urlString) {
    	if(Debug.isDebuggerConnected()) Log.d("[appMobi]", urlString);
    	
    	AMSResponse data = null;
		
		try {
            /* Create a URL we want to load some xml-data from. */
            URL url = new URL(urlString);

            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            AMSResponseHandler handler = new AMSResponseHandler();
            xr.setContentHandler(handler);

            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(url.openStream()));
            /* Parsing has finished. */

            /* Our ExampleHandler now provides the parsed data to us. */
            data = handler.getParsedData();
            
       } catch (Exception e) {
            /* Display any Error to the GUI. */
            if(Debug.isDebuggerConnected()) Log.e("error", "AppConfig Parsing Error w/ url: " + urlString, e);
	   }
	       
       return data;    	
    }

    public void getUserNotifications()
    {
    	if( strPushUser.length()==0 ) return;

    	String urlString = webview.config.pushServer + "/?CMD=ampush.getmessagesforuser&user=" + strPushUser + "&passcode=" + strPushPass + "&device=" + activity.getDeviceID() + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);
    	AMSNotification latest = null;
    	if(response!=null && "ok".equals(response.result)) {
    		for( int i = 0; i < response.notifications.size(); i++ ){
    			AMSNotification newNote = response.notifications.get(i);
    			AMSNotification oldNote = pushUserNotifications.get(newNote.ident);
    			if(oldNote == null) {
    				if( newNote.richhtml.indexOf(' ')==-1 && newNote.richhtml.indexOf('<')==-1 ) {
    					newNote.richhtml = new String(Base64.decode(newNote.richhtml, Base64.DEFAULT));
    				}
    				newNote.richhtml = newNote.richhtml.replace("\"", "\\\"");
    				if( newNote.richurl.indexOf("http")==-1 ) {
    					newNote.richurl = new String(Base64.decode(newNote.richurl, Base64.DEFAULT));
    				}
    				newNote.richurl = newNote.richurl.replace("\"", "\\\"");
    				pushUserNotifications.put(String.valueOf(newNote.ident), newNote);
    			}
    			if(latest==null) latest = newNote;
    			else if(newNote.ident>latest.ident) latest = newNote;
    		}
    		if(pushUserNotifications.size()>0 && latest!=null) {
    			C2DMReceiver.showTrayNotification(activity, latest.message, latest.data, String.valueOf(pushUserNotifications.size()), latest.userkey);
    		} else {
    			C2DMReceiver.removeTrayNotification(activity);
    		}
    	}
    }
        
    public String getNotificationsString() {
    	if( strPushUser.length()==0 ) return "";
    	if( !webview.config.hasPushNotify ) return "";
    	
    	StringBuilder notes = new StringBuilder();
    	Set<String> keys = pushUserNotifications.keySet();
    	for(String key:keys){
    		AMSNotification note = pushUserNotifications.get(key);
    		notes.append("{");
    		notes.append("\"id\":");//id name
    		notes.append(note.ident);//id value
    		notes.append(", \"msg\":");//msg name
    		notes.append(String.format("\"%1$s\"", note.message));//msg value
    		notes.append(", \"data\":");//data name
    		notes.append(String.format("\"%1$s\"", note.data));//data value
    		notes.append(", \"userkey\":");//userkey name
    		notes.append(String.format("\"%1$s\"", note.userkey));//userkey value
    		notes.append(", \"richhtml\":");//richhtml name
    		notes.append(String.format("\"%1$s\"", note.richhtml));//richhtml value
    		notes.append(", \"richurl\":");//richurl name
    		notes.append(String.format("\"%1$s\"", note.richurl));//richurl value
    		notes.append(", \"target\":");//target name
    		notes.append(String.format("\"%1$s\"", note.target));//target value
    		notes.append(", \"url\":");//url name
    		notes.append(String.format("\"%1$s\"", note.url));//url value
    		notes.append(", \"isRich\":");//isRich name
    		notes.append(note.isRich);//isRich value
    		notes.append("}, ");
    	}
    	String js = "AppMobi.notifications = [" + notes.toString() + "];";	
    	if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    	return js;
    }
    
    public interface PushNotificationUpdateListener {
    	public void updateOccurred();
    }
    ArrayList<PushNotificationUpdateListener> pnuListeners = new ArrayList<PushNotificationUpdateListener>();
    public void addPushNotificationUpdateListener(PushNotificationUpdateListener listener) {
    	pnuListeners.add(listener);
    }
    
    public void updatePushNotifications() {
    	if( !webview.config.hasPushNotify ) return;
    	getUserNotifications();
    	
    	if(pnuListeners.size()>0) {
    		for(PushNotificationUpdateListener listener:pnuListeners) {
    			listener.updateOccurred();
    		}
    	}
    	
    	String js = getNotificationsString();
    	js += "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.receive',true,true);e.success=true;document.dispatchEvent(e);";
    	injectJS(js);
    }

    public void refreshPushNotifications() {
    	if( !webview.config.hasPushNotify ) return;
    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.refreshPushNotifications, No push user available.\");";
        	injectJS(js);
    		return;
    	}

    	getUserNotifications();

		String js = getNotificationsString();
    	js += "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.refresh',true,true);e.success=true;document.dispatchEvent(e);";
    	injectJS(js);
    }

    public void registerDevice(String token, boolean shouldFire) {
    	if( !webview.config.hasPushNotify ) return;
    	String js = null;
    	if(token!=null) {

    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]:", "token: " + token);

    		boolean wasDeviceRegistered = activity.getSharedPreferences(pushPrefs, 0).contains("pushRegistered");
    		String ampushCommand = null;
    		//check if user existed previously or if we just added them
    		if(!wasDeviceRegistered) {
    			// add the device
    	    	//persist and update local references
    			SharedPreferences.Editor editor = activity.getSharedPreferences(pushPrefs, 0).edit();
    			editor.putString("pushUser", strPushUser);
    			editor.putString("pushPass", strPushPass);
    			editor.putString("pushEmail", strPushEmail);
    			editor.commit();

    			ampushCommand = "adddevice";
    			
    		} else {
    			ampushCommand = "adddevice";
    		}
    		
    		String strModel = android.os.Build.MODEL;
    		String deviceKey = AppMobiAnalytics.getDeviceKey();
    		String urlString = webview.config.pushServer + "/?CMD=ampush." + ampushCommand + "&user=" + strPushUser + "&passcode=" + strPushPass + "&deviceid=" + activity.getDeviceID() + "&devicekey=" + deviceKey + "&email=&token=" + token + "&type=android&model=" + strModel  + "&appname=" + webview.config.appName;
    		AMSResponse response = getPushServerResponse(urlString);
    		
    		if(response!=null) {
    			if("ok".equals(response.result)) {
    				//device is registered
    				js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=true;document.dispatchEvent(e);";
    				//save in prefs
    				SharedPreferences.Editor editor = activity.getSharedPreferences(pushPrefs, 0).edit();
    				editor.putBoolean("pushRegistered", Boolean.TRUE);
    				// get notifications on the server.
    				new Thread("AppMobiNotification:registerDevice") {
    					public void run() {
    						updatePushNotifications();
    					}
    				}.start();
    			} else {
    				//device registration failed
    				js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='" + response.message + "';document.dispatchEvent(e);";
    			}
    		} else {
    			//an error occurred
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    		}
    	} else {
    		//an error occurred
    		// if token is nil, then it should fire error fn or event.
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    	}

    	//update js object and fire an event
    	if (shouldFire) {
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    	}
    }

    public void checkPushUser(String userID, String password) {
    	if( !webview.config.hasPushNotify ) return;
		// verify against the server using this user
    	String urlString = webview.config.pushServer + "/?CMD=ampush.checkuser&user=" + userID + "&passcode=" + password + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);
    	
    	String js = null;
		//valid credentials, enable user and fire success event with email
    	if(response!=null && "ok".equals(response.result)) {
    		//update local references
    		strPushUser = userID;
			strPushPass = password;
			strPushEmail = response.email;

			//pre-froyo hack
    		if(isPreFroyo) {
    			registerDevice("PREFROYO", true);
    		} else {
    			C2DMReceiver.refreshAppC2DMRegistrationState(activity, true);
    		}

    	} else {
    		if(response!=null) {
    			if("user does not exist".equals(response.message)) {
					//user does not exist
					js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='user does not exist';document.dispatchEvent(e);";
    			} else {
    				//user exists, wrong password
    				js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='incorrect password';document.dispatchEvent(e);";
    			}
    		} else {
    			//an error occurred
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    		}
    		
    		//update js object and fire an event
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    	}
    }

    public void addPushUser(String userID, String password, String email) {
    	if( !webview.config.hasPushNotify ) return;
    	//params validated in js

    	// try to create/add this user
    	String urlString = webview.config.pushServer + "/?CMD=ampush.adduser&user=" + userID + "&email=" + email + "&passcode=" + password + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);
    	
    	String js = null;
    	if (response!=null && "ok".equals(response.result)) {
    		//valid credentials, enable user and fire success event with email

    		//update local references
			strPushUser = userID;
			strPushPass = password;
			strPushEmail = email;
    		
    		//pre-froyo hack
    		if(isPreFroyo) {
    			registerDevice("PREFROYO", true);
    		} else {
    			C2DMReceiver.refreshAppC2DMRegistrationState(activity, true);
    		}
    		
    	} else {
    		if(response==null) {
    			//an error occurred
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    		} else if ("invalid passcode".equals(response.message)) {
    			//user exists, wrong password
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='invalid passcode';document.dispatchEvent(e);";
    			//dont fire success until end when device is registered
    		} else {
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.enable',true,true);e.success=false;e.message='error adding user record to database';document.dispatchEvent(e);";
    		}
    		//update js object and fire an event
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    	}
    	
    }

	public void editPushUser(String email, String newPassword) {
    	if( !webview.config.hasPushNotify ) return;
    	//input params validated in js

    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.editPushUser, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}
    	
    	// try to create/add this user
    	String urlString = webview.config.pushServer + "/?CMD=ampush.edituser&user=" + strPushUser + "&appname=" + webview.config.appName;
    	if (email!=null && email.length()>0) {
    		urlString += "&email=" + email;
    	}
    	urlString += "&passcode=" + strPushPass;
    	if (newPassword!=null && newPassword.length()>0) {
    		urlString += "&newpasscode=" + newPassword;
    	}
    	
    	AMSResponse response = getPushServerResponse(urlString);
    	
    	String js = null;
    	
    	if(response!=null) {
    		if ("ok".equals(response.result)) {
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.edit',true,true);e.success=true;document.dispatchEvent(e);";
    			
    			//update defaults
    			SharedPreferences.Editor editor = activity.getSharedPreferences(pushPrefs, 0).edit();
    			if (email!=null && email.length()>0) {
    				strPushEmail = email;
    				editor.putString("pushEmail", email);
    			}
    			if (newPassword!=null && newPassword.length()>0) {
    				strPushPass = newPassword;
    				editor.putString("pushPass", newPassword);
    			}
    			editor.commit(); 
    			
    		} else {
    			if ("invalid passcode".equals(response.result)) {
    				//user exists, wrong password
    				js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.edit',true,true);e.success=false;e.message='invalid passcode';document.dispatchEvent(e);";
    			} else {
    				//an unknown error occurred
    				js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.edit',true,true);e.success=false;e.message='error adding user record to database';document.dispatchEvent(e);";
    			}
    		}
    	} else {
    		//an error occurred
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.edit',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    	}

    	
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }

    public void deletePushUser() {
    	if( !webview.config.hasPushNotify ) return;
    	//make sure user is logged in
    	if("".equals(strPushUser) ) {
    		String js = "throw(\"Error: AppMobi.notification.deletePushUser, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}
    	
    	// request user be deleted
		String urlString = webview.config.pushServer + "/?CMD=ampush.deletedevice&user=" + strPushUser + "&passcode=" + strPushPass + "&deviceid=" + activity.getDeviceID() + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);

    	String js = null;
    	if(response!=null && "ok".equals(response.result)) {
			//update defaults
			SharedPreferences.Editor editor = activity.getSharedPreferences(pushPrefs, 0).edit();
			strPushUser = "";
			strPushPass = "";
			strPushEmail = "";
			editor.remove("pushUser");
			editor.remove("pushPass");
			editor.remove("pushEmail");
			editor.commit(); 

			//user was deleted
			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.disable',true,true);e.success=true;document.dispatchEvent(e);";
    	} else {
    		//an error occurred
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.disable',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    	}		
    	
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }

	
    public void sendPushUserPass() {
    	if( !webview.config.hasPushNotify ) return;
    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.sendPushUserPass, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}
    	
    	// request password be sent to email
    	String urlString = webview.config.pushServer + "/?CMD=ampush.getpasscode&user=" + strPushUser + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);

    	String js = null;
    	if(response!=null) {
    		if ("ok".equals(response.result)) {
    			//password was sent
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.sendpassword',true,true);e.success=true;document.dispatchEvent(e);";
    		} else {
    			//user does not exist
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.sendpassword',true,true);e.success=false;e.message='user does not exist';document.dispatchEvent(e);";
    		}
    	} else {
    		//an error occurred
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.sendpassword',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    	}		
    	
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }

    //this is not yet tested
    public void setPushUserAttributes(String attributes) {
    	if( !webview.config.hasPushNotify ) return;
    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.setPushUserAttributes, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}
    	
    	attributes.replace(' ', '+');
    	String urlString = webview.config.pushServer + "/?CMD=ampush.setuserattributes&user=" + strPushUser + "&passcode=" + strPushPass + "&appname=" + webview.config.appName + "&attributes=" + attributes;
    	AMSResponse response = getPushServerResponse(urlString);
    	
    	String js = null;
    	if(response!=null) {
    		if ("ok".equals(response.result)) {
    			//password was sent
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.editattributes',true,true);e.success=true;document.dispatchEvent(e);";
    		} else {
    			//user does not exist
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.editattributes',true,true);e.success=false;e.message='" + response.message + "';document.dispatchEvent(e);";
    		}
    	} else {
    		//an error occurred
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.editattributes',true,true);e.success=false;e.message='An unexpected error occurred';document.dispatchEvent(e);";
    	}
    	
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }

    public void findPushUser(String userID, String email) {
    	if( !webview.config.hasPushNotify ) return;
//    	//input params validated in js
    	String urlString = "";
    	if( userID.length()==0 && email.length()== 0 )
    		urlString = webview.config.pushServer+ "/?CMD=ampush.finduser" + "&appname=" + webview.config.appName;
    	else if( userID.length()!=0 )
    		urlString = webview.config.pushServer+ "/?CMD=ampush.finduser&user=" + userID + "&appname=" + webview.config.appName;
    	else if( email.length()!=0 )
    		urlString = webview.config.pushServer+ "/?CMD=ampush.finduser&email=" + email + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);
    	
    	String js = null;
    	if(response!=null) {
    		if ("ok".equals(response.result)) {
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.find',true,true);e.success=true;e.userid='" + response.user + "';document.dispatchEvent(e);";
    		} else {
    			js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.find',true,true);e.success=false;e.message='unable to find a user';document.dispatchEvent(e);";
    		}
    	} else {
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.user.find',true,true);e.success=false;e.message='an unexpected error occurred';document.dispatchEvent(e);";
    	}

		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }
    
    public void readPushNotifications(String notificationIDs) {
    	readPushNotifications(notificationIDs, true);
    }
    public void readAllPushNotifications() {
    	StringBuilder notificationIDs = new StringBuilder();
    	for(String key:pushUserNotifications.keySet()) {
    		notificationIDs.append(key);
    		notificationIDs.append('|');
    	}
    	readPushNotifications(notificationIDs.toString(), false);
    }
    public void readPushNotifications(String notificationIDs, boolean shouldInjectJS) {
    	if( !webview.config.hasPushNotify ) return;
    	//input params validated in js
    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.deletePushNotifications, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}

    	//notificationIDs is a pipe delimited list of messages
    	
    	String[] tokens = notificationIDs.split("\\|");	
    	String tokenstr = "";
    	
    	for( String token: tokens )
    	{
    		AMSNotification oldnote = pushUserNotifications.get(token);
    		if( oldnote != null )
    		{
    			if(webview!=null) {
    				String userkey = oldnote.userkey;
    				if(userkey == null || userkey.length() == 0) userkey = "-";
    				webview.autoLogEvent("/notification/push/delete.event", userkey);
    			}

    			tokenstr += token + "~";
    		}
    	}
    	AMSResponse response = null;
    	if(tokenstr.length()>0) {
    		tokenstr = tokenstr.substring(0, tokenstr.length()-1);
        	String urlString = webview.config.pushServer + "/?CMD=ampush.readmessages&user=" + strPushUser + "&passcode=" + strPushPass + "&msgs=" + tokenstr + "&appname=" + webview.config.appName;
        	response = getPushServerResponse(urlString);
    	}

    	String js = null;
    	if( response != null && "ok".equals(response.result))
    	{
    		for( String token: tokens )
    		{
    			AMSNotification oldnote = pushUserNotifications.get(token);
        		if( oldnote != null )
    			{
    				pushUserNotifications.remove(token);
    			}        		
    		}

    		if(pushUserNotifications.size()>0) {
            	AMSNotification latest = pushUserNotifications.get(Collections.max(pushUserNotifications.keySet()));
            	C2DMReceiver.showTrayNotification(activity, latest.message, latest.data, String.valueOf(pushUserNotifications.size()), latest.userkey);
    		} else {
    			C2DMReceiver.removeTrayNotification(activity);
    		}
    		
    		js = getNotificationsString();
    		js += "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.delete',true,true);e.success=true;document.dispatchEvent(e);";
    	} else {
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.delete',true,true);e.success=false;e.message='an unexpected error occurred';document.dispatchEvent(e);";
    	}
    	
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		if(shouldInjectJS) injectJS(js);	
    }

    public void sendPushNotification(String userID, String message, String data) {
    	if( !webview.config.hasPushNotify ) return;
    	//input params validated in js

    	String urlString = webview.config.pushServer + "/?CMD=ampush.sendmessage&user=" + userID + "&msg=" + URLEncoder.encode(message) + "&data=" + URLEncoder.encode(data) + "&appname=" + webview.config.appName;
    	AMSResponse response = getPushServerResponse(urlString);

    	String js = null;
    	if( response!=null && "ok".equals(response.result) )
    	{	
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.send',true,true);e.success=true;document.dispatchEvent(e);";
    	} else {
    		js = "var e = document.createEvent('Events');e.initEvent('appMobi.notification.push.send',true,true);e.success=false;e.message='an unexpected error occurred';document.dispatchEvent(e);";
    	}

    	//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
		injectJS(js);	
    }   
	
	public Map<String, Integer> getCountsPerBookmark() {
		HashMap<String, Integer> app2Count = new HashMap<String, Integer>();
		Set<String> keys = pushUserNotifications.keySet();
		for(String key:keys) {
			AMSNotification note = pushUserNotifications.get(key);
			Integer count = app2Count.get(note.target);
			if(count == null) {
				count = 1;
				app2Count.put(note.target, count);
			} else {
				//need to replace object in map?
				count++;
				app2Count.put(note.target, count);
			}
		}
		
		return app2Count;
	}
	
	public void showRichPushViewer(String notificationID, final int closeImageXPort, final int closeImageYPort, final int closeImageXLand, final int closeImageYLand, final int closeImageWidth, final int closeImageHeight) {
    	if( !webview.config.hasPushNotify ) return;
    	//make sure user is logged in
    	if("".equals(strPushUser)) {
    		String js = "throw(\"Error: AppMobi.notification.showRichPushViewer, No push user available.\");";
    		if(Debug.isDebuggerConnected()) Log.d("[appMobi]", js);
    		injectJS(js);	
    		return;
    	}
		try {
			if(isShowingRMP) {
				injectJS("var e =document.createEvent('Events');e.initEvent('appMobi.notification.push.rich.busy');document.dispatchEvent(e);");
			}

			if( notificationID == null || notificationID.length() == 0 ) return;
			
			this.rmpCloseXPort = closeImageXPort;
			this.rmpCloseYPort = closeImageYPort;
			this.rmpCloseXLand = closeImageXLand;
			this.rmpCloseYLand = closeImageYLand;
			this.rmpCloseW = (closeImageWidth==0?36:closeImageWidth);
			this.rmpCloseH = (closeImageHeight==0?36:closeImageHeight);
			
			final AMSNotification richMessage = pushUserNotifications.get(notificationID);
			if(richMessage==null || !richMessage.isRich) return;
			currentlyShownRMPId = notificationID;
			
			rmpPreviewText.setText(richMessage.message);
			
			final boolean hasRichHTML = (richMessage.richhtml != null) && (richMessage.richhtml.length()>0);
			
			final String richHTMLUrl;
			if(hasRichHTML) {
				//write to disk
				ByteArrayInputStream bais = new ByteArrayInputStream(richMessage.richhtml.getBytes());
				//File richHTMLFile = new File(activity.baseDir(), "richMediaPush.html");
				File richHTMLFile = new File(activity.appDir(), "richMediaPush.html");
				if(richHTMLFile.exists()) richHTMLFile.delete();
				FileOutputStream fos = new FileOutputStream(richHTMLFile, false);
				FileUtils.copyInputStream(bais, fos);
				String[] appInfo = activity.getAppInfo2();
				richHTMLUrl = "http://localhost:58888/" + appInfo[AppInfo.NAME] + "/" + appInfo[AppInfo.RELEASE] + "/richMediaPush.html";
			} else {
				richHTMLUrl = null;
			}

			this.onConfigurationChanged(activity.orientation);
			
			activity.runOnUiThread(new Runnable() {

				public void run() {
					if(rmpView == null) {
						//add rich media view to layout
						rmpView = activity.richMediaWebView;
						//rmpView.setBackgroundColor(Color.WHITE);
						rmpView.setVisibility(View.INVISIBLE);
						//set max preview display time
						rmpLayout.addView(rmpView);
					}
					//set the button image: doing this here is a hack to fix skinnability for OTAU
					Drawable remoteCloseImage = null;
					File remoteCloseImageFile = new File(activity.appDir(), "_appMobi/rich_close.png");
					if(remoteCloseImageFile.exists()) {
						remoteCloseImage = (Drawable.createFromPath(remoteCloseImageFile.getAbsolutePath()));
					}
					else {
						remoteCloseImage = ( activity.getResources().getDrawable(R.drawable.rich_close));
					}
					rmpClose.setImageDrawable(remoteCloseImage);

					final long timestamp = System.currentTimeMillis();
					final Runnable finished = new Runnable() {
						public void run() {
							activity.runOnUiThread(new Runnable() {
								public void run() {
									try {
										//ensure minimum preview display time
										long now = System.currentTimeMillis();
										long duration = now-timestamp;
										if(duration<minPreviewDisplayLength) {
											try {
												Thread.sleep(minPreviewDisplayLength-duration);
											} catch (InterruptedException e) {
												if(Debug.isDebuggerConnected()) {
													Log.d("[appMobi]", e.getMessage(), e);
												}
											}
										}
										//show the webview
										rmpView.setVisibility(View.VISIBLE);
										rmpView.bringToFront();
										rmpClose.bringToFront();
										rmpView.requestFocusFromTouch();
									} catch (Exception e) {
										if(Debug.isDebuggerConnected()) {
											Log.e("[appMobi]", e.getMessage(), e);
										}
									}
								}
							});
						}
					};
					final Runnable canceled = new Runnable() {
						public void run() {
							try {
								finished.run();
							} catch (Exception e) {
								if(Debug.isDebuggerConnected()) {
									Log.e("[appMobi]", e.getMessage(), e);
								}
							}
						}
					};

					//load the url or html
					if(hasRichHTML) {
						rmpView.loadUrl(richHTMLUrl, finished, maxPreviewDisplayLength/1000, canceled);
					} else {
						rmpView.loadUrl(richMessage.richurl, finished, maxPreviewDisplayLength/1000, canceled);
					}
					
					//show the view
					rmpLayout.setVisibility(View.VISIBLE);
					//set the flag
					isShowingRMP = true;
					//get focus
					rmpView.requestFocus();
					rmpView.requestFocusFromTouch();
				}
				
			});
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.e("[appMobi]", e.getMessage(), e);
			}
		}
		
	}
	
	public void closeRichPushViewer() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				rmpView.setVisibility(View.INVISIBLE);
				rmpView.stopLoading();
				rmpView.loadUrl("about:blank");
				rmpLayout.setVisibility(View.GONE);
				activity.appView.requestFocus();
				activity.appView.requestFocusFromTouch();
		    	injectJS("var e =document.createEvent('Events');e.initEvent('appMobi.notification.push.rich.close',true,true);e.id="+currentlyShownRMPId+";document.dispatchEvent(e);");
		    	isShowingRMP = false;
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	private void setRMPLandscape() {
		rmpClose.setLayoutParams(new AbsoluteLayout.LayoutParams(rmpCloseW==0?48:rmpCloseW, rmpCloseH==0?48:rmpCloseH, rmpCloseXLand, rmpCloseYLand));
		rmpPreviewText.setLayoutParams(new AbsoluteLayout.LayoutParams(384, 80, 90, 20));
		rmpPreviewText.setHeight(384);//reversed for landscape
		rmpPreviewText.setWidth(80);//reversed for landscape
		int displayWidth = (int) (metrics.heightPixels);//reversed for landscape
		//int displayHeight = (int) ((metrics.widthPixels));//reversed for landscape
		int spinnerWidth = 48;
		int spinnerHeight = 48;
		int spinnerX = (displayWidth/2) - spinnerWidth/2;
		int spinnerY = 125;//(displayHeight/2) - spinnerHeight/2;
		rmpPreviewSpinner.setLayoutParams(new AbsoluteLayout.LayoutParams(spinnerWidth, spinnerHeight, spinnerX, spinnerY));
		rmpLayout.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.rich_splash_ls));
	}
	
	@SuppressWarnings("deprecation")
	private void setRMPPortrait() {
		rmpClose.setLayoutParams(new AbsoluteLayout.LayoutParams(rmpCloseW==0?48:rmpCloseW, rmpCloseH==0?48:rmpCloseH, rmpCloseXPort, rmpCloseYPort));
		rmpPreviewText.setLayoutParams(new AbsoluteLayout.LayoutParams(292, 100, 14, 71));
		rmpPreviewText.setHeight(292);
		rmpPreviewText.setWidth(100);
		int displayWidth = (int) (metrics.widthPixels);
		//int displayHeight = (int) (metrics.heightPixels);
		int spinnerWidth = 48;
		int spinnerHeight = 48;
		int spinnerX = (displayWidth/2) - spinnerWidth/2;
		int spinnerY = 210; //(displayHeight/2) - spinnerHeight/2;
		rmpPreviewSpinner.setLayoutParams(new AbsoluteLayout.LayoutParams(spinnerWidth, spinnerHeight, spinnerX, spinnerY));
		rmpLayout.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.rich_splash_port));
	}

	public void onConfigurationChanged(final int orientation) {
		activity.runOnUiThread(new Runnable() {

			public void run() {		
				if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
					setRMPLandscape();
				} else {
					setRMPPortrait();
				}
				rmpLayout.invalidate();
				rmpClose.bringToFront();
			}
		});
	}
	
    @Override
	protected void injectJS(String js) {
		super.injectJS("javascript:" + js);
	}
    
    public void createTestRichMediaPushMessage(int ident, String data, String message, String righHTML, String richURL, String target, String url, boolean isRich) {
		//debug to test with
		final AMSNotification richMessage = new AMSNotification();
		richMessage.ident = ident;//1234
		richMessage.data = data;
		richMessage.message = message;//"this is a test rich media push message";
		richMessage.richhtml = righHTML;//"";
		richMessage.richurl = richURL;//"http://www.appmobi.com";
		richMessage.target = target;
		richMessage.url = url;
		richMessage.isRich = isRich;
		pushUserNotifications.put(String.valueOf(richMessage.ident), richMessage);
    }
    
}
