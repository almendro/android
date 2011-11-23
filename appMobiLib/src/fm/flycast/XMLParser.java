package fm.flycast;
import java.io.ByteArrayInputStream;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

public class XMLParser
{
    private String _xml = null;
    private xmlHandler handler = null;
    private Stack<XMLDirectory> nodeStack = new Stack<XMLDirectory>();
    private Stack<String> tagStack = new Stack<String>();
    public XMLDirectory directory = null;
    public XMLMetadata metadata = null;
    public XMLTracklist tracklist = null; 	//BH_09-01-09
    public XMLTrack curtrack = null;		//BH_09-01-09


    private static String STR_TRACKINFO = "trackinfo";
    private static String STR_DIR = "DIR";
    private static String STR_NAME = "name";
    private static String STR_ID = "id";
    private static String STR_COLOR = "color";
    private static String STR_TITLE = "title";
    private static String STR_ADIMG = "adimg";
    private static String STR_FLYBACK = "flyback";//BH_09-01-09
    private static String STR_ADID = "adid";	//BH_02-18-09 added this
    private static String STR_NODE = "NOD";
    private static String STR_URL = "url";
    private static String STR_SID = "sid";
    private static String STR_SKIP = "skip";
    private static String STR_TOP = "top";
    private static String STR_INFO = "info";
    private static String STR_VALUE = "value";
    private static String STR_ADURL = "adurl";
    private static String STR_ALIGN = "align";	//BH_09-01-09
    private static String STR_TYPE = "type";
    private static String STR_PLAYER = "player";
    private static String STR_FAIL = "FAIL";
    private static String STR_MESSAGE = "message";
    private static String STR_METADATA = "metadata";
    private static String STR_ARTIST = "artist";
    private static String STR_ALBUM = "album";
    private static String STR_HEIGHT = "height";//BH_02-18-09 added this
    private static String STR_WIDTH = "width";	//BH_02-18-09 added this
    private static String STR_LOCAL = "local";	//BH_09-01-09
    private static String STR_SHOUT = "shout";	//BH_09-01-09
    private static String STR_JACKETURL = "jacketurl";
    private static String STR_SALESHTML = "salesHTML";
    private static String STR_MEDIUMIMAGE = "MediumImageURL"; //BH_09-01-09
    private static String STR_DESCRIPTION = "description";	//BH_12-18-08 added this
    private static String STR_IMAGE = "image";//BH_09-01-09
    private static String STR_LOCATION = "location";//BH_09-01-09
    private static String STR_TRACK = "track";//BH_09-01-09
    private static String STR_TRACKLIST = "tracks";//BH_09-01-09
    private static String STR_STATION = "station";//BH_09-01-09
    private static String STR_BITRATE = "bitrate";//BH_09-01-09
    private static String STR_ALLOWSKIP = "allowskip";//BH_09-01-09
    private static String STR_MINBACK = "minback";//BH_09-01-09
    private static String STR_ONE = "1";//BH_09-01-09
    private static String STR_PID = "pid";//BH_09-01-09
    private static String STR_STARTINDEX = "startIndex";//BH_09-01-09
    private static String STR_AUTOPLAY = "autoplay";//BH_09-01-09
    private static String STR_CONTINUE = "continue";//BH_09-01-09
    private static String STR_ADDART = "addart";//BH_09-01-09
    private static String STR_EXPDAYS = "expdays";//BH_09-01-09
    private static String STR_EXPPLAYS = "expplays";//BH_09-01-09
    private static String STR_DELETEOK = "deleteOK";//BH_09-01-09
    private static String STR_SHUFFLEOK = "shuffleOK";//BH_09-01-09
    private static String STR_AUTOHIDE = "autoHide";//BH_09-01-09
    private static String STR_AUTOSHUFFLE = "autoShuffle";//BH_09-01-09
    private static String STR_SHOWAD = "showad";//BH_09-01-09    
    private static String STR_ADBANNERZONE = "ad.banner.zone";
    private static String STR_ADBANNERWIDTH = "ad.banner.width";
    private static String STR_ADBANNERHEIGHT = "ad.banner.height";
    private static String STR_ADBANNERFREQUENCY = "ad.banner.frequency";
    private static String STR_ADPREROLLZONE = "ad.preroll.zone";
    private static String STR_ADPREROLLWIDTH = "ad.preroll.width";
    private static String STR_ADPREROLLHEIGHT = "ad.preroll.height";
    private static String STR_ADPREROLLFREQUENCY = "ad.preroll.frequency";
    private static String STR_ADPOPUPZONE = "ad.popup.zone";
    private static String STR_ADPOPUPWIDTH = "ad.popup.width";
    private static String STR_ADPOPUPHEIGHT = "ad.popup.height";
    private static String STR_ADPOPUPFREQUENCY = "ad.popup.frequency";
    private static String STR_ADINTERSTITIALZONE = "ad.interstitial.zone";
    private static String STR_ADINTERSTITIALWIDTH = "ad.interstitial.width";
    private static String STR_ADINTERSTITIALHEIGHT = "ad.interstitial.height";
    private static String STR_ADINTERSTITIALFREQUENCY = "ad.interstitial.frequency";
    private static String STR_ADSIGNUPZONE = "ad.signup.zone";
    private static String STR_ADSIGNUPWIDTH = "ad.signup.width";
    private static String STR_ADSIGNUPHEIGHT = "ad.signup.height";
    private static String STR_ADSIGNUPFREQUENCY = "ad.signup.frequency";

