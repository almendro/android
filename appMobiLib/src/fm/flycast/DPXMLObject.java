package fm.flycast;

import java.util.Vector;

public class DPXMLObject
{
    public static int NONE = 0;
    public static int DIRECTORY = 1;
    public static int NODE = 2;
    public static int FAIL = 3;
    public static int TRACKLIST = 4;
    public static int TRACK = 5;
    public static int DPMESSAGELIST = 6;
    public static int DPMESSAGE = 7;     

    public int type;
    public Vector<DPXMLObject> children;

    DPXMLObject()
    {
        type = NONE;
        children = null;
    }
}
