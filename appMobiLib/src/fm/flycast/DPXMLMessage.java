package fm.flycast;


public class DPXMLMessage  extends DPXMLObject
{
	//Message Types
    public static int NONE = 0;
    public static int RETRYING_CONNECTION = 1;
    public static int TRACK_IS_CACHED = 2;
    public static int TRACK_IS_PLAYING = 3;
    public static int TRACK_IS_RECORDING = 4;
    public static int TRACK_IS_STARTING = 5;
    public static int TRACK_IS_BUFFERED = 6;
    public static int TRACK_IS_COVERED = 7;
    public static int TRACK_IS_PAUSED = 8;
    public static int TRACK_IS_ENDED = 9;
    public static int TRACK_WAS_REMOVED = 10;
    public static int TRACK_HAS_UPDATED = 11;
    public static int TRACK_WAS_ADDED = 12;
    public static int TRACKLIST_EMPTY = 13;
    public static int RECORDING_HAS_STARTED = 14;
    public static int RECORDING_HAS_FINISHED = 15;
    public static int STREAM_IS_BUFFERED = 16;	
    public static int EXCEPTION = 17;
    
    public static int STR_TRACKLIST  = 100; 	//I am treating a Tracklist as a message     
	
    public String name = null;		//Message Name
    public int type = 0;			//Message Type
    public String guidSong = null;
	public DPXMLTracklist tracklist = null;
	public DPXMLTrack track = null;
	
	//Exception support:
	public String ErrorMessage = null;
	public String ErrorCode = null;
	public String Request = null;
	
	DPXMLMessage() {
		type = DPXMLObject.DPMESSAGE;
		children = null;
	}

}