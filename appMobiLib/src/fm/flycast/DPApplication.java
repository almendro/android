package fm.flycast;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

/**
 * Singleton Implementation to handle all the global level data of the webserver.
 * @author Sudeep
 *
 */
public class DPApplication {

	public boolean resumeMode = false;
	public DPXMLTracklist _Tracklist = null; 
	private DPManager _DownLoader = null;
	private Queue<DPMessage> _MessageQueue = new LinkedList<DPMessage>();
	private Hashtable<DPXMLTrack, DPTrackState> _TrackStateManager = new Hashtable<DPXMLTrack, DPTrackState>();
	private String uid = null;
	
	public String FlyBack30Url=null;
	public String FlyBack60Url=null;
	public String FlyBack120Url=null;
	public String app_name = null;
	
	private int currentlyPlayingIndex = 0;
	private int trackMaxPlayed = 0;

	private static DPApplication _Application = new DPApplication();
	
	private DPApplication(){
	}
	
	public static DPApplication Instance(){
		return _Application;
	}
	
	 
	public void RemoveTrackState(DPXMLTrack t){
		_TrackStateManager.remove(t.guidSong);
	}
	
	/**
	 * This function removes the track if found in the tracklist before returning the track.
	 * 
	 * @param tracklist
	 * @param track
	 * @return
	 */
	private DPXMLTrack findTrack(DPXMLTracklist tracklist, DPXMLTrack track)
	    {
	        if( tracklist == null || track == null || track.guidSong == null ) return null;

	        for( int i = 0; i < tracklist.children.size(); i++ )
	        {
	            DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
	            if( temp.guidSong.equals( track.guidSong ) )
	            {
	            	tracklist.children.removeElementAt(i);
	                return temp;
	            }
	        }

	        return null;
	    }
	 
	
	public DPXMLTrack findTrack(DPXMLTrack track)
    {
        if( _Tracklist == null || track == null || track.guidSong == null ) return null;

        for( int i = 0; i < _Tracklist.children.size(); i++ )
        {
            DPXMLTrack temp = (DPXMLTrack) _Tracklist.children.elementAt(i);
            //if( temp.flyback ) continue;
            if( temp.guidSong == null ) continue;
            if( temp.guidSong.equals( track.guidSong ) )
            {
                return temp;
            }
        }

        return null;
    }
	 
	private String _StationId = "";
	
	public String getCurrentStation(){
		return _StationId;
	}
	public void setCurrentStation(String station){
		_StationId = station;
	}
	
