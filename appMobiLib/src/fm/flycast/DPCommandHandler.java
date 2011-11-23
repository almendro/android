package fm.flycast;

import java.net.Socket;
import java.net.SocketException;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;
  
/**
 * Class to handle all the Commands received by the webserver.
 * @author Sudeep
 *
 */

public class DPCommandHandler {
	
	public static final int COMMAND_ISALIVE = 0;
	public static final int COMMAND_STARTSTATION = 1;
	public static final int COMMAND_PLAYTRACK = 2;
	public static final int COMMAND_GETMESSAGES = 3;
	public static final int COMMAND_STOPSTATION = 4;
	
	/*********************START FOR RECORDING*****************************/
	public static final int COMMAND_GETRECORDINGS = 5;
	public static final int COMMAND_DELETETRACK = 6;
	public static final int COMMAND_DELETERECORDINGS = 7;
	public static final int COMMAND_DELETEALLRECORDINGS = 8;
	/*********************END FOR RECORDING*****************************/
	
	public static final int COMMAND_SETERRORLEVEL = 9;
	
	public static final int COMMAND_SETFAVORITES = 10;
	public static final int COMMAND_GETFAVORITES = 11;
	
	public static final int COMMAND_RESUME_TRACK = 12;
	public static final int COMMAND_LIVE_TRACK = 13;
	public static final int COMMAND_FLYBACK = 14;
	public static final int COMMAND_SENDFILE = 15;
	//public static final int COMMAND_IS_IN_LIVE_MODE = 15;
	
	public static final String IS_ALIVE =  "<XML><ALIVE><ACK/></ALIVE></XML>";
	public static final String IS_DONE =  "<XML><ACK><OK/></ACK></XML>";
	public static final String IS_NOT_DONE =  "<XML><ACK><NOT OK/></ACK></XML>";

	private static String FB30URL=null;
	private static String FB60URL=null;
	private static String FB120URL=null;	
	
	private Socket _Socket = null;
	private DPHttpRequest _Request = null;
	
	public DPCommandHandler(){
	}
	
	public DPCommandHandler(Socket soc, DPHttpRequest req){
		_Socket = soc;
		_Request = req;
	}
	
	/**
	 * Handle all the commands of the client. Example are start station , play track etc.
	 * 
	 * @param command
	 * @throws Exception
	 */
	public void HandleCommand( int command) throws Exception{
		switch( command ){
		case COMMAND_ISALIVE:{
				//At this point set the logging level to debug.
			String aLive = IsAlive();	
			SendResponseHeader(aLive.length());
			SendResponse(aLive);
			break;
			}
		case COMMAND_STARTSTATION:{
				HandleCommandStartStation();
				break;
			}
		
		case COMMAND_LIVE_TRACK:{
			HandleCommandLiveTrack();
			break;
		}
		
		case COMMAND_PLAYTRACK:{
				HandleCommandPlayTrack();
				break;
			}
		
		case COMMAND_FLYBACK:{
			HandleCommandFlyBack();
			break;
		}
		
		/*case COMMAND_IS_IN_LIVE_MODE:{
			String res = "No";
			if(DPApplication.Instance()._DownLoader.isLive())
				res = "YES";
			
			SendResponseHeader(res.length());
			SendResponse(res);
			break;
			
		}*/

		case COMMAND_RESUME_TRACK:{
			String res = "";
			String guid = _Request.GetValue("SongGUID");
			
			if( guid == null ){
				if( PlayTrackLastSource != null && PlayTrackLastSource.HasMoreContent() ){
					res = "YES";
				}
			}
			else{
				if( PlayTrackLastSource != null && PlayTrackLastSource.currenttrack.guidSong.equalsIgnoreCase(guid) && PlayTrackLastSource.HasMoreContent() ){
					res = "YES";
				}
			}
				
			
			if( res.equalsIgnoreCase("YES")){
				if(Debug.isDebuggerConnected()) Log.d("Socket", "Request to resume track.");
			}
			else{
				if(Debug.isDebuggerConnected()) Log.d("Socket", "Request for new track.");
			}
			
			SendResponseHeader(res.length());
			SendResponse(res);
			break;
		}
		
		case COMMAND_GETMESSAGES:{
			//android.util.if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Received the Get Messages command.");
			HandleCommandGetMessages();
			break;
		}
		case COMMAND_STOPSTATION:{
			HandleCommandStopStation();
			break;
		}	
		
		/*********************START FOR RECORDING*****************************/
		case COMMAND_GETRECORDINGS:{
			//TODO
			HandleCommandGetRecordings();
			break;
		}
		case COMMAND_DELETETRACK:{
			//TODO
			HandleCommandDeleteTrack();
			break;
		}
		case COMMAND_DELETERECORDINGS:{
			//TODO
			HandleCommandDeleteRecording();
			break;
		}
		case COMMAND_DELETEALLRECORDINGS:{
			//TODO
			HandleCommandAllDeleteRecording();
			break;
		}		
		/*********************END FOR RECORDING*****************************/
		
		case COMMAND_SETERRORLEVEL:{
			//TODO
			HandleCommandSetErrorLevel();
			break;
		}
		
		
		case COMMAND_SENDFILE:{
			HandleCommandSendFile();
			break;
		}
		
		}

	}

	
	/*
	 * 2/15/2010
	 * In order to remove HTTP communication
	 * this method will be used now instead of ALIVE command
	 * 
	 */
	public String IsAlive(){
		return IS_ALIVE;
	}
	
