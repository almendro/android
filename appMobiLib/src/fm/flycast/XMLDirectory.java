package fm.flycast;

//-- Following version ported from Rosco's Blackberry 1.2x code 12-18-08, then 2.x code 09-01-09
import java.util.Vector;

import android.graphics.drawable.Drawable;

public class XMLDirectory extends XMLObject
{
    public String dirname = null;
    public String dirdesc = null;	//BH_09-02-09
    public String dirid = null;
    public String dircolor = null;
    public String dirtitle = null;
    public String dirvalue = null;
    public String diradimg = null;
    public String dirmessage = null;
    public Drawable adimage = null;
    public String diradid = null;	//BH_09-02-09
    public String diraddart = null;	//BH_09-02-09
    public String dirpid = null;	//BH_09-02-09
    public String diralign = null;	//BH_09-02-09
    public boolean isRecording = false;	//BH_09-02-09
    public boolean isLibrary = false;	//BH_09-02-09
    public int filecount = 0;	//BH_09-02-09
    public int childcount = 0;	//BH_09-02-09
    public int dirheight = -1;	//BH_09-02-09
    public int dirwidth = -1;	//BH_09-02-09
    public XMLDirectory parent = null;
    public boolean dynamic = false;
    public String adid = null;		//Added 01-30-09 
    public String height = null;	//Added 01-30-09 If not null or "", override ad height
    public String width = null;		//Added 06-05-09 If not null or "", override ad width
    public String adpage = null;	//Added 06-05-09    
    

    XMLDirectory()
    {
        type = XMLObject.DIRECTORY;
        children = new Vector<XMLObject>();
    }
}
