package com.appMobi.appMobiLib.oauth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appMobi.appMobiLib.R;

public class VerificationActivity extends Activity {
	
	private LinearLayout container;
	private WebView webview;
	ProgressDialog loading;
	final static String FAKE_OAUTH_CALLBACK = "http://www.example.com/OAuthCallback";
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.oauth_verification);

		container = (LinearLayout)findViewById(R.id.verContainer);
		webview = (WebView)findViewById(R.id.verWebView);
		
		webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient( 
			new WebViewClient() {
				
				@Override
				public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
					super.onReceivedError(view, errorCode, description, failingUrl);
				}

				/*
				 * see: http://stackoverflow.com/questions/4049710/how-to-solve-301-moved-permanently-in-android-webview
				 */
				@Override
				public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
					handler.proceed();
				}

				@Override
				public void onLoadResource(WebView view, String url) {
					super.onLoadResource(view, url);
			        Log.d("[appMobi]", "onLoadResource: " + url);
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					view.loadUrl(url);
			        Log.d("[appMobi]", "loading: " + url);
					webview.requestFocus();
					container.forceLayout();
					return true;
				}

				@Override
				public synchronized void onPageStarted(WebView view, String url, Bitmap favicon) {
					if(url.startsWith(FAKE_OAUTH_CALLBACK)) {
						view.stopLoading();
						
						//found our callback - need to parse request and then setresultandfinish
				        Log.d("[appMobi]", "found: " + url);
				        
				        //parse off the oauth_token and oauth_verifier
				        URI uri = null;
						try {
							uri = new URI(url);
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				        List<NameValuePair> nvps = URLEncodedUtils.parse(uri, "UTF8");
				        String oauthToken = null, oauthVerifier = null;
				        for(NameValuePair nvp:nvps) {
				        	if("oauth_token".equals(nvp.getName())) oauthToken = nvp.getValue();
				        	if("oauth_verifier".equals(nvp.getName())) oauthVerifier = nvp.getValue();
				        }
				        setResultAndFinish(false, oauthVerifier);
				        
					} else {
						//show a loading dialog only if we didnt already show one
						if(loading==null) {
					        loading = ProgressDialog.show(VerificationActivity.this, "Please wait", "Loading service provider verification page...");
					        Log.d("[appMobi]", "started: " + url);
						}
					}
				}

				@Override
				public synchronized void onPageFinished(WebView view, String url) {
					//dismiss loading dialog if one is showing
			        Log.d("[appMobi]", "finished: " + url);
					if (loading!=null) {
						loading.dismiss();
						loading = null;
				        Log.d("[appMobi]", "dismissed: " + url);
					}
				}	
			}	
    	);
        
        String url = getIntent().getData().toString();
        webview.loadUrl(url);
		webview.requestFocus();
		container.forceLayout();
	}


	private void setResultAndFinish(boolean didCancel, String verification) {
        Intent resultIntent = new Intent();
        //copy the callbackid from the request intent to the result intent
        resultIntent.putExtra("callbackId", getIntent().getStringExtra("callbackId"));
        if(verification!=null && !didCancel) {
	        resultIntent.putExtra("verification", verification);
	        setResult(Activity.RESULT_OK, resultIntent);
        } else {
	        setResult(Activity.RESULT_CANCELED, resultIntent);
        }
        finish();
    }

}
