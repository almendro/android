package fm.flycast;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.appMobi.appMobiLib.AppMobiActivity;

public class DPRequestThread  implements Runnable {
	public static final String SDCARD_NOT_PRESENT =  "<XML><ALIVE><SDCARD_Not_Present/></ALIVE></XML>";
	public static final String SDCARD_OUT_OF_MEMORY_IS_ALIVE =  "<XML><ALIVE><SDCARD_Out_Of_Memory/></ALIVE></XML>";
	public static final String SDCARD_OUT_OF_MEMORY =  "<XML><ResourceContainer name=\"OutOfMemory\"></ResourceContainer></XML>";
	public String _path;
	public DPRequestThread(String path, Socket socket) {
		_path = path;
        _socket = socket;
    }
    
    /**
     * Parse the request.
     * Pass the Request to DPCommandHandler to handle the request.
     */
    public void run()  {
    	try
    	{	
    		DPHttpRequest req = new DPHttpRequest(_socket);
            DPCommandHandler handler = new DPCommandHandler(_socket, req);
            int command = GetCommand(req);
            
            if( command == DPCommandHandler.COMMAND_ISALIVE ){
            	//need to handle this differently and not just bail
            	if(!DPMemoryStatus.externalMemoryAvailable()){
//            		SendSDCardErrors(SDCARD_NOT_PRESENT);
//    				return;
    				}
    			else{
    				if(DPMemoryStatus.getAvailableExternalMemorySize() < 4000000){
//                		SendSDCardErrors(SDCARD_OUT_OF_MEMORY_IS_ALIVE);
//    		            return;
    				}
    			  }
            }
            
            if(DPMemoryStatus.getAvailableExternalMemorySize() < 4000000){
            	//need to handle this differently and not just bail
//        		SendSDCardErrors(SDCARD_OUT_OF_MEMORY);
//	            return;
			}
            
            if( command == DPCommandHandler.COMMAND_SENDFILE)
            {
            	sendFile(req);
            	if(Debug.isDebuggerConnected()) Log.d("DPRequestThread", "Received command to play the track for ID " + req._Path);
            }
            else
            {
            	if( command == DPCommandHandler.COMMAND_PLAYTRACK)
	            {
            		if(Debug.isDebuggerConnected()) Log.d("DPRequestThread","Request is to play Track with Guid"+ req.GetValue(DPHttpRequest.GUID));
	            	String tGUID = req.GetValue(DPHttpRequest.GUID);
	            	if(Debug.isDebuggerConnected()) Log.d("DPRequestThread", "Received command to play the track for ID " + tGUID);
	            }
            	handler.HandleCommand(command);
            }
    	}
    	catch(Exception ex){
    		//System.out.println(ex.toString());
    		if(Debug.isDebuggerConnected()) Log.d("DPWebServer", "DPRequestThread Run Exception=" + ex.getMessage(), ex);
    		
    	}
    	finally
    	{
    		try
    		{
    			_socket.close();
    		}
    		catch(Exception ex){
    			//Its safe to eat up this exception .. There is nothing else we can do.
    			if(Debug.isDebuggerConnected()) Log.d("DPWebServer", "DPRequestThread Socket Close Exception=" + ex.getMessage());
    		}
    	}
    }
    
    
	/**
	 * Retun command as int.
	 * 
	 * @param req
	 * @return
	 */
    private int GetCommand(DPHttpRequest req)
	{
		String val = req.GetCommand();
		if(val == null)
			return -1;

		if(val.contains("ALIVE"))
			return DPCommandHandler.COMMAND_ISALIVE;
		else if(val.contains("STARTSTATION"))
			return DPCommandHandler.COMMAND_STARTSTATION;
		else if(val.contains("PLAYTRACK"))
			return DPCommandHandler.COMMAND_PLAYTRACK;
		else if(val.contains("STOPSTATION"))
			return DPCommandHandler.COMMAND_STOPSTATION;
		else if(val.contains("GETSTATUS"))
			return DPCommandHandler.COMMAND_GETMESSAGES;		
		else if(val.contains("GETRECORDING"))				//For Recording
			return DPCommandHandler.COMMAND_GETRECORDINGS;
		else if(val.contains("DELETETRACK"))
			return DPCommandHandler.COMMAND_DELETETRACK;
		else if(val.contains("DELETERECORDINGS"))
			return DPCommandHandler.COMMAND_DELETERECORDINGS;
		else if(val.contains("DELETEALLRECORDINGS"))
			return DPCommandHandler.COMMAND_DELETEALLRECORDINGS;	
		else if(val.contains("RESUME"))
			return DPCommandHandler.COMMAND_RESUME_TRACK;	
		else if(val.contains("LIVE"))
			return DPCommandHandler.COMMAND_LIVE_TRACK;
		else if(val.contains("FLYBACK"))
			return DPCommandHandler.COMMAND_FLYBACK;
			
		return DPCommandHandler.COMMAND_SENDFILE;
	}    
    
    private void SendSDCardErrors(String err){
		try
		{
	    	DPApplication.Instance().CleanUp();
			SendResponseHeader(err.length());
			SendResponse(err);
		}
		catch(Exception ex){
			
		}
    }
    
