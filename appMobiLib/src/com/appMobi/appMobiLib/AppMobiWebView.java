package com.appMobi.appMobiLib;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebStorage.QuotaUpdater;
import android.widget.LinearLayout;

import com.appMobi.appMobiLib.oauth.AppMobiOAuth;
import com.appMobi.appMobiLib.util.Debug;
import com.phonegap.CallbackServer;
import com.phonegap.api.PluginManager;

public class AppMobiWebView extends WebView implements AppMobiActivity.ConfigurationChangeListener {
	
	//hack for HTC devices
	private static boolean hasHTCUndocumentedMethods;
	static {
		Method m = null;
		try {
			m = WebView.class.getMethod("setScaleWithoutCheck", boolean.class);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
        hasHTCUndocumentedMethods = (m != null); 
	}
	
	
	public AppConfigData config;
	AppMobiActivity activity;
	Map<String, AppMobiCommand> commandObjects;

	AppMobiWebViewClient awvc = null;
	
	public AppMobiAccelerometer accelerometer;
	public AppMobiAdvertising advertising;
	public AppMobiCache cache;
	public AppMobiAnalytics stats;
	public AppMobiPlayer player;
	public AppMobiDevice device;
	public AppMobiNotification notification;
	public AppMobiDebug debug;
	public AppMobiDisplay display;
	public AppMobiOAuth oauth;
	public AppMobiAudio audio;
	public AppMobiCamera camera;
	public AppMobiPayments payments;
	public AppMobiFile file;
	public AppMobiGeolocation geolocation;
	public AppMobiSpeech speech;
	public AppMobiContacts contacts;
	
	private Runnable pageFinishedRunnable = null;
	private Runnable pageTimedOutRunnable = null;
	
	boolean isReady = false;
	
	//ad blocking support
	List<String> whiteList;
	boolean wasLoadingStopped;
	
	//HTC hack support
	boolean firstTime = true;
	
	//phonegap support
	public PluginManager pluginManager;
	public CallbackServer callbackServer;

	
	public AppMobiWebView(Context context, AppConfigData configData) {
		super(context);
		activity = AppMobiActivity.sharedActivity;
		config = configData;
		init();
		
		activity.addConfigurationChangeListener(this);
		
		bindBrowser();
		isReady = true;
		
	}

	public AppMobiWebView(Context context, AttributeSet attrs, AppConfigData configData) {
		super(context, attrs);
		activity = AppMobiActivity.sharedActivity;
		config = configData;
		init();
		bindBrowser();
	}
	
	private void init() {
        //setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams webviewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
        		ViewGroup.LayoutParams.FILL_PARENT, 1.0F);
        setLayoutParams(webviewParams);
        
        setWebChromeClient(new AppMobiChromeClient(activity));
        awvc = new AppMobiWebViewClient();
        setWebViewClient(awvc);
        
        //start hacks for HTC Incredible
        if(hasHTCUndocumentedMethods) {
        	setPictureListener( 
        		new WebView.PictureListener() {
					public void onNewPicture(WebView view, Picture picture) {
						if(firstTime && display!=null) {
							display.checkScale();
							firstTime = false;
						}
					}
				} 
        	);
        	//from: http://community.htc.com/na/htc-forums/android/f/91/p/2332/8538.aspx
        	// To avoid WebView pan and zoom problem.
        	// add the following section code before load any URL or data.
        	// ===================== Begin =====================
        	try {
        	   Method m = getClass().getMethod("setIsCacheDrawBitmap", boolean.class);
        	    if (m != null) {
        	        m.invoke(this, false);
        	        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        	    }
        	} catch (NoSuchMethodException e) {
        	} catch (IllegalAccessException e) {
        	} catch (InvocationTargetException e) {
        	}
        	// ===================== End =====================        	
        }
        //end hacks for HTC Incredible
        
        WebViewDatabase.getInstance(activity).clearHttpAuthUsernamePassword(); 
        
        setInitialScale(100);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        settings.setGeolocationEnabled(true);
        //enable database
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(activity.baseDir().getAbsolutePath()+"/appmobi.db");
        //enable local storage
        settings.setDomStorageEnabled(true);

//Check for undocumented methods in WebView
//			Method[] methods = WebView.class.getDeclaredMethods();
//			for(Method method:methods) {
//				String log = new String();
//				int modifiers = method.getModifiers();
//				if(Modifier.isStatic(modifiers)) log+= "static ";
//				if(Modifier.isAbstract(modifiers)) log+= "abstract ";
//				if(Modifier.isPublic(modifiers)) log+= "public ";
//				if(Modifier.isPrivate(modifiers)) log+= "private ";
//				if(Modifier.isProtected(modifiers)) log+= "protected ";
//				Class returnType = method.getReturnType();
//				log += returnType==null?"void":returnType + " " + method.getName() + "(";
//				Class[] params = method.getParameterTypes();
//				for(Class param:params) log += param.getName() + ",";
//				log+=")";
//				if(Debug.isDebuggerConnected()) Log.i("[appMobi]", log);
//			}

    	//clear the cache - needed so that javascript is reloaded - possibly optimize later
    	clearCache(true);
    	//if(Debug.isDebuggerConnected()) Log.i("appmobi", "called clearCache");
		
	}
	
