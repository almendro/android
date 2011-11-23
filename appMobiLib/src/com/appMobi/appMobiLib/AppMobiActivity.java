package com.appMobi.appMobiLib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.appMobi.appMobiLib.oauth.ServicesData;
import com.appMobi.appMobiLib.util.Debug;
import com.appMobi.appMobiLib.util.FrameLayoutThatDetectsSoftKeyboard;
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.phonegap.api.IPlugin;

import fm.flycast.DPWebServer;
import fm.flycast.DPXMLTrack;
import fm.flycast.FlyCastPlayer;
import fm.flycast.FlyCastPlayer.DisplayTypes;
import fm.flycast.FlyCastPlayer.PlayStartInit;

public class AppMobiActivity extends Activity {
	
	public final static String adCache = "_adcache";//top-level container for all ads
	public final static String mediaCache = "_mediacache";
	public final static String appMobiCache = "appmobicache";
    public final static String appConfig = "appconfig.xml";
    public final static String newConfig = "newappconfig.xml";
    public final static String oldConfig = "oldappconfig.xml";
    public final static String bundle = "bundle.zip";
    public final static String tempBundle = "tempbundle.zip";
    public final static String newBundle = "newbundle.zip";
    public final static int CHOOSE_RELEASE = 0;
    public final static int CHOOSE_TIMER = 1;
	public final static int SCAN_QR_CODE = 2;
	public final static int OAUTH_VERIFICATION = 3;
	public final static int SELECT_PICTURE = 4;
	public static final int PICTURE_RESULT = 5;
	public static final int CONTACT_ADDER_RESULT = 6;
	public static final int CONTACT_CHOOSER_RESULT = 7;
	public static final int CONTACT_EDIT_RESULT = 8;
	
	public int mTimerMaxValue = 0;

    public boolean timerStarted = false;
    public int CurrentSoundVolume = 100;
    public FrameLayoutThatDetectsSoftKeyboard root;
    public AppMobiVersionedCameraPreview arView;
	public LinearLayout webRoot;
	public AppMobiWebView appView;
	PayMobiPayments payMobi;
	public AppMobiWebView richMediaWebView;
	public AppMobiWebView remoteWebView;
	
	public static AppMobiActivity sharedActivity = null;
	
	public String uri;
	public int virtualBackCount;
	public int orientation;
	
	protected boolean isTestContainer = false;

	public FlyCastPlayer flyCastPlayer = null;
	
	String mPlatform = "ANDROID";
	public String mUID = "d464e90a-7c3c-40e9-89e3-473c7b24a405";
	public String BaseQueryString; // Common Querystring for all FlyTunes Web
	
	public PowerManager.WakeLock wl=null;
	public State state = null;

	private DPWebServer dpWebServer=null;
	
	//cached in parseConfig
	public AppConfigData configData;
	public AppConfigData payConfig;
	//updated in downloadConfig
	protected boolean configDataUpdated = false;
	
	//push notification support
	private String userKey; //for auto-logging when started from push notification
	
	private boolean launchedChildActivity = false; 

	ToggleButton tbPackage;
	EditText etAppName;
	TextView tvOr, tvAppLabel, tvAppName, tvPackageLabel, tvPackageName, tvTestContainerVersion, tvReleaseLabel, tvReleaseName;
	Button btnGetApp, btnUseCurrent;
	ProgressDialog progress2;
	ProgressDialog installProgress = null;

	protected boolean startedFromProtocolHandler = false;
	
	//oauth support
	public ServicesData services = null;
	
	//mobius support
	protected boolean isMobius = false;
	private boolean wasStarted = false;
	final public ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();
	
	final String payApp = "idm.1tapnative";
	final String payRel = "3.2.5";
	
	
	String TAG_LIFECYCLE = "LifeCycle";
	
	private List<ConfigurationChangeListener> configurationChangeListeners;
	
	private static final String LAST_VERSIONCODE = "LAST_VERSIONCODE";

	//SPEECH SUPPORT
	public static SpeechKit _speechKit;
	public static final String SpeechKitServer = "65.124.114.176";
	public static final int SpeechKitPort = 443;
	public static final String SpeechKitAppId = "NMDPTRIAL_snarf211220110608075912";
	public static final byte[] SpeechKitApplicationKey = {(byte)0xd9, (byte)0x24, (byte)0xd1, (byte)0xf8, (byte)0x37, (byte)0x17, (byte)0x39, (byte)0x8e, (byte)0x3f, (byte)0x8e, (byte)0xad, (byte)0xe1, (byte)0x9d, (byte)0x97, (byte)0xda, (byte)0xd1, (byte)0xb4, (byte)0x3b, (byte)0x30, (byte)0x89, (byte)0xec, (byte)0x24, (byte)0xcf, (byte)0x79, (byte)0x58, (byte)0x18, (byte)0x73, (byte)0xdf, (byte)0x14, (byte)0xda, (byte)0x4e, (byte)0xed, (byte)0xfe, (byte)0x1f, (byte)0xe5, (byte)0x35, (byte)0x36, (byte)0x1f, (byte)0xc4, (byte)0x76, (byte)0xad, (byte)0x71, (byte)0x57, (byte)0x4a, (byte)0x08, (byte)0x31, (byte)0x1b, (byte)0xbc, (byte)0x6d, (byte)0x4c, (byte)0x45, (byte)0x59, (byte)0x70, (byte)0x14, (byte)0xd2, (byte)0xc8, (byte)0x2c, (byte)0x45, (byte)0xa8, (byte)0x40, (byte)0x20, (byte)0xf6, (byte)0x2e, (byte)0x1e};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//android.os.Debug.startMethodTracing("applab");
		
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onCreate");
		sharedActivity = this;
		virtualBackCount = 0;

        super.onCreate(savedInstanceState);
        
        if(!isMobius) {
        	
        	//if (Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB) {
        	if (Build.VERSION.SDK_INT<11) {
		        getWindow().requestFeature(Window.FEATURE_NO_TITLE); 
		        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); 

				this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        	}
        	
