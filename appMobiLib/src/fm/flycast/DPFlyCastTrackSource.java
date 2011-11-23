package fm.flycast;

import java.io.IOException;
import java.io.InputStream;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;


public class DPFlyCastTrackSource
{
    public  static final int READ_CHUNK = 32768;
    public  static final int PACKET_SIZE = 2048 * 256;
    public  int bitrate = 128;
    public  int bufferseconds = 64;
    public  int streamsize = bitrate * (128 * bufferseconds);
    public  int buffersize = bitrate * (128 * 1);
    public  boolean blockread = false;

    public  int[] version1layer3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    public  int[] version2layer1 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0};
    public  int[] version2layer3 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0};

    public  int[] version1 = {44100, 48000, 32000, 0};
    public  int[] version2 = {22050, 24000, 16000, 0};

    public  DPXMLTrack sourcetrack;
    public  volatile boolean _stop = false;
    public  byte[] currentBuffer;
    public  byte[] syncdata = new byte[8194];


    private volatile int _leftToRead = 0;
    private volatile int readpos = 0;
    public  volatile FileConnection backingfileReader = null;
    private volatile InputStream inputFileStream = null;
    private volatile int sizeOnDisk = 0;
    public DPXMLTrack currenttrack;
    public boolean shoutcasting;

    public static int OFFSET = 90000;
    /*
     * Some stations gives error if we try to use the normal readpos.
     * Add the offset in those cases to make it run.
     */
    public static boolean AddOffset(){
    	String sid = DPApplication.Instance().getCurrentStation();
    	if( sid != null  ){
    		sid = sid.trim();
    		if( sid.equalsIgnoreCase(""))
    			return false;

    		if( sid.equalsIgnoreCase("223354"))
    			return true;
    	}

    	return false;
    }

    public DPFlyCastTrackSource(DPXMLTrack track, boolean shoutcast)
    {
    	currenttrack = track;
    	shoutcasting = shoutcast;

        if (track.mediatype == null)
        {
            track.mediatype = DPStringConstants.STR_AUDIO_MPEG;
        }

        Init();
    }

    public long seek(long where) throws IOException
        {
            //System.err.println("************** SEEK TO ************** " + where );
            if (where + currenttrack.start + currenttrack.syncoff < sizeOnDisk)
            {
                readpos = (int)where + currenttrack.start + currenttrack.syncoff;
            }
            else
            {
                //System.err.println("************** ADJUSTING SEEK, PAST END OF BACKING FILE ************** " + where );
                readpos = sizeOnDisk - 4096 + currenttrack.start + currenttrack.syncoff;
                if( sizeOnDisk == 0 )
                    readpos = currenttrack.start + currenttrack.syncoff;
            }

            //System.err.println("************** readpos ************** " + readpos + " of size " + sizeOnDisk + " for track -- " + currenttrack.title );
            return (readpos - currenttrack.start - currenttrack.syncoff);
        }

        public long tell()
        {
            //System.err.println("************** TELL ************** " + (readpos - currenttrack.start - currenttrack.syncoff) );
            return readpos - currenttrack.start - currenttrack.syncoff;
        }

        public  String lastfile;
        public int getBytesRemaingOnDisk()
        {
            try
            {
               //System.err.println("FCTSource -- getBytesRemaingOnDisk -- " + backingfileReader.fileSize() + " -- " + currenttrack.length + " -- " + readpos);
               return (int)backingfileReader.fileSize() - readpos;
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        public int getSecondsAvailable()
        {
            try
            {
                return getBytesRemaingOnDisk()/(currenttrack.bitrate * 128);
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        public int getSecondsRemaining()
        {
            try
            {
                //System.err.println("FCTSource -- getSecondsRemaining -- " + currenttrack.length + " -- " + currenttrack.start + " -- " + currenttrack.syncoff);
                return (currenttrack.length - currenttrack.start - currenttrack.syncoff - readpos)/(currenttrack.bitrate * 128);
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        //BH_12-02-09 added to support PlayTrack "RESUME MODE"
        public int getSecondsRemaining(int mOffset)
        {
            try
            {
                //System.err.println("FCTSource -- getSecondsRemaining -- " + currenttrack.length + " -- " + currenttrack.start + " -- " + currenttrack.syncoff);
                return (currenttrack.length - currenttrack.start - currenttrack.syncoff - mOffset)/(currenttrack.bitrate * 128);
            }
            catch (Exception e)
            {
                return 0;
            }
        }



        /**
         * Cause the current thread to block till either we have
         *  -- 6 secs of data to play
         *  -- Or track has finished and it has less then 6 secs to play.
         *
         *
         */
        public void Block(){
    		if(Debug.isDebuggerConnected()) Log.d("BLock", "Block entered for track " + currenttrack.guidSong);
        	while( true ){
        		int sixSecondBytes = currenttrack.bitrate * 128 * 10;
        		//Seconds of buffer we have to this point.
        		int rSeconds = getSecondsAvailableAfterReadPos();

        		// Should never be negative.. Just a safety check.
        		if( rSeconds <= 0 )
        			break;

        		if(Debug.isDebuggerConnected()) Log.d("BLock", "Song buffered when we resumed for playresume for " + rSeconds + " seconds");

        		//Do we have 6 seconds of track buffered.
        		if( rSeconds < 6  ){//No
        			//Has track finished?
        			//Readpos includes track.start and track.syncoff in it. We must account for that.
        			int remainingTrackLength = ( currenttrack.length + currenttrack.start + currenttrack.syncoff ) - readpos;
            		//calculate for debug purpose only.
        			int d = (remainingTrackLength)/(currenttrack.bitrate * 128);
        			if(Debug.isDebuggerConnected()) Log.d("BLock", "Seconds of song left after the last play is " + d);

        			if( remainingTrackLength < sixSecondBytes )
        			{
            			//Track has finished.
        				if(Debug.isDebuggerConnected()) Log.d("BLock", "Finished Blocking!!!!!");
        				break;
        			}

        			//Loop on till we have six seconds of data download.
        			try{
            			if(Debug.isDebuggerConnected()) Log.d("BLock", "Sleeping for block.....");
        				Thread.sleep(500);
        			}catch(Exception ex){}
        			continue;
        		}
        		break;
        	}
        }

        /**
         * Returns the no of seconds for which track can be played from the currently
         * downloaded song from the position till where it has already been played.
         * Lets say total bytes downloaded till now is X secs.
         * We have already played Y secs. This function will then return (X - Z).
         * @return
         */
        public int getSecondsAvailableAfterReadPos()
        {
            try
            {
                //Remember that the file size has everthing included in it.
            	//Track has track start + track sync + actuall track
            	//Readpos included track start and track sync. So effectively readpos is current pointer in the file from we will
            	//need the next bytes.
                return ((int)backingfileReader.fileSize() - readpos)/(currenttrack.bitrate * 128);
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        public int getSecondsAvailableOnDisk()
        {
            try
            {
                //System.err.println("FCTSource -- getSecondsAvailableOnDisk -- " + backingfileReader.fileSize() + " -- " + currenttrack.start + " -- " + currenttrack.syncoff);
                return ((int)backingfileReader.fileSize() - currenttrack.start - currenttrack.syncoff)/(currenttrack.bitrate * 128);
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        /**
         * Return the media type for this track.
         *
         * @return
         */
        public String MediaType(){
        	if( currenttrack == null ) return null;
        	return currenttrack.mediatype;        
        }
        
        /**
         * Return the bytes which will be returned.
         *
         * @return
         */
        public int ContentLength(){

		   //*
           //-- BEGIN Version of 01-04-10 ------
		   if(Debug.isDebuggerConnected()) Log.d("PlayStationRemote", "ContentLength() currenttrack.length=" + currenttrack.length);
		   if( currenttrack.length > 0 && currenttrack.length != 999999999 )
		   {
			   //System.err.println("FCTSource -- REQUESTED CONTENT LENGTH is " + (currenttrack.length - currenttrack.syncoff));
			   return currenttrack.length - currenttrack.syncoff;
		   }
		   else
		   {
			   if( currenttrack.bitrate == 99999 )
				   return 117964800;
			   else
				   return (currenttrack.bitrate * 128 * 60 * 15);
		   }
           //-- END Version of 01-04-10 ------
           //*/
        }

	    private int taperOff = 500;
        public boolean HasMoreContent(){
        
    		//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "currenttrack.length is " +   currenttrack.length);
        	//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "currenttrack.syncoff is  " + currenttrack.syncoff);
        	//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "currenttrack.start is  " + currenttrack.start);
        	
    		//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "content length is  " + (currenttrack.length - currenttrack.syncoff));
    		//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent","read position for  "+currenttrack.guidSong + "is " + (readpos - currenttrack.start-currenttrack.syncoff));
    		//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "bytes Sent are ....." + currenttrack.totalBytesSent);
    		//if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "currenttrack.cached is ....." + currenttrack.cached);
    		
        	// RDS -- This code is kinda untested .. we start shoutcast with a small
        	//        track to make it start quicker but then keep adjusting it larger
        	//        as we play .. there is only ever one track
    		if( shoutcasting == true )
    		{
    			if(currenttrack.length-readpos < (3 * 60 * 128 * currenttrack.bitrate) )
    				currenttrack.length += (10 * 60 * 128 * currenttrack.bitrate);    			
    		}
    		
    		/*
    		 * using taperOff it will sometime because syncOff may be more than 500
    		 * so replace following with syncOff
    		 * 
    		 */
        	if ((readpos - currenttrack.start-currenttrack.syncoff >= currenttrack.length - taperOff ) && currenttrack.cached ){
        		currenttrack.finished = true;
        		if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", " 1st condition ...false");
        		return false;
        	}
        	
        	if(currenttrack.cached && ( currenttrack.totalBytesSent >= currenttrack.length- currenttrack.syncoff)){
        		currenttrack.finished = true;
        		if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", " 2nd condition ...false");
        		return false;        		
        	}
        	
        	if(currenttrack.cached && ( currenttrack.totalBytesSent >= currenttrack.length- currenttrack.syncoff-taperOff)){
        		currenttrack.finished = true;
        		if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", " 3rd condition ...false");
        		return false;        		
        	}
        	if ((readpos - currenttrack.start-currenttrack.syncoff >= currenttrack.length -  currenttrack.start-currenttrack.syncoff ) && currenttrack.cached ){
        		currenttrack.finished = true;
        		if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", " 4th condition ...false");
        		return false;
        	}        	
        	
        	if(Debug.isDebuggerConnected()) Log.d("HasMoreContent", "  "+ "true");
        	return true;
        }

        public int read(byte[] b, int off, int len) throws IOException
        {
            while( blockread == true )
            {
                try { Thread.sleep(750); }
                catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(323) -- " + e.getMessage()); }
            }

            if (lastfile == null)
            {
                lastfile = currenttrack.filename;
            }

            if (lastfile != currenttrack.filename)
            {
                //System.err.println("FCTSource -- !!!!!!!!!!SWITCHED FILES FROM " + lastfile + " TO " + currenttrack.filename);
                lastfile = currenttrack.filename;
            }

            //if(Debug.isDebuggerConnected()) Log.d("DPFlyCastTrackSource", "ROSCO -- Requesting Read " + len + " at " + readpos + " of " + sizeOnDisk + " with " + _leftToRead + " left");
            //if( application != null ) application.logError( "FCTSource -- Requesting Read " + len + " at " + readpos + " of " + sizeOnDisk + " with " + _leftToRead + " left", "read" ); // *** RDS ***

            try {
            	//Thread.sleep(50);
            }
            catch (Exception e) {
            	System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(323) -- " + e.getMessage());
            }

            try
            {
                if (readpos - currenttrack.start >= currenttrack.length)
                {
                    //System.err.println("FCTSource -- FINISHED READING FILE - DEBUG");
                    //System.err.println( "***** -- End of track file in TRACKSOURCE -- " + currenttrack.title );
                    currenttrack.finished = true;
                    return -1;
                }
            }
            catch (Exception e)
            {
                System.err.println("FCTSource -- ERROR OPENING BACKING FILE: " + DPStringConstants.STR_EXCEPTION + " -- FCTSource(414) -- " + e.getMessage());
                inputFileStream = null;
                backingfileReader = null;
            }

            int readLength = ( len > PACKET_SIZE ) ? PACKET_SIZE : len;
            int length = 0;

            readLength = ( readpos + readLength > currenttrack.length + currenttrack.start + currenttrack.syncoff) ? currenttrack.length + currenttrack.start + currenttrack.syncoff - readpos: readLength;

            while( true && !_stop )
            {
                //if(Debug.isDebuggerConnected()) Log.d("DPFlyCastTrackSource", "ROSCO -- Requesting Read " + len + " at " + readpos + " of " + sizeOnDisk + " with " + _leftToRead + " left");
                if (readpos - currenttrack.start - currenttrack.syncoff >= currenttrack.length)
                {
                    //System.err.println("FCTSource -- FINISHED READING FILE bytes read = " + currenttrack.length);
                }

                if(_leftToRead  < readLength && _leftToRead > 0 )
                {
                    readLength = _leftToRead;
                }

                //System.err.println("FCTSource -- adjusted readlength to " + readLength);
                //System.err.println("FCTSource -- BEFORE: Filesize is " + currenttrack.length + ", current size is " + sizeOnDisk + ", read_offset, " + currenttrack.start + ", readpos = " + readpos + " with " + _leftToRead + " remaining on this pass.");

                if( _leftToRead > 0)
                {
                    try
                    {
                        //System.err.println( "***** -- Reading data from track file in TRACKSOURCE -- " + readLength );
                        length = inputFileStream.read(b, off, readLength);
                        //if(Debug.isDebuggerConnected()) Log.d("DPFlyCastTrackSource", "***** -- Reading data from track file in TRACKSOURCE -- " + readLength);
                    }
                    catch (IOException e)
                    {
                        System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(450) -- " + e.getMessage());
                        //System.err.println("FCTSource -- FAILED to read from backing file " + DPStringConstants.STR_EXCEPTION + e.getMessage());
                    }

                    /* DEBUG code -- keep
                    if( length == 10 )
                    {
                        System.err.println("FCTSource -- track name -- " + currenttrack.title + " -- guid -- " + currenttrack.guidIndex);
                        System.err.println("FCTSource -- readpos -- " + readpos + " -- start -- " + currenttrack.start + " -- sync -- " + currenttrack.syncoff);
                        System.err.println("FCTSource -- byte 0 ** " + b[off+0]);
                        System.err.println("FCTSource -- byte 1 ** " + b[off+1]);
                        System.err.println("FCTSource -- byte 2 ** " + b[off+2]);
                        System.err.println("FCTSource -- byte 3 ** " + b[off+3]);
                        System.err.println("FCTSource -- byte 4 ** " + b[off+4]);
                        System.err.println("FCTSource -- byte 5 ** " + b[off+5]);
                        System.err.println("FCTSource -- byte 6 ** " + b[off+6]);
                        System.err.println("FCTSource -- byte 7 ** " + b[off+7]);
                        System.err.println("FCTSource -- byte 8 ** " + b[off+8]);
                        System.err.println("FCTSource -- byte 9 ** " + b[off+9]);
                    }
                    //*/

                    if( length > 0 )
                    {
                        //System.err.println("FCTSource -- Tried to read " + readLength + ": Actually Read " + length + " at position " + readpos);

                        readpos += length;
                        _leftToRead = _leftToRead - length;

                        //System.err.println("FCTSource -- AFTER: Filesize is " + currenttrack.length + ", current size is " + sizeOnDisk + ", read_offset, " + currenttrack.start + ", readpos = " + readpos + " with " + _leftToRead + " remaining on this pass.");

                        if (_leftToRead < 0)
                        {
                            _leftToRead = 0;
                            //System.err.println(_leftToRead + " WARNING LEFT READ WENT NEGATIVE!!!!!!!!!!!!!!!");
                        }

                        //System.err.println("FCTSource -- read this much -- " + length);
                        return length;
                    }
                    else
                    {
                        try
                        {
                            currenttrack.flush = true;
                            inputFileStream.close();
                            inputFileStream = null;
                            backingfileReader.close();
                            backingfileReader = null;

                            //System.err.println( "***** -- Opening of track file in TRACKSOURCE -- " + currenttrack.filename );
                            backingfileReader = new FileConnection(currenttrack.filename);

                            if( backingfileReader.exists() )
                            {
                                inputFileStream = backingfileReader.openInputStream();
                                sizeOnDisk = (int)backingfileReader.fileSize();
                                _leftToRead = sizeOnDisk - readpos;
                                if (_leftToRead <= 0)
                                {
                                    _leftToRead = 0;
                                    try { //Thread.sleep(2000);
                                    }
                                    catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(527) -- " + e.getMessage()); }
                                    return 0;
                                }
                                else
                                {
                                    //System.err.println("FCTSource -- REFRESH : new size on disk is" + sizeOnDisk + " with " + _leftToRead + " remaining on this pass.");
                                    //System.err.println("FCTSource -- REFRESH Read " + len + " at " + readpos + " of " + sizeOnDisk + " with " + _leftToRead + " left");
                                    inputFileStream.skip(readpos);
                                    continue;
                                }
                            }
                            //System.err.println("FCTSource -- Reopening File size " + sizeOnDisk + " with left " + _leftToRead + " at " + readpos + " from " + currenttrack.start);
                        }
                        catch (Exception e2)
                        {
                            System.err.println("FCTSource -- ERROR ON REFRESH : Requested " + len + ": Read " + length + " at position " + readpos + ". Current file size is " + sizeOnDisk + " with " + _leftToRead + " remaining on this pass.");
                            return 0;
                        }
                    }
                }
                else
                {
                    if (_leftToRead < 0)
                    {
                        _leftToRead = 0;
                    }
                    if (_leftToRead == 0)
                    {
                        try
                        {
                            currenttrack.flush = true;
                            inputFileStream.close();
                            inputFileStream = null;
                            backingfileReader.close();
                            backingfileReader = null;

                            //System.err.println( "***** -- Opening of track file in TRACKSOURCE -- " + currenttrack.filename );
                            backingfileReader = new FileConnection(currenttrack.filename);

                            if( backingfileReader.exists() )
                            {
                                inputFileStream = backingfileReader.openInputStream();
                                sizeOnDisk = (int)backingfileReader.fileSize();
                                _leftToRead = sizeOnDisk - readpos;
                                if (_leftToRead <= 0)
                                {
                                    _leftToRead = 0;
                                    try { /*Thread.sleep(2000); */}
                                    catch (Exception e) { System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(501) -- " + e.getMessage()); }
                                    return 0;
                                }
                                else
                                {
                                    //System.err.println("FCTSource -- REFRESH : new size on disk is" + sizeOnDisk + " with " + _leftToRead + " remaining on this pass.");
                                    inputFileStream.skip(readpos);
                                    continue;
                                }
                            }
                            //System.err.println("FCTSource -- Reopening File size " + sizeOnDisk + " with left " + _leftToRead + " at " + readpos + " from " + currenttrack.start);
                        }
                        catch (Exception e2)
                        {
                            System.err.println("FCTSource -- ERROR ON REFRESH : Requested " + len + ": Read " + length + " at position " + readpos + ". Current file size is " + sizeOnDisk + " with " + _leftToRead + " remaining on this pass.");
                            return 0;
                        }
                    }
                }
            }
            //stop was signaled
            return -1;
        }


        public void Init(){
        	try{
        	//if (backingfileReader == null)
            {
                //System.err.println("FCTSource -- ************CREATING BACKING READER**********");
                //System.err.println( "***** -- Opening of track file in TRACKSOURCE -- " + currenttrack.filename );
                backingfileReader = new FileConnection(currenttrack.filename);
                if( backingfileReader.exists() )
                {
                    if( currenttrack.synced == false && currenttrack.mediatype != null && currenttrack.mediatype.equals(DPStringConstants.STR_AUDIO_MP3) )
                    {
                        int offset = 0;
                        currenttrack.syncoff = 0;
                        int filesize = (int) backingfileReader.fileSize();
                        InputStream syncStream = null;
                        while( currenttrack.synced == false && offset < filesize )
                        {
                            if( syncStream == null )
                            	syncStream = backingfileReader.openInputStream();

                            syncStream.skip(offset + currenttrack.start);
                            syncStream.read(syncdata, 0, 8194);
                            for( int i = 0; i < 8192; i++ )
                            {
                                if( syncdata[i] == (byte)0xFF && ( (syncdata[i+1] == (byte)0xFA) || (syncdata[i+1] == (byte)0xFB) || (syncdata[i+1] == (byte)0xF3) || (syncdata[i+1] == (byte)0xF2) ) )
                                {
                                    int bitrate = -1;

                                    //byte aa = (byte) ( ( syncdata[i+2] >> 2 ) & (byte)0x03 );
                                    byte bb = (byte) ( ( syncdata[i+2] >> 4 ) & (byte)0x0F );
                                    int index = bb;
                                    byte cc = (byte) ( ( syncdata[i+2] >> 2 ) & (byte)0x03 );
                                    if( cc == 3 ) continue;
                                    if( (syncdata[i+1] == (byte)0xFA) || (syncdata[i+1] == (byte)0xFB) )
                                    {
                                        bitrate = version1layer3[index];
                                    }
                                    else
                                    {
                                        bitrate = version2layer3[index];
                                    }
                                    if( currenttrack.bitrate == 99999 )
                                    {
                                        currenttrack.bitrate = bitrate;
                                    }
                                    if( bitrate == currenttrack.bitrate )
                                    {
                                        currenttrack.syncoff += i;
                                        currenttrack.synced = true;
                                    }

                                    //System.err.println("FCTSource -- start at (" + currenttrack.start + ") SYNC FRAME on read size at (" + (offset+i) + ") -- proxy BR (" + currenttrack.bitrate + ") audio BR (" + bitrate + ")");
                                    break;
                                }
                            }
                            if( currenttrack.synced == false )
                            {
                                currenttrack.syncoff += 8192;
                                offset += 8192;
                            }
                            //syncStream.close();
                            //syncStream = null;
                        }
                        if( currenttrack.synced == false )
                        {
                            currenttrack.synced = true;
                            currenttrack.syncoff = 0;
                        }
                    }

                    inputFileStream = backingfileReader.openInputStream();
                    readpos = currenttrack.start + currenttrack.syncoff;
                    if( DPFlyCastTrackSource.AddOffset() ){
                    	readpos += DPFlyCastTrackSource.OFFSET;
                    }

                    inputFileStream.skip(readpos);
                    sizeOnDisk = (int)backingfileReader.fileSize() ;
                    _leftToRead = sizeOnDisk - readpos;
                    //System.err.println("FCTSource -- Requesting Read " + len + " at " + readpos + " of " + sizeOnDisk + " with " + _leftToRead + " left");
                }/*
                else
                {
                    try { Thread.sleep(2000); }
                    catch (Exception e) {
                    	System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(407) -- " + e.getMessage());
                    }
                    return 0;
                }*/
                //System.err.println("FCTSource -- Opening File size " + sizeOnDisk + " with left " + _leftToRead + " at " + readpos + " from " + currenttrack.start);
            }
        	}
        	catch(Exception ex){

        	}
        }

        public void close() throws IOException
        {
            try
            {
                _stop = true;
                inputFileStream.close();
                backingfileReader.close();
                inputFileStream = null;
                backingfileReader = null;

                sizeOnDisk = 0;
                _leftToRead = 0;

                //System.err.println("FCTSource -- CLOSED STRWAM STREAM");
            }
            catch (Exception e)
            {
                System.err.println(DPStringConstants.STR_EXCEPTION + " -- FCTSource(542) -- " + e.getMessage());
            }
        }
    
}
