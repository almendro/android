package fm.flycast;

import android.graphics.drawable.Drawable;

public class DPXMLTrack extends DPXMLObject
{	

	/*
	 *Narinder 1/22/2010
	 *totalBytesSent checks the total number of bytes sent to client.
	 *this also helps in determining whether a track has finished not
	 *This is specially helpful in case of live tracks as we are not aware of
	 *track length till the track has finished downloading.
	 *
	 */
	
	public int totalBytesSent = 0;
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
    public String adurl = null;
    public String addart = null;
    public long timecode = 0;
    public int offset = 0;
    public int length = 0;
    public int current = 0;
    public int start = 0;
    public int syncoff = 0;
    public int bitrate = 99999;
    public int seconds = 0;
    public int stationid = 0;
    public int expdays = -1;
    public int expplays = -1;
    public int numplay = 0;
    public boolean clickAd = false;
    public boolean audioAd = false;
    public boolean reloadAd = false;
    public boolean buffered = false;
    public boolean cached = false;
    public boolean covered = false;
    public boolean playing = false;
    public boolean flyback = false;
    public boolean delayed = false;
    public boolean finished = false;
    public boolean listened = false;
    public boolean played = false;
    public boolean flush = false;
    public boolean redirecting = false;
    public boolean terminating = false;
    public boolean redirected = false;
    public boolean unsupported = false;
    public boolean synced = false;
    public String albumfile = null;
    public String filename = null;
    public Drawable imageoriginal = null;	//BB was Bitmap
    public boolean imageoriginaldownloaded = false; //true if final image has been downloaded  
//    public Drawable imagescaled = null;	//BB was Bitmap
//    public Drawable imagelive = null;		//BB was Bitmap

    public int IndexInList = 0; 
    
    DPXMLTrack()
    {
        type = DPXMLObject.TRACK;
        children = null;
    }


    public void copy(DPXMLTrack input)
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
        seconds     = input.seconds;
        stationid   = input.stationid;
        expdays     = input.expdays;
        expplays    = input.expplays;
        numplay     = input.numplay;
        clickAd     = input.clickAd;
        audioAd     = input.audioAd;
        reloadAd    = input.reloadAd;
        buffered 	= input.buffered;
        cached      = input.cached;
        covered     = input.covered;
        playing     = input.playing;
        flyback     = input.flyback;
        delayed     = input.delayed;
        finished    = input.finished;
        listened    = input.listened;
        played      = input.played;
        flush       = input.flush;
        redirecting = input.redirecting;
        terminating = input.terminating;
        redirected  = input.redirected;
        unsupported = input.unsupported;
        albumfile   = input.albumfile;
        filename    = input.filename;
        IndexInList = input.IndexInList;
        totalBytesSent=input.totalBytesSent;
    }
}
