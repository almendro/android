package com.appMobi.appMobiLib;

import java.util.Vector;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.Criteria;
import android.content.Context;
import android.os.Bundle;

public class AppMobiGeolocation extends AppMobiCommand {
	public static boolean debug = true;
	LocationManager locMan;
	Vector<MobiLocationListener> listeners;
	boolean listening, watching;
	
	public AppMobiGeolocation(AppMobiActivity activity, AppMobiWebView webview) { 
		super(activity, webview);
		locMan = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		listeners = new Vector<MobiLocationListener>();
	}
	/*
	 * Return current position.
	 * Note:	errorCallback and options arguments may be null.
	 * 			successCallback will be called with latitude, longitude, etc.
	 */
	public synchronized long getCurrentPosition(int successCallbackID, int errorCallbackID, 
			int maximumAge, boolean highAccuracy) {
		//TODO: timeout?
		String provider = getBestProvider(highAccuracy);
		MobiLocationListener listener = newListener(true, successCallbackID, errorCallbackID);
		locMan.requestLocationUpdates(provider, 0, 0, listener);
		return listener.index;
	}
	/*
	 * Return current position periodically until clearWatch is called.
	 * Note:	errorCallback and options arguments may be null.	 
	 * 			successCallback will be called with latitude, longitude, etc.
	 */
	public synchronized long watchPosition(int successCallbackID, int errorCallbackID,
			int freq, int maximumAge, boolean highAccuracy) {
		String provider = getBestProvider(highAccuracy);
		MobiLocationListener listener = newListener(false, successCallbackID, errorCallbackID);
		//TODO: Timeout?
		//System.out.printf("Loc: new watchpoint, freq = %1$d, index = %2$d\n", freq, listener.index);
		locMan.requestLocationUpdates(provider, freq, 0, listener);
		return listener.index;
	}
	public synchronized void clearWatch(long watchId) {
		MobiLocationListener listener = listeners.elementAt((int)watchId);
		//System.out.println("Loc: clear watchpoint: " + watchId);
		if (listener != null)
			cancelListener(listener);
	}
	public String pollLocation(long id) {
		if (id < 0 || id >= listeners.size())
			return "";
		
		MobiLocationListener listener = listeners.elementAt((int) id);
		//System.out.println("Geo: pollLocation: id = " + id + ", listeners.index = " + listener.index);
		return listener == null ? "" : listener.getLast();
	}
	public void printMessage(String s) {
		System.out.println("Geo: " + s);
	}
	private String getBestProvider(boolean highAccuracy) {
		Criteria c = new Criteria();
		c.setAccuracy(highAccuracy ? Criteria.ACCURACY_FINE : Criteria.ACCURACY_COARSE);
		String provider = locMan.getBestProvider(c, true);
		//System.out.println("getBestProvider: " + provider);
		return provider;
	}
	private synchronized MobiLocationListener newListener(boolean once, int success, int error) {
		int ind = listeners.indexOf(null);	// Get spot for new entry.
		if (ind < 0) {
			ind = listeners.size();
			listeners.setSize(ind + 1);
		}
		MobiLocationListener listener = new MobiLocationListener(ind, once, success, error);
		listeners.setElementAt(listener, ind);
		return listener;
	}
	private synchronized void cancelListener(MobiLocationListener listener) {
		locMan.removeUpdates(listener);
		listeners.setElementAt(null, listener.index);
	}
	// Define a listener that responds to location updates
	class MobiLocationListener implements LocationListener {
		private int successID, errorID;
		private boolean oneTime, locationValid;
		private int index;
		private Location lastLoc;
		
		private MobiLocationListener(int ind, boolean once, int success, int error) {
			oneTime = once; index = ind;
			successID = success; errorID = error;
		}
	    public void onLocationChanged(Location loc) {
	      // Called when a new location is found by the network location provider.
	    	//System.out.printf("Loc: %1$f, %2$f, %3$f\n", loc.getLatitude(), loc.getLongitude(), loc.getAltitude());
	    	if (lastLoc == null)			// Save for when requested.
	    		lastLoc = new Location(loc);
	    	else
	    		lastLoc.set(loc);
	    	locationValid = true;
	    }
	    public String getLast() {
	    	//System.out.println("Geo: getLast, index = " + index);
	    	if (!locationValid)
	    		return "";
	    	String js = String.format(
	    			"%1$d,%2$d,%3$f,%4$f,%5$f,%6$f,%7$f,%8$f,%9$f,%10$d", oneTime?1:0, successID,
	    			lastLoc.getLatitude(), lastLoc.getLongitude(), lastLoc.getAltitude(),
	    			lastLoc.getAccuracy(), 0.00, lastLoc.getBearing(), lastLoc.getSpeed(),
	    			lastLoc.getTime());
	    	//System.out.println("Geo: getLast: js = " + js);
	    	locationValid = false;
	    	if (oneTime)
	    		cancelListener(this);
	    	return js;
	    }
	    private void fail() {
	    	injectJS(String.format("javascript:AppMobi.geolocation.errorCB(%1$d);", errorID));
	    }
	    public void onStatusChanged(String provider, int status, Bundle extras) {
			if(status == 0) {	// Out of service.
				fail();
			} else if(status == 1) {
				// TEMPORARILY_UNAVAILABLE.
			} else {
				// Available
			}
	    }
	    public void onProviderEnabled(String provider) {}
	    public void onProviderDisabled(String provider) {
	    	fail();
	    }
	  };

}
