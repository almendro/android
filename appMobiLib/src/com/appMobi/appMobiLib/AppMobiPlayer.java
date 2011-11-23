package com.appMobi.appMobiLib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import com.appMobi.appMobiLib.util.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import fm.flycast.DPMemoryStatus;
import fm.flycast.FlyCastPlayer;
import fm.flycast.FlyProgressView;

public class AppMobiPlayer extends AppMobiCommand {
	private boolean playingPodcast = false;
	//private boolean playingStation = false;
	private boolean playingAudio = false;
	public MediaPlayer mediaPlayer = null;
	public String soundName;
	
	final protected SoundPool soundPool;
    final private HashMap<String, Integer> soundPoolMapName2Id;
    final private HashMap<Integer, String> soundPoolMapId2Name;
    final private HashMap<String, Boolean> soundPoolShouldPlay;
    //private static AppMobiPlayer instance = null;
    private boolean hasLoadSoundCallback = false;
    private int PODCAST_REQUEST_CODE = 0;
    
    public static AppMobiPlayer getInstance(AppMobiActivity activity, AppMobiWebView webview) {
    	AppMobiPlayer instance = null;
    	//if(instance==null) {
			final boolean isFroyo = (Integer.parseInt(Build.VERSION.SDK) > 8 /*Build.VERSION_CODES.FROYO*/);
			if(isFroyo) {
				instance = new FroyoAppMobiPlayer(activity, webview);
				instance.hasLoadSoundCallback = true;
			} else {
				instance = new AppMobiPlayer(activity, webview);
			}
    	//}
    	return instance;
    }
    
	private AppMobiPlayer(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
		
        soundPool = new SoundPool(30, AudioManager.STREAM_MUSIC, 100);

        soundPoolMapName2Id = new HashMap<String, Integer>();
        soundPoolMapId2Name = new HashMap<Integer, String>();
        soundPoolShouldPlay = new HashMap<String, Boolean>();
		
	}

	private static class FroyoAppMobiPlayer extends AppMobiPlayer {

