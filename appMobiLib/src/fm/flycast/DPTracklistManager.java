package fm.flycast;

import java.io.File;
import java.util.ArrayList;

import com.appMobi.appMobiLib.LocalService;


/**
 * Class to keep all the information regarding the tracks saved on the disk 
 * and the tracks which are there in the track list currently being played.
 * 
 * 
 * @author Administrator
 *
 */
public class DPTracklistManager {
	
	private static DPTracklistManager manager = null;
	
	private	int m_maxSeconds = 50 * 60 * 60;
	private int m_curSeconds = 0 ;
	private String uid = null;
	private	ArrayList<DPXMLTrack> m_allTracks = new ArrayList<DPXMLTrack>();    
	private	ArrayList<DPXMLTracklist> m_allTracklists = new ArrayList<DPXMLTracklist>();
    private DPXMLTracklist  m_currentList = null;
	
	
	static{
		manager = new DPTracklistManager();
	}
	
	private DPTracklistManager()
	{
		uid = DPApplication.Instance().getUid();
		populateAllTracklists();
		populateAllTrack();		
	}
	
	public static DPTracklistManager inInstance()
	{
		return manager;
	}
	
	
	public void saveTracklists()
	{
		for( int i = 0; i < m_allTracklists.size(); i++ )
        {
            DPXMLTracklist track = m_allTracklists.get(i);
            saveTracklist( track );
        }
		
	}
	
	public DPXMLTracklist saveTracklist(DPXMLTracklist tracklist)
	{
		DPFileHandler fileHandler = new DPFileHandler(uid);
		return fileHandler.saveTracklist(tracklist);
	}
	
	
	public void AddTrackTimeToTotalRecordingTime(int trackTime){
        m_curSeconds += trackTime;
	}
	
	public void SubtractTrackTimeFromTotalRecordingTime(int trackTime){
        m_curSeconds -= trackTime;
	}

	public int getTotalTrackTime(){
		return m_curSeconds;
	}
	
	public DPXMLTrack addNewTrack(DPXMLTrack track)
	{
		if( m_maxSeconds == 0 ) return null;

        AddTrackTimeToTotalRecordingTime(track.seconds );
        m_allTracks.add(track);
        while( m_curSeconds > m_maxSeconds )
        {
            DPXMLTrack temp = m_allTracks.get(0);
            m_allTracks.remove(0);
            deleteTrack(temp);
        }
		return null;
	}
	
