package com.appMobi.appMobiLib.oauth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.appMobi.appMobiLib.AppMobiActivity;
import com.appMobi.appMobiLib.AppMobiCommand;
import com.appMobi.appMobiLib.AppMobiWebView;
import com.appMobi.appMobiLib.util.Debug;

public class AppMobiOAuth extends AppMobiCommand {
	
	private Map<String, VerificationCallbackData> millisToVerification = new HashMap<String, VerificationCallbackData>();
	private class VerificationCallbackData {
		OAuthProvider service;
		OAuthConsumer consumer;
		Service serviceData;
		String protectedUrl;
		String httpMethod;
		String body;
		String headers;
		String jsId;
		
		public VerificationCallbackData(OAuthProvider service, OAuthConsumer consumer, Service serviceData, String protectedUrl, String jsId, String httpMethod, String body, String headers)
		{
			this.service = service;
			this.consumer = consumer;
			this.serviceData = serviceData;
			this.protectedUrl = protectedUrl;
			this.httpMethod = httpMethod;
			this.body = body;
			this.headers = headers;
			this.jsId = jsId;
		}
	}
	
	private class Token implements Serializable
	{
		private static final long serialVersionUID = 715000866082812683L;
		private final String token;
		private final String secret;

		public Token(String token, String secret) {
			this.token = token;
			this.secret = secret;
		}
		@Override
		public String toString() {
			return String.format("Token[%s , %s]", token, secret);
		}
	}