			//check if launched from protocol handler
			if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
	    		setContentView(R.layout.test_container_splash);
	    		setProgressBarIndeterminateVisibility(true);
			}
			else if(!isTestContainer) {
				if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "splash");
	    		setContentView(R.layout.splash);
	    		setProgressBarIndeterminateVisibility(true);
	    	} else {
		        setContentView(R.layout.login);
	
		        //get reference to login controls
		    	tbPackage = (ToggleButton)findViewById(R.id.ToggleButton01);
		    	etAppName = (EditText)findViewById(R.id.EditText01);
		    	tvOr = (TextView)findViewById(R.id.TextView03);
		    	tvAppLabel = (TextView)findViewById(R.id.TextView05);
		    	tvAppName = (TextView)findViewById(R.id.TextView06);
		    	tvPackageLabel = (TextView)findViewById(R.id.TextView04);
		    	tvPackageName = (TextView)findViewById(R.id.TextView07);
		    	tvReleaseLabel = (TextView)findViewById(R.id.TextView10);
		    	tvReleaseName = (TextView)findViewById(R.id.TextView11);
		    	btnGetApp = (Button)findViewById(R.id.Button01);
		    	btnUseCurrent = (Button)findViewById(R.id.Button02);
		    	tvTestContainerVersion = (TextView)findViewById(R.id.TextView08);
		    	tvTestContainerVersion.setText(R.string.version);
	
		    	//add event handlers
		    	btnGetApp.setOnClickListener(new OnClickListener() {
		            public void onClick(View v) {
		            	AppMobiActivity.this.runOnUiThread(new Runnable() {
							//@Override
							public void run() {
			                	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(etAppName.getApplicationWindowToken(), 0);
								progress2 = ProgressDialog.show(AppMobiActivity.this, "appMobi", "loading", true);
				            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "show");
							}
		        		});
		            	new Thread("btnGetApp") {
							@Override
							public void run() {
								getReleases();
							}
		            	}.start();
		            }
		        });
		    	btnUseCurrent.setOnClickListener(new OnClickListener() {
		            public void onClick(View v) {
		            	AppMobiActivity.this.runOnUiThread(new Runnable() {
							//@Override
							public void run() {
			                	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(etAppName.getApplicationWindowToken(), 0);
								progress2 = ProgressDialog.show(AppMobiActivity.this, "appMobi", "loading", true);
				            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "show");
							}
		        		});
		            	new Thread("btnUseCurrent") {
							public void run() {
								//reparse the config so that we run "current" instead of applab
								parseConfig(new File(baseDir(), appConfig));
								
								//launch the app
								launchApp();
								
						    	//check for an update the app
								//checkForUpdate();
							}
		            	}.start();
		            }
		        });
	    	}
        }
        
        state = new com.appMobi.appMobiLib.State();
        
    	//parse bookmarks and setup home buttons
        //commented out for now due to impact on splash screen display
    	//parseBookmarks(false);
        
    	configurationChangeListeners = new ArrayList<ConfigurationChangeListener>();
	}

	@Override
	protected void onStart() {
		super.onStart();// K6 //S2
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onStart");

        if(flyCastPlayer==null) flyCastPlayer = new FlyCastPlayer(this); 
	
		new Thread("onStart") {

			public void run() {
				
		        // If this Activity is being recreated due to a config change (e.g. 
		        // screen rotation), check for the saved SpeechKit instance.
		        _speechKit = (SpeechKit)getLastNonConfigurationInstance();
		        if (_speechKit == null)
		        {
		        	 
		            _speechKit = SpeechKit.initialize(getApplication().getApplicationContext(), SpeechKitAppId, SpeechKitServer, SpeechKitPort,SpeechKitApplicationKey);
		            _speechKit.connect();
		            // TODO: Keep an eye out for audio prompts not working on the Droid 2 or other 2.2 devices.
		            Prompt beep = _speechKit.defineAudioPrompt(R.raw.beep);
		           // _speechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100), null, null);
		        }
		        
				//determine startup type and do any required initialization
				
				//compare previous versioncode to current versioncode to determine if an application update was installed (via market)
				try {
					int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
					SharedPreferences prefs = getSharedPreferences(AppMobiActivity.LAST_VERSIONCODE, Context.MODE_PRIVATE);
					int previousVersionCode = prefs.getInt(AppMobiActivity.LAST_VERSIONCODE, 0);
					//if an apk has been installed, the versioncode will have been revved - delete the old bundle
					if(currentVersionCode>previousVersionCode) {
						extractUpdatedBundle();
					}
					//update the versioncode
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt(AppMobiActivity.LAST_VERSIONCODE, currentVersionCode);
					editor.commit();
				} catch (NameNotFoundException e) {}
				
				// check if there is an app installed
    			boolean anAppIsInstalled = isAnAppInstalled();
    			
    			if(!anAppIsInstalled) {
    				//TODO: this should not be required if the user only evers runs via PH and TC, but currently is due to how config parsing and app setup happens
	    			//1. first time bundled app startup
					//if no app is installed, extract the bundle - this parses the config
					extractBundledApp();
    			}
				
				if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
				//1a. protocol handler
	    			startedFromProtocolHandler = true;
	    			isTestContainer = false;
	    			Uri data = getIntent().getData();
	    			if (data != null) { //what if it IS null - do we need to handle that case?
	    				String strName = data.getQueryParameter("APP");
	    				String strPassword = data.getQueryParameter("PASS");
	    				String strPackage = data.getQueryParameter("PKG");
	    				final String rel = data.getQueryParameter("REL");
	    				
	    				//check for debug override
	    				String strInt = data.getQueryParameter("INT");
	    				if("1".equals(strInt)) Debug.override = true;
	    				
	    				//save to shared prefs
	    				setAppInfo(strName, strPassword, rel, strPackage, false);

		    			//check if appconfig needs deleted for Test Local or Test Local -> Test Anywhere case
    					File appConfigFile = new File(baseDir(), appConfig);
	    				Log.d("[appMobi]", "checking for LD_ appconfig at: " + appConfigFile.getAbsolutePath());//TODO - remove after testing
    					if(appConfigFile.exists()){
    						AppConfigData config = parseConfigWithoutCaching(appConfigFile);
    						if(!config.packageName.equals(strPackage)) {
	    						appConfigFile.delete();
    						}
    					}
	    				
	    				//update app installed flag
	    				anAppIsInstalled = isAnAppInstalled();
	    				//Log.d("[appMobi]", "detected PH, checking if app is installed already: " + strName);//TODO - remove after testing
	    				
	    			}
				}
				if (getIntent().hasExtra(C2DMReceiver.FROM_NOTIFICATION) && getIntent().getStringExtra(C2DMReceiver.FROM_NOTIFICATION).length()>0) {
					//1b. testMode push messaging handler - handle as if started from protocol handler
    				String testModeApp = getIntent().getStringExtra(C2DMReceiver.FROM_NOTIFICATION);
    				//Log.d("[appMobi]", "detected C2DMReceiver.FROM_NOTIFICATION extra, testModeApp:" + testModeApp);
    				
    				//check if the app is installed
					String testModeRelease = null, testModePackage = null;
    				File testModeAppDir = new File(getApplicationContext().getFilesDir(), testModeApp);
    				if(testModeAppDir.exists()) {
    					//find a release subdir with an appconfig.xml
    					File[] children = testModeAppDir.listFiles();
    					for(File child:children) {
    						if(child.isDirectory()) {
    							File testModeAppConfigCandidate = new File(child, appConfig);
    							if(testModeAppConfigCandidate.exists()) {
    								testModeRelease = child.getName();
    								AppConfigData testModeConfig = parseConfig(testModeAppConfigCandidate);
    								testModePackage = testModeConfig.packageName;
    								break;
    							}
    						}
    					}
    				}
    				
	    			if (testModeRelease != null) { //what if it IS null - do we need to handle that case?
	    				//save to shared prefs
	    				setAppInfo(testModeApp, "", testModeRelease, testModePackage, false);
	    				//update app installed flag
	    				anAppIsInstalled = isAnAppInstalled();
	    				Log.d("[appMobi]", "found installed testModeApp:" + testModeApp + ", " + testModeRelease + ", " + testModePackage);

	    				startedFromProtocolHandler = true;
		    			isTestContainer = false;
	    			}
	    			
	    			//save userkey for analytics auto-log event
	    			if(appView!=null) {
	    				String userkey = getIntent().getStringExtra(C2DMReceiver.C2DM_USERKEY_EXTRA);
	    				if(userkey == null || userkey.length() == 0) userkey = "-";
	    				AppMobiActivity.this.userKey = userkey;
	    			}
	    			
				}
				//3. test container - get app - deferred to button click
				//4. test container - use current - deferred to button click

				//check if pay app is installed
				new Thread("onStart:installPayments") {
					public void run() {
						installPayments();
					}
				}.start();
				
				//start up the web server
				//the second condition in the if is a workaround for a race condition between onActivityResult and onStart - sometimes onActivityResult executes before we get here, causing launchedChildActivity to be false when it should be true
				try {
			        if(!isLaunchedChildActivity() && dpWebServer==null) {
			        	dpWebServer = new DPWebServer(webRootDir().getPath(), Integer.parseInt(getString(R.string.DeviceProxyMessagingPort)));//58888
			        	dpWebServer.activate();
			        } else {
			        	setLaunchedChildActivity(false);
			        }
					state.dpWebServerBind = true;
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", e.getMessage(), e);
					}
				}
				
				//if we havent parsed the config yet, parse it now
				if(isAnAppInstalled() && configData==null) {
					parseConfig(new File(baseDir(), appConfig));
				}
				
				//this used to happen in onCreate - now that it is moved to onStart, causes problems with being run every time
				//wasStarted is a hack to guard it so it only happens once
				if(!wasStarted) {
					wasStarted = true;
					new Thread("onStart:getPlayerViewLatest") {
						public void run() {
					    	try { flyCastPlayer.getPlayerViewLatest(); }
					    	catch (Exception e) { 
								if(Debug.isDebuggerConnected()) {
									Log.d("[appMobi]", e.getMessage(), e);
								}
					    	}
						}
					}.start();

			    	if( flyCastPlayer.serviceAlreadyRunning )
					{
	    				Log.d("[appMobi]", "in 'flyCastPlayer.serviceAlreadyRunning' block");//TODO - remove after testing
			    		//hack to avoid NPE in this case (onResume case)
			    		//TODO: get config location from shared prefs
			    		doStartApplication();

			    		//check for an update the app
						//checkForUpdate();
			    		
					}
			    	else
					{
				    	try { 
				    		// handle launching via appmobi protocol when app is not installed
				    		if (startedFromProtocolHandler && !anAppIsInstalled) {
			    				Log.d("[appMobi]", "handle launching via appmobi protocol when app is not installed");//TODO - remove after testing

				    			Uri data = getIntent().getData();

				    			if (data != null) {
				    				runOnUiThread(new Runnable() {
				    					//@Override
				    					public void run()
				    					{
						    	    		addContentView(getLayoutInflater().inflate(R.layout.test_container_splash, null), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
						    	    		setProgressBarIndeterminateVisibility(true);
				    					}
				    				});
				                	new Thread("onStart:getApp") {
				    					public void run() {
				    						try {
				    							getApp();
				    						} catch(Exception e){
				    							Toast.makeText(getApplicationContext(), "Invalid shortcut - unable to launch app.", Toast.LENGTH_LONG).show();
				    						}
				    					}
				                	}.start();
				    			} else {
				    				Toast.makeText(getApplicationContext(), "Invalid shortcut - unable to launch app.", Toast.LENGTH_LONG).show();
				    			}
				    		} else {
				    			// if there is an app installed, show alternate controls
				    			if (anAppIsInstalled && isTestContainer) {
				    				SharedPreferences lastRunPrefs = getSharedPreferences(AppInfo.LAST_RUN_APP_PREFS, Context.MODE_PRIVATE);
				    				
				    				final String 
				    					appName = lastRunPrefs.getString("name", ""), 
				    					releaseName = lastRunPrefs.getString("rel", ""), 
				    					packageName = lastRunPrefs.getString("pkg", "");
				    				
				    				setAppInfo(appName, "", releaseName, packageName, false);
				    				
				    				runOnUiThread(new Runnable() {
				    					//@Override
				    					public void run()
				    					{
				    						etAppName.setText(appName);
				    						tvOr.setVisibility(View.VISIBLE);
				    						btnUseCurrent.setVisibility(View.VISIBLE);
				    						tvAppLabel.setVisibility(View.VISIBLE);
				    						tvAppName.setVisibility(View.VISIBLE);
				    						tvAppName.setText(appName);
				    						tvReleaseLabel.setVisibility(View.VISIBLE);
				    						tvReleaseName.setVisibility(View.VISIBLE);
				    						tvReleaseName.setText(releaseName);
				    						tvPackageLabel.setVisibility(View.VISIBLE);
				    						tvPackageName.setVisibility(View.VISIBLE);
				    						tvPackageName.setText(packageName);
				    					}
				    				});
				    			}
				    	    	else if(!isTestContainer) {
				    	    		//This is the non-test container path
				    	    		
				    	    		//launch the app
				    	    		launchApp();
				    	    		
				    	        	//check for an update the app
				    	    		//checkForUpdate();
				    	    	}
				    		}
				    	}
				    	catch (Exception e) { 
			        		Log.d("[appMobi]", "crash in onStart:"+e.getMessage(),e);//TODO - remove after testing
							if(Debug.isDebuggerConnected()) {
								Log.d("[appMobi]", e.getMessage(), e);
							}
				    	}
				    }
				}
			}
		}.start();
	}	

	@Override
	protected void onResume() {
		super.onResume(); // K7 //S3
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onResume");
		flyCastPlayer.doOnResume();
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onResume End");
	}

	@Override
	protected void onPause() {
		super.onPause(); // K2
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onPause");
		// unregisterReceiver(flyCastPlayer.ReceiverPlayerPrepared);
		try {
			
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) Log.e("ERROR", e.getMessage());
		}
	}

	@Override
	protected void onStop() {
		if(Debug.isDebuggerConnected()) Log.d("StartService", "onStop in DroidGap");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onStop");
		
		/*
		 * if we were on show player and hit Home key its value should set as true
		 */
		flyCastPlayer.playerEditor.putBoolean("ShowPlayer", state.showPlayer);
		flyCastPlayer.playerEditor.commit();
		
		state.showPlayer = false;
		if( appView != null && !isLaunchedChildActivity()) {
			//inject javascript event
			appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.device.suspend',true,true);document.dispatchEvent(ev);");
			if (appView.stats != null)
				appView.autoLogEvent("/device/suspend.event", "-");
		}
		
        if(!isLaunchedChildActivity()) {
		
			try{
				dpWebServer.stop();//stop the Server
			}catch(Exception e){
				
			}	
			state.dpWebServerBind = false;
        }
        
		super.onStop(); // K3
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onRestart");
		
		//check if we are returning from an activity we started
        if(!isLaunchedChildActivity()){
        	appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.device.resume',true,true);document.dispatchEvent(ev);");
			//analytics auto-log event
        	if (appView!=null && appView.stats != null)
        		appView.autoLogEvent("/device/resume.event", "-");
        	
        	//check for an update the app
    		//checkForUpdate();
        	
        	//if this is pre-froyo, check for messages since c2dm doesnt work
        	if(Integer.parseInt(Build.VERSION.SDK) < 8) {
				new Thread("AppMobiNotification:registerDevice") {
					public void run() {
						if(appView.notification!=null) {
							appView.notification.updatePushNotifications();
						}
					}
				}.start();
        	}
        }
	}
	
	@Override
	protected void onDestroy() {
		if(Debug.isDebuggerConnected()) Log.d("StartService", "onDestroy in DroidGap");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onDestroy");

		
		if(flyCastPlayer!=null) {
			flyCastPlayer.doOnDestroy();

			if(flyCastPlayer.playProgressUpdaterShouldRun ==true ) {
				flyCastPlayer.playProgressUpdaterShouldRun = false;
			}
		}
		
		try { 
			Thread.sleep(100);
			flyCastPlayer.UnBindFlyCastServiceRemote();
		} catch (Exception e) { 
			//e.printStackTrace(); 
		}
		try { 
			Thread.sleep(50);
			unregisterReceiver(mBatInfoReceiver);
		} catch (Exception e) { 
			//e.printStackTrace(); 
		}
		try { 
			Thread.sleep(50);
			unregisterReceiver(flyCastPlayer.mReceiver);
		} catch (Exception e) { 
			//e.printStackTrace(); 
		}
		state.showPlayer = false;
		super.onDestroy(); // K4
	}

	@Override
	public void finish() {
		//analytics auto-log event
		if (appView!=null && appView.stats != null)
			appView.autoLogEvent("/device/exit.event", "-");
		
		super.finish();
	}

	
	// Called after bundle has been downloaded and/or verified
	public void doStartApplication()
	{        
		checkForUpdate();
		
		this.runOnUiThread(new Runnable() {

			//@Override
			public void run()
			{
				if(!isMobius) {
			        
					root = new FrameLayoutThatDetectsSoftKeyboard(AppMobiActivity.this);
					root.setBackgroundColor(Color.WHITE);
			
			        //AR
			        arView = AppMobiVersionedCameraPreview.newInstance(AppMobiActivity.this);
			
			        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
			        		ViewGroup.LayoutParams.FILL_PARENT, 0.0F);
			        
			        webRoot = new LinearLayout(AppMobiActivity.this);
			        webRoot.setBackgroundColor(Color.TRANSPARENT);        
			        webRoot.setLayoutParams(containerParams);
			        
			        root.addView(arView);
			        root.addView(webRoot);
			        
			        //AppMobiWebView constructors do some things that block the ui thread and should be threaded off...
			    	richMediaWebView = new AppMobiWebView(AppMobiActivity.this, configData);
			    	remoteWebView = new AppMobiWebView(AppMobiActivity.this, configData);
			        appView = new AppMobiWebView(AppMobiActivity.this, configData);
			        
			        appView.setBackgroundColor(Color.TRANSPARENT);

			        webRoot.addView(appView); 
			        
					//Added by Parveen on April 29,2010 - Register receiver for battery status changes.
					registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			    	
					//if userKey != null, then we were started from a push notification, so autolog an event
					if(userKey!=null) {
	    				appView.autoLogEvent("/notification/push/interact.event", userKey);
	    				AppMobiActivity.this.userKey = null;
					}
					// update push notifications if authorized
			        if(configData!=null && configData.hasPushNotify && configData.pushServer!=null) {
			        	//start new Thread so as not to block the ui
			        	new Thread("doStartApplication:getUserNotifications") {
			        		public void run() {
					        	appView.notification.getUserNotifications();
								//if there is a user, register for notifications
								if( appView!=null && appView.notification!=null && appView.notification.strPushUser!=null && appView.notification.strPushUser.length() != 0 ) {
									C2DMReceiver.refreshAppC2DMRegistrationState(AppMobiActivity.this, false); 
								}
			        		}
			        	}.start();
			        }
			        
					try {
						Runnable r = new Runnable() {
							public void run() {
								runOnUiThread(new Runnable() {
									public void run() {
										setContentView(root);
										webRoot.requestFocusFromTouch();
										//android.os.Debug.stopMethodTracing();
									}
								});
							}
						};
						String url = "http://localhost:58888/" + appInfo[AppInfo.NAME] + "/" + appInfo[AppInfo.RELEASE] + "/index.html";
						//10 second timeout, then show webview
						//this is a workaround for slow loading apps like fnr2go
						appView.loadUrl(url, 15, r);
						
					} catch (Exception e) { 
						if(Debug.isDebuggerConnected()) {
							Log.d("[appMobi]", e.getMessage(), e);
						}
					}
					if(Debug.isDebuggerConnected()) Log.d("WebView", "View is loaded....");

					//check install option - if it is auto install on resume/restart, install now
					if(isAnUpdateDownloaded()) {
						if(configData.installOption==AppConfigData.InstallOption.AUTO_INSTALL_ON_RESTART) {
							installUpdate(false);
							//show user dialog with ok button
					        AlertDialog.Builder alertBldr = new AlertDialog.Builder(AppMobiActivity.this);
					        alertBldr.setTitle("An Application Update Was Installed");
					        alertBldr.setMessage("Description of update: \n" + configData.installMessage);
					        alertBldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					            public void onClick(DialogInterface dialog, int which) {
					            	dialog.dismiss();
					            }
					        });
					        alertBldr.show();
						}
					}
					
			        //payMobi = new PayMobiPayments(AppMobiActivity.this, appView);

					//analytics auto-log event
					if (appView!=null && appView.stats != null)
						appView.autoLogEvent("/device/start.event", "-");
		        }	
			}
		});

		//copy the merchant icon to the right place
		//TODO: this is not the right place to do this, but it should work for testing
		try {
			File merchantIcon = new File(baseDir(), "merchant.png");
			if(configData.paymentIcon!=null && configData.paymentIcon.length()>0) {
				URL payURL = new URL(configData.paymentIcon);
				if(merchantIcon.exists()) merchantIcon.delete();
				FileUtils.copyInputStream(payURL.openStream(), new FileOutputStream(merchantIcon));
			}
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.e("[appMobi]", e.getMessage(), e);
			}
		}
		
	}
	
	protected boolean extractUpdatedBundle() {
    	boolean success = false;
    	try {

    		//copy config from resource to temp file and parse
			InputStream isResourceConfig = getResources().openRawResource(R.raw.appconfig);
			File tempConfig = new File(getFilesDir(), "tempConfig.xml");
			FileOutputStream osTargetConfig = new FileOutputStream(tempConfig, false);
			FileUtils.copyInputStream(isResourceConfig, osTargetConfig);
			configData = parseConfigWithoutCaching(tempConfig);

			//store the app info
    		setAppInfo(configData.appName, "", configData.releaseName, configData.packageName, true);
			
			//move config to correct location and fix file property
    		if(baseConfigFile().exists()) baseConfigFile().delete();
    		File base = baseDir();
    		base.mkdirs();
			File targetConfig = baseConfigFile();
			tempConfig.renameTo(targetConfig);
			configData.file = targetConfig;
			
    		//copy bundle from resource to file
			InputStream isResourceBundle = getResources().openRawResource(R.raw.bundle);
			File newBundleZip = new File(baseDir(), newBundle);
			FileOutputStream osTargetBundle = new FileOutputStream(newBundleZip, false);
			FileUtils.copyInputStream(isResourceBundle, osTargetBundle);
			
			//appconfig.xml and newbundle.zip are set up - continue down the install update path
			success = doInstallUpdate(false);
			
    	} catch(Exception e){
    	} finally {
    	}
    	
    	return success;

	}
	
    protected boolean extractBundledApp() {
    	boolean success = false;
    	try {

    		//copy config from resource to temp file and parse
			InputStream isResourceConfig = getResources().openRawResource(R.raw.appconfig);
			File tempConfig = new File(getFilesDir(), "tempConfig.xml");
			FileOutputStream osTargetConfig = new FileOutputStream(tempConfig, false);
			FileUtils.copyInputStream(isResourceConfig, osTargetConfig);
			configData = parseConfigWithoutCaching(tempConfig);

			//store the app info
    		setAppInfo(configData.appName, "", configData.releaseName, configData.packageName, true);
			
			//move config to correct location and fix file property
    		File base = baseDir();
    		base.mkdirs();
			File targetConfig = baseConfigFile();
			tempConfig.renameTo(targetConfig);
			configData.file = targetConfig;
			
    		//copy bundle from resource to file
			InputStream isResourceBundle = getResources().openRawResource(R.raw.bundle);
			File targetBundle = baseBundleFile();
			FileOutputStream osTargetBundle = new FileOutputStream(targetBundle, false);
			FileUtils.copyInputStream(isResourceBundle, osTargetBundle);

			//create appMobi cache
			File appMobiCacheDir = appDir();
			appMobiCacheDir.mkdirs();

			//extract bundle
			FileUtils.unpackArchive(targetBundle, appDir());
			FileUtils.checkDirectory(appDir());
			
			//create the cache subdirs
			new File(appMobiCacheDir, mediaCache).mkdir();
			new File(appMobiCacheDir, adCache).mkdir();

			//getJS
			getJS(configData);
			
			checkForSkinnableAssets();
			
			success = true;
    	} catch(Exception e){
    		AppMobiActivity.this.runOnUiThread(new Runnable() {
				//@Override
				public void run() {
	    			Toast.makeText(getApplicationContext(), "Unable to launch app.", Toast.LENGTH_SHORT).show();
				}
    		});
    	} finally {
    	}
    	
    	return success;
    }

	protected boolean isAnUpdateDownloaded() {
		return new File(baseDir(), newBundle).exists();
	}

    protected void getReleases() /*throws AppMobiException*/{
    	//check if there are multiple releases
    	String strName = etAppName.getText().toString();
    	String strPackage = tbPackage.isChecked()?"PRODUCTION":"QA";

		DefaultHttpClient httpclient = new DefaultHttpClient();
		if(Debug.isDebuggerConnected()) Log.d("getReleases","https://services.appmobi.com/external/clientservices.aspx?feed=getreleases&app="+etAppName.getText().toString());
		HttpGet httpget = new HttpGet("https://services.appmobi.com/external/clientservices.aspx?feed=getreleases&app="+etAppName.getText().toString());
		try {
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			InputStream is = entity.getContent();
            StringWriter s = new StringWriter();
            while (true) {
                int c = is.read();
                if (c == -1) break;
                s.write((char)c);
            }
            is.close();
            String releases = s.toString(), start="data=\"", end="\" />", appStart = "app=\"";
        	//<XML><releases app="TH" data="Initial Release|test|cachetest|includesandroid|" /></XML>
            //update strName to correct casing
            strName = releases.substring(releases.indexOf(appStart)+appStart.length(), releases.indexOf('"', releases.indexOf(appStart)+appStart.length()));
            releases = releases.substring(releases.indexOf(start)+start.length(), releases.indexOf(end));
            final String[] releaseList = releases.split("\\|");
            if(releaseList==null || releaseList.length==0) {
        		this.runOnUiThread(new Runnable() {
    				//@Override
    				public void run() {
    					if(progress2!=null) progress2.hide();
    	    			Toast.makeText(getApplicationContext(), "Unable to retrieve app data from the cloud.\nDid you push Test Anywhere in the XDK?", Toast.LENGTH_SHORT).show();
    				}
        		});
            }
            else if(releaseList.length>1){
            	Intent intent = new Intent(this, ReleaseListActivity.class);
            	intent.putExtra("releases", releaseList);
            	try {
            		//save to shared prefs
            		setAppInfo(strName, "", "", strPackage, false);
            		this.setLaunchedChildActivity(true);
            		startActivityForResult(intent, CHOOSE_RELEASE);
            	} catch (ActivityNotFoundException e) {
        			if(Debug.isDebuggerConnected()) {
        				Log.d("[appMobi]", e.getMessage(), e);
        			}
            	}

            } else {
        		//save to shared prefs
        		setAppInfo(strName, "", releaseList[0], strPackage, false);
            	getApp();
            }

		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
    		this.runOnUiThread(new Runnable() {
				//@Override
				public void run() {
					if(progress2!=null) progress2.hide();
	    			Toast.makeText(getApplicationContext(), "Unable to retrieve release data.", Toast.LENGTH_SHORT).show();
				}
    		});
		}
    }

	public void getApp() {
		Log.d("[appMobi]", "in getApp");//TODO - remove after testing
    	//try to download the config
    	boolean didGetApp = false;
    	AppConfigData config = downloadConfig();
    	//try to download and extract the app
    	if(config!=null) {
    		Log.d("[appMobi]", "about to downloadApp");//TODO - remove after testing
    		didGetApp = downloadApp(config, false);
        	//if(didExtractApp) {
        	if(didGetApp) {
        		Log.d("[appMobi]", "getApp succeeded, about to launchApp");//TODO - remove after testing

        		//launch the app
        		//launchApp();
        	} else {
        		this.runOnUiThread(new Runnable() {
					//@Override
					public void run() {
						if(progress2!=null) progress2.hide();
		            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "hide");
    	    			Toast.makeText(getApplicationContext(), "Unable to retrieve app data from the cloud.\nDid you push Test Anywhere in the XDK?", Toast.LENGTH_SHORT).show();
					}
        		});
         	}
    	} else {
    		this.runOnUiThread(new Runnable() {
				//@Override
				public void run() {
					if(progress2!=null) progress2.hide();
	            	//if(Debug.isDebuggerConnected()) Log.i("appmobi", "hide");
	    			//Toast.makeText(getApplicationContext(), "Unable to retrieve app data from the cloud.\nDid you push Test Anywhere in the XDK?", Toast.LENGTH_SHORT).show();
	        		//Toast.makeText(getApplicationContext(), "Login failed, please check credentials and try again.", Toast.LENGTH_SHORT).show();
			        AlertDialog.Builder alertBldr = new AlertDialog.Builder(AppMobiActivity.this);
			        alertBldr.setTitle("Error");
			        StringBuffer message = new StringBuffer("Unable to retrieve app data from the cloud.");
			        if(startedFromProtocolHandler) message.append("\nDid you push Test Anywhere in the XDK?");
			        alertBldr.setMessage(message);
			        alertBldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
			            	dialog.dismiss();
			            	finish();
			            }
			        });
			        alertBldr.show();
					
				}
    		});
    	}
    }

    protected void launchApp() /*throws AppMobiException*/{
		this.runOnUiThread(new Runnable() {
			//@Override
			public void run() {
				if(progress2!=null) progress2.setMessage("Launching. Please wait...");
			}
		});
    	//try to launch the app
		try {
			
			if(Debug.isDebuggerConnected()) {
				File appMobiCacheDir = appDir();
				if (appMobiCacheDir.exists()) {
		    		List<String> files = Arrays.asList(appMobiCacheDir.list());
		    		for(String f: files) {
		    			//System.out.println(f);
		    		}
				}else{
					//if(Debug.isDebuggerConnected()) Log.i("fail", "fail");
				}
			}
			
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		} finally {
			if(progress2!=null) progress2.dismiss();
		}

		doStartApplication();
    }

	private boolean getJS(AppConfigData config) throws FileNotFoundException, IOException {
		return getJS(config, false);
	}
	
	protected boolean getJS(AppConfigData config, boolean useBundledJS) throws FileNotFoundException, IOException {
		File targetDir = new File(webRootDir(), config.appName + "/" + config.releaseName);
		targetDir.mkdirs();
		
		// try to get JS file based on config
		boolean didSucceed = false;
		InputStream isJS = null;
		
		//do not use configured javascript in following cases:
		//1. if the debugger is connected, always use bundled js
		//2. explicit override (useBundledJS is true)
		//3. jsURL has not been set (javascript was not configured)
		if(!Debug.isDebuggerConnected() && !useBundledJS && config.jsURL!=null)
		{
			HttpEntity entity=null;
			try {
				DefaultHttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(config.jsURL);
				HttpResponse response = httpclient.execute(httpget);
				entity = response.getEntity();
				isJS = entity.getContent();
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
			}
		}
		
		//default to bundled JS
		if(isJS==null) {
			isJS = getResources().openRawResource(R.raw.appmobi_android);
		}
		
		File bundleJS = new File(targetDir, "_appMobi/appmobi.js");
		//make sure target dir exists
		File _appMobi = new File(targetDir, "_appMobi/");
		if(!_appMobi.exists()) _appMobi.mkdir();
		//if there is already an appmobi.js in the target dir, remove it
		if( bundleJS.exists() ) bundleJS.delete();
		//write appmobi.js to target fir
		FileOutputStream osBundleJS = new FileOutputStream(bundleJS, false);
		FileUtils.copyInputStream(isJS, osBundleJS);
		
		//also extract bundled js to a default location
		FileUtils.copyInputStream(getResources().openRawResource(R.raw.appmobi_android), new FileOutputStream(new File(baseDir(), "appmobi.js")));		

		//copy module javascripts from bundle
		for(String moduleName:getResources().getStringArray(R.array.javascripts)) {
			//skip empty or zero-length module names
			if(moduleName == null || moduleName.length()==0) continue;

			try {
				Field module = R.raw.class.getField(moduleName);
				int id = module.getInt(R.raw.class);
				isJS = getResources().openRawResource(id);
				bundleJS = new File(targetDir, "_appMobi/"+moduleName+".js");
				//if there is already an phonegap.js in the target dir, remove it
				if( bundleJS.exists() ) bundleJS.delete();
				//write appmobi.js to target fir
				osBundleJS = new FileOutputStream(bundleJS, false);
				FileUtils.copyInputStream(isJS, osBundleJS);
			} catch (NoSuchFieldException e) {
				if(Debug.isDebuggerConnected()) Log.e("error", e.getMessage());
			} catch (IllegalArgumentException e) {
				if(Debug.isDebuggerConnected()) Log.e("error", e.getMessage());
			} catch (IllegalAccessException e) {
				if(Debug.isDebuggerConnected()) Log.e("error", e.getMessage());
			}
		}
		didSucceed = true;
		
		return didSucceed;
	}

    private void checkForUpdate() /*throws appMobiException */{
     	
    	Log.d("[appMobi]", "about to start update thread");
    	
		//start an update thread
		new Thread("checkForUpdate") {
			public void run() {
				//go to sleep to let UI get started
				try {
					Thread.sleep(15*1000);
				} catch (InterruptedException e) {}
				File current = baseConfigFile();
		    	AppConfigData currentConfig = configData;
				String newConfigURL = currentConfig.baseURL + "/" + appConfig;
				//append deviceid & platform
				newConfigURL = appendDeviceIdAndPlatform(newConfigURL);
				File updated = new File(baseDir(), newConfig);
				try{
			    	//Log.d("[appMobi]", "updateconfigurl:"+newConfigURL);
					AppMobiCacheHandler.get(newConfigURL, getApplicationContext(), newConfig, baseDir());
				} catch(Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", "unable to retrieve updated config" + e.getMessage(), e);
					}
				}
				if(updated.exists()) {
					//hold on to current version
					int currenVersion = currentConfig.appVersion;
					//make sure config gets reparsed
					configDataUpdated = true;
					//parse the new config
					AppConfigData updateConfig = parseConfig(updated);
					
			    	//Log.d("[appMobi]", updateConfig==null?"updatecnonfig is null":("updateconfig version:" + updateConfig.appVersion));
					
					if(updateConfig!=null) {
						//rename the current config to old and new to current
						current.renameTo(new File(baseDir(), oldConfig));
						updated.renameTo(baseConfigFile());
						//update the cached config
						updateConfig.file = updated;
						configData = updateConfig;
						
						//compare the current version to the updated version
						if(configData.hasLiveUpdate && configData.appVersion>currenVersion) {
							//analytics auto-log event
							if (appView!=null && appView.stats != null)
								appView.autoLogEvent("/device/update/available.event", "-");
							downloadApp(configData, true);
						}
						
						//check for services.xml update
						boolean shouldDownloadServicesXml = false;
						File servicesXml = new File(baseDir(), "services.xml");
						if(servicesXml.exists()) {
							if(configData.hasOAuth && currentConfig.oauthSequence<updateConfig.oauthSequence) shouldDownloadServicesXml = true;
						} else {
							if(configData.hasOAuth) shouldDownloadServicesXml = true;
						}
						if(shouldDownloadServicesXml) {
							String servicesXmlString = updateConfig.baseURL;
							servicesXmlString = servicesXmlString.substring(0, servicesXmlString.lastIndexOf('/'));
							servicesXmlString = servicesXmlString.substring(0, servicesXmlString.lastIndexOf('/'));
							servicesXmlString += "/services.xml";
							AppMobiCacheHandler.get(servicesXmlString, getApplicationContext(), "services.xml", baseDir());
						}
					}
				} else {
			    	//Log.d("[appMobi]", "update thread ran but did not get updated");
				}
				//initialize oauth
				if(configData.hasOAuth) {
					services = new ServicesData(AppMobiActivity.this);
					//if(Debug.isDebuggerConnected()) Log.d("[appMobi]", services.toString());
					
					//inject event 
					//TODO - this should be better encapsulated so that webviews get the event
					boolean hasConfigs = services.name2Service.size()>0;
					final String js = String.format("javascript: try{AppMobi.oauthAvailable = %s;var e = document.createEvent('Events');e.initEvent('appMobi.oauth.setup',true,true);e.success=%s;document.dispatchEvent(e);}catch(e){}", (hasConfigs?"true":"false"), (hasConfigs?"true":"false"));
					appView.oauth.isReady = true;
					runOnUiThread(new Runnable() {

						public void run() {
							appView.loadUrl(js);
						}

					});
				}

			}
		}.start();
    }	
    
    protected AppConfigData downloadConfig() /*throws AppMobiException*/{
    	AppConfigData config = null;
    	
    	//update ui
		this.runOnUiThread(new Runnable() {
			//@Override
			public void run() {
            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "setMessage: getting config");
            	if(progress2!=null) progress2.setMessage("getting config");
			}
		});
		
		String[] appInfo = getAppInfo2();
		String configURL = "https://services.appmobi.com/external/clientservices.aspx?feed=getappconfig&app="+
			appInfo[AppInfo.NAME]+"&pkg="+appInfo[AppInfo.PACKAGE]+"&pw="+appInfo[AppInfo.PASSWORD]+"&rel="+appInfo[AppInfo.RELEASE]+"&redirect=1";
		
		//replace spaces with %20
		configURL = configURL.replace(" ", "%20");
		
		//append deviceid & platform
		configURL = appendDeviceIdAndPlatform(configURL);
		
		//only log is debugging
		if(Debug.isDebuggerConnected()) Log.d("downloadConfig",configURL);
		
		boolean didSucceed = AppMobiCacheHandler.get(configURL, getApplicationContext(), appConfig, baseDir());
		
		if(didSucceed) {
			configDataUpdated = true;
			config = parseConfig(new File(baseDir(), appConfig));
		}
		//Log.d("[appMobi]", "in downloadConfig, configDataUpdated="+configDataUpdated);//TODO - remove after testing
		
		return config;

    }

    protected boolean downloadApp(final AppConfigData config, boolean isAnUpdate) /*throws AppMobiException*/{
		this.runOnUiThread(new Runnable() {
			//@Override
			public void run() {
		    	if(progress2!=null) progress2.setMessage("getting app");
            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "setMessage: getting app");
			}
		});

    	boolean didSucceed = false;
    	try {

			String bundleURL = config.baseURL + "/" + config.bundleName;
			//append deviceid & platform
			bundleURL = appendDeviceIdAndPlatform(bundleURL);

			AppMobiCacheHandler.get(bundleURL, getApplicationContext(), tempBundle, baseDir());
			

			//after download complete, move from temp to new
			File tempBundleFile = new File(baseDir(), tempBundle), newBundleFile = new File(baseDir(), newBundle);
			if(tempBundleFile.exists()) tempBundleFile.renameTo(newBundleFile);

			if(isAnUpdate) {
				//analytics auto-log event
				if (appView!=null && appView.stats != null)
					appView.autoLogEvent("/device/update/download.event", "-");
				//check install option - if it is auto install asap, install now
				if(configData.installOption==AppConfigData.InstallOption.AUTO_INSTALL_ASAP) {
					//show user dialog with ok button
					this.runOnUiThread(new Runnable() {
						//@Override
						public void run() {
				        AlertDialog.Builder alertBldr = new AlertDialog.Builder(AppMobiActivity.this);
				        alertBldr.setTitle("Application Update");
				        alertBldr.setMessage("Description of update: \n" + configData.installMessage);
				        alertBldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				            public void onClick(DialogInterface dialog, int which) {
				            	dialog.dismiss();
				            	new Thread("updatePrompt:installUpdate") {
				            		public void run() {
						            	installUpdate(true);
				            		}
				            	}.start();
				            }
				        });
				        alertBldr.show();
						}
					});
				} else if(configData.installOption==AppConfigData.InstallOption.PROMPT_USER) {
					//prompt the user to decide what to do
					promptUserForUpdate();
				} else if(configData.installOption==AppConfigData.InstallOption.NOTIFY_DEVELOPER) {
					//check install option - if it is notify developer, inject notification
					String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.device.update.available',true,true);e.updateMessage='" + configData.installMessage + 
						"'; document.dispatchEvent(e);";
					//Log.d("[appMobi]", "update available, injecting developer notification: " + js);//TODO - remove after testing
					appView.loadUrl(js);
				}
			} else {
				//Log.d("[appMobi]", "called from getApp, installing app");//TODO - remove after testing
				installUpdate(false);
			}
			didSucceed = true;
    	} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
			}
		}
		
		return didSucceed;
    }	
    
	private void promptUserForUpdate() {
		this.runOnUiThread(new Runnable() {
			public void run () {
		        AlertDialog.Builder alertBldr = new AlertDialog.Builder(AppMobiActivity.this);
		        alertBldr.setTitle("Application Update");
		        //alertBldr.setMessage("Do you want to install it?");
		        alertBldr.setMessage("Description of update: \n" + configData.installMessage + "\nDo you want to install now?");
		        alertBldr.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int which) {
		            	dialog.dismiss();
		            	new Thread("promptUserForUpdate:installUpdate") {
		            		public void run() {
				            	installUpdate(true);
		            		}
		            	}.start();
		            }
		        });
		        alertBldr.setNeutralButton("No", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
						String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.device.update.available',true,true);e.updateMessage='" + configData.installMessage + "'; document.dispatchEvent(e);";
						//Log.d("[appMobi]", "update available but user clicked no, injecting developer notification: " + js);//TODO - remove after testing
						appView.loadUrl(js);
		            }
		        });
		        alertBldr.show();
			}
		});
	}

	public void installUpdate(final boolean shouldRestart) {
		new Thread("installUpdate") {
			public void run() {
				doInstallUpdate(shouldRestart);
			}
		}.start();

	}		
    
	private boolean doInstallUpdate(final boolean shouldRestart) {
    	boolean didSucceed = false;
    	
		//check if an update is available
		File newBundleZip = new File(baseDir(), newBundle);
		//Log.d("[appMobi]", "in installUpdate, newBundleZip.exists()="+newBundleZip.exists());//TODO - remove after testing
		if(newBundleZip.exists()) {
			//install the update
			
			runOnUiThread(new Runnable() {
				//@Override
				public void run() {
			    	if(progress2!=null) progress2.setMessage("installing app");
	            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "setMessage: installing app");
	            	if(shouldRestart) {
	            		appView.loadUrl("about:blank");
	            		installProgress = ProgressDialog.show(AppMobiActivity.this, "", "Installing update");
//	    			installProgress = new ProgressDialog(AppMobiActivity.this);
//	    			ImageView progressView = new ImageView(AppMobiActivity.this);
//	    			progressView.setImageDrawable(getResources().getDrawable(R.drawable.installing_spinner));
//	    			installProgress.setContentView(progressView);
//	            	installProgress.show();
	            	}
				}
			});

			//move the cache, pictures and audio directories
	    	File appMobiCacheDir = appDir();
			File adCacheDirectory = new File(appMobiCacheDir,adCache), tempAdCache = baseAdDir();
			File mediaCacheDirectory = new File(appMobiCacheDir, mediaCache), tempMediaCache = baseMediaDir();
			File picturesDirectory = new File(appMobiCacheDir,"_pictures"), tempPicturesCache = new File(baseDir(), "_pictures");
			File audioDirectory = new File(appMobiCacheDir,"_recordings"), tempAudioCache = new File(baseDir(), "_recordings");
			if(adCacheDirectory.exists()) {
				adCacheDirectory.renameTo(tempAdCache);
			}
			if(mediaCacheDirectory.exists()) {
				mediaCacheDirectory.renameTo(tempMediaCache);
			}
			if(picturesDirectory.exists()) {
				picturesDirectory.renameTo(tempPicturesCache);
			}
			if(audioDirectory.exists()) {
				audioDirectory.renameTo(tempAudioCache);
			}

	    	try {
				//check if appMobiCache exists and delete it
				FileUtils.deleteDirectory(appDir());

				//create it
				appMobiCacheDir.mkdirs();

				FileUtils.unpackArchive(newBundleZip, appDir());
				newBundleZip.delete();
				FileUtils.checkDirectory(appDir());
				
				//create the cache subdirs
				new File(appMobiCacheDir, adCache).mkdir();
				new File(appMobiCacheDir, mediaCache).mkdir();

				didSucceed = getJS(configData);

				//check for skinnable assets
				checkForSkinnableAssets();

				//download services.xml
				if(configData.hasOAuth) {
					String servicesXmlString = configData.baseURL;
					servicesXmlString = servicesXmlString.substring(0, servicesXmlString.lastIndexOf('/'));
					servicesXmlString = servicesXmlString.substring(0, servicesXmlString.lastIndexOf('/'));
					servicesXmlString += "/services.xml";
					AppMobiCacheHandler.get(servicesXmlString, getApplicationContext(), "services.xml", baseDir());
				}
				
	    	} catch (FileNotFoundException e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
			} catch (IOException e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
			} catch (Exception e) {
        		Log.d("[appMobi]", "something bas happened in installUpdate:"+e.getMessage(), e);//TODO - remove after testing
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
			}

			//move the cache, pictures and audio directories back
			if(tempAdCache.exists()) {
				tempAdCache.renameTo(adCacheDirectory);
			}
			if(tempMediaCache.exists()) {
				tempMediaCache.renameTo(mediaCacheDirectory);
			}
			if(tempPicturesCache.exists()) {
				tempPicturesCache.renameTo(picturesDirectory);
			}
			if(tempAudioCache.exists()) {
				tempAudioCache.renameTo(audioDirectory);
			}
			
			//analytics auto-log event
			if (appView!=null && appView.stats != null)
				appView.autoLogEvent("/device/update/install.event", "-");

			//start the app again
			if(shouldRestart) {
				appView.clearCache(true);
		        //File index = new File(baseCacheDir(), "index.html"); 
				appView.loadUrl("http://localhost:58888/" + appInfo[AppInfo.NAME] + "/" + appInfo[AppInfo.RELEASE] + "/index.html");
			} else {
				doStartApplication();
			}
			
			runOnUiThread(new Runnable() {
				//@Override
				public void run() {
			    	if(progress2!=null) progress2.hide();
	            	if(Debug.isDebuggerConnected()) Log.i("appmobi", "setMessage: installing app complete");
	            	if(installProgress!=null) installProgress.dismiss();

				}
			});

			didSucceed = true;
		} else {
			//Log.d("[appMobi]", "newBundle.zip was missing");//TODO - remove after testing
		}
		return didSucceed;
	}
	
	public AppConfigData parseConfigWithoutCaching(File config) {
		AppConfigData data = null;

		try {
            /* Create a URL we want to load some xml-data from. */
            //URL url = new URL("https://services.appmobi.com/testing/AppConfig.xml");

            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            AppConfigHandler handler = new AppConfigHandler(config, this);
            xr.setContentHandler(handler);

            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(config.toURL().openStream()));
            /* Parsing has finished. */

            /* Our ExampleHandler now provides the parsed data to us. */
            data = handler.getParsedData();
            
       } catch (Exception e) {
            /* Display any Error to the GUI. */
            if(Debug.isDebuggerConnected()) Log.e("error", "AppConfig Parsing Error", e);
       }
       
       return data;
	}
	
	//this version of parseConfig does caching and should only be used inside AppMobiActivity or it's descendants
	protected AppConfigData parseConfig(File configFile) {
		AppConfigData data = null;
		
		if(Debug.isDebuggerConnected()) {
			//dump config to console
			try {
				ByteArrayOutputStream bs = new ByteArrayOutputStream((int) configFile.length());
				FileInputStream fis = new FileInputStream(configFile);
				FileUtils.copyInputStream(fis, bs);
				Log.d("[appMobi]", bs.toString());
			} catch (FileNotFoundException e) {
				Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
			} catch (IOException e) {
				Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
			}
		}
		
		//check if config is stale - if not just return what we already have, otherwise go ahead and parse
		if(configData!=null && configFile.getAbsolutePath().equals(configData.file.getAbsolutePath()) && !this.configDataUpdated){
			data = configData;
		} else {
	       data = parseConfigWithoutCaching(configFile);
           configDataUpdated = false;
		}
       if(data!=null) configData = data;
       
       return data;
	}

	public int mBatteryStatus ;
	public BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
        	mBatteryStatus = intent.getIntExtra("status", -1);          
        }
      };
	private IPlugin activityResultCommand;

	/*
     * webview support
     */
	public void setBasicAuthentication(String host, String username, String password) {
        appView.setHttpAuthUsernamePassword(host, "", username, password);
	}
	
	public void addVirtualPage() {
		virtualBackCount++;
	}
	
	public void removeVirtualPage() {
		virtualBackCount--;
		if( virtualBackCount < 0 ) virtualBackCount = 0;
	}

   
    //mobius support
    protected void redirect(String url) {
    	appView.loadUrl(url);
    	//TODO : mobius support
    	//topUrlField.text = url;
    }
    
	public Bookmark getBookmarkForAppName(String name) {
		Bookmark bookmark = null;
		
		if(name!=null) {
			for(Bookmark b:bookmarks) {
				if(name.equals(b.appName)) {
					bookmark = b;
				}
			}
		}
		
		return bookmark;
	}

	protected void parseBookmarks(boolean shouldDownload) {

		bookmarks.clear();

		try {

			File bookmarkXML = new File(getApplicationContext().getFilesDir(), "bookmarks.xml");
		
			if(shouldDownload) {
				//String bookmarksFeed = "http://services.appmobi.com/external/clientservices.aspx?feed=getbrowserbookmarks";
				//AppMobiCacheHandler.get(bookmarksFeed, getApplicationContext(), "bookmarks.xml", baseDir());
			} else {
				//check if the file exists - if not copy out of bundle
				//force it download every time for now
				if(true || !bookmarkXML.exists()) {
		    		//copy bookmarks.xml from resource to file
					InputStream isResource = getResources().openRawResource(R.raw.bookmarks);
					FileOutputStream osFile = new FileOutputStream(bookmarkXML, false);
					FileUtils.copyInputStream(isResource, osFile);
				}
			}
            
			//get saxparser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            //get xmlreader
            XMLReader xr = sp.getXMLReader();
            //set the contenthandler
            BookmarkHandler handler = new BookmarkHandler();
            xr.setContentHandler(handler);
            //parse the xml
            xr.parse(new InputSource(bookmarkXML.toURL().openStream()));
            
            //get the data
            bookmarks.addAll(handler.getParsedData());
            
        	//Cache images locally
        	File bookmarkImageDir = new File(getApplicationContext().getFilesDir(), "bookmarks");
        	if(!bookmarkImageDir.exists()) bookmarkImageDir.mkdir();

        	for( Bookmark bookmark:bookmarks )
        	{
        		bookmark.imageFile = new File(bookmarkImageDir, bookmark.name + ".png");
        		if( !bookmark.imageFile.exists() )
        		{
		    		//copy image from url to file
        			URL imageUrl = new URL(bookmark.image);
					InputStream isResource = imageUrl.openStream();
					FileOutputStream osFile = new FileOutputStream(bookmark.imageFile, false);
					FileUtils.copyInputStream(isResource, osFile);
        		}
        		
        	}
            
            
       } catch (Exception e) {
            if(Debug.isDebuggerConnected()) Log.e("error", "bookmarks.xml Parsing Error", e);
       }

	}
   
	void clearWebCache(boolean includeDiskFiles){
        appView.clearCache(includeDiskFiles);
	}
	
	/*
	 * activity overrides
	 */
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		if( keyCode == KeyEvent.KEYCODE_BACK)
		{
			if(appView!=null && appView.device!=null && appView.device.isShowingRemoteSite) {
				this.appView.device.remoteClose.performClick();
				return true;
			}
			else if( state.showPlayer == true )
			{
				flyCastPlayer.hidePlayer();
				return true;
			}
			else if( virtualBackCount > 0 )
	        {
	        	virtualBackCount--;
	    		appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.device.hardware.back',true,true);document.dispatchEvent(ev);");
	    		return true;
	        }
			else if( appView != null && appView.canGoBack() )
			{
	            appView.goBack();
	            return true;
	        }
		}

       	return super.onKeyDown(keyCode, event);
    }  
	
	//hack for phonegap camera
	public void startActivityForResult(IPlugin command, Intent intent, int requestCode) {
		this.activityResultCommand = command;
		this.setLaunchedChildActivity(true);
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		//hack for phonegap camera
		if(this.activityResultCommand != null) {
			AppMobiActivity.this.setLaunchedChildActivity(false);
			IPlugin command = this.activityResultCommand;
			this.activityResultCommand = null;
			command.onActivityResult(requestCode, resultCode, intent);
			return;
		}
		new Thread() {
    		public void run() {
    			if(requestCode==SCAN_QR_CODE || requestCode==OAUTH_VERIFICATION || requestCode==SELECT_PICTURE || requestCode==PICTURE_RESULT ||  requestCode==CONTACT_CHOOSER_RESULT ||  requestCode==CONTACT_ADDER_RESULT ||  requestCode==CONTACT_EDIT_RESULT) {
	    	    	//in case the activity was killed, wait until appView is ready before continuing in order to avoid NPE
	    	    	while(appView==null || !appView.isReady) {
	    	    		try {
	    					Thread.sleep(250);
	    					if(Debug.isDebuggerConnected()) Log.d("[appMobi]", "in onActivityResult, waiting for appView.isReady");
	    				} catch (InterruptedException e) {}
	    	    	}
    			}    			
    	    	switch(requestCode) {
    				case CHOOSE_RELEASE:
    					AppMobiActivity.this.setLaunchedChildActivity(false);
    					if(resultCode == RESULT_CANCELED) {
    						if(Debug.isDebuggerConnected()) Log.e("appmobi", "releases list failed to display");
    					}     			
    					else {
    						String release = intent.getExtras().getString("RELEASE");
    	            		
    						//save to shared prefs
    						String[] appInfo = getAppInfo2();
    	            		setAppInfo(appInfo[AppInfo.NAME], appInfo[AppInfo.PASSWORD], release, appInfo[AppInfo.PACKAGE], false);
    						
    						//make honeycomb happy
    						new Thread() {
    							public void run() {
    								getApp();
    								
    							}
    						}.start();
    					}
    					break;
    	    		case CHOOSE_TIMER:
    	    			if(resultCode == RESULT_CANCELED) {
    	    				if(Debug.isDebuggerConnected()) Log.e("appmobi", "releases list failed to display");
    	    			} else {
    	    				String time = intent.getExtras().getString("TIMER");
    	    				int min = Integer.parseInt(time);
    	    				timerStarted = true;
    	    				flyCastPlayer.setTimerImage();
    	    				flyCastPlayer.m_flycast_service_remote.startTimerThread(min);		
    	    				if(Debug.isDebuggerConnected()) Log.d("TimerThread", "TimerThread started");
    	    			}
    	    			break;
    	    		case SCAN_QR_CODE:
    	    			if(resultCode == Activity.RESULT_FIRST_USER) {
    	    				finish();
    	    				System.exit(0);
    	    			}
    	    			else {
    	    				appView.device.handleQRCodeResult(resultCode, intent);
    	    			}
    	    			break;
    	    		case OAUTH_VERIFICATION:
    	    			appView.oauth.oAuthVerificationCallback(resultCode, intent);
    	    			break;
    	    		case SELECT_PICTURE:    			
    	    			if(resultCode == RESULT_CANCELED)
    	    				appView.camera.importCancelled();
    	    			else
    	    				appView.camera.imageSelected(intent);
    	    			break;
    	    		case PICTURE_RESULT:
    	    			appView.camera.cameraActivityResult(resultCode, intent);
    	    			break;
    	    		case CONTACT_CHOOSER_RESULT:
    	    			appView.contacts.contactsChooserActivityResult(requestCode, resultCode, intent);
    	    			break;    	    		
    	    		case CONTACT_ADDER_RESULT:
    	    			appView.contacts.contactsAdderActivityResult(requestCode, resultCode, intent);
    	    			break;    	   
    	    		case CONTACT_EDIT_RESULT:
        	    			appView.contacts.contactsEditActivityResult(requestCode, resultCode, intent);
        	    			break;
    	    		default:
    	    			AppMobiActivity.super.onActivityResult(requestCode, resultCode, intent);
    	    			break;
    	    	}
    	    }
    	}.start();

	}	
    
    int addedOptionsItemsOffset = 1;
    ArrayList<String[]> addedOptionsItems;
    protected void addOptionsItem(String text, String callback) {
    	if(addedOptionsItems==null) {
    		addedOptionsItems = new ArrayList<String[]>();
    	}
    	addedOptionsItems.add(new String[]{text, callback});
    }

    /* Creates the menu items */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Shutdown " + getResources().getString(R.string.app_name));
        if(addedOptionsItems!=null) {
	        for(int i=0;i<addedOptionsItems.size();i++) {
	        	int id = i+addedOptionsItemsOffset;
	        	menu.add(0, id, id, addedOptionsItems.get(i)[0]);
	        }
        }
