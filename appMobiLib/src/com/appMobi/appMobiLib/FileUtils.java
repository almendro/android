package com.appMobi.appMobiLib;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class FileUtils {
	private FileUtils(){}
	
    //this method checks the unzipped bundle to see if there is a nested directory in the bundle
    //if there is and it contains index.html moves all the files up one level and deletes the nested directory
    public static void checkDirectory(File appmobicache) {
    	File[] files = appmobicache.listFiles();
    	if(files.length==1 && files[0].isDirectory() && new File(files[0].getAbsolutePath(), "index.html").exists()) {
    		File nestedDirectory = files[0];
    		//move the nested directory to temp
    		File parent = nestedDirectory.getParentFile();
    		File temp = new File(parent, "../temp");
    		nestedDirectory.renameTo(temp);
    		files = temp.listFiles();
    		for(int i=0;i<files.length;i++) {
    			File source = files[i];
    			String name = source.getName();
    			File dest = new File(appmobicache, name);
    			source.renameTo(dest);
    		}
    		temp.delete();
    	}
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	/**
	 * Unpack a zip file
	 *
	 * @param theFile
	 * @param targetDir
	 * @return the file
	 * @throws IOException
	 */
	public static File unpackArchive(File theFile, File targetDir)
			throws IOException {
		if (!theFile.exists()) {
			throw new IOException(theFile.getAbsolutePath() + " does not exist");
		}
		if (!buildDirectory(targetDir)) {
			throw new IOException("Could not create directory: " + targetDir);
		}
		ZipFile zipFile = new ZipFile(theFile);
		for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			File file = new File(targetDir, File.separator + entry.getName());
			if (!buildDirectory(file.getParentFile())) {
				throw new IOException("Could not create directory: "
						+ file.getParentFile());
			}
			if (!entry.isDirectory()) {
				copyInputStream(zipFile.getInputStream(entry),
						new BufferedOutputStream(new FileOutputStream(file)));
			} else {
				if (!buildDirectory(file)) {
					throw new IOException("Could not create directory: " + file);
				}
			}
		}
		zipFile.close();
		return theFile;
	}

	public static void copyInputStream(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024];
		int len = in.read(buffer);
		while (len >= 0) {
			out.write(buffer, 0, len);
			len = in.read(buffer);
		}
		in.close();
		out.close();
	}

	public static boolean buildDirectory(File file) {
		return file.exists() || file.mkdirs();
	}	
}