	/**
	 * This method creates _tracklist with n-1 flyback URLs + 1 normal scenario Url
	 * where normal scenario URL is nth.
	 * flyback 120 track is added to _tracklist as first element i.e. at 0 location
	 * flyback 60 track is added to _tracklist as second element i.e. at 1 location
	 * flyback 30 track is added to _tracklist as third element i.e. at 2 location
	 * 
	 * @param stationId
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public DPXMLTracklist CreateXMLTrackList (String stationId, String url) throws Exception{
		int index=0;
		DPHttpRequest req = new DPHttpRequest(url);
		int sid=-1;
		int bitrate=128;
		int bitrate30=-1;
		int bitrate60=-1;
		int bitrate120=-1;
		try{
		 sid = Integer.parseInt(stationId);
		}catch (NumberFormatException e) {
			throw new DPException("Station Id is not number", 100);  
		}
		try{
		bitrate = Integer.parseInt(req.GetValue(DPHttpRequest.BITRATE));
		//if(Debug.isDebuggerConnected()) Log.d(FlyCastServiceRemote.TAG_DEVICEPROXY, "CreateXMLTrackList bitrate=" + bitrate);
		}catch(Exception e){
			bitrate = 128;
		}
		
		_StationId = stationId;
		
		_Tracklist = new DPXMLTracklist();
		_Tracklist.startindex = 0;
		_Tracklist.flycasting = true;
		_Tracklist.continuing = true;
		_Tracklist.timecode = System.currentTimeMillis();
		_Tracklist.stationid = sid;
		
		try{
			_Tracklist.bitrate = bitrate;
			
			if(FlyBack120Url!=null){
				DPHttpRequest req120 = new DPHttpRequest(FlyBack120Url);
				bitrate120 = Integer.parseInt(req120.GetValue(DPHttpRequest.BITRATE));
				DPXMLTrack flyBack120Track = new DPXMLTrack();
				flyBack120Track.stationid = sid;
				flyBack120Track.mediaurl = FlyBack120Url;
				flyBack120Track.bitrate = bitrate120;
				flyBack120Track.flyback=true;
				flyBack120Track.timecode = System.currentTimeMillis();
				DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, flyBack120Track));
				flyBack120Track.IndexInList = index;
				_Tracklist.children.insertElementAt(flyBack120Track, index++);
				FlyBack120Url = null;
			}
		
			if(FlyBack60Url!=null){
				DPHttpRequest req60 = new DPHttpRequest(FlyBack60Url);
				bitrate60 = Integer.parseInt(req60.GetValue(DPHttpRequest.BITRATE));
				DPXMLTrack flyBack60Track = new DPXMLTrack();
				flyBack60Track.stationid = sid;
				flyBack60Track.mediaurl = FlyBack60Url;
				flyBack60Track.bitrate = bitrate60;
				flyBack60Track.flyback=true;
				flyBack60Track.timecode = System.currentTimeMillis();
				DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, flyBack60Track));
				flyBack60Track.IndexInList = index;
				_Tracklist.children.insertElementAt(flyBack60Track, index++);
				FlyBack60Url = null;
			}
			
		
			if(FlyBack30Url!=null){
				DPHttpRequest req30 = new DPHttpRequest(FlyBack30Url);
				bitrate30 = Integer.parseInt(req30.GetValue(DPHttpRequest.BITRATE));
				DPXMLTrack flyBack30Track = new DPXMLTrack();
				flyBack30Track.stationid = sid;
				flyBack30Track.mediaurl = FlyBack30Url;
				flyBack30Track.bitrate = bitrate30;
				flyBack30Track.flyback=true;
				flyBack30Track.timecode = System.currentTimeMillis();
				
				DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, flyBack30Track));
				flyBack30Track.IndexInList = index;
				_Tracklist.children.insertElementAt(flyBack30Track, index++);
				FlyBack30Url = null;
			}
			
		//if(Debug.isDebuggerConnected()) Log.d(FlyCastServiceRemote.TAG_DEVICEPROXY, "CreateXMLTrackList bitrate=" + bitrate);
		}catch(Exception e){
		}	
		 
		DPXMLTrack track = new DPXMLTrack();
		track.stationid = sid;
		track.mediaurl = url;
		track.bitrate = bitrate;
		track.timecode = System.currentTimeMillis();
		track.IndexInList = index;
		
		if( sid == 0 )
		{
			_Tracklist.flycasting = false;
			_Tracklist.shoutcasting = true;
			track.mediatype = DPStringConstants.STR_AUDIO_MPEG;
			track.guidIndex = String.valueOf( System.currentTimeMillis() );
            track.guidSong = String.valueOf( System.currentTimeMillis() );
            track.length = (30 * 60 * 128 * bitrate);
            track.listened = true;
            track.album = null;
		}
		
		DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, track));
		_Tracklist.children.insertElementAt(track, index);
		_Tracklist.startindex = index;
		 
		return _Tracklist;
	}
	
	public DPXMLTracklist GetTrackList(){
		return _Tracklist;
	}
	
	public void setTrackList(DPXMLTracklist list){
		_Tracklist = list;
	}
	public DPManager GetDownloadManager(){
		if( _DownLoader == null)
			_DownLoader = new DPManager(app_name, null);
		_DownLoader.setUid(uid);
		return _DownLoader;
	}
	public void setDownloadManager(){
		 _DownLoader = null;
	}
	
	public void StartTrackList()
	{
		/*
		 * In normal(non flyback) scenario current index will be equal to size of _tracklist
		 * 
		 */
		
		if( _DownLoader == null || !_DownLoader.IsResuable())
		{
			_DownLoader = new DPManager(app_name, null);
			_DownLoader.setUid(uid);
		}
		else
		{
			_DownLoader.reset();
		}
		
