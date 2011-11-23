package fm.flycast;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.appMobi.appMobiLib.util.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.appMobi.appMobiLib.AppMobiActivity;
import com.appMobi.appMobiLib.GoogleAdActivity;
import com.appMobi.appMobiLib.LocalService;
import com.appMobi.appMobiLib.R;
import com.appMobi.appMobiLib.LocalService.ServiceBinder;

@SuppressWarnings("deprecation")
public class FlyCastPlayer  implements ServiceConnection, AppMobiActivity.ConfigurationChangeListener {

	public SharedPreferences pView = null;
	private boolean pEditor = false;
	public SharedPreferences.Editor playerEditor = null;

	public static boolean playStarted = false;
	private boolean bServiceConnected = false;
	public String uri;
	ImageButton btnTopLeft;
	private static Hashtable<String, Drawable> imageHashTable = new Hashtable<String, Drawable>();
	// -- PackageManager/PackageInfo
	PackageInfo mPackageInfo = new PackageInfo();
	// -- Configuration
	Configuration mConfiguration;

	// -- FlyCast Specific
	String mPlatform = "ANDROID"; // THIS code is for Android devices
	public String mUID = "d464e90a-7c3c-40e9-89e3-473c7b24a405";
	//public String mUID = "00000000-0000-0000-0000-000000000000";

	public String BaseQueryString; // Common Querystring for all FlyTunes Web
									// service calls
	// Initial values of following fetched from strings.xml
	public String FlyTunesBaseUrl; // http://flycast.fm
	public String FlyTunesClientServicesBaseUrl; // http://flycast.fm/External/ClientServices.aspx
	public String StreamingBaseUrl; // DeviceProxy will use a localhost address
	public String WhiteLabelText; // Used in querystring Example: &WL=FLYCAST
	public String DeviceProxyBaseUrl; // "http://localhost:"
	public String DeviceProxyMessagingPort; // "88"
	public String DeviceProxyMediaPort; // "89"

	public String adwidgetSourceUrl = "";
	public String adwidgetDestUrl = "";
	public String adwidgetImageButtonUrl = "";
	public int adwidgetfreqSeconds = 0;
	public Drawable adwidgetimage;

	public boolean Update = false; // Need this??
	/*
	 * public boolean ShowSearch = false; public boolean ShowFavorites = false;
	 * public boolean ShowHistory = false; public boolean ShowSettings = false;
	 */
	public boolean ShowShoutCast = false;
	public String Services = "";
	public String WebPage = "";
	public String Buffersize;

	public enum Modes {
		MyStuff, Guide, Search, Favorites, History, Settings, Web, AudioPlay, VideoPlay
	}

	Modes CurrentMode;

	public enum PlayModes {
		Stop, Play, Pause
	}

	public PlayModes CurrentPlayMode;

	// -- SubPanelModes ---------
	// CurrentMode - current MAIN MODE(Guide/Search/Favorites/History/Settings)
	// Play - Play View with Rotator, Play Controls, and optional small ad
	// overlay browser
	// Web = Large Browser, no Navigation or address bar
	// WebNav - Large Browser with Navigation and Address Bar
	public enum SubPanelModes {
		CurrentMode, Play, Web, WebNav
	}

	SubPanelModes CurrentSubPanelMode;

	// -- DisplayTypes ---------
	// QVGA_P = 240x320 Portrait, QVGA_L = 320x20 Landscape
	// HVGA_P = 320x480 Portrait, HVGA_L = 480x320 Landscape (T-Mobile G1)
	// WQVGA_P = 240x400 Portrait, WQVGA_L = 400x240 Landscape
	// FWQVGA_P = 240x432Portrait, FWQVGA_L = 432x240 Landscape
	// WVGA_P = 480X800 Portrait, WVGA_L = 800X480 Landscape
	// FWVGA_P = 480X854 Portrait, FWVGA_L = 854X480 Landscape (Verizon Droid)
	public enum DisplayTypes {
		UNSUPPORTED, UNKNOWN, QVGA_P, QVGA_L, HVGA_P, HVGA_L, WQVGA_P, WQVGA_L, FWQVGA_P, FWQVGA_L, WVGA_P, WVGA_L, FWVGA_P, FWVGA_L
	}

	public DisplayTypes CurrentDisplayType;
	public DisplayTypes DefaultDisplayType;

	int CurrentSoundVolume = 100;
	Boolean ProcessingClick = false;
	Boolean SoundIsMuted = false;

	Boolean WaitingForFirstStart = false;
	Boolean WaitingForGuide = false;
	Boolean WaitingForBrowserSearch = false;
	Boolean WaitingForBrowserFavorites = false;
	Boolean WaitingForBrowserHistory = false;
	Boolean WaitingForBrowserSettings = false;

	Boolean WaitingForAppStart = false; // Waiting for Application Startup
	public int incident = 0; // RS Used by logging
	public final Handler uiclickhandler = new Handler(); // RS

	// --- BCOM - current values ---------
	public String BCOM_URL = ""; // URL sent by BCOM
	public static final String BCOM_COMMAND_UID = "UID";
	public static final String BCOM_COMMAND_CLOSE = "CLOSE";
	public static final String BCOM_COMMAND_PLAY = "PLAY";
	public static final String BCOM_COMMAND_READY = "READY";
	public static final String BCOM_COMMAND_SHOWCLOSE = "SHOWCLOSE";
	public static final String BCOM_COMMAND_HIDECLOSE = "HIDECLOSE";
	public static final String BCOM_COMMAND_ADCLOSE = "ADCLOSE";// BH_01-30-09
	public static final String BCOM_COMMAND_LOADAD = "LOADAD";
	public static final String BCOM_COMMAND_SHOWAD = "SHOWAD";
	public static final String BCOM_COMMAND_HIDEAD = "HIDEAD";
	public static final String BCOM_COMMAND_ADLOADED = "ADLOADED";

	public static final String BROADCAST_BOOTER_PREPARED = "BOOTER_PREPARED";
	public static final String BROADCAST_BOOTER_COMPLETED = "BOOTER_COMPLETED";
	public static final String BROADCAST_STREAMER_PREPARED = "STREAMER_PREPARED";
	public static final String BROADCAST_STREAMER_COMPLETED = "STREAMER_COMPLETED";
	public static final String BROADCAST_MEDIAPLAYER_PREPARED = "MEDIAPLAYER_PREPARED";
	public static final String BROADCAST_MEDIAPLAYER_ERROR = "MEDIAPLAYER_ERROR";
	public static final String BROADCAST_MEDIAPLAYER_ERRORKEY = "MEDIAPLAYER_ERRORKEY";
	public static final String BROADCAST_GUIMESSAGE = "GUIMESSAGE";
	public static final String BROADCAST_UI_UPDATE_MESSAGE = "UI_UPDATE_MSG";
	public static final String BROADCAST_UI_UPDATE_MESSAGE_TITLE_KEY = "UI_UPDATE_TITLE_KEY";
	public static final String BROADCAST_UI_UPDATE_MESSAGE_BODY_KEY = "UI_UPDATE_BODY_KEY";
	public static final String BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED = "UI_UPDATE_MSG_PLAYFAILED";
	public static final String BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_TITLE_KEY = "UI_UPDATE_PLAYFAILED_TITLE_KEY";
	public static final String BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_BODY_KEY = "UI_UPDATE_PLAYFAILED_BODY_KEY";
	public static final String BROADCAST_UI_PLAYSTARTPROGRESS_OPEN = "PLAYSTARTPROGRESS_OPEN";
	public static final String BROADCAST_UI_PLAYSTARTPROGRESS_CLOSE = "PLAYSTARTPROGRESS_CLOSE";
	public static final String BROADCAST_PLAY_CANCELLED = "PLAY_CANCELLED";
	public static final String BROADCAST_TRACKLIST_NEW = "TRACKLIST_NEW"; //
	public static final String BROADCAST_TRACKLIST_NEW_BODY_KEY = "TRACKLIST_NEW_BODY_KEY"; //
	public static final String BROADCAST_TRACK_IS_PLAYING = "TRACK_IS_PLAYING"; //
	public static final String BROADCAST_TRACK_IS_PLAYING_TRACKCURRENTINDEX_KEY = "TRACK_IS_PLAYING_TRACKCURRENTINDEX_KEY";
	public static final String BROADCAST_TRACK_IS_PLAYING_GUIDSONG_KEY = "TRACK_IS_PLAYING_GUIDSONG_KEY";
	public static final String BROADCAST_TRACK_IS_SWITCHING = "TRACK_IS_SWITCHING"; //
	public static final String BROADCAST_TRACK_IS_SWITCHING_BODY_KEY = "TRACK_IS_SWITCHING_BODY_KEY";
	public static final String BROADCAST_TRACK_PLAY_BUFFER_DELAY = "TRACK_PLAY_BUFFER_DELAY"; // Used
																								// when
	public static final String BROADCAST_DEVICEPROXY_MESSAGELIST = "DEVICEPROXY_MESSAGELIST";
	public static final String BROADCAST_DEVICEPROXY_MESSAGELIST_BODY_KEY = "DEVICEPROXY_MESSAGELIST_BODY_KEY";

	public static final String BROADCAST_FLYBACK = "DEVICEPROXY_FLYBACK";
	public static final String BROADCAST_FLYBACK_BODY_KEY = "DEVICEPROXY_FLYBACK_INDEX";

	public static final String BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE = "DEVICEPROXY_PLAYSTATIONNAME_UPDATE";
	public static final String BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE_BODY_KEY = "DEVICEPROXY_PLAYSTATIONNAME_UPDATE_BODY_KEY";
	public static final String BROADCAST_IS_LIVE_TRACK = "IS_LIVE_TRACK";
	public static final String BROADCAST_IS_LIVE_TRACK_BODY_KEY = "IS_LIVE_TRACK_BODY_KEY";
	public static final String TRACK_NOT_AVAILABLE = "TRACK_NOT_AVAILABLE";

	public static final int TRACK_UNAVAILABLE= 210005;
	public static final int SEND_TRACK_INFO= 210006;

	public static final int DISABLE_SWITCHER = 210007;
	public static final int ENABLE_SWITCHER = 210008;
	public static final String SEND_TRACK_INFO_TO_WEBVIEW = "SEND_TRACK_INFO_TO_WEBVIEW";

	public static final String SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW = "SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW";
	public static final int SEND_STATION_ERROR_MESSAGE= 210009;
	
	public static final String MEDIAPLAYER_STOPPED_WITH_TIMER_THREAD = "MEDIAPLAYER_STOPPED_WITH_TIMER_THREAD";
	public static final int MEDIAPLAYER_STOPPED= 210010;

	private Context RootContext;
	public int mDisplayHeight;
	public int mDisplayWidth;

	public static final String TAG_ACTIVITY = "Player";
	public static final String TAG_LIFECYCLE = "LifeCycle";
	public static final String TAG_PLAYSTATION = "PlayStation";
	public static final String TAG_DPXMLParser = "DPXMLParser";

	public static final String PREFS_NAME = "FlyCastPrefs";
	public static final String PLAYER_PREFS_NAME = "PlayerView";
	public String ReturnMessageTitle = ""; // Used for passing Error message
											// Title back from function
	public String ReturnMessageBody = ""; // Used for passing Error message Body
											// back from function
	String ReturnString;
	public static final String PLAY_ERROR_BODY = "We're sorry but we were unable to connect to this station. "
			+ "Please try another station or verify your connection in Settings.";
	public static final String GUIDE_ERROR_BODY = "Guide Error";

	public static boolean FLYBACKING = false;
	XMLObject XMLObjectRoot;
	XMLObject XMLObjectCurrent;
	XMLObject XMLObjectTemp;
	XMLDirectory XMLDirectoryCurrent;
	XMLDirectory XMLDirectoryTemp;
	XMLNode XMLNodeCurrent;
	public ImageListAdapter ImageListAdapterCurrent;
	String PlayUrlCurrent;
	String GuideTitleCurrent;
	String GuideTitlePrevious;
	String DeviceProxyXMLStringCurrent;
	String PlayStationNameCurrent;
	String PlayNotificationExtra="";
	public DPXMLTracklist UI_tracklistcurrent = null;//get rid of this variable make use of DPApplication.instance.getTracklist
	DPXMLTrack UI_trackcurrent; // currently playing track
	
	public int UI_trackcurrentindex = 0; // currently playing track index in tracklist
	public int UI_trackmaxplayed = 0; // highest track that has been played
	
	public String PlayguidSong;

	XMLParser mParser;
	HttpClient mClient;
	MediaPlayer mMediaPlayer;

	Boolean WebViewPlayOverlayIsActive = false;
	//WebView browserWebMode; // Used for Full size Ads
	EditText WebModeAddress; // browserWebMode - WebModeAddress (EditText)
	ImageButton WebModeNavBack; // browserWebMode - WebModeNavBack
	ImageButton WebModeNavForward; // browserWebMode - WebModeNavForward
	LinearLayout ll_ui_main;
	ImageView background;
	AbsoluteLayout alPlayModeView;

	// -- Top Row Navigation Buttons ---

	public TextView PlayNotification;// Play Mode Notification Bar FlyCast 2.x
	public TextView PlayInfo;// Play Mode Information Bar FlyCast 2.x

	// -- Play Mode buttons ---
	boolean TransportIsShowing = false;
	boolean CloseAdvertIsShowing = false; // BH_01-28-09

	// -- Play Panel Buttons ---
	public ImageButton btnCloseAdvert;
	public ImageButton btnCloseAdvertHybridMode;
	public ImageButton btnHideTransport;
	public ImageButton btnShowCurrentTrack;
	public ImageButton btnPrev;
	public ImageButton btnPauseToggle;
	public ImageButton btnStop;
	public ImageButton btnNext;
	//public ImageButton btnShare;
	public ImageButton btnTimer;
	//ImageButton btnDeleteTrack;
	public TextView tvBufferedTime; // tvBufferedTime.setText("00:00");

	public ImageSwitcher mSwitcher;
	public ReflectionView mMirror;
	public Gallery mGallery;
	public ImageView imgLive;

	Thread mThread;
	Thread mPlayStart1Thread;
	Thread mPlayStart2Thread;
	Thread mPlayStart3Thread;
	Thread mPlayStart4Thread;

	PlayStartLooper mPlayStartLooper;
	AppStartLooper mAppStartLooper;
	public String mNodeId;
	public String mShoutUrl;
	View myview = null;

	public FlyProgressView  seekbarBuffered;
	public String guidSongCurrentlyDisplayed;

	public boolean flycastserviceready = false; // RS_02-13-09
	public boolean mRemoteIsBound = false;
	public LocalService m_flycast_service_remote = null; // The primary interface we

	public static AppMobiActivity appMobiActivity = null; // will be calling on the
	public boolean serviceAlreadyRunning = false;

