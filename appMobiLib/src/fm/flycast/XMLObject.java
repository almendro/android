package fm.flycast;
//-- Following version ported from Rosco's Blackberry 1.2x code 12-18-08, then 2.x code 09-01-09

import java.util.Vector;

public class XMLObject
{
    public static int NONE = 0;
    public static int DIRECTORY = 1;
    public static int NODE = 2;
    public static int FAIL = 3;
    public static int TRACKLIST = 4;
    public static int TRACK = 5;

    public int type;
    public Vector<XMLObject> children;

    XMLObject()
    {
        type = NONE;
        children = null;
    }
}
