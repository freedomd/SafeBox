package safebox.client;

import java.net.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;


import safebox.file.SafeFile;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * @author Zhi Zeng
 * 
 */
public class SafeBoxClient {
	private Socket clientSocket;
	private AmazonS3 fileStorage;
	private User user;
	private boolean isLogin;
	private PrintWriter outToServer;
	private Map<String, RSAPublicKey> keyMap;
	//private BufferedReader inFromServer;
	
	/* client requests */
	static final int  REGISTER = 1,		LOGIN = 2,		LOGOUT = 3, 	EXIT = 100, 
					  CREATEDIR = 4, 	DELETEDIR = 5,	
					  PUTFILE = 6, 		REMOVEFILE = 7, 
					  SHAREDIR = 8,		UNSHAREDIR = 9,
					  ACCEPT = 10, 		REJECT = 11, 	SYNC = 99;

	/* server requests */
	static final int  REGISTER_RES = 1,	LOGIN_RES = 2,	LOGOUT_RES = 3, EXIT_RES = 100, 
		 			  CREATEDIR_RES = 4,DELETEDIR_RES = 5,	
		 			  PUTFILE_RES = 6, 	REMOVEFILE_RES = 7, 
		 			  SHAREDIR_REQ = 8, SHAREDIR_REQ_RES = 14, SHAREDIR_RES = 9,  UNSHAREDIR_NOTI = 10,	UNSHAREDIR_RES = 11,
		 			  PUSH_PUT = 12, 	PUSH_REMOVE = 13, 	SYNC_RES = 99,
					  ACCEPT_RES = 15,  REJECT_RES = 16;

