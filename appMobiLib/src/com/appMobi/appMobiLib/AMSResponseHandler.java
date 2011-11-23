package com.appMobi.appMobiLib;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.appMobi.appMobiLib.util.Base64;

public class AMSResponseHandler extends DefaultHandler {

	private AMSResponse responseBeingParsed;
	private String currentProperty;
	
	public AMSResponseHandler() {
	}

	public AMSResponse getParsedData() {
		return responseBeingParsed;
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
		if(localName.equals("function")) {
			responseBeingParsed = new AMSResponse();
			if(atts.getValue("name")!=null) responseBeingParsed.name = atts.getValue("name");			
			if(atts.getValue("return")!=null) responseBeingParsed.result = atts.getValue("return");			
			if(atts.getValue("msg")!=null) responseBeingParsed.message = atts.getValue("msg");			
			if(atts.getValue("username")!=null) responseBeingParsed.user = atts.getValue("username");			
			if(atts.getValue("email")!=null) responseBeingParsed.email = atts.getValue("email");			
			
		} else if(localName.equals("message")) {
			AMSNotification notification = new AMSNotification();
			responseBeingParsed.notifications.add(notification);
			
			if(atts.getValue("id")!=null) notification.ident = Integer.parseInt(atts.getValue("id"));	
			if(atts.getValue("userdata")!=null) notification.data = atts.getValue("userdata");	
			if(atts.getValue("message")!=null) notification.message = atts.getValue("message");	
			if(atts.getValue("userurl")!=null) notification.url = atts.getValue("userurl");	
			if(atts.getValue("usertarget")!=null) notification.target = atts.getValue("usertarget");
			if(atts.getValue("richhtml")!=null) notification.richhtml = new String(Base64.decode(atts.getValue("richhtml"), Base64.DEFAULT));	
			if(atts.getValue("richurl")!=null) notification.richurl = atts.getValue("richurl");	
			notification.isRich = ( notification.richurl.length() > 0 || notification.richhtml.length() > 0 );
			if(atts.getValue("userkey")!=null) notification.userkey = atts.getValue("userkey");	

		}
	}	
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (localName.equals("CONFIG")) {
		} else if (localName.equals("BUNDLE")) {
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		if(currentProperty!=null) currentProperty += ch;
	}
}
