package fm.flycast;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

public class DPXMLParser 
{
	private static final String TAG_DPXMLParser = "DPXMLParser";	//tags for android.util.Log Logging
	
    private String _xml = null;
    private xmlHandler handler = null;
//    private Stack nodeStack = new Stack();
//    private Stack tagStack = new Stack();
    
    public DPXMLMessageList messagelist = null; 
    public DPXMLMessage curmessage = null;    
    //public DPXMLTracklist tracklist = null;
    private DPXMLTrack curtrack = null;
    private String curname;
    private String curvalue;
    
    private boolean In_ResourceContainer_Tracklist = false;	//if true we have encountered ResourceContainer/Tracklist start element, but not yet end element    
    private boolean In_ResourceContainer_MessageNoGuidSong = false;	//if true we have encountered ResourceContainer/Message, but not yet end element    
    private boolean In_ResourceContainer_MessageWithGuidSong = false;	//if true we have encountered ResourceContainer/Message with guidSong, but not yet end element    
    private boolean In_ResourceContainer_Exception = false;	//
    private boolean In_Tracklist_Metadata = false;	//In Tracklist data	//if true when we have encountered Metadata start element, but not yet end element    
    private boolean In_Resource_Track = false;				//if true when we have encountered Resource/Track start element, but not yet end element    
    
//    public XMLMetadata metadata = null;
    
    //<XML> Strings of interest
    private static String STR_XML = "XML";
    private static String STR_RESOURCECONTAINER = "ResourceContainer";
    private static String STR_METADATA = "Metadata";
    private static String STR_RESOURCE = "Resource";    
    private static String STR_KEY = "key"; 

    //--- ResourceContainer names ------------
    public static String STR_TRACKLIST = "TrackList";	//Yes it's a Capital "L" in this XML
    //These are all "Messages" returned in GETMESSAGES DeviceProxy call  
    public static String STR_RECORDING_HAS_FINISHED = "RECORDING_HAS_FINISHED"; 
    public static String STR_RECORDING_HAS_STARTED = "RECORDING_HAS_STARTED";
    public static String STR_RETRYING_CONNECTION = "RETRYING_CONNECTION";
    public static String STR_STREAM_IS_BUFFERED = "STREAM_IS_BUFFERED";
    public static String STR_TRACK_WAS_ADDED = "TRACK_WAS_ADDED";
    public static String STR_TRACKLIST_EMPTY = "TRACKLIST_EMPTY";    
    public static String STR_TRACK_HAS_UPDATED = "TRACK_HAS_UPDATED";		//Message includes guidSong key Name/Value Pair
    public static String STR_TRACK_IS_BUFFERED = "TRACK_IS_BUFFERED";		//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_CACHED = "TRACK_IS_CACHED";			//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_COVERED = "TRACK_IS_COVERED";		//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_ENDED = "TRACK_IS_ENDED";			//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_PAUSED = "TRACK_IS_PAUSED";			//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_PLAYING = "TRACK_IS_PLAYING ";		//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_IS_RECORDING  = "TRACK_IS_RECORDING";	//Message includes guidSong Name/Value Pair 
    public static String STR_TRACK_IS_STARTING = "TRACK_IS_STARTING";		//Message includes guidSong Name/Value Pair
    public static String STR_TRACK_WAS_REMOVED = "TRACK_WAS_REMOVED";		//Message includes guidSong Name/Value Pair
    public static String STR_LIVE_TRACK = "LIVE_TRACK";		                //Message includes full DPXMLTrack obj
    public static String STR_EXCEPTION = "EXCEPTION";	
    

    
  //--- Resource names ------------
    private static String STR_TRACK = "Track";
    
    
  //<ResourceContainer name="TrackList>
    
    
    //<ResourceContainer name="TrackList> <Metadata> block NAMES of Name/Value Pairs
    private static String STR_TRACKLIST_STATION="station";
    private static String STR_TRACKLIST_IMAGEURL="imageurl";
    private static String STR_TRACKLIST_LIVEMEDIAURL="livemediaurl";
    private static String STR_TRACKLIST_STOPGUID="stopGuid";
    private static String STR_TRACKLIST_SESSION="session";
    private static String STR_TRACKLIST_STATIONID="stationid";
    private static String STR_TRACKLIST_BITRATE="bitrate";
    private static String STR_TRACKLIST_STARTINDEX="startindex";
    private static String STR_TRACKLIST_AUTOPLAY="autoplay";
    private static String STR_TRACKLIST_CONTINUING="continuing";
    private static String STR_TRACKLIST_SHUFFLEABLE="shuffleable";
    private static String STR_TRACKLIST_DELETEABLE="deleteable";
    private static String STR_TRACKLIST_PODCASTING="podcasting";
    private static String STR_TRACKLIST_SHOUTCASTING="shoutcasting";
    private static String STR_TRACKLIST_FLYCASTING="flycasting";
    private static String STR_TRACKLIST_FLYBACKING="flybacking";
    private static String STR_TRACKLIST_RECORDING="recording";
    private static String STR_TRACKLIST_OFFLINE="offline";
    private static String STR_TRACKLIST_THROWAWAY="throwaway";
    private static String STR_TRACKLIST_SHUFFLED="shuffled";
    private static String STR_TRACKLIST_AUTOHIDE="autohide";
    private static String STR_TRACKLIST_AUTOSHUFFLE="autoshuffle";
    private static String STR_TRACKLIST_USERS="users";
    private static String STR_TRACKLIST_EXPDAYS="expdays";
    private static String STR_TRACKLIST_EXPPLAYS="expplays";
    private static String STR_TRACKLIST_ALBUMFILE="albumfile";
    private static String STR_TRACKLIST_TIMECODE="timecode";    
 
