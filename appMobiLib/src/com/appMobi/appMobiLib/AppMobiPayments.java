package com.appMobi.appMobiLib;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.appMobi.appMobiLib.AppMobiActivity.AppInfo;

public class AppMobiPayments extends AppMobiCommand {
	
	//private AppConfigData currentConfig = null;
	private View approvePaymentView = null;
    private TextView payMerchantName = null;
    private ImageView payMerchantIcon =null;
    private PaymentEvent payEvent = null;
	
	public AppMobiPayments(AppMobiActivity activity, AppMobiWebView webview) {
		super(activity, webview);
		
        
	}

	public void buyApplicationInternal(String appName, String relName, String sequence, String price) {
		PayMobiPayments payMobi = activity.payMobi;
		payMobi.bBuying = true;
		payMobi.initializePaymentsForAppAndRel(sequence, appName, relName, price, activity.configData, webview);//TODO: should be an appMobiWebView
	}
	
	//called from buyPage 
	public void buyApplication(String appName, String relName, String sequence, String price) {
		if(!activity.configData.hasPayments) return;
		
		PayMobiPayments payMobi = activity.payMobi;
		if(payMobi.bPaying) return;
		payMobi.bPaying = true;
		
		String permission = "payments." + appName;
		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		boolean approved = prefs.getBoolean(permission, false);
		
		//lookup bookmark
		Bookmark bookmark = activity.getBookmarkForAppName(appName);
		
		if(false && bookmark.isDeleted && bookmark.isAuthorized) {
			rebuyApp(); 
			payMobi.bPaying = false;
			return;
		}
		
		if(approved) {
			buyApplicationInternal(appName, relName, sequence, price);
		} else{
			this.payEvent = new PaymentEvent(PaymentEventType.BUYAPPLICATION, Arrays.asList(new String[]{appName, relName, sequence, price}));
			approvePayments();
		}
	}
	
	public void getPaymentInfoInternal(String success, String error, String sequence, String price) {
		activity.payMobi.initializePaymentsWithSuccessOrError(sequence, success, error, price, activity.configData, webview);//TODO: should be an appMobiWebView
	}
	
	public void getPaymentInfo(final String successCallback, final String errorCallback, final String sequence, final String price) {
		if(!activity.configData.hasPayments) return;
		
		if(activity.payMobi.bPaying) return;
		activity.payMobi.bPaying = true;
		
		String permission = "payments." + activity.configData.appName;
		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		boolean approved = prefs.getBoolean(permission, false);
		
//		listener.onPaymentEvent(event);
		
		if(approved) {
			getPaymentInfoInternal(successCallback, errorCallback, sequence, price);
		} else {
			this.payEvent = new PaymentEvent(PaymentEventType.GETPAYMENTINFO, Arrays.asList(new String[]{successCallback, errorCallback, sequence, price}));
			approvePayments();
		}
	}
	
	public void editPaymentInfoInternal() {
		activity.payMobi.bBuying = false;
		activity.payMobi.modifyPayments();
	}
	
	public void editPaymentInfo(final String successCallback, final String errorCallback, final String sequence, final String price) {
		if(!activity.configData.hasPayments) return;
		
		if(activity.payMobi.bPaying) return;
		activity.payMobi.bPaying = true;
		
		String permission = "payments." + activity.configData.appName;
		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		boolean approved = prefs.getBoolean(permission, false);
		
		if(approved) {
			editPaymentInfoInternal();
		} else {
			this.payEvent = new PaymentEvent(PaymentEventType.EDITPAYMENTINFO, Arrays.asList(new String[]{successCallback, errorCallback, sequence, price}));
			approvePayments();
		}
	}
	
