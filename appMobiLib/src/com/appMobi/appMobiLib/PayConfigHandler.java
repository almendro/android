package com.appMobi.appMobiLib;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

public class PayConfigHandler extends DefaultHandler {

	private PayConfigData data;
	
	public PayConfigData getParsedData() {
		return data;
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (localName.equals("getapppaymentinfo")) {
			data = new PayConfigData();
			if(atts.getValue("key")!=null) data.keys = atts.getValue("key");
			if(atts.getValue("preferredpaymentid")!=null) data.pref = atts.getValue("preferredpaymentid");
			if(atts.getValue("extendedinfo")!=null) data.data = atts.getValue("extendedinfo");
			if(atts.getValue("isverified")!=null) data.isVerified = Boolean.getBoolean(atts.getValue("isverified"));
			
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", "PayConfigData - keys:" + data.keys + ", pref:" + data.pref + ", data:" + data.data + ", isVerified: " + data.isVerified);
			}
		}
	}	
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
	}

	@Override
	public void characters(char ch[], int start, int length) {
	}	
}
