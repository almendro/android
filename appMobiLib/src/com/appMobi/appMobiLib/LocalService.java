package com.appMobi.appMobiLib;

import java.net.BindException;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import com.appMobi.appMobiLib.util.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import fm.flycast.DPApplication;
import fm.flycast.DPCommandHandler;
import fm.flycast.DPException;
import fm.flycast.DPMemoryStatus;
import fm.flycast.DPTrackState;
import fm.flycast.DPUtility;
import fm.flycast.DPWebServer;
import fm.flycast.DPXMLMessage;
import fm.flycast.DPXMLObject;
import fm.flycast.DPXMLParser;
import fm.flycast.DPXMLTrack;
import fm.flycast.DPXMLTracklist;
import fm.flycast.FlyCastPlayer;
import fm.flycast.TrackProgressInfo;
import fm.flycast.XMLFail;
import fm.flycast.XMLNode;
import fm.flycast.XMLObject;
import fm.flycast.XMLParser;

//---------------------------------------------------------------------------------
// 1) This application service runs in a different process than the application.
// 2) Assigning android:process=":remote" to the service in the AndroidManifest.xml
// 	  file enables it to run in a separate process
// 3) Because it can be in another process, we MUST use IPC to interact with it.
//----------------------------------------------------------------------------------x
public class LocalService extends Service {

	int lastSavedPlayedPosition=0;
	Hashtable<String, DPXMLTracklist> savedTracklist = new Hashtable<String, DPXMLTracklist>();
	String lastPlayedStation;
	boolean receivedbufferMessage = false;
	boolean startStationCalled = false;
	
	private static DPXMLTrack previousListenedTrack=null;
	private int flyBackPosition=-1;
	//Initial values of following fetched from strings.xml
	public String FlyCastBaseUrl;					//http://flycast.fm
	public String FlyCastClientServicesBaseUrl;	//http://flycast.fm/External/ClientServices.aspx
	public String WhiteLabelText;	//Used in querystring Example: &WL=FLYCAST
	public String DeviceProxyBaseUrl; //"http://localhost:"
	public String DeviceProxyMessagingPort; //"88"
	public String DeviceProxyMediaPort; //"89"
 	public Long DeviceProxyPollMilliseconds = 1000L; //Polling Interval in Milliseconds
	boolean DeviceProxyIsAlive = false;

    XMLObject XMLObjectTemp;

	//-- FlyCast Specific
	String mPlatform = "ANDROID";	//THIS code is for Android devices
	String mUID = "";				//UserID (FlyCast User ID)

	public String BaseQueryString;		//Common Querystring for all FlyCast Web service calls
	public String CurrentDisplayType;

	//-- ConnectivityState ---------
	public String CurrentConnectivityState;
	public static final String CONNECTIVITY_NONE = "NONE";
	public static final String CONNECTIVITY_2G = "2G";
	public static final String CONNECTIVITY_3G = "3G";
	public static final String CONNECTIVITY_WIFI = "WIFI";

	private String GetConnectivityState() {
		WifiManager wifiMgr = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		if (wifiMgr.isWifiEnabled() == true) {
			return CONNECTIVITY_WIFI;
		}

		TelephonyManager telMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		int data = telMgr.getDataState();
		if( data == TelephonyManager.DATA_DISCONNECTED || data == TelephonyManager.DATA_SUSPENDED )
			return CONNECTIVITY_NONE;
		else
			return CONNECTIVITY_3G;
	}

	private String BaseQueryStringUpdate() {
		CurrentConnectivityState = GetConnectivityState(); //Added by BH_02-13-09
		if (CurrentConnectivityState == CONNECTIVITY_NONE) {
			return CONNECTIVITY_NONE;
		}
		else {
			BaseQueryString = "?PLAT=" + mPlatform + "&VER=" + mPackageInfo.versionName + "&DISPLAYTYPE=" +
			CurrentDisplayType + "&UID=" + mUID + "&SPEED=" + CurrentConnectivityState + "&WL=" + WhiteLabelText;
   		}
		return "";
	}

    //public static FlyCastMediaPlayer svc_mediaplayer = null;
    public static MediaPlayer svc_mediaplayer = null;

    // tags for android.util.Log Logging
    private static final String TAG_SERVICE = "FCServiceRemote";
    private static final String TAG_PLAYSTATIONREMOTE = "PlayStationRemote";
    public static final String TAG_DEVICEPROXY = "DeviceProxy";

    private static final String TAG_LIFECYCLE = "LifeCycle";

	private static final String TAG_DPWEBSERVER = "DPWebServer";
	private static final String TAG_DPXMLParser = "DPXMLParser";

//	public static final String IS_ALIVE =  "<XML><ALIVE><ACK/></ALIVE></XML>";
//	public static final String IS_DONE =  "<XML><ACK><OK/></ACK></XML>";
//	public static final String IS_NOT_DONE =  "<XML><ACK><NOT OK/></ACK></XML>";

	//from DPCommandHandler
	public static final String IS_ALIVE =  "<XML><ALIVE><ACK/></ALIVE></XML>";
	public static final String IS_DONE =  "<XML><ACK><OK/></ACK></XML>";
	public static final String IS_NOT_DONE =  "<XML><ACK><NOT OK/></ACK></XML>";

	//from DPRequestThread
	public static final String SDCARD_NOT_PRESENT =  "<XML><ALIVE><SDCARD_Not_Present/></ALIVE></XML>";
	public static final String SDCARD_OUT_OF_MEMORY_IS_ALIVE =  "<XML><ALIVE><SDCARD_Out_Of_Memory/></ALIVE></XML>";
	public static final String SDCARD_OUT_OF_MEMORY =  "<XML><ResourceContainer name=\"OutOfMemory\"></ResourceContainer></XML>";

	public String PlayNodeID;
	public String PlayShoutUrl;
	public String PlayguidSong="";
	boolean PlayguidSongIsLiveTrack;

	//public String PlayTrackUrl;
	public String PlayUrlCurrent;
	public String PlayStationNameCurrent;

	String FB30Url;		//FlyBack 30 Minutes Url
	String FB60Url;		//FlyBack 60 Minutes Url
	String FB120Url;	//FlyBack 120 Minutes Url

	String FB30Name;	//Display text
	String FB60Name;
	String FB120Name;

    XMLNode XMLNodeCurrent;
	XMLParser mXMLParser;
	//XMLParser mXMLParser2;

	/*
	 * Narinder 1/29/2010
	 * We will not be using this variable anymore
	 * instead we will directly access _Tracklist from DPApplication.java
	 */
	//DPXMLTracklist SVC_tracklistcurrent;


	int SVC_trackmaxplayed = 0;
	DPXMLTrack SVC_trackcurrent;
	int SVC_trackcurrentindex;
	DPXMLTrack SVC_tracktemp;

	public int MinimumSecondsBuffered = 6;	//BH_12-01-09 Seconds to buffer before sending TRACK_IS_BUFFERED
	public int MinimumBytesBuffered;		//MinimumBytesBuffered = MinimumSecondsBuffered * bitrate * 128;
	public int SVC_tracklistcurrent_bitrate;

	int MediaPlayerCurrentDuration;	//Total length of current track in milliseconds
	int MediaPlayerCurrentPosition;	//Most recent update of play position in milliseconds
	int MediaPlayerCurrentPercentBuffered; //Most recent update of PercentBuffered
	//int MediaPlayerBuffered;
	boolean MediaPlayerResumeTrackMode;

	public String DeviceProxyXMLStringCurrent;

	HttpClient mClient;


	enum PlayModes {Stop, Play, Pause}
	public PlayModes CurrentPlayMode;

	String ReturnMessageTitle = "";	//Used for passing Error message Title back from function
	String ReturnMessageBody = "";	//Used for passing Error message Body back from function
	String ReturnString;
	public static final String PLAY_ERROR_BODY =
		"We're sorry but we were unable to connect to this station. " +
		"Please try another station or verify your connection in Settings.";

	public static final int PROGRESS_MODE_PLAYSTART = 20;
	public static final int PROGRESS_MODE_PLAYSTARTCANCEL = 21;
	public static String BASE_DIR = "";

	public static final String PROGRESS_TEXT_PLAYSTART =
		"Connecting to station," + (char) 10 + "please wait..." + (char) 10 + "To cancel press BACK button";
	public static final String PROGRESS_TEXT_PLAYSTARTCANCEL = "Cancelling Play," + (char) 10 + "please wait...";

	PackageInfo mPackageInfo = new PackageInfo();

    public Context m_RootContext;
	int deviceProxyAudioPort;

    int mValue = 0;

