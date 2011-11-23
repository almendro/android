package fm.flycast;


public class DPMessage extends DPMessageObject
{
	public DPXMLTracklist tracklist = null;
	public DPXMLTrack track = null;
	public String str = null;
	public DPMessage(int type)
	{
		this.type = type;
	}
	public DPMessage(int type, DPXMLTrack track)
	{
		this.type = type;
		this.track = track;
	}	
	public DPMessage(int type, String str)
	{
		this.type = type;
		this.str = str;
	}
	public DPMessage(int type, DPXMLTracklist tracklist)
	{
		this.type = type;
		this.tracklist = tracklist;
	}
}