	/**
	 * Constructor, take the server's address as parameter.
	 * Initialize the socket connection, writer, reader, and AmazonS3 instances.
	 * @param dstHost
	 */
	public SafeBoxClient(String dstHost) {
		try {
			clientSocket = new Socket(dstHost, 8000);
			outToServer = new PrintWriter(clientSocket.getOutputStream());
			//inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			AWSCredentials c = new BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA", "XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
			fileStorage = new AmazonS3Client(c);
			keyMap = new ConcurrentHashMap<String, RSAPublicKey>();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public Socket getClientSocket() {
		return clientSocket;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(String username) {
		user = new User(username);
	}
	
	public void clearUser() {
		user = null;
	}
	
	public void setLogin(boolean isLogin) {
		this.isLogin = isLogin;
	}

	/**
	 * Register a new account.
	 * Both username and password are up to 20 characters, and must be not null or empty.
	 * Username cannot contain split character ";" and white space.
	 * Initialize the user's information and automatically login if register succeed.
	 * @return true if registration succeed, otherwise return false
	 */
	public void register() {
		try {
			String username, password;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			// prompt the user to enter their name
			System.out.println("Register:\nEnter your username: ");
			username = br.readLine();
			System.out.println("Enter your password: ");
			password = br.readLine();

			if(username == null || username.isEmpty()) {
				System.out.println("The username cannot be null or empty");
				return;
			} 
			if (username.contains(";") || username.contains(" ")) {
				System.out.println("The username contains illegal charactors!");
				return;
			}
			if (username.length() > 20 || password.length() > 20) {
				System.out.println("The username and password are up to 20 characters!");
				return;
			}

			String os = String.format("%d;%s;%s;", REGISTER, username, password);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Login to the SafeBox server
	 * Both username and password are up to 20 characters, and must be not null or empty.
	 * Username cannot contain split character ";" and white space.
	 * Initialize the user's information if login succeed.
	 * @return true if login succeed, otherwise return false
	 */
	public void login() {
		try {
			String username, password;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			// prompt the user to enter their name
			System.out.println("Login:\nEnter your username: ");
			username = br.readLine();
			System.out.println("Enter your password: ");
			password = br.readLine();

			if(username == null || username.isEmpty()) {
				System.out.println("The username cannot be null or empty!");
				return;
			} 
			if (username.contains(";") || username.contains(" ")) {
				System.out.println("The username contains illegal charactors!");
				return;
			}
			if (username.length() > 20 || password.length() > 20) {
				System.out.println("The username and password are up to 20 characters!");
				return;
			}
		
			String os = String.format("%d;%s;%s;", LOGIN, username, password);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	


	/**
	 * Logout from the server
	 * Clear the user's information
	 * @return true if logout succeed, otherwise return false
	 */
	public void logout() {
		try {
			System.out.println("Logout:");

			String os = String.format("%d;%s;", LOGOUT, user.getUsername());
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a directory with a setup file
	 * Need to input the parent path and the name of the directory to be created.
	 * The default path is under the root directory of the user.
	 */
	public void createDir() {
		try {
			String parentPath, dirName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("CreateDir:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the directory name: ");
			dirName = br.readLine();
			
			if (!createDirToLocal(parentPath, dirName)) {
				return;
			}
			if (!createDirToAWS(parentPath, dirName)) {
				return;
			}
			createDirToServer(parentPath, dirName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void checkPath(String path) { // check each directory in path exists or not
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		String[] dirs = path.split("\\\\");
		int len = dirs.length;
		String parent, cp; // parent path, current path;
		if(len == 0) {
			parent = path;
			cp = path;
			len = 1;
		} else {
			parent = dirs[0] + "\\" + dirs[1]; 
			cp = dirs[0] + "\\" + dirs[1];  // current path
		}
		
		for(int i = 1; i < len; i++) { // check ancestors
			SafeFile fp = new SafeFile(1, parent, user.getUsername()); // fake parent
			SafeFile fcp = new SafeFile(1, cp, user.getUsername()); // fake current file
			
			if( !m.containsKey(fcp) ) { // this path does not exist, create it
				m.get(user.getUsername()).put(fcp, new Vector<SafeFile>());
				if( !fp.equals(fcp) ) { // does not equals to the parent, add it to the parent's vector
					m.get(user.getUsername()).get(fp).add(fcp);
				}
			}
			// next
			if(i + 1 < len) {
				parent = cp;
				cp = String.format("%s\\%s", parent, dirs[i + 1]);
			}
		}
	}
	
	/**
	 * Create a directory on local machine
	 * Put it in the user's account if creation succeed.
	 * If the directory exists, return true instead of false.
	 * @param parentPath
	 * @param dirName
	 * @return true if creation succeed, otherwise return false
	 */
	public boolean createDirToLocal(String parentPath, String dirName) {
		try {
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) { // username\
				dirPath = parentPath + dirName;
			} else { // username\***
				dirPath = parentPath + "\\" + dirName;
			}			
			System.out.println(dirPath);

			// create on local machine
			File newDir = new File(dirPath);
			if (!newDir.exists()) {
				newDir.mkdirs();	
			} else {
				System.out.println("The directory exits in local, " + dirPath);
				return false;
			}
			
			// record in user's account
			Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
			SafeFile newSafeDir = new SafeFile(1, dirPath, user.getUsername());
			if (!m.containsKey(newSafeDir)) {
				// make sure to create each directory in parentPath in user's account
				if(parentPath.length() != user.getUsername().length() + 1) {
					checkPath(parentPath); // check parent path
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					m.get(user.getUsername()).get(parentSafeDir).add(newSafeDir); // add new file to parent
				}				
				// add the directory to the map
				m.get(user.getUsername()).put(newSafeDir, new Vector<SafeFile>()); 
				
				System.out.println("New directory created in local successfully, " + newSafeDir.getFilePath());
				return true;
			} else {
				System.out.println("Directory exists in user's account, failed to create in local, " + newSafeDir.getFilePath());
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void buildPath(String path) { // create a setup file under each directory in path exists or not
		try {

		String[] dirs = path.split("\\\\");
		int len = dirs.length;
		String cp, fileName; // current path, fileName;
		if(len == 0) {
			cp = path;
			fileName = path;
			len = 1;
		} else {
			cp = dirs[1]; // current path
			fileName = dirs[1];
		}
		
		//String username = user.getUsername();
		
		for(int i = 1; i < len; i++) { // check ancestors
			String setupPath = dirs[0] + "\\" + cp + "\\" + fileName +".data";
			File setupFile = new File(setupPath); 
			setupFile.createNewFile();
			System.out.println("Setup file is created successfully, " + setupPath);
			
			String bucketName = "SafeBox";
	        String key = setupPath.replace("\\", "/");
	        
            fileStorage.putObject(new PutObjectRequest(bucketName, key, setupFile));
            System.out.println("Uploding to S3 succeed, " + setupPath);
            
			// next
			if(i + 1 < len) {
				cp = String.format("%s\\%s", cp, dirs[i + 1]);
				fileName = dirs[i + 1];
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a directory on AWS
	 * Because there is no way to create an empty folder on AWS using Java SDK,
	 * a setup file is created under the directory named "username.data".
	 * @param parentPath
	 * @param dirName
	 * @return true if creation succeed, otherwise return false
	 */
	public boolean createDirToAWS(String parentPath, String dirName) {
		try {    
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				dirPath = parentPath + dirName;
			} else {
				dirPath = parentPath + "\\" + dirName;
			}
			
			if(parentPath.length() != user.getUsername().length() + 1) {
				buildPath(parentPath); // create a setup file under each directory in the path on AWS
			}			
			
	        // create a file to setup the folder on S3
			String setupPath = dirPath + "\\" + dirName + ".data";
			File setupFile = new File(setupPath); 
			setupFile.createNewFile();
			System.out.println("Setup file is created successfully, " + setupPath);
			
			String bucketName = "SafeBox";
	        String key = setupPath.replace("\\", "/");
	        
            fileStorage.putObject(new PutObjectRequest(bucketName, key, setupFile));
            System.out.println("Uploding to S3 succeed, " + setupPath);
	        return true;
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
            return false;
        } catch (Exception e) {
        	e.printStackTrace();
        	return false;
        }
	}
	
	/**
	 * Create a directory on SafeBox server
	 * @param parentPath
	 * @param dirName
	 * @return true if creation succeed, otherwise return false
	 */
	public void createDirToServer(String parentPath, String dirName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s", CREATEDIR, user.getUsername(), toServerPath, dirName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Delete a directory with a setup file
	 * Need to input the parent path and the name of the directory to be deleted.
	 * The default path is under the root directory of the user.
	 */
	public void deleteDir() {
		try {
			String parentPath, dirName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("DeleteDir:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the directory name: ");
			dirName = br.readLine();
			
			if (!deleteDirFromAWS(parentPath, dirName)) {
				return;
			}
			if (!deleteDirFromLocal(parentPath, dirName)) {
				return;
			}
			deleteDirFromServer(parentPath, dirName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Delete a directory on AWS
	 * Recursively delete all the files contained in the directory
	 * @param parentPath
	 * @param dirName
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean deleteDirFromAWS(String parentPath, String dirName) {
		try {    
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				dirPath = parentPath + dirName;
			} else {
				dirPath = parentPath + "\\" + dirName;
			}
			
			String bucketName = "SafeBox";
	        String key = dirPath.replace("\\", "/");

	        // delete all the files contained in the object directory
	        ObjectListing objs = fileStorage.listObjects("SafeBox", key);
	        for (S3ObjectSummary objectSummary : objs.getObjectSummaries()) {
	            fileStorage.deleteObject(bucketName, objectSummary.getKey());
	        }	        
	        System.out.println("The directory deleted successfully on AWS, " + dirPath);
	        return true;
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
            return false;
        } catch (Exception e) {
        	e.printStackTrace();
        	return false;
        }
	}
	
	/**
	 * Helper method, recursively delete a directory on local machine
	 * @param folder
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files != null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	            	if (!deleteFolder(f)) return false;
	            } else {
	                if (!f.delete()) return false;
	            }
	        }
	    }
	    return folder.delete();
	}
	
	/**
	 * Helper method, recursively delete a directory in user's account
	 * @param folder
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean deleteSafeFolder(SafeFile delFolder) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		if (m.get(user.getUsername()).containsKey(delFolder)) {
			for(SafeFile delFile : m.get(user.getUsername()).get(delFolder)) {
				if (m.get(user.getUsername()).containsKey(delFile)) {
					if (delFile.getIsDir() == 1) {
						return deleteSafeFolder(delFile);
					} else {
						m.get(user.getUsername()).remove(delFile);
					}
				} else { // a file that is in the delFolder, but not in the entire file map
					return false;
				}
			}
			m.get(user.getUsername()).remove(delFolder);
			return true;
		} else {
			System.out.println("Directory does not exist in user's account.");
			return true;
		}
	}
	
	/**
	 * Delete a directory from local machine and user's account
	 * Recursively delete all the files contained in the directory
	 * @param parentPath
	 * @param dirName
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean deleteDirFromLocal(String parentPath, String dirName) {
		try {
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				dirPath = parentPath + dirName;
			} else {
				dirPath = parentPath + "\\" + dirName;
			}
			
			// delete on local machine
			File deleteDir = new File(dirPath);
			if (deleteDir.exists()) {
				// delete the directory and all files contained in it in local
				if (!deleteFolder(deleteDir)) {
					System.out.println("Error in deleting files on local machine, " + dirPath);
					return false;
				}
			} else {
				System.out.println("The directory does not exist in local, " + dirPath);
				return false;
			}	
			
			// delete the directory and all files contained in it from user's account
			SafeFile deleteSafeDir = new SafeFile(1, dirPath, user.getUsername());				
			if (deleteSafeFolder(deleteSafeDir)) {				
				if (parentPath.length() != user.getUsername().length() + 1) {
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					user.getFileMap().get(user.getUsername()).get(parentSafeDir).remove(deleteSafeDir); // delete the directory from its parent's list
				}
				System.out.println("Directory deleted in local successfully, " + deleteSafeDir.getFilePath());
				return true;
			} else {
				System.out.println("Error in deleting files in user's account, " + deleteSafeDir.getFilePath());
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Delete a directory from SafeBox server
	 * @param parentPath
	 * @param dirName
	 * @return true if deletion succeed, otherwise return false
	 */
	public void deleteDirFromServer(String parentPath, String dirName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s", DELETEDIR, user.getUsername(), toServerPath, dirName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

//			String[] temp = inFromServer.readLine().split(";");
//			int methodID = Integer.parseInt(temp[0]);
//			if (methodID == DELETEDIR_RES) {
//				String result = temp[1];
//				if (result.equals("OK")) {
//					System.out.println("Deleting diretory from server succeed, " + dirPath);
//					return true;
//				} else {
//					String failMessage = temp[2];
//					System.out.println(failMessage);
//					return false;
//				}
//			} else {
//				System.out.println("Communication message error!");
//				return false;
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a file in the parent directory
	 * Need to input the parent path and the name of the file to be created.
	 * The default path is under the root directory of the user.
	 */
	public void putFile() {
		try {
			String parentPath, fileName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("PutFile:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the file name: ");
			fileName = br.readLine();
			
			if (!putFileToLocal(parentPath, fileName)) {
				return;
			}
			if (!putFileToAWS(parentPath, fileName)) {
				return;
			}
			putFileToServer(parentPath, fileName);
			
//			if (parentPath.length() == user.getUsername().length() + 1) {
//				filePath = parentPath + fileName;
//			} else {
//				filePath = parentPath + "\\" + fileName;
//			}
//			System.out.println("New File created successfully, " + filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a file on local machine
	 * Put it in the user's account if creation succeed.
	 * @param parentPath
	 * @param fileName
	 * @return true if creation succeed, otherwise return false
	 */
	public boolean putFileToLocal(String parentPath, String fileName) {
		try {
			String filePath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				filePath = parentPath + fileName;
			} else {
				filePath = parentPath + "\\" + fileName;
			}
			
			// check if the parent path is existed on local machine, if not then create it
			if (parentPath.length() != user.getUsername().length() + 1) {
				File parentDir = new File(parentPath);
				if (!parentDir.exists()) {
					parentDir.mkdirs();
				}
			}
			// create on local machine
			File newFile = new File(filePath);
			if (!newFile.exists()) {
				newFile.createNewFile();
			} else {
				System.out.println("File exists in local, " + filePath);
				return false;
			}
			
			// record in user's account
			Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
			SafeFile newSafeFile = new SafeFile(0, filePath, user.getUsername()); // isDir, path, owner
			if (!m.get(user.getUsername()).containsKey(newSafeFile)) {
				// make sure to create each directory in parentPath in user's account
				if(parentPath.length() != user.getUsername().length() + 1) {
					checkPath(parentPath); // check parent path
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					m.get(user.getUsername()).get(parentSafeDir).add(newSafeFile); // add new file to parent
				}				
				// add the file to the map
				m.get(user.getUsername()).put(newSafeFile, new Vector<SafeFile>()); 
				
				System.out.println("New File created in local successfully, " + newSafeFile.getFilePath());
				return true;
			} else {
				System.out.println("File exists in user's account, failed to create in local, " + newSafeFile.getFilePath());
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Create a file on AWS
	 * @param parentPath
	 * @param fileName
	 * @return true if creation succeed, otherwise return false
	 */
	public boolean putFileToAWS(String parentPath, String fileName) {
		try {    
			String filePath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				filePath = parentPath + fileName;
			} else {
				filePath = parentPath + "\\" + fileName;
			}
			
			File newFile = new File(filePath); 
			if (!newFile.exists()) {
				System.out.println("The file does not exist on local machine, failed to upload to AWS, " + filePath);
				return false;
			}
			
			String bucketName = "SafeBox";
	        String key = filePath.replace("\\", "/");
	        
            fileStorage.putObject(new PutObjectRequest(bucketName, key, newFile));
            System.out.println("Uploding to S3 succeed, " + filePath);
	        return true;
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
            return false;
        } catch (Exception e) {
        	e.printStackTrace();
        	return false;
        }
	}
	
	/**
	 * Create a file on SafeBox server
	 * @param parentPath
	 * @param fileName
	 * @return true if creation succeed, otherwise return false
	 */
	public void putFileToServer(String parentPath, String fileName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s", PUTFILE, user.getUsername(), toServerPath, fileName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove a file
	 * Need to input the parent path and the name of the file to be deleted.
	 * The default path is under the root directory of the user.
	 */
	public void removeFile() {
		try {
			String parentPath, fileName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("RemoveFile:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the file name: ");
			fileName = br.readLine();
			
			if (!removeFileFromAWS(parentPath, fileName)) {
				return;
			}
			if (!removeFileFromLocal(parentPath, fileName)) {
				return;
			}
			removeFileFromServer(parentPath, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove a file on AWS
	 * @param parentPath
	 * @param fileName
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean removeFileFromAWS(String parentPath, String fileName) {
		try {    
			String filePath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				filePath = parentPath + fileName;
			} else {
				filePath = parentPath + "\\" + fileName;
			}
			
			String bucketName = "SafeBox";
	        String key = filePath.replace("\\", "/");

	        fileStorage.deleteObject(bucketName, key);        
	        System.out.println("The directory deleted successfully on AWS, " + filePath);
	        return true;
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            return false;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
            return false;
        } catch (Exception e) {
        	e.printStackTrace();
        	return false;
        }
	}
	
	/**
	 * Remove a file from local machine and user's account
	 * @param parentPath
	 * @param fileName
	 * @return true if deletion succeed, otherwise return false
	 */
	public boolean removeFileFromLocal(String parentPath, String fileName) {
		try {
			String filePath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				filePath = parentPath + fileName;
			} else {
				filePath = parentPath + "\\" + fileName;
			}
			
			// delete on local machine
			File rmFile = new File(filePath);
			if (rmFile.exists()) {
				// delete the file in local
				if (!rmFile.delete()) {
					System.out.println("Error in deleting files on local machine, " + filePath);
					return false;
				}
			} else {
				System.out.println("File does not exist in local, " + filePath);
				return false;
			}	
			
			// delete the file from user's account
			Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
			
			SafeFile rmSafeFile = new SafeFile(0, filePath, user.getUsername());				
			if (m.get(user.getUsername()).containsKey(rmSafeFile)) {
				m.get(user.getUsername()).remove(rmSafeFile);
				if (parentPath.length() != user.getUsername().length() + 1) {
					// delete the file from its parent's file list
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					m.get(user.getUsername()).get(parentSafeDir).remove(rmSafeFile); // delete the directory from its parent's list
				}
				System.out.println("Directory deleted in local successfully, " + rmSafeFile.getFilePath());
				return true;
			} else {
				System.out.println("File does not exist in user's account, deleted in local successfully, " + rmSafeFile.getFilePath());
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Remove from SafeBox server
	 * @param parentPath
	 * @param fileName
	 * @return true if deletion succeed, otherwise return false
	 */
	public void removeFileFromServer(String parentPath, String fileName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s", REMOVEFILE, user.getUsername(), toServerPath, fileName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Share a directory and the files contained in it to a friend
	 */
	public void shareDir() {
		try {
			String parentPath, dirName, friendName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("ShareDir:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the directory name: ");
			dirName = br.readLine();
			System.out.println("Enter the friend username: ");
			friendName = br.readLine();
			
			// check if the directory exists on local machine
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				dirPath = parentPath + dirName;
			} else {
				dirPath = parentPath + "\\" + dirName;
			}
			File shareDir = new File(dirPath);
			if (!shareDir.exists()) {
				System.out.println("Directory does not exist in local, " + dirPath);
				return;
			}
			
			// exist, send request to server
			shareDirToServer(parentPath, dirName, friendName);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send the share request to server
	 * @param parentPath
	 * @param dirName
	 * @param friendName
	 */
	public void shareDirToServer(String parentPath, String dirName, String friendName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s;%s", SHAREDIR, user.getUsername(), toServerPath, dirName, friendName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void unshareDir() {
		try {
			String parentPath, dirName, friendName;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("UnshareDir:\nEnter the parent path(under the root dir): ");
			parentPath = String.format("%s\\%s", user.getUsername(), br.readLine());
			System.out.println("Enter the directory name: ");
			dirName = br.readLine();
			System.out.println("Enter the friend username: ");
			friendName = br.readLine();
			
			// check if the directory exists on local machine
			String dirPath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				dirPath = parentPath + dirName;
			} else {
				dirPath = parentPath + "\\" + dirName;
			}
			File shareDir = new File(dirPath);
			if (!shareDir.exists()) {
				System.out.println("Directory does not exist in local, " + dirPath);
				return;
			}
			
			// exist, send request to server
			unshareDirToServer(parentPath, dirName, friendName);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send the share request to server
	 * @param parentPath
	 * @param dirName
	 * @param friendName
	 */
	public void unshareDirToServer(String parentPath, String dirName, String friendName) {
		try {
			String toServerPath;
			if(parentPath.length() != user.getUsername().length() + 1) {
				toServerPath = parentPath.substring(user.getUsername().length() + 1);
			}
			else {
				toServerPath = null;
			}
			String os = String.format("%d;%s;%s;%s;%s", UNSHAREDIR, user.getUsername(), toServerPath, dirName, friendName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void shareDirChoice(String ownerName, String parentPath, String dirName) {
//		try {
//			String choice;
//			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//			System.out.println("ShareDir_Request:\n1. Accept\n2. Reject");
//			
//			choice = br.readLine();
//			while (!choice.equals("1") && !choice.equals("2")) {
//				if (choice.equals("1")) {
//					accept(ownerName, parentPath, dirName);
//				} else if (choice.equals("2")) {
//					reject(ownerName, parentPath, dirName);
//				} else {
//					System.out.println("Illegal choice, please enter 1 or 2!");
//					choice = br.readLine();
//				}
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void accept(String ownerName, String parentPath, String dirName) {
//		try {
//			String os = String.format("%d;%s;%s;%s;%s", ACCEPT, user.getUsername(), parentPath, dirName, ownerName);
//			System.out.println(os);
//			outToServer.println(os);
//			outToServer.flush();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void reject(String ownerName, String parentPath, String dirName) {
//		try {
//			String os = String.format("%d;%s;%s;%s;%s", REJECT, user.getUsername(), parentPath, dirName, ownerName);
//			System.out.println(os);
//			outToServer.println(os);
//			outToServer.flush();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	
	/**
	 * Helper method, add friend to the friend list of each file under parentPath.
	 * @param safeFiles
	 * @param parentPath
	 * @param friendName
	 */
	public void shareSubFiles(Set<SafeFile> safeFiles, String parentPath, String friendName) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		Vector<SafeFile> subs = m.get(user.getUsername()).get(parentPath);
		
		for(SafeFile sf : safeFiles) {
			if (subs.contains(sf.getFilePath())) {
				sf.getFriendList().add(friendName);
				if (sf.getIsDir() == 1) {					
					shareSubFiles(safeFiles, sf.getFilePath(), friendName);
				}
			}
		}
	}
	
	/**
	 * The friend accepted the share request, add him/her to the friend list of this directory.
	 * @param dirPath
	 * @param friendName
	 */
	public void shareDirAccepted(String dirPath, String friendName, String mod, String expo) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		Set<SafeFile> safeFiles = m.get(user.getUsername()).keySet();
		
		for(SafeFile sf : safeFiles) {
			if (sf.getFilePath().equals(dirPath)) {
				sf.getFriendList().add(friendName);
				shareSubFiles(safeFiles, sf.getFilePath(), friendName);
			} 
		}
		
		//createShareAESKey(String friendName, String mod, String expo);
	}
	
	/**
	 * Synchronized the files from server and AWS after login
	 * @param user
	 */
	public void synchronize(User user) {
		System.out.println("Synchronize from server");
	}
	
	/**
	 * Exit the SafeBox client
	 */
	public void exit() {
		try {
			System.out.println("Exit:");

			String os = String.format("%d;", EXIT);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		SafeBoxClient client = new SafeBoxClient("192.168.1.5");
		MessageReceiver receiver = new MessageReceiver(client);
		receiver.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int cmd = 0;
		System.out.println("Enter operation number:\n"
				+ "1. Register\n2. Login\n3. Logout\n4. Create Directory\n"
				+ "5. Delete Direcotry\n6. Put File\n7. Remove File\n"
				+ "8. Share Direcotry\n9. Unshare Direcotry\n100. Quit\n");
		
		try {
			cmd = Integer.parseInt(br.readLine());
		} catch (NumberFormatException e) {
			System.out.println("please enter the number listed above!");
		}
		while (cmd != EXIT) {
			switch (cmd) {
				case REGISTER: 
					if(!client.isLogin) {
							client.register(); { // new user, automatically log in after registration
						}
					} else {
						System.out.println("The user is already logged in!");
					}
					break;
				case LOGIN: 
					if(!client.isLogin) {
						client.login();
						if (client.isLogin) {
							client.synchronize(client.user);
						}
					} else {
						System.out.println("The user is already logged in!");
					}
					break;
				case LOGOUT:
					if(client.isLogin == true) {
						client.logout();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case CREATEDIR:
					if(client.isLogin == true) {
						client.createDir();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case DELETEDIR:
					if(client.isLogin == true) {
						client.deleteDir();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case PUTFILE:
					if(client.isLogin == true) {
						client.putFile();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case REMOVEFILE:
					if(client.isLogin == true) {
						client.removeFile();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case SHAREDIR:
					if(client.isLogin == true) {
						client.shareDir();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				case UNSHAREDIR:
					if(client.isLogin == true) {
						client.unshareDir();
					} else {
						System.out.println("The user has not logged in!");
					}
					break;
				default:
					break;
			}
			System.out.println("\nEnter operation number:\n"
					+ "1. Register\n2. Login\n3. Logout\n4. Create Directory\n"
					+ "5. Delete Direcotry\n6. Put File\n7. Remove File\n"
					+ "8. Share Direcotry\n9. Unshare Direcotry\n100. Quit\n");

			try {
				cmd = Integer.parseInt(br.readLine());
			} catch (NumberFormatException e) {
				cmd = 0;
				System.out.println("please enter the number listed above!");
			}
		}
		client.exit();
		client.clientSocket.close();
	}

}
