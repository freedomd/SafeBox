package safebox.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import safebox.file.SafeFile;

public class User {
	private String username;
	//private Map<SafeFile, Vector<SafeFile>> myFile;
	//private Map<SafeFile, Vector<SafeFile>> sharedFile;
	private Map<String, Map<SafeFile, Vector<SafeFile>>> fileMap;  // owner ; directory structure
	private SafeKey safeKey;
	
	public User(String username) {
		try {
			this.username = username;
			File rootDir = new File(this.username);
			if(!rootDir.exists()) {
			    rootDir.mkdirs();
				//System.out.println("Create root directory in local machine");
			}
			
			String setupPath = this.username + "\\" + this.username + ".data";
			File setupFile = new File(setupPath); 
			setupFile.createNewFile();
			//System.out.println("Setup file is created successfully, " + setupPath);
			//myFile = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
			//sharedFile = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
			fileMap = new ConcurrentHashMap<String, Map<SafeFile, Vector<SafeFile>>>();
			fileMap.put(this.username, new ConcurrentHashMap<SafeFile, Vector<SafeFile>>());
			safeKey = new SafeKey();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public SafeKey getSafeKey() {
		return safeKey;
	}

	public void setSafeKey() {
		try {
			this.safeKey = new SafeKey(username);
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			System.out.println("Failed to create security keys! Exit!");
			System.exit(-1);
		}
	}

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
