package com.appMobi.appMobiLib;

import android.content.Intent;


abstract public class AppMobiCommand {
	public AppMobiWebView webview = null;
	final public AppMobiActivity activity;
	
	public AppMobiCommand(AppMobiActivity activity, AppMobiWebView webview){
		this.activity = activity;
		this.webview = webview;
	}
	
	protected void injectJS(final String js) {
		activity.runOnUiThread(new Runnable() {

			public void run() {
				webview.loadUrl(js);
			}

		});
	}
	
	protected void stopCommand() {
		
	}
	
	protected void pauseCommand() {
		
	}
	
	protected void resumeCommand(){
		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//do nothing
	}
}
