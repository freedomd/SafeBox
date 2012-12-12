/**
 * 
 */
package safebox.server;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


import safebox.file.*;

/**
 * @author Rongdi Huang
 *
 */
public class UserFileMap {
	private String username;
	private Map<String, Map<SafeFile, Vector<SafeFile>>> fileMap;  // owner ; directory structure
	// if owner == username, own directory structure map
	// else, share directory structure map
	
	static final int FILE = 0, DIR = 1;
	
	public UserFileMap(String username) {
		this.username = username;
		this.fileMap = new ConcurrentHashMap<String, Map<SafeFile, Vector<SafeFile>>>();
		fileMap.put(this.username, new ConcurrentHashMap<SafeFile, Vector<SafeFile>>()); // own directory structure
	}
	
	public Map<String, Map<SafeFile, Vector<SafeFile>>> getFileMap() {
		return fileMap;
	}
	
	
	/**
	 * Check the path, create un-existed directories
	 * @param path
	 * @param owner
	 */
	public void checkPath(String path, String owner) { // check each directory in path exists or not
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(owner);
		String[] dirs = path.split("\\\\");
		int len = dirs.length;
		String parent, cp; // parent path, current path;
		if(len == 0) {
			parent = path;
			cp = path;
			len = 1;
		} else {
			parent = dirs[0]; 
			cp = dirs[0]; // current path
		}
		
		for(int i = 0; i < len; i++) { // check ancestors
			SafeFile fp = new SafeFile(DIR, parent, owner); // fake parent
			SafeFile fcp = new SafeFile(DIR, cp, owner); // fake current file
			
			if( !m.containsKey(fcp) ) { // this path does not exist, create it
				m.put(fcp, new Vector<SafeFile>());
				if( !fp.equals(fcp) ) { // does not equals to the parent, add it to the parent's vector
					m.get(fp).add(fcp);
				}
			}
			// next
			if(i + 1 < len) {
				parent = cp;
				cp = String.format("%s\\%s", parent, dirs[i + 1]);
			}
		}
		return ;
	}
	
	
	/**
	 * Add a new file into map
	 * @param filetype
	 * @param parentPath
	 * @param filename
	 * @param owner
	 * @return error message
	 */
	public String addNewFile(int filetype, String parentPath, String filename, String owner) {
		Map<SafeFile, Vector<SafeFile>> m;
		
		if(fileMap.containsKey(owner)) {
			m = fileMap.get(owner);
		} else { // create new for shares 
			m = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
			fileMap.put(owner, m); 
		}

		String filepath;
		
		if(parentPath.equals("null")) { 
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		SafeFile newFile = new SafeFile(filetype, filepath, owner);
		if( m.containsKey(newFile) ) { // already exists
			return String.format("%s already exists!", newFile.getFilePath());
		} else { // add to map
			if(!parentPath.equals("null")){  // parent exists
				checkPath(parentPath, owner); // check parent path
				SafeFile fp = new SafeFile(DIR, parentPath, owner);
				
				// get parent friend list
				Set<SafeFile> s = m.keySet();
				Vector<String> friendList = new Vector<String>(); 
				
				for(SafeFile f : s) {
					if(f.getFilePath().equals(parentPath)) {
						friendList = f.getFriendList();
						break;
					}
				}
				
				newFile.setFriendList(friendList); // set friend list as the same as its parent
				m.get(fp).add(newFile); // add new file to parent
			}
			m.put(newFile, new Vector<SafeFile>());
		}
		System.out.println("Add " + filepath);
		printFileMap();
		return null;
	}
	
	
	/**
	 * Delete a file from map
	 * @param filetype
	 * @param parentPath
	 * @param filename
	 * @param owner
	 * @return error message
	 */
	public String deleteFile(int filetype, String parentPath, String filename, String owner) {
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(owner);
		String message = null;
		String filepath;
		
		if(parentPath.equals("null")) {// || parentPath == null) { 
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		//System.out.println(filepath);
		SafeFile ff = new SafeFile(filetype, filepath, owner); // fake file
		if( m.containsKey(ff) ) { // exists
			// first delete files it contains recursively 
			while(true) {
				Vector<SafeFile> fList = m.get(ff); // file list, check it contains or not
				if( fList.isEmpty() ) break;
				SafeFile f = fList.firstElement();
				message = deleteFile(f.getIsDir(), filepath, f.getFilename(), owner);
			} 
			
			// delete this file from map
			m.remove(ff);
			// delete this file from parent path
			if(!parentPath.equals("null")) {
				SafeFile parent = new SafeFile(DIR, parentPath, owner); // fake file
				m.get(parent).remove(ff); // delete from parent's file list
			}
			
		} else { // return error message
			return String.format("%s does not exist!", ff.getFilePath());
		}
		System.out.println("Delete " + filepath);
		printFileMap();
		return message;
	}
	
	
	/**
	 * Add share directory structure into friend's map
	 * @param parentPath
	 * @param filename
	 * @param um
	 * @param owner
	 * @return
	 */
	public String addShareFile(String parentPath, String filename, Map<SafeFile, Vector<SafeFile>> um, String owner) {
		Map<SafeFile, Vector<SafeFile>> m;
		
		if(fileMap.containsKey(owner)) {
			m = fileMap.get(owner);
		} else { // create new for shares 
			m = new ConcurrentHashMap<SafeFile, Vector<SafeFile>>();
			fileMap.put(owner, m); 
		}
		
		String filepath;
		
		if(parentPath.equals("null")) {// || parentPath == null) { 
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		
		Set<SafeFile> s = um.keySet();
		Iterator<SafeFile> it = s.iterator();
		while(it.hasNext()) {
			SafeFile f = it.next();
			if(f.getFilePath().equals(filepath)) {
				m.put(f, um.get(f));
				for(SafeFile ff : um.get(f)) {
					addShareFile(f.getFilePath(), ff.getFilename(), um, owner);
				}
				break;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Add share friend info into owner's map for a specific directory
	 * @param parentPath
	 * @param filename
	 * @param friend
	 * @return
	 */
	public String addShareInfo(String parentPath, String filename, String friend) { // share parentpath/filename with freind
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(username); // get user's map
		Set<SafeFile> s = m.keySet();
		Iterator<SafeFile> it = s.iterator();
		String filepath;
		
		if(parentPath.equals("null")) {
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		while(it.hasNext()) {
			SafeFile f = it.next();
			if(f.getFilePath().equals(filepath)) {
				f.addFriend(friend);
				Vector<SafeFile> fList = m.get(f);
				for(SafeFile ff : fList) {
					 // file list, add friend info recursively
					addShareInfo(filepath, ff.getFilename(), friend);
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Delete share friend info from owner's map of a specific directory
	 * @param parentPath
	 * @param filename
	 * @param friend
	 * @return
	 */
	public String deleteShareInfo(String parentPath, String filename, String friend) { // unshare parentpath/filename with freind
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(username); // get user's map
		Set<SafeFile> s = m.keySet();
		Iterator<SafeFile> it = s.iterator();
		String filepath = String.format("%s\\%s", parentPath, filename);
		while(it.hasNext()) {
			SafeFile f = it.next();
			if(f.getFilePath().equals(filepath)) {
				f.deleteFriend(friend);
				Vector<SafeFile> fList = m.get(f);
				for(SafeFile ff : fList) {
					 // file list, delete friend info recursively
					deleteShareInfo(filepath, ff.getFilename(), friend);
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Print out current map with certain format
	 */
	public void printFileMap() {
		Set<String> keys = fileMap.keySet();
		Iterator<String> kit = keys.iterator();
		System.out.println("*********************\n" + username + "\n**");
		while(kit.hasNext()) {
			String key = kit.next();
			System.out.println(key + "\n==============");
			Map<SafeFile, Vector<SafeFile>> m = fileMap.get(key);
			Set<SafeFile> ks = m.keySet();
			Iterator<SafeFile> it = ks.iterator();
			while(it.hasNext()) {
				SafeFile k = it.next();
				System.out.println(k.getFilePath() + ": ");
				Vector<SafeFile> fs = m.get(k);
				Iterator<SafeFile> sfit = fs.iterator();
				while(sfit.hasNext()) {
					SafeFile f = sfit.next(); // files
					System.out.println("   " + f.getFilePath());
				}
				System.out.println("------");
			}
		}
	}
	
	
	/**
	 * Get all files with its owner in this map
	 * @return file list
	 */
	public String getFileList() {
		String fileList = "";
		Set<String> keys = fileMap.keySet();
		Iterator<String> kit = keys.iterator(); // all usernames

		while(kit.hasNext()) {
			String uname = kit.next(); // username
			Map<SafeFile, Vector<SafeFile>> m = fileMap.get(uname); // for each user
			
			Set<SafeFile> fs = m.keySet(); // all file entries
			Iterator<SafeFile> it = fs.iterator();
			while(it.hasNext()) {
				SafeFile f = it.next();
				String filepath = String.format("%d\\%s\\%s", f.getIsDir(), uname, f.getFilePath());
				fileList = String.format("%s;%s", fileList, filepath);				
			}
		}
		return fileList;
	}
	
	/**
	 * Get all files of owner under a specific path
	 * @return file list
	 */
	public String getFileList(String fileList, String path) {
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(username);

		//System.out.println(fileList + "\n" + path);
		Set<SafeFile> fs = m.keySet(); // all file entries
		for(SafeFile f : fs) {
			//System.out.println(f.getFilePath());
			if(f.getFilePath().equals(path)) {
				String filepath = String.format("%d\\%s\\%s", f.getIsDir(), username, f.getFilePath());
				fileList = String.format("%s;%s", fileList, filepath);
				Vector<SafeFile> flist = m.get(f);
				for(SafeFile ff : flist) {
					fileList = getFileList(fileList, ff.getFilePath());
				}
			}
		}
		return fileList;
	}
	
	public Vector<String> getFriendList(String filepath) {
		Set<SafeFile> s = fileMap.get(username).keySet();
		
		for(SafeFile f : s) {
			if(f.getFilePath().equals(filepath)) {
				return f.getFriendList();
			}
		}
		
		return new Vector<String>(); // empty
	}
}