	/**
	 * Delete the track from all track lists.
	 * Track is not being removed from m_AllTracks .. Why?
	 * @param track
	 */
	public void deleteTrack(DPXMLTrack track)
    {
        DPXMLTracklist cachedTracklist = null;
        DPXMLTracklist realTracklist = null;
        int trackidx = -1;
        
        //Find the track list for the station to which track belongs
        for( int i = 0; i < m_allTracklists.size(); i++ )
        {
            DPXMLTracklist temp = m_allTracklists.get(i);
            if( temp.stationid == track.stationid )
            {
                //We have found the track list with the same station.
            	trackidx = i;
                cachedTracklist = temp;
                break;
            }
        }

        //We could not find the track list for the station to which track belongs.
        //so set the track list to current track list.
        if( cachedTracklist == null && m_currentList != null )
        {
            cachedTracklist = m_currentList;
        }

        if( m_currentList != null && m_currentList.shuffled == true )
        {
            realTracklist = cachedTracklist;
            cachedTracklist = m_currentList;
        }

        //Does this track exist in multiple track lists?
        boolean duplicate = false;
        //The index of the track in the list which is same DPXMLTrack object as 
        //track object passed as parameter to this function.
        int index = -1;
        for( int i = 0; cachedTracklist != null && i < cachedTracklist.children.size(); i++ )
        {
            DPXMLTrack ttemp = (DPXMLTrack) cachedTracklist.children.elementAt(i);
            if( ttemp != track && ttemp.guidIndex != null && track.guidIndex != null && ttemp.guidIndex.equals( track.guidIndex ) )
            {
                duplicate = true;
            }
            if( ttemp == track )
            {
                index = i;
            }
        }

        //realTrackList will always be the currently playing track list.
        for( int j = 0; realTracklist != null && j < realTracklist.children.size(); j++ )
        {
            DPXMLTrack ttemp = (DPXMLTrack) realTracklist.children.elementAt(j);
            if( ttemp == track )
            {
                realTracklist.children.removeElementAt(j);
                break;
            }
        }

        if( track.cached == true )
        {
            SubtractTrackTimeFromTotalRecordingTime(track.seconds);
        }


        //Delete the track from the file system.One of the below conditions must be true.
        //Track was found in some track list other then the currently playing track list.
        if( index != -1 )
        {
            cachedTracklist.children.removeElementAt(index);
            //HOW WILL TRACK BE REMOVED IN THE CASE CHILDREN SIZE IS NOT 0?
            if( cachedTracklist.children.size() == 0 && trackidx != -1 )
            {
                m_allTracklists.remove(trackidx);
                deleteTracklist(cachedTracklist);
                DPFileHandler filehandler = new DPFileHandler(uid);
                filehandler.deleteDirectory(LocalService.BASE_DIR + uid + DPStringConstants.TEMP_STR_SLASH + cachedTracklist.stationid + DPStringConstants.TEMP_STR_SLASH);
            }
        }

        //Track is in the currently playing track list.
        if( duplicate == false )
        {
            //deleteTrackFiles( track );
        	DPFileHandler fileHandler = new DPFileHandler(uid);
        	fileHandler.deleteTrackFiles(track);
        }
    }
	
	
	public void deleteTracklist(DPXMLTracklist tracklist)
    {
        if( tracklist == null || tracklist.children.size() == 0 ) return;

        for( int i = tracklist.children.size()-1; i >= 0; i-- )
        {
            DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
            deleteTrack( temp );
        }
    }
	
	
	public DPXMLTrack findTrack(DPXMLTracklist tracklist, DPXMLTrack track)
    {
        if( tracklist == null || track == null || track.guidSong == null ) return null;

        DPXMLTracklist cachedTracklist = null;
        for( int i = 0; i < m_allTracklists.size(); i++ )
        {
            DPXMLTracklist temp = m_allTracklists.get(i);
            if( temp.stationid == tracklist.stationid )
            {
                cachedTracklist = temp;
                break;
            }
        }

        if( cachedTracklist == null )
        {
            return null;
        }

        for( int i = 0; i < cachedTracklist.children.size(); i++ )
        {
            DPXMLTrack temp = (DPXMLTrack) cachedTracklist.children.elementAt(i);
            if( temp.guidSong.equals( track.guidSong ) )
            {
                cachedTracklist.children.removeElementAt(i);
                return temp;
            }
        }

        return null;
    }
	
	
	public DPXMLTracklist persistTracklist(DPXMLTracklist tracklist)
    {
        if( tracklist != null && tracklist.offline == true )
        {
            for( int i = tracklist.children.size() -1; i >= 0; i-- )
            {
                DPXMLTrack track = (DPXMLTrack) tracklist.children.elementAt(i);
                if( verifyTrack( track ) == false )
                {
                    deleteTrack( track );
                }
            }
        }

        if( tracklist == null || tracklist.shuffled == true || tracklist.users == true ) return null;
        verifyTracklistDirectory( tracklist );


        if( m_maxSeconds == 0 ) return null;

        for( int i = tracklist.children.size() -1; i >= 0; i-- )
        {
            DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
            if( temp.cached == false || tracklist.throwaway == true )
            {
                tracklist.children.removeElementAt(i);
                deleteTrack( temp );
            }
        }
        if( tracklist.children.size() == 0 ) return null;

        for( int i = 0; i < tracklist.children.size(); i++ )
        {
            DPXMLTrack temp = (DPXMLTrack) tracklist.children.elementAt(i);
            //temp.original = null; NOT PRESENT IN DPXMLTrack object
            //temp.scaled = null;	NOT PRESENT IN DPXMLTrack object
            //temp.live = null;		NOT PRESENT IN DPXMLTrack object
            if( tracklist.offline == false ) addNewTrack(temp);
        }
        System.gc();

        DPXMLTracklist cachedTracklist = null;
        for( int i = 0; i < m_allTracklists.size(); i++ )
        {
            DPXMLTracklist temp = m_allTracklists.get(i);
            if( temp.stationid == tracklist.stationid )
            {
                cachedTracklist = temp;
                break;
            }
        }

        if( cachedTracklist == tracklist )
        {
            cachedTracklist.startindex = 0;
            cachedTracklist.offline = true;
            return tracklist;
        }
        else if( cachedTracklist == null )
        {
            cachedTracklist = tracklist;
            cachedTracklist.startindex = 0;
            cachedTracklist.offline = true;
            m_allTracklists.add(tracklist);
        }
        else
        {
            cachedTracklist.shuffleable = tracklist.shuffleable;
            cachedTracklist.deleteable = tracklist.deleteable;
            cachedTracklist.autoshuffle = tracklist.autoshuffle;
            cachedTracklist.autohide = tracklist.autohide;

            for( int i = 0; i < tracklist.children.size(); i++ )
            {
                DPXMLTrack track = (DPXMLTrack) tracklist.children.elementAt(i);
                cachedTracklist.children.addElement( track );
            }
        }

        cachedTracklist.offline = true;
        return cachedTracklist;
    }
	
	
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
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- FlyCast(1530) -- " + e.getMessage());
            file = null;
        }

        return false;
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
	
	public void populateAllTracklists()
	{
		//find the all station id for the user
		int[] stationIDs = getAllStationIds(uid);
		
		//For all station read the station list data 
		//and populate DPXMLTracklist object
		for(int i = 0, len = stationIDs.length; i < len; i++)
		{
			DPXMLTracklist tracklist = readTracklist(stationIDs[i]);
			//store DPXMLTracklist in the m_allTracklists variable
			if(tracklist != null)
				m_allTracklists.add(tracklist);
		}
	}
	
	public DPXMLTracklist readTracklist(int stationID)
	{
		DPFileHandler fileHandler = new DPFileHandler(uid);
		return fileHandler.readTracklist(stationID);
	}
	
	public int[] getAllStationIds(String userId)
	{
		DPFileHandler fileHandler = new DPFileHandler(userId == null ? uid : userId);		
		return fileHandler.getAllStationIDs(null);
	}
	
	
	
	public void populateAllTrack()
	{
		//Comment:: populateAllTracklists should not be called by this function.
		//Reason being this function is responsible to create the all track list
		//not to populate the all track list.
		if(m_allTracklists == null || m_allTracklists.size() == 0)
		{
			populateAllTracklists();
		}
		int tracklistLen = m_allTracklists == null ? 0 : m_allTracklists.size();
		for(int i = 0; i < tracklistLen; i++ )
		{
			DPXMLTracklist tempTracklist =  m_allTracklists.get(i);
			int trackLen = tempTracklist == null ? 0:tempTracklist.children.size();
			for(int j=0 ; j < trackLen; j++)
			{
				m_allTracks.add((DPXMLTrack)tempTracklist.children.get(j));
			}
			
		}
		
	}
}
