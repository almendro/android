package com.appMobi.appMobiLib;


public class AMSNotification {
	public int ident;
	public String data, message, url, target, richhtml, richurl, userkey;
	public boolean isRich;
	
	public AMSNotification() {
	}

     @Override
	public String toString(){
         return 
         "data: " + data + "\n" +
         "message: " + message + "\n" +
         "url: " + url + "\n" +
         "target: " + target + "\n" +
         "richhtml: " + richhtml + "\n" +
         "richurl: " + richurl + "\n" +
         "userkey: " + userkey;
     }
}