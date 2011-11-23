package com.appMobi.appMobiLib;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import com.appMobi.appMobiLib.util.Debug;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.WebView;

import com.appMobi.appMobiLib.AppMobiActivity.AppInfo;
import com.appMobi.appMobiLib.util.Base64;

public class PayMobiPayments extends AppMobiCommand {
	
	Map<String, String> payments;
	String paySuccess, payError, paySequence, payPrice, payMerchant, secret, user, payApp, payRel;
	boolean bPaying, bAuthorized, bBuying, bEditing;
	PayConfigData payConfig;
	WebView buyView;//TODO: AppMobiWebView buyView; //built-in webview
	AppMobiWebView paymentView; //equivalent to AMVC's paymentView
	AppMobiCache cache;
	
	private String appUserKey = "payments.user";
	SharedPreferences prefs = null;
	String deviceId = null;

	public PayMobiPayments(AppMobiActivity activity, AppMobiWebView webview) {
		super(activity, webview);
		prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
        deviceId = activity.getDeviceID();
        
        //TODO: paymentView initialization
        paymentView = new AppMobiWebView(activity, activity.configData); //TODO:probably should not init with the configData
//        cache = new AppMobiCache(activity, webview);
//        paymentView.addJavascriptInterface(cache, "AppMobiCache");
        
	}
	
	public boolean isPaying() {
		return bPaying;
	}

	public void setPaying(boolean paying) {
		this.bPaying = paying;
	}

	public boolean isBuying() {
		return bBuying;
	}

	public void setBuying(boolean buying) {
		this.bBuying = buying;
	}
	
	protected void initializePaymentsWithSuccessOrError(String sequence, String success, String error, String price, AppConfigData config, WebView webView) {//AppMobiWebView webView) {

		this.paySuccess = success;
		this.payError = error;

		this.initializePayments(sequence, price, config, webView);
	}
	
	protected void initializePaymentsForAppAndRel(String sequence, String app, String release, String price, AppConfigData config, WebView webView) {//AppMobiWebView webView) {

		this.payApp = app;
		this.payRel = release;
		
		this.initializePayments(sequence, price, config, webView);
	}
	
	private void initializePayments(String sequence, String price, AppConfigData config, WebView webView) {
		String appSecretKey = webview.config.appName + ".secret";
		this.bAuthorized = prefs.contains(appSecretKey);
		this.secret = prefs.getString(appSecretKey, "");
		this.user = prefs.getString(appUserKey, "");
		
		this.paySequence = sequence;
		this.payPrice = price;
		this.payMerchant = config.paymentMerchant;
		buyView = webView;
		
		showPayment(config);
		
		refreshPayments();
		
		if(payConfig.keys!=null) readSecureStore();
	}
	
