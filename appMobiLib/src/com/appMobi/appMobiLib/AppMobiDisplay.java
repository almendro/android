package com.appMobi.appMobiLib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class AppMobiDisplay extends AppMobiCommand {
	
	private float scale = -1;
	
	public AppMobiDisplay(AppMobiActivity activity, AppMobiWebView webview) {
		super(activity, webview);
	}
	
	public void startAR() {
		activity.arView.showCamera();
	}
	
	public void stopAR() {
		activity.arView.hideCamera();
	}
	
    //start hacks for HTC Incredible
	Method setScaleWithoutCheck, setScaleTo;
	public void forceScale(float scale) {
		try {
			WebView webview = activity.appView;
			if(setScaleWithoutCheck == null) setScaleWithoutCheck = webview.getClass().getMethod("setScaleWithoutCheck", boolean.class);
			if (setScaleWithoutCheck != null) {
				setScaleWithoutCheck.invoke(webview, true);
			}
			
			if(setScaleTo == null) setScaleTo = webview.getClass().getMethod("setScaleTo", float.class);
			if (setScaleTo != null) {
				setScaleTo.invoke(webview, scale);
				if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "setScaleTo called with " + scale);
				//if forceScale succeeded, hold onto scale
				this.scale = scale;
			} else{
				if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "setScaleTo not called");
			}
		} catch (NoSuchMethodException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		
	}

	public void checkScale() {
		float newScale = activity.appView.getScale();
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "checkScale called with " + newScale);
		if(this.scale!=-1 && newScale!=this.scale) {
			forceScale(this.scale);
			if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "checkScale calling forceScale with " + this.scale);
		}
		
	}
    //end hacks for HTC Incredible
	
}
