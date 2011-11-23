package fm.flycast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

import com.appMobi.appMobiLib.util.Debug;
import android.os.Environment;
import android.util.Log;

public class DPManager
{
	
	private boolean changeDownLoadIndexCalled = false;
	private boolean flybackStarted = false;
	private DPXMLTrack thirdTrackInTracklist=null;
	
	public static boolean flyback = false;
	public static int flybackURLIndex = 0;
	private DPXMLTrack terminatingTrack=null;
	int playindex = -1;
	
	/**
	 * station is declared to find the track information in sdcard
	 *
	 */
    private String baseDir = null;
	private int station=-1;
	private DPXMLTrack track=null;

	public  int MinimumSecondsBuffered = 10;	//BH_12-01-09 Seconds to buffer before sending TRACK_IS_BUFFERED
	public  int MinimumBytesBuffered;		//MinimumBytesBuffered = MinimumSecondsBuffered * bitrate * 128;
	public  int currenttrackindex = 0;
    public  static final int READ_CHUNK = 16384;
    public  static final int PACKET_SIZE = 16384;
    public  static final int UNKNOWN_SIZE = 117964800;
    public  int initial = 16384;
    public  int podinitial = 60 * 128 * 64;
    public  int bitrate = 128;
    public  int buffersize = 64;
    public  int streamsize = bitrate * (128 * buffersize);
    public  int filesize = bitrate * (128 * 1);
    public  int metaint = -1;
    public  int metatoread = -1;
    public  int metatilnext = -1;
    public  String metastr = null;
    public  Vector<DPXMLTracklist> _recordings = new Vector<DPXMLTracklist>();
    public  int[] version1layer3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0 };
    public  int[] version2layer1 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0 };
    public  int[] version2layer3 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 };

    private Socket socket = null;
    InputStream is = null;
    OutputStream os = null;
    private DPFileReaderWriter writer = null;
    private static DPFileReaderWriter logWriter = null;
    private static DPFileReaderWriter messageQueueWriter = null;

    public  Random _random;

    public  volatile int streamRead = 0;
    public  volatile int bufferWrote = 0;
    public  volatile int fileWrote = 0;
    public  volatile int totalLength = UNKNOWN_SIZE;
    public  volatile int streamLength = streamsize;
    public  volatile boolean buffered = false;
    public  volatile boolean trackcached = false;
    public  volatile boolean badtrack = false;

    private ConnectionThread loaderThread = null;
    //private SessionThread sessionThread = null;
    public  DPXMLTracklist _tracklist = null;
    public  DPXMLTracklist _nexttracklist = null;
    public  DPXMLTrack _track = null;
    public  int _currentindex = -1;
    public  int _nextindex = -1;
    public String _host = null;
    public String _parm = null;
    public String app_name = null;
    public DownloadIndexChanged _IndexChanged = null;
    
    private volatile boolean flybacking = false;
    private volatile String flybackTerminatingGUID = null;
 
    private String uid = null;

    public void init()
    {
     	baseDir = Environment.getExternalStorageDirectory().getPath() + "/" + app_name + "/data/";

        DPFileReaderWriter.CreateDir(baseDir);

        String logfile = baseDir + "logFile.txt";
        String messageQueue = baseDir + "messageQueue.txt";

	    logWriter = new DPFileReaderWriter();
	    messageQueueWriter = new DPFileReaderWriter();
	    logWriter.Open(logfile, false);
	    messageQueueWriter.Open(messageQueue, false);
    }

    public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public DPManager(String name, DPXMLTracklist tracklist)
    {
		app_name = name;
		init();
        _tracklist = tracklist;
        _random = new Random(System.currentTimeMillis());
        loaderThread = new ConnectionThread();
        loaderThread.start();
    }

    public void startTracklist(DPXMLTracklist tracklist)
    {
        buffered = false;
        _nexttracklist = tracklist;
        _nextindex = 0;
    }
    

    public void playTracklist(DPXMLTracklist tracklist, int index)
    {
    	 buffered = false;
        _nexttracklist = tracklist;

        _nexttracklist = tracklist;
        _nextindex = index;
         currenttrackindex = index;
        
        //-- BEGIN BH_12-01-09 ------
        bitrate = tracklist.bitrate;
        MinimumBytesBuffered = MinimumSecondsBuffered * bitrate * 128;
        if (MinimumBytesBuffered<65536) MinimumBytesBuffered = 65536;	//64KB
        if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "DPManager playTracklist tracklist.bitrate=" + tracklist.bitrate);
        if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "DPManager playTracklist MinimumBytesBuffered=" + MinimumBytesBuffered);

        streamsize = bitrate * (128 * buffersize);
        filesize = bitrate* (128 * 1);
        //-- END BH_12-01-09 ------
    }

    public void recordTracklist(DPXMLTracklist tracklist)
    {
        _recordings.addElement(tracklist);
    }

    
    public void SetTrackIndex(int index){
    	if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "SetTrackIndex called with  index  " + index);
    	
    	if(index==_tracklist.startindex && thirdTrackInTracklist==null){
    		thirdTrackInTracklist = (DPXMLTrack)_tracklist.children.get(index);
    		terminatingTrack = (DPXMLTrack)_tracklist.children.get(index);
    		if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "third track and terminatingTrack got initialized");
    	}
    	
    	while(changeDownLoadIndexCalled){
    		if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Going to sleep for 100ms....");	
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		
    	}
    	
		currenttrackindex = index;

			if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the currenttrackindex as  " + currenttrackindex);
			
	//	}
    }
    
    /*
    private DPXMLTrack findTrack(String guid){
		for(int i=0; i<_tracklist.children.size(); i++)
		{
			DPXMLTrack track = (DPXMLTrack)_tracklist.children.get(i);
			//if( track.flyback ) continue;
			if(track.guidSong == null ) continue;
			
			if(track.guidSong.equals(guid)){
				return track;
			}
		}
		
		return null;
    }
    //*/
    
    public void SetTrackIndexForClient(int index) {
    	if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "SetTrackIndexForClient called with  index  " + index);
    	
    	/*
    	 * Narinder 04/03/2010
    	 * When index is same as playIndex do not do anything
    	 * just return
    	 */
    	if(index==currenttrackindex){
    		if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Index is same as currenttrackindex....");
    		return;
    	}
	changeDownLoadIndexCalled = true;
    	_IndexChanged =  new DownLoadChangedImpl(index);
    }
    
    public class DownLoadChangedImpl implements DownloadIndexChanged{
    	public int index = 0;
    	
    	public DownLoadChangedImpl(int i ){
    		index = i;
    	}
    	
		public void ChangeDownLoadIndex() {
			
			if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "ChangeDownLoadIndex() called with  index  " + index);
			if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "thirdtrackis  " + thirdTrackInTracklist);
			if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flybackStarted  " + flybackStarted);
			
			boolean setWriterNull = false;
	    
			int i = 0;
			if (index >= currenttrackindex) {
				changeDownLoadIndexCalled = false;
				if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", " currenttrackindex is " + currenttrackindex
						+ " clicked image index is.. " + index);

				if (thirdTrackInTracklist != null && flybackStarted) {
					int s = _tracklist.children.size();

					for (; i < s; i++) {
						DPXMLTrack t = (DPXMLTrack) _tracklist.children
								.elementAt(i);
						
						if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "t.guid is ..... " + t.guidSong);
						
			        		
						if ((!t.flyback) && (t.guidSong!=null) && (t.guidSong)
								.equalsIgnoreCase(thirdTrackInTracklist.guidSong)){
							  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Position of termnating track is " + i);
							break;
						}
					}

					if (i <= index) {//going in live mode from flyback
						//currenttrackindex = index;
						DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
						 if(!newTrack.cached){
							  _currentindex = index;
							  _track = newTrack;
							  setWriterNull = true;
							  _host = null;
						  }
						 else{
							 /*
							  * find the first non cached track in tracklist and set _currentindex to that 
							  * 
							  */
							 int j;
							 boolean trackFound = false;
							 
							 for(j=index+1;j<_tracklist.children.size();j++){
					
								try{	
								 DPXMLTrack tempTrack = (DPXMLTrack)_tracklist.children.elementAt(j);
									 if(tempTrack.cached)
										 continue;
									 _currentindex = j;
									  _track = tempTrack;
									  setWriterNull = true;
									  _host = null;
									  trackFound = true;
									  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Track found at  index " + j);
									  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);
									  break;
									  
								}catch(ArrayIndexOutOfBoundsException ex){
									if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Exception .. " + ex.getMessage());
								}
							 }
							 if(!trackFound){//may not require this code..
								 DPXMLTrack tempTrack = (DPXMLTrack)_tracklist.children.elementAt(j-1);
								 _currentindex = j-1;
								  _track = tempTrack;
								  setWriterNull = true;
								  _host = null;
								  trackFound = true;
								  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Track not  found  ");
								  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);
							 }
							 
						 }
						 
						//flyback = false;
						flybacking = false;
						_tracklist.recording = false;
						  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flybacking is .. " + flybacking);
								
					} else {
						//currenttrackindex = index;
						DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
						 if(!newTrack.cached){
							  _host = null;
							 _currentindex = index;
							  setWriterNull = true;
						  _track = newTrack;
						
						  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);	
						  }
					/*
					 * Check here what is the nearest non cached track before terminating track..
					 * 	
					 */
					}
				}
				else {
					DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
					  if(!newTrack.cached){
						  _host = null;
						  _currentindex = index;
						  setWriterNull = true;
						  _track = newTrack;
						  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);		  
					  }
					//currenttrackindex = index;
				}
				
			}

			else if (index < currenttrackindex) {
				changeDownLoadIndexCalled = false;
				if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "currenttrackindex is " + currenttrackindex
						+ " .index to switch is.. " + index);

				if (thirdTrackInTracklist != null && flybackStarted) {
					int s = _tracklist.children.size();
					int k; 
					for (k=0; k < s; k++) {
						DPXMLTrack t = (DPXMLTrack) _tracklist.children
								.elementAt(k);
						if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "t.guid is ..... " + t.guidSong);
						if ((!t.flyback) && (t.guidSong!=null) && (t.guidSong)
								.equalsIgnoreCase(thirdTrackInTracklist.guidSong))
							break;
					}

					if (k >= index) {//going in flyback again..
					
						 DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
						 
						 if(newTrack.flyback){
							 changeDownLoadIndexCalled = false;
							  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flyback track returning....");
							return;
						}
						 
						 
						 if(!newTrack.cached){
							 _currentindex = index;
							 _host = null;
							  setWriterNull = true;
							  _track = newTrack;
							  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);	
						 }
						 else{
							 /*
							  * find first non cached track and set its index as current index 
							  * 
							  */
							 
							 int j;
							 
							 for(j=index+1;j<k;j++){
					
								try{	
								 DPXMLTrack tempTrack = (DPXMLTrack)_tracklist.children.elementAt(j);
									 if(tempTrack.cached)
										 continue;
									 _currentindex = j;
									  _track = tempTrack;
									  setWriterNull = true;
									  _host = null;
									  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Track found at  index " + j);
									  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);
									  break;
									  
								}catch(ArrayIndexOutOfBoundsException ex){
									if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Exception .. " + ex.getMessage());
								}
							 }
							 
						 }
						//currenttrackindex = index;
						flybacking = true;
						_tracklist.recording = true;
						if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Flybacking again ....");
					}
					else{
						DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
						if(!newTrack.cached){
							_host = null; 
							_currentindex = index;
							  setWriterNull = true;
							  _track = newTrack;
							  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the CurrentIndex as " + _currentindex);	 
						 }
						//currenttrackindex = index;
					}

				}
				else {
					
					
					DPXMLTrack newTrack = (DPXMLTrack)_tracklist.children.elementAt(index);
					
					if(newTrack.flyback){
						changeDownLoadIndexCalled = false;	
						  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flyback track returning....");
						return;
					}
					
					
					else if(!newTrack.cached){//it accomodates flybacks automatically as flyback track is not cached
						  _currentindex = index;
						  _track = newTrack;
						  setWriterNull = true;
						  _host = null;
						  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Setting the current index as  " + _currentindex); 
					}
				}
			}
			if(setWriterNull ){
				writer = null;
			}
				/*
				 * 16july 2010
				 * make it false as soon as possible otherwise it will take some time to switch the tracks 
				 * even though the tracks are buffered
				 * 
				 *  Other option is to check in SetTrackIndex is if the track is buffered add the track is buffered message to message Queue
				 * 
				 */
			changeDownLoadIndexCalled = false;
			 if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "setTrackIndexForClientCalled is complete.....");
		}
		
			
    }

    public void openfile(int station, DPXMLTrack track)
    {
    	this.station=station;
    	this.track=track;
    	String dirPath;

    	//-- NEW WAY ------
    	dirPath = baseDir + uid + "/" + station + "/";

    	//Environment.getExternalStorageDirectory()
    	try
        {
            //dirPath = DPStringConstants.STR_MEDIA_DIR + DPStringConstants.TEMP_STR_SLASH +uid+ DPStringConstants.TEMP_STR_SLASH+station + DPStringConstants.TEMP_STR_SLASH;
            DPFileReaderWriter.CreateDir(dirPath);
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "openfile CreateDir Exception " + e);
        	if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "openfile CreateDir dirPath=" + dirPath);
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (114) -- " + e.getMessage());
        }

        if( track.filename == null )
        {
        	//For Debugging purpose
        	if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "openfile track.filename was NULL");

        	//-- NEW WAY ------
            track.filename = dirPath = baseDir + uid + "/" + station + "/"+ track.guidSong;

            if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "openfile new track.filename " + track.filename);
        }

        try
        {
            //System.err.println( "***** -- Opening new filename in DOWNLOADER -- " + track.filename );
            writer = new DPFileReaderWriter(_track);
            writer.Open(track.filename, false);

            //For Debugging purpose
       
          DPXMLTrack temp = DPTracklistManager.inInstance().findTrack( _tracklist, track );
            if( temp != null )
            {
                String url = _track.mediaurl;
                int offset = _track.offset;
                _track.copy(temp);
                _track.listened = (_track.listened == true) || (_tracklist.recording == false) || ( (_tracklist.recording == true) && (_currentindex - 1 < currenttrackindex) );
                _track.mediaurl = url;
                _track.offset = offset;
                _track.current = offset + _track.length;
                trackcached = true;
                return;
            }

            int writeIndex = track.start + (track.current - track.offset);
            if( writeIndex == 0 )
            {
            	int looper = 11 + ( Math.abs(_random.nextInt()) % 217 );
                byte[] b1 = new byte[]{(byte)0x34,(byte)0x45};
            	for( int i = 0; i < looper; i++ )
                {
                    writer.writer.write(b1);

                    int sizer = 175 + ( Math.abs(_random.nextInt()) % 50 );
                    track.start += sizer + 2;
                    for( int j = 0; j < sizer; j++ )
                    {
                    	int random = Math.abs(_random.nextInt());
                    	writer.writer.write(random);
                    	writeIndex += 1;
                    }
                }

            	bufferMessage();
            }
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("Device Proxy", "openfile Writer Exception " + e);
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (167) -- " + e.getMessage());
        }
    }	//public void openfile(int station, DPXMLTrack track)

    public void bufferMessage()
    {
    	if(track != null)
        {
          	DPTrackState dts = DPApplication.Instance().getTrackState(track);
        	if( dts != null && dts.GetTrackState() != 3 )
        	{        		
	        	if( (track.current - track.offset) > MinimumBytesBuffered )
	        		DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, track));
        	}
        }
   }
    
    public void closefile()
    {
    	if(writer != null)
        {
        	try
        	{
	    		String filename = baseDir + uid + "/" + station + "/" + track.guidSong;
	        	File file = new File(filename);

	        	DPTrackState dts = DPApplication.Instance().getTrackState(track);

	        	if(file.exists() && dts != null && dts.GetTrackState() != 3){
	        		// DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _track));
	        		 DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, track));
	        	}
	        	
	        	writer.Close();
	        	writer = null;
        	}
        	catch(Exception ex){
        		// Allow the socket to close.
        	}
        }

        try
        {
	        if(socket != null)
	        {
	        	socket.close();
	        	socket = null;
	        }
        }
        catch(IOException ex)
        {

        }
    }	//public void closefile()

    /*
     *This method will stop the running thread
     *if there is an error with the SD Card
     *
     */

    public void stopThread()
    {
       //Nothing required here
    	loaderThread.stop();
    }

    public void cleanup()
    {
       //Nothing required here
    }

    public void trackRemoved()
    {
        _currentindex--;
        if( _currentindex < 0 ) _currentindex = 0;
    }

    public void close()
    {
        if( loaderThread != null ) loaderThread.close();
        //if( sessionThread != null ) sessionThread.close();
    }

    public void reset()
    {
        try
        {
        	loaderThread.stop2();
        }
        catch(Exception ex){
        	//This should never ever throw exception.
        }
    }

    public boolean IsResuable(){
    	return loaderThread.IsReusable();
    }


    //------------------------------------------------
    //BEGIN ConnectionThread
    //------------------------------------------------
    private class ConnectionThread extends Thread
    {
        public String streamurl = null;
        private volatile boolean close = false;

        public synchronized void close()
        {
           close = true;
        }

        /**
         * DO NO THROW EXCEPTION.
         */
        public synchronized void stop2()
        {
            try
            {	thirdTrackInTracklist = null;
            	flybackStarted= false;
	        	_track = null;
	            _tracklist = null;
	            _currentindex = -1;
	            flybackTerminatingGUID = null;
	            flyback  = false;
	            flybacking = false;
	            closefile();
	            cleanup();
            }
            catch(Exception ex){

            }
        }

        public void endTrack(boolean cleartrack)
        {
            _track.length = _track.current - _track.offset;
            //_track.buffered = true;
            _track.buffered =  true;
         //   DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _track));
            bufferMessage();
            if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message in end track " + _track.guidSong);
            _track.cached = true;

            //BH_12-14-09
//            if(_track.bitrate==0)
//            	_track.bitrate=128;
            _track.seconds = _track.length / 128 / _track.bitrate;

//            m_app.callback(FlyCast.TRACK_IS_CACHED, _track);
            DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_IS_CACHED, _track));

            closefile();

            if( cleartrack == true )
            {
                _track = null;
                _host = null;
            }
        }

        public void addTrack()
        {
            boolean remove = ( _track.current - _track.offset == 0 );
            endTrack(false);
            if( remove == true )
            {
                _tracklist.children.removeElementAt( _currentindex );
                _currentindex--;
                DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_REMOVED, _track));
            }
            nextTrack();
        }

        public void nextTrack()
        {
            DPXMLTrack temp = new DPXMLTrack();
            temp.stationid = _tracklist.stationid;
            temp.bitrate = _track.bitrate;
            temp.mediaurl = _track.mediaurl;
            temp.timecode = System.currentTimeMillis();
            temp.offset = _track.current;
            temp.current = _track.current;
            temp.listened = (temp.listened == true) || (_tracklist.recording == false) || ( (_tracklist.recording == true) && (_currentindex - 1 < currenttrackindex) );
            temp.delayed = true;
            temp.expdays = _tracklist.expdays;
            temp.expplays = _tracklist.expplays;
            _track = temp;
            _currentindex++;

            /*
             * Narinder 02/02/2010
             * 
             * 
             */
            
        
            if(_track!=null && _tracklist.children.contains(_track)){
              	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", "Duplicate track entry for guid " + _track.guidSong);
            }
            else{
            
            	if(_track!=null /* && _track.guidSong!=null && !(_track.guidSong).equalsIgnoreCase("")*/ &&  !_tracklist.children.contains(_track)  ){
            	DPApplication.Instance().AddMessage(
							new DPMessage(DPMessageObject.TRACK_WAS_ADDED,
									_track));

            	
            	
            	
            	_track.IndexInList = _currentindex;
					_tracklist.AddTrackAt(_track, _currentindex);
					//_tracklist.children.insertElementAt(_track, _currentindex);
            	}
            }

            
            /*if(!_tracklist.children.contains(_track)){
            	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, _track));
            	_track.IndexInList = _currentindex;
            	_tracklist.children.insertElementAt(_track, _currentindex);	
            }*/
            
            
          
            /*
             *Narinder 02/01/2010 
             * set track listened to false before adding it to tracklist
             * 
             */
            
           // _track.listened=false;
           
           
            
            
            
            if(temp.IndexInList>2){
            	if(Debug.isDebuggerConnected()) Log.d("DPTracklist","Track added to tracklist"+ _track.guidSong + ".. delayed.." +_track.delayed+ "1ST COND");
            	if(Debug.isDebuggerConnected()) Log.d("DPTracklist","Track added to tracklist"+ _track.guidSong + "..listened .." +_track.listened);
            }
            
        }

        public String getHeaderValue(String headers, String key)
        {
            String temp = null;

            int where = headers.indexOf(key);
            if( where > -1 )
            {
                int ender = headers.indexOf(DPStringConstants.STR_LINEFEED, where + 1);
                if( ender > -1 )
                {
                    temp = headers.substring(where + key.length(), ender);
                    temp = temp.trim();
                }
            }

            if( temp != null && temp.length() == 0 ) temp = null;

            return temp;
        }

        public int getHeaderValueInt(String headers, String key)
        {
            int num = 0;

            int where = headers.indexOf(key);
            if( where > -1 )
            {
                int ender = headers.indexOf(DPStringConstants.STR_LINEFEED, where + 1);
                if( ender > -1 )
                {
                    String temp = headers.substring(where + key.length(), ender);
                    temp = temp.trim();
                    if( temp.length() > 0 ) num = Integer.parseInt(temp);
                    if( num < 0 ) num = UNKNOWN_SIZE;
                }
            }

            return num;
        }

        public int parseMetadata(byte[] data, int offset, byte[] parsed, int length)
        {
            int parsedoffset = 0;
            int outlength = 0;

            while( length > 0 )
            {
                if( metatilnext > 0 )
                {
                    int towrite = ( length > metatilnext ) ? metatilnext : length;
                    System.arraycopy(data, offset, parsed, parsedoffset, towrite);
                    offset += towrite;
                    parsedoffset += towrite;
                    metatilnext -= towrite;
                    length -= towrite;
                    outlength += towrite;
                }
                else if( metatoread == 0 )
                {
                    metatoread = data[offset] * 16;
                    offset += 1;
                    length -= 1;
                    if( metatoread == 0 )
                    {
                        metatilnext = metaint;
                    }
                    else
                    {
                        _track.artist = DPStringConstants.STR_EMPTY;
                        metastr = DPStringConstants.STR_EMPTY;
                    }
                }
                else
                {
                	try
                    {
                		int towrite = ( length > metatoread ) ? metatoread : length;
	                    String temp = new String( data, offset, towrite );
	                    metastr += temp;
	                    offset += towrite;
	                    metatoread -= towrite;
	                    length -= towrite;
	                    if( metatoread == 0 )
	                    {
	                        metatilnext = metaint;
	                        int where1 = metastr.indexOf(DPStringConstants.STR_STREAM_TITLE);
	                        int where2 = metastr.indexOf(DPStringConstants.STR_STREAM_URL);
	                        if( where1 > -1 && where2 > -1)
	                        {
	                        	metastr = metastr.substring(where1+12, where2);
	                        	metastr = metastr.replaceAll("'", "");
	                            _track.artist = metastr;
	//                            m_app.callback(FlyCast.TRACK_WAS_ADDED, DPStringConstants.STR_TRACK_ADDED);
	                            DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, _track));
	                            if(_track.IndexInList>2){
	                            	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", _track.guidSong + "..delayed.." +_track.delayed);
	                            	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", _track.guidSong + "..listened .." +_track.listened);
	                            } 
	                        }
	                        metastr = DPStringConstants.STR_EMPTY;
                        }
                    }
                    catch(Exception e) { System.err.println("error parsing shoutcast metadata"); e.printStackTrace(); }
                }
            }

            return outlength;
        }

        public int parseHeaders(byte[] data, boolean getlength)
        {
            int marker = 0;
            boolean found = false;

            try
            {
                String headers = new String(data);
                int where = headers.indexOf(DPStringConstants.STR_DOUBLE_LINEFEED);
                if( where > -1 )
                {
                    marker = (where + 4);
                }
                else return -1;

                //System.err.println( "HEADERS -- " + headers.substring(0, marker) );
                if( getlength == false ) return marker;

                /*
                System.err.println( " ***** TRACKS IN HEADERS -- title curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_TITLE ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- artis curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_ARTIST ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- album curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_ALBUM ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- metad curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_METADATA ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- guidi curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_MFLID ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- guidc curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_CACHEID ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- lengt curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_LENGTH ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- syncb curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_SYNCBYTE ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- title next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_TITLE ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- artis next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_ARTIST ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- album next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_ALBUM ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- metad next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_METADATA ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- guidi next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_MFLID ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- guidc next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_CACHEID ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- lengt next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_LENGTH ) + ")" );
                System.err.println( " ***** TRACKS IN HEADERS -- syncb next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_SYNCBYTE ) + ")" );
                //*/

                //System.err.println( "ROSCO TRACKS IN HEADERS -- title curr (" + getHeaderValue( headers, DPStringConstants.STR_FC_TITLE ) + ")" );
                //System.err.println( "ROSCO TRACKS IN HEADERS -- title next (" + getHeaderValue( headers, DPStringConstants.STR_FCN_TITLE ) + ")" );

                //if( )
                where = headers.indexOf(DPStringConstants.STR_NOT_FOUND);
                if( where > -1 )
                {
                    badtrack = true;
                    return marker;
                }

                if( _track != null )
                {
                    String newguid = getHeaderValue( headers, DPStringConstants.STR_FC_MFLID );

                    if(Debug.isDebuggerConnected()) Log.d("No more track found", "newGuid is..... "+newguid);
                  
                    if( _track.guidIndex != null && newguid != null && _track.guidIndex.equals( newguid ) == false )
                    {
                        if( _currentindex + 1 < _tracklist.children.size() )
                        {
                            DPXMLTrack temp = (DPXMLTrack) _tracklist.children.elementAt(_currentindex + 1);
                            if( temp.guidIndex.equals( newguid ) )
                            {
                                _track.length = _track.current - _track.offset;
                                _track.buffered =   true ;
                              //  DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _track));
                                bufferMessage();
                                if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message in unknown condition " + _track.guidSong);
                                _track.cached = true;

//                                m_app.callback(FlyCast.TRACK_IS_CACHED, _track);
                                DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_IS_CACHED, _track));

                                closefile();
                                _currentindex++;
                                _track = temp;
                            }
                        }
                        else
                        {
                            if( _tracklist.recording == true && _track.terminating == true )
                            {
                            	 if(Debug.isDebuggerConnected()) Log.d("No more track found", "condition \n"+ "\n_tracklist.recording == true && _track.terminating == true"+"........ _tracklist=null");
                            	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.RECORDING_HAS_FINISHED, _tracklist));
                                _tracklist.recording = false;
                                _tracklist = null;
                                _track = null;
                                _host = null;
                            }
                            addTrack();
                        }
                    }

                    where = headers.indexOf(DPStringConstants.STR_CONTENT_RANGE);
                    if( where > -1 )
                    {
                        int ender = headers.indexOf(DPStringConstants.STR_LINEFEED,where + 1);
                        if( ender > -1 )
                        {
                            String temp  = headers.substring(where+14, ender);
                            temp = temp.trim();
                            where = temp.indexOf(DPStringConstants.STR_SLASH);
                            if( where > -1 )
                            {
                                found = true;
                                String temp2 = temp.substring(where+1);
                                temp2 = temp2.trim();
                                totalLength = (int) Long.parseLong(temp2);
                                if( totalLength < 0 ) totalLength = UNKNOWN_SIZE;
                                _track.length = (int) totalLength;
                            }
                        }
                    }

                    where = headers.indexOf(DPStringConstants.STR_CONTENT_LENGTH);
                    if( found == false && where > -1 )
                    {
                        totalLength = getHeaderValueInt( headers, DPStringConstants.STR_CONTENT_LENGTH );
                        _track.length = (int) totalLength;
                    }

                    if( _track.guidIndex == null )
                    {
                        _track.mediatype = getHeaderValue( headers, DPStringConstants.STR_CONTENT_TYPE );
                        _track.metadata = getHeaderValue( headers, DPStringConstants.STR_FC_METADATA );
                        _track.artist = getHeaderValue( headers, DPStringConstants.STR_FC_ARTIST );
                        _track.album = getHeaderValue( headers, DPStringConstants.STR_FC_ALBUM );
                        _track.title = getHeaderValue( headers, DPStringConstants.STR_FC_TITLE );
                        _track.starttime = getHeaderValue( headers, DPStringConstants.STR_FC_STARTTIME );
                        _track.imageurl = getHeaderValue( headers, DPStringConstants.STR_FC_COVERART );
                    	if(Debug.isDebuggerConnected()) Log.d("Index", "Setting the GUID of the song  " + _track.guidSong );
                        _track.guidIndex = getHeaderValue( headers, DPStringConstants.STR_FC_MFLID );
                        _track.guidSong = getHeaderValue( headers, DPStringConstants.STR_FC_CACHEID ); // in the future STR_FC_CACHEID
                        _track.length = getHeaderValueInt( headers, DPStringConstants.STR_FC_LENGTH );
                        DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, _track));
                      
                        if(_track.IndexInList>2){
                        	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", _track.guidSong + "..delayed.." +_track.delayed);
                        	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", _track.guidSong + "..listened.." +_track.listened);
                        } 
                        
                        if( getHeaderValue( headers, DPStringConstants.STR_FC_SYNCBYTE ) != null )
                        {
                            _track.synced = true;
                            _track.syncoff = getHeaderValueInt( headers, DPStringConstants.STR_FC_SYNCBYTE );
                            if( _track.syncoff == UNKNOWN_SIZE )
                            {
                                _track.syncoff = 0;
                                _track.synced = false;
                            }
                        }
                        if( _tracklist.stopGuid == null ) _tracklist.stopGuid = _track.guidIndex;
                        //System.err.println( " ***** Adding first track -- " + _track.title );
                    }
                    else if( _tracklist.flycasting == true && _track.redirect == null )
                    {
                        _track.length = getHeaderValueInt( headers, DPStringConstants.STR_FC_LENGTH );
                    }

                    if( _track.redirect == null || _tracklist.podcasting == true )
                    {
                        String newredir = getHeaderValue( headers, DPStringConstants.STR_LOCATION_URL );
                        //System.err.println( " ***** Redirect to -- " + newredir );
                        if( newredir != null ) _track.redirect = newredir;
                    }
                    
                    
                    if( _track != null &&  flybackTerminatingGUID != null && flybackTerminatingGUID.equalsIgnoreCase(_track.guidSong)){
                    	if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message in unknown condition ");
                    }

                    if( getHeaderValue( headers, DPStringConstants.STR_FCN_MFLID ) != null )
                    {
                        String nextguid = getHeaderValue( headers, DPStringConstants.STR_FCN_MFLID );
                        if( nextguid != null  )
                        	if(Debug.isDebuggerConnected()) Log.d("NextGUID", nextguid);

                        if( nextguid != null && _currentindex + 1 < _tracklist.children.size() )
                        {
                            DPXMLTrack next = (DPXMLTrack) _tracklist.children.elementAt(_currentindex + 1);
                            if( next.guidIndex != null && next.guidIndex.equals( nextguid ) )
                            {
                                if( flybackTerminatingGUID != null && next != null  && flybackTerminatingGUID.equalsIgnoreCase(next.guidSong)){
                                	if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message in unknown condition ");
                                }
                            	nextguid = null;
                            }
                        }

                        if( nextguid != null )
                        {
                            String guidTest = getHeaderValue( headers, DPStringConstants.STR_FCN_MFLID );
                            if( _tracklist.stopGuid != null && _tracklist.recording == true && _track.terminating == false && guidTest != null && guidTest.equals( _tracklist.stopGuid ))
                            {
                                _track.terminating = true;
                            }
                            else
                            {
                            	DPXMLTrack temp = new DPXMLTrack();
                                temp.stationid = _tracklist.stationid;
                                temp.mediaurl = _track.mediaurl;
                                temp.bitrate = _track.bitrate;
                                temp.timecode = System.currentTimeMillis();
                                temp.listened = (temp.listened == true) || (_tracklist.recording == false) || ( (_tracklist.recording == true) &&(_currentindex - 1 < currenttrackindex) );
                                temp.current = _track.offset + _track.length;
                                temp.offset = _track.offset + _track.length;
                                temp.expdays = _tracklist.expdays;
                                temp.expplays = _tracklist.expplays;

                                temp.mediatype = getHeaderValue( headers, DPStringConstants.STR_CONTENT_TYPE );
                                temp.metadata = getHeaderValue( headers, DPStringConstants.STR_FCN_METADATA );
                                temp.artist = getHeaderValue( headers, DPStringConstants.STR_FCN_ARTIST );
                                temp.album = getHeaderValue( headers, DPStringConstants.STR_FCN_ALBUM );
                                temp.title = getHeaderValue( headers, DPStringConstants.STR_FCN_TITLE );
                                temp.starttime = getHeaderValue( headers, DPStringConstants.STR_FCN_STARTTIME );
                                temp.imageurl = getHeaderValue( headers, DPStringConstants.STR_FCN_COVERART );
                                temp.guidIndex = getHeaderValue( headers, DPStringConstants.STR_FCN_MFLID );
                                temp.guidSong = getHeaderValue( headers, DPStringConstants.STR_FCN_CACHEID ); // in the future STR_FC_CACHEID
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Adding new track " + _track.guidSong );
                            	temp.length = getHeaderValueInt( headers, DPStringConstants.STR_FCN_LENGTH );
                                if( getHeaderValue( headers, DPStringConstants.STR_FCN_SYNCBYTE ) != null )
                                {
                                    temp.synced = true;
                                    temp.syncoff = getHeaderValueInt( headers, DPStringConstants.STR_FCN_SYNCBYTE );
                                    if( temp.syncoff == UNKNOWN_SIZE ) temp.synced = false;
                                }

                                if( temp.metadata != null && temp.artist == null && temp.title == null )
                                {
                                    temp.artist = _tracklist.station;
                                    temp.title = temp.metadata;
                                }

                                playindex = currenttrackindex;
                              
                                if( _currentindex > playindex )
                                {
                                    temp.delayed = true;
                                }

                                temp.IndexInList = _currentindex + 1;
                              
                                if(_tracklist.children.contains(temp)){
                                  	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", "Duplicate track entry for guid " + temp.guidSong);
                                }
                                else{
                                	_tracklist.AddTrackAt(temp, _currentindex+1);
                                	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_ADDED, temp));
                                }
                                
                       		 if(temp.IndexInList>2){
	                                	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", "Track added to tracklist \n"+ temp.guidSong + "..delayed.." +temp.delayed+ "2ND PLACE");
	                                	if(Debug.isDebuggerConnected()) Log.d("DPTracklist", "Track added to tracklist \n"+ temp.guidSong + "..listened.." +temp.listened);
	                                }                                
                            }
                        }
                    }

                    if( _tracklist.podcasting == true )
                    {
                        where = headers.indexOf( DPStringConstants.STR_RANGE_FAIL );
                        if( where > -1 && where < headers.indexOf( DPStringConstants.STR_LINEFEED ) )
                        {
                            _track.unsupported = true;
                        }
                    }

                    if( _track.redirect != null && ( _track.redirected == false || _tracklist.podcasting == true ) )
                    {
                        where = headers.indexOf( DPStringConstants.STR_REDIRECT1 );
                        if( where == -1 ) where = headers.indexOf( DPStringConstants.STR_REDIRECT2 );
                        if( where > -1 && where < headers.indexOf( DPStringConstants.STR_LINEFEED ) )
                        {
                            String type = getHeaderValue( headers, DPStringConstants.STR_CONTENT_TYPE );
                            if( type != null && !type.equalsIgnoreCase( DPStringConstants.STR_AUDIO_AAC ) && !type.equalsIgnoreCase( DPStringConstants.STR_AUDIO_MPEG ) )
                            {
                                _track.length = 0;
                            }
                            _track.redirecting = true;
                        }
                    }

                    if( _track.metadata != null && _track.artist == null && _track.title == null )
                    {
                        _track.artist = _tracklist.station;
                        _track.title = _track.metadata;
                    }

                    if( _tracklist.shoutcasting == true )
                    {
                        metaint = getHeaderValueInt( headers, DPStringConstants.STR_ICY_METAINT );
                        _track.bitrate = getHeaderValueInt( headers, DPStringConstants.STR_ICY_BITRATE );
                    }
                }

                headers = null;
                return marker;
            }
            catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (624) -- " + e.getMessage()); }
            return marker;
        }

        public void run()	//ConnectionThread
        {
            try
            {
                int toread;
                int offset;
                int readoffset = 0;
                int failures = 0;
                int readlength = 0;
                boolean connerror = false;
                boolean sweeper = false;
                int length = 0;
                int playindex = -1;
                streamLength = 0;
                streamsize = bitrate * 128 * buffersize;
                byte[] data = new byte[READ_CHUNK];
                byte[] parsed = new byte[READ_CHUNK];

                setPriority(MAX_PRIORITY);

                while( true )
                {
                    try
                    {
                        if(DPMemoryStatus.getAvailableExternalMemorySize() < 4000000){
                        	//close = true;
                        }
                        
                        if( _IndexChanged != null ){
                        	_IndexChanged.ChangeDownLoadIndex();
                        	_IndexChanged = null;
                        }
                        
						/**
                         * Lets make the check for flyback at this point.
                         * Get the Track for the Flyback and its index.
                         * Remove all the flybacktracks in the track list from this index onwards.
                         * reset the _currentindex with the index of this track.
                         * And set the _track variable with the new track.
                         */
                        
                        	if(flyback /*&& canSwitchToFlyback */){
                        		
                        		 /*
                                 * Try to get the initial 3rd track in the tracklist 
                                 * i.e. when tracklist was first created
                                 * This track will be when we will move from 
                                 * normal to falyback and vice versa
                                 * 
                                 */
                                
	                        		DPXMLTrack flybackTrack=null;
	                        		int flybackTrackIndex=0;
	                        		 
										DPXMLTrack tempTrack = (DPXMLTrack) _tracklist.children.elementAt(DPManager.flybackURLIndex);
										flybackTrack = tempTrack;
										flybackTrackIndex = DPManager.flybackURLIndex;
									
										  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flyback index is.. "+DPManager.flybackURLIndex);
										  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "_tracklist.children.size() is.. "+_tracklist.children.size());
                        		  
									
										  flybackStarted = true;
			                          		if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flybackStarted is " + flybackStarted);	
		                         		_tracklist.recording = true;
		                         		flybacking = true;
										  
		                				
		                         		int x = flybackTrackIndex;
		                				for(int i=flybackTrackIndex;i<_tracklist.children.size();i++){
		                					if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "i is "+i);
		                					DPXMLTrack t = (DPXMLTrack) DPApplication.Instance()._Tracklist.children.elementAt(x);
		                					if(t.flyback){
		                						_tracklist.children.remove(t);
		                						if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Removed track at position "+i);
		                					}else{
		                						x++;
		                					}
		                				}
		                				
		                        
	                        		  
	                        		  //We have a DPTrackState to keep the state of the track. 
	                        		  //We must change the object to keep DPTrackState in sync.
	                        		  //Also there is no mechanism to send the update track information to the client.
	                        		  //So if we want to resync the client and the server with new track information 
	                        		  //of any track delete that specific track and add it new.
	                        		
		                			DPXMLTrack newTrack = new DPXMLTrack();
	                        		  newTrack.copy(flybackTrack);
	                        		  
	                        		  /*
	                                   *Narinder 02/01/2010 
	                                   * set track listened to false before adding it to tracklist
	                                   * 
	                                   */
	                                  
	                        		 newTrack.listened=false;//16july 2010 it should be true
	                        		  _tracklist.AddTrackAt(newTrack, flybackTrackIndex);
	                        	
	                                  	//if(Debug.isDebuggerConnected()) Log.d("DPTracklist","Track added to  tracklist"+ newTrack.guidSong + "... dalayed.."+ newTrack.delayed+"Flyback"+" at "+flybackTrackIndex);
	                                  	//if(Debug.isDebuggerConnected()) Log.d("DPTracklist","Track added to  tracklist"+ newTrack.guidSong + " ...listened.."+ newTrack.listened+"Flyback"+" at "+flybackTrackIndex);
	                        		  
	                        		  /*
	                        		   * we are marking flybackTerminatingGUID as terminatingTrack.guidSong
	                        		   * because we do not want it to change when higher FB follows lower FB
	                        		   * (i. e. when we do flycast60 or FB120 after FB30) 
	                        		   * 
	                        		   */
	                        		  
	                        		  if(flybackTerminatingGUID==null){
	                        			  flybackTerminatingGUID = terminatingTrack.guidSong;
	                        		  }
	                        		  
	                        		  if(Debug.isDebuggerConnected()) Log.d("flyback", "Going in flybacking with termination GUID as " + flybackTerminatingGUID);
	                        		  if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Going in flybacking ");
	               
	                        		 _track=newTrack;
	                        		 _track.flyback = false;
	                        		 _currentindex = flybackTrackIndex;
	                                 _host = null;
	                                 writer=null;
	                        		 flyback = false;
	                        		 
	                        		 if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "flyback if complete.... ");	 
	                       }                        	
                        	
                       if( close == true ) return;

                        if( failures >= 100 )
                        {
                            close = true;
                            return;
                        }

                        while( ( _tracklist == null && _nexttracklist == null && _recordings.size() == 0 ) || connerror == true )
                        {
                            if( close == true ) return;
                            connerror = false;

                            try {
                            	if( _IndexChanged != null ){
                                	_IndexChanged.ChangeDownLoadIndex();
                                	_IndexChanged = null;
                                }
                              Thread.sleep(250+((int)Math.sqrt(failures)*400));
                                    
                            }
                            catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage()); }
                        }

                        if( _nexttracklist != null )
                        {    
                        	if(Debug.isDebuggerConnected()) Log.d("Index", "Going to download.");
                             buffered = ( _tracklist == _nexttracklist );
                            closefile();
                            cleanup();
                            _tracklist = _nexttracklist;
                            _currentindex = -1;
                            _nexttracklist = null;
                            _track = null;
                            _host = null;

                            /*
                            if( sessionThread != null )
                            {
                                sessionThread.close();
                                sessionThread = null;
                            }
                            //*/

                            if( _nextindex >= 0 && _nextindex < _tracklist.children.size() )
                            {
                            	_track = (DPXMLTrack) _tracklist.children.elementAt(_nextindex);
                                _currentindex = _nextindex;
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "CurrentIndex set to :: " + _currentindex);
                                _nextindex = -1;
                            }

                            if( _tracklist.flycasting == true && _currentindex + 1 < _tracklist.children.size() )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Delayed !!!!!!!!!");
                            	DPXMLTrack temp = (DPXMLTrack) _tracklist.children.elementAt(_currentindex + 1);
                                temp.delayed = false;
                                DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_HAS_UPDATED, temp));
                            }

                            /*
                            if( _tracklist.flycasting == true && _track != null && _tracklist.session != null )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "flycasting and session.  !!!!!!!!!");
                            	String proxy = DPStringConstants.STR_EMPTY;
                                int where = _track.mediaurl.indexOf(DPStringConstants.STR_DOUBLE_SLASH);
                                if( where > -1 )
                                {
                                    int ender = _track.mediaurl.indexOf(DPStringConstants.STR_SLASH, where + 3);
                                    proxy = new String(_track.mediaurl.substring(0, ender));

                                    sessionThread = new SessionThread(_tracklist.session, proxy);
                                    sessionThread.start();
                                }
                            }
                            //*/
                        }

                        if( _tracklist == null && _recordings.size() > 0 )
                        {
                            _tracklist = (DPXMLTracklist) _recordings.elementAt(0);
                            _recordings.removeElementAt(0);
                        }

                        if( _track == null )
                        {
                            if( _currentindex + 1 < _tracklist.children.size() )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "First Condition.");
                                _currentindex++;
                                _track = (DPXMLTrack) _tracklist.children.elementAt(_currentindex);
                              
                            }

                            playindex = currenttrackindex;
                            if( _tracklist.flycasting == true && _currentindex - 1 < playindex )
                            {
                                DPXMLTrack temp = (DPXMLTrack) _tracklist.children.elementAt(_currentindex + 1);
                                if( temp.delayed == true )
                                {
                                    temp.delayed = false;
                                    DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_HAS_UPDATED, temp));
                                }
                            }

                            if( _track == null )
                            {
                                while( _currentindex + 1 >= _tracklist.children.size() )
                                {
                                    if( close == true ) return;
                                    if( _tracklist == null ) continue;

                                    try {
                                        if( _IndexChanged != null ){
                                        	_IndexChanged.ChangeDownLoadIndex();
                                        	_IndexChanged = null;
                                        }
                                    		//if(Debug.isDebuggerConnected()) Log.d("NextGUID", "CurrentIndex Counter is " + (_currentindex + 1) + " and track list count is " + _tracklist.children.size());
                                    		Thread.sleep(1000);
                                    	}
                                    catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage()); }
                                }
                            }

                            if( _track.length == 0 )
                                totalLength = UNKNOWN_SIZE;
                            else
                                totalLength = _track.length;
                        }
                      
                        
                        if(flybacking){
                        	if(_track.guidSong != null && _track.guidSong.equalsIgnoreCase(flybackTerminatingGUID)){
                        		if(Debug.isDebuggerConnected()) Log.d("flyback", "Breaking from the flyback as terminating GUID meet.");
                        		flybacking = false;
                        		flybackStarted = false;
                        		_tracklist.recording = false;
                        		
                        		if(Debug.isDebuggerConnected()) Log.d("CurrentIndex", "Breaking from the flyback as terminating GUID meet....");	
                        	}
                        }

                        if( _track.cached == true )
                        {
                            if( buffered == false )
                            {
                            	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.STREAM_IS_BUFFERED, DPStringConstants.STR_TRACK_BUFFERED));
                                buffered = true;
                            }

                            if( _tracklist.recording == true && _tracklist.stopGuid != null && _track.guidIndex.equals(_tracklist.stopGuid) )
                            {
                                _tracklist.recording = false;
                            }

                            _track = null;
                            _host = null;
                            continue;
                        }

                        if( _host == null )
                        {
                            streamurl = _track.mediaurl;
                            if( _track.redirected == true && _track.redirect != null )
                            {
                                streamurl = _track.redirect;
                            }
                            if( _tracklist.recording == true && _tracklist.stopGuid == null )
                            {
                                streamurl = _tracklist.livemediaurl;
                            }
                            //System.err.println( "STREAM URL ** " + streamurl );
                            //streamurl = "http://10.1.1.201:50004/StreamMedia.aspx?SID=11111111-1111-1111-1111-111111111111&CMD=PLAY_STATION&FILENAMEONLY=TRUE&BITRATE=48&StationName=FLYTUNES%20Test%20AACPlus&PLAYSTREAM=true&EXT=.mp3";
                            //streamurl = "http://209.9.238.10:8008";
                            int where = streamurl.indexOf(DPStringConstants.STR_DOUBLE_SLASH);
                            if( where > -1 )
                            {
                                int ender = streamurl.indexOf(DPStringConstants.STR_SLASH, where + 3);
                                if( ender == -1 )
                                {
                                	_host = new String(streamurl.substring(where+2));
                                	_parm = "/";
                                }
                                else
                                {
                                	_host = new String(streamurl.substring(where+2, ender));
                                	_parm = new String(streamurl.substring(ender));
                                }
                                if( _host.indexOf(DPStringConstants.STR_COLON) == -1 )
                                {
                                    _host += DPStringConstants.STR_PORT80;
                                }
                            }
                        }

                        _track.listened = (_track.listened == true) || (_tracklist.recording == false) || ( (_tracklist.recording == true) && (_currentindex - 1 < currenttrackindex) );

                        playindex = currenttrackindex;
                        sweeper = false;
                        if( _currentindex > 0 )
                        {
                        	DPXMLTrack temp = (DPXMLTrack) _tracklist.children.elementAt(_currentindex - 1);
                            if( temp.seconds < 31 && temp.cached == true )
                            {
                                sweeper = true;
                            }
                        }

                        if(flybacking){
                        	if(_track.guidSong != null && _track.guidSong.equalsIgnoreCase(flybackTerminatingGUID)){
                        		if(Debug.isDebuggerConnected()) Log.d("flyback", "Breaking from the flyback as terminating GUID meet.");
                        		flybacking = false;
                        		flybackStarted = false;
                        	}
                        }
                        /*
                         * Narinder 02/05/2010
                         * 
                         * Let it go inside this loop even in case of flyback
                         * it will break from the loop once it will meet the condition
                         * if( flyback && terminatingTrack != null)
                         *  
                         */
                                               
                            while( _currentindex > playindex + 1 && _tracklist.recording == false && /*_track.buffered*/ sweeper==false)
	                        {
	                        	/*
	                        	 * Narinder 02/04/2010
	                        	 * 
	                        	 * now we will not check that terminating track is cached or not
	                        	 * we can come back and downlaod it again from the point it was left
	                        	 */
	                        	
	                        	if( flybacking && terminatingTrack != null /*&& terminatingTrack.cached */){
	                            	 
	                            	 break;
	                             }
	                            if( close == true ) return;
	                            if( _tracklist == null ) break;
	
	                            try {
	                            	
	                            	if( _IndexChanged != null ){
	                                	_IndexChanged.ChangeDownLoadIndex();
	                                	_IndexChanged = null;
	                                }
	                            	//if(Debug.isDebuggerConnected()) Log.d("NextGUID", "2. CurrentIndex Counter is " + _currentindex + " and playindex count is " + playindex);
	                            	Thread.sleep(1000); 
	                                
	                            }
	                            catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage()); }
	                            playindex = currenttrackindex;
	                        }
                     	if(Debug.isDebuggerConnected()) Log.d("Index", "Woken from the wait condition.");

                        if( _tracklist == null ) continue;

                        if( _track.buffered == true && buffered == false )
                        {
                        	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.STREAM_IS_BUFFERED));
                            buffered = true;
                        }

                        length = 0;
                        streamRead = 0;
                        fileWrote = 0;
                        bufferWrote = 0;
                        readoffset = 0;
                        if( _track.length == 0 && _tracklist.podcasting == false )
                        {
                            streamLength = initial;
                        }
                        else if( _track.length == 0 )
                        {
                            streamLength = podinitial;
                        }
                        else
                        {
                            streamLength = _track.length;
                        }

                        try
                        {
                            //System.err.println("PROXY ******* -- " + conn);
                        	//System.err.println( "ROSCO DOWNLOAD PROGRESS ** " + (_track.current) + " - " + _track.length );
                        	System.err.println( "ROSCO _parm ***** (" + _host + ") -- (" + _currentindex + ") -- " + _parm );
                            //m_app.logError( "_parm ***** (" + _host + ") -- (" + _currentindex + ") -- " + _parm, "ConnectionThread" ); // *** RDS ***
                        	socket = new Socket(GetURL(_host), GetPort(_host));
                        	socket.setSoTimeout(15 * 1000);
                        	is = socket.getInputStream();
                        	os = socket.getOutputStream();
                            
                            StringBuffer request = new StringBuffer(128);
                            request.append(DPStringConstants.STR_GET);
                            request.append(_parm);
                            request.append(DPStringConstants.STR_HTTP_HOST);
                            request.append(_host);
                            if( _tracklist.shoutcasting == true )
                                request.append(DPStringConstants.STR_USER_AGENT_SHOUT);
                            else
                                request.append(DPStringConstants.STR_USER_AGENT_GET);
                            request.append(DPStringConstants.STR_RANGE);
                            if( _track.redirected )
                            {
                                request.append((_track.current - _track.offset));
                                request.append(DPStringConstants.STR_DASH);
                                request.append(streamLength-1);
                                System.err.println( "ROSCO Range Requested ** " + (_track.current - _track.offset) + " - " + (streamLength-1) );
                            }
                            else
                            {
                                request.append(_track.current);
                                request.append(DPStringConstants.STR_DASH);
                                request.append(_track.offset+streamLength-1);
                                System.err.println( "ROSCO Range Requested ** " + (_track.current) + " - " + (_track.offset+streamLength-1) );
                            }
                            request.append(DPStringConstants.STR_DOUBLE_LINEFEED);
                            //System.err.println("GET ******* -- " + request.toString());

                            os.write(request.toString().getBytes());
                            os.flush();
                        }
                        catch (Exception e)
                        {
                            //System.err.println("Connection Error *******");
                            System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (901) -- " + e.getMessage());
                            //application.rotator.resetIndicator();
                            cleanup();
                            failures++;
                            connerror = true;
                            continue;
                        }

                        while( streamRead < streamLength && length > -1 )
                        {
                            if( _IndexChanged != null ){
                            	_IndexChanged.ChangeDownLoadIndex();
                            	_IndexChanged = null;
                            	break;
                            }
                            
                            if( streamRead == 0 && _tracklist.shoutcasting == true )
                            {
                            	try { Thread.sleep(1250); }
                            	catch (Exception e) { e.printStackTrace(); }
                            }
                        	
                        	toread = ((_track.current-_track.offset)+READ_CHUNK>_track.length)?(_track.length-(_track.current-_track.offset)):READ_CHUNK;
                            if( toread < 0 ){
                            	endTrack(true);
                            	break;
                            }
                            if( _track.length == 0 || streamRead == length ) toread = READ_CHUNK;
                            readlength = -1;
                            try
                            {
                            	readlength = is.read(data, readoffset, toread-readoffset);
                                connerror = false;
                            }
                            catch (Exception e)
                            {
                                //System.err.println("Read Error *******");
                                e.printStackTrace(System.out);
                            	System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (922) -- " + e.getMessage());
                            }

                            length = readlength;
                            if( length == -1 )
                            {
                                _host = null;
                            	cleanup();
                                break;
                            }
                            failures = 0;
                            connerror = false;
                            streamRead += length;
                            //System.err.println( " Read : " + toread + " -- " + length + " *** " + _track.current + " - " + _track.length);

                            offset = 0;
                            if( streamRead == length || readoffset != 0 )
                            {
                                int marker = parseHeaders(data, true);
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "After adding the GUID.");
                                readoffset += length;
                                if( marker == -1 )
                                {
                                	if(Debug.isDebuggerConnected()) Log.d("Index", "Marker not found going to continue.");
                                	continue;
                                }
                                readoffset = 0;
                                streamRead -= marker;
                                offset = marker;
                                length -= marker;
                                metatoread = 0;
                                metatilnext = metaint;
                                
                                // workaround if shoutcast only sends headers in first packet
                                if( _tracklist.shoutcasting == true ) streamRead--;
                            }
                            
                            // workaround for shoutcast since we start with a short length and that
                            // is what set the streamLength variable even though the track length is
                            // later grown .. this ensures streamLength grows as well
                            if( _tracklist.shoutcasting == true ) streamLength = _track.length;

                            if( _tracklist.recording == true && _track.terminating == false && _tracklist.stopGuid != null && _track.guidIndex.equals(_tracklist.stopGuid) )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Terninating.");
                            	_track.terminating = true;
                            }

                            if( _track.unsupported == true  )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Unsuported.");
                                break;
                            }

                            if( _track.redirecting )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Redirecting.");
                            	if( _currentindex + 1 < _tracklist.children.size() )
                                {
                                    DPXMLTrack temp = (DPXMLTrack) _tracklist.children.elementAt( _currentindex + 1 );
                                    if( temp.guidIndex != null && _track.guidIndex != null && temp.guidIndex.equals( _track.guidIndex ) )
                                    {
                                        _tracklist.children.removeElementAt( _currentindex );
                                        _track = null;
                                        _host = null;
                                    	if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 1.");
                                    	break;
                                    }
                                }

                                _track.redirecting = false;
                                _track.redirected = true;
                                streamurl = _track.redirect;
                                _host = null;
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 2.");
                            	break;
                            }

                            if( badtrack == true )
                            {
                            	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_WAS_REMOVED, DPStringConstants.STR_TRACK_REMOVED));
                                _tracklist.children.removeElementAt(_currentindex);
                                _currentindex--;
                                _track = null;
                                _host = null;
                                badtrack = false;
                                if( _tracklist.children.size() == 0 )
                                {
                                	if(Debug.isDebuggerConnected()) Log.d("No more track found", "condition \n"+ "\n  _tracklist.children.size() == 0 "+"........ _tracklist=null");
                                    _tracklist = null;
                                    DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACKLIST_EMPTY, DPStringConstants.STR_TRACK_REMOVED));
                                }
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 3.");
                            	break;
                            }
                            
                            /*
                             * Narinder 02/05/2010
                             * 
                             *  Do we need to block the else loop in case of flyback????
                             * 
                             */
                            if(flybacking){
                            	if(_track.guidSong != null && _track.guidSong.equalsIgnoreCase(flybackTerminatingGUID)){
                            		if(Debug.isDebuggerConnected()) Log.d("flyback", "Breaking from the flyback as terminating GUID meet.");
                            		flybacking = false;
                            		flybackStarted = false;
                            	}
                            }
                            else
                            {
                            	if( _currentindex > currenttrackindex + 1 && _tracklist.recording == false && sweeper == false )
                            	{
	                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 5.");
	                                break;
	                            }
                            }

                            if( _track.filename == null || writer == null )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Calling opne file.");
                                openfile(_tracklist.stationid, _track);
                            }
                            if( trackcached == true )
                            {
                                trackcached = false;
                                _track.buffered =  true;
                                if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message added Guid as open file marked it as true " + _track.guidSong);
                                if( buffered == false )
                                {
                                	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.STREAM_IS_BUFFERED, DPStringConstants.STR_TRACK_BUFFERED));
                                    buffered = true;
                                }
                                //nextTrack(); // shouldn't be needed, if a track is cached then proxy should have told us about next track already too
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 6.");
                            	break;
                            }

                            if( _tracklist.shoutcasting == true )
                            {
                                length = parseMetadata( data, offset, parsed, length );
                            	//if(Debug.isDebuggerConnected()) Log.d("Index", "Writing to file 1.");
                            	writer.Write(parsed, 0, length );
                            	bufferMessage();
                            }
                            else
                            {
                            	//if(Debug.isDebuggerConnected()) Log.d("Index", "Writing to file 2.");
                                writer.Write(data, offset, length );
                                bufferMessage();
                            }

                            //if(Debug.isDebuggerConnected()) Log.d("DPManager", "Writing new data -- (" + length + ") -- (" + _track.current + ")" );
                            if( _track.bitrate == 99999 )
                            {
                                for( int i = offset; i < length-2; i++ )
                                {
                                    if( data[i] == (byte)0xFF && ( (data[i+1] == (byte)0xFA) || (data[i+1] == (byte)0xFB) || (data[i+1] == (byte)0xF3) || (data[i+1] == (byte)0xF2) ) )
                                    {
                                        byte bb = (byte) ( ( data[i+2] >> 4 ) & (byte)0x0F );
                                        int index = (int) bb;
                                        byte cc = (byte) ( ( data[i+2] >> 2 ) & (byte)0x03 );
                                        if( cc == 3 ) continue;
                                        if( (data[i+1] == (byte)0xFA) || (data[i+1] == (byte)0xFB) )
                                        {
                                            _track.bitrate = version1layer3[index];
                                        }
                                        else
                                        {
                                            _track.bitrate = version2layer3[index];
                                        }
                                    	if(Debug.isDebuggerConnected()) Log.d("Index", "Storm only break.");
                                        break;
                                    }
                                }
                            }

                            _track.current += length;
                            //System.err.println( "ROSCO -- WROTE DATA " + (_track.current) + " - " + _track.length );
                            //m_app.updateBuffered();
                            if( _track.delayed == true )
                            {
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Track Delayed.");
                            	_track.delayed = false;
                                //m_app.callback(FlyCast.TRACK_HAS_UPDATED, DPStringConstants.STR_TRACK_ADDED);
                                DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.TRACK_HAS_UPDATED, DPStringConstants.STR_TRACK_ADDED));
                            }
                            //System.err.println( " Wrote : " + toread + " -- " + length + " *** " + _track.current + " - " + _track.length);
                            try
                            {
                            if( _track.buffered == false /*&& m_storm == false*/ && (_track.current - _track.offset) >= (_track.bitrate * 128 * 4 ) ) // 4 seconds of audio
                            {
                                writer.Flush();
                                //System.err.println( DPStringConstants.STR_TRACK_BUFFERED );
                                _track.buffered =  true ;
                                //DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _track));
                                //m_app.callback(FlyCast.TRACK_IS_BUFFERED, _track);
                                bufferMessage();
                                if(Debug.isDebuggerConnected()) Log.d("Index", "Buffer Message added Guid " + _track.guidSong);
                                if(Debug.isDebuggerConnected()) Log.d("Index", "Condition for buffer " + (_track.current - _track.offset));
                                if(Debug.isDebuggerConnected()) Log.d("Index", "Condition for buffer " + (_track.bitrate * 128 * 4 ));
                                if( buffered == false )
                                {
                                   
                                	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.STREAM_IS_BUFFERED));
                                    buffered = true;
                                }
                            }

                            if( _track.buffered == false /*&& m_storm == true*/ && (_track.current - _track.offset) >= 58000 ) // Storm boot needs 58000 bytes
                            {
                                writer.Flush();
                                //System.err.println( DPStringConstants.STR_TRACK_BUFFERED );
                                _track.buffered  = true ;
                                //DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _track));
                                //m_app.callback(FlyCast.TRACK_IS_BUFFERED, _track);
                                bufferMessage();
                                if(Debug.isDebuggerConnected()) Log.d("Index", "1 Buffer Message added Guid " + _track.guidSong);
                                if(Debug.isDebuggerConnected()) Log.d("Index", "1 Condition for buffer " + (_track.current - _track.offset));
                                if(Debug.isDebuggerConnected()) Log.d("Index", "1 Condition for buffer " + 58000);
                                if( buffered == false )
                                {
                                    //m_app.callback(FlyCast.STREAM_IS_BUFFERED, null);
                                	DPApplication.Instance().AddMessage(new DPMessage(DPMessageObject.STREAM_IS_BUFFERED));
                                    buffered = true;
                                }
                            }

                            if( ( _track.current - _track.offset ) == _track.length && _track.length != 0 )
                            {
                                //System.err.println( " NEXT TRACK -- " + _track.filename + " -- " + (_track.length + _track.start) );
                                //System.err.println( DPStringConstants.STR_TRACK_CACHED );
                                if( _tracklist.recording == true && _track.terminating == true )
                                {
                                    endTrack(true);
                                    //m_app.callback(FlyCast.RECORDING_HAS_FINISHED, _tracklist);
                                    _tracklist.recording = false;
                                }
                                else
                                {
                                    endTrack(true);
                                }

                                /*if( _tracklist.continuing == false )
                                {
                                    _tracklist = null;
                                }*/
                                //if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 8.");
                            	break;
                            }

                            if( _track == null )
                            	{
                            		if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 9.");
                            		break;
                            	}
                            if( _tracklist == null )                            	{
                        		if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 10.");
                        		break;
                        	};
                            if( _nexttracklist != null )                             	{
                        		if(Debug.isDebuggerConnected()) Log.d("Index", "Break Condition 11.");
                        		break;
                        	};
                            }
                            catch(Exception ex){
                            	if(Debug.isDebuggerConnected()) Log.d("Index", "Inner Exception ex :: " + ex.toString());
                            }
                        	//ajay
                        }

                        cleanup();
                    }
                    catch (Exception e)
                    {
                    	if(Debug.isDebuggerConnected()) Log.d("Index", "Exception :: " + e.getMessage());
                    	//if(Debug.isDebuggerConnected()) Log.d("Index", "Extra EXception info :: " + e.);
                    	cleanup();
                        e.printStackTrace(System.out);
                        System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (1135) -- " + e.getMessage());
                    }
                }
            }
            catch (Exception e)
            {
                cleanup();
                System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (1142) -- " + e.getMessage());
            }
            finally
            {
            	_IsReusable = false;
            }
        }	


        private boolean _IsReusable = true;
        private boolean IsReusable(){
        	return _IsReusable;
        }


        String GetURL(String host)
        {
        	return host.substring(0, host.indexOf(":"));
        }

        int GetPort(String host)
        {
        	return Integer.parseInt(host.substring(host.indexOf(":") + 1, host.length()));
        }
    }	//private class ConnectionThread extends Thread

    //------------------------------------------------
    // END ConnectionThread
    //------------------------------------------------

    //--------------------
    //BEGIN SessionThread
    //--------------------
    /*
    private class SessionThread extends Thread
    {
        public String m_sid = null;
        public String m_proxy = null;
        public volatile boolean close = false;

        public SessionThread(String sid, String proxy)
        {
            m_sid = sid;
            m_proxy = proxy;
        }

        public synchronized void close()
        {
            close = true;

            try { if( menuConn != null ) menuConn.close(); }
            catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (1165) -- " + e.getMessage()); }
            menuConn = null;
        }


        public void run()
        {

            setPriority(MIN_PRIORITY);
            while( !close )
            {
                try
                {
                    String url = m_proxy + DPStringConstants.STR_UPDATE_SESSION + m_sid;
                    String newurl = url + DPStringConstants.STR_DEVICE_SIDE + getConnectionString(connerror, false);
                    //System.err.println("Session Renew ******* -- " + newurl);
                    menuConn = (HttpConnection) Connector.open(newurl);
                    menuConn.setRequestMethod(HttpConnection.GET);
                    int code = menuConn.getResponseCode();
                    menuConn.close();
                    menuConn = null;
                }
                catch (Exception e)
                {
                    connerror = true;
                    menuConn = null;
                    System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (1191) -- " + e.getMessage());
                }

                try { Thread.sleep(120000); }
                catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage()); }
            }
        }
    }	//private class SessionThread extends Thread
    //-------------------
    //END SessionThread
    //-------------------
    //*/
}

interface DownloadIndexChanged{
	void ChangeDownLoadIndex();
}
