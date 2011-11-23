package com.appMobi.appMobiLib;

import java.util.ArrayList;

import com.appMobi.appMobiLib.util.Debug;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class AppMobiContacts extends AppMobiCommand {
	static boolean busy = false;
	static String contactBeingEdited = "";
	public AppMobiContacts(AppMobiActivity activity, AppMobiWebView webview) { 
		super(activity, webview);
		
	}
	
	
	public void chooseContact()
	{
		if( busy == true ) {
			String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}
		busy = true;
		
		Intent intent = new Intent(Intent.ACTION_PICK);           
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);           
        activity.startActivityForResult(intent, AppMobiActivity.CONTACT_CHOOSER_RESULT);           
        activity.setLaunchedChildActivity(true);      

	}
	//CONTACT CHOOSER RESULT HANDLER
	public void contactsChooserActivityResult(int requestCode, int resultCode, Intent intent) 
	{
	          
	    Cursor cursor =  activity.managedQuery(intent.getData(), null, null, null, null);
	      cursor.moveToNext();
	      String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
	      String  name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)); 
		
	      if (Debug.isDebuggerConnected()) Log.i("[appMobi]", "SELECTED CONTACT ID:" + contactId + " -- NAME: "+name );
		
	      String jsContacts = getAllContacts();
				
	  	String js =String.format(" var e = document.createEvent('Events');e.initEvent('appMobi.contacts.choose',true,true);e.success=true;e.contactid='%s';document.dispatchEvent(e);",contactId );
	  	injectJS("javascript:"+jsContacts+js);
	  	busy = false;
	    
	  }
	
	public void getContacts()
	{
	    String jsContacts = getAllContacts();
		String js =" var e = document.createEvent('Events');e.initEvent('appMobi.contacts.get',true,true);e.success=true;document.dispatchEvent(e);";
		injectJS("javascript:"+jsContacts+js);
	}
	
	//ADD CONTACT LAUNCHER
	public void addContact()
	{
		if( busy == true ) {
			String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}
		
		busy = true;
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);  
		intent.putExtra(ContactsContract.Intents.Insert.FULL_MODE, true);
		
        activity.startActivityForResult(intent, AppMobiActivity.CONTACT_ADDER_RESULT);           
        activity.setLaunchedChildActivity(true); 
	}
	//CONTACT ADDER RESULT HANDLER
	public void contactsAdderActivityResult(int requestCode, int resultCode, Intent intent) 
	{       
		
		 //Contact Added
	     if(resultCode == Activity.RESULT_OK)
	     {
		     Cursor cursor =  activity.managedQuery(intent.getData(), null, null, null, null);
		     cursor.moveToNext();
		     String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
		    
		     if (Debug.isDebuggerConnected()) Log.i("[appMobi]", "ADDDED CONTACT ID:" + contactId);
		     String js = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.add',true,true);e.success=true;e.contactid='%s';document.dispatchEvent(e);", contactId);
		  
			injectJS("javascript:"+js);
		  	busy = false;
	     }
	     //Contact not Added
	     else
	     {
	    	 String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.add',true,true);e.success=false;e.cancelled=true;document.dispatchEvent(e);";
	    	 injectJS(js);
	    	 busy = false;
	     }
	  
	}
	
	//EDIT CONTACT LAUNCHER
	public void editContact(String contactId)
	{
		if( busy == true ) {
			String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}

		//Determine if Contact ID exists
		Uri res = null; 
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contactId);
		
	    try{res = ContactsContract.Contacts.lookupContact(activity.getContentResolver(), lookupUri);}catch(Exception e){e.printStackTrace();}
	    if(res != null)
	    {
			busy = true;
			Intent intent = new Intent(Intent.ACTION_EDIT);
			contactBeingEdited = contactId;
			intent.setType(ContactsContract.Contacts.CONTENT_TYPE);  
			intent.setData(res);
			//launch activity
	        activity.startActivityForResult(intent, AppMobiActivity.CONTACT_EDIT_RESULT);           
	        activity.setLaunchedChildActivity(true); 
	    }
	    else
	    {
	    	contactBeingEdited = "";
	    	String errjs1 = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.edit',true,true);e.success=false;e.error='contact not found';e.contactid='%s';document.dispatchEvent(e);", contactId);
	    	injectJS("javascript: "+errjs1);
	    }
	}
	//CONTACT EDIT RESULT HANDLER
	public void contactsEditActivityResult(int requestCode, int resultCode, Intent intent) 
	{       
		 //Contact EDITED
	     if(resultCode == Activity.RESULT_OK)
	     {  
		    String js = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.edit',true,true);e.success=true;e.contactid='%s';document.dispatchEvent(e);", contactBeingEdited);
			injectJS("javascript:"+js);
		  	busy = false;
	     }
	     //Contact not Added
	     else
	     {
	    	 String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.edit',true,true);e.success=false;e.cancelled=true;e.contactid='%s';document.dispatchEvent(e);", contactBeingEdited);
	    	 injectJS(js);
	    	 busy = false;
	     }
	     contactBeingEdited = "";
	}
	
	
	public void removeContact(String contactId)
	{
		if( busy == true ) {
			String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.contacts.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
			injectJS(js);
			return;
		}
		
	        try{
	            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contactId);
	            int rowsDeleted = activity.getContentResolver().delete(uri, null, null);
	          
	            if(rowsDeleted > 0)
	            {
		        	String js = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.remove',true,true);e.success=true;e.contactid='%s';document.dispatchEvent(e);", contactId);
		        	injectJS("javascript: "+js);
	            }
	            else
	            {
	        		String js = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.remove',true,true);e.success=false;e.error='error deleting contact';e.contactid='%s';document.dispatchEvent(e);", contactId);
	        		injectJS("javascript: "+js);
	            }
	            busy = false;
	        }
	        catch(Exception e)
	        {
	            System.out.println(e.getStackTrace());
		    	String errjs = String.format("var e = document.createEvent('Events');e.initEvent('appMobi.contacts.remove',true,true);e.success=false;e.error='contact not found';e.contactid='%s';document.dispatchEvent(e);", contactId);
		    	injectJS("javascript: "+errjs);
				busy = false;
				return;
	        }
	 
	      	
	    
	}
	
	private String getAllContacts()
	{ 	
	    	// Run query
	        Uri uri = ContactsContract.Contacts.CONTENT_URI;
	        String[] projection = new String[] {
	                ContactsContract.Contacts._ID,
	                ContactsContract.Contacts.DISPLAY_NAME,
	                ContactsContract.Contacts.LOOKUP_KEY
	        };
	        String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '" +
	                "1" + "'";
	        String[] selectionArgs = null;
	        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

	        Cursor cursor =  activity.managedQuery(uri, projection, selection, selectionArgs, sortOrder);

	    	String jsContacts = "AppMobi.people = [";

	        if (cursor.getCount() > 0)
	        {
	        	while (cursor.moveToNext())
	        	{
		    	    //GRAB CURRENT LOOKUP_KEY;	
				    String lk = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				    String jsPerson = JSONValueForPerson(lk);
				    jsContacts += jsPerson ;
	            }
	        }

	    	jsContacts += "];";
	    	return jsContacts;
	}
	
	public String JSONValueForPerson(String idlk)
    {	
		ContentResolver cr = activity.getContentResolver();
    	//PROCESS NAME ELEMENTS FOR CURRENT CONTACT ID
	    String firstName = "",lastName = "", compositeName = "", id="";
	    String nameWhere = ContactsContract.Data.LOOKUP_KEY + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
	    String[] params = new String[]{idlk, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
	    Cursor nameCur = cr.query(ContactsContract.Data.CONTENT_URI, null, nameWhere ,params , null); 
	    if(nameCur.getCount() > 0)
	    {
	    	nameCur.moveToFirst();
	    	id = nameCur.getString(nameCur.getColumnIndex(ContactsContract.Contacts._ID));

		    firstName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
		    lastName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
		    compositeName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
					
			firstName = (firstName == null)?"":escapeStuff(firstName);
			lastName = (lastName == null)?"":escapeStuff(lastName);
			compositeName = (compositeName == null)?"":escapeStuff(compositeName);
	    }
		
		//PROCESS EMAIL ADDRESES FOR CURRENT CONTACT ID
		Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{id}, null); 
		String emailAddresses = "[]";
		if(emailCur.getCount() > 0)
		{
			emailAddresses = "[";
			while (emailCur.moveToNext())
			{ 
				String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
		 	   	email = escapeStuff(email);
			    //String emailType = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)); 
		 	   
		 	   emailAddresses += "'"+email+"', ";

			} 
			emailAddresses += "]";
		}
		 	emailCur.close();

		 
		//PROCESS PHONE NUMBERS FOR CURRENT CONTACT ID
		Cursor phoneCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null); 
		
		String phoneNumbers = "[]";
		if(phoneCur.getCount() > 0)
		{
			phoneNumbers = "[";
			while (phoneCur.moveToNext())
			{ 
				String phoneNum = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				phoneNum = escapeStuff(phoneNum);
		 	   phoneNumbers += "'"+phoneNum+"', ";
			} 
			phoneNumbers += "]";
		}
			phoneCur.close();

		
		//PROCESS STREET ADDRESSES FOR CURRENT CONTACT ID
		String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
		String[] addrWhereParams = new String[]{id, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};
		Cursor addressCur = cr.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, null, addrWhere, addrWhereParams, null); 
		
		String streetAddresses = "[]";
		if(addressCur.getCount() > 0)
		{
			streetAddresses = "[";
			while (addressCur.moveToNext())
			{ 
				
				String street = addressCur.getString(addressCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
				String city = addressCur.getString(addressCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
				String state = addressCur.getString(addressCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
				String zip = addressCur.getString(addressCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
				String country = addressCur.getString(addressCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
			
				
				street = escapeStuff(street);
				city = escapeStuff(city);
				state = escapeStuff(state);
				zip = escapeStuff(zip);
				country = escapeStuff(country);
				
				String addressstr = String.format("{ street:'%s', city:'%s', state:'%s', zip:'%s', country:'%s' }", 
										street, city, state, zip, country);
				streetAddresses += addressstr;
			
			} 
			streetAddresses += "]";
		}
		addressCur.close();
		
		 	
		 	
		String jsPerson =  String.format("{ id:'%s', name:'%s', first:'%s', last:'%s', phones:%s, emails:%s, addresses:%s }, ",
								idlk, compositeName, firstName, lastName, phoneNumbers, emailAddresses, streetAddresses);
		return  jsPerson.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
		
    }
	
	public String escapeStuff(String myString)
	{
		myString = myString.replaceAll("\\\\", "\\\\\\\\");
		myString = myString.replaceAll("'", "\\\\'");
		return myString;
	}

}