    /**
	 * Send the header to the client.
	 * @param contentLenght
	 * @throws Exception
	 */
	private void SendResponseHeader(int contentLenght) throws Exception{
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy" + CRLF;
		String statusLine = null;
		String contentTypeLine = null;
		String contentLengthLine = "error";
		String acceptRangeLine = "Accept-Ranges: none" + CRLF;

		statusLine = "HTTP/1.0 200 OK" + CRLF ;
		contentTypeLine = "Content-Type: text/xml" + CRLF ;
		contentLengthLine = "Content-Length: " + contentLenght + CRLF;

		String response = statusLine + serverLine + contentTypeLine + contentLengthLine + acceptRangeLine+ CRLF;
		
//		// Send the status line.
//		_socket.getOutputStream().write(statusLine.getBytes());
//
//		// Send the server line.
//		_socket.getOutputStream().write(serverLine.getBytes());
//
//		// Send the content type line.
//		_socket.getOutputStream().write(contentTypeLine.getBytes());
//
//		// Send the Content-Length
//		_socket.getOutputStream().write(contentLengthLine.getBytes());
//
//		// Send a blank line to indicate the end of the header lines.
//		_socket.getOutputStream().write(CRLF.getBytes());
		
		OutputStream os = _socket.getOutputStream();
		os.write(response.getBytes());
		os.flush();
		//os.close();
	}
	
	private void SendResponse(String content) throws Exception
	{
		_socket.getOutputStream().write(content.getBytes());
	}
    
    private Socket _socket;

	private void send404() throws Exception{
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy" + CRLF;
		String statusLine = null;

		statusLine = "HTTP/1.0 404 Not Found" + CRLF ;

		// Send the status line.
		_socket.getOutputStream().write(statusLine.getBytes());

		// Send the server line.
		_socket.getOutputStream().write(serverLine.getBytes());

		// Send a blank line to indicate the end of the header lines.
		_socket.getOutputStream().write(CRLF.getBytes());
		
	}
	
    
	// "/"+"files"+"/"+"appmobicache"+"/"+"index.html"+"/" ;
	// "file:/data/data/com.appMobi.example.cachetest/files/appmobicache/index.html"
	public void sendFile(DPHttpRequest req) throws Exception {
		
		FileConnection backingfileReader = null;
		String rewrittenUrl = null;
		if(req._Path.indexOf("_appMobi")!=-1) {
			if(req.GetHeaderValue("Referer").startsWith("http://localhost:58888")) {
				String relativeReferer = req.GetHeaderValue("Referer").replace("http://localhost:58888", "");
				String appInfo = relativeReferer.substring(0, relativeReferer.lastIndexOf('/')+1);
				rewrittenUrl = _path+appInfo+"_appMobi/"+req._Path.substring(req._Path.lastIndexOf('/')+1);
			} else {
				rewrittenUrl = new File(AppMobiActivity.sharedActivity.baseDir(), "appmobi.js").getAbsolutePath();
			}
			if(Debug.isDebuggerConnected()) Log.d("[appMobi]","rewrittenUrl for" + req._Path + " is "+rewrittenUrl);
			backingfileReader = new FileConnection(rewrittenUrl);
		} else {
			backingfileReader = new FileConnection(_path+(req._Path.startsWith("/")?"":"/")+req._Path);
		}
		//byte[] b = new byte[(int) backingfileReader.fileSize()];
		// put these bytes in hashtable to sync them
		//InputStream syncStream = backingfileReader.openInputStream();
		//syncStream.read(b, 0, (int) backingfileReader.fileSize());

		String mediaType = null;

		String acceptHeader = req.GetHeaderValue("accept");
		if(Debug.isDebuggerConnected()) Log.d("mediaType","acceptHeader for" + req._Path + " is  "+acceptHeader);
		
		int indexOfDot = req._Path.lastIndexOf('.');
		if (indexOfDot > 0) {
			mediaType = (String) mediaTypeMap.get(req._Path.substring(indexOfDot));
			if(Debug.isDebuggerConnected()) Log.d("mediaType","mediaType for" + req._Path + " is  "+mediaType);
		}
		if (mediaType == null) {
			mediaType = "unknown/unknown";
		}
		/*
		 * 
		 * if (req._Path.contains(".js") || req._Path.contains(".css")) {
		 * mediaType = "text/plain"; } else if (req._Path.contains(".jpg") ||
		 * req._Path.contains(".jpeg")) { mediaType = "image/jpeg"; } else if
		 * (req._Path.contains(".gif")) { mediaType = "image/gif"; } else if
		 * (req._Path.contains(".png")) { mediaType = "image/png";// need to
		 * varify it } else mediaType = "text/html";
		 */
		if(backingfileReader.fileSize()<=0) {
			send404();
		} else {
			//reading file into memory was causing OOM - quick hack to stream instead
			OutputStream os = _socket.getOutputStream();
			SendResponseHeaderFile(backingfileReader.fileSize(), mediaType, os);

			//SendResponse(b, b.length);
			
			InputStream is = backingfileReader.openInputStream();
			byte[] buffer = new byte[1024]; // tweaking this number may increase performance  
			int len;  
			while ((len = is.read(buffer)) != -1)  
			{  
			    os.write(buffer, 0, len);  
			}  
			os.flush(); 
			//is.close();
			//os.close();  		
		}
	}

