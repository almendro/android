package fm.flycast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.appMobi.appMobiLib.LocalService;

public class DPFileHandler {
	
	private String uid = null;
	public static int m_maxSeconds = 50 * 60 * 60;
	public static int m_curSeconds = 0;
	
	public DPFileHandler(String uid)
	{
		this.uid = uid;
	}	
	
	public void deleteTracklist(DPXMLTracklist tracklist)
    {		
		String filename = LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + tracklist.stationid;
		deleteDirectory(filename);		
    }
	
	//This function will be called from DPApplication.
	/*
	 * Step -1 . Get the existing recordings from the file structure.
	 * Step -2 . If previous recording exist then append the content of tracklist to it.
	 * Step -3 . If previous does not exist then just save this track list.
	 * 
	 * 
	 */
	public DPXMLTracklist saveTracklist(DPXMLTracklist tracklist)
    {
        
		verifyTracklistDirectory( tracklist );
        if( tracklist == null || tracklist.children.size() == 0 ) return null;
        DataOutputStream outputStream = null; 
        FileOutputStream fos = null;
        File  file = null;
        try
        {        	
            String filename = LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + tracklist.stationid + DPStringConstants.TEMP_STR_SLASH + DPStringConstants.STR_TRACKLIST_FILE3;
            file = new File(filename);            
            if( !file.exists() )
            {
            	file.createNewFile();            	
            }
            //Open fileOutputStream in non append mode
            fos = new FileOutputStream(file);            
            outputStream = new DataOutputStream(fos);
            int s = 0;
            for(int i = 0; i < tracklist.children.size(); i++){
            	DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
            	if( temp.cached)
            		s++;
            }

            outputStream.writeInt( s );
            outputStream.writeInt( tracklist.stationid );
            outputStream.writeLong( tracklist.timecode );
            writeString( tracklist.station, outputStream );
            outputStream.writeBoolean( tracklist.shuffleable );
            outputStream.writeBoolean( tracklist.deleteable );
            outputStream.writeBoolean( tracklist.autoshuffle );
            outputStream.writeBoolean( tracklist.autohide );            
            for( int i = 0; i < tracklist.children.size(); i++ )
            {
                DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
                if( temp.cached){
	                writeString( temp.artist, outputStream );
	                writeString( temp.title, outputStream );
	                writeString( temp.album, outputStream );
	                writeString( temp.metadata, outputStream );
	                writeString( temp.imageurl, outputStream );
	                writeString( temp.mediaurl, outputStream );
	                writeString( temp.redirect, outputStream );
	                writeString( temp.mediatype, outputStream );
	                writeString( temp.starttime, outputStream );
	                writeString( temp.guidIndex, outputStream );
	                writeString( temp.guidSong, outputStream );
	                outputStream.writeLong( temp.timecode );
	                outputStream.writeInt( temp.offset );
	                outputStream.writeInt( temp.length );
	                outputStream.writeInt( temp.current );
	                outputStream.writeInt( temp.start );
	                outputStream.writeInt( temp.bitrate );
	                outputStream.writeInt( temp.seconds );
	                outputStream.writeInt( temp.stationid );
	                outputStream.writeBoolean( temp.buffered );
	                outputStream.writeBoolean( temp.cached );
	                outputStream.writeBoolean( temp.covered );
	                outputStream.writeBoolean( temp.flyback );
	                outputStream.writeBoolean( temp.listened );
	                outputStream.writeBoolean( temp.played );
	                outputStream.writeInt( temp.expdays );
	                outputStream.writeInt( temp.expplays );
	                outputStream.writeInt( temp.numplay );
	                outputStream.writeBoolean( temp.clickAd );
	                outputStream.writeBoolean( temp.audioAd );
	                outputStream.writeBoolean( temp.reloadAd );
	                writeString( temp.addart, outputStream );
	                writeString( temp.adurl, outputStream );
	                outputStream.writeBoolean( temp.synced );
	                outputStream.writeInt( temp.syncoff );
                }else
                {
                	tracklist.children.remove(i);
                }
            }
                    
        }
        catch (Exception e)
        {
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(1789) -- " + e.getMessage());
            outputStream = null;
            fos = null;
            return null;
        }finally{
        	try {
				fos.close();
				fos = null;
				outputStream.close();            
		        outputStream = null; 
			} catch (IOException e) {
				e.printStackTrace();
			}              
        }
        return tracklist;
    }
	
	public void verifyTracklistDirectory(DPXMLTracklist tracklist)
    {
        File dir = null;        
        dir = new File(LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + tracklist.stationid + DPStringConstants.TEMP_STR_SLASH);
        if(!dir.exists())
        {
        	dir.mkdirs();
        }
    }	
	
