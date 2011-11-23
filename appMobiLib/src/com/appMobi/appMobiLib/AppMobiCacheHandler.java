package com.appMobi.appMobiLib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.appMobi.appMobiLib.util.Base64;

public class AppMobiCacheHandler {
	
	interface DownloadProgressEmitter {
		public void emit(long current, long length);
	}
	
	public static Boolean get(String url, Context context, String file, File dir)
	{
		return get(url, context, file, dir, null);
	}
	
	public static Boolean get(String url, Context context, String file, File dir, DownloadProgressEmitter emitter)
	{
		HttpEntity entity = AppMobiCacheHandler.getHttpEntity(url);
		try {
			AppMobiCacheHandler.writeToDisk(entity, context, file, dir, url, emitter);
		} catch (Exception e) { 
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
			return false; 
		}
		try {
			entity.consumeContent();
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
			return false;
		}
		return true;
	}

	public static HttpEntity getHttpEntity(String url)
	/**
	 * get the http entity at a given url
	 */
	{
		HttpEntity entity=null;
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url);
			//check for user info in url, do pre-emptive authentication if present
			if(httpget.getURI().getUserInfo()!=null) {
				String[] creds = httpget.getURI().getUserInfo().split(":");
				if(creds.length==2) httpget.setHeader("Authorization", "Basic " + Base64.encodeToString((creds[0]+":"+creds[1]).getBytes(), Base64.NO_WRAP));
			}
			HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();
		} catch (Exception e) { 
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
			return null; }
		return entity;
	}

	public static void writeToDisk(HttpEntity entity, Context context, String file, File dir, String url, DownloadProgressEmitter emitter) throws Exception /*appMobiException*/
	/**
	 * writes a HTTP entity to the specified filename and location on disk
	 */
	{
		long i=0;
		long lastEmission = System.currentTimeMillis();
		long current = 0;
		InputStream in = entity.getContent();
		byte buff[] = new byte[1024];
		FileOutputStream out = null;
		if(dir==null){
			out = context.openFileOutput(file, Context.MODE_PRIVATE);
		} else {
			dir.mkdirs();
			File fileTemp = new File(dir, file);
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", "mediacache writing " + url + "to: " + fileTemp.getAbsolutePath());
			}
			if(fileTemp.exists()) fileTemp.delete();
			boolean didSucceed = fileTemp.createNewFile();
			if(didSucceed) {
				out = new FileOutputStream(fileTemp);
			} else {
				//throw new appMobiException
				System.out.println("fileTemp.createNewFile(); failed");
			}
		}
		do {
			int numread = in.read(buff);
			if (numread <= 0)
               	break;
			current+=numread;
			out.write(buff, 0, numread);
			//System.out.println(new String(buff));
			if(emitter!=null) {
				//limit emissions to 1/sec
				long now = System.currentTimeMillis();
				if((now-lastEmission)>999) {
					emitter.emit(current, entity.getContentLength());
					lastEmission = now;
				}
			}
			i++;
		} while (true);
		out.flush();
		out.close();
	}
}
