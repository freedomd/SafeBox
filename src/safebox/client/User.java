package safebox.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import safebox.file.SafeFile;

public class User {
	private String username;
	//private Map<SafeFile, Vector<SafeFile>> myFile;
	//private Map<SafeFile, Vector<SafeFile>> sharedFile;
	private Map<String, Map<SafeFile, Vector<SafeFile>>> fileMap;  // owner ; directory structure
	
	public User(String username) {
		this.username = username;
		File rootDir = new File(this.username);
		if(!rootDir.exists()) {
		    rootDir.mkdirs();
			System.out.println("Create root directory in local machine");
		}
		//myFile = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
		//sharedFile = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
		fileMap = new ConcurrentHashMap<String, Map<SafeFile, Vector<SafeFile>>>();
	}
	
//	/**
//	 * @return the myFile
//	 */
//	public Map<SafeFile, Vector<SafeFile>> getMyFile() {
//		return myFile;
//	}
//
//	/**
//	 * @return the sharedFile
//	 */
//	public Map<SafeFile, Vector<SafeFile>> getSharedFile() {
//		return sharedFile;
//	}

	public Map<String, Map<SafeFile, Vector<SafeFile>>> getFileMap() {
		return fileMap;
	}
	
	
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}



	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
}