		public FroyoAppMobiPlayer(final AppMobiActivity activity, AppMobiWebView webview) {
			super(activity, webview);
			soundPool.setOnLoadCompleteListener(
				new SoundPool.OnLoadCompleteListener() {
					public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
						soundLoaded(sampleId, status);
					}
				}
			);
		}
		
	}
	
	@Override
	protected void stopCommand() {
		stop();
		stopAudio();
		//try to stop podcast
		activity.finishActivity(PODCAST_REQUEST_CODE);
	}
	
	
    public void show()
    {
    	Message message = new Message();
    	message.what = FlyCastPlayer.SHOW_PLAYER;
		activity.nonGUIUpdateHandler.sendMessage(message);
	}
    
    public void hide()
    {
    	Message message = new Message();
    	message.what = FlyCastPlayer.HIDE_PLAYER;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    public void playPodcast(final String strPodcastURL)
    {   
    	String fixedPodcastURL = null;
    	
    	if(isPlayingStation() || isPlayingPodcast() || isPlayingAudio()) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.podcast.busy');document.dispatchEvent(ev);");
    		return;
    	}
    	
    	if(strPodcastURL.startsWith("http://") == false) {
    		//try to find on local system
    		File podcast = new File(activity.appDir(), strPodcastURL);
    		if(podcast.exists()) {
    			//rewrite url to use localhost
    			//switch to webview.rootDir instead of building manually
    			StringBuffer rewrittenURL = new StringBuffer("http://localhost:58888/");
    			rewrittenURL.append(webview.config.appName);
    			rewrittenURL.append('/');
    			rewrittenURL.append(webview.config.releaseName);
    			if(strPodcastURL.charAt(0)!='/') rewrittenURL.append('/');
    			rewrittenURL.append(strPodcastURL);
    			
    			fixedPodcastURL = rewrittenURL.toString();
    		}
    	} else {
    		fixedPodcastURL = strPodcastURL;
    	}
    	
    	if( fixedPodcastURL == null ) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.podcast.error',true,true);document.dispatchEvent(ev);");
    		return;
    	}
    	
    	setPlayingPodcast(true);
    	injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.podcast.start',true,true);document.dispatchEvent(ev);");
    	Intent i = new Intent(activity, PodcastActivity.class);
    	i.putExtra("url",fixedPodcastURL);
    	activity.setLaunchedChildActivity(true);
    	activity.startActivityForResult(i, PODCAST_REQUEST_CODE);
    }
    
    public void startStation(String strStationID, boolean resumeMode, boolean showPlayer)
    {
		//check auth
    	if(webview.config!=null && !webview.config.hasStreaming) return;
    	
    	if(Debug.isDebuggerConnected()) Log.d("LifeCycle", "PhoneGap::loadStation");
    	if(isPlayingPodcast() || isPlayingAudio()) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.busy');document.dispatchEvent(ev);");
    		return;
    	} 
    	else if(activity.state.playStarted && strStationID!=null && FlyCastPlayer.stationID!=null && strStationID.equalsIgnoreCase(FlyCastPlayer.stationID) ){
    		if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "request to play same station returning...");
    		return;
    	} else if(!checkSDCard()) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.error');document.dispatchEvent(ev);");
    		return;    		
    	}
    	else if(strStationID.equals("")){
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.station.error');document.dispatchEvent(ev);");
    		return;
    	}
    	
    	activity.state.stationResumeMode = resumeMode;
    	
    	//strStationID = "224811"; // slimfit for testing
    	FlyCastPlayer.stationID = strStationID;
        	
    	//By Parveen - Set player visibility player accordingly here.
		activity.state.showPlayer = showPlayer;
		
    	Message message = new Message();
    	message.what = FlyCastPlayer.START_STATION;
		activity.nonGUIUpdateHandler.sendMessage(message);
    	setPlayingStation(true);    	
    }
    
    private MediaPlayer.OnErrorListener soundOnError = new MediaPlayer.OnErrorListener(){
		//@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.error',true,true);document.dispatchEvent(ev);");
			setPlayingAudio(false);
			return true;
		}
    };    

    private MediaPlayer.OnCompletionListener soundOnComplete = new MediaPlayer.OnCompletionListener(){
		//@Override
		public void onCompletion(MediaPlayer mp) {
			setPlayingAudio(false);
			mediaPlayer.release();
			mediaPlayer = null;
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.stop',true,true);document.dispatchEvent(ev);");
	        
			activity.trackPageView("/appMobi.podcast." + getAudioName(soundName) + ".stop");
		}
    };
    
    public int loadSound(String strRelativeFileURL) {
		String path = activity.appDir() + "/" + strRelativeFileURL;
		
		Integer id = soundPoolMapName2Id.get(strRelativeFileURL);
		
		if( id == null ) {
			id = soundPool.load(path, 1);
			//an id of 0 means unable to load the sound
			soundPoolMapName2Id.put(strRelativeFileURL, id);
			soundPoolMapId2Name.put(id, strRelativeFileURL);
			soundPoolShouldPlay.put(strRelativeFileURL, false);
		}
		
		return id;
    }
    
    protected void soundLoaded(int id, int status) {
    	String strRelativeFileURL = soundPoolMapId2Name.get(id);
    	boolean shouldPlay = soundPoolShouldPlay.get(strRelativeFileURL) && status==0;
    	
		if(shouldPlay) {
			playSound(strRelativeFileURL);
    	} else {
			StringBuffer js = new StringBuffer("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.sound.load',true,true);ev.name='");
			js.append(strRelativeFileURL);
			js.append("';e.success=");
			js.append(id!=0 && status==0);
			js.append("document.dispatchEvent(ev);");
	
			injectJS(js.toString());
    	}
    }
    
    public void unloadSound(String strRelativeFileURL) {
    	boolean didUnload = false;
		
		Integer id = soundPoolMapName2Id.get(strRelativeFileURL);
		if( id != null ) {
			didUnload = soundPool.unload(id.intValue());
			//didUnload is always false??
			soundPoolMapName2Id.remove(strRelativeFileURL);
			soundPoolMapId2Name.remove(id);
			soundPoolShouldPlay.remove(strRelativeFileURL);
		}
		
		StringBuffer js = new StringBuffer("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.sound.unload',true,true);ev.name='");
		js.append(strRelativeFileURL);
		js.append("';e.success=");
		js.append(didUnload);
		js.append("document.dispatchEvent(ev);");

		injectJS(js.toString());
    }
    
	public void playSound(final String strRelativeFileURL) {
		Integer id = soundPoolMapName2Id.get(strRelativeFileURL);

		if( id == null ) {
			String path = activity.appDir() + "/" + strRelativeFileURL;
			id = soundPool.load(path, 1);
			if( id == 0 ) {  // unable to load the sound
				return;
			}
			soundPoolMapName2Id.put(strRelativeFileURL, id);
			soundPoolMapId2Name.put(id, strRelativeFileURL);
			soundPoolShouldPlay.put(strRelativeFileURL, true);
			
			//hack for pre-froyo: wait 250 ms to play sound first time through
			if (!hasLoadSoundCallback) {
				final int soundId = id;
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					public void run() {
						soundLoaded(soundId, 0);
					}
					
				}, 250);
			}
		} else {	
	        AudioManager mgr = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
	        int streamVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	        int playingSound = soundPool.play(id.intValue(), streamVolume, streamVolume, 1, 0, 1f);
	        //should inject playingSound so it can be stopped/paused/resumed?
		}
	}

	public void startAudio(String strRelativeFileURL) {
		if (isPlayingStation() || isPlayingPodcast()) {
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.busy',true,true);document.dispatchEvent(ev);");
			return;
		} else if (isPlayingAudio()) {
			stopAudio();
		}
		
		soundName = strRelativeFileURL;
		String path = activity.appDir() + "/" + strRelativeFileURL;
		File file = new File(path);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}		

		try {
			try {
				if(mediaPlayer!=null) {
					mediaPlayer.release();
					mediaPlayer = null;
				}
			} catch(Exception e) {}
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnErrorListener(soundOnError);
			mediaPlayer.setOnCompletionListener(soundOnComplete);		
			mediaPlayer.setDataSource(fis.getFD());
			mediaPlayer.prepare();
			mediaPlayer.start();
		}
		catch (Exception e) {
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.error',true,true);document.dispatchEvent(ev);");
			return;
		}		
        
		activity.trackPageView("/appMobi.podcast." + getAudioName(soundName) + ".stop");
		
		setPlayingAudio(true);
		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.start');document.dispatchEvent(ev);");	
	}
	
	public void toggleAudio()
	{
		if( mediaPlayer != null )
		{
			if( mediaPlayer.isPlaying() ) mediaPlayer.pause();
			else mediaPlayer.start();
		}
	}
	
	public void stopAudio()
	{
		if( mediaPlayer != null )
		{
			try {
				mediaPlayer.stop();
				injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.audio.stop',true,true);document.dispatchEvent(ev);");
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		setPlayingAudio(false);
	}

    private String getAudioName(String url)
    {
    	String name = "";
    	int last = url.lastIndexOf("/");
    	if( last > 0 )
    	{
    		name = url.substring(last + 1);
    	}
    	
    	return name;
    }
    
    public void play()
    {    	
    	Message message = new Message();
    	message.what = FlyCastPlayer.PLAY;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    public void pause()
    {    	
    	Message message = new Message();
    	message.what = FlyCastPlayer.TOGGLE_PLAY_PAUSE;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    public void stop()
    {    	
    	setPlayingStation(false);
		activity.state.playStarted = false;
    	Message message = new Message();
    	message.what = FlyCastPlayer.PLAYER_STOP;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    boolean firstTime = true;
    int volPercen=-1; 
    public void volume(int iVolumePercentage)
    {    	
    	if(firstTime){
    		volPercen = iVolumePercentage;
    		firstTime =false;
    		return;
    	}
    	
    	//showMessage("playerFfwd: " + iVolumePercentage + "%");
    	if(iVolumePercentage>volPercen){    		
    		volPercen = iVolumePercentage;
    		Message message = new Message();
        	message.what = FlyCastPlayer.PLAYER_VOLUME_UP;
			activity.nonGUIUpdateHandler.sendMessage(message);
        	
    	}
    	else{
    		volPercen = iVolumePercentage;
    		
    		Message message = new Message();
        	message.what = FlyCastPlayer.PLAYER_VOLUME_DOWN;
			activity.nonGUIUpdateHandler.sendMessage(message);
    	}    	    
    }
    
    public void rewind()
    {
    	if( activity.flyCastPlayer == null ) return;
    	
    	Message message = new Message();
    	message.what = FlyCastPlayer.REWIND;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    public void ffwd()
    {
    	if( activity.flyCastPlayer == null ) return;
    	
    	Message message = new Message();
    	message.what = FlyCastPlayer.FORWARD;
		activity.nonGUIUpdateHandler.sendMessage(message);
    }
    
    public void setColors(String strBackColor, String strFillColor, String strDoneColor, String strPlayColor)
    {
    	FlyProgressView.setBackColor(makeColor(strBackColor));
    	FlyProgressView.setTintColor(makeColor(strFillColor));
    	FlyProgressView.setMarkColor(makeColor(strPlayColor));
    	FlyProgressView.setDoneColor(makeColor(strDoneColor));
    }

    private int makeColor(String hexColor)
    {
    	//trim and upper
    	hexColor = hexColor.trim().toUpperCase();
    	
    	//strip #
    	if (hexColor.startsWith("#")) 
    		hexColor = hexColor.substring(1);

    	// if the value isn't 6 characters at this point return 
    	// the color black	
    	if (hexColor.length() != 6) 
    		return 0;  
    	
    	hexColor = "FF" + hexColor;
    	
    	return (int)Long.parseLong(hexColor, 16);
    }
    
    // Merely stubbed for now -- Allows setting the position of the player screen on large tablets
    // May be not implemented on Android based on the way Activity and screens work
    public void setPosition(int portraitX, int portraitY, int landscapeX, int landscapeY) {
    }
    
    public void startShoutcast(String strShoutcastURL, boolean showPlayer)
    {
		//check auth
    	if(webview.config!=null && !webview.config.hasStreaming) return;
    	
    	// Need new code to make this work
    	if(Debug.isDebuggerConnected()) Log.d("LifeCycle", "PhoneGap::startShoutcast");
    	if(isPlayingPodcast() || isPlayingAudio()) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.busy');document.dispatchEvent(ev);");
    		return;
    	} 
    	else if(activity.state.playStarted && strShoutcastURL!=null && FlyCastPlayer.shoutcastURL!=null && strShoutcastURL.equalsIgnoreCase(FlyCastPlayer.shoutcastURL) ){
    		if(Debug.isDebuggerConnected()) Log.d("ShoutCast", "request to play same station returning...");
    		return;
    	} else if(!checkSDCard()) {
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.error');document.dispatchEvent(ev);");
    		return;    		
    	}
    	else if(strShoutcastURL.equals("")){
    		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.shoutcast.error');document.dispatchEvent(ev);");
    		return;
    	}
    	
    	FlyCastPlayer.shoutcastURL = strShoutcastURL;
        	
    	//By Parveen - Set player visibility player accordingly here.
		activity.state.showPlayer = showPlayer;
		
    	Message message = new Message();
    	message.what = FlyCastPlayer.START_SHOUTCAST;
		activity.nonGUIUpdateHandler.sendMessage(message);
    	setPlayingStation(true);    	
    }

	public void setPlayingStation(boolean playingStation) {
		activity.state.playStarted = playingStation;
		//this.playingStation = playingStation;
	}

	public boolean isPlayingStation() {
		return activity.state.playStarted;
		//return playingStation;
	}

	public void setPlayingPodcast(boolean playingPodcast) {
		this.playingPodcast = playingPodcast;
	}

	public boolean isPlayingPodcast() {
		return playingPodcast;
	}

	public void setPlayingAudio(boolean playingAudio) {
		this.playingAudio = playingAudio;
	}

	public boolean isPlayingAudio() {
		return playingAudio;
	}

	private boolean checkSDCard() {
		boolean ok = true;
		String title = null, message = null;
		if(!DPMemoryStatus.externalMemoryAvailable()){
			ok = false;
			title = "No SD card";
			message = "Streaming is not available because this device does not have an SD card.\nInstall an SD card to enable streaming.";
		} else if (DPMemoryStatus.getAvailableExternalMemorySize() < 4000000){
			ok = false;
			title = "SD card full";
			message = "Streaming is not available because the SD card installed in this device is full.\nFree some space on the SD card to enable streaming.";
		}
		
		if(!ok) {
			new AlertDialog.Builder(activity)
			.setTitle(title)
			.setMessage(message)
			.setNeutralButton("Close",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
			}).show();
		}
		
		return ok;
	}
}