	private void approvePayments() {
		
		StringBuilder iconFileName = new StringBuilder();
		iconFileName.append(activity.baseDir());
		iconFileName.append("/merchant.png");
		final File iconFile = new File(iconFileName.toString());
		
		activity.runOnUiThread(new Runnable() {
			public void run() {
		        activity.setContentView(R.layout.payment);

		        ImageButton allowPayment = (ImageButton) activity.findViewById(R.id.pay_allow);
		        ImageButton denyPayment = (ImageButton) activity.findViewById(R.id.pay_deny);
		        payMerchantName = (TextView) activity.findViewById(R.id.pay_merchant_name_txt);
		        payMerchantIcon = (ImageView) activity.findViewById(R.id.pay_merchant_icon);
		        
		        allowPayment.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						onPayAgree();
					}
		        	
		        });
		        
		        denyPayment.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						onPayCancel();
					}
		        	
		        });
				payMerchantName.setText(activity.configData.paymentMerchant);
				payMerchantIcon.setImageURI(Uri.fromFile(iconFile));
			}
		});
	}

	//TODO: handle push (bApprovePAy in ios)
	private void onPayAgree() {
		String permission = "payments." + activity.configData.appName;
		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(permission, true);
		editor.commit();
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.setContentView(activity.root);
			}
		});
		if(PaymentEventType.GETPAYMENTINFO.equals(this.payEvent.type)) {
			this.getPaymentInfoInternal(this.payEvent.params.get(0), this.payEvent.params.get(1), this.payEvent.params.get(2), this.payEvent.params.get(3));
		} else if(PaymentEventType.BUYAPPLICATION.equals(this.payEvent.type)) {
			this.buyApplicationInternal(this.payEvent.params.get(0), this.payEvent.params.get(1), this.payEvent.params.get(2), this.payEvent.params.get(3));
		} else if(PaymentEventType.EDITPAYMENTINFO.equals(this.payEvent.type)){
			this.editPaymentInfoInternal();
		}
	}
	
	//TODO: handle push (bApprovePAy in ios)
	private void onPayCancel() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.setContentView(activity.root);
			}
		});

		activity.payMobi.bPaying = false;

		StringBuilder js = new StringBuilder();
		if(PaymentEventType.GETPAYMENTINFO.equals(this.payEvent.type)) {
			js.append(this.payEvent.params.get(1));//error
			js.append("(");
			js.append("eval(\"({ 'cancelled':true, 'verified':false, 'payment':'{}', 'data':'{}' })\")");
			js.append(",'");
			js.append(this.payEvent.params.get(2));//sequence
			js.append("');");
		} else if(PaymentEventType.BUYAPPLICATION.equals(this.payEvent.type)) {
			js.append("var e = document.createEvent('Events');e.initEvent('appMobi.payments.buy',true,true);e.success=false;e.message='user cancelled';e.sequence='");
			js.append(this.payEvent.params.get(2));//sequence
			js.append("';document.dispatchEvent(e);");
		}
		if(js.length()>0) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", js.toString());
				this.injectJS(js.toString());
			}
		}
	}

	private void rebuyApp() {
		// TODO implement rebuyApp()
		
	}
	
	public void updateApplications() {
		if(!activity.configData.hasPayments) return;

		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		String appUser = prefs.getString("payments.user", "");
		StringBuilder urlString = new StringBuilder("https://services.appmobi.com/external/AppPurchases.apsx?cmd=getauthorizedapps&userid=");
		urlString.append(appUser);
		
		AMSResponse response = null;
		try {
            URL url = new URL(urlString.toString());
            XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            AMSResponseHandler handler = new AMSResponseHandler();
            xr.setContentHandler(handler);
            InputStream is = url.openStream();
            xr.parse(new InputSource(is));
            response = handler.getParsedData();
            is.close();
       } catch (Exception e) {
            if(Debug.isDebuggerConnected()) Log.e("error", "AppConfig Parsing Error w/ url: " + urlString, e);
	   }
       
       boolean bNewPurchases = false;
       
       for(AMSPurchase purchase:response.purchases) {
    	   Bookmark bookmark = activity.getBookmarkForAppName(purchase.app);
    	   if(purchase.isAuthorized && !bookmark.isInstalled) {
    		   bookmark.isAuthorized = true;
    		   bNewPurchases = true;
    	   }
       }
       
       if(bNewPurchases) {
    	   new Thread("AppMobiPayments:updateApplications()") {
    		   public void run() {
    			   authorizeApps();
    		   }
    	   }.start();
       }
       
	}
	
	private void authorizeApps() {
		//TODO: implement authorizeApps
	}
	