	private void showPayment(AppConfigData config) {
		paymentView.config = config;
		
		final String url = "http://localhost:58888/idm.1tapnative/3.2.5/index.html";
		
		//TODO
		/*
		payBlocker.hidden = NO;
		paymentView.hidden = NO;
		 */
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.setContentView(paymentView);
				paymentView.loadUrl(url);
			}
		});
	}
	
	public void modifyPayments() {
		String appSecretKey = webview.config.appName + ".secret";
		this.bAuthorized = prefs.contains(appSecretKey);
		this.secret = prefs.getString(appSecretKey, "");
		this.user = prefs.getString(appUserKey, "");
		
		editPayment(paymentView.config);

		bEditing = true;
		
		refreshPayments();
		if(payConfig.keys!=null) readSecureStore();
	}
	
	private void editPayment(AppConfigData config) {
		
		paymentView.config = config;
		
		String url = "http://localhost:58888/idm.1tapnative/3.2.5/edit.html";
		paymentView.loadUrl(url);
		
		//TODO
		/*
	payBlocker.hidden = NO;
	paymentView.hidden = NO;
		 */
	}
	
	private void hidePayment() {
		//TODO
		/*
		payBlocker.hidden = YES;
		 */
		paymentView.loadUrl("about:blank");
		/*
		paymentView.hidden = YES;
		 */
	}
	
	public void authorize(String secret, String user) {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		String appSecretKey = webview.config.appName + ".secret";
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString(appSecretKey, secret);
		editor.putString(appUserKey, user);
		editor.commit();
		
		this.secret = secret;
		this.user = user;
		this.bAuthorized = true;
		
		refreshPayments();
		if(payConfig.keys!=null) readSecureStore();
		
		String js = "var e = document.createEvent('Events');e.initEvent('payMobi.payments.authorize',true,true);document.dispatchEvent(e);";
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}
	
	public void updatePaymentInfo(String preferredId, String info, boolean verify) {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		payConfig.pref = preferredId;
		payConfig.data = info;
		payConfig.isVerified = verify;
		
		String js = "var e = document.createEvent('Events');e.initEvent('payMobi.payments.update',true,true);document.dispatchEvent(e);";
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}
	
	public void cancelPayment() {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		if(!bEditing) {
			String js = null;
			if(!bBuying) {
				js = "var e = document.createEvent('Events');e.initEvent('appMobi.payments.buy',true,true);e.success=false;e.message='user cancelled';e.sequence='" + this.paySequence + "';document.dispatchEvent(e);";
			} else {
				String obj = "eval(\"({ 'cancelled':true, 'verified':%@, 'payment':'{}', 'data':'{}' })\")" + (this.payConfig.isVerified ? "true" : "false");
				js = this.payError + "(" + obj + "," + this.paySequence + ");";
			}
			if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
			injectJS(js);
		}
		
		hidePayment();
		bPaying = false;
		bBuying = false;
		bEditing = false;
	}
	
	public void makePayment(String dirty) {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		if("1".equals(dirty)) {
			refreshPayments();
		}
		
		String payment = payments.get(payConfig.pref);
		if(payment == null) {
			payment = "";
		}
		payment = payment.replaceAll("\"", "\\\"");
		String data = payConfig.data.replaceAll("\"", "\\\"");
		
		if(bBuying) {
			String payPage = null;
			String app = this.paySuccess;
			// SEQUENCE -- USER -- APP -- CCINFO
			ArrayList<Bookmark> bookmarks = activity.bookmarks;
			for(Bookmark bookmark:bookmarks) {
				if(bookmark.appName.equals(app)&&bookmark.config!=null) {
					payPage = bookmark.payPage;
					break;
				}
			}
			
			String newSeq = URLEncoder.encode(this.paySequence);
			String newApp = URLEncoder.encode(app);
			String newRel = URLEncoder.encode(this.payError);
			String newUser = URLEncoder.encode(this.user);
			String newCC = URLEncoder.encode(payment);
			String newData = URLEncoder.encode(data);

			String payParams = "&SEQUENCE=" + newSeq + "&APP=" + newApp + "&REL=" + newRel + "&USER=" + newUser + "&CCINFO=" + newCC + "&USERDATA=" + newData;
			String payUrl = payPage + payParams;
			activity.redirect(payUrl);
			
		} else {
			String obj = "eval(\"({ 'cancelled':false, 'verified':" + (payConfig.isVerified ? "true" : "false") + ", 'payment':'" + payment + "', 'data':'" + data + "' })\")";
			final String js = "\"" + (payConfig.isVerified ? paySuccess : payError) + "(" + obj + "'" + this.paySequence + "');";
			if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
			activity.runOnUiThread(new Runnable() {

				public void run() {
					buyView.loadUrl(js);
				}

			});
		}
		
		hidePayment();
		bPaying = false;
		bBuying = false;
	}
	
	public void editPayment(String iden, String data) {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		payments.put(iden, data);
		writeSecureStore();
		
		final String js = "var e = document.createEvent('Events');e.initEvent('payMobi.payments.edit',true,true);e.id='" + iden + "';e.success=true;document.dispatchEvent(e);";
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
		
	}
	
	public void removePayment(String iden) {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		String data = payments.remove(iden);
		writeSecureStore();

		final String js = "var e = document.createEvent('Events');e.initEvent('payMobi.payments.remove',true,true);e.id='" + iden + "';e.success=" + (data == null ? "false" : "true") + ";document.dispatchEvent(e);"; 
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}
	
	public void getPayments() {
		String payData = paymentsToJSON();
		final String js = "var e = document.createEvent('Events');e.initEvent('payMobi.payments.get',true,true);e.price='" + this.payPrice + "';e.merchant='" + this.payMerchant + "';e.authorized=" + (bAuthorized?"true":"false") + ";e.payments=" + payData + ";document.dispatchEvent(e);";  
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}
	
	private void refreshPayments() {
		if(!webview.config.hasPayments) return;//TODO - switch to appmobiwebview.config
		
		String server = webview.config.paymentServer;//TODO - switch to appmobiwebview.config
		String url = server + "/paymobi/paymobiservices.aspx?cmd=getapppaymentinfo&appname=" + webview.config.appName + "&deviceid=" + deviceId + "&secret=" + this.secret; //TODO - switch to appmobiwebview.config
		
		try {
			this.payConfig = parsePayConfig(new URL(url));
		} catch (MalformedURLException e) {
			if(Debug.isDebuggerConnected()) Log.d("[appMobi]", e.getMessage(), e);
		}
		
	}

	private PayConfigData parsePayConfig(URL configUrl) {
		PayConfigData data = null;

		try {
            /* Create a URL we want to load some xml-data from. */
            //URL url = new URL("https://services.appmobi.com/testing/AppConfig.xml");

            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            PayConfigHandler handler = new PayConfigHandler();
            xr.setContentHandler(handler);

            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(configUrl.openStream()));
            /* Parsing has finished. */

            /* Our ExampleHandler now provides the parsed data to us. */
            data = handler.getParsedData();
            
       } catch (Exception e) {
            /* Display any Error to the GUI. */
            if(Debug.isDebuggerConnected()) Log.e("error", "AppConfig Parsing Error", e);
       }
       
       return data;
	}

	private void createSecureStore() {
		
		//check if the secure store exists already
		boolean exists = false;
		Cursor cur = null;
		try {
			cur = activity.getContentResolver().query(Data.CONTENT_URI, new String[] {Data.DATA14}, Data.MIMETYPE + " = '" + StructuredName.CONTENT_ITEM_TYPE + "' AND " + Data.DATA14 + " = '" + deviceId + "'", null, null);
			if (cur.moveToFirst()) {
				exists = true;
			}
		} catch (Exception e) {
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		
		if(exists) {
			return;
		}
		
        //create the raw contact
        ArrayList<ContentProviderOperation> op_list = new ArrayList<ContentProviderOperation>(); 
        op_list.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI) 
            .withValue(RawContacts.ACCOUNT_TYPE, "") 
            .withValue(RawContacts.ACCOUNT_NAME, "") 
            .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED) 
            .build()); 

		// create the secure store entry
		op_list.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, 0)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.GIVEN_NAME, "SECURED")
				.withValue(StructuredName.MIDDLE_NAME, "DATA")
				.withValue(StructuredName.FAMILY_NAME, "DO NOT DELETE")
				.withValue(Data.DATA14, deviceId)
				.withValue(Data.DATA15, "")
				.build());

		// Ask the Contact provider to create a new contact
		try {
			ContentProviderResult[] results = activity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, op_list);
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", results[1].toString());
			}
		} catch (Exception e) {
			// Display warning
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
	}

	private void parseSecureStore(String payData) {
		payments.clear();
		
		String[] lines = payData.split("\n");
		for(String line:lines) {
			if(line.length()==0) continue;
			String[] parts = line.split("-=-");
			payments.put(parts[0], parts[1]);
		}
	}

	private String formatSecureStore() {
		StringBuilder payData = new StringBuilder();
		
		for(String key:payments.keySet()) {
			payData.append(key);
			payData.append("-=-");
			payData.append(payments.get(key));
			payData.append("\n");
		}
		
		return payData.toString();
	}

	private String encryptText(String text) {
        byte[] cipherText = null;
		try {
			final MessageDigest md = MessageDigest.getInstance("md5");
			final byte[] digestOfPassword = md.digest(payConfig.keys.getBytes("utf-8"));
			final byte[] keyBytes = new byte[24];
			System.arraycopy(digestOfPassword, 0, keyBytes, 0, Math.min(digestOfPassword.length, 24));
			for (int j = 0, k = 16; j < 8;) {
			        keyBytes[k++] = keyBytes[j++];
			}

			final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
			final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
			final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);

			final byte[] plainTextBytes = text.getBytes("utf-8");
			cipherText = cipher.doFinal(plainTextBytes);
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}

        return Base64.encodeToString(cipherText, Base64.DEFAULT);
    }
	
	private String decryptText(String text) {
		byte[] cipherBytes = Base64.decode(text, Base64.DEFAULT);
		String plainText = null;
	    try
        {
			final MessageDigest md = MessageDigest.getInstance("md5");
			final byte[] digestOfPassword = md.digest(payConfig.keys.getBytes("utf-8"));
			final byte[] keyBytes = new byte[24];
			System.arraycopy(digestOfPassword, 0, keyBytes, 0, Math.min(digestOfPassword.length, 24));
			for (int j = 0,  k = 16; j < 8;)
			{
				keyBytes[k++] = keyBytes[j++];
			}
			
			final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
			final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
			final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			decipher.init(Cipher.DECRYPT_MODE, key, iv);
			
			plainText = new String(decipher.doFinal(cipherBytes));
			
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}	
	    return plainText;
	}

	private void readSecureStore() {
		createSecureStore();
		
		String cipherText = null;
		
		// lookup the data
		Cursor cur = null;
		try {
			cur = activity.getContentResolver().query(Data.CONTENT_URI, new String[] { Data.DATA15 }, Data.MIMETYPE + " = '" + StructuredName.CONTENT_ITEM_TYPE + "' AND " + Data.DATA14 + " = '" + deviceId + "'", null, null);
			if (cur.moveToFirst()) {
				cipherText = cur.getString(0);
			}
		} catch (Exception e) {
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		
		String payData = decryptText(cipherText);
		if(payData!=null) {
			parseSecureStore(payData);
		} else {
			payments.clear();
		}
	}
	
	private void writeSecureStore() {
		String payData = formatSecureStore();
		String encryptedPayData = encryptText(payData);
		
		// update the data entry
        ArrayList<ContentProviderOperation> op_list = new ArrayList<ContentProviderOperation>(); 
		op_list.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
				.withValue(Data.DATA15, encryptedPayData)
				.withSelection(Data.DATA14 + "=?", new String[]{deviceId})
				.build());

		try {
			ContentProviderResult[] results = activity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, op_list);
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", results[0].toString());
			}
		} catch (Exception e) {
			// Display warning
			if (Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
	}
	
	private String paymentsToJSON() {
		StringBuilder payData = new StringBuilder("[");
		
		for(String key:payments.keySet()) {
			payData.append("{ 'id':'");
			payData.append(key);
			payData.append("', 'data':'");
			payData.append(payments.get(key));
			payData.append("'},");
		}
		
		payData.append("]");
		
		return payData.toString();
	}
}