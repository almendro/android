package com.appMobi.appMobiLib.oauth;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.appMobi.appMobiLib.AppMobiActivity;
import com.appMobi.appMobiLib.FileUtils;

public class ServicesData {
	public Map<String, Service> name2Service;
	private AppMobiActivity activity;
	private final long timestampMod = 262;
	
	public ServicesData(AppMobiActivity activity) {
		this.activity = activity;
		name2Service = new HashMap<String, Service>();
		
		//decrypt services.xml
		String decryptedServicesDotXml = null;
		try {
			decryptedServicesDotXml = decryptServicesDotXml();
		} catch(Exception e) {
			if(Debug.isDebuggerConnected()) Log.d("[appMobi]", e.getMessage(), e);
		}
		//parse decryptedServicesDotXml
		parseServicesXml(decryptedServicesDotXml);
	}

     @Override
	public String toString(){
    	 StringBuffer services = new StringBuffer();
    	 for(Service service:name2Service.values()) {
    		 services.append(service.toString());
    	 }
         return services.toString();
    }
     
    private String decryptServicesDotXml() throws Exception {
		byte[] encryptedData = null;
		String decryptedString = null;
		
		//retrieve the key
		long timestamp = System.currentTimeMillis();
		timestamp -= timestamp%timestampMod;
		URL url = new URL(String.format("http://services.appmobi.com/external/clientservices.aspx?feed=OA&appname=%s&timestamp=%d", activity.configData.appName, timestamp));
		InputStream in = url.openConnection().getInputStream();
		ByteArrayOutputStream keyStream = new ByteArrayOutputStream();
		FileUtils.copyInputStream(in, keyStream);
		in.close();
		byte[] keyBytes = new byte[24], keyStreamBytes = keyStream.toByteArray();
		String keyMark = "key=\"";
		System.arraycopy(keyStreamBytes, (new String(keyStreamBytes).indexOf(keyMark)+keyMark.length()), keyBytes, 0, keyBytes.length);
		keyStream.close();
		
		//read the encrypted data from file into byte[]
		File servicesDotXml = new File(activity.baseDir(), "services.xml");
    	InputStream is = new FileInputStream(servicesDotXml);
	    long length = servicesDotXml.length();
	    encryptedData = new byte[(int)length];
	    int offset = 0, numRead = 0;
	    while (offset < encryptedData.length && (numRead=is.read(encryptedData, offset, encryptedData.length-offset)) >= 0) {
	        offset += numRead;
	    }
	    is.close();
	    
	    //set up crypto objects
	    final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final Cipher decipher = Cipher.getInstance("DESede/ECB/NoPadding");
        decipher.init(Cipher.DECRYPT_MODE, key);
        
        //decrypt data and return string
        final byte[] plainText = decipher.doFinal(encryptedData);
        
        decryptedString = new String(plainText, "UTF-8");
        
        return decryptedString;
    }     
    
	public void parseServicesXml(String servicesDotXml) {

		try {
            /* Create a URL we want to load some xml-data from. */
            //URL url = new URL("https://services.appmobi.com/testing/AppConfig.xml");

            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            ServicesHandler handler = new ServicesHandler(this);
            xr.setContentHandler(handler);

            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(new StringReader(servicesDotXml)));
            /* Parsing has finished. */
            
       } catch (Exception e) {
            /* Display any Error to the GUI. */
            //if(Debug.isDebuggerConnected()) Log.e("[appMobi]", "services.xml Parsing Error", e);
       }
	}
    
}

