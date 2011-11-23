package fm.flycast;

import java.util.Hashtable;
import java.util.Vector;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;


public class DPXMLTracklist extends DPXMLObject
{
    public String station = null;
    public String imageurl = null;
    public String livemediaurl = null;
    public String stopGuid = null;
    public String session = null;
    public int stationid = 0;
    public int bitrate = 99999;
    public int startindex = 0;
    public boolean autoplay = true;
    public boolean continuing = false;
    public boolean shuffleable = false;
    public boolean deleteable = true;
    public boolean podcasting = false;
    public boolean shoutcasting = false;
    public boolean flycasting = false;
    public boolean flybacking = false;
    public boolean recording = false;
    public boolean offline = false;
    public boolean throwaway = false;
    public boolean shuffled = false;
    public boolean autohide = false;
    public boolean autoshuffle = false;
    public boolean users = false;
    public int expdays = -1;
    public int expplays = -1;
    public String albumfile = null;
    public long timecode = 0;
	public String adbannerzone = null;
	public String adbannerwidth = null;
	public String adbannerheight = null;
	public String adbannerfreq = null;
	public String adprerollzone = null;
	public String adprerollwidth = null;
	public String adprerollheight = null;
	public String adprerollfreq = null;
	public String adpopupzone = null;
	public String adpopupwidth = null;
	public String adpopupheight = null;
	public String adpopupfreq = null;
	public String adinterzone = null;
	public String adinterwidth = null;
	public String adinterheight = null;
	public String adinterfreq = null;
	public String adsignupzone = null;
	public String adsignupwidth = null;
	public String adsignupheight = null;
	public String adsignupfreq = null;

    DPXMLTracklist()
    {
        type = DPXMLObject.TRACKLIST;
        children = new Vector<DPXMLObject>();
    }

    public void copy(DPXMLTracklist input)
    {
        station      = input.station;
        imageurl     = input.imageurl;
        livemediaurl = input.livemediaurl;
        stopGuid     = input.stopGuid;
        session      = input.session;
        stationid    = input.stationid;
        bitrate      = input.bitrate;
        startindex   = input.startindex;
        autoplay     = input.autoplay;
        continuing   = input.continuing;
        shuffleable  = input.shuffleable;
        deleteable   = input.deleteable;
        podcasting   = input.podcasting;
        shoutcasting = input.shoutcasting;
        flycasting   = input.flycasting;
        flybacking   = input.flybacking;
        recording    = input.recording;
        offline      = input.offline;
        throwaway    = input.throwaway;
        shuffled     = input.shuffled;
        autohide     = input.autohide;
        autoshuffle  = input.autoshuffle;
        users        = input.users;
        expdays      = input.expdays;
        expplays     = input.expplays;
        albumfile    = input.albumfile;
        timecode     = input.timecode;
    }
    
    
    /**
     * The list will be initialized only when we have atleast
     * one track that has been buffered.
     */
    public void Intialized(){
    	while(true){
    		int size = children.size();
    		for(int i = 0; i < size; i++){
    			DPXMLTrack t = (DPXMLTrack)children.elementAt(i);
    			if( t.flyback)
    				continue;
    			
				DPTrackState dts = DPApplication.Instance().getTrackState(t);
				if( dts.GetTrackState() == 3 )
					return;
    		}
    		
    		try{
    			Thread.sleep(1000);
    		}
    		catch(Exception ex){
    			
    		}
    	}
    }
    
    
    Hashtable<String, DPXMLTrack> _Debug = new Hashtable<String, DPXMLTrack>();
    
    public void AddTrack(DPXMLTrack t){
    	
    }
    
    public void AddTrackAt(DPXMLTrack t, int pos){
    	children.insertElementAt(t, pos);
    	try
    	{
	    	if( t.guidSong != null && t.guidSong.trim() != ""){
		    	if( _Debug.contains(t.guidSong) ){
		    		if(Debug.isDebuggerConnected()) Log.d("_Debug", "Duplicate track entry for guid " + t.guidSong);
		    	}
	    	}
	    	else 
	    	{
	    		if(Debug.isDebuggerConnected()) Log.d("_Debug", "GUID not known.");
	    	}
	    	if( t.guidSong != null && t.guidSong.trim() != "")
	    		if(Debug.isDebuggerConnected()) Log.d("_Debug", "Adding track with guid " + t.guidSong + " at index " + pos);
	    	else 
	    		_Debug.put(t.guidSong, t);
    	}
    	catch(Exception ex){
    	}
    }
}