    private static String STR_ADPAGE = "adpage";	//BH_06-05-09 added this


    public XMLParser(String xml)
    {
        _xml = xml;
        directory = null;
    }

    public boolean parse()
    {
        if( _xml == null ) return false;

        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            handler = new xmlHandler();
            ByteArrayInputStream bais = new ByteArrayInputStream(_xml.getBytes());
            saxParser.parse( bais, handler );
        }
        catch (Exception e) { 
        	System.err.println(e.getMessage());
			if(Debug.isDebuggerConnected()) Log.d("XMLParser", "Exception " + e.getMessage());
        }

        return true;
    }

    class xmlHandler extends DefaultHandler
    {
        public xmlHandler()
        {
        }

        @Override
		public void startDocument() throws SAXException {}

        @Override
		public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException
        {
        	if (tagName.equals("")) tagName = localName; //BH_12-18-08 Added this line after switch to Android 1.0 SDK 
            if(tagName.equals(STR_TRACKINFO))
            {
                metadata = new XMLMetadata();
            }
            else if(tagName.equals(STR_STATION))	//BH_09-01-09
            {										//BH_09-01-09
                if( tracklist == null )				//BH_09-01-09
                    tracklist = new XMLTracklist();	//BH_09-01-09
            }										//BH_09-01-09
            else if(tagName.equals(STR_TRACKLIST))	//BH_09-01-09
            {										//BH_09-01-09
                if( tracklist == null )				//BH_09-01-09
                    tracklist = new XMLTracklist();	//BH_09-01-09
            }										//BH_09-01-09
            else if(tagName.equals(STR_TRACK))		//BH_09-01-09
            {										//BH_09-01-09
                curtrack = new XMLTrack();			//BH_09-01-09
            }										//BH_09-01-09
            else if(tagName.equals(STR_DIR))
            {
                XMLDirectory dir = new XMLDirectory();

                int length = attributes.getLength();
                for( int i = 0; i < length; i++ )
                {
                    String name = attributes.getLocalName(i);
                    if(name.equals(STR_NAME))
                    {
                        dir.dirname = attributes.getValue(i);
                    }
                    else if(name.equals(STR_DESCRIPTION))		//BH_09-01-09
                    {											//BH_09-01-09
                        dir.dirdesc = attributes.getValue(i);	//BH_09-01-09
                    }											//BH_09-01-09
                  
                    else if(name.equals(STR_MESSAGE))
                    {
                        dir.dirmessage = attributes.getValue(i);
                    }
                    else if(name.equals(STR_VALUE))
                    {
                        dir.dirvalue = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ID))
                    {
                        dir.dirid = attributes.getValue(i);
                    }
                    else if(name.equals(STR_COLOR))
                    {
                        dir.dircolor = attributes.getValue(i);
                    }
                    else if(name.equals(STR_TITLE))
                    {
                        dir.dirtitle = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADIMG))
                    {
                        dir.diradimg = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADID))	//BH_02-18-09 added "adid" case here
                    {
                    	dir.adid = attributes.getValue(i);//is diradid in B/B
                    }
                    else if(name.equals(STR_ADDART))			//BH_09-01-09
                    {											//BH_09-01-09
                        dir.diraddart = attributes.getValue(i);	//BH_09-01-09
                    }											//BH_09-01-09
					else if(name.equals(STR_PID))				//BH_09-01-09
                    {											//BH_09-01-09
                        dir.dirpid = attributes.getValue(i);	//BH_09-01-09
                    }											//BH_09-01-09
                    else if(name.equals(STR_ALIGN))				//BH_09-01-09
                    {											//BH_09-01-09
                        dir.diralign = attributes.getValue(i);	//BH_09-01-09
                    }											//BH_09-01-09
                    else if(name.equals(STR_HEIGHT))	//BH_02-18-09 added "height" case here
                    {
                    	dir.height = attributes.getValue(i);//BB is "dirheight"
                    }
                    else if(name.equals(STR_WIDTH))		//BH_06-05-09 added "width" case here
                    {
                    	dir.dirwidth = Integer.parseInt(attributes.getValue(i));//BH_09-02-09 added ParseInt
                    }                   
                    else if(name.equals(STR_ADPAGE))	//BH_06-05-09 added "adpage" case here
                    {
                    	dir.adpage = attributes.getValue(i);
                    }                 
				}
                if( directory == null )
                {
                    directory = dir;
                }
                else
                {
                    XMLDirectory curdir = nodeStack.peek();
                    curdir.children.addElement(dir);
                    dir.parent = curdir;
                }

                nodeStack.push(dir);
            }
            else if(tagName.equals(STR_NODE))
            {
                XMLDirectory dir = nodeStack.peek();
                XMLNode node = new XMLNode();

                int length = attributes.getLength();
                for( int i = 0; i < length; i++ )
                {
                    String name = attributes.getLocalName(i);
                    if(name.equals(STR_NAME))
                    {
                        node.nodename = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ID))
                    {
                        node.nodeid = attributes.getValue(i);
                    }
                    else if(name.equals(STR_URL))
                    {
                        node.nodeurl = attributes.getValue(i);
						node.noderawurl = attributes.getValue(i);//BH_09-02-09
                    }
                    else if(name.equals(STR_SID))
                    {
                        node.nodesid = attributes.getValue(i);
                    }
                    else if(name.equals(STR_PLAYER))
                    {
                        node.nodeplayer = attributes.getValue(i);
                    }
                    else if(name.equals(STR_DESCRIPTION))	//BH_12-18-08 added "description" case here
                    {
                    	node.nodedesc = attributes.getValue(i);
                    }                      
                    else if(name.equals(STR_COLOR))
                    {
                        node.nodecolor = attributes.getValue(i);
                    }
                    else if(name.equals(STR_SKIP))
                    {
                        node.nodeskip = attributes.getValue(i);
                    }
                    else if(name.equals(STR_TOP))
                    {
                        node.nodetop = attributes.getValue(i);
                    }
                    else if(name.equals(STR_INFO))
                    {
                        node.nodeinfo = attributes.getValue(i);
                    }
                    else if(name.equals(STR_TYPE))
                    {
                        node.nodetype = attributes.getValue(i);
                    }
                    else if(name.equals(STR_SHOUT))				//BH_09-01-09
                    {											//BH_09-01-09
                        node.nodeshout = attributes.getValue(i);//BH_09-01-09
                    }											//BH_09-01-09
 					else if(name.equals(STR_ADIMG))				//BH_09-01-09
                    {											//BH_09-01-09
                        node.nodeadimg = attributes.getValue(i);//BH_09-01-09
                    }											//BH_09-01-09
                    else if(name.equals(STR_ADID))	//BH_02-18-09 added "adid" case here
                    {
                    	node.adid = attributes.getValue(i);//nodeadid in B/B
					}
                    else if(name.equals(STR_ADURL))
                    {
                        node.nodeadurl = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPAGE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adpage = attributes.getValue(i);
                    }
                    
                    
                    
                    
                    
                    
                    

                    else if(name.equals(STR_ADBANNERZONE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adbannerzone = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADBANNERWIDTH))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adbannerwidth = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADBANNERHEIGHT))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adbannerheight = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADBANNERFREQUENCY))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adbannerfreq = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPREROLLZONE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adprerollzone = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPREROLLWIDTH))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adprerollwidth = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPREROLLHEIGHT))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adprerollheight = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPREROLLFREQUENCY))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adprerollfreq = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPOPUPZONE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adpopupzone = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPOPUPWIDTH))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adpopupwidth = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPOPUPHEIGHT))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adpopupheight = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADPOPUPFREQUENCY))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adpopupfreq = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADINTERSTITIALZONE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adinterzone = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADINTERSTITIALWIDTH))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adinterwidth = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADINTERSTITIALHEIGHT))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adinterheight = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADINTERSTITIALFREQUENCY))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adinterfreq = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADSIGNUPZONE))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adsignupzone = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADSIGNUPWIDTH))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adsignupwidth = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADSIGNUPHEIGHT))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adsignupheight = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ADSIGNUPFREQUENCY))	//BH_06-05-09 added "adpage" case here
                    {
                    	node.adsignupfreq = attributes.getValue(i);
                    }
                    
                    
                    
                    


					//-- BH_09-01-09 begins ---------
                    else if(name.equals(STR_HEIGHT))
                    {
                        node.height = attributes.getValue(i);//BB is nodeheight = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_WIDTH))
                    {
                        node.width = attributes.getValue(i);//BB is nodewidth = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_TITLE))
                    {
                        node.nodetitle = attributes.getValue(i);
                    }
                    else if(name.equals(STR_VALUE))
                    {
                        node.nodevalue = attributes.getValue(i);
                    }
                    else if(name.equals(STR_LOCAL))
                    {
                        node.nodelocal = attributes.getValue(i);
                    }
                    else if(name.equals(STR_PID))
                    {
                        node.nodepid = attributes.getValue(i);
                    }
                    else if(name.equals(STR_ALIGN))
                    {
                        node.nodealign = attributes.getValue(i);
                    }
                    else if(name.equals(STR_MINBACK))
                    {
                        node.nodeminback = attributes.getValue(i);
                    }
                    else if(name.equals(STR_FLYBACK))
                    {
                        node.isFlyBack = (attributes.getValue(i) != null && attributes.getValue(i).length() > 0);
                    }
                    else if(name.equals(STR_EXPDAYS))
                    {
                        node.nodeexpdays = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_EXPPLAYS))
                    {
                        node.nodeexpplays = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_DELETEOK))
                    {
                        node.nodeallowdelete = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_SHUFFLEOK))
                    {
                        node.nodeallowshuffle = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_AUTOHIDE))
                    {
                        node.nodeautohide = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_AUTOSHUFFLE))
                    {
                        node.nodeautoshuffle = Integer.parseInt(attributes.getValue(i));
                    }
                    else if(name.equals(STR_SHOWAD))
                    {
                        node.nodeshowad = Integer.parseInt(attributes.getValue(i));
                    }
					//-- BH_09-01-09 ENDS ---------
                }

                dir.children.addElement(node);
            }
            else if(tagName.equals(STR_FAIL))
            {
                XMLDirectory dir;
                if( nodeStack.size() == 0 )
                {
                    directory = new XMLDirectory();
                    dir = directory;
                }
                else
                {
                    dir = nodeStack.peek();
                }
                XMLFail fail = new XMLFail();

                int length = attributes.getLength();
                for( int i = 0; i < length; i++ )
                {
                    String name = attributes.getLocalName(i);
                    if(name.equals(STR_MESSAGE))
                    {
                        fail.message = attributes.getValue(i);
                    }
                }

                dir.children.addElement(fail);
            }

            tagStack.push(tagName);
        }

        @Override
		public void characters(char[] ch, int start, int length) throws SAXException
        {
            if( metadata != null )
            {
                if(tagStack.peek().equals(STR_METADATA))
                {
                    metadata.metadata = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_ARTIST))
                {
                    metadata.artist = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_TITLE))
                {
                    metadata.title = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_ALBUM))
                {
                    metadata.album = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_JACKETURL))
                {
                    metadata.jacketurl = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_MEDIUMIMAGE))	//BH_09-02-09
                {															//BH_09-02-09
                    metadata.jacketurl = new String(ch, start, length);		//BH_09-02-09
                }															//BH_09-02-09
                else if(tagStack.peek().equals(STR_SALESHTML))
                {
                    metadata.salesHTML = new String(ch, start, length);
                }
            }
			//BEGIN BH_09-02-09
            else if( curtrack != null )
            {
                if(tagStack.peek().equals(STR_ARTIST))
                {
                    curtrack.artist = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_TITLE))
                {
                    curtrack.title = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_ALBUM))
                {
                    curtrack.album = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_IMAGE))
                {
                    curtrack.imageurl = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_LOCATION))
                {
                    //String url = new String(ch, start, length);
                    //curtrack.mediaurl = m_app.URLdecode( url );
                }
            }
            else if( tracklist != null )
            {
                if(tagStack.peek().equals(STR_NAME))
                {
                    tracklist.station = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_IMAGE))
                {
                    tracklist.imageurl = new String(ch, start, length);
                }
                else if(tagStack.peek().equals(STR_BITRATE))
                {
                    tracklist.bitrate = Integer.parseInt(new String(ch, start, length));
                }
                else if(tagStack.peek().equals(STR_STARTINDEX))
                {
                    tracklist.startindex = Integer.parseInt(new String(ch, start, length));
                }
                else if(tagStack.peek().equals(STR_ALLOWSKIP))
                {
                    String allow = new String(ch, start, length);
                    tracklist.shuffleable = allow.equals(STR_ONE);
                }
                else if(tagStack.peek().equals(STR_AUTOPLAY))
                {
                    String allow = new String(ch, start, length);
                    tracklist.autoplay = allow.equals(STR_ONE);
                }
                else if(tagStack.peek().equals(STR_CONTINUE))
                {
                    String allow = new String(ch, start, length);
                    tracklist.continuing = allow.equals(STR_ONE);
                }
            }
		//--- END BH_09-02-09 ---------
        }

        @Override
		public void endElement(String uri, String localName, String tagName) throws SAXException
        {
        	if (tagName.equals("")) tagName = localName; //BH_12-18-08 Added this line after switch to Android 1.0 SDK 
        	tagStack.pop();
        	if(tagName.equals(STR_DIR))
        	{
        		nodeStack.pop();
        	}
            if(tagName.equals(STR_TRACK))				//BH_09-02-09
            {											//BH_09-02-09
                tracklist.children.addElement(curtrack);//BH_09-02-09
                curtrack = null;						//BH_09-02-09
            }											//BH_09-02-09
        }

        @Override
		public void endDocument() throws SAXException { }
    }
}