//TODO: temp for mobius to compile
public void makeFakePayment(PaymentEvent event) {
//	//flag bookmark as authorized: this is a hack because the payment system should do this
//	event.bookmark.isInstalling = true;
//
//	String sequence = event.params.get(2);
//	String app = event.bookmark.appName;
//	String rel = event.bookmark.relName;
//	//&USER=ian5%40appmobi.com&CCINFO=%257B\%2522cc_num\%2522:\%25224111111111111111\%2522,\%2522cc_exp\%2522:\%25221/2011\%2522,\%2522cc_cvv2\%2522:\%2522458\%2522,\%2522cc_holder\%2522:\%2522First%20Last\%2522,\%2522cc_type\%2522:\%2522VISA\%2522%257D&USERDATA=%257B\%2522ship_zip\%2522:\%252211111\%2522,\%2522bill_country\%2522:\%2522US\%2522,\%2522bill_zip\%2522:\%252211111\%2522,\%2522bill_lastname\%2522:\%2522Lasty\%2522,\%2522ship_address2\%2522:\%2522\%2522,\%2522ship_state\%2522:\%2522PA\%2522,\%2522ship_firstname\%2522:\%2522Firsty\%2522,\%2522bill_firstname\%2522:\%2522Firsty\%2522,\%2522ship_country\%2522:\%2522US\%2522,\%2522ship_lastname\%2522:\%2522Lasty\%2522,\%2522bill_city\%2522:\%2522My%20City\%2522,\%2522bill_state\%2522:\%2522PA\%2522,\%2522bill_address2\%2522:\%2522\%2522,\%2522ship_city\%2522:\%2522My%20City\%2522,\%2522bill_address1\%2522:\%2522121%20My%20Streetz\%2522,\%2522ship_address1\%2522:\%2522121%20My%20Streetz\%2522%257D
//	String user = "ian5%40appmobi.com";
//	String ccinfo = "%257B\\%2522cc_num\\%2522:\\%25224111111111111111\\%2522,\\%2522cc_exp\\%2522:\\%25221/2011\\%2522,\\%2522cc_cvv2\\%2522:\\%2522458\\%2522,\\%2522cc_holder\\%2522:\\%2522First%20Last\\%2522,\\%2522cc_type\\%2522:\\%2522VISA\\%2522%257D";
//	String userdata = "%257B\\%2522ship_zip\\%2522:\\%252211111\\%2522,\\%2522bill_country\\%2522:\\%2522US\\%2522,\\%2522bill_zip\\%2522:\\%252211111\\%2522,\\%2522bill_lastname\\%2522:\\%2522Lasty\\%2522,\\%2522ship_address2\\%2522:\\%2522\\%2522,\\%2522ship_state\\%2522:\\%2522PA\\%2522,\\%2522ship_firstname\\%2522:\\%2522Firsty\\%2522,\\%2522bill_firstname\\%2522:\\%2522Firsty\\%2522,\\%2522ship_country\\%2522:\\%2522US\\%2522,\\%2522ship_lastname\\%2522:\\%2522Lasty\\%2522,\\%2522bill_city\\%2522:\\%2522My%20City\\%2522,\\%2522bill_state\\%2522:\\%2522PA\\%2522,\\%2522bill_address2\\%2522:\\%2522\\%2522,\\%2522ship_city\\%2522:\\%2522My%20City\\%2522,\\%2522bill_address1\\%2522:\\%2522121%20My%20Streetz\\%2522,\\%2522ship_address1\\%2522:\\%2522121%20My%20Streetz\\%2522%257D";
//	String payParams = "&SEQUENCE="+sequence+"&APP="+app+"&REL="+rel+"&USER="+user+"&CCINFO="+ccinfo+"&USERDATA="+userdata;
//	
//	String url = event.bookmark.payPage + payParams;
//	List<String> params = Arrays.asList(new String[]{url});
//	PaymentEvent newEvent = new PaymentEvent(PaymentEventType.BUYAPPLICATIONSTEP2, params);
//	newEvent.bookmark = event.bookmark;
//	listener.onPaymentEvent(newEvent);
//	
}
//TODO: temp for mobius to compile
public void doPaymentInfoCallback(PaymentEvent event, boolean wasVerified, boolean didSucceed) {
//	final String successCallback = event.params.get(0);
//	final String errorCallback = event.params.get(1);
//	final String sequence = event.params.get(2);
//	final String price = event.params.get(3);
//	String js = null;
//	
//	if(wasVerified && didSucceed) {
//		/*
//		ev.data= '{"bill_firstname":"Joe","bill_lastname":"Smith","bill_address1":"123 Fake St","bill_address2":"","bill_city":"Lancaster","bill_state":"PA","bill_zip":"17602","bill_country":"US","ship_firstname":"Joe","ship_lastname":"Smith","ship_address1":"123 Fake St","ship_address2":"","ship_city":"Lancaster","ship_state":"PA","ship_zip":"17602","ship_country":"US"}'
//
//		ev.payment = {
//		        cc_num: "4229336315024609",
//		        cc_exp: "02/2012",
//		        cc_cvv2: "131",
//		        cc_holder: "Joe Smith",
//		        cc_type:"Visa"
//		        }
//			 */
//		String payment = "{\\\"cc_num\\\": \\\"4229336315024609\\\",\\\"cc_exp\\\": \\\"02/2012\\\",\\\"cc_cvv2\\\": \\\"131\\\",\\\"cc_holder\\\": \\\"Joe Smith\\\",\\\"cc_type\\\":\\\"Visa\\\"}";
//		String data = "{\\\"bill_firstname\\\":\\\"Joe\\\",\\\"bill_lastname\\\":\\\"Smith\\\",\\\"bill_address1\\\":\\\"123 Fake St\\\",\\\"bill_address2\\\":\\\"\\\",\\\"bill_city\\\":\\\"Lancaster\\\",\\\"bill_state\\\":\\\"PA\\\",\\\"bill_zip\\\":\\\"17602\\\",\\\"bill_country\\\":\\\"US\\\",\\\"ship_firstname\\\":\\\"Joe\\\",\\\"ship_lastname\\\":\\\"Smith\\\",\\\"ship_address1\\\":\\\"123 Fake St\\\",\\\"ship_address2\\\":\\\"\\\",\\\"ship_city\\\":\\\"Lancaster\\\",\\\"ship_state\\\":\\\"PA\\\",\\\"ship_zip\\\":\\\"17602\\\",\\\"ship_country\\\":\\\"US\\\"}";
//		String obj = "eval(\"({ 'cancelled':false, 'verified':" + (wasVerified ? "true" : "false") + ", 'payment':'" + payment + "', 'data':'" + data + "' })\")";
//		js = "javascript: " +successCallback + "(" + obj + ",'" + sequence + "');";
//	} else {
//		String obj = "eval(\"({ 'cancelled':true, 'verified':" + (wasVerified ? "true" : "false") + ", 'payment':'{}', 'data':'{}' })\")";
//		//String obj = "{ 'cancelled':true, 'verified':" + (wasVerified ? "true" : "false") + ", 'payment':'{}', 'data':'{}' }";
//		js = "javascript: " +errorCallback + "(" + obj + ",'" + sequence + "');";
//	}
//	
//	if(Debug.isDebuggerConnected()) Log.d("[appMobi]",js);
//	this.injectJS(js);
}


/*
 * Utility classes and interfaces
 */
public enum PaymentEventType {
	GETPAYMENTINFO,
	BUYAPPLICATION,
	BUYAPPLICATIONSTEP2,
	UPDATEAPPLICATIONS,
	EDITPAYMENTINFO
}
public class PaymentEvent {
	public PaymentEventType type;
	public List<String> params;
	public Bookmark bookmark;
	public PaymentEvent(PaymentEventType type, List<String> params) {
		this.type = type;
		this.params = params;
	}
}
public interface AppMobiPaymentsListener {
	public void onPaymentEvent(PaymentEvent event);
}

AppMobiPaymentsListener listener;

public void setListener(AppMobiPaymentsListener listener) {
	this.listener = listener;
}


}