    @Override
    public void onCreate() {
		super.onCreate(); //0.8.x
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "onCreate");
		if(Debug.isDebuggerConnected()) Log.d("StartService", "Service Started.. FlyCastServiceRemote");
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "FlyCastServiceRemote::onCreate");

		String name = getApplication().getResources().getString(R.string.dir_name);
		DPApplication.Instance().setName(name);

		BASE_DIR = Environment.getExternalStorageDirectory().getPath() + "/" + name + "/data/";

		FlyCastBaseUrl = getString(R.string.FlyCastBaseUrl);					//"http://flycast.fm"
		FlyCastClientServicesBaseUrl = FlyCastBaseUrl + "/External/ClientServices.aspx";
		WhiteLabelText = getString(R.string.WhiteLabelText);					//"FLYCAST"

		DeviceProxyBaseUrl = getString(R.string.DeviceProxyBaseUrl);			//BH_08-25-09 "http://localhost:"
		DeviceProxyMessagingPort = getString(R.string.DeviceProxyMessagingPort);//BH_08-25-09 "88"
		DeviceProxyMediaPort = getString(R.string.DeviceProxyMediaPort);		//BH_08-25-09 "89"
		//DeviceProxyPollMilliseconds = Long.getLong(getString(R.string.DeviceProxyMediaPort));
		//---  ---------
		deviceProxyAudioPort = Integer.parseInt(getString(R.string.DeviceProxyAudioPort));

        //(PROTO) While this service is running, it will continually increment a number.
        //Send the first message that is used to perform the increment
        mHandler.sendEmptyMessage(REPORT_MSG);

		//-- TelephonyManager 0.8.x ---------
		final TelephonyManager telMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		//this.telMgrOutput.setText(telMgr.toString());

		//-- PhoneStateListener 0.8.x ---------
		PhoneStateListener phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(final int state, final String incomingNumber) {
				//TelephonyManagerExample.this.telMgrOutput
				//.stText(TelephonyManagerExample.this.getTelephonyOverview(telMgr));
				//if(Debug.isDebuggerConnected()) Log.d(Constants.LOGTAG, "phoneState updated - incoming number - " + incomingNumber);
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "phoneState updated - incoming number - " + incomingNumber);
	    		switch (state) {
	    		case TelephonyManager.CALL_STATE_IDLE:
	    			MediaPlayerPlayIfPaused();
	    			break;
	    		case TelephonyManager.CALL_STATE_OFFHOOK:
	    			MediaPlayerPauseIfPlaying();
	    			break;
	    		case TelephonyManager.CALL_STATE_RINGING:
	    			MediaPlayerPauseIfPlaying();
	    			break;
	    		}
			}
		};
		telMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);//0.8.x

		//---
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "onCreate, Starting DPWebServer");
		startAudioDPWebServer(deviceProxyAudioPort);
		
		initReceiver();
    }
    
    public void  startAudioDPWebServer(int audioPort){
	    AppMobiActivity appMobiActivity = AppMobiActivity.sharedActivity;
		
		 if(appMobiActivity!=null){
		    String amdir = appMobiActivity.appDir().toString();
	    	new Thread(new Run_DPWebServerStart(amdir, audioPort )).start();
			try {
				Thread.sleep(1000); //Give it a chance to get going
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		  }
		
		String zStr = DP_IsAlive();
		if (zStr.equals("")!=true){
			DeviceProxyIsAlive = false;
		}
		else{
			DeviceProxyIsAlive = true;
		}

		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "onCreate, DeviceProxyIsAlive=" + DeviceProxyIsAlive);		
    }


    @Override
    public void onDestroy() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "LocalService::onDestroy");

        StopCurrentStationImpl();

        // Tell the user we stopped.
        Toast.makeText(this, R.string.app_name + " service has stopped", Toast.LENGTH_SHORT).show();

        //make widget revert to default layout
        //mAppWidgetProvider.notifyChange(this, FlyCastServiceRemote.DESTROYED);

        unregisterReceiver(mIntentReceiver);

        // Remove the next pending message to increment the counter, stopping
        // the increment loop.
        mHandler.removeMessages(REPORT_MSG);

        super.onDestroy();

		setMediaPlayerKillImpl(); //Moved here BH_03-30-09

		//kill my process since it was hanging around after onDetroy()
		Process.killProcess(Process.myPid());
    }

    @Override
    public IBinder onBind(Intent intent) {
            return mBinder;
    }

    public class ServiceBinder extends Binder
    {
    	public Service getService()
    	{
    		return LocalService.this;
    	}
    }
    
    public final IBinder mBinder = new ServiceBinder();
    
    @Override
    public boolean onUnbind(Intent intent) {
        // Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
    	if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "FlyCastServiceRemote::onUnbind");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        // Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
    	if(Debug.isDebuggerConnected()) Log.d(TAG_LIFECYCLE, "FlyCastServiceRemote::onRebind");
    }

    //-- IRemoteInterface is defined through IDL ---------
    //-- PRIMARY interface to the service ---------
    //private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {

        public void MediaPlayerSeekBack() {
        	MediaPlayerSeekBackImpl();
        	return;
        }

        public void MediaPlayerSeekForward () {
        	MediaPlayerSeekForwardImpl();
        	return;
        }

    	//Set DeviceProxy to RECORD tracks from Station
    	public void setPlayerIsRecording(boolean value){
    		PlayerIsRecording.set(value);
    		return;
    	}

  
    	public boolean getPlayerIsRecording(){
    		return PlayerIsRecording.get();
    	}
    	
        public void StopCurrentStation() {
        	StopCurrentStationImpl();
        	return;
        }

    	//void SwitchPlayTrack(int position);
        public void SwitchPlayTrack(int position) {
        	SwitchPlayTrackImpl(position);
        	return;
        }

        //BH_11-18-09
        public String getCurrentTracklistXML() {
        	return getCurrentTracklistXMLImpl();
        }

        //BH_11-20-09
        public int getTrackCurrentIndex() {
        	return SVC_trackcurrentindex;
        }

        //BH_11-23-09
        public int getSVC_trackmaxplayed() {
        	return SVC_trackmaxplayed;
        }

        //CurrentPlayMode = PlayModes.Stop;

        //BH_11-23-09
        public String getSVC_CurrentPlayMode() {
        	return CurrentPlayMode.toString();
        }

        public int getPid() {
            //Returns the identifier of this process, which can be used with killProcess(int) and sendSignal(int, int).
            return Process.myPid();	//int android.os.Process.myPid()
        }

        public String setBaseQueryString(String iBaseQueryString){
        	BaseQueryString = iBaseQueryString;
        	return "";
        }

        public String setCurrentDisplayType(String iCurrentDisplayType) {
        	CurrentDisplayType = iCurrentDisplayType;
        	return "";
        }

        //-- OLD FUNCTIONS from 0.8.x branch BH_08-2-09 ---------
		public String getTestString() throws RemoteException {
			return getTestStringImpl();
		}

		public String setMediaPlayer() throws RemoteException {
			return setMediaPlayerImpl();
		}

		public String setMediaPlayerUrl(String Url) throws RemoteException {
			return setMediaPlayerUrlImpl(Url);
		}

		public String setMediaPlayerPrepare() throws RemoteException {
			setMediaPlayerPrepareImpl();
			return "";
		}

		public String setMediaPlayerStart() throws RemoteException {
			setMediaPlayerStartImpl();
			return "";
		}

		public String setMediaPlayerPauseToggle() throws RemoteException {
			return setMediaPlayerPauseToggleImpl();
		}

		public String setMediaPlayerKill() throws RemoteException {
			return setMediaPlayerKillImpl();
		}

		public String setMediaPlayerStop() throws RemoteException {
			return setMediaPlayerStopImpl();
		}

		public void killFlyCastService() throws RemoteException {
			killFlyCastServiceImpl();
			return;
		}

		public String PlayStationRemote(String NodeID, String NodeShout, String UserID){
			return PlayStationRemoteImpl(NodeID, NodeShout, UserID);
		}

		//@Override
		public TrackProgressInfo getTrackInfoFromGuid(String songGuid)
				throws RemoteException {
			TrackProgressInfo info = LocalService.this.getDisplayedTrackStatus(songGuid);

			return info;
		}
		
		public DPXMLTracklist getCurrentTracklist() {
			DPXMLTracklist list = LocalService.this.getMasterTracklist();

			return list;
		}

		public String getCurrentPlayingStation() {
			try{
			return LocalService.this.getStationName().toString();
			}
			catch(Exception ex){
				return "";
			}
		}

		public String getCurrentPlayingGuid() {
			return LocalService.this.getPlayingGuid().toString();
		}

		public String sendFile() {
			//send request to dp to send back file and send this file to playerappmobilatest
			return "";
		}

    private static final int REPORT_MSG = 1;

    /**
     * Our Handler used to execute operations on the main thread.  This is used
     * to schedule increments of our value.
     */
    private final Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
               
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private void MediaPlayerInit() {

    	//BH_12-09-09 EXPERIMENT Reset it every time
    	//svc_mediaplayer.release();
    	//svc_mediaplayer.reset();

		//See if MediaPlayer already existing
		if (svc_mediaplayer != null) {
			svc_mediaplayer.reset();
		} else
		{
			svc_mediaplayer = new MediaPlayer();
		}
		svc_mediaplayer.setOnErrorListener(MediaPlayerOnErrorListener);
		svc_mediaplayer.setOnPreparedListener(MediaPlayerOnPreparedListener);
		svc_mediaplayer.setOnCompletionListener(MediaPlayerOnCompletionListener);
		svc_mediaplayer.setOnInfoListener(MediaPlayerOnInfoListener);
		// svc_mediaplayer.setOnSeekCompleteListener(listener);
		svc_mediaplayer.setOnBufferingUpdateListener(MediaPlayerOnBufferingUpdateListener);
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerInit READY");
	}

    //-----------------------------------
    //--- svc_mediaplayer Listeners -----
    //-----------------------------------
    private MediaPlayer.OnErrorListener MediaPlayerOnErrorListener = new MediaPlayer.OnErrorListener(){
		//@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {

			// public abstract boolean onError (MediaPlayer mp, int what, int extra)
			// Since: API Level 1
			// Called to indicate an error.
			// Parameters
			//	 mp - the MediaPlayer the error pertains to
			//	 what - the type of error that has occurred:
			//		MEDIA_ERROR_UNKNOWN
			//		MEDIA_ERROR_SERVER_DIED
			//	 extra -	an extra code, specific to the error. Typically implementation dependant.
			// Returns
			//	 True if the method handled the error, false if it didn't.
			//	 Returning false, or not having an OnErrorListener at all, will cause the OnCompletionListener to be called.

			//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnError");
			if(Debug.isDebuggerConnected()) Log.d("PLAYER_COMPLETE", "OnErrorListener");
			String mStr = "";
			if( mp == svc_mediaplayer )
			{
				switch (what) {
				case FlyCastPlayer.MEDIA_ERROR_SERVER_DIED:
					// Media server died. Application must release the MediaPlayer object and instantiate a new one
		    		PlayStartCancel();
					StopCurrentStationImpl();	//Kill existing PLAY if already in progress
					setMediaPlayerKillImpl();

					mStr = "Server died " + extra;
	    			ReturnMessageTitle = "Media Player Error";
	    			ReturnMessageBody = mStr;
		    		ShowFailedDialog();
		    		return true;
					//break;
				case FlyCastPlayer.MEDIA_ERROR_UNKNOWN:
					// Unspecified media player error
					// narinder dev: We have been able to find the reason for error code -10 and -11
					//--When media player is not able to understand the content of media stream it throws unknown error -10
					//--When media player times out before it gets any bytes to play it throws unknown error -11
					//--When we send 0 bytes to media player it throws unknown error -1

					return false;	//Ajay 12-21-09
					//break;
				default:
					//Media error, Base -38. Extra 0 (john)
		    	/*	PlayStartCancel();
					StopCurrentStationImpl();	//Kill existing PLAY if already in progress
					setMediaPlayerKillImpl();

					mStr = "Media error, Base " + what + " , Extra " + extra;
	    			ReturnMessageTitle = "Media Player Error";
	    			ReturnMessageBody = mStr;
		    		SendGUIUpdateMessagePlayFailed();	*/				// return false;
		    		return true;
					// break;
				}
			}
			else {
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnError, NOT svc_mediaplayer " + mStr);
			}

			return false;

		}
    };


    private MediaPlayer.OnInfoListener MediaPlayerOnInfoListener = new MediaPlayer.OnInfoListener(){

		//@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "LOOPING IS " +  mp.isLooping());
			if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "INFO LISTENER WHAT"  + what  + " EXTRA :: " + extra);
			if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "Is playing " + mp.isPlaying() );
			return true;
		}

    };

    private MediaPlayer.OnPreparedListener MediaPlayerOnPreparedListener = new MediaPlayer.OnPreparedListener(){
		//@Override
		public void onPrepared(MediaPlayer mp) {
			Message message = new Message();
	    	message.what = FlyCastPlayer.PLAYER_PREPARED;
	    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
			PlayerWaitingForPrepare.set(false);
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnPrepared");
		}
    };


    private MediaPlayer.OnBufferingUpdateListener MediaPlayerOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

		//@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			MediaPlayerCurrentPercentBuffered = percent;
			MediaPlayerCurrentPosition = svc_mediaplayer.getCurrentPosition();
			//if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "Called with percent " + percent);
			//if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "CurrentPosition=" + MediaPlayerCurrentPosition);
		}

	};

	int lastPlayedPosition = 0;
	int numberOfTries = 0;
    private MediaPlayer.OnCompletionListener MediaPlayerOnCompletionListener = new MediaPlayer.OnCompletionListener(){
		//@Override
		public void onCompletion(MediaPlayer mp) {
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnCompletion");

			//	MediaPlayerCurrentDuration = 0;			//BH_12-07-09 Total length of current track in milliseconds
			//do not ask current position to media player it gives -1 here
			//	MediaPlayerCurrentPosition = svc_mediaplayer.getCurrentPosition();	//BH_12-07-09 Most recent update of play position in milliseconds

			if(Debug.isDebuggerConnected()) Log.d("PLAYER_COMPLETE", "OnCompletionListener");
		

				if(Debug.isDebuggerConnected()) Log.d("onCompletion", "in OnCompletionListener");
				if(Debug.isDebuggerConnected()) Log.d("onCompletion", "lastPlayedPosition is "+lastPlayedPosition);
				if(Debug.isDebuggerConnected()) Log.d("onCompletion", "MediaPlayerCurrentPosition is "+MediaPlayerCurrentPosition);
				if(Debug.isDebuggerConnected()) Log.d("onCompletion", "numberOfTries is "+numberOfTries);
				boolean resume = false;
    		if( DPCommandHandler.PlayTrackLastSource == null ){
    			if(Debug.isDebuggerConnected()) Log.d("onCompletion", "PlayTrackLastSource is null;");
    		}

    		
    		/*
    		 * If we have played n number of bytes and we try to play the track again onCompletionListener will get fired
    		 * at this point we need to check that played bytes are more than n or not
    		 *
    		 *  If they are = to n it means we could not download more bytes from server and we should say
    		 *  that there is a problem with the server please try again laterf
    		 *
    		 */
    		if( DPCommandHandler.PlayTrackLastSource != null && DPCommandHandler.PlayTrackLastSource.HasMoreContent()){
    			resume = true;
    			if(MediaPlayerCurrentPosition > lastPlayedPosition){
    					lastPlayedPosition = MediaPlayerCurrentPosition;
    			}
    		}

    		if( numberOfTries==10){//it retries after 7 seconds 
				boolean stationError = true;
    			DPXMLTrack current= (DPXMLTrack) DPApplication.Instance()._Tracklist.children.elementAt(SVC_trackcurrentindex);
    
    			if (current.cached && (SVC_trackcurrentindex + 1) < (DPApplication.Instance()._Tracklist.children.size())) {
    				DPXMLTrack	next = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.elementAt(SVC_trackcurrentindex+1);
    				if(next.buffered){
    					resume = false;
    					stationError = false;
    				}
				}
    			if(stationError){
					try {
						if(Debug.isDebuggerConnected()) Log.d("onCompletion", "numberOfTries" + numberOfTries);
						if(Debug.isDebuggerConnected()) Log.d("onCompletion", "Going to stop the station ");
						Intent intent = new Intent(FlyCastPlayer.SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW);
						sendBroadcast(intent);
						StopCurrentStationImpl();// stop the current station
					} catch (Exception e) {
						if(Debug.isDebuggerConnected()) Log.d("SEND_STATION_ERROR_MESSAGE_TO_WEBVIEW","Exception while BroadCasting the intent");
					}
				}	
			}

    		
			if(Debug.isDebuggerConnected()) Log.d("onCompletion", "Resume is :: " + resume );

			if (resume){
				numberOfTries = numberOfTries + 1;
				//Player STARVED, ran out of data before buffered
		    	//BROADCAST_TRACK_PLAY_BUFFER_DELAY
				Intent intent = new Intent(FlyCastPlayer.BROADCAST_TRACK_PLAY_BUFFER_DELAY);
				sendBroadcast(intent);

				//Restart this track in RESUME mode
	    		MediaPlayerCurrentPercentBuffered = 0; 	//BH_12-07-09 Most recent update of PercentBuffered
				MediaPlayerResumeTrackMode = true;
				Message message = new Message();
		    	message.what = PLAY_MEDIAPLAYERSET;
		    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
		    	return;
			}
			else{
				lastPlayedPosition = 0;
				numberOfTries =0;
	    		MediaPlayerCurrentPercentBuffered = 0; 	//BH_12-07-09 Most recent update of PercentBuffered
				MediaPlayerResumeTrackMode = false;
				int index;
	    		for (index = 0; index < 60; index++){

	    			//Does next track exist?
	    			if ((SVC_trackcurrentindex + 1) < (DPApplication.Instance()._Tracklist.children.size())) {
	    				DPXMLObject tObj = null;
						try {	//BH_01-12-10 added try/catch
							tObj = (DPXMLObject) DPApplication.Instance()._Tracklist.children.elementAt(SVC_trackcurrentindex+1);
						} catch (Exception e) {
							e.printStackTrace();
						}
	    				if (tObj.type == DPXMLObject.TRACK){
	        				DPXMLTrack newtrack = (DPXMLTrack) tObj;
	        				if ((newtrack.equals(null)!=true) && (newtrack.guidSong.equals(null)!=true)){
	        					SVC_trackcurrentindex ++;
	        					SVC_trackcurrent = newtrack;
	        					PlayguidSong = SVC_trackcurrent.guidSong;
	        					//PlayguidSongIsLiveTrack = getDisplayedTrackStatus(PlayguidSong); //BH_12-22-09
	        					//PlayguidSongIsLiveTrack = true;

	        					//if next Track already BUFFERED run PLAY_MEDIAPLAYERSET now, otherwise DevicProxyPollLoop will pick it up
	        					if (SVC_trackcurrent.buffered==true){
	        						//BH_11-17-09 Ajay says DeviceProxy waits for buffered, we don't need to
	        						//I think I still need it this way
	        						Message message = new Message();
	        				    	message.what = PLAY_MEDIAPLAYERSET;
	        				    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
	        					}

	        			    	//BROADCAST_TRACK_IS_SWITCHING
	        					Intent intent = new Intent(FlyCastPlayer.BROADCAST_TRACK_IS_SWITCHING);
	        					intent.putExtra(FlyCastPlayer.BROADCAST_TRACK_IS_SWITCHING_BODY_KEY, (new Integer(newtrack.IndexInList).toString()));
	        					sendBroadcast(intent);
	        			    	return;
	        				}
	    				}

	     			}	//if ((trackcurrentindex+1) < (SVC_tracklistcurrent.children.size()))
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnCompletion Retry Loop Index=" + index);
	    			try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnCompletion InterruptedException " + e.getMessage());
					}
	    		}	//for (int index = 0; index < 60; index++)
				{
					//No next track after retries, we found END of Tracklist, JANE, STOP THIS CRAZY THING!!!
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "MediaPlayerOnCompletion 2 NO NEW Track");
					Message message = new Message();
			    	message.what = PLAYER_TRACKLIST_ENDED;
			    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
				}
			} 	//MediaPlayerCurrentPercentBuffered==100
		}	//public void onCompletion(MediaPlayer mp)
    };

 	private DPXMLTracklist getMasterTracklist() {
		DPXMLTracklist tracklist = null;
		try {
			tracklist = DPApplication.Instance().GetTrackList();
		}
		catch (Exception dpException) {
			tracklist = null;
		}
	

		return tracklist;
	}
	TrackProgressInfo info = null;
	String testGuid = null;
    //Query DeviceProxy, see if guidsong is the "Live track" on the station
	private TrackProgressInfo getDisplayedTrackStatus(String mguid) {
		//this loop will make sure we initialize only one variable per track
		if(testGuid==null || !testGuid.equalsIgnoreCase(mguid)){
			if(Debug.isDebuggerConnected()) Log.d("WebView", "testGuid is ...."+testGuid);
			testGuid = mguid;
			info = new TrackProgressInfo();
		}

		if(svc_mediaplayer!=null && svc_mediaplayer.isPlaying()){
			MediaPlayerCurrentPosition = svc_mediaplayer.getCurrentPosition();
			//if(Debug.isDebuggerConnected()) Log.d("Progress", "lastPlayedPosition.. " + MediaPlayerCurrentPosition);
		}
	
		//*
		DPXMLTrack tracktemp = null;
		float length=0;
		float downloadPercentile=0;
		float position = 0;
		//float duration = 0;
		float actualDuration =0;
		try {
			tracktemp = DPApplication.Instance().GetTrack(mguid);
		}
		catch (DPException dpException) {
			tracktemp = null;
		}

		if (tracktemp != null) {
			//DPXMLTrack tracktemp = (DPXMLTrack) messagecurrent.tracklist.children.get(0);
	    	//if(Debug.isDebuggerConnected()) Log.d("songGuidIsLiveTrack", "ROSCO *** TRACK length " + tracktemp.length);
	    	//if(Debug.isDebuggerConnected()) Log.d("songGuidIsLiveTrack", "ROSCO *** TRACK current " + tracktemp.current);
	    	//if(Debug.isDebuggerConnected()) Log.d("songGuidIsLiveTrack", "ROSCO *** TRACK offset " + tracktemp.offset);

			
			//init isLive
			  if(tracktemp.length==999999999 || DPApplication.Instance().GetTrackList().shoutcasting){
				  info.isLive = true;
			  }
	    
			  length = (tracktemp.length==999999999)?(15*60*tracktemp.bitrate*128):tracktemp.length;
	    	//if(Debug.isDebuggerConnected()) Log.d("Progress", "Track Guid is  .." + tracktemp.guidSong);
	    	//if(Debug.isDebuggerConnected()) Log.d("Progress", "Length is .." + length);
			//init downloadPercent
	    	 downloadPercentile = ((float)tracktemp.current-(float)tracktemp.offset)/(float)length;
	    	info.downloadPercent = (int)(downloadPercentile*100);
	    	//if(Debug.isDebuggerConnected()) Log.d("Progress", "TRACK downloadPercentile " + downloadPercentile + " TRACK downloadPercent " + info.downloadPercent);

			//init isPlaying
			info.isPlaying = mguid.equals(PlayguidSong);

			//init percent played
			if(info.isPlaying && svc_mediaplayer!=null && svc_mediaplayer.isPlaying()) {
				 position = svc_mediaplayer.getCurrentPosition();
				 //duration = svc_mediaplayer.getDuration();
				//if(Debug.isDebuggerConnected()) Log.d("Progress", "position/1000 is .." + position/1000);
				//if(Debug.isDebuggerConnected()) Log.d("Progress", "duration/1000 is .. " + duration/1000);
				//if( tracktemp.bitrate > 0 ) duration = (length/128/tracktemp.bitrate) * 1000;
				//if(Debug.isDebuggerConnected()) Log.d("Progress", "Duration of track in minutes.. " + (length/128/tracktemp.bitrate));
				//info.playedPercent = (int)((position/duration) * 100);
				/*
				 *
				 * mp.getDuration is not reliable
				 */
				 actualDuration = (float) ((length/128/tracktemp.bitrate) * 1.023);
				//float actualPosition = position/10;
				info.playedPercent = (int)((position/10/actualDuration));
				//if(Debug.isDebuggerConnected()) Log.d("Progress", "playedPercent.. " + info.playedPercent);
			}
		}

		return info;
		//*/
	}


    //-------------------------------------------------------
    //-- BEGIN OLD FUNCTIONS from 0.8.x branch BH_08-2-09 ---
    //-------------------------------------------------------

	synchronized private String getTestStringImpl() {
		return("Test");
	}

	synchronized void killFlyCastServiceImpl() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "killFlyCastServiceImpl");
		try {
			if (svc_mediaplayer != null) {
				if (svc_mediaplayer.isPlaying()==true){
					svc_mediaplayer.stop();
				}
				//svc_mediaplayer.reset();
				svc_mediaplayer.release();
				svc_mediaplayer = null;
			}
			this.finalize();
			stopSelf ();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return;
	}


	synchronized String setMediaPlayerImpl() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerImpl");
		MediaPlayerInit();
		return "";
	}

	synchronized String setMediaPlayerUrlImpl(String Url) {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerImpl Url=" + Url);
		try	{
			if(Debug.isDebuggerConnected()) Log.d("PLAYER_COMPLETE", "SetMediaPlayerImpl");
			svc_mediaplayer.setDataSource(Url);
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
		if(Debug.isDebuggerConnected()) Log.d("Media Player", "setDataSource done");
		return "";
	}

	synchronized String setMediaPlayerPrepareImpl() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerPrepareImpl");
		try	{

			svc_mediaplayer.prepare();
			//If we are in resume mode start the player from last played position
			
			
			if(DPApplication.Instance().resumeMode)
			{
				DPApplication.Instance().resumeMode = false;
				Thread.sleep(1500);
				int position = lastSavedPlayedPosition-5;
				if(Debug.isDebuggerConnected()) Log.d("Media Player", "Starting in resume mode at lastSavedPlayedPosition-5 "+(lastSavedPlayedPosition-5));
				if(position<0)
					position = 0;
				svc_mediaplayer.seekTo(position);
				Thread.sleep(1500);
				
			}
			else if(MediaPlayerResumeTrackMode){
			// TODO: We need to revisit this at some point and see if we can remove all the sleeps
				Thread.sleep(1500);
				int position = lastPlayedPosition-5;
				if(Debug.isDebuggerConnected()) Log.d("Media Player", "Starting in resume mode at lastPlayedPosition-5 "+(lastPlayedPosition-5));
				if(position<0)
					position = 0;
				svc_mediaplayer.seekTo(position);
				Thread.sleep(1500);
			}
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
		if(Debug.isDebuggerConnected()) Log.d("Media Player", "prepare done");
		return ""; //if no error
	}

	synchronized String setMediaPlayerPrepareAsynchImpl() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerPrepareImpl");
		try	{
			svc_mediaplayer.prepareAsync();
			//If we are in resume mode start the player from last played position
			// TODO: We need to revisit this at some point and see if we can remove all the sleeps
		
			if(DPApplication.Instance().resumeMode)
			{
				DPApplication.Instance().resumeMode = false;
				Thread.sleep(1500);
				int position = lastSavedPlayedPosition-5;
				if(Debug.isDebuggerConnected()) Log.d("Media Player", "Starting in resume mode at lastSavedPlayedPosition-5 "+(lastSavedPlayedPosition-5));
				if(position<0)
					position = 0;
				svc_mediaplayer.seekTo(position);
				Thread.sleep(1500);
				
				
			}
			
			else if(MediaPlayerResumeTrackMode){
				Thread.sleep(1500);
				int position = lastPlayedPosition-5;
				if(Debug.isDebuggerConnected()) Log.d("Media Player", "Starting in resume mode at lastPlayedPosition-5 "+(lastPlayedPosition-5));
				if(position<0)
					position = 0;
				svc_mediaplayer.seekTo(position);
				Thread.sleep(1500);
			}
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
		if(Debug.isDebuggerConnected()) Log.d("Media Player", "prepare done");
		return ""; //if no error
	}

	synchronized String setMediaPlayerStartImpl() {
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerStartImpl");
		try	{
			// TODO: We need to revisit this at some point and see if we can remove all the sleeps
			if(MediaPlayerResumeTrackMode){
				Thread.sleep(1000);
			}
			svc_mediaplayer.start();
			CurrentPlayMode = PlayModes.Play;
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
		if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "setMediaPlayerStartImpl Start DONE");
		return ""; //if no error
	}

	synchronized String setMediaPlayerPauseToggleImpl() {
		try	{
			if (CurrentPlayMode == PlayModes.Stop)
				return "stopped";
			else {
				if ((CurrentPlayMode == PlayModes.Play) && (svc_mediaplayer.isPlaying()==true))
				//if ((CurrentPlayMode == PlayModes.Pause))
				{
					svc_mediaplayer.pause();
					//CurrentPlayMode = PlayModes.Play;
					CurrentPlayMode = PlayModes.Pause;
					return "pause";

				}
				//else if ((CurrentPlayMode == PlayModes.Play) && (svc_mediaplayer.isPlaying()==true))
				else if (CurrentPlayMode == PlayModes.Pause)
				{
					svc_mediaplayer.start();
					CurrentPlayMode = PlayModes.Play;
					return "play";
				}
				else return "";
			}
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
		//return "";

		//Before 12-14-09
//		try	{
//			if (svc_mediaplayer.isPlaying()==true){
//				svc_mediaplayer.pause();
//				CurrentPlayMode = PlayModes.Stop;
//				return "pause";
//			}
//			else {
//				svc_mediaplayer.start();
//				CurrentPlayMode = PlayModes.Play;
//				return "play";
//			}
//		}
//		catch (Exception ex) {
//			return ex.getMessage();
//		}
	}

	synchronized String setMediaPlayerStopImpl() {
		try	{
			if (svc_mediaplayer.isPlaying()==true){
				svc_mediaplayer.stop();
				CurrentPlayMode = PlayModes.Stop;
				return "stop";
			}
			else return "";
		}
		catch (Exception ex) {
			return ex.getMessage();
		}
	}

	synchronized String setMediaPlayerKillImpl() {
		if (svc_mediaplayer != null) {
			if (((CurrentPlayMode == PlayModes.Play) && (svc_mediaplayer.isPlaying()==true))
					|| (CurrentPlayMode == PlayModes.Pause)){
				svc_mediaplayer.stop();
				CurrentPlayMode = PlayModes.Stop;
			}
//			svc_mediaplayer.m_done = true;
			svc_mediaplayer.release();
			svc_mediaplayer = null;
		}
		return "";
	}
	
	//flag used to remember if player was paused prior to phone call so that we dont start playing
    //in the case where we were paused before the phone call
    private boolean wasPausedBeforeCall = false;
    
    //Used by phoneStateListener when handling incoming calls
    private void MediaPlayerPauseIfPlaying() {
            if (svc_mediaplayer != null) {
                    if (svc_mediaplayer.isPlaying()) {
                            svc_mediaplayer.pause();
                            CurrentPlayMode = PlayModes.Pause;
                    } else {
                            wasPausedBeforeCall = true;
                    }
            }
    }
    
    //Used by phoneStateListener when handling incoming calls
    private void MediaPlayerPlayIfPaused() {
            if (svc_mediaplayer != null && !wasPausedBeforeCall) {
                    svc_mediaplayer.start();
                    CurrentPlayMode = PlayModes.Play;
            }
    }

	/*
	 * Narinder 04/12/2010
	 * Start the player from beginning when we get the request to play the
	 * same track (i.e track being played currently)
	 */

	private void PlayFromBeginning() {
		if ((svc_mediaplayer != null)) {
			try {
			svc_mediaplayer.seekTo(0);
			/*
			 *  6/30/2010
			 * If Media Player is in pause mode start it
			 */
			if(!svc_mediaplayer.isPlaying()){
					svc_mediaplayer.start(); //Seeks to starting position time position in miiliseconds
			}

			} catch (Exception e) {
				//e.printStackTrace();
			}
		}
	}

	//BH_01-05-10
	private void MediaPlayerSeekBackImpl() {
		if ((svc_mediaplayer != null) && (svc_mediaplayer.isPlaying())) {
			int Position = svc_mediaplayer.getCurrentPosition();	//Returns the current playback position in milliseconds
			int Position2 = Position - 30000;
			if (Position2>=0){
				try {
					svc_mediaplayer.seekTo(Position2);					//Seeks to specified time position in miiliseconds
				} catch (IllegalStateException e) {
					if(Debug.isDebuggerConnected()) Log.d("Rewind", "Illegal State Exception in Service remote in if");
				}catch(Exception e){
					if(Debug.isDebuggerConnected()) Log.d("Rewind", "Exception in Service remote in if"+e.getMessage());
				}
			}
			else {
				try {
					svc_mediaplayer.seekTo(0);					//Seeks to specified time position in miiliseconds
				} catch (IllegalStateException e) {
					//e.printStackTrace();
					if(Debug.isDebuggerConnected()) Log.d("Rewind", "Illegal State Exception in Service remote in else");
				}
			}
		}
	}

	//BH_01-05-10
	private void MediaPlayerSeekForwardImpl() {
		if ((svc_mediaplayer != null) && (svc_mediaplayer.isPlaying())) {
			int Duration = svc_mediaplayer.getDuration();			//Returns the file duration in milliseconds
			int Position = svc_mediaplayer.getCurrentPosition();	//Returns the current playback position in milliseconds
			int Position2 = Position + 30000;
			if (Position2 <= (Duration-4000)){
				try {
					svc_mediaplayer.seekTo(Position2);					//Seeks to specified time position in miiliseconds
				} catch (IllegalStateException e) {
					//e.printStackTrace();
				}
			}
			else {
				try {
					svc_mediaplayer.seekTo(Duration-4000);					//Seeks to specified time position in miiliseconds
				} catch (IllegalStateException e) {
					//e.printStackTrace();
				}
			}
		}
	}

	//-----------------------------------------------------
    //-- END OLD FUNCTIONS from 0.8.x branch BH_08-2-09 ---
    //-----------------------------------------------------

	public void StopCurrentStationImpl() {
		String ssXmlStg;
		DPXMLParser ssParser;
		String tStr = null;
			try {
				//--- StopCurrentStationImpl ---
				if ((PlayerIsPlaying.get()==false) && (PlayerWaitingForStart.get()==false) && (PlayerWaitingForPrepare.get()==false))
				{
					return;
				}

				if (PlayerIsPlaying.get()== true){

				}

				if (PlayerWaitingForStart.get()==true){
		    		Message message = new Message();
		    		message.what = PLAY_START_BAIL;
		    		mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
				}

				if (PlayerIsRecording.get()){
					DPXMLTracklist list = DPApplication.Instance().GetTrackList();
					list.startindex = SVC_trackcurrentindex;
					savedTracklist.put(PlayNodeID, list );
					lastSavedPlayedPosition = MediaPlayerCurrentPosition;
				}else{
					if(savedTracklist.size()>0){
						savedTracklist.clear();
					}
				}
		    	
		    	String mStr = (new DPCommandHandler()).StopStation(PlayNodeID, mUID, PlayerIsRecording.get());

    			ssXmlStg = mStr;
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STOPSTATION returned " + ssXmlStg);

    			if (!PlayerIsRecording.get()) {
    				//If RecordTracks="0", look for IS_DONE (ack)
    				int mIndex = ssXmlStg.indexOf(IS_DONE, 0); //Search for ACK
    				if (mIndex == -1) {
    					//No ack from DeviceProxy
     					throw new Exception("DP_STOPSTATION Record=0, IS_DONE NOT received, string=" + ssXmlStg);
    					//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STOPSTATION Record=0, IS_DONE NOT received, string=" + ssXmlStg);
    				}
    			}
    			else {
    				//RecordTracks="1"
        			if ((ssXmlStg.equals("") == true)) {
    					throw new Exception("DP_STOPSTATION Record=1, Empty response from DeviceProxy");
        			}
        			else {
        				ssParser = new DPXMLParser(ssXmlStg);
            			//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STARTSTATION ssParser2=" + ssParser.toString());
            			tStr = ssParser.parse();
        	    		if ((tStr.equals("") != true)) {
        					throw new Exception("DP_STOPSTATION Parser returned " + tStr);
        	    		}
        	    		else if ((ssParser.messagelist.equals(null))) {
        					throw new Exception("DP_STOPSTATION Parser returned null Messagelist");
        	    		}
        	    		else if ((ssParser.messagelist.children.size()==0)){
        					throw new Exception("DP_STOPSTATION Parser returned Empty Messagelist");
        	    		}
    					ssParser = null;

        			}
    			}
		    	tStr = null;
				try {
					tStr = setMediaPlayerKillImpl();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if ((tStr!="stop") && (tStr!="")){
					//Exception was returned
					//Go ahead and stop the Proxy anyway
					//throw new Exception("MediaPlayerStop Exception " + tStr);
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STOPSTATION MediaPlayerStop Exception=" + tStr);
				}
	    	}
	    	catch (Exception ex) {
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STOPSTATION Exception: " + ex.getMessage());
	    	}
	    	finally {
    			PlayStartCancel();
    			//Update Flags
        		PlayerWaitingForStart.set(false);
        		PlayerIsPlaying.set(false);
        		//PlayerIsRecording.set(false);
		    	PlayerWaitingForStart.set(false);
	    		CurrentPlayMode = PlayModes.Stop;
				//ZZZ UIPlayStartProgressClose();
		    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STOPSTATION Complete");
		    	svc_mediaplayer=null;
	    	}
	    	DPApplication.Instance().resumeMode = false;	
	}

	private DPXMLTrack _flybackLookupTrack = null;
	private int _flybackIndex = -1;

    public void SwitchPlayTrackImpl(int position) {

    	lastPlayedPosition = 0;
    	DPXMLTrack newtrack = null;
	
    	String FlybackID;

		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "SwitchPlayTrack Position = " + position);

		if (DPApplication.Instance()._Tracklist == null) return; //BH_12-11-09 Prevent switch attempt if no tracklist

		MediaPlayerCurrentPercentBuffered = 0; 	//BH_12-07-09 Most recent update of PercentBuffered
		MediaPlayerResumeTrackMode = false;

		if (position >= (DPApplication.Instance()._Tracklist.children.size()))
		{
			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "SwitchPlayTrack Position Out Of Bounds");
			return;
		}
		/*
		 * If we click on the track currently being played do not send any request to device proxy to play it again
		 * instead start it playing from beginning
		 *
		 */
		else if (position == SVC_trackcurrentindex)
		{
			CurrentPlayMode = PlayModes.Play;
			PlayFromBeginning();
			return;
		}
		else {

			try { //BH_01-12-10 added try/catch
				/*
				 * 05/07//2010
				 * The following message should not come in case of flyback track.
				 *
				 */
				newtrack = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.elementAt(position);
				if(!newtrack.flyback && ( newtrack.delayed || !newtrack.listened )){	
				try{
						Intent intent = new Intent(FlyCastPlayer.TRACK_NOT_AVAILABLE);
						sendBroadcast(intent);
						}catch(Exception e){
							if(Debug.isDebuggerConnected()) Log.d("TRACK_NOT_AVAILABLE", "Exception while BroadCasting the intent");
						}
				return;
				}
				/*
				 * 6/30/2010
				 * At this point we are sure that we are going to play the track so
				 * change the CurrentPlayMode to play
				 *
				 */
				CurrentPlayMode = PlayModes.Play;

			/* 05/07//2010
			 * The following code should be called after the above if loop
			 * because we do not want to mark a track as delayed = false when we can not play the currently selected
			 * track because it is delayed.
			 *
			 * Also keep in mind that it should not be a flyback track.
			 */
				if(DPApplication.Instance()._Tracklist.children.size() > position+1){
					DPXMLTrack	delayedTrack = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.get(position+1);
		    				delayedTrack.delayed=false;
		    				delayedTrack.listened = true;
				}

				/*
				 * Narinder 02/05/2010
				 * Set this track as current track in device proxy
				 *
				 */
			DPApplication.Instance().SetTrackAsCurrentForClient(position);
			} catch (Exception e) {
				return;
			}
			if ((newtrack!=null) && (newtrack.guidSong!=null) && (newtrack.flyback==false)){
				//This Track is a valid, normal, playable track
				SVC_trackcurrent = newtrack;
				SVC_trackcurrentindex = position;
				PlayguidSong = newtrack.guidSong;

				//IF BUFFERED, Start play on next track

				//if next Track already BUFFERED run PLAY_MEDIAPLAYERSET now, otherwise DevicProxyPollLoop will pick it up
				//BH_12-10-09 I think the DeviceProxy code will wait for buffering, no need to wait for message from DP
				if (SVC_trackcurrent.buffered==true){
					Message message = new Message();
			    	message.what = PLAY_MEDIAPLAYERSET;
			    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
				}

		    	//BROADCAST_TRACK_IS_SWITCHING
				Intent intent = new Intent(FlyCastPlayer.BROADCAST_TRACK_IS_SWITCHING);
				//intent.putExtra(Player.BROADCAST_TRACK_IS_SWITCHING_BODY_KEY, (new Integer(index + 1).toString()));
				intent.putExtra(FlyCastPlayer.BROADCAST_TRACK_IS_SWITCHING_BODY_KEY, (new Integer(newtrack.IndexInList).toString()));
				sendBroadcast(intent);
		    	return;
			}
			//else if ((newtrack!=null) && (newtrack.guidSong!=null) && (newtrack.flyback==true)){
			else if ((newtrack!=null) && (newtrack.flyback==true)){
				//This Track is a "Flyback"
				//Send FLYBACK command to DeviceProxy
				if (position == 0){
					FlybackID = "FB120";
					flyBackPosition=0;
				}
				else if (position == 1){
					FlybackID = "FB60";
					flyBackPosition=1;
				}
				else if (position == 2){
					FlybackID = "FB30";
					flyBackPosition=2;
				}
				else {
					return;	//illegal Flyback position
				}

				Intent intentFlyback = new Intent("DEVICEPROXY_FLYBACK");
				intentFlyback.putExtra("DEVICEPROXY_FLYBACK_INDEX", flyBackPosition);
				//intent.putExtra(Player.BROADCAST_MEDIAPLAYER_ERRORKEY, mStr);
				sendBroadcast(intentFlyback);

				DPXMLTrack lookUpTrack  = null;
				int x = flyBackPosition;
				while(true){
					DPXMLTrack t = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.elementAt(x);
					if(t.flyback){
						//SVC_tracklistcurrent.children.remove(x);
						x++;
					}
					else{
						lookUpTrack = t;
						break;
					}
				}

				_flybackIndex = flyBackPosition;
				_flybackLookupTrack = lookUpTrack;
				
    			//-- FLYBACK DeviceProxy call ---------

    			try
    			{
    				new DPCommandHandler().FlyBack(FlybackID.toString());
    			}
    			catch (Exception ex){
    				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "SwitchPlayTrack Flyback exception=" + ex.getMessage());
    			}
    		}	
		}

    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- BEGIN PlayStation (V2)  -----------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------

	//new AtomicBooleans default to false
	public AtomicBoolean PlayerWaitingForStart = new AtomicBoolean();	//true while Starting Play but not yet playing
	public AtomicBoolean PlayerIsPlaying = new AtomicBoolean();			//true when Track is actually playing
	public AtomicBoolean PlayerIsRecording = new AtomicBoolean();		//true when we are to record the playing station's tracks
	public AtomicBoolean PlayerStartBailout = new AtomicBoolean(); 		//true when Bailout is wanted
	public AtomicBoolean PlayerWaitingForPrepare = new AtomicBoolean();	//true while waiting for MediaPlayer to Prepare

    public Boolean DeviceProxyPollLoopActive = false;
    //public String guidSongWaitingToBuffer = null;	//this song/track waiting for TRACK_IS_BUFFERED from DeviceProxy


	protected static final int PLAY_CLIENTSERVICES_GET_NODE_DATA = 110001;
	protected static final int PLAY_DEVICEPROXY_START_STATION = 110002;
	protected static final int PLAY_DEVICEPROXY_STOP_STATION = 110003;

	protected static final int PLAY_DEVICEPROXY_POLL_LOOP_START = 110020;
	protected static final int PLAY_MEDIAPLAYERSET = 110021;
	protected static final int PLAY_MEDIAPLAYERPREPARE = 110022;
	protected static final int PLAY_MEDIAPLAYERSTART = 110023;
	//protected static final int PLAY_TRACK_COMPLETE = 1100024;

	protected static final int PLAY_TEST_CALL = 110051;

	protected static final int PLAY_START_ERROR = 120001;
	protected static final int PLAY_START_BAIL = 120002;

	protected static final int PLAYER_PREPARED = 120003;
	protected static final int PLAYER_ERROR = 120004;
	protected static final int PLAYER_TRACKLIST_ENDED = 120005;
	//PLAYER_TRACKLIST_ENDED

	protected static final int MEDIA_ERROR_UNKNOWN = 0x00000001;
	protected static final int MEDIA_ERROR_SERVER_DIED = 0x00000064;

	PlayStationLooper mPlayStationLooper;
	public String RcvValue = "";

    //-------------------------------------------------------------------------
    // PlayStationRemoteImpl - Start and Play Station using new "DeviceProxy"
	//-------------------------------------------------------------------------
    private String PlayStationRemoteImpl(String nodeid, String nodeshout, String userid) {
    	try
    	{
        	//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "NodeName=" + nodename);
        	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "New NodeID=" + nodeid);
        	//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATION, "NodeInfo=" + nodeinfo);

    		String zStr = DP_IsAlive();
    		if (zStr.equals("")!=true){
    			DeviceProxyIsAlive = false;
    			return zStr;
    		}
    		else
    			DeviceProxyIsAlive = true;

        	//--- AA - PlayStationRemoteImpl ------
        	StopCurrentStationImpl();	//Kill existing PLAY if already in progress

        	//now move ahead with the new PLAY
    		PlayNodeID = nodeid;		//PlayNodeID is a "Global" variable
    		PlayShoutUrl = nodeshout;
    		mUID = userid;

    		ReturnMessageTitle = "";//Initialize, no error indicated
    		ReturnMessageBody = "";	//Initialize, no error indicated

    		SVC_trackmaxplayed = 0;

    		//	SVC_tracklistcurrent = null;

    		//Default the control flags
    		PlayerWaitingForStart.set(true);	//true while Starting Play but not yet playing
    		PlayerIsPlaying.set(false);			//true when Track is actually playing
    		//PlayerIsRecording					//true when we are to record the playing station's tracks
    		PlayerStartBailout.set(false); 		//true when Bailout is wanted
    		PlayerWaitingForPrepare.set(false);	//true while waiting for MediaPlayer to Prepare
    	    DeviceProxyPollLoopActive = false;
    	    MediaPlayerResumeTrackMode=false;

    		MediaPlayerCurrentDuration = 0;			//BH_12-07-09 Total length of current track in milliseconds
    		MediaPlayerCurrentPosition = 0;		//BH_12-07-09 Most recent update of play position in milliseconds
    		MediaPlayerCurrentPercentBuffered = 0; 	//BH_12-07-09 Most recent update of PercentBuffered

    		mPlayStationLooper = new PlayStationLooper();
    		mPlayStationLooper.start();
    		Thread.sleep(200); //wait before sending message to mPlayStationLooper.mHandler, it chokes if we don't
//    		//mPlayStationLooper.isAlive() seems to NOT WORK, always returns "true"
//    		//Thread.sleep(200) above seems to fix the problem for now
    		if( PlayShoutUrl == null )
    		{
	    		Message message = new Message();
	    		message.what = PLAY_CLIENTSERVICES_GET_NODE_DATA;  //Step 1 of Play Start
	    		mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
    		}
    		else
    		{    			
    			FB30Url = FB60Url = FB120Url = "";
    			PlayUrlCurrent = PlayShoutUrl;
        		//Tell PlayStationLooper to run the next phase
        		Message message = new Message();
            	message.what = PLAY_DEVICEPROXY_START_STATION;
            	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
    		}
    	}
    	catch (Exception ex) {
    		//String poozle = ex.getMessage();
    		return "Init Exception: " + ex.getMessage();
    	}
    	return ""; //PlayStart Initialized with no error
    }

	private void PlayStartCancel() {
		//ZZZ UIPlayStartProgressClose();
		PlayerWaitingForStart.set(false);// = false;
		return;
	}

    public class PlayStationLooper extends Thread {
    	//public Boolean PlayerWaitingForPrepare = false;
        public Handler mHandler;
        public Looper mLooper;
        public MessageQueue mMessageQueue;
        public ClientServicesFetchNodeData mClientServicesFetchNodeData;
        public DeviceProxyStartStation mDeviceProxyStartStation;
        public DeviceProxyStopStation mDeviceProxyStopStation;
        public DeviceProxyPollLoopStart mDeviceProxyPollLoopStart;
        public MediaPlayerSet mMediaPlayerSet;
        public MediaPlayerPrepare mMediaPlayerPrepare;
        public MediaPlayerStart mMediaPlayerStart;
        //public PlayTrackComplete mPlayTrackComplete;
        
        PlayStationLooper() {
        	super("LocalService:PlayStationLooper");
        }
        
        public void run() {
            Looper.prepare();
			//Instantiate the handle and it will be tied to this thread
            mLooper = Looper.myLooper();
            mMessageQueue = Looper.myQueue();
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    //Process incoming messages here
                	//Message message = new Message();
        			switch (msg.what) { //msg.arg1
        			case PLAY_START_BAIL:	//BAIL OUT from this entire Play_Start
        				PlayerStartBailout.set(true); // = true;
        				PlayerWaitingForStart.set(false);// = false;
        				DeviceProxyPollLoopActive = false;
        				//ZZZ UIPlayStartProgressClose();

        				try
                        {
                        	setMediaPlayerImpl();
                        }
                        catch (Exception ex)
                        {
                        	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStartBail:Play cancelled resetting media player" + ex.getMessage());
                        }

//                      //REMOVED this Broadcast BH_11-19-09
//        				Intent intent = new Intent(Player.BROADCAST_PLAY_CANCELLED);
//        				sendBroadcast(intent);

         	    		//String mStr = null;
          				CurrentPlayMode = PlayModes.Stop; //BH_01-29-09
        				mLooper.quit();
         				break;
            		case PLAY_CLIENTSERVICES_GET_NODE_DATA:	//Step 1-Fetch current Node data for PlayNodeID to XMLNodeCurrent
            			//UIPlayStartProgressOpen();
                		mClientServicesFetchNodeData = new ClientServicesFetchNodeData();
                		mClientServicesFetchNodeData.run();
        				break;
            		case PLAY_DEVICEPROXY_START_STATION:	//Step 2-Do DeviceProxy STARTSTATION command
                		mDeviceProxyStartStation = new DeviceProxyStartStation();
                		mDeviceProxyStartStation.run();
        				break;
            		case PLAY_DEVICEPROXY_POLL_LOOP_START:	//Step 3-Start up DeviceProxyPollLoop thread
                		mDeviceProxyPollLoopStart = new DeviceProxyPollLoopStart();
                		mDeviceProxyPollLoopStart.run();
        				break;
        			case PLAY_MEDIAPLAYERSET:	//Step 4-ONCE TRACK IS BUFFERED, Set or Reset the Media Player, set PlayUrlCurrent, setMediaPlayerUrl
                		new Thread(new MediaPlayerSet(), "LocalService:MediaPlayerSet").start();
        				break;
            		case PLAY_MEDIAPLAYERPREPARE:	//Step 5-Prepare the Media Player
                		new Thread(new MediaPlayerPrepare()).start();
        				break;
        			case PLAYER_PREPARED:	//Step 5B Runs when BroadcastReceiver receives BROADCAST_MEDIAPLAYER_PREPARED
        				PlayerWaitingForPrepare.set(false);
                		new Thread(new MediaPlayerStart()).start();
        				break;
            		case PLAY_MEDIAPLAYERSTART:	//Step 6-START the Media Player
            			//THIS CASE CURRENTLY NOT IN USE, PLAYER_PREPARED runs PlayStart4 now
                		mMediaPlayerStart = new MediaPlayerStart();
                		mMediaPlayerStart.run();
        				break;
            		case PLAY_DEVICEPROXY_STOP_STATION:	//Start DeviceProxyPollLoop Thread
                		mDeviceProxyStopStation = new DeviceProxyStopStation();
                		mDeviceProxyStopStation.run();
        				break;
        			case PLAY_TEST_CALL:
        				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStationLooper.mHandler PLAY_TEST_CALL");
        				break;
        			case PLAYER_ERROR:	//
        				PlayerStartBailout.set(true); // = true;
        				PlayerWaitingForStart.set(false);// = false;
        				ReturnMessageTitle = "Media Player Error";
        				if ((RcvValue.equals(null))||(RcvValue.equals(""))){
        					RcvValue = "No Return Code";
        				}
        				ReturnMessageBody = RcvValue;
        				ShowFailedDialog(); // Sends ReturnMessageTitle and ReturnMessageBody

        				//Added RS_02-25-09
                        try
                        {
                        	setMediaPlayerImpl();
                        }
                        catch (Exception ex)
                        {
                        	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayerError:Play cancelled resetting media player" + ex.getMessage());
                        }

        				mLooper.quit();
         				break;
        			case PLAYER_TRACKLIST_ENDED:	//
        				StopCurrentStationImpl();
        				ReturnMessageTitle = "No more tracks found";
        				if ((RcvValue.equals(null))||(RcvValue.equals(""))){
        					RcvValue = "No Return Code";
        				}
        				ReturnMessageBody = RcvValue;
        				ShowFailedDialog(); // Sends ReturnMessageTitle and ReturnMessageBody
        				mLooper.quit();
         				break;
        			}
                }
            };
			Looper.loop();
        }
    }

    public void fillAdZones()
    {
    	if( XMLNodeCurrent == null ) return;
    	
    	DPXMLTracklist tracklist = getCurrentTracklist();
    	tracklist.station = PlayStationNameCurrent;
    	tracklist.adbannerzone = XMLNodeCurrent.adbannerzone;
    	tracklist.adbannerwidth = XMLNodeCurrent.adbannerwidth; 
    	tracklist.adbannerheight = XMLNodeCurrent.adbannerheight;
    	tracklist.adbannerfreq = XMLNodeCurrent.adbannerfreq;
    	tracklist.adprerollzone = XMLNodeCurrent.adprerollzone; 
    	tracklist.adprerollwidth = XMLNodeCurrent.adprerollwidth;
    	tracklist.adprerollheight = XMLNodeCurrent.adprerollheight; 
    	tracklist.adprerollfreq = XMLNodeCurrent.adprerollfreq; 
    	tracklist.adpopupzone = XMLNodeCurrent.adpopupzone;
    	tracklist.adpopupwidth = XMLNodeCurrent.adpopupwidth;
    	tracklist.adpopupheight = XMLNodeCurrent.adpopupheight; 
    	tracklist.adpopupfreq = XMLNodeCurrent.adpopupfreq;
    	tracklist.adinterzone = XMLNodeCurrent.adinterzone;
    	tracklist.adinterwidth = XMLNodeCurrent.adinterwidth;
    	tracklist.adinterheight = XMLNodeCurrent.adinterheight; 
    	tracklist.adinterfreq = XMLNodeCurrent.adinterfreq;
    	tracklist.adsignupzone = XMLNodeCurrent.adsignupzone;
    	tracklist.adsignupwidth = XMLNodeCurrent.adsignupwidth;
    	tracklist.adsignupheight = XMLNodeCurrent.adsignupheight; 
    	tracklist.adsignupfreq = XMLNodeCurrent.adsignupfreq;
    }

    //--- PLAY Step 1-Fetch current Node data for PlayNodeID to XMLNodeCurrent ---------
    class ClientServicesFetchNodeData implements Runnable {
		public void run() {
			XMLObject tObj;

			try {
				//--- 1 - ClientServicesFetchNodeData ------
				if (PlayerStartBailout.get() == true) {
	    			PlayStartCancel();
	    			return;
	    		}

				//check current connection
				if (BaseQueryStringUpdate()!="") {
					PlayStartCancel();
	    			ReturnMessageTitle = "Connection Error";
	    			ReturnMessageBody = "No network connectivity, restart FlyCast when connected.";
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1" + ReturnMessageTitle);
	    		    //SendGUIUpdateMessage();
	    			ShowFailedDialog(); //BH_12-04-09
					return;
				}

				//---  ---------
				//BH_09-2-09 Changed to "&FEED=PLAY..."  (was "&FEED=DIR...")
	    		HttpGet getMethod=new HttpGet(FlyCastClientServicesBaseUrl + BaseQueryString + "&FEED=PLAY&ID=" + PlayNodeID);
//	        	String s1 = android.os.Build.BRAND;		//G1 Example: "tmobile"
//	    		String s2 = android.os.Build.MODEL; 	//G1 Example: "T-Mobile G1"
//	    		String s3 = android.os.Build.DEVICE;	//G1 Example: "dream"
//	    		String s4 = android.os.Build.PRODUCT;	//G1 Example: "kila"
//	    		String s5 = android.os.Build.BOARD;  	//G1 Example: "trout"
//	    		String s6 = android.os.Build.VERSION.RELEASE;	//G1 Example: "1.1"
	        	getMethod.addHeader("User-Agent", "FlyCast/" + mPackageInfo.versionName
	        			+ " (Android; [" + android.os.Build.MODEL + "] [Build " + android.os.Build.VERSION.RELEASE + "])");//BH_02-23-09
	    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
	    		mClient = new DefaultHttpClient();
	    		String responseBody=mClient.execute(getMethod, responseHandler);
	    		mXMLParser = new XMLParser(responseBody);
	    		mXMLParser.parse();

	    		// result is in parser.directory -- null if no top level <DIR> xml node
	    		if ((mXMLParser.directory == null)) {
	    			PlayStartCancel();
	    			ReturnMessageTitle = "Problem Connecting";
	    			ReturnMessageBody = PLAY_ERROR_BODY + " (2)";
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1" + ReturnMessageTitle + " (2)");
	    			ShowFailedDialog();
	    			return;
	    		}
	    		if (PlayerStartBailout.get() == true) {
	    			PlayStartCancel();
	    			//UIPlayStartProgressClose();
	    			//PlayerWaitingForStart.set(false);// = false;
	    			return;
	    		}

	    		//Handle this situation: (reported by Narinder 12-11-09)
	    		//While playing somestations in file FlycastServiceRemote line 1582 throws classcastexception
	    		//<XML><FAIL message="Unable to find NetStationID 185399 in the database"></FAIL></XML>

	    		//mXMLParser.
	    		if (mXMLParser.directory.children.size()==0) {
	    			PlayStartCancel();
	    			ReturnMessageTitle = "Problem Connecting";
	    			ReturnMessageBody = PLAY_ERROR_BODY + " Empty Directory" ;
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1" + ReturnMessageTitle + " Empty Directory");
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1" + " responseBody=" + responseBody);
	    			ShowFailedDialog();
	    		    return;
	    		}
	    		tObj = (XMLObject) mXMLParser.directory.children.elementAt(0);
	    		if (tObj.type == XMLObject.FAIL) {
	    			XMLFail tFail = (XMLFail) tObj;
	    			//tFail.message
	    			PlayStartCancel();
	    			ReturnMessageTitle = "Problem Connecting";
	    			ReturnMessageBody = PLAY_ERROR_BODY + " " + tFail.message;
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1 " + tFail.message);
	    			ShowFailedDialog();
	    		    return;
	    		}
	    		else if (tObj.type == XMLObject.NODE) {
		    		//XMLNodeCurrent = (XMLNode) mXMLParser.directory.children.elementAt(0);
		    		XMLNodeCurrent = (XMLNode) tObj;
		    		PlayNodeID = XMLNodeCurrent.nodeid;
	    			PlayUrlCurrent = (String)XMLNodeCurrent.nodeurl; // The "REAL" Url to use
	    			//PlayUrlCurrent += "&RS=0"; //REMOVED THIS BH_12-23-09 per Marc Discussion
	    			
	    			PlayStationNameCurrent = (String)XMLNodeCurrent.nodename;
					Intent intent = new Intent(FlyCastPlayer.BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE);
					intent.putExtra(FlyCastPlayer.BROADCAST_DEVICEPROXY_PLAYSTATIONNAME_UPDATE_BODY_KEY, PlayStationNameCurrent);
					sendBroadcast(intent);
	    		}
	    		else {
	    			PlayStartCancel();
	    			ReturnMessageTitle = "Problem Connecting";
	    			ReturnMessageBody = PLAY_ERROR_BODY + " Station Node not returned";
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1" + " Station Node not returned");
	    			ShowFailedDialog();
	    			return;
	    		}

	    		if (mXMLParser.directory.children.size()==4) {
		    		//FB30Url
		    		tObj = (XMLObject) mXMLParser.directory.children.elementAt(1);
		    		if (tObj.type == XMLObject.NODE){
			    		XMLNodeCurrent = (XMLNode) tObj;
			    		FB30Url = (String)XMLNodeCurrent.nodeurl;
			    		FB30Name =  (String)XMLNodeCurrent.nodename;
		    		}
		    		else FB30Url = "";

		    		//FB60Url
		    		tObj = (XMLObject) mXMLParser.directory.children.elementAt(2);
		    		if (tObj.type == XMLObject.NODE){
			    		XMLNodeCurrent = (XMLNode) tObj;
			    		FB60Url = (String)XMLNodeCurrent.nodeurl;
			    		FB60Name =  (String)XMLNodeCurrent.nodename;
		    		}
		    		else FB60Url = "";

		    		//FB120Url
		    		tObj = (XMLObject) mXMLParser.directory.children.elementAt(3);
		    		if (tObj.type == XMLObject.NODE){
			    		XMLNodeCurrent = (XMLNode) tObj;
			    		FB120Url = (String)XMLNodeCurrent.nodeurl;
			    		FB120Name =  (String)XMLNodeCurrent.nodename;
		    		}
		    		else FB120Url = "";
	    		}
	    		else {
	    			FB30Url = "";
	    			FB60Url = "";
	    			FB120Url = "";
	    			FB30Name = "";
	    			FB60Name = "";
	    			FB120Name = "";
	    		}
	    	}
	    	catch (Exception ex) {
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1 Exception: " + ex.getMessage());
	    		ReturnMessageTitle = "Problem Connecting";
	    		ReturnMessageBody = PLAY_ERROR_BODY + (char)10 + (char)10 + "PlayStart1: " + ex.getMessage();
	    		//SendGUIUpdateMessage();
	    		ShowFailedDialog(); //BH_12-04-09
	    		PlayStartCancel();
				//PlayerWaitingForStart.set(false);// = false;
				//UIPlayStartProgressClose();
	    		return;
	    	}
    		//Tell PlayStationLooper to run the next phase
    		Message message = new Message();
        	message.what = PLAY_DEVICEPROXY_START_STATION;
        	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
	    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart1 ClientServicesFetchNodeData Complete");
	   } //public void run() {
    }
    
    public void StartPlay()
    {
		try {
			Thread.sleep(1000*30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(!receivedbufferMessage && startStationCalled){
			if(Debug.isDebuggerConnected()) Log.d("ResumeMode", "stopping station ..");
			ShowFailedDialog();
			StopCurrentStationImpl();
		}    	
    }    

    //--- PLAY Step 2-Do DeviceProxy STARTSTATION command ---------
    class DeviceProxyStartStation implements Runnable {
		public void run() {

			String mStr=null;
			DPXMLParser DPSSParser = null;

			//--- 2 - DeviceProxyStartStation ------
			if (PlayerStartBailout.get() == true) {
    			PlayStartCancel();
				//UIPlayStartProgressClose();
    			//PlayerWaitingForStart.set(false);// = false;
    			return;
    		}

			
			/*
    		 * 29July 2010
    		 * Start the thread here. This thread will keep track that we are able to start playing within 30 secinds or not
    		 * 
    		 */
				new Thread("LocalService:DeviceProxyStartStation") { public void run() { StartPlay(); } }.start();
			
				try {
	    			//-- STARTSTATION DeviceProxy call ---------
					startStationCalled = true;
					receivedbufferMessage  = false;
				
					if(savedTracklist.containsKey(PlayNodeID)){
						if(Debug.isDebuggerConnected()) Log.d("ResumeMode", "Resuming the station....");
						mStr = (new DPCommandHandler()).StartStation(PlayNodeID,savedTracklist.get(PlayNodeID));
					}
						
					else {
						if(savedTracklist.size()>0){
							if(Debug.isDebuggerConnected()) Log.d("ResumeMode", "HashTable size is.."+ savedTracklist.size());	
							savedTracklist.clear();
							if(Debug.isDebuggerConnected()) Log.d("ResumeMode", "Cleared hashTable ..");	
						}
						if(Debug.isDebuggerConnected()) Log.d("ResumeMode", "Starting station..");
						mStr = (new DPCommandHandler()).StartStation(PlayNodeID, mUID, PlayUrlCurrent, FB30Url,FB60Url, FB120Url);
					}					
	 	    	}
				catch (Exception ex) {
		    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "Some Exception caught");
		    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 SocketTimeoutException ");
		    		ReturnMessageTitle = "Problem Connecting Station";
		    		ReturnMessageBody = PLAY_ERROR_BODY + (char)10 + (char)10 + "PlayStart2 SocketTimeoutException";
		    		ShowFailedDialog(); //BH_12-04-09
		    		PlayStartCancel();
		    		return;
		    	}

	    	//-- test and Parse the string from DeviceProxy ---------
   			//DeviceProxyXMLStringCurrent = sb.toString();
			DeviceProxyXMLStringCurrent = mStr;
			if ((DeviceProxyXMLStringCurrent.equals("") == true)) {
    			PlayStartCancel();
    			ReturnMessageTitle = TAG_DPXMLParser + " Error";
    			ReturnMessageBody = "Empty response from DeviceProxy";
    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, ReturnMessageBody);
    			ShowFailedDialog(); //BH_12-04-09
    			return;
			}
			else {
				DPSSParser = new DPXMLParser(DeviceProxyXMLStringCurrent);
    			//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_STARTSTATION DPSSParser2=" + DPSSParser.toString());
    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 About to Parse");
    			String tStr = DPSSParser.parse();
	    		if ((tStr.equals("") != true)) {
	    			PlayStartCancel();
	    			ReturnMessageTitle = TAG_DPXMLParser + " Error";
	    			ReturnMessageBody = tStr;
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Parser returned " + tStr);
	    			ShowFailedDialog();
	    			return;
	    		}
	    		else if ((DPSSParser.messagelist.equals(null))) {
	    			PlayStartCancel();
	    			ReturnMessageTitle = TAG_DPXMLParser + " Parser Error";
	    			ReturnMessageBody = "Null Messagelist";
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Parser returned " + ReturnMessageBody);
	    		    //SendGUIUpdateMessage();
	    			ShowFailedDialog(); //BH_12-04-09
	    			return;
	    		}
	    		else if ((DPSSParser.messagelist.children.size()==0)){
	    			PlayStartCancel();
	    			ReturnMessageTitle = TAG_DPXMLParser + " Parser Error";
	    			ReturnMessageBody = "Empty Messagelist";
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Parser returned " + ReturnMessageBody);
	    		    //SendGUIUpdateMessage();
	    			ShowFailedDialog(); //BH_12-04-09
	    			return;
	    		}

	    		lastPlayedStation = PlayNodeID;
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Parsing OK");

    			//Now Process this messagelist locally
    	    	int i;
    			for (i=0; i<DPSSParser.messagelist.children.size(); i++) {
	    			DPXMLMessage messagecurrent = (DPXMLMessage) DPSSParser.messagelist.children.elementAt(i);
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES messagecurrent.name=" + messagecurrent.name);
	    			//messagecurrent.tracklist		//Can be NULL depending on Message Type
		    		//messagecurrent.track			//Can be NULL depending on Message Type
	    			//messagecurrent.guidSong		//Can be NULL depending on Message Type
		    		if (messagecurrent.name.equals(DPXMLParser.STR_TRACKLIST)){
						//Moved below 12-04-09 BroadcastTrackListNew(); // Broadcast DeviceProxyXMLStringCurrent to user of this service (FlyCast Player UI Activity)
						//DPXMLMessage messagecurrent = (DPXMLMessage) DPSSParser.messagelist.children.elementAt(i);


				DPXMLTracklist tracklist = null;
		    			try {
		    				tracklist = DPApplication.Instance().GetTrackList();
		    			}
		    			catch (Exception dpException) {
		    				tracklist = null;
		    			}

	    				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Got Tracklist");
						int size = tracklist.children.size();
						
						if(DPApplication.Instance().resumeMode){
						
							SVC_trackcurrent = (DPXMLTrack)DPApplication.Instance().GetTrackList().children.elementAt(tracklist.startindex);
							SVC_trackcurrentindex = tracklist.startindex;
						}
						else{
						for(int a = 0; a < size; a++){
							DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(a);
							if( temp.flyback )
								continue;

							SVC_trackcurrent = temp;
							SVC_trackcurrentindex = a;
							break;
						}
						}
						if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 Got Track");
						PlayguidSong = SVC_trackcurrent.guidSong;
			
						DPSSParser = null;

						BroadcastTrackListNew(); // Broadcast DeviceProxyXMLStringCurrent to user of this service (FlyCast Player UI Activity)

				        //-- BEGIN BH_12-03-09 ------
				        if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 tracklist.bitrate=" + DPApplication.Instance()._Tracklist.bitrate);
						SVC_tracklistcurrent_bitrate = DPApplication.Instance()._Tracklist.bitrate;
				        MinimumBytesBuffered = MinimumSecondsBuffered * SVC_tracklistcurrent_bitrate * 128;
				        if (MinimumBytesBuffered<65536) MinimumBytesBuffered = 65536;	//64KB
				        if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 tracklist.bitrate=" + SVC_tracklistcurrent_bitrate);
				        if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 MinimumBytesBuffered=" + MinimumBytesBuffered);

				        if (DPApplication.Instance().resumeMode && (SVC_trackcurrent!=null) && (SVC_trackcurrent.guidSong!=null) && (SVC_trackcurrent.buffered)){
				        	
				        	receivedbufferMessage = true;
				        	startStationCalled = false;
				        	//This Track is a valid, normal, playable track
							PlayguidSong = SVC_trackcurrent.guidSong;
							//IF BUFFERED, Start play on next track
								Message message = new Message();
						    	message.what = PLAY_MEDIAPLAYERSET;
						    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
					    }		
			    		//-- Tell PlayStationLooper to run the next phase
			    		Message message = new Message();
			        	message.what = PLAY_DEVICEPROXY_POLL_LOOP_START;
			        	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
				    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart2 DeviceProxyStartStation COMPLETE, bitrate=" + DPApplication.Instance()._Tracklist.bitrate);

				    	break;
		    		}
				}
			}
	   } 	//public void run()
    } 	//class DeviceProxyStartStation implements Runnable

    //--- PLAY Step 3-Start up DeviceProxyPollLoop thread ------------
    class DeviceProxyPollLoopStart implements Runnable {
    	public void run() {
			//--- 3 - DeviceProxyPollLoopStart ------
    		if (PlayerStartBailout.get() == true) {
    			PlayStartCancel();
				//UIPlayStartProgressClose();
    			//PlayerWaitingForStart.set(false);// = false;
    			return;
    		}
			new Thread(new DeviceProxyPollLoop(), "LocalService:DeviceProxyPollLoop").start();

    		//Tell PlayStationLooper to run the next phase
			//No sequential phase here, loop or user interaction will trigger next phase
	    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart3 DeviceProxyPollLoopStart Complete");
    	}	//public void run()
    }


    //--- PLAY Step 4-ONCE TRACK IS BUFFERED, Set or Reset the Media Player, set PlayUrlCurrent, setMediaPlayerUrl ---------

    class MediaPlayerSet implements Runnable {

    	public void run() {
			String mpsStr = null;
			try {


	    		if (PlayerStartBailout.get() == true) {
	    			PlayStartCancel();
	    			//UIPlayStartProgressClose();
	    			//PlayerWaitingForStart.set(false);// = false;
	    			return;
	    		}

    			//--- 4 - MediaPlayerSet ------
	    		//Added Retry Loop BH_11-13-09
	    		for (int index = 0; index < 3; index++){
	    			mpsStr = setMediaPlayerImpl(); //...or RESET if already there
	    			if (mpsStr.equals("")) {
	    				break;
	    			}
	    			Thread.sleep(1000);
	    		}

	    		if (mpsStr.equals("") != true) {//05/03/2010
    			/*	PlayStartCancel();
	    			ReturnMessageTitle = "Problem Setting Player";
        	   		ReturnMessageBody = "PlayStart4" + (char)10 + (char)10 + "SetMediaPlayer: " + mpsStr;
        	   		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 setMediaPlayer: " + mpsStr);
        	   		SendGUIUpdateMessage();*/
        	   		return;
    			}

    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 PlayUrlCurrent=" + PlayUrlCurrent);
    			if(Debug.isDebuggerConnected()) Log.d(TAG_SERVICE, "PlayStart4 PlayUrlCurrent=" + PlayUrlCurrent);

    			//-- PLAYTRACK DeviceProxy Url for Media Player ---------
    			//http://localhost:PORT/?CMD=PLAYTRACK&ID=xxx&UID=zzz&GUID=ggg
    			mpsStr =
    				DeviceProxyBaseUrl + deviceProxyAudioPort + "/?CMD=PLAYTRACK&ID="
    				+ PlayNodeID + "&UID=" + mUID + "&GUID=" + SVC_trackcurrent.guidSong + "&RESUME=";
    			if (MediaPlayerResumeTrackMode == false) {

    				/*
    				 * Send TrackInfo to Webview from here.
    				 * here we know that it is always a new track.
    				 *
    				 */
    				try{
						Intent intent = new Intent(FlyCastPlayer.SEND_TRACK_INFO_TO_WEBVIEW);
						sendBroadcast(intent);
						}catch(Exception e){
							if(Debug.isDebuggerConnected()) Log.d("SEND_TRACK_INFO_TO_WEBVIEW", "Exception while BroadCasting the intent");
						}

					mpsStr += "FALSE";
					if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "URL for new track;");

        			if(previousListenedTrack!=null){
        				previousListenedTrack.delayed=false;
        				previousListenedTrack.listened=true;
        				if(Debug.isDebuggerConnected()) Log.d("Listened", "GUID is "+previousListenedTrack.guidSong);
        			}
        			previousListenedTrack = SVC_trackcurrent;

    			}
    			else {
    				mpsStr += "TRUE";
        			if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "URL for resume track;");
        		}

    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 setMediaPlayer LocalUrl=" + mpsStr);
    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 setMediaPlayer guidSong=" + SVC_trackcurrent.guidSong);
    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 setMediaPlayer trackcurrent.bitrate=" + SVC_trackcurrent.bitrate);

    			String RetMessage = setMediaPlayerUrlImpl(mpsStr);
    			if (RetMessage.equals("") != true){
    				PlayStartCancel();
	    			ReturnMessageTitle = "Problem Setting Player";
	    			ReturnMessageBody = "PlayStart4" + (char)10 + (char)10 + "MediaPlayerSet: " + mpsStr;
        	   		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 setMediaPlayerUrl: " + mpsStr);
        	   		ShowFailedDialog();
    			}

    			
    			if(_Tracklist==null){
    			_Tracklist = DPApplication.Instance().GetTrackList();
	    		}else if(_Tracklist.children.size() > SVC_trackcurrentindex+1){
	    			
					if(Debug.isDebuggerConnected()) Log.d("FlyBack", "SVC_trackcurrentindex+1 is"+ SVC_trackcurrentindex+1);
					DPXMLTrack nextTrack = (DPXMLTrack) _Tracklist.children.elementAt(SVC_trackcurrentindex+1);
					nextTrack.listened = true;
					if(Debug.isDebuggerConnected()) Log.d("FlyBack", "next track listedned set to true");
				}
    			
	    		//Tell PlayStationLooper to run the next phase
				Message message = new Message();
		    	message.what = PLAY_MEDIAPLAYERPREPARE;
		    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
		    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 MediaPlayerSet Complete");
	    	}
	    	catch (Exception ex) {
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart4 Exception: " + ex.getMessage());
	    		ReturnMessageTitle = "Problem Setting Player";
	    		ReturnMessageBody = "PlayStart4" + (char)10 + (char)10 + "MediaPlayerSet: " + ex.getMessage();
	    		ShowFailedDialog();
	    		PlayStartCancel();
				//PlayerWaitingForStart.set(false);// = false;
				//UIPlayStartProgressClose();
	    		return;
	    	}
	   } 
    }

    //--- PLAY Step 5-Prepare the Media Player ---------
    class MediaPlayerPrepare implements Runnable {
		public void run() {
			String mppStr = null;
			try {
    			//--- 5 - MediaPlayerPrepare ------
				if (PlayerStartBailout.get() == true) {
 	    			PlayStartCancel();
    				//UIPlayStartProgressClose();
	    			//PlayerWaitingForStart.set(false);// = false;
	    			return;
	    		}

	    		PlayerWaitingForPrepare.set(true); //=true;

	    		//Added Retry Loop BH_11-13-09
	    		for (int index = 0; index < 3; index++){
	    			//mppStr = setMediaPlayerPrepareImpl();
	    			try {
						mppStr = setMediaPlayerPrepareAsynchImpl();	//Changed to Asynch BH_11-13-09
					} catch (Exception e) {
						//e.printStackTrace();
			    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart5 Retry Loop Exception: " + e.getMessage());
					}
	    			if (mppStr.equals("")) {
	    				break;
	    			}
	    			Thread.sleep(1000);
	    		}

    			if (mppStr.equals("") != true) {
    				PlayStartCancel();
	    			ReturnMessageTitle = "Problem Preparing Player";
	    			ReturnMessageBody = "PlayStart5" + (char)10 + (char)10 + "MediaPlayerPrepare: " + mppStr;
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart5 MediaPlayerPrepare: " + mppStr);
	    			ShowFailedDialog();
        	   		return;
    			}

 	    	}
	    	catch (Exception ex) {
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart5 Exception: " + ex.getMessage());
	    		ReturnMessageTitle = "Problem Preparing Player";
	    		ReturnMessageBody = "PlayStart5" + (char)10 + (char)10 + "MediaPlayerPrepare: " + ex.getMessage();
	    		ShowFailedDialog();
	    		PlayStartCancel();
				//PlayerWaitingForStart.set(false);// = false;
				//UIPlayStartProgressClose();
	    		return;
	    	}
	   } //public void run() {
    }

    //--- PLAY Step 6-START the Media Player ---------
    class MediaPlayerStart implements Runnable {
		public void run() {
			String mpsStr = null;
			try {
				
				fillAdZones();
				
	    		if (PlayerStartBailout.get() == true) {
	    			PlayStartCancel();
	    			return;
	    		}

				//--- 6 - MediaPlayerStart -------
				PlayStartCancel();
    			//PlayerWaitingForStart.set(false);// = false;
    			//UIPlayStartProgressClose();

	    		//Added Retry Loop BH_11-13-09
	    		for (int index = 0; index < 3; index++){
	    			mpsStr = setMediaPlayerStartImpl();
	    			if (mpsStr.equals("")) {
	    				break;
	    			}
	    			Thread.sleep(1000);
	    		}

    			if (mpsStr.equals("") != true) {
    				PlayStartCancel();
	    			ReturnMessageTitle = "Problem Starting Player";
	    			ReturnMessageBody = "PlayStart6" + (char)10 + (char)10 + "MediaPlayerStart: " + mpsStr;
	    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart6 MediaPlayerstart: " + mpsStr);
	    			ShowFailedDialog();
        	   		CurrentPlayMode = PlayModes.Stop;
        	   		return;
    			}

	    		CurrentPlayMode = PlayModes.Play;
	    		PlayerIsPlaying.set(true);
		    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart6 MediaPlayerStart Complete");

		    	//BH_11-23-09
				if (SVC_trackcurrentindex > SVC_trackmaxplayed)
				{SVC_trackmaxplayed = SVC_trackcurrentindex;}


				MediaPlayerCurrentDuration = svc_mediaplayer.getDuration();	//Gets the duration of the file in milliseconds
				if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart6 SVC_trackmaxplayed=" + SVC_trackmaxplayed + " MPCurrentDuration=" + MediaPlayerCurrentDuration);

		    	//BROADCAST_TRACK_IS_PLAYING
				Intent intent = new Intent(FlyCastPlayer.BROADCAST_TRACK_IS_PLAYING);
				intent.putExtra(FlyCastPlayer.BROADCAST_TRACK_IS_PLAYING_TRACKCURRENTINDEX_KEY, Integer.toString(SVC_trackcurrentindex));
				intent.putExtra(FlyCastPlayer.BROADCAST_TRACK_IS_PLAYING_GUIDSONG_KEY, SVC_trackcurrent.guidSong);
				sendBroadcast(intent);
				//mAppWidgetProvider.performUpdate(FlyCastServiceRemote.this, null);

	    		//PlayerWaitingForStart.set(false);
//    	        	SubPanelModeSet(SubPanelModes.Play, "");
//    	        	btnTopRightClear();
//    	        	//btnTopRightSet("Web");
	    	}
	    	catch (Exception ex) {
	    		if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "PlayStart6 Exception: " + ex.getMessage());
	    		ReturnMessageTitle = "Problem Starting Player";
	    		ReturnMessageBody = "PlayStart6" + (char)10 + (char)10 + "MediaPlayerStart: " + ex.getMessage();
	    		ShowFailedDialog();
	    		PlayStartCancel();
	    		CurrentPlayMode = PlayModes.Stop;
	    		return;
	    	}

	    	return;
	   } //public void run() {
    }

    //--- Stop Station [PlayNodeID] Play and return Tracklist of recorded tracks for THIS STATION ---
    class DeviceProxyStopStation implements Runnable {
		public void run() {
    		//-- STOPSTATION DeviceProxy call ---------
	    	return;
	   } //public void run() {
    }

    //--- Poll DeviceProxy for Status Updates and act accordingly ------
    class DeviceProxyPollLoop implements Runnable {
    	public void run() {
    		String mXMLString;
    		DPXMLParser DPPLParser;	//Parses XML from DeviceProxy
			try {
				DeviceProxyPollLoopActive = true;
				while (DeviceProxyPollLoopActive == true) {
					try{
					Thread.sleep(3000);
					String mStr = (new DPCommandHandler()).GetMessages();
	    			mXMLString = mStr;
	    			if (mXMLString.equals("")){
	    				//DeviceProxy returned NOTHING
	    				//HANDLE as ERROR??
	    			}
	    			else
	    			{
		    			//if ((tStr.equals("<XML></XML>")!=true)) {
			    			DPPLParser = new DPXMLParser(mXMLString);
			    			//if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES DPPLParser1=" + DPPLParser.toString());
			    			String tStg = DPPLParser.parse();
				    		if (tStg.equals("") != true) {
				    			PlayStartCancel();
				    			ReturnMessageTitle = TAG_DPXMLParser + " Error";
				    			ReturnMessageBody = tStg;
				    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES ERROR DPPLParser.parse returned " + tStg);
				    			ShowFailedDialog();
				    			return;
				    		}
				    		if (DPPLParser.messagelist != null) {
				    			//-- messagelist is VALID ---------
				    			//First BROADCAST this messagelist XML up to UI Activity
				    			Intent intent = new Intent(FlyCastPlayer.BROADCAST_DEVICEPROXY_MESSAGELIST);
				    			intent.putExtra(FlyCastPlayer.BROADCAST_DEVICEPROXY_MESSAGELIST_BODY_KEY, mXMLString);
				    			sendBroadcast(intent);

				    			//Now Process this messagelist locally
				    	    	int i;
				    			for (i=0; i<DPPLParser.messagelist.children.size(); i++) {
					    			DPXMLMessage messagecurrent = (DPXMLMessage) DPPLParser.messagelist.children.elementAt(i);
					    			if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES messagecurrent.name=" + messagecurrent.name);
					    			//messagecurrent.tracklist		//Can be NULL depending on Message Type
						    		//messagecurrent.track			//Can be NULL depending on Message Type
					    			//messagecurrent.guidSong		//Can be NULL depending on Message Type
						    		if (messagecurrent.name.equals(DPXMLParser.STR_RECORDING_HAS_FINISHED)){
						    			//Contains Message Name Only
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_RECORDING_HAS_STARTED)){
						    			//Contains Message Name Only
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_RETRYING_CONNECTION)){
						    			//Contains Message Name Only
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_STREAM_IS_BUFFERED)){
						    			//Contains Message Name Only
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_HAS_UPDATED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_BUFFERED)){
						    			
						    			/*
						    			 * 29 July 2010
						    			 * If after starting the station we do not receive buffer messege till 30 seconds
						    			 * call stopStation and send station error message to webView
						    			 * 
						    			 */
						    			if(startStationCalled){
						    				receivedbufferMessage = true;
						    				startStationCalled = false;
						    			}
						    			
						    			//Contains Message Name, guidsong
								    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES " + DPXMLParser.STR_TRACK_IS_BUFFERED +"1 messagecurrent.guidSong=" + messagecurrent.guidSong);
				        				boolean success = false;
						    			if( _flybackLookupTrack != null ){

						    				DPXMLTrack tempTrack = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.get(_flybackIndex);
						    				if(
				        						tempTrack != null &&
				        						tempTrack.IndexInList == _flybackIndex
				        						&& !_flybackLookupTrack.guidSong.equalsIgnoreCase(tempTrack.guidSong) ){
						    					tempTrack.buffered = true;
						    					_flybackLookupTrack = null;
						    					SwitchPlayTrackImpl(_flybackIndex );
						    					success = true;
				        					}
				        				}
						    			if( !success) {
							    			//Contains Message Name, guidsong
									    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES " + DPXMLParser.STR_TRACK_IS_BUFFERED +"1 messagecurrent.guidSong=" + messagecurrent.guidSong);
									    	for (int index = 0; index < DPApplication.Instance()._Tracklist.children.size(); index++) {
											DPXMLTrack tracktemp = null;
											try {	//BH_01-12-10
												tracktemp = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.get(index);
											} catch (Exception e) {
										    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES " + DPXMLParser.STR_TRACK_IS_BUFFERED +"2 EXCEPTION=" + e.getMessage());
											}
											if (tracktemp != null){	//BH_01-12-10
										    	if (messagecurrent.guidSong.equals(tracktemp.guidSong)){
													tracktemp.buffered = true;
													//if(messagecurrent.guidSong==guidSongWaitingToBuffer)
													if(messagecurrent.guidSong.equals(SVC_trackcurrent.guidSong)){
														//if we were waiting, go start the PLAY
												    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES " + DPXMLParser.STR_TRACK_IS_BUFFERED +"3 guidSong MATCH");
														Message message = new Message();
												    	message.what = PLAY_MEDIAPLAYERSET;
												    	mPlayStationLooper.mHandler.sendMessageAtFrontOfQueue(message);
													}
												}
										    	if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "DP_GETMESSAGES " + DPXMLParser.STR_TRACK_IS_BUFFERED +"2 tracktemp.guidSong=" + tracktemp.guidSong);
											}
									      }
										}
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_CACHED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_COVERED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_ENDED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_PAUSED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_PLAYING)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_RECORDING)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_IS_STARTING)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_WAS_ADDED)){
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACK_WAS_REMOVED)){
						    			//Contains Message Name, guidsong
						    		}
						    		else if (messagecurrent.name.equals(DPXMLParser.STR_TRACKLIST_EMPTY)){
						    			//Contains Message Only
						    		}
				    			}
				    		}
		    			//}
	    			}
				}
				//while (DeviceProxyPollLoopActive == true)
				catch(Exception ex){
					if(Debug.isDebuggerConnected()) Log.d(TAG_PLAYSTATIONREMOTE, "Here" + ex.toString());
				}
				}
 	    	}
	    	catch (Exception ex) {
	    	}
	    	finally {
	    	}

	    	DeviceProxyPollLoopActive = false;
		   return;
	   } //public void run() {
    }	//class MediaPlay implements Runnable {

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- END PlayStationRemote -------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------

	//--- Instantiate/Start our DeviceProxy WebServer ------------
    public class Run_DPWebServerStart implements Runnable {
        public Integer m_port = null;
        public String m_path = null;

        public Run_DPWebServerStart(String path, int port)
        {
        	m_path = path;
        	m_port = port;
        }

        public void run() {
      //      if(Debug.isDebuggerConnected()) Log.d(TAG_DPWEBSERVER, "Creating, Port " + DeviceProxyMessagingPort);
          //  DPWebServer dps = new DPWebServer(m_path, Integer.parseInt(DeviceProxyMessagingPort)); //
            DPWebServer dps = new DPWebServer(m_path, m_port); //
            try {
            	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPWEBSERVER, "DPServer Activating");
            	//Toast.makeText(m_RootContext, R.string.dpwebserver_starting, Toast.LENGTH_SHORT).show();
    			dps.activate();
    			if(Debug.isDebuggerConnected()) Log.d(TAG_DPWEBSERVER, "DPServer Activated");

    		} 
            
            catch (BindException e) {
			
				e.printStackTrace();
    			if(Debug.isDebuggerConnected()) Log.d(TAG_DPWEBSERVER, "DPWebServerStart Exception= " + e.getMessage());
            	//Toast.makeText(m_RootContext, R.string.dpwebserver_exception + " " + e.getMessage(), Toast.LENGTH_LONG).show();
    			//ShutDown();
				deviceProxyAudioPort += 1;
				startAudioDPWebServer(deviceProxyAudioPort);
    		}            
            
            catch (Exception e) {
    			e.printStackTrace();
    			if(Debug.isDebuggerConnected()) Log.d(TAG_DPWEBSERVER, "DPWebServerStart Exception= " + e.getMessage());
    		}
        }
    } //public class Run_DPWebServerStart implements Runnable {

    //-- Poll DeviceProxy to see if it is active ---------
    //		returns "" if all is well
    public String DP_IsAlive() {
    	try
		{
    			if(!DPMemoryStatus.externalMemoryAvailable()){
//             		ReturnMessageBody = LocalService.SDCARD_NOT_PRESENT;
//             		ShowFailedDialog();
//    				return ReturnMessageBody;
     				}
     			else{
     				if(DPMemoryStatus.getAvailableExternalMemorySize() < 4000000){
//     					ReturnMessageBody =LocalService.SDCARD_OUT_OF_MEMORY;
//     					ShowFailedDialog();
//        				return ReturnMessageBody;
     				}
     			  }
             	String mStr = (new DPCommandHandler()).IsAlive();

             	if (mStr.equals("") == true) {
    	    		ReturnMessageTitle = "DeviceProxy error, ";
    	    		ReturnMessageBody = "ALIVE call returned nothing.";
    	    		ShowFailedDialog();
    				return ReturnMessageBody;
    			}
    			if (mStr.indexOf(IS_ALIVE, 0)!=-1) {
    				return "";	//ALL IS WELL
    			}

    			ReturnMessageTitle = "DeviceProxy error, ";
    			ShowFailedDialog();
    			return ReturnMessageBody;

		}
		catch(Exception ex)
		{
			//tvAlert.setText("ALIVE returned: " + (char)10 + "IOException " + ex.getMessage());
    		ReturnMessageTitle = "DeviceProxy error, ";
    		ReturnMessageBody = "ALIVE call returned: " + "IOException " + ex.getMessage();
    		ShowFailedDialog();
			return ReturnMessageBody;
			//return "IOException " + ex.getMessage());
		}
		finally {
		//	connection.disconnect();
		}

    }

	public void BroadcastTrackListNew() {
		Intent intent = new Intent(FlyCastPlayer.BROADCAST_TRACKLIST_NEW);
		intent.putExtra(FlyCastPlayer.BROADCAST_TRACKLIST_NEW_BODY_KEY, DeviceProxyXMLStringCurrent);
		sendBroadcast(intent);
	}

	public void ShowFailedDialog() {
		//Send GUI message, including globals ReturnMessageTitle and ReturnMessageBody
		Intent intent = new Intent(FlyCastPlayer.BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED);
		intent.putExtra(FlyCastPlayer.BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_TITLE_KEY, "Station Error");
		intent.putExtra(FlyCastPlayer.BROADCAST_UI_UPDATE_MESSAGE_PLAYFAILED_BODY_KEY, "Error playing station. Please try again later");
		sendBroadcast(intent);
	}

	public void UIPlayStartProgressOpen(){
		Intent intent = new Intent(FlyCastPlayer.BROADCAST_UI_PLAYSTARTPROGRESS_OPEN);
		sendBroadcast(intent);
	}

    public void UIPlayStartProgressClose() {
		Intent intent = new Intent(FlyCastPlayer.BROADCAST_UI_PLAYSTARTPROGRESS_CLOSE);
		sendBroadcast(intent);
    }

    //-----------------------------------------------------------
    //BEGIN Functions Copied and adapted from DPApplication.java
    //-----------------------------------------------------------

	private DPXMLTracklist _Tracklist = null;
	private Hashtable<DPXMLTrack, DPTrackState> _TrackStateManager = new Hashtable<DPXMLTrack, DPTrackState>();

	public String getCurrentTracklistXMLImpl(){
		_Tracklist = DPApplication.Instance()._Tracklist;
		String retVal = "<XML>";
		retVal += "<ResourceContainer name=\"TrackList\">";
		retVal += "<Metadata>";
		retVal += GenerateTracklistParameters(_Tracklist);
		retVal += "</Metadata>";
		for(int i=0; i<_Tracklist.children.size(); i++)
		{
			DPXMLTrack track = (DPXMLTrack)_Tracklist.children.get(i);
			DPTrackState state = getTrackState(track);
			boolean b = false;
			try{
				b = state.AddStateSentToClient();
			}catch(Exception ex){
				System.err.println("State Err " + ex.toString());
			}
			if( !b ){
				retVal += "<Resource name = \"Track\">";
				retVal += GenerateTrackParameters(track);
				retVal += "</Resource>";
			}
			else
			{
				System.err.println("Should Not be here.");
			}
		}
		retVal += "</ResourceContainer>";
		//retVal += getAllMessagesString();
		retVal += "</XML>";

		return retVal;
	}

	public String GenerateTracklistParameters(DPXMLTracklist _Tracklist)
	{
		String retVal = "";
		retVal += "<key name=\"albumfile\" value=\"" + EscapeXML(_Tracklist.albumfile) + "\" />";
		retVal += "<key name=\"bitrate\" value=\"" + EscapeXML(_Tracklist.bitrate) + "\" />";
		retVal += "<key name=\"expdays\" value=\"" + EscapeXML(_Tracklist.expdays) + "\" />";
		retVal += "<key name=\"expplays\" value=\"" + EscapeXML(_Tracklist.expplays) + "\" />";
		retVal += "<key name=\"imageurl\" value=\"" + EscapeXML(_Tracklist.imageurl) + "\" />";
		retVal += "<key name=\"livemediaurl\" value=\"" + EscapeXML(_Tracklist.livemediaurl) + "\" />";

		retVal += "<key name=\"session\" value=\"" + EscapeXML(_Tracklist.session) + "\"/>";
		retVal += "<key name=\"startindex\" value=\"" + EscapeXML(_Tracklist.startindex) + "\" />";
		retVal += "<key name=\"station\" value=\"" + EscapeXML(_Tracklist.station )+ "\"/>";
		retVal += "<key name=\"stationid\" value=\"" + EscapeXML(_Tracklist.stationid) + "\" />";
		retVal += "<key name=\"stopGuid\" value=\"" + EscapeXML(_Tracklist.stopGuid) + "\" />";
		retVal += "<key name=\"timecode\" value=\"" + EscapeXML(_Tracklist.timecode) + "\" />";
		retVal += "<key name=\"type\" value=\"" + EscapeXML(_Tracklist.type )+ "\" />";
		retVal += "<key name=\"autohide\" value=\"" + EscapeXML(_Tracklist.autohide )+ "\" />";
		retVal += "<key name=\"autoplay\" value=\"" + EscapeXML(_Tracklist.autoplay )+ "\" />";
		retVal += "<key name=\"autoshuffle\" value=\"" + EscapeXML(_Tracklist.autoshuffle) + "\" />";
		retVal += "<key name=\"continuing\" value=\"" + EscapeXML(_Tracklist.continuing )+ "\" />";
		retVal += "<key name=\"deleteable\" value=\"" + EscapeXML(_Tracklist.deleteable) + "\" />";
		retVal += "<key name=\"flybacking\" value=\"" + EscapeXML(_Tracklist.flybacking) + "\" />";
		retVal += "<key name=\"flycasting\" value=\"" + EscapeXML(_Tracklist.flycasting) + "\"/>";
		retVal += "<key name=\"offline\" value=\"" + EscapeXML(_Tracklist.offline) + "\" />";
		retVal += "<key name=\"podcasting\" value=\"" + EscapeXML(_Tracklist.podcasting) + "\" />";
		retVal += "<key name=\"recording\" value=\"" + EscapeXML(_Tracklist.recording) + "\" />";
		retVal += "<key name=\"shoutcasting\" value=\"" + EscapeXML(_Tracklist.shoutcasting) + "\" />";
		retVal += "<key name=\"shuffleable\" value=\"" + EscapeXML(_Tracklist.shuffleable) + "\" />";
		retVal += "<key name=\"shuffled\" value=\"" + EscapeXML(_Tracklist.shuffled) + "\" />";
		retVal += "<key name=\"throwaway\" value=\"" + EscapeXML(_Tracklist.throwaway )+ "\" />";
		retVal += "<key name=\"users\" value=\"" + EscapeXML(_Tracklist.users) + "\" />";

		return retVal;
	}

	public synchronized String GenerateTrackParameters(DPXMLTrack track)
	{
		String retVal= "";
		retVal += "<key name=\"addart\" value=\"" + EscapeXML(track.addart) + "\" />";
		retVal += "<key name=\"adurl\" value=\"" + EscapeXML(track.adurl) + "\" />";
		retVal += "<key name=\"album\" value=\"" + EscapeXML(track.album) + "\" />";
		retVal += "<key name=\"albumfile\" value=\"" + EscapeXML(track.albumfile) + "\"/>";
		retVal += "<key name=\"artist\" value=\"" + EscapeXML(track.artist) + "\" />";
		retVal += "<key name=\"bitrate\" value=\"" + EscapeXML(track.bitrate) + "\" />";
		retVal += "<key name=\"current\" value=\"" + EscapeXML(track.current )+ "\" />";
		retVal += "<key name=\"expdays\" value=\"" + EscapeXML(track.expdays) + "\" />";
		retVal += "<key name=\"expplays\" value=\"" + EscapeXML(track.expplays )+ "\" />";
		retVal += "<key name=\"filename\" value=\"" + EscapeXML(track.filename) + "\" />";
		retVal += "<key name=\"guidIndex\" value=\"" + EscapeXML(track.guidIndex )+ "\" />";
		retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";

		if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Sent track with guid: " + EscapeXML(track.guidSong ));

		retVal += "<key name=\"imageurl\" value=\"" + EscapeXML(track.imageurl)+ "\" />";
		retVal += "<key name=\"length\" value=\"" + EscapeXML(track.length )+ "\" />";
		retVal += "<key name=\"mediatype\" value=\"" + EscapeXML(track.mediatype )+ "\" />";
		retVal += "<key name=\"mediaurl\" value=\"" + EscapeXML(track.mediaurl)+ "\" />";
		retVal += "<key name=\"metadata\" value=\"" + EscapeXML(track.metadata )+ "\" />";
		retVal += "<key name=\"numplay\" value=\"" + EscapeXML(track.numplay) + "\" />";
		retVal += "<key name=\"offset\" value=\"" + EscapeXML(track.offset )+ "\" />";
		retVal += "<key name=\"redirect\" value=\"" + EscapeXML(track.redirect)+ "\"/>";
		retVal += "<key name=\"seconds\" value=\"" + EscapeXML(track.seconds )+ "\" />";
		retVal += "<key name=\"start\" value=\"" + EscapeXML(track.start )+ "\" />";
		retVal += "<key name=\"starttime\" value=\"" + EscapeXML(track.starttime )+ "\" />";
		retVal += "<key name=\"stationid\" value=\"" + EscapeXML(track.stationid )+ "\" />";
		retVal += "<key name=\"syncoff\" value=\"" + EscapeXML(track.syncoff )+ "\" />";
		retVal += "<key name=\"timecode\" value=\"" + EscapeXML(track.timecode )+ "\" />";
		retVal += "<key name=\"title\" value=\"" + EscapeXML(track.title )+ "\" />";
		retVal += "<key name=\"type\" value=\"" + EscapeXML(track.type )+ "\" />";
		retVal += "<key name=\"audioAd\" value=\"" + EscapeXML(track.audioAd )+ "\" />";
		retVal += "<key name=\"buffered\" value=\"" + EscapeXML(track.buffered )+ "\" />";
		retVal += "<key name=\"cached\" value=\"" + EscapeXML(track.cached) + "\" />";
		retVal += "<key name=\"clickAd\" value=\"" + EscapeXML(track.clickAd )+ "\" />";
		retVal += "<key name=\"covered\" value=\"" + EscapeXML(track.covered )+ "\" />";
		retVal += "<key name=\"delayed\" value=\"" + EscapeXML(track.delayed )+ "\" />";
		retVal += "<key name=\"finished\" value=\"" + EscapeXML(track.finished )+ "\" />";
		retVal += "<key name=\"flush\" value=\"" + EscapeXML(track.flush )+ "\" />";
		retVal += "<key name=\"flyback\" value=\"" + EscapeXML(track.flyback )+ "\" />";
		retVal += "<key name=\"listened\" value=\"" + EscapeXML(track.listened )+ "\" />";
		retVal += "<key name=\"played\" value=\"" + EscapeXML(track.played )+ "\" />";
		retVal += "<key name=\"playing\" value=\"" + EscapeXML(track.playing )+ "\" />";
		retVal += "<key name=\"redirected\" value=\"" + EscapeXML(track.redirected )+ "\" />";
		retVal += "<key name=\"redirecting\" value=\"" + EscapeXML(track.redirecting )+ "\" />";
		retVal += "<key name=\"reloadAd\" value=\"" + EscapeXML(track.reloadAd )+ "\" />";
		retVal += "<key name=\"synced\" value=\"" + EscapeXML(track.synced) + "\" />";
		retVal += "<key name=\"terminating\" value=\"" + EscapeXML(track.terminating )+ "\" />";
		retVal += "<key name=\"unsupported\" value=\"" + EscapeXML(track.unsupported )+ "\" />";

		return retVal;
	}


	private String EscapeXML(Object obj){
		if( obj == null)
			return "";

		try
		{
			return DPUtility.EscapeXML(obj.toString());
		}
		catch(Exception ex){
			return "";
		}
	}

	private DPTrackState getTrackState(DPXMLTrack t){
		DPTrackState state = _TrackStateManager.get(t);
		if( state == null ){
			state = new DPTrackState();
			_TrackStateManager.put(t, state);
		}

		return state;
	}



    //-----------------------------------------------------------
    //END Functions Copied and adapted from DPApplication.java
    //-----------------------------------------------------------

	//appwidget stuff
    public static final String SERVICECMD = "com.appmobi.slimfit.musicservicecommand";
    public static final String TOGGLEPAUSE_ACTION = "com.appmobi.slimfit.togglepause";
    public static final String PAUSE_ACTION = "com.appmobi.slimfit.pause";
    public static final String PREVIOUS_ACTION = "com.appmobi.slimfit.previous";
    public static final String NEXT_ACTION = "com.appmobi.slimfit.next";
    public static final String LAUNCH_ACTION = "com.appmobi.slimfit.launchPlayer";

    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
    public static final String CMDLAUNCHPLAYER = "launchPlayer";

    public static final String PLAYSTATE_CHANGED = "com.appmobi.slimfit.playstatechanged";
    public static final String META_CHANGED = "com.appmobi.slimfit.metachanged";
    public static final String QUEUE_CHANGED = "com.appmobi.slimfit.queuechanged";
    public static final String PLAYBACK_COMPLETE = "com.appmobi.slimfit.playbackcomplete";
    public static final String ASYNC_OPEN_COMPLETE = "com.appmobi.slimfit.asyncopencomplete";

    static final String DESTROYED = "com.appmobi.slimfit.destroyed";


    private void initReceiver() {
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(LAUNCH_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra(CMDNAME);
            if(Debug.isDebuggerConnected()) Log.i("widget", "in service, received intent:" + action +"-"+cmd);
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                //next(true);
            	//mAppWidgetProvider.showToast(FlyCastServiceRemote.this, "Advancing track - please wait", 7);
            	LocalService.this.SwitchPlayTrackImpl(LocalService.this.SVC_trackcurrentindex+1);
            } else if (CMDLAUNCHPLAYER.equals(cmd) || LAUNCH_ACTION.equals(action)) {
            	//mAppWidgetProvider.showToast(FlyCastServiceRemote.this, "Launching player - please wait", 7);
            	LocalService.this.startActivity(new Intent(LocalService.this, AppMobiActivity.class));
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
             	LocalService.this.setMediaPlayerPauseToggleImpl();
             	//hack to get play/pause button updated
             	notifyChange(PLAYSTATE_CHANGED);
            	if (isPlaying()) {
            		//mAppWidgetProvider.showToast(FlyCastServiceRemote.this, "Pausing playback - please wait", 3);
                    //pause();
                } else {
                	//mAppWidgetProvider.showToast(FlyCastServiceRemote.this, "Resuming playback - please wait", 3);
                    //play();
                }
            }
        }
    };

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.android.music.metachanged"
     * "com.android.music.queuechanged",
     * "com.android.music.playbackcomplete"
     * "com.android.music.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {
        try {
/*	        Intent i = new Intent(what);
	        i.putExtra("id", Long.valueOf(getAudioId()));
	        i.putExtra("artist", getArtistName());
	        i.putExtra("album",getAlbumName());
	        i.putExtra("track", getTrackName());
	        sendBroadcast(i);

	        if (what.equals(QUEUE_CHANGED)) {
	            //saveQueue(true);
	        } else {
	            //saveQueue(false);
	        }*/

	        // Share this notification directly with our widgets
	        //mAppWidgetProvider.notifyChange(this, what);
	        //mAppWidgetProvider.performUpdate(this, null);
        } catch(Exception e) {
        	if(Debug.isDebuggerConnected()) Log.i("widget", "crashed in notifyChange", e);
        }
    }

	String getCurrentImageURL() {
		return SVC_trackcurrent.imageurl;
	}

	String getNextImageURL() {
		String imageurl = "";
		if ((SVC_trackcurrentindex + 1) < (DPApplication.Instance()._Tracklist.children.size())) {
			DPXMLObject tObj = null;
			try {	//BH_01-12-10
				tObj = (DPXMLObject) DPApplication.Instance()._Tracklist.children.elementAt(SVC_trackcurrentindex+1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (tObj.type == DPXMLObject.TRACK){
				DPXMLTrack nextTrack = (DPXMLTrack) tObj;
				if ((nextTrack!=null) && (nextTrack.guidSong!=null)){
					imageurl = nextTrack.imageurl;
				}
			}
		}

		return imageurl;
	}

	protected boolean isPlaying() {
		return CurrentPlayMode == PlayModes.Play;
	}

	public CharSequence getTrackName() {
		return SVC_trackcurrent.title;
	}

	public CharSequence getArtistName() {
		return SVC_trackcurrent.artist;
	}

	public CharSequence getStationName() {
		return PlayStationNameCurrent;
	}

	public String getPlayingGuid() {
		return PlayguidSong;
	}
	
	
	StopTimer stopTimerThread;
	boolean timerThreadRunning = false;
	
	public void  startTimerThread(int time){
		stopTimerThread = new StopTimer(time);
		stopTimerThread.start();
		timerThreadRunning = true;
	}
	
	public void  stopTimerThread(){
		stopTimerThread.stop();
		timerThreadRunning = false;
	}
	
	public int getTimeRemainingToStopPlayer(){
		return mTimerMaxValue;
	}
	

	public boolean isTimerThreadRunning(){
		return timerThreadRunning;
	}

	
	int mTimerMaxValue;
	
	 public class StopTimer extends Thread {
			//int timerCount = 0;
			public StopTimer(int maxTimer)
			{
				mTimerMaxValue = maxTimer;
			}
		
			public void  stopThread(){
				//timerStarted = false;
				stopTimerThread.stop();
				if(Debug.isDebuggerConnected()) Log.d("TimerThread", "TimerThread stop..");
			}
			
			public void run() 
			{			
				if(Debug.isDebuggerConnected()) Log.d("TimerThread", "TimerThread run..");	
				while(mTimerMaxValue > 0)
				{
					if(Debug.isDebuggerConnected()) Log.d("TimerThread","Timer time : " + mTimerMaxValue);
					try {					
						Thread.sleep(1000 * 60);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mTimerMaxValue--;
				}
				StopCurrentStationImpl();
				Intent intent = new Intent(FlyCastPlayer.MEDIAPLAYER_STOPPED_WITH_TIMER_THREAD);
				sendBroadcast(intent);
			} 
		}
}
