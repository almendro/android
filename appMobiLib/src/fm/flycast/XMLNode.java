package fm.flycast;
//-- Following version ported from Rosco's Blackberry 1.2x code 12-18-08, then 2.x code 09-01-09

import android.graphics.drawable.Drawable;

public class XMLNode extends XMLObject
{
    public String nodename = null;	//name - Main name of this node
    public String nodeid = null;
    public String nodeurl = null;
    public String noderawurl = null;//BH_09-01-09
    public String nodemeta = null;
    public String noderotator = null;//BH_09-01-09
    public String nodesid = null;
    public String nodeplayer = null;
    public String nodedesc = null;	//description (small subtext) //BH_12-18-08 added this back in, wasn't used in Blackberry

    public String nodecolor = null;

    public String nodeskip = null;
    public String nodetop = null;
    public String nodeinfo = null;	//Url to station info, if info exists show info icon, click to show it
    public String nodetype = null;
    public String nodeshout = null;		//BH_09-01-09
    public String nodeshouturl = null;	//BH_09-01-09
    public String nodetitle = null;		//BH_09-01-09
    public String nodelocal = null;		//BH_09-01-09    
	public String nodevalue = null;
	public String nodeadimg = null;
    public String nodeadurl = null;
    public String adid = null;		//Added 01-30-09 
    public String nodeaddart = null;//BH_09-02-09
    public String nodepid = null;	//BH_09-02-09
    public String nodealign = null;	//BH_09-02-09
    public String nodeminback = null;//BH_09-02-09
    public String nodepath = null;	//BH_09-02-09
    public int nodeexpdays = -1;	//BH_09-02-09
    public int nodeexpplays = -1;	//BH_09-02-09
    public int nodeallowdelete = 1;	//BH_09-02-09
    public int nodeallowshuffle = 0;//BH_09-02-09
    public int nodeautohide = 0;	//BH_09-02-09
    public int nodeautoshuffle = 0;	//BH_09-02-09
    public int nodeshowad = 0;		//BH_09-02-09
    public String height = null;//BB nodeheight-Added 01-30-09 If not null or "", override ad height
    public String width = null;	//BB nodewidth=Added 06-05-09 If not null or "", override ad width
    public boolean isRecording = false;	//BH_09-02-09
    public boolean isFlyBack = false;	//BH_09-02-09
    public boolean isAudioPodcast = false;//BH_09-02-09
    public boolean isVideoPodcast = false;//BH_09-02-09
    public Drawable adimage = null;		//BB is bitmap
    public XMLTracklist tracklist = null;//BH_09-02-09
    public String adpage = null;	//Added 06-05-09
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

    XMLNode()
    {
        type = XMLObject.NODE;
        children = null;
    }
}