package safebox.client;

import java.net.*;
import java.util.Iterator;
import java.util.Vector;
import java.io.*;

import org.aspectj.util.FileUtil;

import safebox.file.SafeFile;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
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
	private AmazonS3 keyStorage;
	private User user;
	private boolean isLogin;
	private PrintWriter outToServer;
	private BufferedReader inFromServer;
	
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
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			AWSCredentials c = new BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA", "XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
			fileStorage = new AmazonS3Client(c);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

	/**
	 * Register a new account.
	 * Both username and password are up to 20 characters, and must be not null or empty.
	 * Username cannot contain split character ";" and white space.
	 * Initialize the user's information and automatically login if register succeed.
	 * @return true if registration succeed, otherwise return false
	 */
	public boolean register() {
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
				return false;
			} 
			if (username.contains(";") || username.contains(" ")) {
				System.out.println("The username contains illegal charactors!");
				return false;
			}
			if (username.length() > 20 || password.length() > 20) {
				System.out.println("The username and password are up to 20 characters!");
				return false;
			}

			String os = String.format("%d;%s;%s;", REGISTER, username, password);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();
			System.out.println("kjla");
			
			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == REGISTER_RES) {
				String result = temp[1];
				System.out.println(result);
				if (result.equals("OK")) {
					System.out.println("Register succeed! Logged in!");
					user = new User(username); // Once a user logged in successful, the user information will be fetched to the local machine
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Login to the SafeBox server
	 * Both username and password are up to 20 characters, and must be not null or empty.
	 * Username cannot contain split character ";" and white space.
	 * Initialize the user's information if login succeed.
	 * @return true if login succeed, otherwise return false
	 */
	public boolean login() {
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
				return false;
			} 
			if (username.contains(";") || username.contains(" ")) {
				System.out.println("The username contains illegal charactors!");
				return false;
			}
			if (username.length() > 20 || password.length() > 20) {
				System.out.println("The username and password are up to 20 characters!");
				return false;
			}
		
			String os = String.format("%d;%s;%s;", LOGIN, username, password);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == LOGIN_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Login succeed!");
					user = new User(username); // Once a user logged in successful, the user information will be fetched to the local machine
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Logout from the server
	 * Clear the user's information
	 * @return true if logout succeed, otherwise return false
	 */
	public boolean logout() {
		try {
			System.out.println("Logout:");

			String os = String.format("%d;%s;", LOGOUT, user.getUsername());
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == LOGOUT_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Logout succeed!");
					user = null;
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			if (!createDirToServer(parentPath, dirName)) {
				return;
			}
			
			System.out.println("New directory created successfully, " + parentPath + "\\" + dirName);
		} catch (Exception e) {
			e.printStackTrace();
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
			String dirPath = parentPath + "\\" + dirName;

			// create on local machine
			File newDir = new File(dirPath);
			if (!newDir.exists()) {
				newDir.mkdir();	
			} else {
				System.out.println("The directory exits in local, " + dirPath);
			}
			
			// record in user's account
			SafeFile newSafeDir = new SafeFile(1, dirPath, user.getUsername());
			if (!user.getMyFile().containsKey(newSafeDir)) {
				// add the directory to the map
				user.getMyFile().put(newSafeDir, new Vector<SafeFile>()); 
				// if the parent directory does not exist, add it to the map
				SafeFile newParentDir = new SafeFile(1, parentPath, user.getUsername());
				if (!user.getMyFile().containsKey(newParentDir)) {
					user.getMyFile().put(newParentDir, new Vector<SafeFile>());
				}
				// add the directory to the parent
				user.getMyFile().get(newParentDir).add(newSafeDir);
				
				System.out.println("New directory created in local successfully, " + newSafeDir.getFilePath());
				return true;
			} else {
				System.out.println("Directory exists in user's account, created in local successfully, " + newSafeDir.getFilePath());
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			String dirPath = parentPath + "\\" + dirName;
	        // create a file to setup the folder on S3
			String setupPath = dirPath + dirName + ".data";
			File setupFile = new File(setupPath); 
			setupFile.createNewFile();
			System.out.println("Setup file is created successfully, " + setupPath);
			
			String bucketName = "SafeBox";
	        String key = setupPath.replace("\\", "/");
	        
            fileStorage.putObject(new PutObjectRequest(bucketName, key, setupFile));
            System.out.println("Uploding to S3 succeed, " + dirPath);
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
	public boolean createDirToServer(String parentPath, String dirName) {
		try {
			String dirPath = parentPath + "\\" + dirName;
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

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == CREATEDIR_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Creating directory to server succeed, " + dirPath);
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			if (!deleteDirFromServer(parentPath, dirName)) {
				return;
			}
			
			System.out.println("Directory deleted successfully, " + parentPath + "\\" + dirName);
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
			String dirPath = parentPath + "\\" + dirName;
			
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
		if (user.getMyFile().containsKey(delFolder)) {
			for(SafeFile delFile : user.getMyFile().get(delFolder)) {
				if (user.getMyFile().containsKey(delFile)) {
					if (delFile.getIsDir() == 1) {
						return deleteSafeFolder(delFile);
					} else {
						user.getMyFile().remove(delFile);
					}
				} else { // a file that is in the delFolder, but not in the entire file map
					return false;
				}
			}
			user.getMyFile().remove(delFolder);
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
			String dirPath = parentPath + "\\" + dirName;
			
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
			}	
			
			// delete the directory and all files contained in it from user's account
			SafeFile deleteSafeDir = new SafeFile(1, dirPath, user.getUsername());				
			if (deleteSafeFolder(deleteSafeDir)) {				
				SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
				user.getMyFile().get(parentSafeDir).remove(deleteSafeDir); // delete the directory from its parent's list
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
	public boolean deleteDirFromServer(String parentPath, String dirName) {
		try {
			String dirPath = parentPath + "\\" + dirName;
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

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == DELETEDIR_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Deleting diretory from server succeed, " + dirPath);
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			if (!putFileToServer(parentPath, fileName)) {
				return;
			}
			
			System.out.println("New File created successfully, " + parentPath + "\\" + fileName);
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
			String filePath = parentPath + "\\" + fileName;
			
			// create on local machine
			File newFile = new File(filePath);
			if (!newFile.exists()) {
				newFile.createNewFile();
			} else {
				System.out.println("File exists in local, " + filePath);
			}	
			
			// record in user's account
			SafeFile newSafeFile = new SafeFile(0, filePath, user.getUsername()); // isDir, path, owner
			if (!user.getMyFile().containsKey(newSafeFile)) {
				// add the directory to the map
				user.getMyFile().put(newSafeFile, new Vector<SafeFile>()); 
				// if the parent directory does not exist, add it to the map
				SafeFile newParentDir = new SafeFile(1, parentPath, user.getUsername());
				if (!user.getMyFile().containsKey(newParentDir)) {
					user.getMyFile().put(newParentDir, new Vector<SafeFile>());
				}
				// add the directory to the parent
				user.getMyFile().get(newParentDir).add(newSafeFile);
				
				System.out.println("New File created in local successfully, " + newSafeFile.getFilePath());
				return true;
			} else {
				System.out.println("File exists in user's account, created in local successfully, " + newSafeFile.getFilePath());
				return true;
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
			String filePath = parentPath + "\\" + fileName;
			
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
	public boolean putFileToServer(String parentPath, String fileName) {
		try {
			String filePath = parentPath + "\\" + fileName;
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

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == PUTFILE_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Creating file to server succeed, " + filePath);
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			if (!removeFileFromServer(parentPath, fileName)) {
				return;
			}
			
			System.out.println("File deleted successfully, " + parentPath + "\\" + fileName);
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
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
			String filePath = parentPath + "\\" + fileName;
			
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
			String filePath = parentPath + "\\" + fileName;
			
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
			}	
			
			// delete the file from user's account
			SafeFile rmSafeFile = new SafeFile(0, filePath, user.getUsername());				
			if (user.getMyFile().containsKey(rmSafeFile)) {
				user.getMyFile().remove(rmSafeFile);
				// delete the file from its parent's file list
				SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
				user.getMyFile().get(parentSafeDir).remove(rmSafeFile); // delete the directory from its parent's list
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
	public boolean removeFileFromServer(String parentPath, String fileName) {
		try {
			String filePath = parentPath + "\\" + fileName;
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

			String[] temp = inFromServer.readLine().split(";");
			int methodID = Integer.parseInt(temp[0]);
			if (methodID == REMOVEFILE_RES) {
				String result = temp[1];
				if (result.equals("OK")) {
					System.out.println("Deleting file from server succeed, " + filePath);
					return true;
				} else {
					String failMessage = temp[2];
					System.out.println(failMessage);
					return false;
				}
			} else {
				System.out.println("Communication message error!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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
		SafeBoxClient client = new SafeBoxClient("169.254.69.85");

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int cmd;
		System.out.println("Enter operation number:\n"
				+ "1. Register\n2. Login\n3. Logout\n4. Create Directory\n"
				+ "5. Delete Direcotry\n6. Put File\n7. Remove File\n"
				+ "8. Share Direcotry\n9. Unshare Direcotry\n100. Quit\n");

		while ((cmd = Integer.parseInt(br.readLine())) != EXIT) {
			switch (cmd) {
				case REGISTER: 
					if(client.isLogin == false) {
						if (client.register() == true) { // new user, automatically log in after registration
							client.isLogin = true;
						}
					} else {
						System.out.println("The user is already logged in!");
					}
					break;
				case LOGIN: 
					if(client.isLogin == false) {
						if (client.login() == true) {
							client.isLogin = true;
							client.synchronize(client.user);
						}
					} else {
						System.out.println("The user is already logged in!");
					}
					break;
				case LOGOUT:
					if(client.isLogin == true) {
						if (client.logout() ==  true) {
							client.isLogin = false;
						}
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
				default:
					break;
			}
			System.out.println("Enter operation number:\n"
					+ "1. Register\n2. Login\n3. Logout\n4. Create Directory\n"
					+ "5. Delete Direcotry\n6. Put File\n7. Remove File\n"
					+ "8. Share Direcotry\n9. Unshare Direcotry\n100. Quit\n");
		}
		client.exit();
		client.clientSocket.close();
	}

}