    //<ResourceContainer name="TrackList"> <Resource name = "Track"> block NAMES of Name/Value Pairs
    private static String STR_TRACK_ARTIST="artist";			//public String artist = null;
    private static String STR_TRACK_TITLE="title";				//public String title = null;
    private static String STR_TRACK_ALBUM="album";				//public String album = null;
    private static String STR_TRACK_METADATA="metadata";		//public String metadata = null;
    private static String STR_TRACK_IMAGEURL="imageurl";		//public String imageurl = null;
    private static String STR_TRACK_MEDIAURL="mediaurl";		//public String mediaurl = null;
    private static String STR_TRACK_REDIRECT="redirect";		//public String redirect = null;
    private static String STR_TRACK_MEDIATYPE="mediatype";		//public String mediatype = null;
    private static String STR_TRACK_STARTTIME="starttime";		//public String starttime = null;
    private static String STR_TRACK_GUIDINDEX="guidIndex";		//public String guidIndex = null;
    private static String STR_TRACK_GUIDSONG="guidSong";		//public String guidSong = null;
    private static String STR_TRACK_ADURL="adurl";				//public String adurl = null;
    private static String STR_TRACK_ADDART="addart";			//public String addart = null;
    private static String STR_TRACK_TIMECODE="timecode";		//public long timecode = 0;
    private static String STR_TRACK_OFFSET="offset";			//public int offset = 0;
    private static String STR_TRACK_LENGTH="length";			//public int length = 0;
    private static String STR_TRACK_CURRENT="current";			//public int current = 0;
    private static String STR_TRACK_START="start";				//public int start = 0;
    private static String STR_TRACK_SYNCOFF="syncoff";			//public int syncoff = 0;
    private static String STR_TRACK_BITRATE="bitrate";			//public int bitrate = 99999;
    private static String STR_TRACK_SECONDS="seconds";			//public int seconds = 0;
    private static String STR_TRACK_STATIONID="stationid";		//public int stationid = 0;
    private static String STR_TRACK_EXPDAYS="expdays";			//public int expdays = -1;
    private static String STR_TRACK_EXPPLAYS="expplays";		//public int expplays = -1;
    private static String STR_TRACK_NUMPLAY="numplay";			//public int numplay = 0;
    private static String STR_TRACK_CLICKAD="clickAd";			//public boolean clickAd = false;
    private static String STR_TRACK_AUDIOAD="audioAd";			//public boolean audioAd = false;
    private static String STR_TRACK_RELOADAD="reloadAd";		//public boolean reloadAd = false;
    private static String STR_TRACK_BUFFERED="buffered";		//public boolean buffered = false;
    private static String STR_TRACK_CACHED="cached";			//public boolean cached = false;
    private static String STR_TRACK_COVERED="covered";			//public boolean covered = false;
    private static String STR_TRACK_PLAYING="playing";			//public boolean playing = false;
    private static String STR_TRACK_FLYBACK="flyback";			//public boolean flyback = false;
    private static String STR_TRACK_DELAYED="delayed";			//public boolean delayed = false;
    private static String STR_TRACK_FINISHED="finished";		//public boolean finished = false;
    private static String STR_TRACK_LISTENED="listened";		//public boolean listened = false;
    private static String STR_TRACK_PLAYED="played";			//public boolean played = false;
    private static String STR_TRACK_FLUSH="flush";				//public boolean flush = false;
    private static String STR_TRACK_REDIRECTING="redirecting";	//public boolean redirecting = false;
    private static String STR_TRACK_TERMINATING="terminating";	//public boolean terminating = false;
    private static String STR_TRACK_REDIRECTED="redirected";	//public boolean redirected = false;
    private static String STR_TRACK_UNSUPPORTED="unsupported";	//public boolean unsupported = false;
    private static String STR_TRACK_SYNCED="synced";			//public boolean synced = false;
    private static String STR_TRACK_ALBUMFILE="albumfile";		//public String albumfile = null;
    private static String STR_TRACK_FILENAME="filename";		//public String filename = null;    
    private static String STR_TRACK_INDEXINLIST="IndexInList";		//public String filename = null;
    
