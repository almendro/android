package com.appMobi.appMobiLib;


abstract public class AppMobiModule {

	protected AppMobiWebView webview;
	protected AppMobiActivity activity;
	
	public void setup(AppMobiActivity activity, AppMobiWebView webview) {
		this.activity = activity;
		this.webview = webview;
	}

	public void initialize(AppMobiActivity activity, AppMobiWebView webview) {
		this.activity = activity;
		this.webview = webview;
	}
}