	/**
	 * This function does not send the headers.
	 * @param content
	 * @throws Exception
	 */
	private void SendResponse(String content) throws Exception
	{
		_Socket.getOutputStream().write(content.getBytes());
	}

	/**
	 * Send the header to the client.
	 * @param contentLenght
	 * @throws Exception
	 */
	private void SendResponseHeaderMedia(int contentLenght, String mediatype) throws Exception{
		//contentLenght = 9999999;
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy";
		String statusLine = null;
		String contentTypeLine = null;
		String contentLengthLine = "error";

		if( mediatype == null ) mediatype = "audio/mpeg";
		statusLine = "HTTP/1.0 200 OK" + CRLF ;
		contentTypeLine = "Content-Type: " + mediatype + CRLF ;
		contentLengthLine = "Content-Length: " + contentLenght + CRLF;

		// Send the status line.
		_Socket.getOutputStream().write(statusLine.getBytes());

		// Send the server line.
		_Socket.getOutputStream().write(serverLine.getBytes());

		// Send the content type line.
		_Socket.getOutputStream().write(contentTypeLine.getBytes());

		// Send the Content-Length
		_Socket.getOutputStream().write(contentLengthLine.getBytes());

		// Send a blank line to indicate the end of the header lines.
		_Socket.getOutputStream().write(CRLF.getBytes());
	}

	
	private void SendResponseHeaderFile(long contentLenght, String mediatype) throws Exception{
		//contentLenght = 9999999;
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy";
		String statusLine = null;
		String contentTypeLine = null;
		String contentLengthLine = "error";

		if( mediatype == null ) mediatype = "audio/mpeg";
		statusLine = "HTTP/1.0 200 OK" + CRLF ;
		contentTypeLine = "Content-Type: " + mediatype + CRLF ;
		contentLengthLine = "Content-Length: " + contentLenght + CRLF;

		// Send the status line.
		_Socket.getOutputStream().write(statusLine.getBytes());

		// Send the server line.
		_Socket.getOutputStream().write(serverLine.getBytes());

		// Send the content type line.
		_Socket.getOutputStream().write(contentTypeLine.getBytes());

		// Send the Content-Length
		_Socket.getOutputStream().write(contentLengthLine.getBytes());

		// Send a blank line to indicate the end of the header lines.
		_Socket.getOutputStream().write(CRLF.getBytes());
	}
	
	/**
	 * Send the header to the client.
	 * @param contentLenght
	 * @throws Exception
	 */
	private void SendResponseHeader(int contentLenght) throws Exception{
		//contentLenght = 9999999;
		String CRLF = "\r\n";

		// Construct the response message.
		String serverLine = "Server: DeviceProxy";
		String statusLine = null;
		String contentTypeLine = null;
		String contentLengthLine = "error";

		statusLine = "HTTP/1.0 200 OK" + CRLF ;
		contentTypeLine = "Content-Type: text/xml" + CRLF;
		contentLengthLine = "Content-Length: " + contentLenght + CRLF;

		// Send the status line.
		_Socket.getOutputStream().write(statusLine.getBytes());

		// Send the server line.
		_Socket.getOutputStream().write(serverLine.getBytes());

		// Send the content type line.
		_Socket.getOutputStream().write(contentTypeLine.getBytes());

		// Send the Content-Length
		_Socket.getOutputStream().write(contentLengthLine.getBytes());

		// Send a blank line to indicate the end of the header lines.
		_Socket.getOutputStream().write(CRLF.getBytes());
	}
	
