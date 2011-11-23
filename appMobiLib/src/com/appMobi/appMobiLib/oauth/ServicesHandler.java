package com.appMobi.appMobiLib.oauth;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ServicesHandler extends DefaultHandler {

	/* 

		<xml>
			<service name="servicename1" appkey="appkey" secretkey="secretkey" requesttokenendpoint="requesttokenendpoint" authorizeendpoint="authorizeendpoint" accesstokenendpoint="accesstokenendpoint verb="POST"/>
			<service name="servicename2" appkey="appkey" secretkey="secretkey" requesttokenendpoint="requesttokenendpoint" authorizeendpoint="authorizeendpoint" accesstokenendpoint="accesstokenendpoint" verb="POST"/>
		</xml>
	 */
	
	public ServicesData data;
	
	public ServicesHandler(ServicesData data) {
		this.data = data;
	}

	public ServicesData getParsedData() {
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
		if (localName.equals("service")) {
			Service service = new Service();
			if(atts.getValue("name")!=null) service.name = atts.getValue("name");
			if(atts.getValue("appkey")!=null) service.appKey = atts.getValue("appkey");
			if(atts.getValue("secretkey")!=null) service.secret = atts.getValue("secretkey");
			if(atts.getValue("requesttokenendpoint")!=null) service.requestTokenEndpoint = atts.getValue("requesttokenendpoint");
			if(atts.getValue("authorizeendpoint")!=null) service.authorizeEndpoint = atts.getValue("authorizeendpoint");
			if(atts.getValue("accesstokenendpoint")!=null) service.accessTokenEndpoint = atts.getValue("accesstokenendpoint");
			if(atts.getValue("verb")!=null) service.verb = atts.getValue("verb");
			data.name2Service.put(service.name, service);
		}

	}	
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (localName.equals("service")) {}
	}

	@Override
	public void characters(char ch[], int start, int length) {
	}
}