	public String readString(DataInputStream stream)
    {
        String data = null;
        try
        {
            int size = stream.readShort();
            if( size == 0 ) return data;
            byte[] raw = new byte[size];
            stream.read(raw, 0, size );
            for( int i = 0; i < size; i++ )
            {
                raw[i] -= ( i + 128 );
            }
            data = new String(raw, 0, size);
        }
        catch (Exception e)
        {
            System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage());
        }

        return data;
    }
	
	public void writeString(String data, DataOutputStream stream)
    {
        try
        {
            if( data == null )
            {
                stream.writeShort(0);
                return;
            }
            stream.writeShort(data.length());
            byte[] raw = data.getBytes();
            for( int i = 0; i < data.length(); i++ )
            {
                stream.writeByte(raw[i] + i + 128);
            }
        }
        catch (Exception e)
        {
            System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage());
        }
    }
	
	
	public DPXMLTracklist readTracklist(int stationid)
    {
        DataInputStream inputStream = null;       
        File file = null;
        DPXMLTracklist tracklist = null;
        boolean version2 = false;
        boolean version3 = true;
        try
        {
            String filename = LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + stationid + DPStringConstants.TEMP_STR_SLASH + DPStringConstants.STR_TRACKLIST_FILE3;
            file = new File(filename);

            if(!file.exists())
            {
            	 version3 = false;
                 version2 = true;
                 filename = LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + stationid + DPStringConstants.TEMP_STR_SLASH + DPStringConstants.STR_TRACKLIST_FILE2;
                 file = new File(filename);            	
            }
            if(!file.exists())
            	return null;
            FileInputStream fis = new FileInputStream(file);
            inputStream = new DataInputStream(fis);
            int size = inputStream.readInt();
            tracklist = new DPXMLTracklist();
            tracklist.stationid = inputStream.readInt();
            tracklist.timecode = inputStream.readLong();
            tracklist.station = readString(inputStream);
            tracklist.shuffleable = inputStream.readBoolean();
            if( version3 == true ) tracklist.deleteable = inputStream.readBoolean();
            if( version3 == true ) tracklist.autoshuffle = inputStream.readBoolean();
            if( version3 == true ) tracklist.autohide = inputStream.readBoolean();
            tracklist.offline = true;
            for( int i = 0; i < size; i++ )
            {
                DPXMLTrack temp = new DPXMLTrack();
                temp.artist = readString(inputStream);
                temp.title = readString(inputStream);
                temp.album = readString(inputStream);
                temp.metadata = readString(inputStream);
                temp.imageurl = readString(inputStream);
                temp.mediaurl = readString(inputStream);
                temp.redirect = readString(inputStream);
                temp.mediatype = readString(inputStream);
                temp.starttime = readString(inputStream);
                temp.guidIndex = readString(inputStream);
                temp.guidSong = readString(inputStream);
                temp.timecode = inputStream.readLong();
                temp.offset = inputStream.readInt();
                temp.length = inputStream.readInt();
                temp.current = inputStream.readInt();
                temp.start  = inputStream.readInt();
                temp.bitrate = inputStream.readInt();
                temp.seconds = inputStream.readInt();
                temp.stationid = inputStream.readInt();
                temp.buffered = inputStream.readBoolean();
                temp.cached = inputStream.readBoolean();
                temp.covered = inputStream.readBoolean();
                temp.flyback = inputStream.readBoolean();
                temp.listened = inputStream.readBoolean();
                if( version3 == true || version2 == true ) temp.played = inputStream.readBoolean();
                else temp.played = true;
                if( version3 == true ) temp.expdays = inputStream.readInt();
                if( version3 == true ) temp.expplays = inputStream.readInt();
                if( version3 == true ) temp.numplay = inputStream.readInt();
                if( version3 == true ) temp.clickAd = inputStream.readBoolean();
                if( version3 == true ) temp.audioAd = inputStream.readBoolean();
                if( version3 == true ) temp.reloadAd = inputStream.readBoolean();
                if( version3 == true ) temp.addart = readString(inputStream);
                if( version3 == true ) temp.adurl = readString(inputStream);
                if( version3 == true ) temp.synced = inputStream.readBoolean();
                if( version3 == true ) temp.syncoff = inputStream.readInt();
                temp.albumfile = new String( LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + tracklist.stationid + DPStringConstants.TEMP_STR_SLASH + temp.guidSong + DPStringConstants.STR_JPEG );
                temp.filename = new String( LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + tracklist.stationid + DPStringConstants.TEMP_STR_SLASH + temp.guidSong );

                if( verifyTrack( temp ) )
                {
                    tracklist.children.addElement(temp);
                    DPTracklistManager.inInstance().addNewTrack(temp); //
                }
                else
                {
                    deleteTrackFiles( temp );
                }
            }

            if( tracklist.children.size() == 0 )
            {
                tracklist = null;
                file.delete();
            }
            inputStream.close();
            inputStream = null;            
            file = null;
            if( version3 == false ) saveTracklist( tracklist );
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("DPFileHandler","readTracklist method",e.fillInStackTrace());
        	if(Debug.isDebuggerConnected()) Log.e("DPFileHandler","readTracklist method",e.fillInStackTrace());
        	
        	e.printStackTrace();
            System.err.println(DPStringConstants.STR_EXCEPTION + e.getMessage());
            inputStream = null;
            file = null;
        }
        return tracklist;
    }
	
	/*public DPXMLTracklist appendTrackList(DPXMLTracklist firstTrackList, DPXMLTracklist secondTrackList)
    {
		Vector firstChieldTrack =firstTrackList.children;
		Vector secondChieldTrack =secondTrackList.children;
		for(int i = 0, len = secondChieldTrack.size(); i < len; i++)
		{
			DPXMLTrack temp = (DPXMLTrack) secondChieldTrack.elementAt(i);
			addNewTrack(firstTrackList, temp);			
		}		
		return firstTrackList;
    }*/
	
	public boolean verifyTrack(DPXMLTrack track)
    {        
		File file = null;
        try
        {
           	file = new File(track.filename);      
        	if( file.exists() )
            {
        		file = null;
                if( track.expplays != -1 && track.numplay >= track.expplays ) return false;
                long current = System.currentTimeMillis();
                long elapsed = (current - track.timecode) / 1000 / 60 / 60 / 24; // Number of elapsed days
                if( track.expdays != -1 && elapsed >= track.expdays ) return false;
                return true;
            }
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("DPFileHandler","verifyTrack method trying to create file with (track.filename)",e.fillInStackTrace());
        	if(Debug.isDebuggerConnected()) Log.e("DPFileHandler","verifyTrack method trying to create file with (track.filename)",e.fillInStackTrace());
        	
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(1530) -- " + e.getMessage());
            file = null;
        }

        return false;
    }
	
	 
	public void addNewTrack2(DPXMLTracklist tl, DPXMLTrack track)
    {        
		/*tl.timecode += track.seconds;
		tl.children.addElement(track);        
        while( tl.timecode > m_maxSeconds )
        {
            DPXMLTrack temp = (DPXMLTrack) tl.children.elementAt(0);
            tl.children.removeElementAt(0);
            deleteTrack(temp);
        }*/
    }
	
	/*public void addNewTrack(DPXMLTracklist trackList, DPXMLTrack track)
    {
        if( m_maxSeconds == 0 ) return;

        m_curSeconds += track.seconds;
        trackList.children.addElement(track);       
        while( m_curSeconds > m_maxSeconds )
        {
            DPXMLTrack temp = (DPXMLTrack) trackList.children.elementAt(0);
            trackList.children.removeElementAt(0);
            deleteTrack(temp);
        }
    }*/
	
	public void deleteTrackFiles(DPXMLTrack track)
    {        
		File file = null;       
        try
        {
           	file = new File(track.albumfile);
        	file.delete();
        	file = null;            
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("DPFileHandler","deleteTrackFiles method trying to create file with (track.albumfile)",e.fillInStackTrace());
        	if(Debug.isDebuggerConnected()) Log.e("DPFileHandler","deleteTrackFiles method",e.fillInStackTrace());
        	
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(2193) -- " + e.getMessage());
            file = null;
        }

        try
        {           
        	file = new File(track.filename);
        	file.delete();         
        	file = null;
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("DPFileHandler","deleteTrackFiles method trying to create file with (track.filename)",e.fillInStackTrace());
        	if(Debug.isDebuggerConnected()) Log.e("DPFileHandler","deleteTrackFiles method",e.fillInStackTrace());
        	
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(2206) -- " + e.getMessage());
            file = null;
        }
    }
	
	
	
	public DPXMLTracklist deleteTrack(DPXMLTrack track)
    {
        DPXMLTracklist deletedTrackList = null;
        if(track == null)
        	return null;
        deletedTrackList = DPApplication.Instance().GetTrackList();
        if(deletedTrackList == null)
        	return null;
        //does track duplicate ??? 
        boolean duplicate = false;
        for( int i = 0, len = deletedTrackList.children.size(); i < len; i++ )
        {
            DPXMLTrack ttemp = (DPXMLTrack) deletedTrackList.children.elementAt(i);
            if( ttemp != track && ttemp.guidIndex != null && track.guidIndex != null && ttemp.guidIndex.equals( track.guidIndex ) )
            {
                duplicate = true;
            }else if( ttemp == track )//Delete the track
	            {
	                deletedTrackList.children.removeElementAt(i);
	                break;
	            }
        }
        
        if( track.cached == true )
        {
            m_curSeconds -= track.seconds;
        }
        if( duplicate == false )
        {
            deleteTrackFiles( track );
        }
        return deletedTrackList;
    }
	
	
	public boolean deleteRecording(String uid, String stationID)
	{
		String deletedFolderName = LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + stationID;
		deleteDirectory(deletedFolderName);
		return true;
	}
	
	public boolean deleteAllRecording(String uid)
	{
		String deletedFolderName = LocalService.BASE_DIR + uid;
		deleteDirectory(deletedFolderName);
		return true;
	}
	
	
	public void deleteDirectory(String directory)
    {        
		File file = null;
        try
        {
           	file = new File(directory);
            if( file.isDirectory())
            {
                String[] fileList = file.list();
                for(String fileName:fileList)
                {
                	File tempFile = new File(directory+DPStringConstants.TEMP_STR_SLASH+fileName);
                	if(file.isDirectory())
                	{
                		deleteDirectory(directory+DPStringConstants.TEMP_STR_SLASH+fileName);
                	}else
                	{
                		tempFile.delete();
                	}
                	tempFile = null;
                }                
                file.delete();
            }else
            {
            	file.delete();
            }
            file = null;
        }
        catch (Exception e)
        {
        	if(Debug.isDebuggerConnected()) Log.d("DPFileHandler","deleteDirectory method",e.fillInStackTrace());
        	if(Debug.isDebuggerConnected()) Log.e("DPFileHandler","deleteDirectory method",e.fillInStackTrace());
        	
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(1256) -- " + e.getMessage());
            file = null;
        }
    }
		
	public DPXMLTrack getTrack(String stationID, String songGUID) throws DPException
	{		
		DPXMLTracklist traceList = DPApplication.Instance().GetTrackList();
		if(traceList != null){
			for(int i = 0, len = traceList.children.size(); i < len; i++)
			{
				DPXMLTrack track = (DPXMLTrack)traceList.children.get(i);
				if(songGUID.equals(track.guidSong))
				{
					return track;
				}
			}
		}	
		throw new DPException("Track not found");		
	}
	
	public ArrayList<DPXMLTracklist> getAllTrackList(String uid) throws DPException
	{
		ArrayList<DPXMLTracklist> allTrackList = new ArrayList<DPXMLTracklist>();
		String rootDir = LocalService.BASE_DIR + uid;
		File dir = new File(rootDir);
		if(dir == null) //No recording till now
			return null;
		String[] dirList = dir.list();
		int len = dirList.length;
		for(int i = 0; i < len; i++ )
		{
			File tempFile = new File(rootDir+DPStringConstants.TEMP_STR_SLASH+dirList[i]);
			if(tempFile.isDirectory())
			{
				int stationID = -1;
				try{
					stationID = Integer.parseInt(dirList[i]);
				}catch(NumberFormatException nfe)
				{
					throw new DPException("Station Id is not number", 100); 
					//Directory is not stationID so don't do any thing					
				}
				if(stationID != -1)
				{
					allTrackList.add(readTracklist(stationID));					
				}				
			}
		}		
		return allTrackList;
	}	
	
	public int[] getAllStationIDs()
	{
		return getAllStationIDs(null);
	}
	
	
	/**
	 * Recording for each station in done in directory with name same as station.
	 * Find all the directories and return them as stations.
	 * 
	 * @param userId
	 * @return
	 */
	public int[] getAllStationIDs(String userId)
	{
		String rootDir = LocalService.BASE_DIR + (userId == null ? uid : userId);
		File dir = new File(rootDir);
		if(dir == null) //No recording till now
			return new int[0];
		String[] dirList = dir.list();
		int len = dirList.length;
		int index = 0;
		int[] allStationIds = new int[len];
		for(int i = 0; i < len; i++ )
		{
			File tempFile = new File(rootDir+DPStringConstants.TEMP_STR_SLASH+dirList[i]);
			if(tempFile.isDirectory())
			{
				int stationID = -1;
				try{
					stationID = Integer.parseInt(dirList[i]);
				}catch(NumberFormatException nfe)
				{
					//Directory is not stationID so don't do any thing					
				}
				if(stationID != -1)
				{
					allStationIds[index++] = stationID;				
				}				
			}			
		}		
		return allStationIds;
	}

}