	public boolean hasHTCUndocumentedMethods() {
		return AppMobiWebView.hasHTCUndocumentedMethods;
	}
	
	public void bindBrowser()
	{
		//only create these once
		accelerometer = new AppMobiAccelerometer(activity, this);
		advertising = new AppMobiAdvertising(activity, this);
		cache = new AppMobiCache(activity, this);
		stats = new AppMobiAnalytics(activity, this);
		//hack to fix crash bug
		player = AppMobiPlayer.getInstance(activity, this);
		debug = new AppMobiDebug(activity, this);
		device = new AppMobiDevice(activity, this);
		notification = new AppMobiNotification(activity, this);
		display = new AppMobiDisplay(activity, this);
		//slows down startup a lot
		oauth = new AppMobiOAuth(activity, this);
		audio = new AppMobiAudio(activity, this);
		camera = new AppMobiCamera(activity, this);
		//payments = new AppMobiPayments(activity, this);
		file = new AppMobiFile(activity, this);
		geolocation = new AppMobiGeolocation(activity, this);
		speech = new AppMobiSpeech(activity,this);
		contacts = new AppMobiContacts(activity,this);
		
		addJavascriptInterface(accelerometer, "AppMobiAccelerometer");
		addJavascriptInterface(advertising, "AppMobiAdvertising");
		addJavascriptInterface(cache, "AppMobiCache");
		addJavascriptInterface(stats, "AppMobiAnalytics");
		addJavascriptInterface(player, "AppMobiPlayer");
		addJavascriptInterface(debug, "AppMobiDebug");
		addJavascriptInterface(device, "AppMobiDevice");
		addJavascriptInterface(notification, "AppMobiNotification");
		addJavascriptInterface(display, "AppMobiDisplay");
		addJavascriptInterface(oauth, "AppMobiOAuth");
		addJavascriptInterface(audio, "AppMobiAudio");
		addJavascriptInterface(camera, "AppMobiCamera");
		//addJavascriptInterface(payments, "AppMobiPayments");
		addJavascriptInterface(file, "AppMobiFile");
		addJavascriptInterface(geolocation, "AppMobiGeolocation");
		addJavascriptInterface(speech, "AppMobiSpeech");
		addJavascriptInterface(contacts, "AppMobiContacts");
		
		//modules will not currently work right in mobius
		for(String moduleName:getResources().getStringArray(R.array.modules))
		{
			if(moduleName == null || moduleName.length()==0) continue;			

			AppMobiModule module = null;
			
			try {
				module = (AppMobiModule) Class.forName(moduleName).newInstance();
			}
			catch(Exception e)
			{
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", "unable to retrieve appmobi.js from server config" + e.getMessage(), e);
				}
			}
			
			if( module != null )
			{
				module.setup(activity, this);
			}
		}
	}
	
    public File appDirectory()
    {
    	File temp = activity.getApplicationContext().getFilesDir();
    	return temp;
    }
    
    public File baseDirectory()
    {
    	File temp = new File(appDirectory(), "root");
    	return temp;
    }
    
    public void registerCommand(AppMobiCommand command, String name)
    {
    	addJavascriptInterface(command, name);
    }
    
	
	@Override
	public void loadUrl(final String url)
	{
		super.loadUrl(url);
	}
	
	public void loadUrl(final String url, final int loadTimeout, final Runnable timedOut) {
		loadUrl(url, null, loadTimeout, timedOut);
	}
	
	public void loadUrl(final String url, final Runnable finished, final int loadTimeout, final Runnable timedOut)
	{
		AppMobiWebView.this.pageFinishedRunnable = finished;
		AppMobiWebView.this.pageTimedOutRunnable = timedOut;
		super.loadUrl(url);
		
		if(loadTimeout!=0) {
			//enforce load timeout
			new Thread("AppMobiWebView:loadUrl") {
				public void run() {
					try {
						//sleep for duration of timeout
						Thread.sleep(loadTimeout*1000);
						//check if the runnable has been cleared;
						//if it is still present, the page hasn't finished loading
						if(AppMobiWebView.this.pageTimedOutRunnable!=null && AppMobiWebView.this.pageTimedOutRunnable.equals(timedOut)) {
							Log.i("[appMobi]", "AppMobiWebView timedOut for " + url);
							runPageTimedOutRunnable();
						}
					} catch (Exception e) {
						if(Debug.isDebuggerConnected()) {
							Log.e("[appMobi]", e.getMessage(), e);
						}
					}
				}
			}.start();
		}
	}
	
	private void runPageTimedOutRunnable() {
		//screen orientation is set to portrait in manifest to prevent splash screen weirdness
		//need to update to default or whatever was set by user here
		if(this.device!=null) this.device.updateOrientation();
		
		if(pageTimedOutRunnable!=null) new Thread(pageTimedOutRunnable, "AppMobiWebView:timedOut").start();
		AppMobiWebView.this.pageTimedOutRunnable = null;
		AppMobiWebView.this.pageFinishedRunnable = null;
	}
	
	protected void hideSplash() {
		runPageTimedOutRunnable();		
	}

	private class AppMobiWebViewClient extends WebViewClient {
		
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.i("[appMobi]", "AppMobiWebView - pageFinished for " + url);
			if(AppMobiWebView.this.pageFinishedRunnable!=null) {
				new Thread(AppMobiWebView.this.pageFinishedRunnable, "AppMobiWebViewClient:onPageFinished").start();
				AppMobiWebView.this.pageTimedOutRunnable = null;
				AppMobiWebView.this.pageFinishedRunnable = null;
			}
			super.onPageFinished(view, url);
			
		}

		@Override
        public void onReceivedHttpAuthRequest  (WebView view, HttpAuthHandler handler, String host,String realm) {
            String[] up = view.getHttpAuthUsernamePassword(host, "");
            if( up != null && up.length == 2 ) {
                handler.proceed(up[0], up[1]);
            }
            else{
                if(Debug.isDebuggerConnected()) Log.d("WebAuth","Could not find user/pass for domain :"+host+" with realm = "+realm+"(\"\" specified)");
           }
        }

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("vnd.youtube:") || url.startsWith("mailto:") || url.startsWith("tel:")) { 
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); 
				activity.startActivity(intent);
				return true;
			} else if(handleYoutube(url)) {
				return true;
			} else {
				return super.shouldOverrideUrlLoading(view, url);
			}
		}
		
		@Override
		public void onLoadResource(WebView view, String url) {
			//Log.d("[appMobi]", "onLoadResource: "+url);
			
			if(wasLoadingStopped) {
				//get domain to check against whitelist
				String domain = url.substring(url.indexOf("://")+3);
				domain = domain.substring(0, domain.indexOf('/'));
				
				//if the url is allowed, load it, otherwise inject an event
				if("http://localhost:58888".equals(domain) || whiteList.contains(domain)) {
					//load the page
					loadUrl(url);
				} else {
					stopLoading();
					//inject event
					String js = String.format("javascript:var e = document.createEvent('Events');e.initEvent('appMobi.device.remote.block',true,true);e.success=true;e.blocked='%s';document.dispatchEvent(e);", url.replaceAll("'", "\\\\'"));
					if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "onPageStarted js:" + js);
					loadUrl(js);
				}
				wasLoadingStopped = false;
			} else {

				//wrap youtube special handling in exception block so it doesnt affect other functionality
				try {
					if(!handleYoutube(url)) {
						super.onLoadResource(view, url);
					}
				} catch(Exception e) {
					//just do debug logging if an exception occurs
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}

		}				
		
	}
	
	private boolean handleYoutube(String url) {
		//if this is froyo, check if this is a youtube video that the user is trying to play
		//http://m.youtube.com/gen_204?action=playback&imgparams=ns%3Dyt%26ps%3Dblazer%26playback%3D1%26el%3Ddetailpage%26app%3Dyoutube_mobile%26fmt%3D18%26hl%3Den%26gl%3DUS%26docid%3DbDhYDY9A4TI%26rdm%3D8493%26feature%3D%26reloadCount%3D0%26waitTime%3D3&ajax=1&tsp=1&tspv=v2&xl=xl_blazer
		if((Integer.parseInt(Build.VERSION.SDK) >= 8 /*Build.VERSION_CODES.FROYO*/) && 
				(url.startsWith("http://m.youtube.com/") && url.indexOf("action=playback")!=-1) || (url.startsWith("http://s.youtube.com/") && url.indexOf("playback=1")!=-1)) {
			//parse out the docid
			String decodedUrl = URLDecoder.decode(url);
			String queryString = decodedUrl.replaceFirst(".*\\?", "");
			String[] queryStringParams = queryString.split("&");
			for(String param:queryStringParams) {
				if(param.startsWith("docid=")) {
					String docid = param.split("=")[1];
					//if we got to here, we want to load this style url:
					//vnd.youtube:bDhYDY9A4TI?vndapp=youtube_mobile&vndclient=mv-google&vndel=watch&vndxl=xl_blazer
					final String youtubeUrl = "vnd.youtube:"+docid;//+"?vndapp=youtube_mobile&vndclient=mv-google&vndel=watch&vndxl=xl_blazer";
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)); 
					activity.setLaunchedChildActivity(true);
					activity.startActivity(intent);
					break;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	private class AppMobiChromeClient extends WebChromeClient {

        Context mCtx;
        AppMobiChromeClient(Context ctx) {
                mCtx = ctx;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                AlertDialog.Builder alertBldr = new AlertDialog.Builder(mCtx);
                alertBldr.setMessage(message);
                alertBldr.setTitle(url.substring(url.lastIndexOf('/') + 1));
                alertBldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                        dialog.dismiss();
	                }
                });
                alertBldr.show();
                result.confirm();
                return true;
        }
        
        //phonegap support
        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        	
        	if(callbackServer==null || pluginManager==null) {
        		return super.onJsPrompt(view, url, message, defaultValue, result);
        	}
        	
        	// Security check to make sure any requests are coming from the page initially
        	// loaded in webview and not another loaded in an iframe.
        	boolean reqOk = false;
        	if (url.indexOf("http://localhost:58888/") == 0) {
        		reqOk = true;
        	}
			
        	// Calling PluginManager.exec() to call a native service using 
        	// prompt(this.stringify(args), "gap:"+this.stringify([service, action, callbackId, true]));
        	if (reqOk && defaultValue != null && defaultValue.length() > 3 && defaultValue.substring(0, 4).equals("gap:")) {
        		JSONArray array;
        		try {
        			array = new JSONArray(defaultValue.substring(4));
        			String service = array.getString(0);
        			String action = array.getString(1);
        			String callbackId = array.getString(2);
        			boolean async = array.getBoolean(3);
        			String r = pluginManager.exec(service, action, callbackId, message, async);
        			result.confirm(r);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}
        	
        	// Polling for JavaScript messages 
        	else if (reqOk && defaultValue.equals("gap_poll:")) {
        		String r = callbackServer.getJavascript();
        		result.confirm(r);
        	}
        	
        	// Calling into CallbackServer
        	else if (reqOk && defaultValue.equals("gap_callbackServer:")) {
        		String r = "";
        		if (message.equals("usePolling")) {
        			r = ""+callbackServer.usePolling();
        		}
        		else if (message.equals("restartServer")) {
        			callbackServer.restartServer();
        		}
        		else if (message.equals("getPort")) {
        			r = Integer.toString(callbackServer.getPort());
        		}
        		else if (message.equals("getToken")) {
        			r = callbackServer.getToken();
        		}
        		result.confirm(r);
        	}
        	
        	// Show dialog
        	else {
        		return super.onJsPrompt(view, url, message, defaultValue, result);
			}
        	return true;
        }
        
        @Override
		public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
			//super.onGeolocationPermissionsShowPrompt(origin, callback);
			callback.invoke(origin, true, false);
		}
        
        public void addMessageToConsole(String message, int lineNumber, String sourceID) {
			if(Debug.isDebuggerConnected()) Log.d("AppMobiLog", sourceID + ": Line " + Integer.toString(lineNumber) + " : " + message);
		}

        @Override
		public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, QuotaUpdater quotaUpdater) {
			long minDBSizeinBytes = 10*1024*1024; 
			//if user specified db size and this is first time through, make it that size or the min size
			if(currentQuota==0 && estimatedSize!=0) {
				if(estimatedSize < minDBSizeinBytes) {
					quotaUpdater.updateQuota(minDBSizeinBytes);
				} else {
					quotaUpdater.updateQuota(estimatedSize);
				}
			} else if(currentQuota==0){
				//otherwise make it the min size
				quotaUpdater.updateQuota(minDBSizeinBytes);
			} else {
				quotaUpdater.updateQuota(currentQuota);
			}
//			if(currentQuota==0) {
//				//didn't really exceed, just first time through so quota is 0 bytes - allocate 2MB quota
//				final WebStorage ws = WebStorage.getInstance();
//				ValueCallback<Map> originsCB = new ValueCallback<Map>() {
//
//					public void onReceiveValue(Map value) {
//						for(Object origin:value.keySet()) {
//							final String strOrigin = (String)origin;
//							ws.getQuotaForOrigin((String)origin, new ValueCallback<Long>() {
//								public void onReceiveValue(Long value) {
//									if(Debug.isDebuggerConnected()) Log.d("[appMobi]", strOrigin+":"+value);
//								}								
//							});
//						}
//						
//					}
//					
//				};
//				ws.getOrigins(originsCB);
//			} else {
//				//really exceeded the quota - inject notification
//				appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.device.dbquota',true,true);ev.url='"+url
//						+"';ev.dbId='"+databaseIdentifier+"';ev.originQuota='"+currentQuota+"';ev.dbSize='"+estimatedSize+"';ev.originUsed='"+totalUsedQuota+"';document.dispatchEvent(ev);");
//			}
		}

		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			// TODO Auto-generated method stub
			super.onShowCustomView(view, callback);
		}
		
	}

	public void onConfigurationChanged(int orientation) {
		//inject an orientation change event
		//update device dimensions
		DisplayMetrics dm = new DisplayMetrics(); 
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm); 

		String jsOrientChange = "javascript:" + 
			"try{AppMobi.device.width=" + dm.widthPixels + ";AppMobi.device.height=" + dm.heightPixels + ";" +
			"AppMobi.device.setOrientation("+((orientation==Configuration.ORIENTATION_PORTRAIT)?"0":"-90")+");}catch(e){}";
		loadUrl(jsOrientChange);
		
		if(Debug.isDebuggerConnected()) {
			Log.d("[appMobi]", "injecting an orientation change event:" + jsOrientChange);
		}
		
	}	
	
	public void setBlockedPagesWhitelist(String domainList) {
		whiteList = Arrays.asList(domainList.split("\\|"));
	}
	
	void autoLogEvent(String event, String query) {
		if( query == null ) query = "-";
		
		stats.logPageEvent(event, query, "200", "GET", "0", "index.html");
	}
	
}