//        for(int i=0;i<menu.size();i++) {
//        	MenuItem item = menu.getItem(i);
//        	item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        }
        return true;
    }

    /* Handles item selections */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
//    	due to new directory structure this is no longer needed
//        switch (item.getItemId()) {
//        case 0:
//        	//check if launched from protocol handler, delete if so
//    		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", this.getIntent().getAction());
//        	if(Intent.ACTION_VIEW.equals(this.getIntent().getAction())) {
//        		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "was started from protocol handler - cleaning up");
//    			//delete the app
//        		FileUtils.deleteDirectory(appDir());
//    			File config = new File(baseDir(), appConfig);
//    			config.delete();
//        	}
//        }
        switch (item.getItemId()) {
        case 0:
        	try {
        		if(flyCastPlayer!=null)flyCastPlayer.ShutDownAppMobi();
        	} catch(Exception e){
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
        	}
        	finish();
    		System.exit(0);
            return true;            

        default:
        	String[] addedOptionsItem = addedOptionsItems.get(item.getItemId()-addedOptionsItemsOffset);
        	if(addedOptionsItem!=null) {
	        	final String js = "javascript:" + addedOptionsItem[1] + "()";
	        	this.runOnUiThread(new Runnable() {
	        		public void run() {
	                	if(appView!=null)appView.loadUrl(js);
	        		}
	        	});
	            return true;
        	} else {
        		return false;
        	}
        }
        	
    }
	
	//from: http://stuffthathappens.com/blog/2010/06/04/android-color-banding/
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		// Helps with color banding
		window.setFormat(PixelFormat.RGBA_8888);
	}	

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
		// don't reload the current page when the orientation is changed
		super.onConfigurationChanged(newConfig);
		
		if(arView != null ) arView.onConfigurationChanged(newConfig);
		
		orientation = newConfig.orientation;

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			orientation = Configuration.ORIENTATION_LANDSCAPE;
			if(state.showPlayer) {
				setContentView(R.layout.main_hvga_l);
			}
		} else {
			orientation = Configuration.ORIENTATION_PORTRAIT;
			if(state.showPlayer) {
				setContentView(R.layout.main_hvga_p);
			}
		}

		for(ConfigurationChangeListener listener:configurationChangeListeners) {
			if(listener!=null) {
				listener.onConfigurationChanged(newConfig.orientation);
			}
		}
    }
    
    /*
	 * utility functions
	 */
	public void trackPageView(String page) {//TODO call new analytics?
		//stats.logEvent(page);
	}

	private void checkForSkinnableAssets() throws Exception {
		//check ad-specific cache for skinnable assets
		File adLoadingGif = new File(appDir(), "_appMobi/ad_loading.gif");
		//if they are missing, extract default into ad-specific cache
		if(!adLoadingGif.exists()) {
			InputStream is = getResources().openRawResource(R.drawable.ad_loading);
			//make sure target dir exists
			//make sure target dir exists
			File _appMobi = new File(appDir(), "_appMobi/");
			if(!_appMobi.exists()) _appMobi.mkdir();
			//write appmobi.js to target fir
			FileOutputStream os = new FileOutputStream(adLoadingGif, false);
			FileUtils.copyInputStream(is, os);
		}
	}
	
	private boolean installPayments() {
		boolean success = false;
		
		//check if pay app is installed
		File payConfigFile = new File(getApplicationContext().getFilesDir() + "/" + payApp + "/" + payRel, appConfig);
		if(!payConfigFile.exists()) {
//			if(payConfigFile.exists())
//				payConfigFile.delete();
			try {
				//copy config from resource to temp file and parse
				InputStream isResourceConfig = getResources().openRawResource(R.raw.payappconfig);
				File tempConfig = new File(getFilesDir(), "tempPayConfig.xml");
				FileOutputStream osTargetConfig = new FileOutputStream(tempConfig, false);
				FileUtils.copyInputStream(isResourceConfig, osTargetConfig);
				payConfig = parseConfigWithoutCaching(tempConfig);

				//move config to correct location and fix file property
				File base = new File(payConfig.baseDirectory);
				base.mkdirs();
				File targetConfig = new File(base, appConfig);
				tempConfig.renameTo(targetConfig);
				payConfig.file = targetConfig;
				
				//copy bundle from resource to file
				InputStream isResourceBundle = getResources().openRawResource(R.raw.paybundle);
				File targetBundle = new File(base, bundle);
				FileOutputStream osTargetBundle = new FileOutputStream(targetBundle, false);
				FileUtils.copyInputStream(isResourceBundle, osTargetBundle);

				//create appMobi cache
				File appDir = new File(payConfig.appDirectory);
				appDir.mkdirs();

				//extract bundle
				FileUtils.unpackArchive(targetBundle, appDir);
				FileUtils.checkDirectory(appDir);
				
				//install js
				getJS(payConfig, true);
				
				success = true;
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.e("[appMobi]", e.getMessage(), e);
				}
			}			
		}
		
		//TODO: check for updates (modify to checkForUpdate() to take a config argument)

		
		return success;
	}
	
	/*
	 * First try to use Telephony Manager device id
	 * If not available... 
	 * then for Gingerbread or above, use: android.os.Build.SERIAL, for Froyo and below, use Secure.ANDROID_ID 
	 * if previous fails or is bogus, fall back to a UUID that is persisted for duration of install only
	 * 
	 * See: http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
	 * 		http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id/5626208#5626208
	 * 		http://code.google.com/p/android/issues/detail?id=10603
	 * 		
	 */
	String getDeviceID() {
		String id = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

		if(id==null) {  // check for devices that do not have a Telephony Manager device id
			
			if(Build.VERSION.SDK_INT>8) { //Gingerbread and above
				
				//the following uses relection to get android.os.Build.SERIAL to avoid having to build with Gingerbread
				try {
					Field serial = android.os.Build.class.getField("SERIAL");
					id = (String) serial.get(android.os.Build.class);
					if(android.os.Build.UNKNOWN.equals(id)) id = "";
				} catch(Exception e) {
					e.printStackTrace();
					id = "";
				}
				
			} else {
				id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
			}
			
			if ("".equals(id) || "9774d56d682e549c".equals(id)) { // check for failure or devices affected by the "9774d56d682e549c" bug
				final String USER_DATA = "device-id";
				SharedPreferences prefs = sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
				id=prefs.getString(USER_DATA, "0000");
				if(id=="0000") { 		// did not exist
					UUID uuid = UUID.randomUUID();
					id=uuid.toString();
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(USER_DATA, id);
					editor.commit();
				}
			}
			
		}
		return id;
	}
	
    String appendDeviceIdAndPlatform(String string){
    	try {
    		string += (string.indexOf('?')!=-1?'&':'?') + "deviceid=" + getDeviceID() + "&platform=android";
    	} catch(Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
			}
    	}
    	return string;   
    }
    
	public File getInstallDirectory()
	{
		return baseDir();
	}
	
	public File getRootDirectory()
	{
		return appDir();
	}
    
	//e.g., ROOT/app/rel
	public File baseDir()
    {
    	String[] appInfo = getAppInfo2();
		
		/*
    	File sddir = Environment.getExternalStorageDirectory();
    	File app = new File(sddir, getResources().getString(R.string.dir_name));
    	File data = new File( app, "data");
    	return data;
    	//*/
    	//*
    	File temp = new File(getApplicationContext().getFilesDir(), appInfo[AppInfo.NAME] + "/" + appInfo[AppInfo.RELEASE]);
    	return temp;
    	//*/
    }
    
    //e.g., APPROOT/_mediacache
    public File baseMediaDir()
    {
    	return new File(baseDir(), mediaCache);
    }
    
    //e.g., APPROOT/_adcache
    public File baseAdDir()
    {
    	return new File(baseDir(), adCache);
    }
    
    //e.g., APPROOT/appmobicache/app/rel
    public File appDir()
    {
    	String[] appInfo = getAppInfo2();
    	return new File(getApplicationContext().getFilesDir(), appMobiCache + "/" + appInfo[AppInfo.NAME] + "/" + appInfo[AppInfo.RELEASE]);
    }
    
    //e.g., APPROOT/appmobicache/
    public File webRootDir()
    {
    	return new File(getApplicationContext().getFilesDir(), appMobiCache);
    }

    //e.g., APPROOT/appconfig.xml
    public File baseConfigFile()
    {
    	return new File(baseDir(), appConfig);
    }
    
    //e.g., APPROOT/bundle.zip
    public File baseBundleFile()
    {
    	return new File(baseDir(), bundle);
    }	
	
	protected boolean isAnAppInstalled() {
   		boolean appIsInstalled = false;
//   		File baseCacheDirectory = baseCacheDir();
//   		String [] list = baseCacheDirectory!=null?baseCacheDirectory.list():null;
//   		if( list == null ) return appIsInstalled;
//    	List<String> files = Arrays.asList(list);
//		if(files.contains(appConfig)) appIsInstalled = true;

   		//check if app info has been initialized
   		boolean appInfoInitialized = (appInfo!=null && appInfo[AppInfo.NAME] != null && appInfo[AppInfo.NAME].length()>0);
   		
   		//if not, try to initialize from sved prefs
   		if(!appInfoInitialized) {
   			appInfo = getStoredAppInfo2();
   			appInfoInitialized = (appInfo!=null && appInfo[AppInfo.NAME] != null && appInfo[AppInfo.NAME].length()>0);
   		}   		
   		
   		if(appInfoInitialized) {
   			appIsInstalled = new File(baseDir(), appConfig).exists();
   		}
   		
		//Log.d("[appMobi]", "checking if an app is installed, appInfoInitialized:"+appInfoInitialized+(appInfoInitialized?(" ("+appInfo[AppInfo.NAME]+")"):"")+" appIsInstalled:"+appIsInstalled);//TODO - remove after testing

   		
		return appIsInstalled;
    }
    
    final class AppInfo {
    	static final String APP_PREFS = "_APPPREFS";
    	static final String LAST_RUN_APP_PREFS = "_LASTRUNAPPPREFS";
    	static final int NAME = 0;
    	static final int PASSWORD = 1;
    	static final int RELEASE = 2;
    	static final int PACKAGE = 3;
    }
    
    boolean appInfoIsDirty = false;
    private void setAppInfo(String name, String password, String rel, String pkg, boolean shouldStore) {
    	synchronized(this) {
    		appInfo = new String[4];
			appInfo[AppInfo.NAME] = name;
			appInfo[AppInfo.PASSWORD] = password;
			appInfo[AppInfo.RELEASE] = rel;
			appInfo[AppInfo.PACKAGE] = pkg;
    		
			appInfoIsDirty = true;
	    	if(shouldStore) {

				SharedPreferences prefs = getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
				SharedPreferences.Editor prefsEditor = prefs.edit();
				
				prefsEditor.putString("name", name);
				prefsEditor.putString("password", password);
				prefsEditor.putString("rel", rel);
				prefsEditor.putString("pkg", pkg);
		
				prefsEditor.commit();
	    	}
	    	
			SharedPreferences prefs = getSharedPreferences(AppInfo.LAST_RUN_APP_PREFS, Context.MODE_PRIVATE);
			SharedPreferences.Editor prefsEditor = prefs.edit();
			
			prefsEditor.putString("name", name);
			prefsEditor.putString("password", password);
			prefsEditor.putString("rel", rel);
			prefsEditor.putString("pkg", pkg);
	
			prefsEditor.commit();
			
    	}
    }
    
    private String[] appInfo = null;
    public String[] getAppInfo2() {
		return appInfo;
	}

	private String[] getStoredAppInfo2() {
    	if(appInfo==null || appInfoIsDirty) {
    		synchronized(this) {
	    		appInfo = new String[4];
				SharedPreferences prefs = getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
				
				appInfo[AppInfo.NAME] = prefs.getString("name", "");
				appInfo[AppInfo.PASSWORD] = prefs.getString("password", "");
				appInfo[AppInfo.RELEASE] = prefs.getString("rel", "");
				appInfo[AppInfo.PACKAGE] = prefs.getString("pkg", "");
    		}
    	}
    	return appInfo;
    }
    
	/*
	 * player support
	 */
	public Drawable portraitBackground, landscapeBackground, buttonBackground;
	public void getBackgrounds() {
		File appMobiCache = appDir(); 
		
		File portraitBackgroundFile = 
			new File(appMobiCache, "_appMobi/player_bg_port.png");
		if(portraitBackgroundFile.exists()){
			portraitBackground = Drawable.createFromPath(portraitBackgroundFile.getAbsolutePath());
		} else {
			portraitBackground = getResources().getDrawable(R.drawable.player_bg_port);
		}

		File landscapeBackgroundFile = 
			new File(appMobiCache, "_appMobi/player_bg_ls.png");
		if(landscapeBackgroundFile.exists()){
			landscapeBackground = Drawable.createFromPath(landscapeBackgroundFile.getAbsolutePath());
		} else {
			landscapeBackground = getResources().getDrawable(R.drawable.player_bg_ls);
		}

		File buttonBackgroundFile = 
			new File(appMobiCache, "_appMobi/player_back_button.png");
		if(buttonBackgroundFile.exists()){
			buttonBackground = Drawable.createFromPath(buttonBackgroundFile.getAbsolutePath());
		} else {
			buttonBackground = getResources().getDrawable(R.drawable.player_back_button);
		}
		
		//Added by Parveen On April 16,2010 - Add backgroud images to hash table;
		flyCastPlayer.cachedImages.put("bg_port", portraitBackground);
		flyCastPlayer.cachedImages.put("bg_land", landscapeBackground);
		flyCastPlayer.cachedImages.put("bg_btn", buttonBackground);				
		
	}
	
	public boolean wasServiceRunning() {
		return flyCastPlayer.wasServiceRunning();
	}
    
	public void sendTrackInfoToWebView(){
		
		if(Debug.isDebuggerConnected()) Log.d("WebView", " DroidGap sendTrackInfoToWebView");
			if (flyCastPlayer.UI_tracklistcurrent != null
					&& flyCastPlayer.UI_tracklistcurrent.children.size() > 0) {
				
				
				DPXMLTrack trackInfo = (DPXMLTrack) flyCastPlayer.UI_tracklistcurrent.children
						.get(flyCastPlayer.UI_trackcurrentindex);
				String artist = trackInfo.artist;
				if (artist == null)
					artist = "";
				String title = trackInfo.title;
				if (title == null)
					title = "";
				String album = trackInfo.album;
				if (album == null)
					album = "";
				String imageUrl = trackInfo.imageurl;
				if (imageUrl == null)
					imageUrl = "";

				String lastTrackInfo = "javascript: AppMobi.playingtrack = {artist:\"" + artist + "\", title:\"" + title + "\", album:\"" + album + "\", imageurl:\"" + imageUrl+ "\"};var e = document.createEvent('Events');e.initEvent('appMobi.player.track.change',true,true);document.dispatchEvent(e);";

				appView.loadUrl(lastTrackInfo);
				}
			else{
				if(Debug.isDebuggerConnected()) Log.d("WebView", " tracklist is null....");	
			}
	}
	
	private void setImagesInTracks()
	{
		//By Parveen - call if need to show view.
		if (state.showPlayer)
		{
			flyCastPlayer.mSwitcher.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_LOADING)));
			flyCastPlayer.mMirror.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_LOADING)));
		}
	}

	// Create (message) handler object that will handle the messages sent to the
	// UI activity from other threads
	public PlayStartInit mPlayStartInit;
	public Handler myGUIUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (state.showPlayer) {
				switch (msg.what) {

				case FlyCastPlayer.DISABLE_SWITCHER:

					flyCastPlayer.mSwitcher.setEnabled(false);
					break;

				case FlyCastPlayer.ENABLE_SWITCHER:
					flyCastPlayer.mSwitcher.setEnabled(true);
					break;

				case FlyCastPlayer.TRACK_UNAVAILABLE:
				flyCastPlayer.ShowAlertDialog("", "Tracks become available as you listen.");
					break;
				/******************** UI Updation Cases *****************************/
				case FlyCastPlayer.GUI_BTN_SHOWCURRENTTRACK_SHOW:
					flyCastPlayer.btnShowCurrentTrack.setEnabled(true);
					flyCastPlayer.btnShowCurrentTrack.setAlpha(255);
					break;
				case FlyCastPlayer.GUI_SHOWCURRENTTRACK_HIDE:
					flyCastPlayer.btnShowCurrentTrack.setEnabled(false);
					flyCastPlayer.btnShowCurrentTrack.setAlpha(128);
				case FlyCastPlayer.GUI_SHOWCURRENTTRACK_GOTO:
				flyCastPlayer.mGallery.setSelection(flyCastPlayer.UI_trackcurrentindex);
					if(Debug.isDebuggerConnected()) Log.d("Selection", "Selected index in Galley " + flyCastPlayer.UI_trackcurrentindex + " in GUI_SHOWCURRENTTRACK_GOTO");
					break;

				/**************************************************/

				case FlyCastPlayer.UPDATE_IMAGE_DATA:
					flyCastPlayer.adpt.updateData(flyCastPlayer.mDPXMLTracklist);
					break;

				case FlyCastPlayer.SET_TRACK_IMAGES:
					setImagesInTracks();
					break;

				case FlyCastPlayer.SET_SELECTED_TRACK_IMG:
				flyCastPlayer.mSwitcher.setImageDrawable(flyCastPlayer.selectedTrack.imageoriginal);
				flyCastPlayer.mMirror.setImageDrawable(flyCastPlayer.selectedTrack.imageoriginal);
				break;
					
			case FlyCastPlayer.SET_NO_ART_WORK_IMG:	
				flyCastPlayer.mSwitcher.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_NO_ARTWORK)));
				flyCastPlayer.mMirror.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_NO_ARTWORK)));
				break;
				
			case FlyCastPlayer.SET_TEMP_TRACK_IMG:
				flyCastPlayer.mSwitcher.setImageDrawable(flyCastPlayer.tracktemp.imageoriginal);
				flyCastPlayer.mMirror.setImageDrawable(flyCastPlayer.tracktemp.imageoriginal);
				break;
				
			case FlyCastPlayer.SET_UNKNOWN_TRACK_IMG:
				flyCastPlayer.mSwitcher.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_UNKNOWN)));
				flyCastPlayer.mMirror.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_UNKNOWN)));
				break;	
				
			case FlyCastPlayer.SET_FLYBACK_IMG:	
				flyCastPlayer.mSwitcher.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_GO_BACK)));	
				flyCastPlayer.mMirror.setImageDrawable((Drawable)(flyCastPlayer.GetCustomImage(FlyCastPlayer.IMAGE_GO_BACK)));
				break;	
			
			case FlyCastPlayer.UPDATE_IMAGE_LIST_ADAPTER:	
				flyCastPlayer.ImageListAdapterCurrent.updateData(flyCastPlayer.UI_tracklistcurrent);
				break;	
				
			case FlyCastPlayer.SET_GALLERY_ADAPTER:
				flyCastPlayer.mGallery.setAdapter(flyCastPlayer.ImageListAdapterCurrent);
				break;
			
			case FlyCastPlayer.SET_GALLERY_SELECTION:
					if(Debug.isDebuggerConnected()) Log.d("Selection", "UI_trackcurrentindex is "
							+ flyCastPlayer.UI_trackcurrentindex
							+ " in  SET_GALLERY_SELECTION");
					if(Debug.isDebuggerConnected()) Log.d("Selection", "mSwitcherTracklistPosition in Galley "
							+ FlyCastPlayer.mSwitcherTracklistPosition
							+ " in SET_GALLERY_SELECTION");
					if (flyCastPlayer.UI_trackcurrentindex == FlyCastPlayer.mSwitcherTracklistPosition + 1) {
						flyCastPlayer.mGallery.setSelection(flyCastPlayer.UI_trackcurrentindex);
						
						if(Debug.isDebuggerConnected()) Log.d("Selection", "Selected index in Galley "
								+ flyCastPlayer.UI_trackcurrentindex
								+ " in SET_GALLERY_SELECTION"
								+ "UI_trackcurrentindex");

					} else {						
						flyCastPlayer.mGallery.setSelection(FlyCastPlayer.mSwitcherTracklistPosition);
						if(Debug.isDebuggerConnected()) Log.d("Selection","Selected index in Galley "+ FlyCastPlayer.mSwitcherTracklistPosition	+ " SET_GALLERY_SELECTION mSwitcherTracklistPosition");
					}

					// flyCastPlayer.mGallery.setSelection(PlayerAppMobiLatest.mSwitcherTracklistPosition);
					break;
			case FlyCastPlayer.SET_GALLERY_SELECTED_ITEM_LISTENER:
				flyCastPlayer.mGallery.setOnItemSelectedListener(flyCastPlayer.mGalleryOnItemSelectedListener);
				break;
			
			case FlyCastPlayer.SEEKBAR_GONE_INVISIBLE:
				flyCastPlayer.imgLive.setVisibility(View.INVISIBLE);
				flyCastPlayer.btnShowCurrentTrack.setEnabled(false);
				flyCastPlayer.btnShowCurrentTrack.setAlpha(128);
				break;
			
			case FlyCastPlayer.SET_SEEKBAR_DEFAULT_VISIBLITY:
				flyCastPlayer.btnShowCurrentTrack.setEnabled(false);
				flyCastPlayer.btnShowCurrentTrack.setAlpha(128);
				break;
			
			case FlyCastPlayer.SET_IMAGEVIEW_VISIBLE:
				flyCastPlayer.imgLive.setVisibility(View.VISIBLE);
				break;
				
			case FlyCastPlayer.SET_IMAGEVIEW_INVISIBLE:
				flyCastPlayer.imgLive.setVisibility(View.INVISIBLE);
				break;
				
			case FlyCastPlayer.SET_SEEKBAR_VISIBLITY:
				flyCastPlayer.btnShowCurrentTrack.setEnabled(true);
				flyCastPlayer.btnShowCurrentTrack.setAlpha(255);
				break;
			}
			}
		}
	};

	// Non Gui Handler
	public Handler nonGUIUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FlyCastPlayer.START_STATION:
				flyCastPlayer.PlayStationStream();
				break;
			case FlyCastPlayer.START_SHOUTCAST:
				flyCastPlayer.PlayShoutcastStream();
				break;
			case FlyCastPlayer.SHOW_PLAYER:
				flyCastPlayer.showPlayer();
				break;
			case FlyCastPlayer.HIDE_PLAYER:
				flyCastPlayer.hidePlayer();
				break;
			case FlyCastPlayer.REWIND:
				try {
					flyCastPlayer.m_flycast_service_remote.MediaPlayerSeekBack();
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
					}
				}
				break;
			case FlyCastPlayer.FORWARD:
				try {
					flyCastPlayer.m_flycast_service_remote.MediaPlayerSeekForward();
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
					}
				}
				break;
			case FlyCastPlayer.PLAYER_VOLUME_UP:
				flyCastPlayer.VolumeUp();
				break;
			case FlyCastPlayer.PLAYER_VOLUME_DOWN:
				flyCastPlayer.VolumeDown();
				break;
			case FlyCastPlayer.SET_MAIN_LAYOUT:	
				if ((flyCastPlayer.mDisplayWidth == 320)&&(flyCastPlayer.mDisplayHeight == 480)) {
					flyCastPlayer.CurrentDisplayType = FlyCastPlayer.DisplayTypes.HVGA_P;	//P = PORTRAIT
					setContentView(R.layout.main_hvga_p);        	
				}
				else if ((flyCastPlayer.mDisplayWidth == 480)&&(flyCastPlayer.mDisplayHeight == 320)) {
					flyCastPlayer.CurrentDisplayType = FlyCastPlayer.DisplayTypes.HVGA_L;	//L = LANDSCAPE
					setContentView(R.layout.main_hvga_l);
				}       
				//--- WVGA and FWVGA New in Android 2.0 Eclair ---
				else if ((flyCastPlayer.mDisplayWidth == 480)&&(flyCastPlayer.mDisplayHeight == 800)) {
					flyCastPlayer.CurrentDisplayType = FlyCastPlayer.DisplayTypes.WVGA_P;
					setContentView(R.layout.main_hvga_p);
				} 			
				else if ((flyCastPlayer.mDisplayWidth == 800)&&(flyCastPlayer.mDisplayHeight == 480)) {
					flyCastPlayer.CurrentDisplayType = DisplayTypes.WVGA_L;
					setContentView(R.layout.main_hvga_l);
				}			
				else if ((flyCastPlayer.mDisplayWidth == 480)&&(flyCastPlayer.mDisplayHeight == 854)) {
					flyCastPlayer.CurrentDisplayType = DisplayTypes.FWVGA_P;
					setContentView(R.layout.main_hvga_p);
				} 			
				else if ((flyCastPlayer.mDisplayWidth == 854)&&(flyCastPlayer.mDisplayHeight == 480)) {
					flyCastPlayer.CurrentDisplayType = DisplayTypes.FWVGA_L;
					setContentView(R.layout.main_hvga_l);
				}	

				else {
					flyCastPlayer.CurrentDisplayType = DisplayTypes.UNKNOWN;
					setContentView(R.layout.main_hvga_p);
				}

				flyCastPlayer.MainLayoutSet();
				//flyCastPlayer.initTracklist();
				break;
		   case FlyCastPlayer.PLAY:
			    flyCastPlayer.playPauseTogle();
			    break;
			    
		   case FlyCastPlayer.TOGGLE_PLAY_PAUSE:
			   flyCastPlayer.playPauseTogle();
			   break;						   			   
		   case FlyCastPlayer.PLAYER_STOP:
			   flyCastPlayer.stopStation();
			   break;
			 case FlyCastPlayer.LOAD_URL:
			      appView.loadUrl("about:blank");
			      break;
			}// SWITCH

		}// handleMessage
	};
	
	public interface ConfigurationChangeListener {
		public void onConfigurationChanged(int orientation);
	}
	public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
		configurationChangeListeners.add(listener);
	}
	
	//have to set this up for polling since injecting js causes soft keyboard to be dismissed
	public boolean isSoftKeyboardShowing() {
		return root.isSoftKeyboardShowing();
	}

	public void setLaunchedChildActivity(boolean launchedChildActivity) {
		this.launchedChildActivity = launchedChildActivity;
	}

	public boolean isLaunchedChildActivity() {
		return launchedChildActivity;
	}
	
	
}
