package com.appMobi.appMobiLib;


public class AssetCacheFile {
	public String url;
	public int checkpoint;
	public long length;

	public String getFilename() {
		String filename = url.substring(url.lastIndexOf('/')+1);
		return filename;
	}	
	
    @Override
	public String toString(){
        return 
        "checkpoint: " + checkpoint + "\n" +
        "url: " + url  + "\n" +
        "length: " + length;
    }
	
}