		_DownLoader.playTracklist(_Tracklist, _Tracklist.startindex);
	}
	
	
	public void StopTrackList(){
		_DownLoader.reset();
	}
	
	public static Object _Lock = new Object();
	
	Hashtable<String, DPMessage> localCache = new Hashtable<String, DPMessage>();
	
	public void AddMessage(DPMessage m){
		synchronized(_Lock)
		{
			DPTrackState state = null;
			if( m.type ==  DPMessage.TRACK_WAS_ADDED){
				
				/**11/18/09
				 * Will write the "TRACK_IS_ADDED with the GUID: ......XYZ......." 
				 * to the file messageQueue
				 * 
				 */
				state = getTrackState(m.track);
				try
				{
					state.setAddTrackState();
				}catch(Exception ex){
					System.err.println("State Error" +  ex.toString());
				}
			} else if( m.type == DPMessage.TRACK_IS_BUFFERED){
						
				state = getTrackState(m.track);
				try
				{
					state.setBufferedTrackState();
				}catch(Exception ex){
					System.err.println("State Error" +  ex.toString());
				}
			}
			_MessageQueue.add(m);
			
		}
	}

	public DPTrackState getTrackState(DPXMLTrack t ){
		DPTrackState state = _TrackStateManager.get(t);
		if( state == null ){
			state = new DPTrackState();
			_TrackStateManager.put(t, state);
		}
		
		return state;
	}
	
	public void AddMessageBuffered(DPMessage m){
		if( m.type == DPMessageObject.TRACK_IS_BUFFERED){
				synchronized(_Lock)
				{
					DPTrackState state = null;
					state = getTrackState(m.track);
					try
					{
						state.setBufferedTrackState();
					}catch(Exception ex){
						System.err.println("State Error" +  ex.toString());
					}
					
					_MessageQueue.add(m);
					
					/**
					 * 11/18/09
					 * Will write the "TRACK_IS_BUFFERED with the GUID: .........XYZ......." 
					 * to the file messageQueue
					 * 
					 */
				}

		}
	}
	
	public int GetMessageCount(){
		return _MessageQueue.size();
	}
	
	public String GenerateTracklistParameters(DPXMLTracklist _Tracklist)
	{
		String retVal = "";
		retVal += "<key name=\"albumfile\" value=\"" + EscapeXML(_Tracklist.albumfile) + "\" />";
		retVal += "<key name=\"bitrate\" value=\"" + EscapeXML(_Tracklist.bitrate) + "\" />";
		retVal += "<key name=\"expdays\" value=\"" + EscapeXML(_Tracklist.expdays) + "\" />";
		retVal += "<key name=\"expplays\" value=\"" + EscapeXML(_Tracklist.expplays) + "\" />";				
		retVal += "<key name=\"imageurl\" value=\"" + EscapeXML(_Tracklist.imageurl) + "\" />";
		retVal += "<key name=\"livemediaurl\" value=\"" + EscapeXML(_Tracklist.livemediaurl) + "\" />";
		
		retVal += "<key name=\"session\" value=\"" + EscapeXML(_Tracklist.session) + "\"/>";
		retVal += "<key name=\"startindex\" value=\"" + EscapeXML(_Tracklist.startindex) + "\" />";
		retVal += "<key name=\"station\" value=\"" + EscapeXML(_Tracklist.station )+ "\"/>";
		retVal += "<key name=\"stationid\" value=\"" + EscapeXML(_Tracklist.stationid) + "\" />";
		retVal += "<key name=\"stopGuid\" value=\"" + EscapeXML(_Tracklist.stopGuid) + "\" />";
		retVal += "<key name=\"timecode\" value=\"" + EscapeXML(_Tracklist.timecode) + "\" />";
		retVal += "<key name=\"type\" value=\"" + EscapeXML(_Tracklist.type )+ "\" />";
		retVal += "<key name=\"autohide\" value=\"" + EscapeXML(_Tracklist.autohide )+ "\" />";
		retVal += "<key name=\"autoplay\" value=\"" + EscapeXML(_Tracklist.autoplay )+ "\" />";
		retVal += "<key name=\"autoshuffle\" value=\"" + EscapeXML(_Tracklist.autoshuffle) + "\" />";
		retVal += "<key name=\"continuing\" value=\"" + EscapeXML(_Tracklist.continuing )+ "\" />";
		retVal += "<key name=\"deleteable\" value=\"" + EscapeXML(_Tracklist.deleteable) + "\" />";
		retVal += "<key name=\"flybacking\" value=\"" + EscapeXML(_Tracklist.flybacking) + "\" />";
		retVal += "<key name=\"flycasting\" value=\"" + EscapeXML(_Tracklist.flycasting) + "\"/>";
		retVal += "<key name=\"offline\" value=\"" + EscapeXML(_Tracklist.offline) + "\" />";
		retVal += "<key name=\"podcasting\" value=\"" + EscapeXML(_Tracklist.podcasting) + "\" />";
		retVal += "<key name=\"recording\" value=\"" + EscapeXML(_Tracklist.recording) + "\" />";
		retVal += "<key name=\"shoutcasting\" value=\"" + EscapeXML(_Tracklist.shoutcasting) + "\" />";
		retVal += "<key name=\"shuffleable\" value=\"" + EscapeXML(_Tracklist.shuffleable) + "\" />";
		retVal += "<key name=\"shuffled\" value=\"" + EscapeXML(_Tracklist.shuffled) + "\" />";
		retVal += "<key name=\"throwaway\" value=\"" + EscapeXML(_Tracklist.throwaway )+ "\" />";
		retVal += "<key name=\"users\" value=\"" + EscapeXML(_Tracklist.users) + "\" />";
		
		return retVal;
	}
	
	public synchronized String GenerateTrackParameters(DPXMLTrack track)
	{
		String retVal= "";
		retVal += "<key name=\"addart\" value=\"" + EscapeXML(track.addart) + "\" />";				
		retVal += "<key name=\"adurl\" value=\"" + EscapeXML(track.adurl) + "\" />";
		retVal += "<key name=\"album\" value=\"" + EscapeXML(track.album) + "\" />";
		retVal += "<key name=\"albumfile\" value=\"" + EscapeXML(track.albumfile) + "\"/>";
		retVal += "<key name=\"artist\" value=\"" + EscapeXML(track.artist) + "\" />";
		retVal += "<key name=\"bitrate\" value=\"" + EscapeXML(track.bitrate) + "\" />";
		retVal += "<key name=\"current\" value=\"" + EscapeXML(track.current )+ "\" />";
		retVal += "<key name=\"expdays\" value=\"" + EscapeXML(track.expdays) + "\" />";
		retVal += "<key name=\"expplays\" value=\"" + EscapeXML(track.expplays )+ "\" />";
		retVal += "<key name=\"filename\" value=\"" + EscapeXML(track.filename) + "\" />";
		retVal += "<key name=\"guidIndex\" value=\"" + EscapeXML(track.guidIndex )+ "\" />";
		retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";	
		if(EscapeXML(track.guidSong ).trim().equalsIgnoreCase("") ){
			if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Should never reach here");
		}
		if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Sent track with guid: " + EscapeXML(track.guidSong ) + " at index : " + track.IndexInList);
		if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Sent track with guid: " + EscapeXML(track.guidSong ) + "track.listened " +track.listened);
		retVal += "<key name=\"imageurl\" value=\"" + EscapeXML(track.imageurl)+ "\" />";
		retVal += "<key name=\"length\" value=\"" + EscapeXML(track.length )+ "\" />";
		retVal += "<key name=\"mediatype\" value=\"" + EscapeXML(track.mediatype )+ "\" />";
		retVal += "<key name=\"mediaurl\" value=\"" + EscapeXML(track.mediaurl)+ "\" />";
		retVal += "<key name=\"metadata\" value=\"" + EscapeXML(track.metadata )+ "\" />";
		retVal += "<key name=\"numplay\" value=\"" + EscapeXML(track.numplay) + "\" />";
		retVal += "<key name=\"offset\" value=\"" + EscapeXML(track.offset )+ "\" />";
		retVal += "<key name=\"redirect\" value=\"" + EscapeXML(track.redirect)+ "\"/>";
		retVal += "<key name=\"seconds\" value=\"" + EscapeXML(track.seconds )+ "\" />";
		retVal += "<key name=\"start\" value=\"" + EscapeXML(track.start )+ "\" />";
		retVal += "<key name=\"starttime\" value=\"" + EscapeXML(track.starttime )+ "\" />";
		retVal += "<key name=\"stationid\" value=\"" + EscapeXML(track.stationid )+ "\" />";
		retVal += "<key name=\"syncoff\" value=\"" + EscapeXML(track.syncoff )+ "\" />";
		retVal += "<key name=\"timecode\" value=\"" + EscapeXML(track.timecode )+ "\" />";
		retVal += "<key name=\"title\" value=\"" + EscapeXML(track.title )+ "\" />";
		retVal += "<key name=\"type\" value=\"" + EscapeXML(track.type )+ "\" />";
		retVal += "<key name=\"audioAd\" value=\"" + EscapeXML(track.audioAd )+ "\" />";
		retVal += "<key name=\"buffered\" value=\"" + EscapeXML(track.buffered )+ "\" />";
		retVal += "<key name=\"cached\" value=\"" + EscapeXML(track.cached) + "\" />";
		retVal += "<key name=\"clickAd\" value=\"" + EscapeXML(track.clickAd )+ "\" />";
		retVal += "<key name=\"covered\" value=\"" + EscapeXML(track.covered )+ "\" />";
		retVal += "<key name=\"delayed\" value=\"" + EscapeXML(track.delayed )+ "\" />";
		retVal += "<key name=\"finished\" value=\"" + EscapeXML(track.finished )+ "\" />";
		retVal += "<key name=\"flush\" value=\"" + EscapeXML(track.flush )+ "\" />";
		retVal += "<key name=\"flyback\" value=\"" + EscapeXML(track.flyback )+ "\" />";
		retVal += "<key name=\"listened\" value=\"" + EscapeXML(track.listened )+ "\" />";
		retVal += "<key name=\"played\" value=\"" + EscapeXML(track.played )+ "\" />";
		retVal += "<key name=\"playing\" value=\"" + EscapeXML(track.playing )+ "\" />";
		retVal += "<key name=\"redirected\" value=\"" + EscapeXML(track.redirected )+ "\" />";
		retVal += "<key name=\"redirecting\" value=\"" + EscapeXML(track.redirecting )+ "\" />";
		retVal += "<key name=\"reloadAd\" value=\"" + EscapeXML(track.reloadAd )+ "\" />";
		retVal += "<key name=\"synced\" value=\"" + EscapeXML(track.synced) + "\" />";
		retVal += "<key name=\"terminating\" value=\"" + EscapeXML(track.terminating )+ "\" />";
		retVal += "<key name=\"unsupported\" value=\"" + EscapeXML(track.unsupported )+ "\" />";
		retVal += "<key name=\"IndexInList\" value=\"" + EscapeXML(track.IndexInList )+ "\" />";
		 
		return retVal;
	}
	
	public synchronized String GetTrackMessages(){
		String retVal = "<XML>";
		retVal += "<ResourceContainer name=\"TrackList\">";
		retVal += "<Metadata>";
		retVal += GenerateTracklistParameters(_Tracklist);
		retVal += "</Metadata>";
		for(int i=0; i<_Tracklist.children.size(); i++)
		{
			DPXMLTrack track = (DPXMLTrack)_Tracklist.children.get(i);
			DPTrackState state = getTrackState(track);
			boolean b = false;
			try{
				b = state.AddStateSentToClient();
			}catch(Exception ex){
				System.err.println("State Err " + ex.toString());
			}
			
			if( !b ){
				retVal += "<Resource name = \"Track\">";
				retVal += GenerateTrackParameters(track);
				retVal += "</Resource>";
			}
			else
			{
				System.err.println("Should Not be here.");
			}
		}
		retVal += "</ResourceContainer>";
		//retVal += getAllMessagesString();
		retVal += "</XML>";
		
		return retVal;
	}
	
	public DPXMLTrack GetTrack(String guid) throws DPException {
		
		//if(Debug.isDebuggerConnected()) Log.d("DPApplication", "Tracklist size is"+_Tracklist.children.size());
		for(int i=0; i<_Tracklist.children.size(); i++)
		{
			DPXMLTrack track = (DPXMLTrack)_Tracklist.children.get(i);
			// if( track.flyback ) continue;//12july2010 not sure it should be
			// here or not
			// as can we get a request to play a flybcak track?? we get request
			// to play only when we have buffered this track
			if(track.guidSong == null ) continue;
			if(track.guidSong.equals(guid)){
				//if(Debug.isDebuggerConnected()) Log.d("DPApplication", "Track Found");
				setCurrentPlayingIndex(i);
				setTrackmaxplayed(i);
				return track;
			}
		}
		if(Debug.isDebuggerConnected()) Log.d("DPApplication", "Track not found");
		throw new DPException("Track not found");
		
	}
		
	public void SetTrackAsCurrent(String guid){
		int index  = -1;
			if(Debug.isDebuggerConnected()) Log.d("NextGUID", "TrackList count is " + _Tracklist.children.size());
		for(int i=0; i<_Tracklist.children.size(); i++)
		{
			DPXMLTrack track = (DPXMLTrack)_Tracklist.children.get(i);
			//if( track.flyback ) continue;
			if(track.guidSong == null ) continue;
			
			if(track.guidSong.equals(guid)){
				index = i;
				break;
			}
		}
		
		if( index != -1 )
		{
			if(Debug.isDebuggerConnected()) Log.d("NextGUID", "Setting the index as " + index);
			_DownLoader.SetTrackIndex(index);
		}
	}

	public void SetTrackAsCurrentForClient(int index){
		if( index != -1 )
		{
			if(Debug.isDebuggerConnected()) Log.d("NextGUID", "Setting the index as " + index);
			_DownLoader.SetTrackIndexForClient(index);
		}
	}

	/*******************
	 * START FOR RECORDING
	 * 
	 * @throws DPException
	 * @throws DPException
	 *             *
	 *******************************/

	/*
	 * deleteTrack method can throw "Track not found exception" 
	 * 
	 */
	
	public DPXMLTracklist deleteTrack(String uid, String stationID, String songGUID) throws DPException 
	{
		DPFileHandler fileHandler = new DPFileHandler(uid);		
		DPXMLTrack track = fileHandler.getTrack(stationID, songGUID);
		DPXMLTracklist trackList = fileHandler.deleteTrack(track);		
		return trackList;
	}
	
	public boolean deleteRecording(String uid, String stationID)
	{
		DPFileHandler fileHandler = new DPFileHandler(uid);		
		boolean isDeleted = fileHandler.deleteRecording(uid, stationID);				
		return isDeleted;
	}
	
	public boolean deleteAllRecording(String uid)
	{
		DPFileHandler fileHandler = new DPFileHandler(uid);		
		boolean isDeleted = fileHandler.deleteAllRecording(uid);				
		return isDeleted;
	}
	
	public String getRecording(String uid) throws DPException
	{	ArrayList<DPXMLTracklist> allTrackList=null;
		DPFileHandler fileHandler = new DPFileHandler(uid);	
		allTrackList = fileHandler.getAllTrackList(uid);
		
		String recordingStr = null;
		if(allTrackList == null)//No Recorded Track, so return current data
		{
			recordingStr = GetTrackMessages();
		}else
		{
			recordingStr = GetAllTrackMessages(allTrackList);
		}
		return recordingStr;
	}
	
	public String GetAllTrackMessages(ArrayList<DPXMLTracklist> allTrackList){
		String retVal = "<XML>";
		for(DPXMLTracklist trackList: allTrackList)
		{
			if(trackList != null)
			{
				retVal += "<ResourceContainer name=\"TrackList\">";
				retVal += "<Metadata>";
				retVal += GenerateTracklistParameters(trackList);
				retVal += "</Metadata>";
				for(int i=0; i< trackList.children.size(); i++)
				{
					DPXMLTrack track = (DPXMLTrack)trackList.children.get(i);
					retVal += "<Resource name = \"Track\">";
					retVal += GenerateTrackParameters(track);
					retVal += "</Resource>";
				}
				retVal += "</ResourceContainer>";
			}						
		}		
		retVal += "</XML>";		
		return retVal;
	}
	
	
	/*******************END FOR RECORDING********************************/
	
	public synchronized String GetAllMessages(){
		String retVal = "<XML>";
		//Before sending the Message YOU MUST ADD THE NODES FOR THE EXCEPTION.
		retVal += getAllMessagesString();
		retVal += "</XML>";
	
		return retVal;
	}
	
	//-- PRE 12-15-09 version ----------
	private synchronized String getAllMessagesString(){
		String retVal = "";
		DPMessage m = null;
		synchronized(_Lock) {
			Iterator<DPMessage> iter = _MessageQueue.iterator();
				while( iter.hasNext()){
					m = iter.next();
					try
					{
						if( m.type == DPMessage.RECORDING_HAS_FINISHED){
							retVal += "<ResourceContainer name=\"RECORDING_HAS_FINISHED\">";
							retVal += "<Metadata/>";
							retVal += "<Resource/>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.RECORDING_HAS_STARTED){
							retVal += "<ResourceContainer name=\"RECORDING_HAS_STARTED\">";
							retVal += "<Metadata/>";
							retVal += "<Resource/>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.RETRYING_CONNECTION){
							retVal += "<ResourceContainer name=\"RETRYING_CONNECTION\">";
							retVal += "<Metadata/>";
							retVal += "<Resource/>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.STREAM_IS_BUFFERED){
							retVal += "<ResourceContainer name=\"STREAM_IS_BUFFERED\">";
							retVal += "<Metadata/>";
							retVal += "<Resource/>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_HAS_UPDATED){
							retVal += "<ResourceContainer name=\"TRACK_HAS_UPDATED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_BUFFERED){
							DPXMLTrack track = (DPXMLTrack)m.track;
							DPTrackState state = getTrackState(track);
							boolean b = true;
							try{
								b = state.BufferedStateSentToClient() ;
							}catch(Exception ex){
								System.err.println("State Err " + ex.toString());
							}
							if ( !b ){
								retVal += "<ResourceContainer name=\"TRACK_IS_BUFFERED\">";
								retVal += "<Metadata/>";
								retVal += "<Resource>";
								if( track != null)
								{
									if(Debug.isDebuggerConnected()) Log.d("TrackIfo", "Track buffered for GUID " + track.guidSong);
									retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";
								}
								retVal += "</Resource>";
								retVal += "</ResourceContainer>";
							}
							else
							{
								System.err.println("Should not be here.");
							}
						}
						else if( m.type == DPMessage.TRACK_IS_CACHED){
							retVal += "<ResourceContainer name=\"TRACK_IS_CACHED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";
							retVal += "</Resource>";
			
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_COVERED){
							retVal += "<ResourceContainer name=\"TRACK_IS_COVERED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" +EscapeXML( track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_ENDED){
							retVal += "<ResourceContainer name=\"TRACK_IS_ENDED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong) + "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_PAUSED){
							retVal += "<ResourceContainer name=\"TRACK_IS_PAUSED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" +EscapeXML( track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_PLAYING){
							retVal += "<ResourceContainer name=\"TRACK_IS_PLAYING\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong) + "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_RECORDING){
							retVal += "<ResourceContainer name=\"TRACK_IS_RECORDING\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_IS_STARTING){
							retVal += "<ResourceContainer name=\"TRACK_IS_STARTING\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" +EscapeXML(track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACK_WAS_ADDED){
							DPXMLTrack track = (DPXMLTrack)m.track;
							DPTrackState state = getTrackState(track);
							//if( m.track.flyback) continue;
							boolean b = true;
							try{
								b = state.AddStateSentToClient();
							}catch(Exception ex){
								System.err.println("State Err " + ex.toString());
							}
							if( !b || _Tracklist.shoutcasting ){
								retVal += "<ResourceContainer name=\"TRACK_WAS_ADDED\">";
								retVal += "<Metadata/>";
								retVal += "<Resource name = \"Track\">";
								retVal += GenerateTrackParameters(track);
								retVal += "</Resource>";
								retVal += "</ResourceContainer>";
							}
							else{
								System.err.println("Should not be here.");
							}
						}
						else if( m.type == DPMessage.TRACK_WAS_REMOVED){
							retVal += "<ResourceContainer name=\"TRACK_WAS_REMOVED\">";
							retVal += "<Metadata/>";
							DPXMLTrack track = (DPXMLTrack)m.track;
							retVal += "<Resource>";
							if( track != null)
								retVal += "<key name=\"guidSong\" value=\"" + EscapeXML(track.guidSong )+ "\" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
						else if( m.type == DPMessage.TRACKLIST_EMPTY){
							retVal += "<ResourceContainer name=\"TRACKLIST_EMPTY\">";
							retVal += "<Metadata/>";
							retVal += "</ResourceContainer>";
						}
					
						/*
						 * It will send exception to the client
						 * 
						 */
						
						/*
						 * 
						 * get message and errorcode from DPExceptionMessage
						 */
						
						else if( m.type == DPExceptionMessage.EXCEPTION){
							retVal += "<ResourceContainer name=\"EXCEPTION\">";
							retVal += "<Metadata/>";
							retVal += "<Resource>";
							retVal += "<key name=\"ErrorMessage\" value=\"message \" />";
							retVal += "<key name=\"ErrorCode\" value=\"ErrCode \" />";
							retVal += "</Resource>";
							retVal += "</ResourceContainer>";
						}
					}
					catch(Exception ex){
						
					}
				}
				
			/*	String live = "FALSE";
				if(_DownLoader.isLive())
					live = "TRUE";
				
				retVal += "<ResourceContainer name=\"IS_LIVE\">";
				retVal += "<Metadata/>";
				retVal += "<Resource>";
				retVal += "<key name=\"IS_LIVE\" value=\"" + EscapeXML(live )+ "\" />";
				retVal += "</Resource>";
				retVal += "</ResourceContainer>";

			 */				
				_MessageQueue.clear();
		}
		
		return retVal;
	}
	
	public void RemoveMessages(){
		if( _MessageQueue != null)
		_MessageQueue.clear();
	}
	
	
	private String EscapeXML(Object obj){
		if( obj == null)
			return "";
		
		try
		{
			return DPUtility.EscapeXML(obj.toString());
		}
		catch(Exception ex){
			return "";
		}
	}
	
	public String saveTracklist(String uid){
		_Tracklist = DPTracklistManager.inInstance().persistTracklist(_Tracklist);
		DPFileHandler fileHandler = new DPFileHandler(uid);
		_Tracklist = fileHandler.saveTracklist(_Tracklist);		
		return GetTrackMessages();
	}
	
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setName(String name) {
		this.app_name = name;
	}
	
	/**
	 * Try to find the track in the track list which has been saved on the file system for this 
	 * station. If the track is found then delete the track from the saved track list and save the 
	 * track list again to the file system.
	 * 
	 * Return the found track list.
	 * 
	 * THIS FUNCTION NEVER CRASHES. DEBUG IF REQUIRED.
	 * @param stationId
	 * @param track
	 * @return
	 */
	public DPXMLTrack GetCachedTrack(int stationId, DPXMLTrack track)
	{
		try
		{
		DPFileHandler fHandler = new DPFileHandler(DPApplication.Instance().getUid());
		DPXMLTracklist tList = fHandler.readTracklist(stationId);
		DPXMLTrack foundTrack = findTrack(tList, track );
		if( foundTrack != null )//track has been deleted from the track list
			fHandler.saveTracklist(tList);
		return foundTrack;
		}
		catch(Exception ex){
			//NEVER EVER LET THIS FUNCTION CRASH THE APP. LET TRACK BE SAVED MULTIPLE TIMES
			return null;
		}
	}

	/**
	 * This message clear the _MessageQueue.
	 * After clear it we will add only exceptions to the _MessageQueue so that exceptions can be handled
	 * 
	 */
	public void clearMessageQueue(){
		_MessageQueue.clear();
	}

	public void CleanUp(){
		_MessageQueue.clear();
		_TrackStateManager.clear();
		_Tracklist = null;
		_DownLoader.stopThread();
		_DownLoader = null;
	}

	public int getCurrentPlayingIndex() {
		return currentlyPlayingIndex;
	}

	public void setCurrentPlayingIndex(int index) {
		currentlyPlayingIndex = index;
	}

	public void setTrackmaxplayed(int currentIndex) {

		if (currentlyPlayingIndex > trackMaxPlayed) {
			trackMaxPlayed = currentlyPlayingIndex;
		}

	}

	public int getTrackmaxplayed() {
		return trackMaxPlayed;
	}
}
