package com.appMobi.appMobiLib;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.TreeSet;

import android.media.MediaPlayer;
import android.media.MediaRecorder;


/*
 * This class is only compatible with Froyo and above
 */

public class AppMobiAudio extends AppMobiCommand {
	public static boolean debug = true;
	private TreeSet<String> recordingList; //++++Is this really needed?
	private MediaPlayer mediaPlayer = null;
	private MediaRecorder mediaRecorder = null;
    
	public AppMobiAudio(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
		recordingList = new TreeSet<String>();
	}

	//	Pass list of current recordings to Javascript.
    public void initRecordingList() {
    	File dir = new File(recordingDir());
		String[] children = dir.list();
		int cnt = children == null ? 0 : children.length;
        for (int i = 0; i < cnt; i++) {
        	// Add name to JavaScript list.
    		String js = String.format("javascript:AppMobi.recordinglist.push('%1$s');", children[i]);
    		injectJS(js);
    		if (debug)
    			System.out.println("initRecordingList: " + js);
        }
    }
	
	/*
	 * 	Record methods.
	 */
	private String recordingDir() {
		// Guessing here.
		return activity.appDir().toString() + "_recordings";
	}
	public void startRecording(String format, int samplingRate, int numChannels) {
		int encodeFormat = MediaRecorder.AudioEncoder.DEFAULT;
		
		if (format.toUpperCase().matches("AMR_NB"))
			encodeFormat = MediaRecorder.AudioEncoder.AMR_NB;
		if (mediaRecorder != null) {
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.record.busy',true,true);document.dispatchEvent(ev);");
			return;
		}
		mediaRecorder = new MediaRecorder();
		String dir = recordingDir();
		File dirFile = new File(dir);
		if (!dirFile.exists())
			dirFile.mkdirs();
		String filePath = null, baseName = null;
		File outFile;
		int i = 0;
		do {
			baseName = String.format("recording_%1$03d.amr", i++);
			filePath = String.format("%1$s/%2$s", dir, baseName);
			outFile = new File(filePath);
		} while (outFile.exists());
		mediaRecorder.setOnErrorListener(recordOnError);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mediaRecorder.setAudioEncoder(encodeFormat);
		mediaRecorder.setAudioChannels(numChannels);
		if (samplingRate > 0)
			mediaRecorder.setAudioSamplingRate(samplingRate);
		mediaRecorder.setOutputFile(filePath);
		try {
			mediaRecorder.prepare();
			mediaRecorder.start();   // Recording is now started
		} catch (Exception e) {
			if (debug) System.out.println("AppMobiAudio.startRecording err: " + e.getMessage());
			injectJS("javascript:var ev = document.createEvent('Events');" +
					"ev.initEvent('appMobi.audio.record.error',true,true);document.dispatchEvent(ev);");
			mediaRecorder.release();
			mediaRecorder = null;
			return;
		}
		if (debug) System.out.println("AppMobiAudio.startRecording: file = " + filePath);
		recordingList.add(filePath);
		injectJS("javascript:var ev = document.createEvent('Events');" +
				"ev.initEvent('appMobi.audio.record.start',true,true);document.dispatchEvent(ev);");
		// Add name to JavaScript list.
		String js = String.format("javascript:AppMobi.recordinglist.push('%1$s');", baseName);
		injectJS(js);
	}
	public void pauseRecording() {
		if (debug) System.out.println("AppMobiAudio.pauseRecording: not supported");
		injectJS("javascript:var ev = document.createEvent('Events');" +
				"ev.initEvent('appMobi.audio.pause.notsupported',true,true);document.dispatchEvent(ev);");
	}
	public void resumeRecording() {
		if (debug) System.out.println("AppMobiAudio.resumeRecording: not supported");
		injectJS("javascript:var ev = document.createEvent('Events');" +
				"ev.initEvent('appMobi.audio.resume.notsupported',true,true);document.dispatchEvent(ev);");
	}
	public void stopRecording() {
		if (debug) System.out.println("AppMobiAudio.stopRecording");
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.release();
			mediaRecorder = null;
			injectJS("javascript:var ev = document.createEvent('Events');" +
			"ev.initEvent('appMobi.audio.record.stop',true,true);document.dispatchEvent(ev);");
		}
	}
	private final String baseFromUrl(String url) {
		int ind = url.lastIndexOf('/');
		return (ind < 0 || ind == url.length() - 1) ? url : url.substring(ind + 1);
	}
	private final String fileFromUrl(String url) {
		return recordingDir() + "/" + baseFromUrl(url);
	}
	public void deleteRecording(String url) {
		String filePath = fileFromUrl(url);
		String baseName = baseFromUrl(url);
		if (debug) System.out.println("AppMobiAudio.deleteRecording: " + url);
		File f = new File(filePath);
		boolean removed = f.delete();
		if (removed) {	/* Update the dictionary. */
			recordingList.remove(filePath);
			String js = String.format(
						"var i = 0; " +
						"while (i < AppMobi.recordinglist.length)" + 
						"{ if (AppMobi.recordinglist[i] == '%1$s') " +
							"{ AppMobi.recordinglist.splice(i, 1); } " +
						 " else { i++; }};", baseName);
			injectJS("javascript:" + js);
			injectJS("javascript:var ev = document.createEvent('Events');" +
			"ev.initEvent('appMobi.audio.record.removed',true,true);document.dispatchEvent(ev);");
		} else {
			injectJS("javascript:var ev = document.createEvent('Events');" +
			"ev.initEvent('appMobi.audio.record.notRemoved',true,true);document.dispatchEvent(ev);");
		}
	}
	public void clearRecordings() {
		recordingList.clear();
		// Remove all the files.
		String dirName = recordingDir();
		File dir = new File(dirName);
		String[] children = dir.list();
		int cnt = children == null ? 0 : children.length;
        for (int i = 0; i < cnt; i++) {
            new File(dir, children[i]).delete();
        }
        injectJS("javascript:AppMobi.recordinglist = new Array();");
        injectJS("javascript:var ev = document.createEvent('Events');" +
		"ev.initEvent('appMobi.audio.record.clear',true,true);document.dispatchEvent(ev);");
	}
	private MediaRecorder.OnErrorListener recordOnError = new MediaRecorder.OnErrorListener(){
		//@Override
		public void onError(MediaRecorder mp, int what, int extra) {
			if (debug) System.out.println("AppMobiAudio.recordOnError: " + what);
			injectJS("javascript:var ev = document.createEvent('Events');" + 
					"ev.initEvent('appMobi.audio.record.error',true,true);document.dispatchEvent(ev);");
			mediaRecorder.release();
			mediaRecorder = null;
		}
    };    
	/*
	 * 	Play methods.
	 */
	public void startPlaying(String url) {
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) {
					injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.busy',true,true);document.dispatchEvent(ev);");
					return;
				}
			} catch (Exception e) { }
			mediaPlayer.release();
			mediaPlayer = null;
		}
		String path = fileFromUrl(url);
		if (debug) System.out.println("AppMobiAudio.startPlaying: " + path);
		File file = new File(path);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}		
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnErrorListener(soundOnError);
			mediaPlayer.setOnCompletionListener(soundOnComplete);		
			mediaPlayer.setDataSource(fis.getFD());
			mediaPlayer.prepare();
			mediaPlayer.start();
		}
		catch (Exception e) {
			if (debug) System.out.println("AppMobiAudio.startPlaying err: " + e.getMessage());
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.error',true,true);document.dispatchEvent(ev);");
			return;
		}		
		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.start',true,true);document.dispatchEvent(ev);");	
	}
	
	public void stopPlaying() {
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying())
					mediaPlayer.stop();
				mediaPlayer.release();
			} catch (Exception e) { 
				if (debug) System.out.println("AppMobiAudio.stopPlaying err: " + e.getMessage());
				System.out.println("AppMobiAudio.stopPlaying: " + e.getMessage());
			}
			mediaPlayer = null;
		}
		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.stop',true,true);document.dispatchEvent(ev);");
	}
	
	public void pausePlaying() {
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying())
					mediaPlayer.pause();
			} catch (Exception e) {
				if (debug) System.out.println("AppMobiAudio.pausePlaying err: " + e.getMessage());
				System.out.println("AppMobiAudio.pausePlaying: " + e.getMessage());
			}
		}
		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.pause',true,true);document.dispatchEvent(ev);");
	}
	
	public void continuePlaying() {
		if (mediaPlayer != null) {
			try {
				if (!mediaPlayer.isPlaying())
					mediaPlayer.start();
			} catch (Exception e) {
				if (debug) System.out.println("AppMobiAudio.continuePlaying err: " + e.getMessage());
				System.out.println("AppMobiAudio.continuePlaying: " + e.getMessage());
			}
		}
		injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.continue',true,true);document.dispatchEvent(ev);");
	}
	private MediaPlayer.OnErrorListener soundOnError = new MediaPlayer.OnErrorListener(){
		//@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			if (debug) System.out.println("AppMobiAudio.soundOnError: " + what);
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.error',true,true);document.dispatchEvent(ev);");
			return true;
		}
    };    

    private MediaPlayer.OnCompletionListener soundOnComplete = new MediaPlayer.OnCompletionListener(){
		//@Override
		public void onCompletion(MediaPlayer mp) {
			mediaPlayer.release();
			mediaPlayer = null;
			injectJS("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.audio.play.stop',true,true);document.dispatchEvent(ev);");
		}
    };
}
