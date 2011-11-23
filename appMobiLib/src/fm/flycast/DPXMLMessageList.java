package fm.flycast;

import java.util.Vector;

public class DPXMLMessageList  extends DPXMLObject
{

	DPXMLMessageList ()
	{
      type = DPXMLObject.DPMESSAGELIST;
      children = new Vector<DPXMLObject>();
  }

}