package test.java.com.apporiented.algorithm.clustering;

import java.io.File;
import java.util.HashMap;


public class LoadTiffFiles implements Runnable {
	private File file; 
	private HashMap<String, TiffParser> parsers;
	private String key;
	
	public LoadTiffFiles(String key, File file, HashMap<String, TiffParser> parsers){
		this.file = file;
		this.key = key;
		this.parsers = parsers;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		this.parsers.put(this.key, new TiffParser(this.file.getAbsolutePath()));
	}

}
