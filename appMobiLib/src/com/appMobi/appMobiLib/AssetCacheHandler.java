package com.appMobi.appMobiLib;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;


public class AssetCacheHandler extends DefaultHandler {

	public AssetCache cache;
	public Activity activity;

	public AssetCacheHandler(Activity activity) {
		super();
		this.activity = activity;
	}

	public AssetCache getParsedData() {
		return cache;
	}

	@Override
	public void startDocument() throws SAXException {
		cache = new AssetCache(this.activity);
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (localName.equals("ASSETCACHE")) {
			if(atts.getValue("checkpoint")!=null) cache.checkpoint = Integer.parseInt(atts.getValue("checkpoint"));
		} else if (localName.equals("FILE")) {
			AssetCacheFile file = new AssetCacheFile();
			file.checkpoint = Integer.parseInt(atts.getValue("checkpoint"));
			file.url = atts.getValue("url");
			file.length = Long.parseLong(atts.getValue("length"));
			cache.addFile(file);
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
	}
}
