/**
 * 
 */
package safebox.server;
import java.util.Iterator;
import java.util.Map;
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
		String[] dirs = path.split("\\");
		int len = dirs.length;
		String parent = dirs[0]; // parent path
		String cp = dirs[0]; // current path
		
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
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(owner);
		String filepath;
		
		if(parentPath != null) {
			filepath = String.format("%s\\%s", parentPath, filename);
		} else {
			filepath = filename;
		}
		SafeFile newFile = new SafeFile(filetype, filepath, owner);
		if( m.containsKey(newFile) ) { // already exists
			return String.format("%s already exists!\n", newFile.getFilePath());
		} else { // add to map
			if(parentPath != null) {
				checkPath(parentPath, owner); // check parent path
			}
			SafeFile fp = new SafeFile(DIR, parentPath, owner);
			m.get(fp).add(newFile); // add new file to parent
			if(filetype == DIR) { // directory
				m.put(newFile, new Vector<SafeFile>());
			} else { // file
				m.put(newFile, null);
			}
		}
		return null;
	}
	
	public String deleteFile(String parentPath, String filename, String owner) {
		Map<SafeFile, Vector<SafeFile>> m = fileMap.get(owner);
		String message = null;
		String filepath;
		
		if(parentPath != null) {
			filepath = String.format("%s\\%s", parentPath, filename);
		} else {
			filepath = filename;
		}
		SafeFile ff = new SafeFile(DIR, filepath, owner); // fake file
		if( m.containsKey(ff) ) { // exists
			// first delete files it contains recursively 
			Vector<SafeFile> fList = m.get(ff); // file list, check it contains or not
			if( fList != null ) {
				Iterator<SafeFile> fit = fList.iterator();
				while( fit.hasNext() ) {
					message = deleteFile(filepath, fit.next().getFilename(), owner);
				}
			} // else : this is a file
			
			// delete this file from map
			m.remove(ff);
			// delete this file from parent path
			if(parentPath != null) {
				SafeFile parent = new SafeFile(DIR, parentPath, owner); // fake file
				m.get(parent).remove(ff); // delete from parent's file list
			}
			
		} else { // return error message
			return String.format("%s does not exist!\n", ff.getFilePath());
		}
		return message;
	}
	

}