	public FlyCastPlayer(AppMobiActivity activity) {
		FlyCastPlayer.appMobiActivity = activity;
		pView = FlyCastPlayer.appMobiActivity.getSharedPreferences(FlyCastPlayer.PLAYER_PREFS_NAME, 0);
		playerEditor = pView.edit();
		
		if(wasServiceRunning()){
			serviceAlreadyRunning = true;
			pEditor = pView.getBoolean("PlayerView", false);
			if(pEditor){
				appMobiActivity.state.playStarted = true;	
			}
		}
		else{
			playerEditor.putBoolean("PlayerView", FlyCastPlayer.appMobiActivity.state.playStarted);// false
			playerEditor.commit();
			playerEditor.putBoolean("ShowPlayer", FlyCastPlayer.appMobiActivity.state.showPlayer);//false
			playerEditor.commit();
			playerEditor.clear();
		}

		pEditor = pView.getBoolean("ShowPlayer", false);
		if(pEditor){
			appMobiActivity.state.showPlayer = true;	
		}
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		if(Debug.isDebuggerConnected()) Log.d("StartService", "onServiceConnected in player appmobilatest");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest::onServiceConnected");

		m_flycast_service_remote = (LocalService) ( (ServiceBinder) service ).getService();
		flycastserviceready = true;
		try {
			if(BaseQueryString==null || BaseQueryString.equals("")){
				BaseQueryString = BaseQueryStringUpdate();
			}

			m_flycast_service_remote.setBaseQueryString(BaseQueryString);
		} catch (Exception e) {
		}

		// -- Get ready to launch the AppStartLooper thread
		ReturnMessageTitle = "";// Initialize, no error indicated
		ReturnMessageBody = ""; // Initialize, no error indicated
		AppStartBailout = false;
		mAppStartLooper = new AppStartLooper();
		mAppStartLooper.start();

		// why does this ever get called from the player
		try {
			String mStr = null;
			if (!wasServiceRunningOnCreate)
				mStr = m_flycast_service_remote.setMediaPlayer();
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "FlyCastServiceInit setMediaPlayer RetCode=" + mStr);
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) Log.d("service", e.getMessage());
		}
		setbServiceConnected(true);

		if(serviceAlreadyRunning){
			init();
		try {
			String mStr = null;
			mStr = m_flycast_service_remote.getCurrentPlayingStation();
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "FlyCastServiceInit getCurrentPlayingStation RetCode=" + mStr);

			if( mStr.length() > 0 )
			{
				CurrentPlayMode = PlayModes.Play;
				stationID = mStr;
			}
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) Log.d("service", e.getMessage());
		}
			if(Debug.isDebuggerConnected()) Log.d("WebView", "in onServiceConnected..sendTrackInfoToWebView....calling from onServiceConnected..");
			appMobiActivity.sendTrackInfoToWebView();
		}
	}

	public void onServiceDisconnected(ComponentName className) {
		if(Debug.isDebuggerConnected()) Log.d("StartService", "onServiceDisconnected in player appmobilatest");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest::onServiceDisconnected");
		m_flycast_service_remote = null;
		flycastserviceready = false;
		setbServiceConnected(false);
	}

	public  void BindFlyCastServiceRemote() {
		if(Debug.isDebuggerConnected()) Log.d("StartService", "in BindFlyCastServiceRemote ");
		if(Debug.isDebuggerConnected()) Log.d("StartService", "in Bind.... mRemoteIsBound is" + mRemoteIsBound);
		if(Debug.isDebuggerConnected()) Log.d("Service", "BindFlyCastServiceRemote called ");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest::BindFlyCastServiceRemote");

		// try to unbind before binding to avoid leaking
		if (mRemoteIsBound)
			UnBindFlyCastServiceRemote();
		if(Debug.isDebuggerConnected()) Log.d("StartService", "in Bind.... Going to bind Service");
		Class<?> serviceClass = null;
		try {
			serviceClass = Class.forName(appMobiActivity.getString(R.string.remote_service_action));
		} catch( Exception e ) { }
		mRemoteIsBound = appMobiActivity.bindService( new Intent(appMobiActivity, serviceClass), (ServiceConnection) this, Context.BIND_AUTO_CREATE);
		if(Debug.isDebuggerConnected()) Log.d("StartService", "BindFlyCastServiceRemote in PlayerappmobiLatest");
	}

	// Modified by Parveen on April 05.2010 - Pass reference of this (current
	// context) to close the service.
	public  void UnBindFlyCastServiceRemote() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest::UnBindFlyCastServiceRemote");
		if (mRemoteIsBound) {
			try{
			appMobiActivity.unbindService((ServiceConnection) this);
			mRemoteIsBound = false;
			}catch(Exception e){

			}
		}
	}

	// -- ConnectivityState ---------
	public String CurrentConnectivityState;
	public static final String CONNECTIVITY_NONE = "NONE";
	public static final String CONNECTIVITY_2G = "2G";
	public static final String CONNECTIVITY_3G = "3G";
	public static final String CONNECTIVITY_WIFI = "WIFI";

	public String GetConnectivityState() {
		WifiManager wifiMgr = (WifiManager) appMobiActivity.getSystemService(Context.WIFI_SERVICE);
		if (wifiMgr.isWifiEnabled() == true) {
			return CONNECTIVITY_WIFI;
		}
		
		TelephonyManager telMgr = (TelephonyManager) appMobiActivity.getSystemService(Context.TELEPHONY_SERVICE);
		int data = telMgr.getDataState();
		if( data == TelephonyManager.DATA_DISCONNECTED || data == TelephonyManager.DATA_SUSPENDED )
			return CONNECTIVITY_NONE;
		else
			return CONNECTIVITY_3G;
	}

	public String GetConnectivityType() {
		WifiManager wifiMgr = (WifiManager) appMobiActivity.getSystemService(Context.WIFI_SERVICE);
		if (wifiMgr.isWifiEnabled() == true) {
			return "wifi";
		}
		
		TelephonyManager telMgr = (TelephonyManager) appMobiActivity.getSystemService(Context.TELEPHONY_SERVICE);
		int data = telMgr.getDataState();
		if( data == TelephonyManager.DATA_DISCONNECTED || data == TelephonyManager.DATA_SUSPENDED )
			return "none";
		else
			return "cell";
	}

	public String BaseQueryStringUpdate() {
		CurrentConnectivityState = GetConnectivityState(); // Added by
															// BH_02-13-09
		if (CurrentConnectivityState == CONNECTIVITY_NONE) {
			return CONNECTIVITY_NONE;
		} else {
			BaseQueryString = "?PLAT=" + mPlatform + "&VER="
					+ mPackageInfo.versionName + "&DISPLAYTYPE="
					+ CurrentDisplayType + "&UID=" + mUID + "&SPEED="
					+ CurrentConnectivityState + "&WL=" + WhiteLabelText;
		}
		return "";
	}

	public boolean wasServiceRunningOnCreate = false;
	//public static boolean wasServiceRunningOnCreate = false;

	public void getScreenSizes()
	{
		DisplayMetrics dm = new DisplayMetrics();
		appMobiActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		mDisplayHeight = dm.heightPixels;
		mDisplayWidth = dm.widthPixels;

		//-- Use this data for dynamic layout adjustments (in the future)
		if(mDisplayWidth < mDisplayHeight){//Portrait Mode
			CurrentDisplayType = DisplayTypes.HVGA_P;
			appMobiActivity.orientation = Configuration.ORIENTATION_PORTRAIT;
		}
		else if(mDisplayWidth > mDisplayHeight) {//Landscape mode
			CurrentDisplayType = DisplayTypes.HVGA_L;
			appMobiActivity.orientation = Configuration.ORIENTATION_LANDSCAPE;
		}
		else{
			CurrentDisplayType = DisplayTypes.UNKNOWN; //if we do not know the mode try to load app in Portrait mode
			appMobiActivity.orientation = Configuration.ORIENTATION_PORTRAIT;
		}

		if (appMobiActivity.state.showPlayer)
		{
			if( appMobiActivity.orientation == Configuration.ORIENTATION_PORTRAIT )
				appMobiActivity.setContentView(R.layout.main_hvga_p);
			else
				appMobiActivity.setContentView(R.layout.main_hvga_l);
		}
	}


	private void initialiseLayouts(){
     	try {
    		if (appMobiActivity.state.showPlayer)
    		{
    			if( appMobiActivity.orientation == Configuration.ORIENTATION_PORTRAIT )
    				appMobiActivity.setContentView(R.layout.main_hvga_p);
    			else
    				appMobiActivity.setContentView(R.layout.main_hvga_l);
    		}
    		
	        ll_ui_main = (LinearLayout)appMobiActivity.findViewById(R.id.ui_main);
	        ll_ui_main.setVisibility(View.VISIBLE);

	        background = (ImageView)appMobiActivity.findViewById(R.id.background);

	     	seekbarBuffered = (FlyProgressView)appMobiActivity.findViewById(R.id.seek_buffered);
		} catch (Exception ex) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", ex.getMessage(), ex);
			}
		}
	}

	protected static final int BUTTON_TOP_LEFT =10001;
	protected static final int SHOW_CURRENT_TRACK =10002;
	protected static final int BUTTON_STOP =10003;
	protected static final int BUTTON_PAUSE =10004;
	protected static final int BUTTON_PLAY =10005;
	protected static final int BUTTON_FORWARD =10006;
	protected static final int BUTTON_REWIND =10007;
	protected static final int BUTTON_SHARE =10008;
	protected static final int BUTTON_TIMER =10009;
	public static final int IMAGE_UNKNOWN =10010;
	public static final int IMAGE_NO_ARTWORK =10011;
	public static final int IMAGE_LOADING =10012;
	public static final int IMAGE_ORIGINAL =10013;
	public static final int IMAGE_GO_BACK =10014;
	public static final int IMAGE_GO_LIVE =10015;


	public Hashtable<String,Drawable> cachedImages = new Hashtable<String,Drawable> ();
	//Added by Parveen on April 02,2010 - Check if custom images are provided by user
	//otherwise load default images for player buttons.
	public void SetIFCustomImage(Integer imgName)
	{
		File appMobiCache = appMobiActivity.appDir();
		Drawable dr = null;

		switch(imgName)
		{
			case BUTTON_TOP_LEFT:
				btnTopLeft = (ImageButton) appMobiActivity.findViewById(R.id.buttonBack);
				File btnTopLeftFile = new File(appMobiCache, "_appMobi/player_back_button.png");

				if(btnTopLeftFile.exists())
				{
					dr = (Drawable.createFromPath(btnTopLeftFile.getAbsolutePath()));
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.player_back_button));
				}
				cachedImages.put("btnback", dr);
				btnTopLeft.setImageDrawable(dr);

				break;

			case SHOW_CURRENT_TRACK:
				btnShowCurrentTrack = (ImageButton) appMobiActivity.findViewById(R.id.buttonCurrent);
				File showCurrentTrackFile = new File(appMobiCache, "_appMobi/snap_current_button.png");
				if(showCurrentTrackFile.exists())
				{
					dr = (Drawable.createFromPath(showCurrentTrackFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.current));
				}
				cachedImages.put("btncurrent", dr);
				btnShowCurrentTrack.setImageDrawable(dr);
				break;

			case BUTTON_FORWARD:
				btnNext = (ImageButton) appMobiActivity.findViewById(R.id.buttonNext);

				File btnForwardFile = new File(appMobiCache, "_appMobi/next_button.png");
				if(btnForwardFile.exists())
				{
					dr = (Drawable.createFromPath(btnForwardFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.next_button));
				}

				cachedImages.put("btnnext", dr);
				btnNext.setImageDrawable(dr);

				break;

			case BUTTON_TIMER:
				btnTimer=(ImageButton)appMobiActivity.findViewById(R.id.buttonTimer);
				File btnTimerFile =null;
				if(m_flycast_service_remote!=null && m_flycast_service_remote.isTimerThreadRunning()){
					appMobiActivity.timerStarted = true;
					btnTimerFile = new File(appMobiCache, "_appMobi/timer_button_on.png");	
				}else{
					btnTimerFile = new File(appMobiCache, "_appMobi/timer_button.png");
				}
				
				if(btnTimerFile.exists())
				{
					dr = (Drawable.createFromPath(btnTimerFile.getAbsolutePath()) );
				}
				else
				{
					if(m_flycast_service_remote!=null && m_flycast_service_remote.isTimerThreadRunning())
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.timer_button_on));
					else
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.timer_button));	
				}
				cachedImages.put("btntimer", dr);
				btnTimer.setImageDrawable(dr);

				break;

			case BUTTON_REWIND:
				btnPrev = (ImageButton) appMobiActivity.findViewById(R.id.buttonPrev);

				File btnRewindFile = new File(appMobiCache, "_appMobi/prev_button.png");
				if(btnRewindFile.exists())
				{
					dr = (Drawable.createFromPath(btnRewindFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.prev_button));
				}
				cachedImages.put("btnrewind", dr);
				btnPrev.setImageDrawable(dr);

				break;

			/*
			case BUTTON_SHARE:
				btnShare=(ImageButton)appMobiActivity.findViewById(R.id.buttonShare);
				File btnShareFile = new File(appMobiCache, "_appMobi/share_button.png");
				if(btnShareFile.exists())
				{
					dr = (Drawable.createFromPath(btnShareFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.share_button));
				}

				cachedImages.put("btnshare", dr);
				btnShare.setImageDrawable(dr);

				break;
			//*/

			case BUTTON_STOP:
				btnStop = (ImageButton) appMobiActivity.findViewById(R.id.buttonStop);
				File btnStopFile = new File(appMobiCache, "_appMobi/stop_button.png");
				if(btnStopFile.exists())
				{
					dr = (Drawable.createFromPath(btnStopFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.stop_button));
				}
				cachedImages.put("btnstop", dr);
				btnStop.setImageDrawable(dr);

				break;

			case BUTTON_PAUSE:
				btnPauseToggle = (ImageButton) appMobiActivity.findViewById(R.id.buttonPause);
				File btnTogglePauseFile = new File(appMobiCache, "_appMobi/pause_button.png");
				if(btnTogglePauseFile.exists())
				{
					dr = (Drawable.createFromPath(btnTogglePauseFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.pause_button));
				}
				cachedImages.put("btnpause", dr);
				btnPauseToggle.setImageDrawable(dr);

				break;

			case BUTTON_PLAY:
				btnPauseToggle = (ImageButton) appMobiActivity.findViewById(R.id.buttonPause);
				File btnTogglePlayFile = new File(appMobiCache, "_appMobi/play_button.png");
				if(btnTogglePlayFile.exists())
				{
					dr = (Drawable.createFromPath(btnTogglePlayFile.getAbsolutePath()) );
				}
				else
				{
					dr = ( appMobiActivity.getResources().getDrawable(R.drawable.play_button));
				}
				cachedImages.put("btnplay", dr);
				btnPauseToggle.setImageDrawable(dr);

				break;
		}
	}

	//Added by Parveen on April 16,2010 - Check and set for available skin.
	private void SetAvailableSkin()
	{
		appMobiActivity.getBackgrounds();

		SetIFCustomImage(BUTTON_TOP_LEFT);
		SetIFCustomImage(SHOW_CURRENT_TRACK);
		SetIFCustomImage(BUTTON_REWIND);
		SetIFCustomImage(BUTTON_PLAY);
		SetIFCustomImage(BUTTON_PAUSE);

		//Added by Parveen on April 12,2010 - Check current playing mode and show button accordingly.
		String mStr = null;
		if(wasServiceRunningOnCreate)
			CurrentPlayMode = PlayModes.Play;
		mStr = CurrentPlayMode.name().toLowerCase();
		if (mStr.equals("play"))
		{
			SetIFCustomImage(BUTTON_PAUSE);
		}
		else
		{
			SetIFCustomImage(BUTTON_PLAY);
		}

		// ImageButton btnStop(Stop Currently Playing Station)
		SetIFCustomImage(BUTTON_STOP);
		// ImageButton btnNext(SEEK forward 30 seconds in current track)
		SetIFCustomImage(BUTTON_FORWARD);
		SetIFCustomImage(BUTTON_SHARE);
		SetIFCustomImage(BUTTON_TIMER);
	}

	//Added by Parveen on April 16,2010 - Get images from hashtable.
	private void GetCachedImages()
	{
		appMobiActivity.portraitBackground = (Drawable)cachedImages.get("bg_port");
		appMobiActivity.landscapeBackground = (Drawable)cachedImages.get("bg_land");
		appMobiActivity.buttonBackground = (Drawable)cachedImages.get("bg_btn");

		//For rewind button.
		btnTopLeft = (ImageButton) appMobiActivity.findViewById(R.id.buttonBack);
		btnTopLeft.setImageDrawable((Drawable)cachedImages.get("btnback"));

		//For current track button.
		btnShowCurrentTrack = (ImageButton) appMobiActivity.findViewById(R.id.buttonCurrent);
		btnShowCurrentTrack.setImageDrawable((Drawable)cachedImages.get("btncurrent"));

		// ImageButton btnNext(SEEK forward 30 seconds in current track)
		btnNext = (ImageButton) appMobiActivity.findViewById(R.id.buttonNext);
		btnNext.setImageDrawable((Drawable)cachedImages.get("btnnext"));

		// ImageButton btnTop(SEEK back 30 seconds in current track)
		btnPrev = (ImageButton) appMobiActivity.findViewById(R.id.buttonPrev);
		btnPrev.setImageDrawable((Drawable)cachedImages.get("btnrewind"));

		//Added by Parveen on April 12,2010 - Check current playing mode and show button accordingly.
		String mStr = null;
		mStr =CurrentPlayMode.name().toLowerCase();
		if (mStr.equals("play"))
		{
			btnPauseToggle = (ImageButton) appMobiActivity.findViewById(R.id.buttonPause);
			btnPauseToggle.setImageDrawable((Drawable)cachedImages.get("btnpause"));
		}
		else
		{
			btnPauseToggle = (ImageButton) appMobiActivity.findViewById(R.id.buttonPause);
			btnPauseToggle.setImageDrawable((Drawable)cachedImages.get("btnplay"));
		}

		// ImageButton btnStop(Stop Currently Playing Station)
		btnStop = (ImageButton) appMobiActivity.findViewById(R.id.buttonStop);
		btnStop.setImageDrawable((Drawable)cachedImages.get("btnstop"));

		//btnShare=(ImageButton)appMobiActivity.findViewById(R.id.buttonShare);
		//btnShare.setImageDrawable((Drawable)cachedImages.get("btnshare"));

		btnTimer=(ImageButton)appMobiActivity.findViewById(R.id.buttonTimer);
		btnTimer.setImageDrawable((Drawable)cachedImages.get("btntimer"));
	}

	private void SetImageListeners()
	{
		btnTopLeft.setOnClickListener(btnTopLeftListener);

		btnShowCurrentTrack.setVisibility(View.VISIBLE);
		btnShowCurrentTrack.setOnClickListener(btnShowCurrentTrackListener);

		btnNext.setOnClickListener(btnNextListener);

		//btnTimer.setEnabled(false);
		//btnTimer.setAlpha(128);
		btnTimer.setOnClickListener(btnTimerListener);

		btnPrev.setOnClickListener(btnTopListener);

		//btnShare.setEnabled(false);
		//btnShare.setAlpha(128);

		btnStop.setOnClickListener(btnStopListener);

		btnPauseToggle.setOnClickListener(btnPauseToggleListener);
	}

	public boolean isFirstTime=true;

	
	// -- The Main application layout displayed after login ---------

	public void MainLayoutSet() {

		initialiseLayouts();

		//Added by Parveen on April 16,2010 - Get player images only once.
		//Set skin only once at first time.
		if (isFirstTime)
		{
			SetAvailableSkin();
			isFirstTime = false;
		}
		else
		{
			GetCachedImages();
		}
		SetImageListeners();

		// -- Psuedo-CoverFlow Setup BH_09-01-09 ---------
		// Temp fix for Change user Bug where we were trying to init
		// mSwitcher.setFactory(mSwitcherViewFactory); again
		
		mMirror = (ReflectionView ) appMobiActivity.findViewById(R.id.mirror);

		//Switcher should not be null in this case as we switch b/w player and html view.
		mSwitcher = (ImageSwitcher) appMobiActivity.findViewById(R.id.switcher);
		mSwitcher.setFactory(mSwitcherViewFactory);
		mSwitcher.setInAnimation(AnimationUtils.loadAnimation(appMobiActivity, android.R.anim.fade_in));
		mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(appMobiActivity, android.R.anim.fade_out));
		mSwitcher.setOnClickListener(mSwitcherOnClickListener);
		mSwitcher.setImageResource(R.drawable.retrieving_data);

		mGallery = (Gallery) appMobiActivity.findViewById(R.id.gallery);
		ImageListAdapterCurrent = new ImageListAdapter(appMobiActivity, this,
				UI_tracklistcurrent, UI_trackcurrentindex, UI_trackmaxplayed, uiclickhandler);
		mGallery.setAdapter(ImageListAdapterCurrent);
		mGallery.setOnItemSelectedListener(mGalleryOnItemSelectedListener);

		// -- Setup the Live Track Indicator View
		imgLive = (ImageView) appMobiActivity.findViewById(R.id.imageLive); // BH_01-04-10
		PlayInfo = (TextView) appMobiActivity.findViewById(R.id.textPlayInfo);

		// --- Find alPlayModeView ----------
		alPlayModeView = (AbsoluteLayout) appMobiActivity.findViewById(R.id.player);

		//CurrentMode = Modes.Guide;
		//CurrentPlayMode = PlayModes.Play;
		ll_ui_main.setVisibility(View.VISIBLE);
		SubPanelModeSet(SubPanelModes.Play, "");
		
		if (appMobiActivity.state.playStarted) {
			EnableCommandButtons();
		} else {
			DisableCommandButtons();
		}
	}

	private void DisableCommandButtons() {
		btnShowCurrentTrack.setEnabled(false);
		btnShowCurrentTrack.setAlpha(128);
		btnPrev.setEnabled(false);
		btnPrev.setAlpha(128);
		btnPauseToggle.setEnabled(false);
		btnPauseToggle.setAlpha(128);

		// Added by Parveen on may 13,2010 - Make button disable if stationid is
		// null
		if (stationID == null || stationID.equals("")) {
			btnStop.setEnabled(false);
			btnStop.setAlpha(128);
		}

		btnNext.setEnabled(false);
		btnNext.setAlpha(128);
		btnTimer.setEnabled(false);
		btnTimer.setAlpha(128);
		mGallery.setEnabled(false);
		mSwitcher.setEnabled(false);
	}

	private void EnableCommandButtons() {
		// btnShowCurrentTrack.setEnabled(true);
		// btnShowCurrentTrack.setAlpha(255);
		try{

			if(CurrentPlayMode==PlayModes.Play){
				btnPrev.setEnabled(true);
				btnPrev.setAlpha(255);
				btnNext.setEnabled(true);
				btnNext.setAlpha(255);
			}else if(CurrentPlayMode==PlayModes.Pause){
				btnNext.setEnabled(false);
				btnNext.setAlpha(128);
				btnPrev.setEnabled(false);
				btnPrev.setAlpha(128);
				}

		btnPauseToggle.setEnabled(true);
		btnPauseToggle.setAlpha(255);
		btnStop.setEnabled(true);
		btnStop.setAlpha(255);
		btnTimer.setEnabled(true);
		btnTimer.setAlpha(255);

		mGallery.setEnabled(true);
		mSwitcher.setEnabled(true);
		}catch(Exception e){
			if(Debug.isDebuggerConnected()) Log.d("StartService", "Null Pointer Exception in EnableCommandButtons");
		}
	}

	// -- btnTopLeft Listener ---------
	private OnClickListener btnTopLeftListener = new OnClickListener() {
		public void onClick(View v) {
			hidePlayer();
		}
	};

	private void SubPanelModeSet(SubPanelModes SubPanelMode, String Url) {


		if(Debug.isDebuggerConnected()) Log.d("SubPanelModeSet", "Url=" + Url);
		ReturnMessageTitle = "";//Initialize, no error indicated
		ReturnMessageBody = "";	//Initialize, no error indicated

	//	ResetSubPanelsVisibility(); //Kill ALL "SubPanels"
		if (BaseQueryStringUpdate()!="") {
			ShowAlertDialog("Connection Error", "No network connectivity, restart FlyCast when connected.");
			return;
		}
		 if (SubPanelMode == SubPanelModes.Play) {
	  		WebViewPlayOverlayIsActive = false;
	  		alPlayModeView.setVisibility(View.VISIBLE);//LinearLayout
	  		if( appMobiActivity.orientation == Configuration.ORIENTATION_PORTRAIT )
	  			//ll_ui_main.setBackgroundDrawable(appMobiActivity.portraitBackground);
  				background.setImageDrawable(appMobiActivity.portraitBackground);
	  		else
	  			//ll_ui_main.setBackgroundDrawable(appMobiActivity.landscapeBackground);
	  			background.setImageDrawable(appMobiActivity.landscapeBackground);
	  		CurrentSubPanelMode = SubPanelModes.Play;
	  	}
		 else if (SubPanelMode == SubPanelModes.CurrentMode) {
			 CurrentSubPanelMode = SubPanelModes.CurrentMode; //for all 5 CurrentModes
		 }
	  	//handle progressbar
	  	showHideProgressBar();
	}

	// Intercept BACK KEY and do nothing when pressed UNLESS Progess Dialog is
	// up
		// --- MENU ---------
	public static final int OPTIONS_MENU_FAVORITESTATIONADD = 0;
	public static final int OPTIONS_MENU_HIDEFLYCAST = 1;
	public static final int OPTIONS_MENU_SHUTDOWNFLYCAST = 2;
	public static final int OPTIONS_MENU_INFORMATION = 3;

	public void ShutDownAppMobi() {
		try {
			if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest:ShutDownAppMobi bgn");
			appMobiActivity.state.shutDownAppMobi = true;
			//no need to do this here: it happens in onDestroy()
			//appMobiActivity.unregisterReceiver(mReceiver);
			UnBindFlyCastServiceRemote();
			Class<?> serviceClass = null;
			try {
				serviceClass = Class.forName(appMobiActivity.getString(R.string.remote_service_action));
			} catch( Exception e ) { }
			mRemoteIsBound = appMobiActivity.stopService(new Intent(appMobiActivity, serviceClass));
			wasServiceRunningOnCreate = false;

			if(appMobiActivity.flyCastPlayer!=null){
				appMobiActivity.state.playStarted = false;
				playerEditor.putBoolean("PlayerView", appMobiActivity.state.playStarted );// Key, // Value
				playerEditor.commit();
				playerEditor.clear();
			}
			else{
				playerEditor.putBoolean("PlayerView", false);
				playerEditor.clear();

			}

			//Thread.sleep(100);
			//WebViewsClearDestroy();
			this.finalize();
			currentStationID= "";

			appMobiActivity.finish();

//			GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
//			if(tracker!=null)tracker.stop();

			if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "PlayerAppMobiLatest:ShutDownAppMobi end"); // this cannot be
		} catch (Throwable e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
	}

	// --- btnShowCurrentTrackListener ---------
	private OnClickListener btnShowCurrentTrackListener = new OnClickListener() {
		public void onClick(View v) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "btnShowCurrentTrackListener");
			Message message = new Message();
			message.what = GUI_SHOWCURRENTTRACK_GOTO; //
			SendGUIUpdateMessage(message);
		}
	};

	public void setPlayButton(){
		CurrentPlayMode = PlayModes.Pause;
		SetIFCustomImage(BUTTON_PLAY);
		btnNext.setEnabled(false);
		btnNext.setAlpha(128);
		btnPrev.setEnabled(false);
		btnPrev.setAlpha(128);
	}

	// --- btnPauseToggleListener ----------
	// --- Toggles Pause and Play ----------
	private OnClickListener btnPauseToggleListener = new OnClickListener() {
		public void onClick(View v) {
			String mStr = null;
			try {
				mStr = m_flycast_service_remote.setMediaPlayerPauseToggle();
				
			if (mStr.equals("play")) {
				CurrentPlayMode = PlayModes.Play;
				// Inject event to web view
				if( mNodeId.equals("0") )
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.play',true,true);document.dispatchEvent(ev);");
				else 
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.play',true,true);document.dispatchEvent(ev);");
				SetIFCustomImage(BUTTON_PAUSE);
				btnNext.setEnabled(true);
				btnNext.setAlpha(255);
				btnPrev.setEnabled(true);
				btnPrev.setAlpha(255);
			} else if (mStr.equals("pause")) {
				// Inject event to web view
				if( mNodeId.equals("0") )
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.pause',true,true);document.dispatchEvent(ev);");
				else 
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.pause',true,true);document.dispatchEvent(ev);");
				setPlayButton();

			} else if (mStr.equals("stopped")) {
				CurrentPlayMode = PlayModes.Stop;
				SetIFCustomImage(BUTTON_PAUSE);
				// Inject event to web view
				if( mNodeId.equals("0") )
				{
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.stop',true,true);document.dispatchEvent(ev);");
					
					appMobiActivity.trackPageView("/appMobi.shoutcast." + mShoutUrl + ".stop");
					
				}
				else
				{
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.stop',true,true);document.dispatchEvent(ev);");

					appMobiActivity.trackPageView("/appMobi.station." + stationID + ".stop");
					
				}

			} else {
				ReturnMessageTitle = "Problem Pausing Player";
				ReturnMessageBody = "Exception: " + mStr;
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, ReturnMessageTitle + ReturnMessageBody);
				SendGUIUpdateMessage();
				return;
			}

			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}
	};

	
	//Added by Parveen on July 26,2010 - only stop current playing station from service(shoutcast - stop older station before playing new station).
	public void stopStationOnly(){
		if(m_flycast_service_remote!=null) {
			m_flycast_service_remote.StopCurrentStation();
			currentShoutcastURL ="";
			currentStationID = "";
			appMobiActivity.state.playStarted = false;
		}
	}
	public void stopStation(){
		if(m_flycast_service_remote!=null) {
			m_flycast_service_remote.StopCurrentStation();
			appMobiActivity.state.playStarted = false;
			PlayerStop();
		}
	}
	
	// --- btnStopListener -------
	//Added by Parveen on April 01,2010 - Stop player.
	public void PlayerStop()
	{
		try
		{
			appMobiActivity.trackPageView("/appMobi.station." + stationID + ".stop");
			
			if( mNodeId.equals("0") )
				loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.stop',true,true);document.dispatchEvent(ev);");
			else
				loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.stop',true,true);document.dispatchEvent(ev);");
			UIQuitPlay(); // BH_11-16-09 Added

		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
	}

	// --- ----------
	private OnClickListener btnStopListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				// Added by Parveen on May13,2010 - Clear station id here.
				stationID = "";
				appMobiActivity.state.playStarted = false;
				m_flycast_service_remote.StopCurrentStation();
				if( mNodeId.equals("0") )
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.stop',true,true);document.dispatchEvent(ev);");
				else
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.stop',true,true);document.dispatchEvent(ev);");
				UIQuitPlay(); // BH_11-16-09 Added
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}
	};

	// Added by Parveen on May13,2010 - Volume up
	public void VolumeUp() {
		AudioManager mAudioManager = (AudioManager) appMobiActivity .getSystemService(Context.AUDIO_SERVICE);
		int MaxSoundVolume = mAudioManager.getStreamMaxVolume(0x00000003);
		appMobiActivity.CurrentSoundVolume = mAudioManager.getStreamVolume(0x00000003);
		appMobiActivity.CurrentSoundVolume += 1;
		if (appMobiActivity.CurrentSoundVolume > MaxSoundVolume) {
			appMobiActivity.CurrentSoundVolume = MaxSoundVolume;
		}
		mAudioManager.setStreamVolume(0x00000003, appMobiActivity.CurrentSoundVolume, 0);
	}

	// Added by Parveen on May13,2010 - Volume down.
	public void VolumeDown() {
		AudioManager mAudioManager = (AudioManager) appMobiActivity .getSystemService(Context.AUDIO_SERVICE);
		// int MaxSoundVolume = mAudioManager.getStreamMaxVolume(0x00000003);
		appMobiActivity.CurrentSoundVolume = mAudioManager.getStreamVolume(0x00000003);
		appMobiActivity.CurrentSoundVolume -= 1;
		if (appMobiActivity.CurrentSoundVolume < 0) {
			appMobiActivity.CurrentSoundVolume = 0;
		}
		mAudioManager.setStreamVolume(0x00000003, appMobiActivity.CurrentSoundVolume, 0);
	}

	private void UIQuitPlay() {
		// BH_11-16-09 Added following function:
		CurrentPlayMode = PlayModes.Stop;
		// Parveen - To send PlayStation Command again.
		currentStationID = "";
		// Added by Parveen on May13,2010 - Clear station id here.
		stationID = "";

		currentShoutcastURL ="";
		shoutcastURL="";
		
		hidePlayer();
	}

	// --- btnTopListener (SEEK BACK 30 seconds in current track) -------
	private OnClickListener btnTopListener = new OnClickListener() {
		public void onClick(View v) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Transport btnTopListener");
			try {
				m_flycast_service_remote.MediaPlayerSeekBack();
			} catch (Exception e) {
			}
		}
	};
	
	// Do NOT delete
	public void showGoogleAd()
	{
		Intent intent = new Intent(appMobiActivity, GoogleAdActivity.class);
    	try {
    		appMobiActivity.startActivity(intent);
    	} catch (ActivityNotFoundException e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
    	}
	}

	// --- btnNextListener (SEEK FORWARD 30 seconds in current track) -------
	private OnClickListener btnNextListener = new OnClickListener() {
		public void onClick(View v) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Transport btnNextListener");
			try {
				m_flycast_service_remote.MediaPlayerSeekForward();
			} catch (Exception e) {
			}
		}
	};	
	
	public void setTimerImage(){
	   	   if(!appMobiActivity.timerStarted)
			btnTimer.setImageDrawable(appMobiActivity.getResources().getDrawable(R.drawable.timer_button));
	   	   else 
	   		btnTimer.setImageDrawable(appMobiActivity.getResources().getDrawable(R.drawable.timer_button_on));
	}
	
	public void showStopTimerDialog(){
		String remainingTime = m_flycast_service_remote!=null?String.valueOf(m_flycast_service_remote.getTimeRemainingToStopPlayer()):"?";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(appMobiActivity);
		builder.setMessage(remainingTime+" minutes until shutoff. Hit Stop to stop timer.").setCancelable(true)
		       .setPositiveButton("Stop", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               //appMobiActivity.stopTimerThread.stopThread();
		        	   if(m_flycast_service_remote!=null)m_flycast_service_remote.stopTimerThread();
		        	   appMobiActivity.timerStarted = false;
		        	   appMobiActivity.runOnUiThread(
		   					new Runnable() {
		   						public void run() {
		   							setTimerImage();
		   						}
		   		    		});
		           }
		       });
	//	AlertDialog alert = builder.create();
		builder.show();
	}
	
	private OnClickListener btnTimerListener = new OnClickListener() {
		public void onClick(View v) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Transport btnTimerListener");
			
			try {
				if(Debug.isDebuggerConnected()) Log.d("TimerThread","appMobiActivity.timerStarted is.." +appMobiActivity.timerStarted);
				if(appMobiActivity.timerStarted){
					if(Debug.isDebuggerConnected()) Log.d("TimerThread","calling dialog");
					//if player already chosen time show a dialog box otherwise start activity
					showStopTimerDialog();
				}
				else{
					if(Debug.isDebuggerConnected()) Log.d("TimerThread","calling activity");
					Intent intent = new Intent(appMobiActivity, SleepTimerActivity.class);
	            	try {
	            		appMobiActivity.startActivityForResult(intent, AppMobiActivity.CHOOSE_TIMER);
	            	} catch (ActivityNotFoundException e) {
	        			if(Debug.isDebuggerConnected()) {
	        				Log.d("[appMobi]", e.getMessage(), e);
	        			}
	            	}
				}
			} catch (Exception e) {
			}
		}
	};

	// ----------------------------------
	// --- BEGIN BH_01-05-10 ------------
	// ----------------------------------
	public static String STR_FEED_RATING = "&FEED=ADFAV";
	public static String STR_SONG_RATING_BAD = "&TYPE=SONG&ISFAVORITE=0&RATING=";
	public static String STR_SONG_RATING_GOOD = "&TYPE=SONG&ISFAVORITE=1&RATING=";

	// ShowToast(RootContext, "Please wait...", 0x00000001);//
	public void ShowToast(Context context, String text, int duration) {
		// LENGTH_SHORT=0x00000000 , LENGTH_LONG=0x00000001
		Toast.makeText(context, text, duration).show();
		return;
	}

	// ---------------------------------------------------------------
	// -- FlyCast 2.X psuedo "coverflow" ---------
	// public ImageSwitcher mSwitcher;
	// public Gallery mGallery;
	//

	// Boolean ProcessingSwitcherClick = false;
	// -- mSwitcherOnClickListener ---------
	private OnClickListener mSwitcherOnClickListener = new OnClickListener() {

		public void onClick(View v) {

			if(DPApplication.Instance().GetTrackList()!=null && DPApplication.Instance().GetTrackList().shoutcasting)
				return;
			
			Message message = new Message();
			message.what = DISABLE_SWITCHER;
			appMobiActivity.myGUIUpdateHandler.sendMessage(message);

			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "mSwitcherOnClickListener, mGallery.getSelectedItemPosition()="
							+ mGallery.getSelectedItemPosition());
			// SWITCH THE TRACK -OR- Switch To Selected FlyBack

			try {
				int position = mGallery.getSelectedItemPosition();
				// m_flycast_service_remote.SwitchPlayTrack(mGallery.getSelectedItemPosition());
				m_flycast_service_remote.SwitchPlayTrack(position);
				FlyCastPlayer.mSwitcherTracklistPosition = position;
												
				/*
				 * If we are in pause mode and we click on an Image so MediaPlayer will be running and
				 * now we are in Play mode
				 * so the Java Script event on click of it.
				 *
				 */

				if(CurrentPlayMode == PlayModes.Pause){
					if( mNodeId.equals("0") )
						loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.play',true,true);document.dispatchEvent(ev);");
					else
						loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.play',true,true);document.dispatchEvent(ev);");
					setPauseButton();
					EnableCommandButtons();
				}

			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}

			// mSwitcher.setEnabled(true);
			Message msg = new Message();
			msg.what = ENABLE_SWITCHER;
			appMobiActivity.myGUIUpdateHandler.sendMessage(msg);

			try {
				// temp fix to prevent lockup with multiple clicks, needs
				// better solution
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}

	};

	public DPXMLTrack selectedTrack = null;
	public String mSwitcherArtist;
	public String mSwitcherTitle;
	public static int mSwitcherTracklistPosition = 0; // BH_01-08-10 Save latest
	// Viewed Position
	public AdapterView.OnItemSelectedListener mGalleryOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {

		// @Override
		public void onItemSelected(AdapterView<?> arg0, View v, int position,
				long id) {
			String title = "";
			if (UI_tracklistcurrent==null || position > UI_tracklistcurrent.children.size())
				return;
			mSwitcherTracklistPosition = position; // BH_01-08-10 Save latest
			// Viewed Position

			if(Debug.isDebuggerConnected()) Log.d("mSwitcherTracklistPosition", "mSwitcherTracklistPosition is" + mSwitcherTracklistPosition);

			selectedTrack = (DPXMLTrack) UI_tracklistcurrent.children.get(position);// "arg2"
			if (selectedTrack.flyback) {
				Message message = new Message();
				message.what = SET_FLYBACK_IMG;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);
								
				imgLive.setVisibility(View.INVISIBLE); //Added by Parveen on July 23,2010 - Hide live track image in this case.
																							//Fix to issue where is we quickly flick to flyback track from live track .
				mSwitcherArtist = UI_tracklistcurrent.station;
				if (position == 0)
					mSwitcherTitle = "Flyback 120 Minutes";
				else if (position == 1)
					mSwitcherTitle = "Flyback 60 Minutes";
				else
					mSwitcherTitle = "Flyback 30 Minutes";
				title = mSwitcherArtist + " - " + mSwitcherTitle;

				//By Parveen April 01,2010 - Show go to current track.
				SetIFCustomImage(SHOW_CURRENT_TRACK);
				btnShowCurrentTrack.setEnabled(true);
				btnShowCurrentTrack.setAlpha(255);

				// ???? Do following after DP puts in track object ????
				// mSwitcherArtist = tracktemp.title;
				// title = mSwitcherArtist;
			} else {
				mSwitcherArtist = selectedTrack.artist;
				mSwitcherTitle = selectedTrack.title;
				if( mSwitcherArtist == null )
					title = mSwitcherTitle;
				else
					title = mSwitcherArtist + " - " + mSwitcherTitle;
				
				if( selectedTrack.album != null && selectedTrack.album.length() > 0 )
					title += " - " + selectedTrack.album;
				

				// We already know this track is not a Flyback by now
				// If within "Biz Rules" display the way it is already stored in
				// the Track

				if (selectedTrack.flyback == true) {
					Message message = new Message();
					message.what = SET_FLYBACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}
				/*
				 * Narinder 1/28/2010
				 *
				 * if ( (tracktemp.delayed == true) || (tracktemp.listened ==
				 * false) should be checked before if ( (tracktemp.imageurl ==
				 * null) || (tracktemp.imageurl.equals("")) because both of
				 * these conditions are true in the case of advertisement when
				 * tracktemp.delayed is true. if we do not do this initially an
				 * image will be "unknown" and when it comes to focus it becomes
				 * "ArtWork NotAvailable"
				 */
				else if ((selectedTrack.delayed == true)
						|| (selectedTrack.listened == false)) {

					Message message = new Message();
					message.what = SET_UNKNOWN_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
					title = DPStringConstants.STR_UNAVAILABLE;
				}

				else if ((selectedTrack.imageurl == null)
						|| (selectedTrack.imageurl.equals(""))) {

					Message message = new Message();
					message.what = SET_NO_ART_WORK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else if (imageHashTable.size() > 0
						&& imageHashTable.containsKey(selectedTrack.guidSong)) {
					// if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGEROW, "Image already downloaded");
					selectedTrack.imageoriginal = imageHashTable.get(selectedTrack.guidSong);
					selectedTrack.imageoriginaldownloaded = true;
					Message message = new Message();
					message.what = SET_SELECTED_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else if (selectedTrack.imageoriginaldownloaded == true) {
					// if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGEROW, "Image already downloaded");
					Message message = new Message();
					message.what = SET_SELECTED_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else {
					Message message = new Message();
					message.what = SET_TRACK_IMAGES;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}
			}
			guidSongCurrentlyDisplayed = selectedTrack.guidSong;

			if (selectedTrack.cached == true && selectedTrack.bitrate > 0) {
				double time = selectedTrack.length / selectedTrack.bitrate / 128 * 1.023;
				int itime = (int) time;
				int mins = (int) (itime / 60);
				int secs = itime - (mins * 60);
				if (secs < 10) {
					title += " (" + mins + ":0" + secs + ")";
				} else {
					title += " (" + mins + ":" + secs + ")";
				}
			}
			PlayInfo.setText(title); // BH_01-06-10
			playProgressUpdater();
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "mGalleryItemSelectedListener, position=" + position);
		}

		//@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}

	};

	ViewSwitcher.ViewFactory mSwitcherViewFactory = new ViewSwitcher.ViewFactory() {
		//@Override
		public View makeView() {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "mSwitcher makeView");
			ImageView i = new ImageView(appMobiActivity);
			i.setBackgroundColor(0xFF000000);
			i.setScaleType(ImageView.ScaleType.FIT_CENTER);
			i.setLayoutParams(new ImageSwitcher.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			return i;
		}
	};

	public DPXMLTrack tracktemp = null;
	public class Run_mSwitcher_Set_Image implements Runnable {
		public int mPosition;

		public Run_mSwitcher_Set_Image(int position) {
			mPosition = position;
		}

		//@Override
		public void run() {

			if(UI_tracklistcurrent==null)
				UI_tracklistcurrent=DPApplication.Instance().GetTrackList();
			
			if(UI_tracklistcurrent!=null){
			
			tracktemp = (DPXMLTrack) UI_tracklistcurrent.children.get(mPosition);
			if (tracktemp != null) {

				if (tracktemp.guidSong != null && imageHashTable.size() > 0
						&& imageHashTable.containsKey(tracktemp.guidSong)) {
					// if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGEROW, "Image already downloaded");
					tracktemp.imageoriginal = imageHashTable .get(tracktemp.guidSong);
					tracktemp.imageoriginaldownloaded = true;
					Message message = new Message();
					message.what = SET_SELECTED_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else if ((tracktemp.delayed == true) || (tracktemp.listened == false)) {
					Message message = new Message();
					message.what = SET_UNKNOWN_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else if (tracktemp.imageoriginaldownloaded == true) {
					// if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGEROW, "Image already downloaded");
					Message message = new Message();
					message.what = SET_TEMP_TRACK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else if ((tracktemp.imageurl == null) || (tracktemp.imageurl.equals(""))) {
					Message message = new Message();
					message.what = SET_NO_ART_WORK_IMG;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				else {
					Message message = new Message();
					message.what = SET_TRACK_IMAGES;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				Message message = new Message();
				message.what = 600;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);
			}
			guidSongCurrentlyDisplayed = tracktemp.guidSong;
		}
		}
	}

	public class Run_mGallery_Adapter_New_Tracklist implements Runnable {
		public ImageListAdapter mImageListAdapter = null;
		public DPXMLTracklist mDPXMLTracklist = null;

		public Run_mGallery_Adapter_New_Tracklist(ImageListAdapter ila,
				DPXMLTracklist object) {
			try{
			Thread.sleep(500);//was 2000
			}catch(Exception e){

			}
		}

		public void run() {
			try {
				ImageListAdapterCurrent = new ImageListAdapter(appMobiActivity, appMobiActivity.flyCastPlayer,
						UI_tracklistcurrent, UI_trackcurrentindex,
						UI_trackmaxplayed, uiclickhandler);

				Message message = new Message();
				message.what = SET_GALLERY_ADAPTER;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);

				getTracklistImages();

				message = new Message();
				message.what = SET_GALLERY_SELECTED_ITEM_LISTENER;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);



				message = new Message();
				message.what = SET_GALLERY_SELECTION;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				/*
				 * May 07, 210 We do no need this code
				 */

				/*
				 * //Parveen April 07,2010 - Send message to set track list
				 * position with current track. message = new Message();
				 * message.what = GUI_SHOWCURRENTTRACK_GOTO;
				 * appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				 */

				// mGallery.setSelection(UI_trackcurrentindex);//BH old
				// mGallery.setSelection(mSwitcherTracklistPosition); //
				// BH_01-14-10
				// latest
				// Viewed
				// Position
			} catch (Exception ex) {
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
						"Run_mGallery_Adapter_New_Tracklist Exception: "
								+ ex.getMessage());
			}
		}
	}
	public ImageListAdapter adpt =null;
	public DPXMLTracklist mDPXMLTracklist = null;
	public class Run_mGallery_Adapter_Update implements Runnable {
		public ImageListAdapter mImageListAdapter = null;


		public Run_mGallery_Adapter_Update(ImageListAdapter ila,
				DPXMLTracklist object) {
			mImageListAdapter = ila;
			mDPXMLTracklist = object;
		}

		public void run() {
			 adpt = (ImageListAdapter) mGallery.getAdapter();
			//adpt.updateData(mDPXMLTracklist);
			Message message = new Message();
			message.what = UPDATE_IMAGE_DATA;
			appMobiActivity.myGUIUpdateHandler.sendMessage(message);


		}
	}

	String TargetType = "";
	String TargetAdImg = ""; // adimg
	String TargetAdUrl = ""; // adurl
	String TransitionAdUrl = "";//

	boolean enter = false;

	public class Run_showalert_dialog implements Runnable {
		public String m_title = null;
		public String m_message = null;

		public Run_showalert_dialog(String title, String message) {
			m_title = title;
			m_message = message;
		}

		public void run() {
			ShowAlertDialog(m_title, m_message);
		}
	}

	private String XMLObjectTempGet(String mUrl) {
		String RetString = "";
		try {
			HttpGet getMethod = new HttpGet(mUrl); // Prepare a request object
			getMethod.addHeader("User-Agent", "FlyCast/"
					+ mPackageInfo.versionName + " (Android; ["
					+ android.os.Build.MODEL + "] [Build "
					+ android.os.Build.VERSION.RELEASE + "])");// BH_02-23-09

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			mClient = new DefaultHttpClient();
			String responseBody = mClient.execute(getMethod, responseHandler);

			mParser = new XMLParser(responseBody);
			mParser.parse();
			// result is in parser.directory -- null if no top level <DIR> xml
			// node
			// if ((mParser.directory ==
			// null)||(mParser.directory.children.size()!=1)) {
			if ((mParser.directory == null)) {
				// Actually some situations null directory is LEGAL, not an
				// error
			}
			XMLObjectTemp = (XMLObject) mParser.directory;
			mParser = null; // BH_11-04-08 ???
			RetString = ""; // No error return
		} catch (Exception ex) {
			if (ex.getMessage() == null)
				RetString = "Error 1330 unknown exception";
			else
				RetString = ex.getMessage();
		}
		return RetString;
	}

	// ==========================================================
	// BEGIN AppStartLooper
	// local function to seee if we should BAIL
	private boolean CheckAppStartBailout() {
		if (AppStartBailout == false) {
			return false;
		} else {
			ReturnMessageTitle = "Application Start Cancelled";
			ReturnMessageBody = "Application has been cancelled";
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageBody=" + ReturnMessageBody);
			SendGUIUpdateMessage();
			return true;
		}
	}

	public boolean AppStartBailout = false;
	public boolean AppStartUpgradeAvailable = false;
	protected static final int APP_START_BAIL = 110001;

	public class AppStartLooper extends Thread {
		public Handler mHandler;
		public Looper mLooper;
		public MessageQueue mMessageQueue;
		public AppStart mAppStart;

		public void run() {
			registerReceiver();
			Looper.prepare();
			mLooper = Looper.myLooper();
			mMessageQueue = Looper.myQueue();
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					switch (msg.what) { // msg.arg1
					case APP_START_BAIL:
						AppStartBailout = true;
						mAppStartLooper.mLooper.quit();
						Message message = new Message();
						message.what = GUI_CLOSE_APP;
						SendGUIUpdateMessage(message);
						break;
					}
				}
			};

			mAppStart = new AppStart();
			mAppStart.run();
			Looper.loop();

		}
	}

	// -- AppStart Runnable ---
	class AppStart implements Runnable {

		public void run() {
			// --
			XMLObject ChildXMLObjectTemp;
			XMLNode ChildXMLNodeTemp;
			try {
				WaitingForAppStart = true;

				if (CheckAppStartBailout() == true) {
					return;
				}
				// SharedPreferences - all activities in THIS application
				// Try to fetch UID from SharedPreferences to mUID

				FlyTunesBaseUrl = appMobiActivity.getString(R.string.FlyCastBaseUrl);

				int i;
				for (i = 0; i < 3; i++) {
					//
					ReturnString = XMLObjectTempGet("http://flycast.fm//External//ClientServices.aspx");

					/*ReturnString = XMLObjectTempGet(FlyTunesBaseUrl
							+ "//external//clientservices.aspx"
							+ BaseQueryString + "&FEED=START");*/
					if (ReturnString == "") {
						break;
					}
					if (CheckAppStartBailout() == true) {
						return;
					}
					Thread.sleep(2000);
				}

				if (ReturnString != "") {
					ReturnMessageTitle = "Communication Error";
					ReturnMessageBody = "Client Services Error: "
							+ ReturnString;
					if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageTitle="
							+ ReturnMessageTitle);
					SendGUIUpdateMessage();
					return; // uncommented this line BH_02-03-09
				}

				// if XMLObjectTemp **IS** null, go load layout directly, no
				// need to fool around with the browser
				// if XMLObjectTemp is **NOT** null, extract the URL and start
				// it up for intervention
				if (XMLObjectTemp != null) {
					// -- There is an Url to load, go find it and do it
					// ------------
					ChildXMLObjectTemp = (XMLObject) XMLObjectTemp.children
							.elementAt(0);
					if (ChildXMLObjectTemp.type == XMLObject.NODE) {
						ChildXMLNodeTemp = (XMLNode) XMLObjectTemp.children
								.get(0);
						ReturnString = ChildXMLNodeTemp.nodeurl;
					} else {
						ReturnMessageTitle = "Communication Error";
						ReturnMessageBody = "Client Services illegal object type";
						if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageTitle="
								+ ReturnMessageTitle);
						SendGUIUpdateMessage();
					}

					if (ReturnString.indexOf("?") == -1) {
						ReturnString = ReturnString + "?";
					} else {
						ReturnString = ReturnString + "&";
					}
					if (CheckAppStartBailout() == true) {
						return;
					}
					/*browserFull.loadUrl(FlyTunesBaseUrl + ReturnString + "UID="
							+ mUID);*/
				/*	Message message = new Message();
					message.what = GUI_SHOW_LAYOUTBROWSERONLY;
					SendGUIUpdateMessage(message)*/;// Send a message to the
													// handler
				} else {
					if (CheckAppStartBailout() == true) {
						return;
					}
					Message message = new Message();
					message.what = GUI_SHOW_LAYOUTMAIN;
					SendGUIUpdateMessage(message);// Send a message to the
													// handler
				}
			} catch (Exception ex) {
				if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageBody=" + ex.getMessage());
				/*ReturnMessageTitle = "Problem Starting FlyCast";
				ReturnMessageBody = ex.getMessage();
				SendGUIUpdateMessage();*/
				WaitingForAppStart = false;
				// ProgressDialogClose();
			}

			WaitingForAppStart = false;

			if (!wasServiceRunningOnCreate) {
				init();

				if(appMobiActivity.state.showPlayer){
					doOnResumeStuff();
				}

			} else {
				try {
					PlayStationNameCurrent = m_flycast_service_remote
							.getCurrentPlayingStation();
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) Log.e("ERROR", e.getMessage());
				}
				appMobiActivity.runOnUiThread(new Runnable() {
					public void run() {
						init();
						if(appMobiActivity.state.showPlayer){
							doOnResumeStuff();
						}
					}
				});
			}

			return;
		}
	}

	public String FlyCastServiceInit() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "PlayerAppMobiLatest::FlyCastServiceInit");
		try {
			if (!wasServiceRunning())
			{				
				Class<?> serviceClass = null;
				try {
					serviceClass = Class.forName(appMobiActivity.getString(R.string.remote_service_action));
				} catch( Exception e ) { }
				appMobiActivity.startService(new Intent(appMobiActivity, serviceClass));
			}
			BindFlyCastServiceRemote();
			/*
			 * int tries = 0; while( tries < 30 && !flycastserviceready ) {
			 * if(Debug.isDebuggerConnected()) Log.d(TAG_APPSTART, "waiting again"); try { Thread.sleep(250); }
			 * catch (Exception ex) { } tries++; } if( tries < 30 )
			 * if(Debug.isDebuggerConnected()) Log.d(TAG_APPSTART, "everyone is ready"); else
			 * if(Debug.isDebuggerConnected()) Log.d(TAG_APPSTART, "slow going");
			 */
			wasServiceRunningOnCreate = true;
			return "";
		} catch (Exception ex) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "FlyCastServiceInit Exception:"
					+ ex.getMessage());
			return "FlyCastServiceInit Exception:" + ex.getMessage();
		}
	}

	// -------------------------------------------------------------------------
	// ShowAlertDialog - Display dialog, take no particular action
	// -------------------------------------------------------------------------
	public void ShowAlertDialog(String title, String message) {
		//we were showing some inappropriate error messages to users ("Client services error: flycast.fm"), so disabling unless debugger is connected
		if(Debug.isDebuggerConnected()) {
			new AlertDialog.Builder(appMobiActivity).setTitle(title).setMessage(message)
					.setNeutralButton("Close",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dlg, int sumthin) {
									// do nothing – it will close on its own
								}
							}).show();
		}
	}

	// Send General UI Update Message
	public void SendGUIUpdateMessage() {
		// Send a message to the handler
		Message message = new Message();
		message.what = GUIUPDATEIDENTIFIER;
		myGUIUpdateHandler.sendMessage(message);
		return;
	}

	// Overload - pass message parameter
	private void SendGUIUpdateMessage(Message message) {
		myGUIUpdateHandler.sendMessage(message);
		return;
	}

	// Set up a progress dialog
	// protected ProgressDialog mProgressDialog = null;
	// Set up random unique ID's for message handler
	public static final int GUIUPDATEIDENTIFIER = 110001;
	public static final int GUI_UPDATE_SEARCH_CANCELLED = 120001;
	public static final int GUI_UPDATE_FAVORITES_CANCELLED = 120002;
	public static final int GUI_UPDATE_HISTORY_CANCELLED = 120003;
	public static final int GUI_UPDATE_SETTINGS_CANCELLED = 120004;
	public static final int GUI_UPDATE_PLAYSTART_CANCELLED = 120005;
	public static final int GUI_UPDATE_MEDIAPLAYER_ERROR = 120006;
	public static final int GUI_UPDATE_PLAYLIST = 120007;
	public static final int GUI_UPDATE_PLAYLIST_NULL = 120008;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_CONNECTING = 120009;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_CREATING = 120010;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_BUFFERING = 120011;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_CANCELLING = 120012;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_PLAYING = 120013;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_PLAYING_TEXTONLY = 120014;
	public static final int o = 120015;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_SYNCHING_TRACKLIST = 120016;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_PLAY_BUFFER_DELAY = 120017;
	public static final int GUI_UPDATE_PLAY_FAILED = 120018;
	public static final int GUI_UPDATE_ADROTATOR_UPDATE = 120030;
	public static final int GUI_UPDATE_ADROTATOR_HIDE = 120031;
	public static final int GUI_BTN_SHOWCURRENTTRACK_SHOW = 120032;
	public static final int GUI_SHOWCURRENTTRACK_HIDE = 120033;
	public static final int GUI_SHOWCURRENTTRACK_GOTO = 120034;
	public static final int GUI_UPDATE_PLAY_NOTIFICATION_SWITCHING = 120015;
	public static final int GUI_SHOW_LAYOUTMAIN = 130001;
	public static final int GUI_SHOW_LAYOUTBROWSERONLY = 130002;
	public static final int GUI_SHOW_LAYOUTBROWSERONLYSTARTUP = 130003;
	public static final int GUI_SHOW_LAYOUT_UPGRADE = 130004;
	public static final int GUI_CLOSE_APP = 140001; // ShutDown FlyCast

	//Parveen - Added constants for appMobi used in droid gap.
	public static final int TOGGLE_PLAY_PAUSE = 1;
	public static final int PLAY = 2;
	public static final int FORWARD = 3;
	public static final int REWIND = 4;
	public static final int PLAYER_STOP = 5;
	public static final int PLAYER_VOLUME_UP = 6;
	public static final int PLAYER_VOLUME_DOWN = 7;
	public static final int LOAD_URL = 501;

	public static final int UPDATE_IMAGE_DATA= 997;
	public static final int START_STATION= 996;
	public static final int START_SHOUTCAST= 994;
	public static final int SET_TRACK_IMAGES= 990;
	public static final int SHOW_PLAYER= 933;
	public static final int HIDE_PLAYER= 932;

	public static final int SET_SELECTED_TRACK_IMG= 989;
	public static final int SET_NO_ART_WORK_IMG= 988;
	public static final int SET_TEMP_TRACK_IMG= 987;
	public static final int SET_UNKNOWN_TRACK_IMG= 986;
	public static final int SET_FLYBACK_IMG= 984;
	public static final int UPDATE_IMAGE_LIST_ADAPTER= 800;
	public static final int SET_GALLERY_ADAPTER= 801;
	public static final int SET_GALLERY_SELECTION= 802;
	public static final int SET_GALLERY_SELECTED_ITEM_LISTENER= 803;
	public static final int SEEKBAR_GONE_INVISIBLE= 590;
	public static final int SEEKBAR_INVISIBLE= 580;
	public static final int SEEKBAR_GONE= 570;
	public static final int SEEKBAR_BUFFERING_VISIBLE_BUFFERED_GONE= 560;
	public static final int SEEKBAR_BUFFERING_GONE_BUFFERED_VISIBLE= 540;
	public static final int SET_SEEKBAR_DEFAULT_VISIBLITY= 550;
	public static final int SET_SEEKBAR_VISIBLITY =510;
	public static final int SET_IMAGEVIEW_VISIBLE= 530;
	public static final int SET_IMAGEVIEW_INVISIBLE =520;
	public static final int SET_MAIN_LAYOUT =111;
	
	public void showSeekBar(){
		seekbarBuffered = (FlyProgressView)appMobiActivity.findViewById(R.id.seek_buffered);
	}


	public void setPauseButton(){
		CurrentPlayMode = PlayModes.Play;
		if (appMobiActivity.state.showPlayer && btnPauseToggle != null)
			SetIFCustomImage(BUTTON_PAUSE);
	}

	public void playPauseTogle(){
		String mStr = null;
		if( m_flycast_service_remote == null ) return;
		try {
			mStr = m_flycast_service_remote.setMediaPlayerPauseToggle();
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
		if (mStr.equals("play")) {
			setPauseButton();
		} else if (mStr.equals("pause")) {
			CurrentPlayMode = PlayModes.Pause;
			if (appMobiActivity.state.showPlayer && btnPauseToggle != null)
				SetIFCustomImage(BUTTON_PLAY);
		} else if (mStr.equals("stopped")) {
			CurrentPlayMode = PlayModes.Stop;
		} else {
			ReturnMessageTitle = "Problem Pausing Player";
			ReturnMessageBody = "Exception: " + mStr;
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, ReturnMessageTitle + ReturnMessageBody);
			SendGUIUpdateMessage();
			return;
		}
	}
	
	
	private void stationErrorMessage(){
		
		if( mNodeId!=null && mNodeId.equals("0") )
			loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.error',true,true);document.dispatchEvent(ev);");
		else
			loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.error',true,true);document.dispatchEvent(ev);");
	}
	

	public static String stationID;
	public static String currentStationID = "";
	public static String shoutcastURL;
	public static String currentShoutcastURL = "";

	boolean test = false;

	// Create (message) handler object that will handle the messages sent to the
	// UI activity from other threads
	public Handler myGUIUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			Message _msg = new Message();

			switch (msg.what) {

			case MEDIAPLAYER_STOPPED:
	
				appMobiActivity.runOnUiThread(new Runnable() {
					public void run() {
						appMobiActivity.timerStarted = false;
						setTimerImage();
						PlayerStop();		
					}
				});
				
			break;	
			case SEND_TRACK_INFO:

				if(Debug.isDebuggerConnected()) Log.d("WebView", " handle Message in PlayerAppMobiLatest....SEND_TRACK_INFO..calling  appMobiActivity.sendTrackInfoToWebView()");
				appMobiActivity.sendTrackInfoToWebView();
				break;

			case SEND_STATION_ERROR_MESSAGE:
				if(Debug.isDebuggerConnected()) Log.d("WebView", " SEND_STATION_ERROR_MESSAGE");
				stationErrorMessage();
				ShowAlertDialog("Problem Connecting", "This station is temporarily not available. Please verify your internet connection and try again later.");
				
				break;

			case TRACK_UNAVAILABLE:
				    ShowAlertDialog("", "Tracks become available as you listen");
				    break;

			case 4:
				try {
						m_flycast_service_remote.MediaPlayerSeekBack();
					} catch (Exception e) {
						if(Debug.isDebuggerConnected()) {
							Log.d("[appMobi]", e.getMessage(), e);
						}
					}
				break;

			case 3:
				try {
						m_flycast_service_remote.MediaPlayerSeekForward();;
					} catch (Exception e) {
						if(Debug.isDebuggerConnected()) {
							Log.d("[appMobi]", e.getMessage(), e);
						}
					}
				break;

			case 2:
				playPauseTogle();
				break;
			case 1:
				playPauseTogle();
			break;
			case GUIUPDATEIDENTIFIER:
				// ProgressDialogClose();
				if( appMobiActivity.appView != null )
				{
					stationErrorMessage();	
				}
				if (ReturnMessageBody != "") {
					ShowAlertDialog(ReturnMessageTitle, ReturnMessageBody);
				}
				//myview.invalidate();
				break;
			case GUI_SHOW_LAYOUTMAIN:
				// go load the Main application layout
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", e.getMessage(), e);
					}
				}
				// onGuideItemClick(5);

				break;
			case GUI_SHOW_LAYOUTBROWSERONLY:
				// ProgressDialogOpen(PROGRESS_MODE_FIRSTSTARTUP, tmpStr);
				WaitingForFirstStart = true;
				ll_ui_main.setVisibility(View.VISIBLE);
			//	ll_ui_browser_only.setVisibility(View.GONE);
				break;
			case GUI_CLOSE_APP:
				// ProgressDialogClose();
				ShutDownAppMobi();
				break;
			case GUI_UPDATE_PLAYSTART_CANCELLED:
			//	browserPlay.loadUrl("");
				btnCloseAdvert.setVisibility(View.INVISIBLE);
				btnCloseAdvertHybridMode.setVisibility(View.INVISIBLE);
				// ProgressDialogClose();
				ReturnMessageTitle = "Play Cancelled";
				ReturnMessageBody = "Play has been cancelled";
				// if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageBody=" +
				// ReturnMessageBody);
				ShowAlertDialog("", ReturnMessageBody);
				break;
			case GUI_UPDATE_PLAY_FAILED:
				if (ReturnMessageBody != "") {
					ShowAlertDialog(ReturnMessageTitle, ReturnMessageBody);
				}
				stationErrorMessage();
				UIQuitPlay(); // BH_12-04-09 Added
				myview.invalidate();
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_CONNECTING:

				_msg = new Message();
				_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_CONNECTING;
				appMobiActivity.myGUIUpdateHandler.sendMessageAtFrontOfQueue(_msg);
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_CREATING:
				_msg = new Message();
				_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_CREATING;
				appMobiActivity.myGUIUpdateHandler.sendMessage(_msg);
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_BUFFERING:
				_msg = new Message();
				_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_CREATING;
				appMobiActivity.myGUIUpdateHandler.sendMessageAtFrontOfQueue(_msg);
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_CANCELLING:
				_msg = new Message();
				_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_CREATING;
				appMobiActivity.myGUIUpdateHandler.sendMessage(_msg);
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_PLAYING:
				// Update the Player's ImageList //Added BH_11-16-09
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Run_mGallery_Adapter_New_Tracklist GUI_UPDATE_PLAY_NOTIFICATION_PLAYING");

				// By Parveen - execute this is set to visible.
				if (appMobiActivity.state.showPlayer) {
					try{
					ImageListAdapterCurrent = (ImageListAdapter) mGallery .getAdapter();

					try {Thread.sleep(2000); } catch(Exception e){ }

					uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));

					_msg = new Message();
					_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_PLAYING;
					appMobiActivity.myGUIUpdateHandler.sendMessageAtFrontOfQueue(_msg);
					}catch(Exception e){

					}
				}
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_PLAYING_TEXTONLY:
				/*PlayNotification.setText(STR_PLAY_PLAYING + " "
						+ PlayStationNameCurrent);*/
				myview.invalidate();
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_SWITCHING:
				/*
				 * 05/07/2010 Update the image gallery when we manually switch
				 * the tracks.
				 */
				if(appMobiActivity.state.showPlayer){
					try {
					ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
					ImageListAdapterCurrent.lockView();
					uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));
					} catch (Exception ex) {

					}
				}
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_PLAY_BUFFER_DELAY:
				if(appMobiActivity.state.showPlayer){
					_msg = new Message();
					_msg.what = GUI_UPDATE_PLAY_NOTIFICATION_PLAY_BUFFER_DELAY;
					appMobiActivity.myGUIUpdateHandler.sendMessage(_msg);
				}
				/*
				 * PlayNotification.setText(STR_PLAY_BUFFER_DELAY + " " +
				 * PlayStationNameCurrent + PlayNotificationExtra);
				 */
				break;
			case GUI_UPDATE_PLAY_NOTIFICATION_SYNCHING_TRACKLIST:
				if (appMobiActivity.state.showPlayer) {
					_msg = new Message();
					_msg.what = 995;
					appMobiActivity.myGUIUpdateHandler.sendMessage(_msg);
				}
				/*
				 * PlayNotification.setText(STR_PLAY_SYNCHING_TRACKLIST + " " +
				 * PlayStationNameCurrent + PlayNotificationExtra);
				 */
				break;
			case GUI_UPDATE_PLAYLIST:
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
								"Run_mGallery_Adapter_New_Tracklist GUI_UPDATE_PLAYLIST");
				//By Parveen - Don't show images in case no view is required.
				if (appMobiActivity.state.showPlayer) {
					ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
					ImageListAdapterCurrent.lockView();
					uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));
				}
				break;
			case GUI_UPDATE_PLAYLIST_NULL:
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Run_mGallery_Adapter_New_Tracklist GUI_UPDATE_PLAYLIST");
				ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
				ImageListAdapterCurrent.lockView();
				uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, null));
				break;
			case GUI_BTN_SHOWCURRENTTRACK_SHOW:
				btnShowCurrentTrack.setEnabled(true);
				btnShowCurrentTrack.setAlpha(255);
				break;
			case GUI_SHOWCURRENTTRACK_HIDE:
				btnShowCurrentTrack.setEnabled(false);
				btnShowCurrentTrack.setAlpha(128);
			case GUI_SHOWCURRENTTRACK_GOTO:
				Message _message = new Message();
				_message.what = GUI_SHOWCURRENTTRACK_GOTO;
				appMobiActivity.myGUIUpdateHandler.sendMessage(_message);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};

	// ---------------------------------------------------------
	// AdRotatorStart
	enum FooterAdModes {
		none, a_href, googledisplay
	}

	public FooterAdModes CurrentFooterAdMode;

	public boolean AdRotatorRefreshLoopIsActive = false;
	// public adwidgetfreqMilliseconds;
	protected static final int ADROTATOR_START_INIT = 110001;
	protected static final int ADROTATOR_REFRESH_LOOP_START = 110002;

	// protected static final int ADROTATOR_xxx = 110002;

	public AtomicBoolean PlayerWaitingForStart = new AtomicBoolean();// false;
	public AtomicBoolean PlayerStartBailout = new AtomicBoolean(); // = false;
	public AtomicBoolean WaitingForPlayerPrepare = new AtomicBoolean(); // =
																		// false;
	public String RcvValue = "";

	public static final String STR_PLAY_CONNECTING = "Connecting";
	public static final String STR_PLAY_CREATING = "Creating Station";
	public static final String STR_PLAY_BUFFERING = "Buffering";
	public static final String STR_PLAY_PLAYING = "Playing";
	public static final String STR_PLAY_CANCELLING = "Cancelling Play";
	public static final String STR_PLAY_SWITCHING = "Switching Track";
	public static final String STR_PLAY_BUFFER_DELAY = "Network delay, attempting to resume...";
	public static final String STR_PLAY_SYNCHING_TRACKLIST = "Updating Tracklist";

	public static final int PLAY_START_INIT = 110001;
	public static final int PLAY_START_TRACKLIST_NEW = 110002;
	public static final int PLAY_START_TRACK_IS_PLAYING = 110003;
	public static final int PLAY_START_TRACK_IS_SWITCHING = 110004;
	public static final int PLAY_TRACK_PLAY_BUFFER_DELAY = 110005;
	public static final int PLAY_TRACK_WAS_ADDED = 110020;
	public static final int PLAY_START_ERROR = 120001;
	public static final int PLAY_START_BAIL = 120002;
	public static final int PLAYER_PREPARED = 120003;
	public static final int PLAYER_ERROR = 120004;
	public static final int MEDIA_ERROR_UNKNOWN = 0x00000001;
	public static final int MEDIA_ERROR_SERVER_DIED = 0x00000064;

	public void PlayStation(String nodeid, String nodeshout) {
		try {
			// if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "NodeName=" + nodename);
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "NodeID=" + nodeid);
			// if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "NodeInfo=" + nodeinfo);

			mNodeId = nodeid; // mNodeId is a "Global" variable
			mShoutUrl = nodeshout;
			ReturnMessageTitle = "";// Initialize, no error indicated
			ReturnMessageBody = ""; // Initialize, no error indicated
			/*
			 * btnCloseAdvert.setVisibility(View.INVISIBLE);
			 * btnCloseAdvertHybridMode.setVisibility(View.INVISIBLE);
			 */

			CurrentPlayMode = PlayModes.Play; // BH_12-21-09
			btnPauseToggle.setImageResource(R.drawable.pause_button); // added
			// line
			// BH_12-14-09

			// Start with blank Tracklist display
			// UI_trackmaxplayed = 0;
			UI_tracklistcurrent = null;
			UI_trackcurrentindex = 0;
			UI_trackmaxplayed = 0;
			mSwitcherTracklistPosition = 0; // Narinder 01-12-10

			// <<<<<<<<<<<< BH_11-25-09 THE PROBLEM??? >>>>>>>>>>>>>>>>
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
					"Run_mGallery_Adapter_New_Tracklist PlayStation");
			ImageListAdapterCurrent = new ImageListAdapter(appMobiActivity, this,
					UI_tracklistcurrent, UI_trackcurrentindex,
					UI_trackmaxplayed, uiclickhandler);
			//Commented by Parveen on April 02,2010.
			mSwitcher.setBackgroundDrawable((Drawable)GetCustomImage(IMAGE_UNKNOWN));
			guidSongCurrentlyDisplayed = "";

			mGallery.setAdapter(ImageListAdapterCurrent);
			// Insert images into tracks
			getTracklistImages();

			PlayInfo.setText(""); // BH_11-17-09
			PlayStationNameCurrent = "";

			PlayerStartBailout.set(false);
			PlayerWaitingForStart.set(true);
			WaitingForPlayerPrepare.set(false);

			mPlayStartLooper = new PlayStartLooper();
			mPlayStartLooper.start();
			Thread.sleep(200); // wait before sending message to
			// mPlayStartLooper.mHandler, it chokes if we
			// don't

			Message message = new Message();
			message.what = PLAY_START_INIT;
			mPlayStartLooper.mHandler.sendMessage(message);

			// SubPanelModeSet(SubPanelModes.Play, "");
			// btnTopRightClear();
		} catch (Exception ex) {
			ex.getMessage();
		}
	}

	private void PlayStartCancel() {
		// ProgressDialogClose();
		PlayerStartBailout.set(false);
		PlayerWaitingForStart.set(false);
		WaitingForPlayerPrepare.set(false);
		return;
	}

	public class PlayStartLooper extends Thread {
		public Handler mHandler;
		public Looper mLooper;
		public MessageQueue mMessageQueue;
		public PlayStartInit mPlayStartInit;
		public PlayStartTracklistNew mPlayStartTracklistNew;
		public PlayStartTrackIsPlaying mPlayStartTrackIsPlaying;
		public PlayStartTrackIsSwitching mPlayStartTrackIsSwitching;

		public void run() {
			Looper.prepare();
			mLooper = Looper.myLooper();
			mMessageQueue = Looper.myQueue();
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					Message message = new Message();
					switch (msg.what) { // msg.arg1
					case PLAY_START_BAIL: // BAIL OUT from this entire
						// Play_Start
						PlayerStartBailout.set(true); // = true;
						PlayerWaitingForStart.set(false);// = false;
						// ProgressDialogClose();
						try {
							m_flycast_service_remote
									.setMediaPlayer();
						} catch (Exception ex) {
							if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
									"PlayStartBail:Play cancelled resetting media player"
											+ ex.getMessage());
						}

						message.what = GUI_UPDATE_PLAYSTART_CANCELLED;
						myGUIUpdateHandler.sendMessage(message);

						CurrentPlayMode = PlayModes.Stop; // BH_01-29-09
						mLooper.quit();// BH_01-29-09
						break;
					case PLAY_START_ERROR:
						break;
					case PLAY_START_INIT:// Open Progress Dialog, Setup UI, Call
						mPlayStartInit = new PlayStartInit();
						mPlayStartInit.run();
						break;
					case PLAY_START_TRACKLIST_NEW:
						mPlayStartTracklistNew = new PlayStartTracklistNew();
						mPlayStartTracklistNew.run();
						break;
					case PLAY_START_TRACK_IS_PLAYING:
						mPlayStartTrackIsPlaying = new PlayStartTrackIsPlaying();
						mPlayStartTrackIsPlaying.run();
						break;
					case PLAY_START_TRACK_IS_SWITCHING:
						mPlayStartTrackIsSwitching = new PlayStartTrackIsSwitching();
						mPlayStartTrackIsSwitching.run();
						break;
					case PLAY_TRACK_PLAY_BUFFER_DELAY:
						message.what = GUI_UPDATE_PLAY_NOTIFICATION_PLAY_BUFFER_DELAY;
						myGUIUpdateHandler.sendMessage(message);
						break;
					case PLAYER_ERROR:
						// GUI was already messaged in Broadcast Receiver for
						// BROADCAST_MEDIAPLAYER_ERROR
						// Here we will just shut down the Play Mode
						PlayerStartBailout.set(true); // = true;
						PlayerWaitingForStart.set(false);// = false;
						mLooper.quit();
						break;
					}
				}
			};
			Looper.loop();
		}
	}

	// -- Call PlayStationRemote on FlyCastServiceRemote to START the
	// StationPlay process ---------
	public class PlayStartInit implements Runnable {
		public void run() {
			try {
				if (PlayerStartBailout.get() == true) {
					PlayStartCancel();
					return;
				}

				while( !isbServiceConnected() )
				{
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						if(Debug.isDebuggerConnected()) {
							Log.d("[appMobi]", e.getMessage(), e);
						}
					}
				}
				String mStr = "";
				// Added by Parveen on May13,2010 - Don't make call to service
				// remote if Node is is null or "";
				if (mNodeId != null && mNodeId != "" && mNodeId != "0" ) {
					if(appMobiActivity.state.stationResumeMode){
						m_flycast_service_remote.setPlayerIsRecording(true);
					}else{
						m_flycast_service_remote.setPlayerIsRecording(false);
					}
					mStr = m_flycast_service_remote.PlayStationRemote(mNodeId, null, mUID);
				}
				else if (mShoutUrl != null && mShoutUrl != "") {
					mStr = m_flycast_service_remote.PlayStationRemote("0", mShoutUrl, mUID);
				}

				// Modified by Parveen - Set main layout if set to visible.
				if (appMobiActivity.state.showPlayer) {
					Message message = new Message();
					message.what = SET_MAIN_LAYOUT;
					appMobiActivity.nonGUIUpdateHandler.sendMessage(message);
				}

				if (mStr.equals("") != true) {
					PlayStartCancel();
					ReturnMessageTitle = "Problem Starting Remote Player";
					ReturnMessageBody = "PlayStationRemote: " + mStr;
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "PlayStartInit " + mStr);
					SendGUIUpdateMessage();
					return;
				}

				if( mNodeId.equals("0") )
				{
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.start',true,true);document.dispatchEvent(ev);");

					appMobiActivity.trackPageView("/appMobi.shoutcast." + mShoutUrl + ".start");
					
				}
				else
				{
					loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.start',true,true);document.dispatchEvent(ev);");

					appMobiActivity.trackPageView("/appMobi.station." + stationID + ".start");
				}

				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "1-PlayStartInit Complete");
			} catch (Exception ex) {
				//ReturnMessageTitle = "PlayStartInit Exception";
				//ReturnMessageBody = PLAY_ERROR_BODY + (char) 10 + (char) 10 + "Exception: " + ex.getMessage();
				//SendGUIUpdateMessage();
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, ReturnMessageTitle + "Exception: " + ex.getMessage());
				PlayStartCancel();
				return;
			}
		} // public void run()
	}

	// --- DeviceProxy has returned the INITIAL Tacklist ---------
	public class PlayStartTracklistNew implements Runnable {
		DPXMLParser PSTNParser = null; // Parses XML from DeviceProxy

		public void run() {
			try {
				if (PlayerStartBailout.get() == true) {
					PlayStartCancel();
					return;
				}

				try {
					UI_tracklistcurrent = DPApplication.Instance().GetTrackList();
				} catch (Exception ex) {
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,"2-PlayStartTracklistNew EXCEPTION in XML"+ ex.getMessage());
				}
				if (UI_tracklistcurrent != null && UI_tracklistcurrent.children.size() > 0) {

					UI_trackcurrentindex = UI_tracklistcurrent.startindex; // currently playing track
						UI_trackmaxplayed = UI_tracklistcurrent.startindex; // highest  track  that has been played
						mSwitcherTracklistPosition =  UI_tracklistcurrent.startindex;
						UI_trackcurrent = (DPXMLTrack) UI_tracklistcurrent.children.elementAt(UI_trackcurrentindex);
						PlayguidSong = UI_trackcurrent.guidSong;

					int index;
					for (index = 0; index < UI_tracklistcurrent.children.size(); index++) {
						DPXMLTrack tracktemp = (DPXMLTrack) UI_tracklistcurrent.children.get(index);

						if (tracktemp.guidSong != null && imageHashTable.size() > 0 && imageHashTable.containsKey(tracktemp.guidSong)) {
							tracktemp.imageoriginal = imageHashTable.get(tracktemp.guidSong);
							tracktemp.imageoriginaldownloaded = true;
							if(Debug.isDebuggerConnected()) Log.d("Table", "Get " + "guid is " + tracktemp.guidSong);
						} else if (tracktemp.imageoriginaldownloaded != true) {
							tracktemp.imageoriginal = (Drawable) appMobiActivity.getResources().getDrawable(R.drawable.retrieving_data);
							tracktemp.imageoriginaldownloaded = false;
						}
					}
					Message message = new Message();
					message.what = GUI_UPDATE_PLAY_NOTIFICATION_BUFFERING;
					myGUIUpdateHandler.sendMessage(message);

					// BH_11-29-09 New Way
					message = new Message();
					message.what = GUI_UPDATE_PLAYLIST;
					myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);

					// BH_11-29-09 New Way
					Thread.sleep(3000); // G1 still hoses at 1000
					uiclickhandler.post(new Run_mSwitcher_Set_Image(
							UI_trackcurrentindex));
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "2-PlayStartTracklistNew Complete");

				}
			} catch (Exception ex) {
				ReturnMessageTitle = "PlayStartTracklistNew Exception";
				ReturnMessageBody = PLAY_ERROR_BODY + (char) 10 + (char) 10
						+ "Exception: " + ex.getMessage();
				SendGUIUpdateMessage();
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, ReturnMessageTitle + "Exception: "
						+ ex.getMessage());
				PlayStartCancel();
				return;
			}
		} 
	}

	// --- PlayStartTrackIsPlaying ---------
	class PlayStartTrackIsPlaying implements Runnable {
		public void run() {
			Message message = new Message();
			message.what = GUI_UPDATE_PLAY_NOTIFICATION_PLAYING;
			myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			CurrentPlayMode = PlayModes.Play;
			return;
		} // public void run() {
	}

	// --- PlayStartTrackIsSwitching ---------
	class PlayStartTrackIsSwitching implements Runnable {
		public void run() {
			Message message = new Message();
			message.what = GUI_UPDATE_PLAY_NOTIFICATION_SWITCHING;
			myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			return;
		} 
	}

	public void doOnResumeStuff() {
		// --- BEGIN BH_11-18-09 restore synch with RemoteService ------
		String mPlayMode = null;
		try {
		
			// if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume GUI_UPDATE_PLAYLIST");
			ImageListAdapterCurrent = new ImageListAdapter(appMobiActivity, this,
					UI_tracklistcurrent, UI_trackcurrentindex,
					UI_trackmaxplayed, uiclickhandler);
			guidSongCurrentlyDisplayed = "";

			getTracklistImages();

			mPlayMode = m_flycast_service_remote.getSVC_CurrentPlayMode();
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume mPlayMode " + mPlayMode);
			if (mPlayMode.equals(PlayModes.Play.toString())) {
				CurrentPlayMode = PlayModes.Play;
				SetIFCustomImage(BUTTON_PAUSE);

																	// BH_12-14-09
			} else if (mPlayMode.equals(PlayModes.Pause.toString())) {
				CurrentPlayMode = PlayModes.Pause;
																	// BH_12-14-09
			} else {
				CurrentPlayMode = PlayModes.Stop;
																	// BH_12-14-09
			}
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume CurrentPlayMode " + CurrentPlayMode);

			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume trackcurrentindex="+ UI_trackcurrentindex);

		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume UpdateTracklist Exception "+ e.getMessage());
		}
		try {
			if ((CurrentPlayMode == PlayModes.Play) || (CurrentPlayMode == PlayModes.Pause)) {
				// Insert images into tracks
				getTracklistImages();

				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Run_mGallery_Adapter_New_Tracklist onResume");
				ImageListAdapterCurrent = (ImageListAdapter) mGallery .getAdapter();
				ImageListAdapterCurrent.lockView();
				uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(
						ImageListAdapterCurrent, UI_tracklistcurrent));
				// Thread.sleep(1000);
				Message message = new Message();

				message.what = GUI_UPDATE_PLAY_NOTIFICATION_PLAYING;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);
			} else if (CurrentPlayMode == PlayModes.Stop) {
			}

		} catch (Exception ex) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "onResume Exception: " + ex.getMessage());
		}
	}

	public void registerReceiver(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_MEDIAPLAYER_PREPARED);
		filter.addAction(BROADCAST_MEDIAPLAYER_ERROR);
		filter.addAction(BROADCAST_TRACKLIST_NEW);
		filter.addAction(BROADCAST_DEVICEPROXY_MESSAGELIST);
		filter.addAction(BROADCAST_FLYBACK);
		filter.addAction(BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE); // BH_11-18-09
		filter.addAction(BROADCAST_TRACK_IS_PLAYING);
		filter.addAction(BROADCAST_TRACK_IS_SWITCHING); // BH_11-16-09
		filter.addAction(BROADCAST_TRACK_PLAY_BUFFER_DELAY); // BH_12-02-09
		filter.addAction(BROADCAST_PLAY_CANCELLED);
		filter.addAction(BROADCAST_UI_UPDATE_MESSAGE);
		filter.addAction(BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED); // BH_12-04-09
		filter.addAction(BROADCAST_UI_PLAYSTARTPROGRESS_OPEN);
		filter.addAction(BROADCAST_UI_PLAYSTARTPROGRESS_CLOSE);
		filter.addAction(TRACK_NOT_AVAILABLE);

		filter.addAction(SEND_TRACK_INFO_TO_WEBVIEW);
		filter.addAction(SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW);
		
		//To hide the player view when we stop the media player with Timer Thread
		filter.addAction(MEDIAPLAYER_STOPPED_WITH_TIMER_THREAD);
		appMobiActivity.registerReceiver(mReceiver, filter); // BH_02-16-09
		if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onResume PlayUrlCurrent=" + PlayUrlCurrent);
	}

	public Hashtable<String,Drawable> cachedDefaultImages = new Hashtable<String,Drawable> ();

	public static String LOADING_IMAGE="Loading";
	public static String NO_ARTWORK_IMAGE="NoArtwork";
	public static String UNKNOWN_IMAGE= "Unknown"; 
	public static String GO_BACK_IMAGE= "GoBack";
	public static String GO_LIVE_IMAGE= "GoLive";
	
	public Drawable GetCustomImage(int imgValue)
	{
		File appMobiCache = appMobiActivity.appDir();
		Drawable objReturnDrawable = null;
		switch(imgValue)
		{
		case IMAGE_LOADING:
			
			objReturnDrawable = cachedDefaultImages.get(LOADING_IMAGE); 
			if(objReturnDrawable==null){
				File imgLoadingFile = new File(appMobiCache, "_appMobi/loading.png");
				if(imgLoadingFile.exists())
				{
					objReturnDrawable = Drawable.createFromPath(imgLoadingFile.getAbsolutePath());
				}
				else
				{
					objReturnDrawable = appMobiActivity.getResources().getDrawable(R.drawable.loading);
				}
				cachedDefaultImages.put(LOADING_IMAGE, objReturnDrawable);
			}
			break;

		case IMAGE_NO_ARTWORK:
			objReturnDrawable = cachedDefaultImages.get(NO_ARTWORK_IMAGE);
			if(objReturnDrawable ==null){
				File imgNoArtWorkFile = new File(appMobiCache, "_appMobi/artwork_unavailable.png");
				if(imgNoArtWorkFile.exists())
				{
					objReturnDrawable = Drawable.createFromPath(imgNoArtWorkFile.getAbsolutePath());
				}
				else
				{
					objReturnDrawable = appMobiActivity.getResources().getDrawable(R.drawable.artwork_unavailable);
				}
				cachedDefaultImages.put(NO_ARTWORK_IMAGE, objReturnDrawable);
			}
			break;

		case IMAGE_UNKNOWN:
			objReturnDrawable = cachedDefaultImages.get(UNKNOWN_IMAGE);
			if(objReturnDrawable ==null){
				File imgUnknownFile = new File(appMobiCache, "_appMobi/retrieving_data.png");
				if(imgUnknownFile.exists())
				{
					objReturnDrawable = Drawable.createFromPath(imgUnknownFile.getAbsolutePath());
				}
				else
				{
					objReturnDrawable = appMobiActivity.getResources().getDrawable(R.drawable.retrieving_data);
				}
				cachedDefaultImages.put(UNKNOWN_IMAGE, objReturnDrawable);
			}
			break;

		case IMAGE_GO_BACK:
			objReturnDrawable = cachedDefaultImages.get(GO_BACK_IMAGE);
			if(objReturnDrawable ==null){
			File imgBackFile = new File(appMobiCache, "_appMobi/go_back.png");
			if(imgBackFile.exists())
			{
				objReturnDrawable = Drawable.createFromPath(imgBackFile.getAbsolutePath());
			}
			else
			{
				objReturnDrawable = appMobiActivity.getResources().getDrawable(R.drawable.go_back);
			}
			cachedDefaultImages.put(GO_BACK_IMAGE, objReturnDrawable);
			}
			break;

		case IMAGE_GO_LIVE:
			objReturnDrawable = cachedDefaultImages.get(GO_LIVE_IMAGE);
			if(objReturnDrawable ==null){

			File imgLiveFile = new File(appMobiCache, "_appMobi/go_live.png");
			if(imgLiveFile.exists())
			{
				objReturnDrawable = Drawable.createFromPath(imgLiveFile.getAbsolutePath());
			}
			else
			{
				objReturnDrawable = appMobiActivity.getResources().getDrawable(R.drawable.go_live);
			}
			cachedDefaultImages.put(GO_LIVE_IMAGE, objReturnDrawable);
			}
			break;
		}
		return objReturnDrawable;
	}

	private void getTracklistImages() {
		if (UI_tracklistcurrent == null)
			return;
		try {
			Vector<DPXMLObject> children = (Vector<DPXMLObject>) UI_tracklistcurrent.children;
			int size = children.size();

			for (int index = 0; index < size; index++) {
				DPXMLTrack tracktemp = (DPXMLTrack) children.get(index);

				if (tracktemp.listened && tracktemp.guidSong != null) {
					if(Debug.isDebuggerConnected()) Log.d("ListenedTrack", tracktemp.guidSong);
				}
	
				if (index == this.UI_trackcurrentindex && !tracktemp.flyback) {

					/*
					 * Narinder 02/01/2010 if we have listened a track it means
					 * we have downloaded its image previously fetch this image
					 * from HashTable
					 */

					if (imageHashTable.size() > 0
							&& imageHashTable.containsKey(tracktemp.guidSong)) {
						tracktemp.imageoriginal = imageHashTable
								.get(tracktemp.guidSong);
						if(Debug.isDebuggerConnected()) Log.d("Table", "Get " + "guid is " + tracktemp.guidSong);
						tracktemp.imageoriginaldownloaded = true;
					}
					else {
						uiclickhandler.post(new CellImageDownloader(tracktemp));
					}
				}
				else {
					//We can also check if tracktemp.flyback is false here 
					if (tracktemp.guidSong != null && imageHashTable.size() > 0
							&& imageHashTable.containsKey(tracktemp.guidSong)) {
						tracktemp.imageoriginal = imageHashTable
								.get(tracktemp.guidSong);
						tracktemp.imageoriginaldownloaded = true;
					}
					else if (tracktemp.imageurl != null
							&& !tracktemp.imageoriginaldownloaded
							&& !tracktemp.delayed) {
						new Thread(new CellImageDownloader(tracktemp)).start();
					}
				}

			}
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) Log.e("ERROR", "something bad happened", e);
		}
	}

	/**********************************************************************************
	 * //=======================================================================
	 * ====== // FlyCast LifeCyle Debuging Revised ~11-20-16-09
	 * //===============
	 * ==============================================================
	 *
	 * Monitoring LogCat Filter "LifeCycle"
	 *
	 * Launch Flycast onCreate onStart onresume PlayUrlCurrent = NULL <Guide Now
	 * Displayed>
	 *
	 * Start Station Play PlayUrlCurrent = http://...
	 *
	 * Screen Timeout (Goes Black) (11-20-09) onSaveInstanceState onPause
	 *
	 * Wakeup from black screen (by pressing MENU) (11-20-09) onResume onResume
	 * PlayUrlCurrent = http://...
	 *
	 * Click on Ad (launches new browser with ad, pushes **FlyCast Player
	 * Activity** to activity stack) onSaveInstanceState onPause onstop <Audio
	 * *MAY* stop depending on system load, if so we(FlyCast Player Activity)
	 * are now dead>
	 *
	 * Kill Ad display browser (by pressing BACK key) FlyCast Resumes (popped
	 * from activity stack) onCreate onStart onRestoreInstanceState onresume
	 * PlayUrlCurrent = http://...
	 *******************************************************************************/
	DPXMLTrack lookUpTrack = null;
	// --------------------------------------------------------------------------
	// BroadcastReceiver -------------------------------------------------------
	// --------------------------------------------------------------------------
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {

			if(mPlayStartLooper==null){
				if(Debug.isDebuggerConnected()) Log.d("StartService", "mPlayStartLooper initialized in BroadcastReceiver mReceiver");
				mPlayStartLooper = new PlayStartLooper();
				mPlayStartLooper.start();
				try {
					Thread.sleep(200);
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", e.getMessage(), e);
					}
				} // wait before sending message to
				// mPlayStartLooper.mHandler, it chokes if we
				// don't
			}

			String RcvAction = intent.getAction();
			if(Debug.isDebuggerConnected()) Log.d("PlayerRcvAction", RcvAction);
			if (RcvAction.equals(BROADCAST_MEDIAPLAYER_PREPARED)) {
				Message message = new Message();
				message.what = PLAYER_PREPARED;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);
				WaitingForPlayerPrepare.set(false);
			}
			else if(RcvAction.equals(MEDIAPLAYER_STOPPED_WITH_TIMER_THREAD)){
				Message message = new Message();
				message.what = MEDIAPLAYER_STOPPED;
				FlyCastPlayer.this.myGUIUpdateHandler.sendMessage(message);
			}
			
			else if (RcvAction.equals(TRACK_NOT_AVAILABLE)) {
				Message message = new Message();
		    	message.what = FlyCastPlayer.TRACK_UNAVAILABLE;
		    	FlyCastPlayer.this.myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			}

			else if (RcvAction.equals(SEND_TRACK_INFO_TO_WEBVIEW)) {
				Message message = new Message();
		    	message.what = FlyCastPlayer.SEND_TRACK_INFO;
		    	FlyCastPlayer.this.myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			}

			else if (RcvAction.equals(SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW)) {
				Message message = new Message();
		    	message.what = FlyCastPlayer.SEND_STATION_ERROR_MESSAGE;
		    	FlyCastPlayer.this.myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			}

			else if (RcvAction.equals(BROADCAST_MEDIAPLAYER_ERROR)) {
				Bundle mBundle = intent.getExtras();
				RcvValue = (String) mBundle.get(BROADCAST_MEDIAPLAYER_ERRORKEY);
				Message message = new Message();
				message.what = PLAYER_ERROR;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);

				message = new Message();
				ReturnMessageTitle = "Media Player Error";
				ReturnMessageBody = RcvValue;
				message.what = GUIUPDATEIDENTIFIER;
				myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);

			} else if (RcvAction.equals(BROADCAST_PLAY_CANCELLED)) {
				Message message = new Message();
				message.what = GUI_UPDATE_PLAYSTART_CANCELLED;
				myGUIUpdateHandler.sendMessageAtFrontOfQueue(message);
			} else if (RcvAction.equals(BROADCAST_UI_UPDATE_MESSAGE)) {
				Bundle mBundle = intent.getExtras();
				ReturnMessageTitle = (String) mBundle
						.get(BROADCAST_UI_UPDATE_MESSAGE_TITLE_KEY);
				ReturnMessageBody = (String) mBundle
						.get(BROADCAST_UI_UPDATE_MESSAGE_BODY_KEY);
				SendGUIUpdateMessage();
				if(Debug.isDebuggerConnected()) Log.d("BroadCast", "Receive command  "
						+ BROADCAST_UI_UPDATE_MESSAGE + UI_trackcurrentindex);
			} else if (RcvAction.equals(BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED)) {
				Bundle mBundle = intent.getExtras();
				ReturnMessageTitle = (String) mBundle
						.get(BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_TITLE_KEY);
				ReturnMessageBody = (String) mBundle
						.get(BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_BODY_KEY);

				Message message = new Message();
				message.what = GUI_UPDATE_PLAY_FAILED;
				myGUIUpdateHandler.sendMessage(message);

			} else if (RcvAction.equals(BROADCAST_UI_PLAYSTARTPROGRESS_OPEN)) {
				// ProgressDialogOpen(PROGRESS_MODE_PLAYSTART,
				// PROGRESS_TEXT_PLAYSTART);
			} else if (RcvAction.equals(BROADCAST_UI_PLAYSTARTPROGRESS_CLOSE)) {
				// ProgressDialogClose();
			} else if (RcvAction.equals(BROADCAST_TRACKLIST_NEW)) {
				Bundle mBundle = intent.getExtras();
				DeviceProxyXMLStringCurrent = (String) mBundle
						.get(BROADCAST_TRACKLIST_NEW_BODY_KEY);
				Message message = new Message();
				message.what = PLAY_START_TRACKLIST_NEW;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);

				if(Debug.isDebuggerConnected()) Log.d("BroadCast", "Receive command  "
						+ BROADCAST_TRACKLIST_NEW + UI_trackcurrentindex);
			} else if (RcvAction.equals(BROADCAST_TRACK_IS_PLAYING)) {
				// 13 May, Parveen. This condition will enable
				appMobiActivity.state.playStarted = true;
				if(Debug.isDebuggerConnected()) Log.d("StartService", "BROADCAST_TRACK_IS_PLAYING  appMobiActivity.state.playStarted is "+appMobiActivity.state.playStarted);
				if(appMobiActivity.state.showPlayer){
					EnableCommandButtons();
				}

				Bundle mBundle = intent.getExtras();
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Run_mGallery_Adapter_New_Tracklist");
				UI_trackcurrentindex = Integer.parseInt((String) mBundle
						.get(BROADCAST_TRACK_IS_PLAYING_TRACKCURRENTINDEX_KEY));
				if (UI_trackcurrentindex > UI_trackmaxplayed) {
					UI_trackmaxplayed = UI_trackcurrentindex;
				}
				PlayguidSong = (String) mBundle
						.get(BROADCAST_TRACK_IS_PLAYING_GUIDSONG_KEY);
				Message message = new Message();
				message.what = PLAY_START_TRACK_IS_PLAYING;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
						"BROADCAST_TRACK_IS_PLAYING, UI_trackcurrentindex="
								+ UI_trackcurrentindex);

				if(Debug.isDebuggerConnected()) Log.d("BroadCast", "Receive command  "
						+ BROADCAST_TRACK_IS_PLAYING + UI_trackcurrentindex);
			} else if (RcvAction.equals(BROADCAST_TRACK_IS_SWITCHING)) {

				Bundle mBundle = intent.getExtras();

				/*
				 * 05/07/2010 Get UI_trackcurrentindex here so that we can
				 * immediately switch to next track in case of manual switch
				 */
				UI_trackcurrentindex = Integer.parseInt((String) mBundle
						.get(BROADCAST_TRACK_IS_SWITCHING_BODY_KEY));

				Message message = new Message();
				message.what = FlyCastPlayer.PLAY_START_TRACK_IS_SWITCHING;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);

				if(Debug.isDebuggerConnected()) Log.d("BroadCast", "Receive command  "
						+ BROADCAST_TRACK_IS_SWITCHING + UI_trackcurrentindex);

			} else if (RcvAction.equals(BROADCAST_TRACK_PLAY_BUFFER_DELAY)) {
				Message message = new Message();
				message.what = PLAY_TRACK_PLAY_BUFFER_DELAY;
				mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);
			} else if (RcvAction
					.equals(BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE)) {
				Bundle mBundle = intent.getExtras();
				PlayStationNameCurrent = (String) mBundle
						.get(BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE_BODY_KEY);
			} else if (RcvAction.equals(BROADCAST_DEVICEPROXY_MESSAGELIST)) {
				Bundle mBundle = intent.getExtras();
				String BRXmlStr = (String) mBundle
						.get(BROADCAST_DEVICEPROXY_MESSAGELIST_BODY_KEY);
				// Go Process this messagelist
				if ((BRXmlStr.equals(null)) || (BRXmlStr.equals(""))) {
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, BROADCAST_DEVICEPROXY_MESSAGELIST
							+ ", NO XML");
				} else {
					new Thread(new DPProcessMessageList(BRXmlStr)).start();

				}
				if(Debug.isDebuggerConnected()) Log.d("BroadCast", "Receive command  "
						+ BROADCAST_DEVICEPROXY_MESSAGELIST
						+ UI_trackcurrentindex);
			} else if (RcvAction.equals(BROADCAST_FLYBACK)) {
				int index = intent.getIntExtra(BROADCAST_FLYBACK_BODY_KEY, 0);

				int x = index;
				while (true) {
					DPXMLTrack t = (DPXMLTrack) UI_tracklistcurrent.children
							.elementAt(x);
					if (t.flyback) {
						/*
						 * We should not delete anything now as we are accessing
						 * most recent tracklist from DPApplication.java through
						 * Service
						 */
						// UI_tracklistcurrent.children.remove(x);
						x++;
					} else {
						lookUpTrack = t;
						break;
					}
				}
				FLYBACKING = true;
			}
		}
	};

	// --- Process Messagelist for Status Updates and act accordingly ------
	class DPProcessMessageList implements Runnable {
		String PmlXml;
		DPXMLParser PmlParser;
		String PmlStg;

		public DPProcessMessageList(String xml) {
			PmlXml = xml;
		}

		public void run() {
			//try {
				if ((PmlXml == null) || (PmlXml.equals(""))) {
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList NO XML");
					// DeviceProxy returned NOTHING
					return;
				}
				// if ((tStr.equals("<XML></XML>")!=true)) { }

				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList XML=" + PmlXml);
				PmlParser = new DPXMLParser(PmlXml);
				// if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList Before Parse");
				PmlStg = PmlParser.parse();
				// if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList After Parse");

				if (PmlStg.equals("") != true) {
					// PlayStartCancel();
					ReturnMessageTitle = TAG_DPXMLParser + " Error";
					ReturnMessageBody = PmlStg;
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList Parser error: " + PmlStg);
					SendGUIUpdateMessage();
					return;
				}
				if ((PmlParser.messagelist == null) || (PmlParser.messagelist.children.size() == 0)) {
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList Empty MessageList ");
					return;
				}
				// Process this messagelist
				int i;
				for (i = 0; i < PmlParser.messagelist.children.size(); i++) {
					DPXMLMessage messagecurrent = (DPXMLMessage) PmlParser.messagelist.children.elementAt(i);
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList messagecurrent.name=" + messagecurrent.name);
					if (messagecurrent.name.equals(DPXMLParser.STR_RECORDING_HAS_FINISHED)) {
						// Contains Message Name Only
					}
					// for cashing update to update the time in cover flow
					else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_CACHED)) {
						UI_tracklistcurrent = DPApplication.Instance().GetTrackList();
						if( mGallery != null )
						{
							ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
							ImageListAdapterCurrent.lockView();
							uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));
						}
					}
					// 05/04/2010
					/*
					 * The following condition will give us the most updated
					 * tracklist i.e. once we have track.delayed = false in
					 * DPManager we will get the STR_TRACK_HAS_UPDATED message
					 * and hence the most updated tracklist
					 */
					else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_HAS_UPDATED)) {

						UI_tracklistcurrent =DPApplication.Instance().GetTrackList();
				
						if( mGallery != null )
						{
							ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
							ImageListAdapterCurrent.lockView();
							uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));
						}

					} else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_WAS_ADDED)) {
						// Contains Message Name, Track
						DPXMLTrack tracknew = (DPXMLTrack) messagecurrent.track;
						if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "DPProcessMessageList TRACK_WAS_ADDED NewGuid=" + tracknew.guidSong);

						if ((tracknew != null)) {
								UI_tracklistcurrent = DPApplication.Instance().GetTrackList(); 
								if (tracknew.IndexInList > 2) {
									if(Debug.isDebuggerConnected()) Log.d("UITracklist", tracknew.guidSong + " .." + tracknew.delayed);
								}
								if (FLYBACKING && tracknew.guidIndex.equalsIgnoreCase(lookUpTrack.guidSong)) {
									FLYBACKING = false;
								}
								if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "Run_mGallery_Adapter_New_Tracklist");

							if(DPApplication.Instance().GetTrackList().shoutcasting){
								appMobiActivity.runOnUiThread(new Runnable() {
									public void run() {
										appMobiActivity.sendTrackInfoToWebView();
									}
								});
							}
							
							if( mGallery != null )
							{
								ImageListAdapterCurrent = (ImageListAdapter) mGallery.getAdapter();
								ImageListAdapterCurrent.lockView();
								uiclickhandler.post(new Run_mGallery_Adapter_New_Tracklist(ImageListAdapterCurrent, UI_tracklistcurrent));
							}
							
						}
					}
				} 
					return;
		}
	} 

	public void playProgressUpdater(){
		//if(Debug.isDebuggerConnected()) Log.d("FlyCastPlayer","playProgressUpdater");
		if (m_flycast_service_remote != null && mRemoteIsBound
				&& this.guidSongCurrentlyDisplayed != null
				&& !"".equals(this.guidSongCurrentlyDisplayed)
				&& this.CurrentSubPanelMode == SubPanelModes.Play) {
			try {
				//if(Debug.isDebuggerConnected()) Log.d("FlyCastPlayer", this.guidSongCurrentlyDisplayed);
				TrackProgressInfo info = m_flycast_service_remote.getTrackInfoFromGuid(this.guidSongCurrentlyDisplayed);

				float progress = info.playedPercent;
				float downloaded = info.downloadPercent;
				seekbarBuffered.setPosition(progress/100.0f);
				seekbarBuffered.setProgress(downloaded/100.0f);
				seekbarBuffered.setDone(downloaded >= 100);
				
				// display or hide isLive image
				if (info.isLive) {
					Message message = new Message();
					message.what = SET_IMAGEVIEW_VISIBLE;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);

				} else {
					Message message = new Message();
					message.what = SET_IMAGEVIEW_INVISIBLE;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

				// display or hide progress indicator
				if (info.isPlaying) {
					Message message = new Message();
					message.what = SET_SEEKBAR_DEFAULT_VISIBLITY;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				} else {
					seekbarBuffered.setPosition(-1.0f);
					Message message = new Message();
					message.what = SET_SEEKBAR_VISIBLITY;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
				}

			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}
		/*
		 * Handle flyback scenario here we should not display anything when we
		 * have a flyback track.
		 */

		else {
				seekbarBuffered.setPosition(-1.0f);
				seekbarBuffered.setProgress(0.0f);
				Message message = new Message();
				message.what = SEEKBAR_GONE;
				appMobiActivity.myGUIUpdateHandler.sendMessage(message);
			}
	}
	
	
	public boolean playProgressUpdaterShouldRun = false;

	private void startPlayProgressUpdater() {
		//if(Debug.isDebuggerConnected()) Log.d("FlyCastPlayer","startPlayProgressUpdater called....");
		playProgressUpdater();
		if(playProgressUpdaterShouldRun && appMobiActivity!=null && appMobiActivity.state!=null && appMobiActivity.state.showPlayer) {
			Runnable notification = new Runnable() {
		        public void run() {
		        	//if(Debug.isDebuggerConnected()) Log.d("ProgressBar","Going to call startPlayProgressUpdater again....");
		        	startPlayProgressUpdater();
				}
		    };
		    uiclickhandler.postDelayed(notification,1000);
    	}
		else
		{
			playProgressUpdaterShouldRun = false;
		}
	}

	public void showHideProgressBar() {
		if(Debug.isDebuggerConnected()) Log.d("WebView", " showHideProgressBar....");
		if(Debug.isDebuggerConnected()) Log.d("WebView", " CurrentSubPanelMode is  " +CurrentSubPanelMode);
		if (CurrentSubPanelMode == SubPanelModes.Play) {
			if(Debug.isDebuggerConnected()) Log.d("WebView", " showHideProgressBar....SubPanelModes.Play....");
			Message message = new Message();
			message.what = SEEKBAR_INVISIBLE;
			appMobiActivity.myGUIUpdateHandler.sendMessage(message);

			appMobiActivity.findViewById(R.id.seek_spacer).setVisibility(View.VISIBLE);
			if( playProgressUpdaterShouldRun == false )
			{
				playProgressUpdaterShouldRun = true;
				startPlayProgressUpdater();
			}
			else
			{
				playProgressUpdater();
			}
			
		} else {
			if(Debug.isDebuggerConnected()) Log.d("WebView", " showHideProgressBar....inside else....");
			Message message = new Message();
			message.what = SEEKBAR_GONE_INVISIBLE;
			appMobiActivity.myGUIUpdateHandler.sendMessage(message);
			appMobiActivity.findViewById(R.id.seek_spacer).setVisibility(View.VISIBLE);
			playProgressUpdaterShouldRun = false;
		}
	}

	public class CellImageDownloader implements Runnable {
		public DPXMLTrack m_track = null;
		public Drawable m_Image = null;

		public CellImageDownloader(DPXMLTrack track) {
			m_track = track;
		}

		public void run() {
			try {

				m_Image = BitmapDrawable.createFromStream(new java.net.URL( m_track.imageurl).openStream(), "temp");

				if (m_Image == null) {
					m_Image = (Drawable) RootContext.getResources().getDrawable(R.drawable.artwork_unavailable);
				}
				if (UI_tracklistcurrent == null || UI_tracklistcurrent.children.size() == 0) {
					return;
				} else {
					m_track.imageoriginal = m_Image;
					m_track.imageoriginaldownloaded = true;
					imageHashTable.put(m_track.guidSong, m_Image);
					if(Debug.isDebuggerConnected()) Log.d("Table", "PUT " + "guid is " + m_track.guidSong);
				}

			} catch (IOException ioe) {
				return;
			}

			// BH mSwitcher image is handled by
			// AdapterView.OnItemSelectedListener
			// BH uiclickhandler.post(new
			// Run_mSwitcher_Set_Image(UI_trackcurrentindex));

			uiclickhandler.post(new Runnable() {
				public void run() {

				/*	appMobiActivity.imageAdapter = ImageListAdapterCurrent;
					appMobiActivity.mDPXMLTracklist = UI_tracklistcurrent;*/

					Message message = new Message();
					message.what = UPDATE_IMAGE_LIST_ADAPTER;
					appMobiActivity.myGUIUpdateHandler.sendMessage(message);
			//		ImageListAdapterCurrent.updateData(UI_tracklistcurrent);
					// ImageListAdapterCurrent.notifyDataSetChanged();
				}
			});
		}
	};

	private void PlayStationEx(String nodeid, String nodeshout){

		try {
			mNodeId = nodeid; // mNodeId is a "Global" variable
			mShoutUrl = nodeshout;
			ReturnMessageTitle = "";// Initialize, no error indicated
			ReturnMessageBody = ""; // Initialize, no error indicated

			CurrentPlayMode = PlayModes.Play; // BH_12-21-09
			// btnPauseToggle.setImageResource(R.drawable.pause); // added line
			UI_tracklistcurrent = null;
			// <<<<<<<<<<<< BH_11-25-09 THE PROBLEM??? >>>>>>>>>>>>>>>>
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION,
					"Run_mGallery_Adapter_New_Tracklist PlayStation");
			ImageListAdapterCurrent = new ImageListAdapter(appMobiActivity, this,
					UI_tracklistcurrent, UI_trackcurrentindex,
					UI_trackmaxplayed, uiclickhandler);

			guidSongCurrentlyDisplayed = "";

			PlayStationNameCurrent = "";

			PlayerStartBailout.set(false);
			PlayerWaitingForStart.set(true);
			WaitingForPlayerPrepare.set(false);

			mPlayStartLooper = new PlayStartLooper();
			mPlayStartLooper.start();
			Thread.sleep(200); // wait before sending message to
			// mPlayStartLooper.mHandler, it chokes if we
			// don't

			Message message = new Message();
			message.what = PLAY_START_INIT;
			mPlayStartLooper.mHandler.sendMessageAtFrontOfQueue(message);

		} catch (Exception ex) {
			ex.getMessage();
		}
	}

	public boolean wasServiceRunning() {
		ActivityManager am = (ActivityManager)appMobiActivity.getSystemService(Activity.ACTIVITY_SERVICE);

		List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(25);
		String serviceClass = appMobiActivity.getString(R.string.remote_service_action);
		for (ActivityManager.RunningServiceInfo service : services) {
			ComponentName name = service.service;
			String clazz;
			clazz = name.getClassName();
			if (clazz.equals(serviceClass)) {
				wasServiceRunningOnCreate = true;
				return true;
			}
		}
		wasServiceRunningOnCreate = false;
		return false;
	}

	public void getPlayerViewLatest() {

		try {
				SharedPreferences setSettings = appMobiActivity.getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor mEditor = setSettings.edit();
				mEditor.putString("UID", mUID);//Key, Value
				mEditor.commit();


				//BH_12-14-09 LOCK to Portrait Orientation for this version 1.9.0 beta -
				//appMobiActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //LOCK to Portrait Orientation for this version 1.9.0 beta

				//if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "onCreate savedInstanceState=" + savedInstanceState);

				FlyTunesBaseUrl = appMobiActivity.getString(R.string.FlyCastBaseUrl);					//"http://flycast.fm"
				FlyTunesClientServicesBaseUrl = FlyTunesBaseUrl + "/External/ClientServices.aspx";
				//WhiteLabelText = appMobiActivity.getString(R.string.WhiteLabelText);					//"FLYCAST"

				DeviceProxyBaseUrl = appMobiActivity.getString(R.string.DeviceProxyBaseUrl);			//BH_08-25-09 "http://localhost:"
				DeviceProxyMessagingPort = appMobiActivity.getString(R.string.DeviceProxyMessagingPort);//BH_08-25-09 "88"
				DeviceProxyMediaPort = appMobiActivity.getString(R.string.DeviceProxyMediaPort);		//BH_08-25-09 "89"

				WaitingForAppStart = true;
				myview = new View(appMobiActivity);	//BH_11-18-08 Thread experiments
				RootContext = appMobiActivity; 		//

				CurrentConnectivityState = GetConnectivityState(); //
				if (CurrentConnectivityState == "NONE"){
					ShowAlertDialog("Connection Error", "No network connectivity, restart FlyCast when connected.");
					return;
				}

				//-- Get Our Package Information ------------
				PackageManager mPackageManager = appMobiActivity.getPackageManager();
				//MOVED TO GLOBAL DATA - mPackageInfo and documentation - MOVED TO GLOBAL DATA
				try {
					//String PackageName = getPackageName();
					mPackageInfo = mPackageManager.getPackageInfo(appMobiActivity.getPackageName(), ((int)0));

				} catch (Exception e) {
					ShowAlertDialog("Startup Error", "Error: " + e.getMessage());
					return;
				}

				//--- Get Current Configuration Info ------------
				mConfiguration = appMobiActivity.getResources().getConfiguration();

				getScreenSizes();

		        if (FlyCastServiceInit() != "") {
		  			ReturnMessageTitle = "Application Start Cancelled";
		  			ReturnMessageBody = "Application has been cancelled";
		  			if(Debug.isDebuggerConnected()) Log.d(TAG_ACTIVITY, "ReturnMessageBody=" + ReturnMessageBody);
		  		    SendGUIUpdateMessage();
		  		    return;
				}

			} catch (Exception ex) {
				ShowAlertDialog("Startup Error", "Startup Error: " + ex.getMessage());
			}
	}

	public Boolean isRetrieving = false;

	public void PlayStationStream()
	{
		if(Debug.isDebuggerConnected()) Log.d("StartService", "In PlayStationStream");

		// Condition modified by Parveen on May13,2010 - To Show real player.
		if ((stationID == null) || (stationID.trim().equals(""))) {
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Show Real Player if no station id.");
			PlayStationEx(stationID, "");
			currentStationID = "";
		}

		else if (!appMobiActivity.state.playStarted && stationID != null && stationID != "") {
			currentStationID = stationID;
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Play Station if not already playing.StationID = " + stationID);
			//currentStationID = stationID;
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "About to call play Station");
			PlayStationEx(stationID, "");
			//getPlayerViewLatest();
			BaseQueryStringUpdate();
			currentStationID = stationID; // Avoid null pointer
		}
		//Added by Parveen on July 26,2010 - For Shoutcast station implementation.
		else if (appMobiActivity.state.playStarted && stationID != null && stationID != "" && currentStationID != stationID)
		{
			currentStationID = stationID;
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Stop current station and play new station.");
			//currentStationID = stationID;
			stopStationOnly();
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Stoped current station .");
			PlayStationEx(stationID, "");
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Started current station .Station id = "+ stationID);
			BaseQueryStringUpdate();
			currentStationID = stationID; // Avoid null pointer
		}

		else if (appMobiActivity.state.showPlayer) {
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Show player in running mode if already playing.");
			showPlayer();
		}
		currentShoutcastURL = "";
	}

	public void PlayShoutcastStream()
	{
		if(Debug.isDebuggerConnected()) Log.d("StartService", "In PlayShoutcastStream");

		// Condition modified by Parveen on May13,2010 - To Show real player.
		if ((shoutcastURL == null) || (shoutcastURL.trim().equals(""))) {
			if(Debug.isDebuggerConnected()) Log.d("PlayAudio", "I am in Play");
			PlayStationEx("0", shoutcastURL);
			currentShoutcastURL = "";
		}
		else if (!appMobiActivity.state.playStarted && shoutcastURL != null && shoutcastURL != ""  && (!currentShoutcastURL.equals(shoutcastURL))) {
			currentShoutcastURL = shoutcastURL;
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "About to call play Station");
			PlayStationEx("0", shoutcastURL);
			BaseQueryStringUpdate();
			currentShoutcastURL = shoutcastURL; // Avoid null pointer
		}
		else if (appMobiActivity.state.playStarted && shoutcastURL != null && shoutcastURL != ""  && (!currentShoutcastURL.equals(shoutcastURL)))
		{
			currentShoutcastURL = shoutcastURL;
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Stop current station and play ShoutCast station.");
			stopStationOnly();
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "About to call play Station");
			PlayStationEx("0", shoutcastURL);
			if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "Started current station .shoutcastURL  = "+ shoutcastURL);
			BaseQueryStringUpdate();
			currentShoutcastURL = shoutcastURL; // Avoid null pointer
		}

		else if (appMobiActivity.state.showPlayer) {
			showPlayer();
		}
		
		currentStationID = "";	
	}

	public void hidePlayer()
	{
		appMobiActivity.state.showPlayer = false;
		appMobiActivity.setContentView(appMobiActivity.root);
		loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.hide');document.dispatchEvent(ev);");
	}
	
	public void showPlayer()
	{
		if(Debug.isDebuggerConnected()) Log.d("StartService", "In PlayAudioExLatest showPlayer ");

		if(UI_tracklistcurrent==null || UI_tracklistcurrent.children.size()==0){
			//init();
			initTracklist();
		}
		
		loadUrlInAppMobiWebView("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.show');document.dispatchEvent(ev);");
		appMobiActivity.state.showPlayer = true;
		getScreenSizes();
		MainLayoutSet();
		
		getTracklistImages();
		/*
		 *do not set it if play has not started yet....
		 *otherwise it will stuck to 1st track even in case of flybacks 
		 * check here if state.playstarted is true 
		 */

		
		mGallery.setSelection(UI_trackcurrentindex);// BH old
		if(Debug.isDebuggerConnected()) Log.d("Selection", "Selected index in Galley " + UI_trackcurrentindex + " in PlayAudioExLatest");

		if(appMobiActivity.state.showPlayer && appMobiActivity.state.playStarted){
			EnableCommandButtons();
			if(Debug.isDebuggerConnected()) Log.d("StartService", "EnableCommandButtons() in show player");
		}
	}

	//initialize UI_tracklistcurrent, UI_trackcurrentindex,UI_trackmaxplayed
	public void init(){
		if (m_flycast_service_remote != null && isbServiceConnected() && mRemoteIsBound) {
			DPXMLTracklist list;
			try {
				list = m_flycast_service_remote.getCurrentTracklist();
				if (list != null) {
					UI_tracklistcurrent = list;
				}

				UI_trackcurrentindex = m_flycast_service_remote.getTrackCurrentIndex();
				UI_trackmaxplayed = m_flycast_service_remote.getSVC_trackmaxplayed();
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}
	}
	
	public void initTracklist(){
		//if(UI_tracklistcurrent==null || UI_tracklistcurrent.children.size()==0){
		UI_tracklistcurrent = DPApplication.Instance().GetTrackList();
		UI_trackcurrentindex = DPApplication.Instance().getCurrentPlayingIndex();
		UI_trackmaxplayed = DPApplication.Instance().getTrackmaxplayed();
		
		if(Debug.isDebuggerConnected()) Log.d("mSwitcherTracklistPosition","mSwitcherTracklistPosition in inittracklist is  "+mSwitcherTracklistPosition);
		
	}
	
	private void loadUrlInAppMobiWebView(final String string) {
		appMobiActivity.myGUIUpdateHandler.post(new Runnable() {
			public void run() {
				appMobiActivity.appView.loadUrl(string);
			}
		});
	}

	public void doOnResume() {
		pEditor = pView.getBoolean("PlayerView", false);

		if (pEditor) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "DroidGap::onResume ..PEditor is true");    
			appMobiActivity.state.playStarted = true;
			
			try {
				Message msg = new Message();
				msg.what = FlyCastPlayer.SEND_TRACK_INFO;
				myGUIUpdateHandler.sendMessage(msg);
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		}
		
		//Added by Parveen on Aug 06,2010 - Call ProgressBar updater after clicking HOME button and launching app again.
		if(pView.getBoolean("ShowPlayer", false))
		{			
			showHideProgressBar();
		}
	}

	public void doOnDestroy() {
		if(appMobiActivity.state!=null && !appMobiActivity.state.shutDownAppMobi){
			playerEditor.putBoolean("PlayerView", appMobiActivity.state.playStarted);// Key, // Value
			playerEditor.commit();
			
			/*
			 * if we were on show player and hit Back key its value should set as false which we have already set in onStop
			 */
			playerEditor.putBoolean("ShowPlayer", appMobiActivity.state.showPlayer);
			playerEditor.commit();
		}
	}

	public void onConfigurationChanged(int orientation) {
		MainLayoutSet();
		mGallery.setSelection(FlyCastPlayer.mSwitcherTracklistPosition);
	}

	public void setbServiceConnected(boolean bServiceConnected) {
		this.bServiceConnected = bServiceConnected;
	}

	public boolean isbServiceConnected() {
		return bServiceConnected;
	}

}
