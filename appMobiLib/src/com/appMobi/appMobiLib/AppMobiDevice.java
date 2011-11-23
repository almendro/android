package com.appMobi.appMobiLib;
/* License (MIT)
 * Copyright (c) 2008 Nitobi
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * Software), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsoluteLayout;
import android.widget.ImageButton;

import com.appMobi.appMobiLib.util.Debug;

import fm.flycast.FlyCastPlayer;

public class AppMobiDevice extends AppMobiCommand implements AppMobiActivity.ConfigurationChangeListener {
	public final static String platform = "Android";
	public final static String phonegap = "0.9.1";
	
	private boolean shouldAutoRotate = true;
	private String rotateOrientation = "";
	
	private AbsoluteLayout remoteLayout;
	ImageButton remoteClose;
	private AppMobiWebView remoteView;
	boolean isShowingRemoteSite = false;
	int remoteCloseXPort=0, remoteCloseYPort=0, remoteCloseXLand=0, remoteCloseYLand=0, remoteCloseH=0, remoteCloseW=0;
    
	@SuppressWarnings("deprecation")
	public AppMobiDevice(final AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
		
		activity.addConfigurationChangeListener(this);
		
		//remote site support
		remoteLayout = new AbsoluteLayout(activity);
		remoteLayout.setBackgroundColor(Color.BLACK);
		//hide the remote site display until needed
		remoteLayout.setVisibility(View.GONE);
		//create the close button
		remoteClose = new ImageButton(activity);
		remoteClose.setBackgroundColor(Color.TRANSPARENT);
		Drawable remoteCloseImage = null;
		File remoteCloseImageFile = new File(activity.appDir(), "_appMobi/remote_close.png");
		if(remoteCloseImageFile.exists()) {
			remoteCloseImage = (Drawable.createFromPath(remoteCloseImageFile.getAbsolutePath()));
		}
		else {
			remoteCloseImage = ( activity.getResources().getDrawable(R.drawable.remote_close));
		}
		//set the button image
		remoteClose.setImageDrawable(remoteCloseImage);
		//set up the button click action
		remoteClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				closeRemoteSite();
			}
		});
		//add the close button
		remoteLayout.addView(remoteClose);

		activity.runOnUiThread(new Runnable() {
			public void run() {
				//hack for mobius
				if(activity.root != null) {
					//add layout to activity root layout
					activity.root.addView(remoteLayout);
				}
			}
		});
		
		//turn on logging
//		if(true) {
//			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
//			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
//
//			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
//			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
//			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
//			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
//			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");			
//		}
    }
	
	public String getPlatform()
	{
		return platform;
	}

	public String getOSVersion()
	{
		return android.os.Build.VERSION.RELEASE;
	}
	
	public String getUuid()
	{
		return activity.getDeviceID();
	}
	
	public String getModel()
	{
		return android.os.Build.MODEL;
	}
	
	public String getVersion()
	{
		return activity.getResources().getString(R.string.version);
	}
	
	public String getPGVersion()
	{
		return phonegap;
	}
    
    public String getInitialOrientation() {
    	return getOrientation();
    }
    
    public String getOrientation() {
    	return (activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT)?"0":"-90";
    }
    
    public String getConnection() 
    {
    	return activity.flyCastPlayer.GetConnectivityType();
    }
    
    public float getDisplayDensity() {
		DisplayMetrics dm = new DisplayMetrics(); 
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm); 
		return dm.density;
    }
	
    public int getDisplayHeight() {
		DisplayMetrics dm = new DisplayMetrics(); 
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm); 
		return dm.heightPixels;
    }
	
    public int getDisplayWidth() {
		DisplayMetrics dm = new DisplayMetrics(); 
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm); 
		return dm.widthPixels;
    }
	
	public boolean getWasPlaying()
	{
		boolean playing = false;
		
		if( activity.flyCastPlayer.serviceAlreadyRunning && activity.flyCastPlayer.CurrentPlayMode == FlyCastPlayer.PlayModes.Play)
		{
			playing = true;
		}

		return playing;
	}
	
	public String getPlayingStation()
	{
		String stationID = "";
		
		if( activity.flyCastPlayer.serviceAlreadyRunning && activity.flyCastPlayer.CurrentPlayMode == FlyCastPlayer.PlayModes.Play)
		{
			stationID = FlyCastPlayer.stationID;
		}
		
		return stationID;
	}
	
	public boolean getHasAnalytics()
	{
		AppConfigData config = webview.config;
		boolean analytics = config != null?config.hasAnalytics:false;
		
		return analytics;
	}
	
	public boolean getHasCaching()
	{
		AppConfigData config = webview.config;
		boolean caching = config != null?config.hasCaching:false;

		return caching;
	}
	
	public boolean getHasStreaming()
	{
		AppConfigData config = webview.config;
		boolean streaming = config != null?config.hasStreaming:false;

		return streaming;
	}
	
	public boolean getHasAdvertising()
	{
		AppConfigData config = webview.config;
		boolean advertising = config != null?config.hasAdvertising:false;

		return advertising;
	}
    
	public boolean getHasPush() {
		AppConfigData config = webview.config;
		boolean has = config != null?config.hasPushNotify:false;

		return has;
	}

	public boolean getHasPayments() {
		AppConfigData config = webview.config;
		boolean has = config != null?config.hasPayments:false;

		return has;
	}
	
	public boolean getHasUpdates() {
		AppConfigData config = webview.config;
		boolean has = config != null?config.hasLiveUpdate:false;

		return has;
	}
	
	public String getQueryString() {
		String qs = "";
		if(activity.startedFromProtocolHandler && activity.getIntent()!=null && activity.getIntent().getData()!=null && activity.getIntent().getData().getQuery()!=null) {
			qs = activity.getIntent().getData().getQuery();
		}
		return qs;
	}
	
    public void managePower(boolean shouldStayOn, boolean onlyWhenPluggedIn)
    {
    	// If shouldStayOn is false, revert to normal behaviour -- screen locks after inactivity
    	// If shouldStayOn is true, check onlyWhenPluggedIn
    	// 			if onlyWhenPluggedIn is true, check if device is charging/plugged in -- if not, screen locks after inacitivity
    	// 			if onlyWhenPluggedIn is false, screen never locks after inacitivity
    	
    	// Android code will be different see -- http://developer.android.com/reference/android/os/PowerManager.html
    	/* This code may be useful -- from above link
    	 PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		 wl.acquire();
		   ..screen will stay on during this section..
		 wl.release();
    	//*/
    	
    	//Added by Parveen On April29,2010.
    	//Release if already aquired.
    	if (activity.wl != null)
		{
			activity.wl.release();
			activity.wl = null;
		}
    	
    	if(shouldStayOn)
    	{    		
    		//Added by Parveen on April 29,2010 - Register receiver for battery status changes.
    		activity.registerReceiver(activity.mBatInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    		if(onlyWhenPluggedIn)
    		{    			
    			if((activity.mBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) || (activity.mBatteryStatus == BatteryManager.BATTERY_STATUS_FULL))    				
    			{    				
    				ResourceAcquire();
    			}
    		}
    		else
    		{
    			ResourceAcquire();
    		}
    	}    	
    }
    
    private void ResourceAcquire()
    {    	    
    	PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
    	activity.wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		try
		{
		 	activity.wl.acquire();
		}
		catch(Exception e)
		{
		}
    }
    
    public void setAutoRotate(boolean shouldRotate) 
    {
    	shouldAutoRotate = shouldRotate;
    	updateOrientation();
    }    
    
    public void setRotateOrientation(String orientation) 
    {
    	rotateOrientation = orientation;
    	updateOrientation();
    }
    
    void updateOrientation() {
    	if(rotateOrientation.equalsIgnoreCase("landscape"))
    	{
    		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	}
    	else if(rotateOrientation.equalsIgnoreCase("portrait"))
    	{    		        
    		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	}
    	else {
    		activity.setRequestedOrientation(shouldAutoRotate?ActivityInfo.SCREEN_ORIENTATION_SENSOR:
    			activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?
    					ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	}
    }
    
    public void updateConnection()
    {
    	String currentConnection =  activity.flyCastPlayer.GetConnectivityType();    
    	String connectionType = "javascript: AppMobi.device.connection =  \"" + currentConnection + "\";var e =document.createEvent('Events');e.initEvent('appMobi.device.connection.update',true,true);document.dispatchEvent(e);"; 
    	injectJS(connectionType);
    }
    
	public void setBasicAuthentication(String host, String username, String password) {
        activity.setBasicAuthentication(host, username, password);
	}
    
	public void addVirtualPage() {
        activity.addVirtualPage();
	} 
    
	public void removeVirtualPage() {
        activity.removeVirtualPage();
	}
	
	public void registerLibrary(String strDelegateName)
	{
		AppMobiModule module = null;
		
		try {
			module = (AppMobiModule) Class.forName(strDelegateName).newInstance();
		} catch(Exception e) {
			Log.e("[appMobi]", e.getMessage(), e);
		}
		
		if( module != null )
		{
			module.initialize(activity, activity.appView);
		}
	}
	
	public boolean hasHTCUndocumentedMethods() {
		return activity.appView.hasHTCUndocumentedMethods();
	}
	
	public void launchExternal(String strURL)
	{
		if( strURL == null || strURL.length() == 0 ) return;
	
		activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(strURL)));
	}
	
	public void getRemoteData(final String urlString, final String method, final String postData, final String successCallback, final String errorCallback) {
		getRemoteData(urlString, method, postData, successCallback, errorCallback, "", false);
	}

	public void getRemoteData(final String urlString, final String method, final String postData, final String successCallback, final String errorCallback, final String id, final boolean hasId) {
		new Thread("AppMobiDevice:getRemoteData") {
			public void run() {
				doGetRemoteData(urlString, method, postData, successCallback, errorCallback, id, hasId);
			}
		}.start();
	}
	
	private String getStatusText(int code) {
		String status = "";
		
		switch( code )
		{
				case 200:
				status = "OK";
				break;
				case 201:
				status = "CREATED";
				break;
				case 202:
				status = "Accepted";
				break;
				case 203:
				status = "Partial Information";
				break;
				case 204:
				status = "No Response";
				break;
				case 301:
				status = "Moved";
				break;
				case 302:
				status = "Found";
				break;
				case 303:
				status = "Method";
				break;
				case 304:
				status = "Not Modified";
				break;
				case 400:
				status = "Bad request";
				break;
				case 401:
				status = "Unauthorized";
				break;
				case 402:
				status = "PaymentRequired";
				break;
				case 403:
				status = "Forbidden";
				break;
				case 404:
				status = "Not found";
				break;
				case 500:
				status = "Internal Error";
				break;
				case 501:
				status = "Not implemented";
				break;
				case 502:
				status = "Service temporarily overloaded";
				break;
				case 503:
				status = "Gateway timeout";
				break;			
		}
		
		return status;
	}

	private void doGetRemoteDataExt(final String urlString, final String id, String method, final String body, String headers) {
		String js = null;
		
		//ios does not validate
		
		if( method == null || method.length()==0 ) method = "GET";
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpEntity entity = null;
		HttpUriRequest request = null;
		HttpResponse response = null;
		
		try {
			if("POST".equalsIgnoreCase(method)) {
				request = new HttpPost(urlString);
				((HttpPost)request).setEntity(new ByteArrayEntity(body.getBytes()));
				request.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
			} else{
				request = new HttpGet(urlString);
			}
			
			if( headers.length() > 0 )
			{
				int indexOfTilde, length = 0;
				String field = null, value = null;
				
				indexOfTilde = headers.indexOf('~');
				while(indexOfTilde!=-1) {
					length = Integer.parseInt(headers.substring(0, indexOfTilde));

					if( length > 0 )
					{
						field = headers.substring(indexOfTilde+1).substring(0, length);
					}
					headers = headers.substring(indexOfTilde+1+length);
					
					indexOfTilde = headers.indexOf('~');
					length = Integer.parseInt(headers.substring(0, indexOfTilde));			
					if( length > 0 )
					{
						value = headers.substring(indexOfTilde+1).substring(0, length);
					}
					headers = headers.substring(indexOfTilde+1+length);
					
					if( field != null && value != null )
					{
						if(!"content-length".equals(field.toLowerCase())) {//skip Content-Length - causes error because it is managed by the request
							request.setHeader(field, value);
						}
					}
					
					indexOfTilde = headers.indexOf('~');
					field = null;
					value = null;
				}

			}
			
			response = client.execute(request);
	
			//check response status
			if(response!=null) {
				entity = response.getEntity();

				//inject response
				String responseBody = _getResponseBody(entity);
				char[] bom = {0xef,0xbb,0xbf};
				//check for BOM characters, then strip if present
				if(responseBody.length()>=3 && responseBody.charAt(0)==bom[0]&&responseBody.charAt(1)==bom[1]&&responseBody.charAt(2)==bom[2]) {
					responseBody = responseBody.substring(3);
				}
				
				//escape existing backslashes
				responseBody = responseBody.replaceAll("\\\\", "\\\\\\\\"); 
				
				//escape internal double-quotes
				responseBody = responseBody.replaceAll("\"", "\\\\\"");
                responseBody = responseBody.replaceAll("'", "\\\\'");
				
				//replace linebreaks with \n
				responseBody = responseBody.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
				
				StringBuilder extras = new StringBuilder("{");
				extras.append(String.format("status:'%d',", response.getStatusLine().getStatusCode()));
				extras.append(String.format("statusText:'%s',", getStatusText(response.getStatusLine().getStatusCode())));
				extras.append("headers: {");
				
				Header[] allHeaders = response.getAllHeaders();
				for(Header header:allHeaders) {
					String key = header.getName();
					String value = header.getValue();
					value = value.replaceAll("'", "\\\\'");
					extras.append(String.format("'%s':'%s',", key, value));
				}
				extras.append("} }");
				
				js = String.format(
					"javascript: var e = document.createEvent('Events');e.initEvent('appMobi.device.remote.data',true,true);e.success=true;e.id='%s';e.response='%s';e.extras=%s;document.dispatchEvent(e);", 
						id, responseBody, extras.toString());
			} else {
				if(Debug.isDebuggerConnected()) Log.d("[appMobi]", "getRemoteData("+urlString+","+method+") \nerror -- code: " + response.getStatusLine().getStatusCode());
				if(Debug.isDebuggerConnected()) Log.d("[appMobi]",  Arrays.asList(response.getAllHeaders()).toString());
				throw new Exception("error -- code: " + response.getStatusLine().getStatusCode());
			}
		} catch(Exception ex) {
			js = String.format(
				"javascript: var e = document.createEvent('Events');e.initEvent('appMobi.device.remote.data',true,true);e.success=false;e.id='%s';e.response='';e.extras={};e.error='%s';document.dispatchEvent(e);", 
					id, ex.getMessage());
		} catch (OutOfMemoryError err) {
			js = String.format(
					"javascript: var e = document.createEvent('Events');e.initEvent('appMobi.device.remote.data',true,true);e.success=false;e.id='%s';e.response='';e.extras={};e.error='%s';document.dispatchEvent(e);", 
						id, err.getMessage());			
		}
		
		
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]",js);

		injectJS(js);
	}
	
	public void getRemoteDataExt(final String urlString, final String id, final String method, final String body, final String headers) {
		new Thread("AppMobiDevice:getRemoteDataExt") {
			public void run() {
				doGetRemoteDataExt(urlString, id, method, body, headers);
			}
		}.start();
	}
	
	public void doGetRemoteData(String urlString, final String method, final String postData, final String successCallback, final String errorCallback, final String id, final boolean hasId) {
		boolean success = false;
		
		if( urlString == null || urlString.length() == 0 ) return;
		
		if( method == null || method.length()==0 ) return;
		
		if( successCallback == null || successCallback.length() == 0 ) return;
		
		if( errorCallback == null || errorCallback.length() == 0 ) return;

		DefaultHttpClient client = new DefaultHttpClient();
		HttpEntity entity = null;
		HttpUriRequest request = null;
		HttpResponse response = null;
		Exception e = null;
		
		
		try {
			if("POST".equalsIgnoreCase(method)) {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				if(postData!=null && postData.length()>0) {
					//parse postData
					String[] nvps = postData.split("&");
					if(nvps!=null) {
						for(int i=0;i<nvps.length;i++) {
							String[] nameValue = nvps[i].split("=");
							if(nameValue!=null && nameValue.length==2) {
								nameValuePairs.add(new BasicNameValuePair(nameValue[0], nameValue[1]));
							}
						}
					}
					
				}
				
//				if("https://secure.foxnewsradio.com/paypal/DoDirectPaymentReceipt.php".equals(urlString)) {
//					urlString = "http://192.168.1.217:9090/foxnews.php";
//				}
				
				request = new HttpPost(urlString);
				//((HttpPost)request).setEntity(new StringEntity(postData==null?"":postData));
				((HttpPost)request).setEntity(new UrlEncodedFormEntity(nameValuePairs));
				request.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
			} else{
				request = new HttpGet(urlString);
			}
			response = client.execute(request);
			entity = response.getEntity();
	
			//check error
			//check response status
			if(response.getStatusLine().getStatusCode()==200) {
				success = true;
			} else {
				if(Debug.isDebuggerConnected()) Log.d("[appMobi]", "getRemoteData("+urlString+","+method+") \nerror -- code: " + response.getStatusLine().getStatusCode());
				if(Debug.isDebuggerConnected()) Log.d("[appMobi]",  Arrays.asList(response.getAllHeaders()).toString());
			}
		} catch(Exception ex) {
			e = ex;
		}
		//inject response
		String js = null;
		char delimiter = '"';
		if(success) {
			String responseBody = null;
			try {
				responseBody = _getResponseBody(entity);
				char[] bom = {0xef,0xbb,0xbf};
				//check for BOM characters, then strip if present
				if(responseBody.charAt(0)==bom[0]&&responseBody.charAt(1)==bom[1]&&responseBody.charAt(2)==bom[2]) {
					responseBody = responseBody.substring(3);
				}
				
				//escape existing backslashes
				responseBody = responseBody.replaceAll("\\\\", "\\\\\\\\"); 
				
				//escape internal double-quotes
				responseBody = responseBody.replaceAll("\"", "\\\\\"");
                responseBody = responseBody.replaceAll("'", "\\\\'");
				
				//replace linebreaks with \n
				responseBody = responseBody.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
				
				js = "javascript: " + successCallback + "(" +
					(hasId?(delimiter + id + delimiter + ", "):"") +
					delimiter + responseBody + delimiter + ")";
			} catch(Exception ex) {
				js = "javascript: " + errorCallback + "(" + 
				(hasId?(delimiter + id + delimiter + ", "):"") +
				delimiter + ex.getMessage() + delimiter + ")";
			} catch (OutOfMemoryError err) {
				js = "javascript: " + errorCallback + "(" + 
				(hasId?(delimiter + id + delimiter + ", "):"") +
				delimiter + "Out of memory." + delimiter + ")";
			}
			
		} else {
			String errorMessage;
			if(e != null) {
				errorMessage = e.getMessage();
			} else {
				errorMessage = "error -- code: " + response.getStatusLine().getStatusCode();
			}
			js = "javascript: " + errorCallback + "(" + 
			(hasId?(delimiter + id + delimiter + ", "):"") +
			delimiter + errorMessage + delimiter + ")";
		}
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]",js);

		injectJS(js);
	}

	//from http://thinkandroid.wordpress.com/2009/12/30/getting-response-body-of-httpresponse/
	private String _getResponseBody(final HttpEntity entity) throws IOException, ParseException {

		if (entity == null) {
			throw new IllegalArgumentException("HTTP entity may not be null");
		}

		InputStream instream = entity.getContent();

		if (instream == null) {
			return "";
		}

		if (entity.getContentLength() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
			"HTTP entity too large to be buffered in memory");
		}

		String charset = getContentCharSet(entity);
		if (charset == null) {
			charset = HTTP.DEFAULT_CONTENT_CHARSET;
		}
		
		String result = null;
		Reader reader = null;
		try {
			long length = entity.getContentLength();
			if( length == -1 ) length = 64 * 1024;
			reader = new InputStreamReader(instream, charset);
			StringBuilder buffer = new StringBuilder((int)length);
			char[] tmp = new char[1024];

			int l;
			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}
			result = buffer.toString();	
			reader.close();
		} catch(Exception ex) {
		} catch(OutOfMemoryError err) {
		}
		
		return result;
	}

	private String getContentCharSet(final HttpEntity entity)
			throws ParseException {

		if (entity == null) {
			throw new IllegalArgumentException("HTTP entity may not be null");
		}

		String charset = null;
		if (entity.getContentType() != null) {
			HeaderElement values[] = entity.getContentType().getElements();
			if (values.length > 0) {
				NameValuePair param = values[0].getParameterByName("charset");
				if (param != null) {
					charset = param.getValue();
				}
			}
		}

		return charset;
	}

	public void showRemoteSite(final String strURL, final int closeX, final int closeY, final int closeW, final int closeH) {
		showRemoteSite(strURL, closeX, closeY, closeX, closeY, closeW, closeH);
	}
	
	public void showRemoteSite(final String strURL, final int closeX_pt, final int closeY_pt, final int closeX_ls, final int closeY_ls, final int closeW, final int closeH) {	
		if( strURL == null || strURL.length() == 0 ) return;
		
		this.remoteCloseXPort = closeX_pt;
		this.remoteCloseYPort = closeY_pt;
		this.remoteCloseXLand = closeX_ls;
		this.remoteCloseYLand = closeY_ls;
		this.remoteCloseW = closeW;
		this.remoteCloseH = closeH;
		
		this.onConfigurationChanged(activity.orientation);
		
		activity.runOnUiThread(new Runnable() {

			public void run() {
				//make the remote web view
				if(remoteView == null) {
					remoteView = activity.remoteWebView;
			        remoteLayout.addView(remoteView);
				}

		        Log.d("[appMobi]", strURL);
				//load the url
				remoteView.loadUrl(strURL);
				//show the view
				remoteLayout.setVisibility(View.VISIBLE);
				//set the flag
				isShowingRemoteSite = true;
				//get focus
				remoteView.requestFocus(View.FOCUS_DOWN);
				remoteView.setOnTouchListener(new View.OnTouchListener() {
		            
					public boolean onTouch(View v, MotionEvent event) {
		                switch (event.getAction()) {
		                    case MotionEvent.ACTION_DOWN:
		                    case MotionEvent.ACTION_UP:
		                        if (!v.hasFocus()) {
		                            v.requestFocus();
		                        }
		                        break;
		                }
		                return false;
		            }
					
		        });
		        remoteClose.bringToFront();
			}
			
		});
	}
	
	public void closeRemoteSite() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				remoteLayout.setVisibility(View.GONE);
				//handle case where closeRemoteSite is called from the remote view
				if(webview!=activity.appView) {
					activity.appView.device.closeRemoteSite();
				}
			}			
		});
    	String remoteCloseEvent = "javascript: var e =document.createEvent('Events');e.initEvent('appMobi.device.remote.close',true,true);document.dispatchEvent(e);"; 
    	injectJS(remoteCloseEvent);
    	isShowingRemoteSite = false;
	}
	
	private void setRMPLandscape() {
		AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(remoteCloseW==0?48:remoteCloseW, remoteCloseH==0?48:remoteCloseH, remoteCloseXLand, remoteCloseYLand);
		remoteClose.setLayoutParams(params);
	}
	
	private void setRMPPortrait() {
		AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(remoteCloseW==0?48:remoteCloseW, remoteCloseH==0?48:remoteCloseH, remoteCloseXPort, remoteCloseYPort);
		remoteClose.setLayoutParams(params);
	}

	public void onConfigurationChanged(final int orientation) {
		activity.runOnUiThread(new Runnable() {

			public void run() {		
				if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
					setRMPLandscape();
				} else {
					setRMPPortrait();
				}
				remoteLayout.invalidate();
			}
		});
	}
	
	public void setBlockedPagesWhitelist(String domainList) {
		webview.setBlockedPagesWhitelist(domainList);
	}
	
	public void stopLoading() {
		webview.stopLoading();
		webview.wasLoadingStopped = true;
	}
	
	public void scanBarcode() {
        Intent intent = new Intent(activity.getResources().getString(R.string.qrcode_scan_action));
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        //hack so activity wont stop
        activity.setLaunchedChildActivity(true);
        activity.startActivityForResult(intent, AppMobiActivity.SCAN_QR_CODE);
    }
	
	public void handleQRCodeResult(int resultCode, Intent intent) {
    	String remoteCloseEvent = null;
        String contents = "";
        String format = "";
    	if(intent!=null) {
	        contents = intent.getStringExtra("SCAN_RESULT");
	        format = intent.getStringExtra("SCAN_RESULT_FORMAT");
    	}
        if (resultCode == Activity.RESULT_OK) {
            // Handle successful scan
    		remoteCloseEvent = "javascript: var e =document.createEvent('Events');e.initEvent('appMobi.device.barcode.scan',true,true);e.success=true;e.codetype='"+format+"';e.codedata='"+contents+"';document.dispatchEvent(e);";
//        } else if (resultCode == Activity.RESULT_CANCELED) {
//            // Handle cancel
//    		remoteCloseEvent = "javascript: var e =document.createEvent('Events');e.initEvent('appMobi.device.barcode.scan',true,true);e.success=true;e.cancelled=true;e.codetype='';e.codedata='';document.dispatchEvent(e);";
        } else {
        	//cancelled or failed
    		remoteCloseEvent = "javascript: var e =document.createEvent('Events');e.initEvent('appMobi.device.barcode.scan',true,true);e.success=false;e.codetype='';e.codedata='';document.dispatchEvent(e);";
        }
        injectJS(remoteCloseEvent);
	}
	
	public void installUpdate() {
		new Thread("AppMobiDevice:installUpdate") {
			public void run() {
				activity.installUpdate(true);
			}
		}.start();
	}
	
	public boolean isUpdateAvailable() {
		Log.d("[appMobi]", "isUpdateAvailable: " + activity.isAnUpdateDownloaded());//TODO - remove after testing
		return activity.isAnUpdateDownloaded();
	}
	
	public String getAppName() {
		Log.d("[appMobi]", "AppMobi.name: " + webview.config.appName);//TODO - remove after testing
		return webview.config.appName;
	}
	
	public String getAppRelease() {
		Log.d("[appMobi]", "AppMobi.release: " + webview.config.releaseName);//TODO - remove after testing
		return webview.config.releaseName;
	}
	
	public String injectJSBeforeReady() {
		StringBuilder js = new StringBuilder();
		
		js.append("AppMobi.available = true;");
		js.append("AppMobi.updateAvailable = " + isUpdateAvailable() + ";");
		js.append("AppMobi.updateMessage = '" + webview.config.installMessage + "';");
		js.append("AppMobi.webRoot = 'http://localhost:58888/" + webview.config.appName + "/" + webview.config.releaseName + "/';");
		js.append("AppMobi.app = '" + webview.config.appName + "';");
		js.append("AppMobi.release = '" + webview.config.releaseName + "';");
		js.append("AppMobi.oauthAvailable = " + webview.oauth.isReady + ";");
		js.append(webview.camera.getPictureListJS());

/*
[result appendFormat:@"\nAppMobi.webRoot = 'http://localhost:58888/%@/%@/';", config.appName, config.relName];
[result appendFormat:@"\nAppMobi.app = '%@'; AppMobi.release = '%@';", config.appName, config.relName];		 
*/
		if(Debug.isDebuggerConnected()) Log.d("[appMobi]",  "injectJSBeforeReady js:" + js);
		
		return js.toString();
		//webview.injectJS(js.toString());
	}
	
	public void addMenuItem(String text, String callback) {
		activity.addOptionsItem(text, callback);
	}
	
	public void hideSplashScreen() {
		webview.hideSplash();
		Log.i("[appMobi]", "splash hidden");
	}
	
	public boolean isSoftKeyboardShowing() {
		return activity.isSoftKeyboardShowing();
	}
}
