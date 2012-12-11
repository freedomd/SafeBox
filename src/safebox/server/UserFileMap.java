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
				m.get(fp).add(newFile); // add new file to parent
			}
			m.put(newFile, new Vector<SafeFile>());
		}
		System.out.println("Add " + filepath);
		printFileMap();
		return null;
	}
	
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
	
	public String addShareInfo(String parentPath, String filename, String friend) { // share parentpath/filename with freind
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(username); // get user's map
		Set<SafeFile> s = m.keySet();
		Iterator<SafeFile> it = s.iterator();
		String filepath = String.format("%s\\%s", parentPath, filename);
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
}