	public AppMobiOAuth(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);
	}	
	
	public boolean isReady = false;
	boolean isBusy = false;
	/*
	 * 
	 */
	public void getProtectedData(final String service, final String url, final String id, final String method, final String body, final String headers) {
		if( !webview.config.hasOAuth ) return;
		
		if(activity.services==null) {
			returnNotReadyEvent(id);
			return;
		}
		
		if(isBusy) {
			returnBusyEvent(id);
			return;
		}
		
		isBusy = true;
		
		new Thread("AppMobiOAuth:getProtectedResource") {
			public void run() {
				//default instructions
				//default button label
				
				//get service from services map
				final Service serviceData = activity.services.name2Service.get(service);
				
				if(serviceData==null) {
		    		String js = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.oauth.protected.data',true,true);e.success=false;e.id='%s';e.response='';e.error='%s is not a configured OAuth service.';document.dispatchEvent(e);", id, service);
		    		injectJS(js);
		    		return;
				}
				
				//create consumer
				OAuthConsumer consumer = new CommonsHttpOAuthConsumer(serviceData.appKey, serviceData.secret);
				
				//check if we have an access token saved
				Token accessToken = AppMobiOAuth.this.retrieveAccessTokenForService(serviceData.name);
				
				//if not, try to get one 
				if(accessToken!=null) {
					consumer.setTokenWithSecret(accessToken.token, accessToken.secret);
					getProtectedResourceWithAccessToken(url, id, method, body, headers, consumer, accessToken);
				} else {			
					getNewAccessTokenForService(url, id, method, body, headers, consumer, serviceData);
				}
			}
		}.start();
	}

	private void getProtectedResourceWithAccessToken(String url, String id, String method, String body, String headers, OAuthConsumer consumer, Token accessToken) {
		String js = null;
		
		if(accessToken != null) {
	  	    try {
				//if we do, use it to access protected resource
				DefaultHttpClient client = new DefaultHttpClient();
				HttpUriRequest request;
				
		  	    if("POST".equals(method)) {
		  	    	request = new HttpPost(url);
		  	    	LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
		  	    	//params should be in the format "key1:value1, key2:value2, key3:value3..."
		  	    	String[] bodyParams = body.split("&");
		  	    	for(String param:bodyParams) {
		  	    		String[] keyValue = param.split("=");
		  	    		if(keyValue.length==2)out.add(new BasicNameValuePair(keyValue[0], keyValue[1]));
		  	    	}
					((HttpPost)request).setEntity(new UrlEncodedFormEntity(out));
					
					//do i need this?
			  	    //request.addHeader("Content-Type", "application/x-www-form-urlencoded");

		  	    } else {
		  	    	request = new HttpGet(url);
		  	    }
		  	    
		  	    //handle headers
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
							request.setHeader(field, value);
						}
						
						indexOfTilde = headers.indexOf('~');
						field = null;
						value = null;
					}

				}
		  	    
		  	    //set expect continue false for twitter
				request.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
		  	    
		  	    HttpResponse response = null;
		  	    consumer.sign(request);
	  	    	response = client.execute(request);

	  	    	boolean success = Integer.toString(response.getStatusLine().getStatusCode()).charAt(0)=='2';
	  	    	
	  	    	InputStream is = response.getEntity().getContent();
  	            Writer writer = new StringWriter();

  	            char[] buffer = new char[1024];
  	            try {
  	                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
  	                int n;
  	                while ((n = reader.read(buffer)) != -1) {
  	                    writer.write(buffer, 0, n);
  	                }
  	            } finally {
  	                is.close();
  	            }
  	            String responseBody = writer.toString();
	  	    	
	  	    	
				char[] bom = {0xef,0xbb,0xbf};
				//check for BOM characters, then strip if present
				if(responseBody.charAt(0)==bom[0]&&responseBody.charAt(1)==bom[1]&&responseBody.charAt(2)==bom[2]) {
					responseBody = responseBody.substring(3);
				}
				
				//escape backslashes needed to prserve backslashes due to string evaluation when response is set.
				responseBody = responseBody.replaceAll("\\\\", "\\\\\\\\");
				//escape internal single and double-quotes (although in this case only single need to be escaped)
				responseBody = responseBody.replaceAll("\"", "\\\\\"");
				responseBody = responseBody.replaceAll("\'", "\\\\\'");
				//replace linebreaks with \n
				responseBody = responseBody.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
	  	    	
	  	    	js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.protected.data',true,true);e.success=%s;e.id='%s';" +
	  	    			"e.response='%s';%sdocument.dispatchEvent(e);", success, id, responseBody, success?"":"e.error='"+response.getStatusLine()+"';");

	  	    } catch(Exception e) {
				if(Debug.isDebuggerConnected()) Log.e("[appMobi]", e.getMessage(), e);
	  	    	js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.protected.data',true,true);e.success=false;e.id='%s';" +
	  	    			"e.response='';e.error='%s';document.dispatchEvent(e);", id, e.getMessage());
	  	    }
	  	    
		} else {
			//something failed
  	    	js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.protected.data',true,true);e.success=false;e.id='%s';" +
  	    			"e.response='';e.error='%s';document.dispatchEvent(e);", id, "an unexpected error occurred");
		}
		
		injectJS(js);
		isBusy = false;
	}
	
	private void getNewAccessTokenForService(String url, String id, String method, String body, String headers, final OAuthConsumer consumer, Service serviceData) {
		boolean success = false;
		try {
			//init provider 
			OAuthProvider service = new CommonsHttpOAuthProvider(serviceData.requestTokenEndpoint, serviceData.accessTokenEndpoint, serviceData.authorizeEndpoint);
			
			//get request token
			String authUrl = service.retrieveRequestToken(consumer, VerificationActivity.FAKE_OAUTH_CALLBACK);
			
	        //store the data for the callback
			VerificationCallbackData vcd = new VerificationCallbackData(service, consumer, serviceData, url, id, method, body, headers);
			String vcdLookup = Long.toString(System.currentTimeMillis());
			millisToVerification.put(vcdLookup, vcd);
	        
			//show user the verification screen
	        Uri verificationUri = Uri.parse(authUrl);
			final Intent intent = new Intent(activity, VerificationActivity.class);
			intent.putExtra("callbackId", vcdLookup);
			intent.setData(verificationUri);
			activity.setLaunchedChildActivity(true);
			activity.myGUIUpdateHandler.post(new Runnable() {

				public void run() {
					activity.startActivityForResult(intent, AppMobiActivity.OAUTH_VERIFICATION);
				}
				
			});
	        
	        //success!
	        success = true;
		} catch(Exception e){
			if(Debug.isDebuggerConnected()) Log.e("[appMobi]", e.getMessage(), e);
			isBusy = false;
		}
		
	}

	//need to check if user cancelled, then check if accesstoken is valid
	public void oAuthVerificationCallback(int resultCode, Intent intent) { //throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		activity.setLaunchedChildActivity(false);
		
		VerificationCallbackData vcd = null;
		String callbackId = null;
		if(intent!=null) callbackId = intent.getStringExtra("callbackId");
		if(callbackId!=null) {
			vcd = millisToVerification.remove(intent.getStringExtra("callbackId"));
		} else {
			vcd = new VerificationCallbackData(null, null, null, null, null, null, null, null);
		}
		if(resultCode == Activity.RESULT_OK) {
			String verification = intent.getStringExtra("verification");
			
			//get service from services map
			final Service serviceData = vcd.serviceData;
			
			//set up the OAuthService
			OAuthConsumer consumer = vcd.consumer;
			OAuthProvider service = vcd.service;
	
			//use verification to get access token
			Token accessToken = null;
			try {
				service.retrieveAccessToken(consumer, verification);
		        accessToken = new Token(consumer.getToken(), consumer.getTokenSecret());
		        
		        //store access token
		        storeAccessTokenForService(serviceData.name, accessToken);
			} catch(Exception e) {
				if(Debug.isDebuggerConnected()) Log.e("[appMobi]", e.getMessage(), e);
			}
			getProtectedResourceWithAccessToken(vcd.protectedUrl, vcd.jsId, vcd.httpMethod, vcd.body, vcd.headers, consumer, accessToken);
		} else {
			//inject user cancelled
			getProtectedResourceWithAccessToken(vcd.protectedUrl, vcd.jsId, vcd.httpMethod, vcd.body, vcd.headers, null, null);
		}
	}
	
	private String getPrefsKey(String name) {
		return webview.config.appName + '.' + name + ".oauth.token";
	}
	
	private boolean storeAccessTokenForService(String name, Token token) {
		boolean success = false;
		SharedPreferences prefs = activity.getSharedPreferences(getPrefsKey(name), 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("token", token.token);
		editor.putString("secret", token.secret);
		editor.commit();
		return success;
	}
	
	private Token retrieveAccessTokenForService(String name) {
		Token token = null;
		SharedPreferences prefs = activity.getSharedPreferences(getPrefsKey(name), 0);
		if(prefs.contains("token") && prefs.contains("secret")) {
			token = new Token(prefs.getString("token", null), prefs.getString("secret", null));
		}
		return token;
	}
	
	public void unauthorizeService(String service) {
		if( !webview.config.hasOAuth ) return;
		
		if(activity.services==null) {
			returnNotReadyEvent("");
			return;
		}

		if(isBusy) {
			returnBusyEvent("");
			return;
		}

		isBusy = true;
		String js = null;
		
		//get service from map
		boolean didUnauth = false;
		final Service serviceData = activity.services.name2Service.get(service);
		if( serviceData != null ) {
			SharedPreferences prefs = activity.getSharedPreferences(getPrefsKey(serviceData.name), 0);
			if(prefs!=null && prefs.contains("token") && prefs.contains("secret")) {
				SharedPreferences.Editor editor = prefs.edit();
				didUnauth = editor.remove("token").remove("secret").commit();
			}
		}
		
		if( !didUnauth )
		{
  	    	js = String.format(
  	    		"javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.unauthorize',true,true);e.success=false;e.error='%s is not a configured OAuth service.';document.dispatchEvent(e);", 
  	    		service);
		}
		else
		{	
			js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.unauthorize',true,true);e.success=true;e.service='%s';document.dispatchEvent(e);", service);
		}
		
		injectJS(js);
		isBusy = false;
	}
	
	public void returnNotReadyEvent(String iden) {
		String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.unavailable',true,true);e.success=false;e.id='%s';document.dispatchEvent(e);", iden);
		injectJS(js);
	}

	public void returnBusyEvent(String iden) {
		String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.oauth.busy',true,true);e.success=false;e.id='%s';document.dispatchEvent(e);", iden);
		injectJS(js);
	}

}
