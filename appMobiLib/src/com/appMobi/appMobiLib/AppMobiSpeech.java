package com.appMobi.appMobiLib;

import android.os.Handler;
import android.util.Log;

import com.appMobi.appMobiLib.util.Debug;
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechKit;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.Vocalizer;

public class AppMobiSpeech extends AppMobiCommand {

	private Recognizer _recnuance;
	private Vocalizer _vocnuance;
	private Object _lastTtsContext = null;
	
    private Handler _handler = null;
    private  Recognizer.Listener _listener = null;
    
	private boolean bCancelled;
	private boolean bRecording;
	private boolean bBusy;
	
	//private String voice;
	//private String language;
	//private String text;
	//private Recognizer.EndOfSpeechDetection detection;

	public AppMobiSpeech(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
		
		webview.config.hasSpeech = true;
	
	}
	
	private Recognizer.Listener createListener()
    {
        return new Recognizer.Listener()
        {            
            @Override
            public void onRecordingBegin(Recognizer recognizer) 
            {
            }

            @Override
            public void onRecordingDone(Recognizer recognizer) 
            {

            	bRecording = false;
            	String suc = (bCancelled == false)?"true":"false";
            	String can = (bCancelled == true)?"true":"false";
            	String js = String.format("javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.record',true,true);e.success=%s;e.cancelled=%s;document.dispatchEvent(e);", suc, can);
            	injectJS(js);	
            }

            @Override
            public void onError(Recognizer recognizer, SpeechError error) 
            {
            	
                //[_recnuance release];
            	_recnuance = null;
            	String suggestion = error.getSuggestion();
            	String can = (bCancelled == true)?"true":"false";
            	String sug = (suggestion == null)?"":suggestion;
            	String js =String.format("javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.recognize',true,true);e.success=false;e.results=[];e.suggestion='%s';e.error='%s';e.cancelled=%s;document.dispatchEvent(e);", sug, error.getErrorDetail(), can);
            	
            	injectJS(js);
            	bBusy = false;
            }

            @Override
            public void onResults(Recognizer recognizer, Recognition results) {

               // [_recnuance release];
            	_recnuance = null;
            	
            	String jsResults = "[";
                int count = results.getResultCount();
                Recognition.Result rs;
                for (int i = 0; i < count; i++)
                {
                	 rs = results.getResult(i);     	 
                	 jsResults += String.format("{ text:'%s', score:%d }, ", rs.getText().replaceAll("'", "\\\\'"), rs.getScore());
                }
                jsResults += "]";
                if(Debug.isDebuggerConnected()) Log.i("SPEECH RESULTS",jsResults);
               
                String suc = (count > 0)?"true":"false";
            	String can = (bCancelled == true)?"true":"false";
            	String sug = (results.getSuggestion() == null)?"":results.getSuggestion();
            	String js = String.format("javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.recognize',true,true);e.success=%s;e.results=%s;e.suggestion='%s';e.error='';e.cancelled=%s;document.dispatchEvent(e);", suc, jsResults, sug, can);
            	injectJS(js);
            	bBusy = false;
            	
     
            }
        };
    }
	
	public void recognize(boolean longPause, String language)
	{
		if(!webview.config.hasSpeech) return;
		
		if( bBusy == true || _vocnuance != null || _recnuance != null )
		{
			String js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}
		_listener = createListener();
	
		bRecording = true;
		bCancelled = false;
		bBusy = true;
	
		int speechDetectionType = (longPause==true)?Recognizer.EndOfSpeechDetection.Long:Recognizer.EndOfSpeechDetection.Short;
	
		_recnuance = AppMobiActivity._speechKit.createRecognizer(Recognizer.RecognizerType.Search, speechDetectionType, language, _listener, _handler);
		_recnuance.start();
        
	}
	
	//Stop Recording
	public void stopRecording()
	{
		if(!webview.config.hasSpeech) return;
		
		if( _recnuance != null && bRecording == true )
			_recnuance.stopRecording();
	}
	
    // Create Vocalizer listener
    Vocalizer.Listener vocalizerListener = new Vocalizer.Listener()
    {
        @Override
        public void onSpeakingBegin(Vocalizer vocalizer, String text, Object context) {
           
        }

        @Override
        public void onSpeakingDone(Vocalizer vocalizer, String text, SpeechError error, Object context) 
        {
            // Use the context to detemine if this was the final TTS phrase     	
            //[_vocnuance release];
        	_vocnuance.cancel();
        	_vocnuance = null;
        	
        	String suc = (error == null)?"true":"false";
        	String can = (bCancelled == true)?"true":"false";
        	String err = (error == null)?"":error.toString();//[error localizedDescription];
        	String js = String.format("javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.vocalize',true,true);e.success=%s;e.error='%s';e.cancelled=%s;document.dispatchEvent(e);", suc, err, can);
        	injectJS(js);
        	bBusy = false;
        }
    };
    
	public void vocalize(String text, String voice, String language)
	{
		if(!webview.config.hasSpeech) return;
		
		if( bBusy == true || _vocnuance != null || _recnuance != null )
		{
			String js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.speech.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}
		
		bCancelled = false;
		bBusy = true;
		
		 // Create a single Vocalizer here.
		_vocnuance = AppMobiActivity._speechKit.createVocalizerWithLanguage("en_US", vocalizerListener, new Handler());        
	
		// set the Vocalizer voice
        if (voice != null) {
            _vocnuance.setVoice(voice);
        }
        
        _lastTtsContext = new Object();
        _vocnuance.speakString(text, _lastTtsContext);
	}
	
	public void cancel()
	{
		if(!webview.config.hasSpeech) return;
		
		if( _vocnuance != null )
		{
			_vocnuance.cancel();
			bCancelled = true;
		}

		if( _recnuance != null )
		{
			_recnuance.cancel();
			bCancelled = true;
		}
	}
	
}