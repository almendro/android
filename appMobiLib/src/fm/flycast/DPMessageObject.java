package fm.flycast;

public class DPMessageObject 
{
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
    
    public static int SDCARD_IS_NOT_PRESENT=18;
    public static int SDCARD_IS_FULL=19;
 
    
    

    public int type;
}