    //<ResourceContainer name="EXCEPTION"> block NAMES of Name/Value Pairs    
    private static String STR_EXCEPTION_ERRORMESSAGE="ErrorMessage";
    private static String STR_EXCEPTION_ERRORCODE="ErrorCode";    
    private static String STR_EXCEPTION_REQUEST="Request";      

    /*******************************************************************************    
    Example XML from DeviceProxy  

    --- A Tracklist (can have 1..n <Resource name = "Track"> blocks ---------  
    <XML>
    	<ResourceContainer name="TrackList">
    		<Metadata>
    			key Name/Value Pairs
    		</Metadata>   		
    		<Resource name = "Track">
    			key Name/Value Pairs
    		</Resource>
    	</ResourceContainer>	
    </XML>		
    	
    -- A Message with NO guidSong key Name/Value Pair ---	
    	<ResourceContainer name="RECORDING_HAS_FINISHED">
    		<Metadata/>
    		<Resource/>
    	</ResourceContainer>

    -- A Message WITH guidSong key Name/Value Pair ---
    	<ResourceContainer name="TRACK_IS_BUFFERED">"
    		<Metadata/>
    		<Resource>
    			<key name="guidSong" value="5e60ced9-a9f1-48ef-b8bc-0c2752ea906d"/>
    		</Resource>
    	</ResourceContainer>			
    		
    --- XML Container for messages can have 0..n <ResourceContainer> blocks ------	
    <XML>
    	<ResourceContainer name="RECORDING_HAS_FINISHED">
    		<Metadata/>
    		<Resource/>
    	</ResourceContainer>
    	<ResourceContainer name="TRACK_IS_BUFFERED">"
    		<Metadata/>
    		<Resource>
    			<key name="guidSong" value="5e60ced9-a9f1-48ef-b8bc-0c2752ea906d"/>
    		</Resource>
    	</ResourceContainer>
    	<ResourceContainer name="EXCEPTION">"
    		<Metadata/>
    		<Resource>
    			<key name="ErrorMessage" value="null"/>
    			<key name="ErrorCode" value="-1"/>    
    			<key name="Request" value="GET /?CMD=STARTSTATION&amp;ID=160358&amp;UID=889e07b5-7d2d-4ede-a766-c5f5a5d55368&amp;PROXYURL=http://wap.fcproxy3.broadp3.com:50004/StreamMedia.aspx?SID=8edfb94f-93fe-4559-897d-1753f2c95e0f&amp;CMD=PLAY_STATION&amp;FILENAMEONLY=TRUE&amp;BITRATE=24&amp;MD=1&amp;StationName=FLYTUNES%20idobi%20Radio%20Lo&amp;PLAYSTREAM=true&amp;SERVER_TIME=Thu,%2003%20Dec%202009%2018:42:31%20GMT&amp;ext=.mp3&amp;RS=0 HTTP/1.1"/> 	
    		</Resource>
    	</ResourceContainer>    	
    </XML>
    ************************************************************************************/     
    
    

