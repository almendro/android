package com.appMobi.appMobiLib;


import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BookmarkHandler extends DefaultHandler {

	//https://services.appmobi.com/external/clientservices.aspx?feed=getbrowserbookmarks
	
	public ArrayList<Bookmark> bookmarks;
	
	public BookmarkHandler() {
	}

	public ArrayList<Bookmark> getParsedData() {
		return bookmarks;
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
		if (localName.equals("bookmark")) {
			if(bookmarks == null) {
				bookmarks = new ArrayList<Bookmark>();
			}
			Bookmark bookmark = new Bookmark();
			bookmarks.add(bookmark);
			if(atts.getValue("name")!=null) 
				bookmark.name = atts.getValue("name");
			if(atts.getValue("url")!=null) 
				bookmark.url = atts.getValue("url");
			if(atts.getValue("image")!=null) 
				bookmark.image = atts.getValue("image");
			if(atts.getValue("appname")!=null) 
				bookmark.appName = atts.getValue("appname");
			if(atts.getValue("relname")!=null) 
				bookmark.relName = atts.getValue("relname");
			if(atts.getValue("appconfig")!=null) 
				bookmark.appConfigUrl = atts.getValue("appconfig");
			if(atts.getValue("buypage")!=null) 
				bookmark.buyPage = atts.getValue("buypage");
			if(atts.getValue("paypage")!=null) 
				bookmark.payPage = atts.getValue("paypage");
			if(atts.getValue("bookmarkpage")!=null) 
				bookmark.bookPage = atts.getValue("bookmarkpage");
			if(atts.getValue("webroot")!=null) 
				bookmark.webRoot = atts.getValue("webroot");
			if(bookmark.relName!=null && bookmark.relName.length()>0) {
				bookmark.isAnApp = true;
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
