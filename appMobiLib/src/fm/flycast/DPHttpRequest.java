package fm.flycast;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * Class to parse the HTTP request. The request can be read from the the socket or passed in the 
 * constructor.
 * 
 * @author Sudeep
 *
 */
public class DPHttpRequest {
	public static final String Command = "CMD";
	public static final String SID = "SID";
	public static final String PROXYURL = "PROXYURL";
	public static final String BITRATE = "BITRATE";
	public static final String UID = "UID";
	public static final String ID = "ID";
	public static final String GUID = "GUID";
	public static final String RECORD = "RECORD";
	public static final String RESUME = "RESUME";	//BH_12-02-09
	
	public static final String BaseQueryString = "BaseQueryString";
	
	public static final String FB30URL="FB30URL";
	public static final String FB60URL="FB60URL";
	public static final String FB120URL="FB120URL";
	public static final String FLYBACK="FLYBACK";

	private Socket _Socket = null;
	public String _Path = null;
	private Map<String, String>         headers = new HashMap<String, String>();
	private Map<String, String> _RequestParams = new HashMap<String, String>();   
    
	
	public DPHttpRequest(Socket s){
		_Socket = s;
		ParseRequest();
	}
	
	public DPHttpRequest(String req){
		ParseRequestString( req);
	}
	
	/**
	 * Return the command from the Request.
	 * It never returns null. Instead if not found it returns empty string.
	 * @return
	 */
	public String GetCommand(){
		String str = _RequestParams.get(Command);
		return str == null? "": str ;
	}
	
	/**
	 * Return the header from the request.
	 * Value returned can be null.
	 * @param header
	 * @return
	 */
	public String GetHeaderValue(String header){
		String str = headers.get(header);
		return str;
	}
	
	/**
	 * Return the value from the request(Both querystring and Post data).
	 * Value returned can be null.
	 * @param header
	 * @return
	 */
	public String GetValue(String name){
		String str = _RequestParams.get(name);
		return str;
	}
	
	
	/**
	 * Extract request, Headers and values from the socket. 
	 * 
	 */
	private void ParseRequest(){
	        String request = "unknown";
	        try {
	            BufferedReader in = new BufferedReader(new InputStreamReader(_Socket.getInputStream()), 8192);
	            
	            // Read the first line from the client.
	            request = in.readLine();
	            String path = request.substring(4, request.length() - 9);
	            ParseRequestString( path);
	            DPThreadLocalUrl.setRequestUrl(request);
	            parseHeaders(in);
	            //parsePost(in);
	        }
	        catch (IOException e) {
	        	e.printStackTrace();
	        }
	}
	
	private void ParseRequestString(String req){
		String path = req;
        //get the URL of the request.
        int i = path.indexOf("?");
        if( i == -1 )
        	_Path = path;
        else
        {
        	_Path = path.substring(0, i);
        //	path = path.substring(i+1, path.length() - _Path.length() + 1);
        	path = path.substring(i+1, path.length());
        	int ipurl = path.indexOf(PROXYURL);
        	if( ipurl != -1){
        		ExtractProxyURL(path);
        		path = path.substring(0, ipurl);
        	}
        }
        
        parseQuery(path);
	}
	
	private void extractFlyBackURLs(String path){
	int flybackFirstIndex=path.indexOf("FB");
		String flybackString=path.substring(flybackFirstIndex, path.length());
		
	        String[] params = flybackString.split("&FB");  
	        int i=0;
	        String name=null;
	        String value=null;
		    for (String param : params)   
		    {   
		    	if(i==0){
		    		 name = param.split("=")[0];
		    		 value= param.substring(name.length()+1, param.length());
		    	}
		    	else{
		    		name = "FB"+param.split("=")[0];
		    		value= param.substring(name.length()-1, param.length());
		    	}   
		    	
		    	_RequestParams.put(name, value);   
		    	i++;
		    }		
	}			

	private void ExtractProxyURL( String q){
		int ipurl = q.indexOf(PROXYURL);
		String noise = PROXYURL + "=";
		String url = q.substring(ipurl + noise.length()); 
		_RequestParams.put(PROXYURL, url);
		/*
    	 * call method to add flyBackUrl to _RequestParams
    	 * 
    	 */
    	
    	extractFlyBackURLs(url);    	
	}
	
	/*
	private void parsePost(BufferedReader in) throws IOException {
		String line = null;
		String postData = "";
        while ((line = in.readLine()) != null) {
        	postData += line.trim();
            }
        String[] params = postData.split("&");   
	    for (String param : params)   
	    {   
	        String name = param.split("=")[0];   
	        String value = param.split("=")[1];   
	        _RequestParams.put(name, value);   
	    }  
	}
	//*/

	private void parseQuery(String query)   
	{   
		if(query.contains("&")){
		    String[] params = query.split("&");   
		    for (String param : params)   
		    {   
		    	String[] splitParams = param.split("=");
		    	if(splitParams.length==0) continue;
		        String name = splitParams[0];
		        String value = splitParams.length>1?param.split("=")[1]:"";   
		        _RequestParams.put(name, value);   
		        
		        if(splitParams.length==1) {
		        	Log.d("[appMobi]", "DPHtppRequest -- querystring name without value: " + splitParams[0] + " with query:" + query);
		        }
		    }   
		} 
	} 
	
	private void parseHeaders(BufferedReader in ) throws IOException {
		//Read in and store all the headers.

		String line = null;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                break;
            }
            int colonPos = line.indexOf(":");
            if (colonPos > 0) {
                String key = line.substring(0, colonPos);
                String value = line.substring(colonPos + 1);
                headers.put(key, value.trim());
            }
        }
	}
}