    public DPXMLParser(String xml)
    {
        _xml = xml;
        //tracklist = null;
        messagelist = null;
    }

    public String parse()
    {
    	String tStr = "";
        if( _xml == null ){
        	tStr = TAG_DPXMLParser +  " Error: NULL Source XML String";
        	if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, tStr);
        	return tStr;   	
        }

        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            handler = new xmlHandler();
            ByteArrayInputStream bais = new ByteArrayInputStream(_xml.getBytes());
            saxParser.parse( bais, handler );
            //if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "Complete");
            return "";
        }
        catch (Exception e) { 
        	tStr = TAG_DPXMLParser +  " Exception: " + e.getMessage();
        	System.err.println(tStr);
			if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, tStr);
        	return tStr;
        }
    }

    class xmlHandler extends DefaultHandler
    {
        public xmlHandler() {}

        public void startDocument() throws SAXException {}

        public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException 
        {
        	if (tagName.equals("")==true) tagName = localName; //Added this line after switch to Android 1.0 SDK, still needed(??) 

        	if(tagName.equals(STR_XML)) {
        		//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=XML"); 
        	}
        	else if(tagName.equals(STR_RESOURCECONTAINER)) {
        		String rcname = attributes.getValue(0);
            	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=RESOURCECONTAINER, Name=" + rcname);
        		
            	if(rcname.equals(STR_TRACKLIST)) {
//        			if (tracklist == null){
//    	          		tracklist = new DPXMLTracklist();        				
//        			}
        			if (messagelist == null){
    	          		messagelist = new DPXMLMessageList();        				
        			}        			
        			curmessage = new DPXMLMessage();
        			curmessage.name = rcname;  
        			curmessage.tracklist = new DPXMLTracklist(); 
        			In_ResourceContainer_Tracklist = true;
	            }
        		else if ((rcname.equals(STR_RECORDING_HAS_FINISHED)) ||
        				(rcname.equals(STR_RECORDING_HAS_STARTED)) ||
        				(rcname.equals(STR_RETRYING_CONNECTION)) ||
        				(rcname.equals(STR_STREAM_IS_BUFFERED)) ||
        				(rcname.equals(STR_TRACK_WAS_ADDED)) ||
        				(rcname.equals(STR_TRACKLIST_EMPTY))) {
        			//MESSAGES that DO NOT include guidSong Name/Value Pair
        			if (messagelist == null){
    	          		messagelist = new DPXMLMessageList();        				
        			}
        			curmessage = new DPXMLMessage();
        			curmessage.name = rcname;
        		    In_ResourceContainer_MessageNoGuidSong = true;         			
        		}
        		else if ((rcname.equals(STR_TRACK_HAS_UPDATED)) ||
        				(rcname.equals(STR_TRACK_IS_BUFFERED)) ||
        				(rcname.equals(STR_TRACK_IS_CACHED)) ||
        				(rcname.equals(STR_TRACK_IS_COVERED)) ||
        				(rcname.equals(STR_TRACK_IS_ENDED)) ||
        				(rcname.equals(STR_TRACK_IS_PAUSED)) ||
        				(rcname.equals(STR_TRACK_IS_PLAYING)) ||        				
        				(rcname.equals(STR_TRACK_IS_RECORDING)) ||        				
        				(rcname.equals(STR_TRACK_IS_STARTING)) ||        				
        				(rcname.equals(STR_TRACK_WAS_REMOVED))) {
        			//MESSAGES that DO include guidSong Name/Value Pair
        			if (messagelist == null){
    	          		messagelist = new DPXMLMessageList();        				
        			}   
        			curmessage = new DPXMLMessage();
        			curmessage.name = rcname;
        		    In_ResourceContainer_MessageWithGuidSong = true;  
        		}
        		else if (rcname.equals(STR_LIVE_TRACK)) {
	//        		if (tracklist == null){
	//	          		tracklist = new DPXMLTracklist();        				
	//    			}
	    			if (messagelist == null){
		          		messagelist = new DPXMLMessageList();        				
	    			}        			
	    			curmessage = new DPXMLMessage();
	    			curmessage.name = rcname;  
	    			curmessage.tracklist = new DPXMLTracklist(); 
	    			In_ResourceContainer_Tracklist = true; 
        		}
        		else if (rcname.equals(STR_EXCEPTION)) {
        			if (messagelist == null){
    	          		messagelist = new DPXMLMessageList();        				
        			}   
        			curmessage = new DPXMLMessage();
        			curmessage.name = rcname;
        		    In_ResourceContainer_Exception = true;  
        		}            	
            	
        	}
        	else if(tagName.equals(STR_METADATA)) {
        		{
                	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=METADATA");  
                	if (In_ResourceContainer_Tracklist == true) {
                		curmessage.tracklist = new DPXMLTracklist();	//Create tracklist object once we find <Metadata> block        			
            			In_Tracklist_Metadata = true;               		
                	}
	            }
			}
        	else if(tagName.equals(STR_RESOURCE)) {
        		if (attributes.getLength()>0){
            		String resname = attributes.getValue(0);  
                	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=RESOURCE, Name=" + resname );
            		if(resname.equals(STR_TRACK)) {
    	          		curtrack = new DPXMLTrack();        			
            			In_Resource_Track = true;
    	            }            		
        		}
        		else {
        			if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=RESOURCE, NO NAME");
//					if (In_ResourceContainer_MessageWithGuidSong == true) {
//
//					}       			
        		}
        	}  
        	else if(tagName.equals(STR_KEY)) {
        		curname = attributes.getValue(0);
        		curvalue = attributes.getValue(1);
            	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "startElement tagName=KEY, Name=" + curname + " Value=" + curvalue);         		
        		
     			//--- ResourceContainer, name="Tracklist",  "Metadata" block (1 per Tracklist) ---------------        	
            	if (In_Tracklist_Metadata == true) {
                	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "In_Tracklist_Metadata Name=" + curname + " Value=" + curvalue);        	
                	if(curname.equals(STR_TRACKLIST_STATION))
                    {
                		curmessage.tracklist.station = curvalue;
                    }					
                    else if(curname.equals(STR_TRACKLIST_IMAGEURL))
                    {
                    	curmessage.tracklist.imageurl = curvalue;
                    }				
                    else if(curname.equals(STR_TRACKLIST_LIVEMEDIAURL))
                    {
                    	curmessage.tracklist.livemediaurl = curvalue;
                    }				
                    else if(curname.equals(STR_TRACKLIST_STOPGUID))
                    {
                    	curmessage.tracklist.stopGuid = curvalue;
                    }				
                    else if(curname.equals(STR_TRACKLIST_SESSION))
                    {
                    	curmessage.tracklist.session = curvalue;
                    }				
                    else if(curname.equals(STR_TRACKLIST_STATIONID))
                    {
                    	curmessage.tracklist.stationid = Integer.parseInt(curvalue);
                    }				
                    else if(curname.equals(STR_TRACKLIST_BITRATE))
                    {
                    	curmessage.tracklist.bitrate = Integer.parseInt(curvalue);
                    }				
                    else if(curname.equals(STR_TRACKLIST_STARTINDEX))
                    {
                    	curmessage.tracklist.startindex = Integer.parseInt(curvalue);
                    }				
                    else if(curname.equals(STR_TRACKLIST_AUTOPLAY))
                    {
                    	curmessage.tracklist.autoplay = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_CONTINUING))
                    {
                    	curmessage.tracklist.continuing = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_SHUFFLEABLE))
                    {
                    	curmessage.tracklist.shuffleable = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_DELETEABLE))
                    {
                    	curmessage.tracklist.deleteable = Boolean.parseBoolean(curvalue); 
                    }					
                    else if(curname.equals(STR_TRACKLIST_PODCASTING))
                    {
                    	curmessage.tracklist.podcasting = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_SHOUTCASTING))
                    {
                    	curmessage.tracklist.shoutcasting = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_FLYCASTING))
                    {
                    	curmessage.tracklist.flycasting = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_FLYBACKING))
                    {
                    	curmessage.tracklist.flybacking = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_RECORDING))
                    {
                    	curmessage.tracklist.recording = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_OFFLINE))
                    {
                    	curmessage.tracklist.offline = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_THROWAWAY))
                    {
                    	curmessage.tracklist.throwaway = Boolean.parseBoolean(curvalue); 
                    }					
                    else if(curname.equals(STR_TRACKLIST_SHUFFLED))
                    {
                    	curmessage.tracklist.shuffled = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_AUTOHIDE))
                    {
                    	curmessage.tracklist.autohide = Boolean.parseBoolean(curvalue); 
                    }					
                    else if(curname.equals(STR_TRACKLIST_AUTOSHUFFLE))
                    {
                    	curmessage.tracklist.autoshuffle = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_USERS))
                    {
                    	curmessage.tracklist.users = Boolean.parseBoolean(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_EXPDAYS))
                    {
                    	curmessage.tracklist.expdays = Integer.parseInt(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_EXPPLAYS))
                    {
                    	curmessage.tracklist.expplays = Integer.parseInt(curvalue); 
                    }				
                    else if(curname.equals(STR_TRACKLIST_ALBUMFILE))
                    {
                    	curmessage.tracklist.albumfile = curvalue; 
                    }				
                    else if(curname.equals(STR_TRACKLIST_TIMECODE))
                    {
                    	curmessage.tracklist.timecode = Long.parseLong(curvalue); 
                    }         		
            	}	//if (In_Tracklist_Metadata == true)
                    
                //--- Resource block, name = "Track" (1 or more in each Tracklist) ------------------
           	if (In_Resource_Track == true) {
                	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "In_Resource_Track Name=" + curname + " Value=" + curvalue);        	
                	if(curname.equals(STR_TRACK_ARTIST))
                    {
                		curtrack.artist = curvalue;
                    }        		
                	else if(curname.equals(STR_TRACK_TITLE))
                    {
                		curtrack.title = curvalue;
                    }        		
                	else if(curname.equals(STR_TRACK_ALBUM))
                    {
                		curtrack.album = curvalue;
                    }        		
                	else if(curname.equals(STR_TRACK_METADATA))
                    {
                		curtrack.metadata = curvalue;
                    }           		
                	else if(curname.equals(STR_TRACK_IMAGEURL))
                    {
                		curtrack.imageurl = curvalue;
                    }         		
                	else if(curname.equals(STR_TRACK_MEDIAURL))
                    {
                		curtrack.mediaurl = curvalue;
                    }          		
                	else if(curname.equals(STR_TRACK_REDIRECT))
                    {
                		curtrack.redirect = curvalue;
                    }         		
                	else if(curname.equals(STR_TRACK_MEDIATYPE))
                    {
                		curtrack.mediatype = curvalue;
                    }         		
                	else if(curname.equals(STR_TRACK_STARTTIME))
                    {
                		curtrack.starttime = curvalue;
                    }         		
                	else if(curname.equals(STR_TRACK_GUIDINDEX))
                    {
                		curtrack.guidIndex = curvalue;
                    }        		
                	else if(curname.equals(STR_TRACK_GUIDSONG))
                    {
                		curtrack.guidSong = curvalue;
                    }          		
                	else if(curname.equals(STR_TRACK_ADURL))
                    {
                		curtrack.adurl = curvalue;
                    }       		
                	else if(curname.equals(STR_TRACK_ADDART))
                    {
                		curtrack.addart = curvalue;
                    }        		
                	else if(curname.equals(STR_TRACK_TIMECODE))
                    {
                		curtrack.timecode = Long.parseLong(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_OFFSET))
                    {
                		curtrack.offset = Integer.parseInt(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_LENGTH))
                    {
                		curtrack.length = Integer.parseInt(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_CURRENT))
                    {
                		curtrack.current = Integer.parseInt(curvalue);
                    }          		
                	else if(curname.equals(STR_TRACK_START))
                    {
                		curtrack.start = Integer.parseInt(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_SYNCOFF))
                    {
                		curtrack.syncoff = Integer.parseInt(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_BITRATE))
                    {
                		curtrack.bitrate = Integer.parseInt(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_SECONDS))
                    {
                		curtrack.seconds = Integer.parseInt(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_STATIONID))
                    {
                		curtrack.stationid = Integer.parseInt(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_EXPDAYS))
                    {
                		curtrack.expdays = Integer.parseInt(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_EXPPLAYS))
                    {
                		curtrack.expplays = Integer.parseInt(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_NUMPLAY))
                    {
                		curtrack.numplay = Integer.parseInt(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_CLICKAD))
                    {
                		curtrack.clickAd = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_AUDIOAD))
                    {
                		curtrack.audioAd = Boolean.parseBoolean(curvalue);
                    }          		
                	else if(curname.equals(STR_TRACK_RELOADAD))
                    {
                		curtrack.reloadAd = Boolean.parseBoolean(curvalue);
                    }          		
                	else if(curname.equals(STR_TRACK_BUFFERED))
                    {
                		curtrack.buffered = Boolean.parseBoolean(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_CACHED))
                    {
                		curtrack.cached = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_COVERED))
                    {
                		curtrack.covered = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_PLAYING))
                    {
                		curtrack.playing = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_FLYBACK))
                    {
                		curtrack.flyback = Boolean.parseBoolean(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_DELAYED))
                    {
                		curtrack.delayed = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_FINISHED))
                    {
                		curtrack.finished = Boolean.parseBoolean(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_LISTENED))
                    {
                		curtrack.listened = Boolean.parseBoolean(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_PLAYED))
                    {
                		curtrack.played = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_FLUSH))
                    {
                		curtrack.flush = Boolean.parseBoolean(curvalue);
                    }        		
                	else if(curname.equals(STR_TRACK_REDIRECTING))
                    {
                		curtrack.redirecting = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_TERMINATING))
                    {
                		curtrack.terminating = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_REDIRECTED))
                    {
                		curtrack.redirected = Boolean.parseBoolean(curvalue);
                    }          		
                	else if(curname.equals(STR_TRACK_UNSUPPORTED))
                    {
                		curtrack.unsupported = Boolean.parseBoolean(curvalue);
                    }       		
                	else if(curname.equals(STR_TRACK_SYNCED))
                    {
                		curtrack.synced = Boolean.parseBoolean(curvalue);
                    }         		
                	else if(curname.equals(STR_TRACK_ALBUMFILE))
                    {
                		curtrack.albumfile = curvalue;
                    }         		
                	else if(curname.equals(STR_TRACK_FILENAME))
                    {
                		curtrack.filename = curvalue;
                    }   
                	else if(curname.equals(STR_TRACK_INDEXINLIST))
                    {
                		curtrack.IndexInList = Integer.parseInt(curvalue, 10) ;
                    }   
            	}	//if (In_Resource_Track == true) 
           	
				if (In_ResourceContainer_MessageNoGuidSong == true) {
	           		//ResourceContainer, name=<one of the messages>,  <Resource> block empty            	
  
					
				}	//
				if (In_ResourceContainer_MessageWithGuidSong == true) {
                	if(curname.equals(STR_TRACK_GUIDSONG))
                    {
                		//ResourceContainer, name=<one of the messages>,  <Resource> Key/Value contains guidSong  
                		curmessage.guidSong = curvalue;
                    }
				}	//
				if (In_ResourceContainer_Exception == true) {
					if(curname.equals(STR_EXCEPTION_ERRORMESSAGE))
						curmessage.ErrorMessage = curvalue;
					else if(curname.equals(STR_EXCEPTION_ERRORCODE))
						curmessage.ErrorCode = curvalue;					
					else if(curname.equals(STR_EXCEPTION_REQUEST))
						curmessage.Request = curvalue;					
				}	//				
        	}
        	else {
        		//Unrecognized tagName
        		if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "UNRECOGNIZED tagName=" + tagName); 
        	}
        }	//startElement(

        
        public void characters(char[] ch, int start, int length) throws SAXException
        {
        }

        public void endElement(String uri, String localName, String tagName) throws SAXException
        {
        	if (tagName.equals("")) tagName = localName; //BH_12-18-08 Added this line after switch to Android 1.0 SDK 
        	if(tagName.equals(STR_XML))
        	{
        		//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "endElement tagName=" + tagName);
        	}       	
        	else if(tagName.equals(STR_RESOURCECONTAINER))
        	{
        		//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "endElement tagName=" + tagName);        		
        		if (In_ResourceContainer_Tracklist == true) {
        			In_ResourceContainer_Tracklist = false;
        			messagelist.children.addElement(curmessage);	//Store this Message
        			curmessage = null;
        		}
        		if (In_ResourceContainer_MessageNoGuidSong == true) {
        			In_ResourceContainer_MessageNoGuidSong = false;
        			messagelist.children.addElement(curmessage);	//Store this Message
        			curmessage = null;
        		}
        		if (In_ResourceContainer_MessageWithGuidSong == true) {
        			In_ResourceContainer_MessageWithGuidSong = false;
        			messagelist.children.addElement(curmessage);	//Store this Message
        			curmessage = null;
        		}
        		if (In_ResourceContainer_Exception == true) {
        			In_ResourceContainer_Exception = false;
        			messagelist.children.addElement(curmessage);	//Store this Message
        			curmessage = null;
        		}        		
        	}
        	else if(tagName.equals(STR_METADATA))
        	{
        		//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "endElement tagName=" + tagName);
        		if (In_Tracklist_Metadata == true){
            		In_Tracklist_Metadata = false;        			
        		}
        	}
        	else if(tagName.equals(STR_RESOURCE))
        	{
        		//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "endElement tagName=" + tagName);
        		if (In_Resource_Track == true){
            		In_Resource_Track = false;
            		if (In_ResourceContainer_Tracklist == true) {
            			curmessage.tracklist.children.insertElementAt(curtrack, curtrack.IndexInList);
            			//curmessage.tracklist.children.addElement(curtrack);           			
            		}
            		else {
            			curmessage.track = new DPXMLTrack();
            			curmessage.track = curtrack;
            		}
                    curtrack = null;
        		}
        		
//        		if (In_ResourceContainer_MessageWithGuidSong == true) {
//        			
//        		}
        	}        	
        }

        public void endDocument() throws SAXException {
        	//if(Debug.isDebuggerConnected()) Log.d(TAG_DPXMLParser, "endDocument");        	
        }
    }	//class xmlHandler extends DefaultHandler
}