	/**
	 * Send the track content to the client.
	 * THIS ALSO CHANGE THE CURRENT TRACK OF THE DOWNLOAD MANAGER TO START BUFFERING FROM THIS 
	 * TRACK ONWARDS.
	 * @throws Exception
	 */
	
	
	
	//------ [11:31:20 AM] narinder dev: i have changed the following method only: ------
	public static DPFlyCastTrackSource PlayTrackLastSource = null;
	//private static int PlayTrackTotalBytesRead;
	//private static String PlayTrackLastGUID;

	//public static Object _lock = new Object();
	
	public static boolean Entered = false;
	
	private void HandleCommandPlayTrack() throws Exception {
		{
			DPXMLTrack track = null;
			//int offset = 0;
			int length;
			int bytesSent = 0;
			if(Debug.isDebuggerConnected()) Log.d("HandleCommandPlayTrack", "Going to send bytes to Player..");
			if(Debug.isDebuggerConnected()) Log.d("DPCommandHandler", "in DpCommandHandler palyer..");
			try {
	
				String NewGuid = _Request.GetValue(DPHttpRequest.GUID);
				if(Debug.isDebuggerConnected()) Log.d("DPCommandHandler", "Going to find the track in tracklist..");
				track = DPApplication.Instance().GetTrack(NewGuid);

				
			/*
			 * Narinder 04/03/2010
			 * Always set the track to DPFlyCastTrackSource otherwise it takes 
			 * a while to switch the track..
			 * One shortcoming of this is that we will send the bytes again for the same track 
			 * but good thing is that it will make the system stable UI will not freeze and
			 * tracks will switch almost immediately earlier it was taking 20-30seconds as reported by the QA
			 * 	
			 */
				DPXMLTracklist tracklist = DPApplication.Instance()._Tracklist;
				PlayTrackLastSource = new DPFlyCastTrackSource(track, tracklist.shoutcasting);
				DPApplication.Instance().SetTrackAsCurrent(NewGuid);
				
				//---------------------------------------
				
				length = DPFlyCastTrackSource.READ_CHUNK; // int len = 16384;
	
				/*if (ResumeMode) {
					//enter = false;
					//PlayTrackLastSource.currenttrack.totalBytesSent =  track.totalBytesSent;
					if(Debug.isDebuggerConnected()) Log.d("TotalBytesSent", "Till now TotalBytesSent for "
							+ track.guidSong + " are " + track.totalBytesSent);

				}*/
				PlayTrackLastSource.Block();
				boolean headerSent = false;
				int contentLength = PlayTrackLastSource.ContentLength();
				int bufferSize = length;
				byte[] b = new byte[bufferSize];
				while(true) {
					{
						int temp1 = contentLength - bytesSent;
						if( temp1 <= 0 )
							break;
						
						if( temp1 < length)
							bufferSize = temp1;
					}
					
					int bRead = PlayTrackLastSource.read(b, 0, bufferSize);
					if (bRead == -1)
					{
						if( !headerSent ){
							headerSent = true;
							contentLength = bRead;
							//SendResponseHeaderMedia(contentLength, PlayTrackLastSource.MediaType());
							
							if(Debug.isDebuggerConnected()) Log.d("FLIBuffer","content length is for .."+ track.guidSong+"..is"+PlayTrackLastSource.ContentLength());
							
							/*
							 *Sometimes totalbytes sent are less than content length
							 * I do not have any idea why it is happening.
							 * If this happens and we have sent all the bytes
							 * mark the content length as the total bytes sent 
							 * 
							 */
							//16july 2010 remove the if condition only else code should be here
							
								if(track.totalBytesSent > 0 && track.totalBytesSent < PlayTrackLastSource.ContentLength()){
									SendResponseHeaderMedia(track.totalBytesSent, PlayTrackLastSource.MediaType());	
									if(Debug.isDebuggerConnected()) Log.d("FLIBuffer","content length is for .."+ track.guidSong+"..is"+track.totalBytesSent);
								}
								else{
									SendResponseHeaderMedia(PlayTrackLastSource.ContentLength(), PlayTrackLastSource.MediaType());
								}
						}
						break;
					}
					
					if( bRead == 0 )
					{
						try
						{
							Thread.sleep(500);
						}
						catch(Exception exception) {
							if(Debug.isDebuggerConnected()) Log.d("DPCommandHandler", "Sleep exc .... "  + exception.toString());
						}
						continue;
					}
					
					bytesSent += bRead;
					
					if( !headerSent ){
						headerSent = true;
						if( contentLength == 0 && bRead > 0 ){
							contentLength = bRead;
						}
						SendResponseHeaderMedia(contentLength, PlayTrackLastSource.MediaType());
						if(Debug.isDebuggerConnected()) Log.d("FLIBuffer","content length is.."+contentLength);
					}
					
					//if(Debug.isDebuggerConnected()) Log.d("FLIBuffer","returned amount is.."+bRead);
					SendResponse(b, bRead);

				}
			} 
			catch(SocketException ex){
			}
			
			catch (DPException dpException) {
				DPException dpXxceptionHandler = new DPException(dpException);
				String retVal = dpXxceptionHandler.generateXML();
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			}catch (Exception exception) {	
			}

			/*
			 * Set totalbytesSent by track as 0 whenever we get a command to play the track
			 * because we are sending all  the bytes again to Media Player even in case of Resume Track
			 */
			
			track.totalBytesSent = 0;
		
			track.totalBytesSent += bytesSent;
			if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "Total bytes sent for track are "  + PlayTrackLastSource.currenttrack.guidSong  + " are :: " + track.totalBytesSent);
			DPApplication.Instance().RemoveTrackState(track);
		}
	}
	public String FlyBack(String flyBack) throws Exception {
		DPManager.flyback=true;
		DPApplication.Instance().GetDownloadManager()._tracklist.recording = true;
		DPManager.flybackURLIndex = 0;
		//String urLFirst=DPThreadLocalUrl.getRequestUrl();
		if(flyBack.equals("FB30")){
			DPManager.flybackURLIndex = 2;
		}
		else if(flyBack.equals("FB60")){
			DPManager.flybackURLIndex = 1;
		}

		
		//DPManager.flybackURLIndex=flyBackUrl;
		String str = "YES";
		return str;
	}
	

	private void HandleCommandFlyBack() throws Exception {
			try {
				String flyBack = _Request.GetValue("FLYBACK");
				String str = FlyBack(flyBack);
				SendResponseHeader(str.length());
				SendResponse(str);
	
			} catch (DPException dpException) {
			 	
				if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "DPEXCEPTION .... "  + dpException.toString());
				if(Debug.isDebuggerConnected()) Log.d("NoSound",  "case COMMAND_PLAYTRACK" +
						"Exception is thrown when track in not equals(guid)",
						dpException.fillInStackTrace());
				if(Debug.isDebuggerConnected()) Log.e("NoSound","case COMMAND_PLAYTRACK"+
						"Exception is thrown when track in not equals(guid)",
						dpException.fillInStackTrace());
	
				DPException dpXxceptionHandler = new DPException(dpException);
				String retVal = dpXxceptionHandler.generateXML();
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			} catch (Exception exception) {
				if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "DPEXCEPTION .... "  + exception.toString());
				if(Debug.isDebuggerConnected()) Log.d("NoSound", "HandleCommandFlyBack exception="
						+ exception.getMessage());
				if(Debug.isDebuggerConnected()) Log.e("NoSound , case COMMAND_PLAYTRACK",
						"HandleCommandPlayTrack() throws the exception ", exception
								.fillInStackTrace());
			}
			
			//DPApplication.Instance().RemoveTrackState(track);
		}
	
	
	private static StationStatus _StationStatus = null;
	
	
	public String StartStation(String ID, String UID, String PROXYURL,
			String FB30URL, String FB60URL, String FB120URL) throws Exception {
		String str = null;
		try {
			
			if (FB30URL != null && !FB30URL.trim().equals("")) {
				DPApplication.Instance().FlyBack30Url = FB30URL;
			}
			else
			{
				FB30URL = null;
			}

			if (FB60URL != null && !FB60URL.trim().equals("")) {
				DPApplication.Instance().FlyBack60Url = FB60URL;
			}
			else
			{
				FB60URL = null;
			}
			if (FB120URL != null && !FB120URL.trim().equals("")) {
				DPApplication.Instance().FlyBack120Url = FB120URL;
			}
			else
			{
				FB120URL = null;
			}
			

			boolean reset = false;
			if (_StationStatus == null || !_StationStatus._StationId.trim().equalsIgnoreCase( ID.trim())) {
				_StationStatus = new StationStatus();
				_StationStatus._StationId = ID;
				reset = true;
			}

			DPXMLTracklist trackList = null;
			if (reset) {
				DPApplication.Instance().setUid(UID);
				trackList = DPApplication.Instance().CreateXMLTrackList(ID, PROXYURL);
			} else {
				trackList = DPApplication.Instance().GetTrackList();
				// DPApplication.Instance().StartTrackListCached();
			}
			
			if( ID.equals("0") )
			{
				trackList.shoutcasting = true;
				trackList.flycasting = false;				
			}
			
			if( reset )
			{
				DPApplication.Instance().RemoveMessages();
				DPApplication.Instance().StartTrackList();
				_StationStatus._List = trackList;
			}
			
			while (trackList.children.size() < 2) {
				// Thread.sleep(1000);
				if (trackList.children.size() >= 1) {
					DPXMLTrack track = (DPXMLTrack) trackList.children.elementAt(0);
					if (track != null) {
						DPTrackState dts = DPApplication.Instance().getTrackState(track);
						if (dts.GetTrackState() != 3)
							Thread.sleep(1000);
						else
							break;
					} else {
						Thread.sleep(1000);
					}
				} else {
					Thread.sleep(1000);
				}
			}

			trackList.Intialized();
			str = DPApplication.Instance().GetTrackMessages();

			_StationStatus = null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}	
	
	
	public String StartStation(String stationID, DPXMLTracklist list) throws Exception {
		String str = null;
		DPXMLTracklist tracklist =list;
		try {
			
			if (_StationStatus == null || !_StationStatus._StationId.trim().equalsIgnoreCase( stationID.trim())) {
				_StationStatus = new StationStatus();
				_StationStatus._StationId = stationID;
			}
				DPApplication.Instance().setCurrentStation(stationID);
				//tracklist.startindex = tracklist.children.size()-1;
				DPApplication.Instance().resumeMode = true;
				DPApplication.Instance().setTrackList(tracklist);
				DPApplication.Instance().StartTrackList();
				
		
				_StationStatus._List = tracklist;
			//tracklist.Intialized();
				
				Thread.sleep(10000);
				
			str = DPApplication.Instance().GetTrackMessages();

			_StationStatus = null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}	
	
	
	
	/**
	 * Start the downloader to download from the specified station.
	 * Sleep till we get the intial tracks from the station
	 * Send the track list to the client.
	 * @throws Exception
	 */
	private void  HandleCommandStartStation() throws Exception {
		
		try{
				String url = _Request.GetValue(DPHttpRequest.PROXYURL);
				//flyBack start
				
				try{
					FB30URL=_Request.GetValue(DPHttpRequest.FB30URL);
					FB60URL=_Request.GetValue(DPHttpRequest.FB60URL);
					FB120URL=_Request.GetValue(DPHttpRequest.FB120URL);
				}catch(NullPointerException e){
					
				}
				
				if(FB30URL!=null){
					DPApplication.Instance().FlyBack30Url=FB30URL;
				}
					
				if(FB60URL!=null){
					DPApplication.Instance().FlyBack60Url=FB60URL;
				}
				if(FB120URL!=null){
					DPApplication.Instance().FlyBack120Url=FB120URL;
				}
	
				//DPThreadLocalUrl.setRequestUrl(url);

				String sid = _Request.GetValue(DPHttpRequest.ID);
				String uid = _Request.GetValue(DPHttpRequest.UID);

				boolean reset = false;
				if( _StationStatus == null || !_StationStatus._StationId.trim().equalsIgnoreCase(sid.trim())){
					_StationStatus = new StationStatus();
					_StationStatus._StationId = sid;
					reset = true;
				}
					
				DPXMLTracklist trackList = null;
				if( reset ){
					DPApplication.Instance().setUid(uid);		
					trackList = DPApplication.Instance().CreateXMLTrackList(sid, url);
					DPApplication.Instance().RemoveMessages();
					DPApplication.Instance().StartTrackList();
					_StationStatus._List = trackList;
				}
				else
				{
					trackList = DPApplication.Instance().GetTrackList();
					//DPApplication.Instance().StartTrackListCached();
				}

				while( trackList.children.size() < 2)
				{
					//Thread.sleep(1000);
					if( trackList.children.size() >= 1)
					{
						DPXMLTrack track = 	(DPXMLTrack)trackList.children.elementAt(0);
						if( track != null)
						{
							DPTrackState dts = DPApplication.Instance().getTrackState(track);
							if( dts.GetTrackState() != 3 )
								Thread.sleep(1000);
							else
								break;
						}
						else
						{
							Thread.sleep(1000);
						}
					}
					else
					{
						Thread.sleep(1000);
					}
				}
				
				trackList.Intialized();
				String str = DPApplication.Instance().GetTrackMessages();
				SendResponseHeader(str.length());
				SendResponse(str);
				
				_StationStatus = null;
			}
			catch(DPException dpException){
							
				if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STARTSTATION", "Error is thrown while doing Integer.parseInt", dpException.fillInStackTrace());
				if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STARTSTATION", "Error is thrown while doing Integer.parseInt", dpException.fillInStackTrace());
				
				DPException  dpExcept=new DPException(dpException);						
				String retVal=dpExcept.generateXML();
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			}
			catch(Exception exeption){
				
				if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STARTSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
				if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STARTSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
				
				DPException  dpExcept=new DPException(exeption);						
				String retVal=dpExcept.generateXML();
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			}
	}

	
	public String StopStation(String ID, String UID, boolean RECORD) throws Exception{
		String msg=null;
		try{
        boolean record = RECORD;
	
		if(!record)//Delete all record
		{
			msg =IS_DONE;
			//return IS_DONE;
		}
		else if(record)//We have already saved, so just save tracklist
		{
			//msg = DPApplication.Instance().saveTracklistNew(uid);
			//msg = DPApplication.Instance().saveTracklist(uid);
			//return msg;
			msg = DPApplication.Instance().GetTrackMessages();
		}
		DPApplication.Instance().GetDownloadManager().reset();
		DPApplication.Instance().setDownloadManager();
		}
		catch (NumberFormatException dpException) {
		
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STOPSTATION", "NumberFormatException", dpException.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STOPSTATION", "NumberFormatException", dpException.fillInStackTrace());
		
			
			DPException  dpExcept=new DPException(dpException);						
			String retVal=dpExcept.generateXML();
			return retVal;
		}
		catch (Exception exeption) {
			
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STOPSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STOPSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
			
			DPException  dpExcept=new DPException(exeption);						
			String retVal=dpExcept.generateXML();
			return retVal;
		}
		return msg;
		
	}
		
	
	
	/**
	 * Stop the Station
	 * Just reset the DPManager
	 * @throws Exception 
	 */
	private void HandleCommandStopStation() throws Exception{
		try{
			
			if(Debug.isDebuggerConnected()) Log.d("No more track found","Received command to stop the station.." );
				
			int record=-1;
		String uid = _Request.GetValue(DPHttpRequest.UID);
		String stationID = _Request.GetValue(DPHttpRequest.ID);
		record = Integer.parseInt(_Request.GetValue(DPHttpRequest.RECORD));
		
		if(record == 0)//Delete all record
		{
			DPApplication.Instance().deleteRecording(uid, stationID);				
			SendResponseHeader(IS_DONE.length());
			SendResponse(IS_DONE);
		}else if(record == 1)//We have already saved, so just save tracklist
		{
			String msg = DPApplication.Instance().saveTracklist(uid);
			SendResponseHeader(msg.length());
			SendResponse(msg);
		}
		DPApplication.Instance().GetDownloadManager().reset();
		//SendResponseHeader("".length());
		//SendResponse("");
		}
		catch (NumberFormatException dpException) {
		
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STOPSTATION", "NumberFormatException", dpException.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STOPSTATION", "NumberFormatException", dpException.fillInStackTrace());
		
			
			DPException  dpExcept=new DPException(dpException);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
		}
		catch (Exception exeption) {
			
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_STOPSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_STOPSTATION", "method name is HandleCommandStartStation", exeption.fillInStackTrace());
			
			DPException  dpExcept=new DPException(exeption);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
		}
	}
	
	public String GetMessages() throws Exception {
	
		try{
			String str = DPApplication.Instance().GetAllMessages();
			return str;
			}
		catch(Exception exeption){
			if(Debug.isDebuggerConnected()) Log.d("GetMessage", "exception is "+exeption.getMessage());
			DPException  dpExcept=new DPException(exeption);						
			String retVal=dpExcept.generateXML();
		return retVal; 
		}
		
	}
	
	
	/**
	 * Send All Messages
	 * @throws Exception 
	 */
	private void HandleCommandGetMessages() throws Exception {
		try{
		String str = DPApplication.Instance().GetAllMessages();
		SendResponseHeader(str.length());
		SendResponse(str);
		}catch(Exception exeption){
		if(Debug.isDebuggerConnected()) Log.d("GetMessage", "exception is "+exeption.getMessage());
		
		DPException  dpExcept=new DPException(exeption);						
		String retVal=dpExcept.generateXML();
		SendResponseHeader(retVal.length());
		SendResponse(retVal);
		}
	}
	/**
	 * This function does not send the headers.
	 * @param content
	 * @throws Exception
	 */
	private void SendResponse(byte[] content, int size) throws Exception
	{
		_Socket.getOutputStream().write(content, 0, size);
	}
	
	/*********************START FOR RECORDING*****************************/
	
	/*
	 * It may throw Station Id not integer exception
	 * 
	 */
	
	public void HandleCommandGetRecordings() throws Exception
	{
		try{
		String uid = _Request.GetValue(DPHttpRequest.UID);	
		String allTrackList = DPApplication.Instance().getRecording(uid);		
		SendResponseHeader(allTrackList.length());
		SendResponse(allTrackList);
		}catch (DPException dpException) {
			DPException  dpExcept=new DPException(dpException);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
			
			/*DPExceptionHandler dpXxceptionHandler=new DPExceptionHandler();						
			String retVal=dpXxceptionHandler.generateXML(dpException);
			SendResponseHeader(retVal.length());
			SendResponse(retVal);*/
		}
		catch (Exception exeption) {
			
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG, case COMMAND_GETRECORDINGS", "method name is HandleCommandGetRecordings", exeption.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG, case COMMAND_GETRECORDINGS", "method name is HandleCommandGetRecordings", exeption.fillInStackTrace());
			
			DPException  dpExcept=new DPException(exeption);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
		}
	}
	
	private void HandleCommandDeleteTrack() throws Exception
	{
		try{
		String uid = _Request.GetValue(DPHttpRequest.UID);
		String stationID = _Request.GetValue(DPHttpRequest.ID);
		String songGUID = _Request.GetValue(DPHttpRequest.GUID);
		DPApplication.Instance().deleteTrack(uid, stationID, songGUID);
		
		String str = DPApplication.Instance().GetTrackMessages();
		SendResponseHeader(str.length());
		SendResponse(str);
		}catch(DPException dpException){
			
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_DELETETRACK", "Error is thrown while doing songGUID.equals(track.guidSong)", dpException.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_DELETETRACK", "Error is thrown while doing songGUID.equals(track.guidSong)", dpException.fillInStackTrace());
			
			DPException  dpExcept=new DPException(dpException);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
		}
		
		catch (Exception exeption) {
			if(Debug.isDebuggerConnected()) Log.d("LOG_TAG , case COMMAND_DELETETRACK", "method name is HandleCommandDeleteTrack", exeption.fillInStackTrace());
			if(Debug.isDebuggerConnected()) Log.e("LOG_TAG , case COMMAND_DELETETRACK", "method name is HandleCommandDeleteTrack", exeption.fillInStackTrace());
		
			DPException  dpExcept=new DPException(exeption);						
			String retVal=dpExcept.generateXML();
			SendResponseHeader(retVal.length());
			SendResponse(retVal);
		}
		
	}	
	
	public void HandleCommandDeleteRecording() throws Exception
	{
		String uid = _Request.GetValue(DPHttpRequest.UID);
		String stationID = _Request.GetValue(DPHttpRequest.ID);
		boolean isDeleted = DPApplication.Instance().deleteRecording(uid, stationID);
		if(isDeleted)
		{
			//Positive acknowledgment	
			SendResponseHeader(IS_DONE.length());
			SendResponse(IS_DONE);
		}else
		{
			//Negative acknowledgment	
			SendResponseHeader(IS_NOT_DONE.length());
			SendResponse(IS_NOT_DONE);
		}		
	}
	
	public void HandleCommandAllDeleteRecording() throws Exception
	{
		String uid = _Request.GetValue(DPHttpRequest.UID);		
		boolean isDeleted = DPApplication.Instance().deleteAllRecording(uid);
		if(isDeleted)
		{
			//Positive acknowledgment	
			SendResponseHeader(IS_DONE.length());
			SendResponse(IS_DONE);
		}else
		{
			//Negative acknowledgment	
			SendResponseHeader(IS_NOT_DONE.length());
			SendResponse(IS_NOT_DONE);
		}		
	}
	
	
	/*********************END FOR RECORDING*****************************/
	
	private void HandleCommandLiveTrack() throws Exception {
		
			DPXMLTrack track = null;
			String retVal = "";
			try {			
	
				String NewGuid = _Request.GetValue(DPHttpRequest.GUID);
				track = DPApplication.Instance().GetTrack(NewGuid);
				
				retVal = "<XML>";
				if( track != null )
				{
					String trackparms = DPApplication.Instance().GenerateTrackParameters(track);
					retVal += "<ResourceContainer name=\"LIVE_TRACK\">";
					retVal += "<Resource name = \"Track\">";
					retVal += trackparms;
					retVal += "</Resource>";
					retVal += "</ResourceContainer>";
				}
				retVal += "</XML>";

				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			
			} catch (DPException dpException) {
				
				if(Debug.isDebuggerConnected()) Log.d("LiveTrack", "DPEXCEPTION .... "  + dpException.toString());
				if(Debug.isDebuggerConnected()) Log.d("NoSound",  "case COMMAND_LIVE_TRACK" +
						"Exception is thrown when track in not equals(guid)",
						dpException.fillInStackTrace());
				if(Debug.isDebuggerConnected()) Log.e("NoSound","case COMMAND_PLAYTRACK"+
						"Exception is thrown when track in not equals(guid)",
						dpException.fillInStackTrace());
				retVal = "NO";
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			} catch (Exception exception) {
				if(Debug.isDebuggerConnected()) Log.d("FLIBuffer", "DPEXCEPTION .... "  + exception.toString());
				if(Debug.isDebuggerConnected()) Log.d("NoSound", "HandleCommandPlayTrack exception="
						+ exception.getMessage());
				if(Debug.isDebuggerConnected()) Log.e("NoSound , case COMMAND_LIVE_TRACK",
						"HandleCommandPlayTrack() throws the exception ", exception
								.fillInStackTrace());
				
				retVal = "NO";
				SendResponseHeader(retVal.length());
				SendResponse(retVal);
			}
	}
	
	public void HandleCommandSetErrorLevel(){
		
		//String level = _Request.GetValue(DPHttpRequest.LEVEL);
		//if(Debug.isDebuggerConnected()) Log.iisLoggable  (String tag, int level)
		//if(Debug.isDebuggerConnected()) Log.d("tag", "msg");
	}
	
	//File targ, PrintStream ps
	public void HandleCommandSendFile() throws Exception {
		FileConnection backingfileReader = new FileConnection("index.html");
		
	//	SendResponseHeaderMedia(contentLength, PlayTrackLastSource.MediaType());
		SendResponseHeaderFile(backingfileReader.fileSize(), "");
		byte[] b = new byte[100];
		SendResponse(b, 123);
	}
	
}

class StationStatus{
	public String _StationId;
	public DPXMLTracklist _List = null;
}
