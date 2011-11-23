package fm.flycast;
//package deviceProxy.Android;

//Device Specific
//implementing for java

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DPFileReaderWriter implements DPFileReaderWriterObject
{
	private boolean IsNew = true;
	public FileInputStream reader = null;
	public FileOutputStream writer = null;
	
	
	
	public DPFileReaderWriter(DPXMLTrack currentTrack){
		if( _lastRecordedTrack != null && _lastRecordedTrack.filename != null && !_lastRecordedTrack.filename.trim().equals("")){
	      	File file =new File(_lastRecordedTrack.filename);
        	if(file.exists()){
        		DPTrackState dts = DPApplication.Instance().getTrackState(_lastRecordedTrack);
        		if( dts != null && dts.GetTrackState() != 3)
        			DPApplication.Instance().AddMessageBuffered(new DPMessage(DPMessageObject.TRACK_IS_BUFFERED, _lastRecordedTrack));
        	}
		}
		_lastRecordedTrack = currentTrack;
		
	}
	private static DPXMLTrack _lastRecordedTrack = null;
	
	public DPFileReaderWriter(){
		
	}
	

	
	public static void CreateDir(String path){
		try{
			File dir = new File(path);
	        if( !dir.exists() )
	        {
	        	dir.mkdirs();
	        }
        }
		catch(Exception ex){
            System.err.println(DPStringConstants.STR_EXCEPTION + " -- Downloader (114) -- " + ex.getMessage());
		}
	}
	
	public boolean IsNew()
	{
		return IsNew;
	}
	
	public void Open(String fileName, boolean IsRead)
	{
		if(IsRead)
		{
			try
			{
				reader = new FileInputStream(fileName);
			}
			catch(Exception ex)
			{

			}
		}
		else
		{
			try
			{
				File f = new File(fileName);
				boolean exist = f.createNewFile();
				if( exist )
				{
					IsNew = true;
					writer = new FileOutputStream(f, false);
				}
				else
				{
					IsNew = false;
					writer = new FileOutputStream(f, true);
				}
				
			}
			catch(Exception ex)
			{

			}
		}
	}

	public void Close()
	{
		try
		{
			if(reader != null)
			{
				reader.close();
				reader = null;
			}

			if(writer != null)
			{
				writer.close();
				writer = null;
			}
		}
		catch(IOException ex)
		{

		}
	}

	public void Write(byte[] val, int off, int length)
	{
		try
		{
			IsNew = false;
			writer.write(val, off, length);
		}
		catch(IOException ex)
		{

		}
	}

	public byte[] Read(int off, int length)
	{
		byte[] buff = null;
		try
		{
			buff = new byte[length];
			reader.read(buff, off, length);
		}
		catch(IOException ex)
		{
			return null;
		}

		return buff;
	}
	
	public void Flush()
	{
		try
		{
			writer.flush();
		}
		catch(IOException ex)
		{
			
		}
	}
}
