package com.appMobi.appMobiLib;

import java.util.ArrayList;

public class AMSResponse {
	public String name, result, message, email, user;
	ArrayList<AMSNotification> notifications;
	ArrayList<AMSPurchase> purchases;
	
	public AMSResponse() {
		notifications = new ArrayList<AMSNotification>();
		purchases = new ArrayList<AMSPurchase>();
	}

     @Override
	public String toString(){
         return 
         "name: " + name + "\n" +
         "result: " + result  + "\n" +
         "message: " + message  + "\n" +
         "email: " + email  + "\n" +
         "user: " + user;
     }
}