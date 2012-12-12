package safebox.file;

import java.util.Vector;

public class SafeFile {
	private int isDir; // 0 for file, 1 for directory
	private String owner;
	private String filename;
	private String filePath;
	private Vector<String> friendList;
	
	public SafeFile(int isDir, String filePath, String owner) {
		this.isDir = isDir;
		this.filePath = filePath;
		this.owner = owner;
		String[] dirs = filePath.split("\\\\");
		this.filename = dirs[dirs.length - 1];
		this.friendList = new Vector<String>();
	}

	public SafeFile(SafeFile src) {
		this.isDir = src.isDir;
		this.owner = src.owner;
		this.filename = src.filename;
		this.filePath = src.filePath;
		this.friendList = src.friendList;
	}

	
	public void setFriendList(Vector<String> friendList) {
		this.friendList = friendList;
	}
	
	public Vector<String> getFriendList() {
		return friendList;
	}
	
	public void addFriend(String friend) {
		if(!friendList.contains(friend)) {
			friendList.add(friend);
		}
	}
	
	public void deleteFriend(String friend) {
		friendList.remove(friend);
	}

	/**
	 * @return the isDir
	 */
	public int getIsDir() {
		return isDir;
	}
	
	public String getFilename () {
		return filename;
	}

	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}
	
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}
	
	@Override
	/**
	 * This method override the toString() method.
	 * @return a String which shows the attributes of the file
	 */
	public String toString() {
		if (isDir == 0) {
			return "File: " + filePath + " Owner: " + owner;
		} else {
			return "Directory: " + filePath + " Owner: " + owner;
		}
	}
	
	@Override
	/**
	 * Override the hashCode() method
	 * @return a integer
	 */
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	/**
	 * This method override the equals() method.
	 * It takes an Object as an argument.
	 * If the pathName of a file is the same as another's, 
	 * then they are equal.
	 * @return true if two files are equal, otherwise return false.
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} 
		if (! (obj instanceof SafeFile)) {
			return false;
		}
		
		SafeFile other = (SafeFile) obj;
		if (filePath == null) {
			if (other.filePath != null) {
				return false;
			} 
		} else if (!filePath.equals(other.filePath)) {
			return false;
		}
		return true;
	}
}