	private void SendResponseHeaderFile(long contentLenght, String mediatype, OutputStream os)
			throws Exception {
		// contentLenght = 9999999;
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy" + CRLF;
		String statusLine = null;
		String contentTypeLine = null;
		String contentLengthLine = "error";
		String acceptRangeLine = "Accept-Ranges: none" + CRLF;
		
		if (mediatype == null || mediatype.equals(""))
			mediatype = "text/plain";
		statusLine = "HTTP/1.0 200 OK" + CRLF;
		contentTypeLine = "Content-Type: " + mediatype + CRLF;
		contentLengthLine = "Content-Length: " + contentLenght + CRLF;

//		// Send the status line.
//		_socket.getOutputStream().write(statusLine.getBytes());
//
//		// Send the server line.
//		_socket.getOutputStream().write(serverLine.getBytes());
//
//		// Send the content type line.
//		_socket.getOutputStream().write(contentTypeLine.getBytes());
//
//		// Send the Content-Length
//		_socket.getOutputStream().write(contentLengthLine.getBytes());
//
//		// Send a blank line to indicate the end of the header lines.
//		_socket.getOutputStream().write(CRLF.getBytes());

		String response = statusLine + serverLine + contentTypeLine + contentLengthLine + acceptRangeLine + CRLF;
		//OutputStream os = _socket.getOutputStream();
		os.write(response.getBytes());
		os.flush();
		//os.close();
	}

	/* mapping of file extensions to content-types */
	static Hashtable<String, String> mediaTypeMap = new Hashtable<String, String>();

	static {
		fillMap();
	}

	static void setMediaTypeMap(String k, String v) {
		mediaTypeMap.put(k, v);
	}

	static void fillMap() {

		setMediaTypeMap("", "content/unknown");

		// � Type application: Multipurpose files
		setMediaTypeMap(".json", "application/json");
		setMediaTypeMap(".js", "text/javascript");
		setMediaTypeMap(".ogg", "application/ogg");
		setMediaTypeMap(".oga", "application/ogg");
		setMediaTypeMap(".ogv", "application/ogg");
		//If you don't know whether the Ogg file contains audio or video, you can serve it with the MIME type application/ogg
		// application/xhtml+xml;

		// Type audio: Audio
		setMediaTypeMap(".snd", "audio/basic");
		setMediaTypeMap(".au", "audio/basic");
		setMediaTypeMap(".wav", "audio/x-wav");
		/*
		 * audio/ogg; audio/x-ms-wma; audio/x-ms-wax; audio/vnd.wave;
		 */
		setMediaTypeMap(".mp3", "audio/mpeg");
		
		// � Type image
		setMediaTypeMap(".gif", "image/gif");
		setMediaTypeMap(".jpeg", "image/jpeg");
		setMediaTypeMap(".jpg", "image/jpeg");

		setMediaTypeMap(".png", "image/png");
		// image/svg+xml//: SVG vector image; Defined in SVG Tiny 1.2
		// Specification Appendix M

		setMediaTypeMap(".tiff", "image/tiff");

		// � Type text: Human-readable text and source code

		setMediaTypeMap(".css", "text/css");
		setMediaTypeMap(".csv", "text/csv");

		setMediaTypeMap(".htm", "text/html");
		setMediaTypeMap(".html", "text/html");
		setMediaTypeMap(".txt", "text/plain");
		setMediaTypeMap(".java", "text/plain");
		setMediaTypeMap(".xml", "text/xml");
		setMediaTypeMap(".text", "text/plain");
		setMediaTypeMap(".c", "text/plain");
		setMediaTypeMap(".cc", "text/plain");
		setMediaTypeMap(".c++", "text/plain");
		setMediaTypeMap(".h", "text/plain");
		setMediaTypeMap(".pl", "text/plain");

		// � Type video: Video
		/*
		 * video/mpeg//: MPEG-1 video with multiplexed audio; Defined in RFC
		 * 2045 and RFC 2046 video/mp4//: MP4 video; Defined in RFC 4337
		 * video/ogg//: Ogg Theora or other video (with audio); Defined in RFC
		 * 5334 video/quicktime//: QuickTime video; Registered[9]
		 * video/x-ms-wmv//: Windows Media Video; Documented in Microsoft KB
		 * 288102
		 */
		
		setMediaTypeMap(".mp4", "video/mp4");
		setMediaTypeMap(".m4v", "video/x-m4v");
		setMediaTypeMap(".mpeg", "video/mpeg");
		
		setMediaTypeMap(".uu", "application/octet-stream");
		setMediaTypeMap(".exe", "application/octet-stream");
		setMediaTypeMap(".ps", "application/postscript");
		setMediaTypeMap(".zip", "application/zip");
		setMediaTypeMap(".sh", "application/x-shar");
		setMediaTypeMap(".tar", "application/x-tar");
	}
}
