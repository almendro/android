package fm.flycast;
import java.util.Vector;

import android.graphics.drawable.Drawable;

public class XMLTracklist extends XMLObject
{
    public String station = null;
    public String imageurl = null;
    public String livemediaurl = null;
    public String stopGuid = null;
    public String session = null;	//BH_09-02-09
    public int stationid = 0;
    public int bitrate = 99999;
    public int startindex = 0;
    public boolean autoplay = true;
    public boolean continuing = false;
    public boolean shuffleable = false;
    public boolean deleteable = true;	//BH_09-02-09
    public boolean podcasting = false;
    public boolean shoutcasting = false;
    public boolean flycasting = false;
    public boolean flybacking = false;	//BH_09-02-09
    public boolean recording = false;
    public boolean offline = false;
    public boolean throwaway = false;	//BH_09-02-09
    public boolean shuffled = false;
    public boolean autohide = false;	//BH_09-02-09
    public boolean autoshuffle = false;	//BH_09-02-09
    public boolean users = false;
    public int expdays = -1;	//BH_09-02-09
    public int expplays = -1;	//BH_09-02-09
    public Drawable original = null;	//was Bitmap
    public Drawable scaled = null;		//was Bitmap
    public Drawable live = null;		//was Bitmap
    public Drawable albumfile = null;
    public long timecode = 0;

    XMLTracklist()
    {
        type = XMLObject.TRACKLIST;
        children = new Vector<XMLObject>();
    }

    public void copy(XMLTracklist input)
    {
        station      = input.station;
        imageurl     = input.imageurl;
        livemediaurl = input.livemediaurl;	//BH_09-02-09
        stopGuid     = input.stopGuid;		//BH_09-02-09
        session      = input.session;		//BH_09-02-09
        stationid    = input.stationid;
        bitrate      = input.bitrate;
        startindex   = input.startindex;
        autoplay     = input.autoplay;
        continuing   = input.continuing;
        shuffleable  = input.shuffleable;
        deleteable   = input.deleteable;	//BH_09-02-09
        podcasting   = input.podcasting;
        shoutcasting = input.shoutcasting;
        flycasting   = input.flycasting;
        flybacking   = input.flybacking;	//BH_09-02-09
        recording    = input.recording;		//BH_09-02-09
        offline      = input.offline;
        throwaway    = input.throwaway;		//BH_09-02-09
        shuffled     = input.shuffled;
        autohide     = input.autohide;		//BH_09-02-09
        autoshuffle  = input.autoshuffle;	//BH_09-02-09
        users        = input.users;			//BH_09-02-09
        expdays      = input.expdays;		//BH_09-02-09
        expplays     = input.expplays;		//BH_09-02-09
        original     = input.original;
        scaled       = input.scaled;
        live         = input.live;
        albumfile    = input.albumfile;
        timecode     = input.timecode;
    }
}