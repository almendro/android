package com.appMobi.appMobiLib;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.google.ads.AdSenseSpec;
import com.google.ads.AdViewListener;
import com.google.ads.GoogleAdView;
import com.google.ads.AdSenseSpec.AdFormat;
import com.google.ads.AdSenseSpec.AdType;

/*
	whiteLabel = @"WL=APPMOBI";
	adSenseApplicationAppleID = @"358004585";
	adSenseAppName = @"appMobi";
	adSenseCompanyName = @"appMobi";
	adSenseAppWebContentURL = @"www.appmobi.fm";
	adSenseChannelID = @"9919579637";

	attributes = [NSDictionary dictionaryWithObjectsAndKeys:
				 @"ca-mb-app-pub-8844122551050209", kGADAdSenseClientID,
				 [NSArray arrayWithObjects:curad.strGoogleID, myDelegate.adSenseChannelID, nil], kGADAdSenseChannelIDs,
				 [NSNumber numberWithInt:0], kGADAdSenseIsTestAdRequest,
				 kGADAdSenseImageAdType, kGADAdSenseAdType,
				 myDelegate.adSenseApplicationAppleID, kGADAdSenseApplicationAppleID,
				 myDelegate.adSenseAppName, kGADAdSenseAppName,
				 myDelegate.adSenseCompanyName, kGADAdSenseCompanyName,
				 myDelegate.adSenseAppWebContentURL, kGADAdSenseAppWebContentURL,
				 nil];
//*/

public class GoogleAdActivity extends Activity implements AdViewListener {

    public static String CLIENT_ID = "ca-mb-app-pub-8844122551050209";
    public static String COMPANY_NAME = "appMobi";
    public static String APP_NAME = "appMobi";
    public static String CHANNEL_ID = "8595598965+rock_music";
    public static String WEB_URL = "www.flycast.fm";
    
    public GoogleAdView adView;
    public AdSenseSpec adSenseSpec;
	public ImageButton btnClose;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google);
        btnClose = (ImageButton) findViewById(R.id.close);
        btnClose.setOnClickListener(onClickListener);

        // Set up GoogleAdView.
        adView = (GoogleAdView) findViewById(R.id.adview);
        adSenseSpec =
            new AdSenseSpec(CLIENT_ID)     // Specify client ID. (Required)
        	.setChannel(CHANNEL_ID)        // Set channel ID.
            .setAdType(AdType.IMAGE)        // Set ad type to Text.
            .setAdFormat(AdFormat.FORMAT_300x250)
            //.setAppId(APP_NAME)          // Set application name. (Required)
            .setAppName(APP_NAME)          // Set application name. (Required)
            .setCompanyName(COMPANY_NAME)  // Set company name. (Required)
            .setWebEquivalentUrl(WEB_URL)
            .setAdTestEnabled(true);       // Keep true while testing.
        
        adView.setAdViewListener(this);
        adView.showAds(adSenseSpec);
    }    

	private OnClickListener onClickListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};
    
    public void onStartFetchAd()
    {
    }

    public void onFinishFetchAd()
    {
    }

    public void onClickAd()
    {
    }
}
