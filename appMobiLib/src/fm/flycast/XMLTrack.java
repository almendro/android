package fm.flycast;
import android.graphics.drawable.Drawable;

public class XMLTrack extends XMLObject
{
    public String artist = null;
    public String title = null;
    public String album = null;
    public String metadata = null;
    public String imageurl = null;
    public String mediaurl = null;
    public String redirect = null;
    public String mediatype = null;
    public String starttime = null;
    public String guidIndex = null;
    public String guidSong = null;
    public String adurl = null;	//BH_09-02-09
    public String addart = null;//BH_09-02-09
    public long timecode = 0;
    public int offset = 0;
    public int length = 0;
    public int current = 0;
    public int start = 0;
    public int syncoff = 0;     //BH_09-02-09
    public int bitrate = 99999;
    public int seconds = 0;
    public int stationid = 0;
    public int expdays = -1;		//BH_09-02-09
    public int expplays = -1;		//BH_09-02-09
    public int numplay = 0;			//BH_09-02-09
    public boolean clickAd = false;	//BH_09-02-09
    public boolean audioAd = false;	//BH_09-02-09
    public boolean reloadAd = false;//BH_09-02-09
    public boolean buffered = false;	//***
    public boolean cached = false;		//***
    public boolean covered = false;
    public boolean playing = false;		//***
    public boolean flyback = false;
    public boolean delayed = false;
    public boolean finished = false;	//***
    public boolean deleted = false;
    public boolean listened = false;
    public boolean played = false;	//BH_09-02-09
    public boolean flush = false;	//BH_09-02-09
    public boolean redirecting = false;
    public boolean terminating = false;
    public boolean redirected = false;
    public boolean unsupported = false;	//BH_09-02-09
    public boolean synced = false;		//BH_09-02-09
    public Drawable original = null;	//was Bitmap
    public Drawable scaled = null;		//was Bitmap
    public Drawable live = null;		//was Bitmap
    public String albumfile = null;
    public String filename = null;

    XMLTrack()
    {
        type = XMLObject.TRACK;
        children = null;
    }

    public void copy(XMLTrack input)
    {
        artist      = input.artist;
        title       = input.title;
        album       = input.album;
        metadata    = input.metadata;
        imageurl    = input.imageurl;
        mediaurl    = input.mediaurl;
        redirect    = input.redirect;
        mediatype   = input.mediatype;
        starttime   = input.starttime;
        guidIndex   = input.guidIndex;
        guidSong    = input.guidSong;
        adurl       = input.adurl;
        addart      = input.addart;
        timecode    = input.timecode;
        offset      = input.offset;
        length      = input.length;
        current     = input.current;
        start       = input.start;
        bitrate     = input.bitrate;
        seconds     = input.seconds;	//BH_09-02-09
        stationid   = input.stationid;	//BH_09-02-09
        expdays     = input.expdays;	//BH_09-02-09
        expplays    = input.expplays;	//BH_09-02-09
        numplay     = input.numplay;	//BH_09-02-09
        clickAd     = input.clickAd;	//BH_09-02-09
        audioAd     = input.audioAd;	//BH_09-02-09
        reloadAd    = input.reloadAd;	//BH_09-02-09
        buffered    = input.buffered;
        cached      = input.cached;
        covered     = input.covered;
        playing     = input.playing;
        flyback     = input.flyback;
        delayed     = input.delayed;
        finished    = input.finished;
        listened    = input.listened;	//BH_09-02-09
        played      = input.played;		//BH_09-02-09
        flush       = input.flush;		//BH_09-02-09
        redirecting = input.redirecting;
        terminating = input.terminating;//BH_09-02-09
        redirected  = input.redirected;
        unsupported = input.unsupported;//BH_09-02-09
        original    = input.original;
        scaled      = input.scaled;
        live        = input.live;
        albumfile   = input.albumfile;
        filename    = input.filename;
    }
}
