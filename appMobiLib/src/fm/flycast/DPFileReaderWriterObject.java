package fm.flycast;

public interface DPFileReaderWriterObject 
{
	public boolean IsNew();
	public void Open(String fileName, boolean IsRead);
	public void Close();
	public void Write(byte[] b, int off, int length);
	public byte[] Read(int off, int length);
	public void Flush();
}
