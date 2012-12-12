/**
 * 
 */
package safebox.server;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
 * @author Rongdi Huang
 *
 */
public class ServerThread extends Thread {
	private int id;
	private Socket socket = null;
	private boolean running;
	private UserInfoMap userMap;
	private Map<String, UserFileMap> globalMap; 
	private BufferedReader fromClient;
	//private DataOutputStream toClient;
	private PrintWriter toClient;
	private AmazonS3 fileStorage;
	
	/* client requests */
	static final int  REGISTER = 1,		LOGIN = 2,		LOGOUT = 3, 	EXIT = 100, 
					  CREATEDIR = 4, 	DELETEDIR = 5,	SETKEY = 19, 	SHARE_AES_KEY = 20,
					  PUTFILE = 6, 		REMOVEFILE = 7, 
					  SHAREDIR = 8,		UNSHAREDIR = 9,
					  ACCEPT = 10, 		REJECT = 11, 	SYNC = 99;
	/* server requests */
	static final int  REGISTER_RES = 1,	LOGIN_RES = 2,	LOGOUT_RES = 3, EXIT_RES = 100, 
		 			  CREATEDIR_RES = 4,DELETEDIR_RES = 5,	
		 			  PUTFILE_RES = 6, 	REMOVEFILE_RES = 7, 
		 			  SHAREDIR_REQ = 8, SHAREDIR_REQ_RES = 14, SHAREDIR_RES = 9,  UNSHAREDIR_NOTI = 10,	UNSHAREDIR_RES = 11,
		 			  PUSH_PUT = 12, 	PUSH_REMOVE = 13, 	SYNC_RES = 99,
					  ACCEPT_RES = 15,  REJECT_RES = 16, SETKEY_RES = 19, PUSH_AES_KEY = 20;
	
	/* file type */
	static final int FILE = 0, DIR = 1;

	/**
	 * Server Thread Constructor
	 * @param client
	 * @param clientNum id of this user
	 * @param userMap record all user info
	 * @param globalMap 
	 */
	public ServerThread(Socket client, int clientNum, UserInfoMap userMap, Map<String, UserFileMap> globalMap) {
		this.id = clientNum;
		this.socket = client;
		this.running = true;
		this.userMap = userMap;
		
		this.globalMap = globalMap;
		AWSCredentials c = new BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA", "XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
		this.fileStorage = new AmazonS3Client(c);
		
		try {
			this.fromClient =
				new BufferedReader( new InputStreamReader(this.socket.getInputStream()) );
			this.toClient =
				new PrintWriter( this.socket.getOutputStream() );
				//new DataOutputStream(this.socket.getOutputStream());
		}  catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Initialization Error.");
			System.exit(-1);
		}
		System.out.println("Client " + this.id + " get connection.");
	}
	
	/**
	 * Login Method
	 * @param fields methodId;username;password
	 * @return error message
	 */
	public String login(String[] fields) {
		String response;
		if( userMap.authenticate(fields[1], fields[2]) ) { // success
			// update socket
			if( userMap.updateSocket(fields[1], this.socket) ) {
				response = String.format("%d;OK;%s;", LOGIN_RES, fields[1]); 
				List<String> requests = userMap.getUserInfo(fields[1]).getUnhandledRequests(); // get requests
				if(requests != null) {
					Iterator<String> rit = requests.iterator();
					while(rit.hasNext()) { // send every request
						toClient.println(rit.next());
						toClient.flush();
					}
				}
				userMap.getUserInfo(fields[1]).clearRequestsList(); // clear request list
			} else {
				response = String.format("%d;FAIL;No such user exists;", LOGIN_RES); 
			}
		} else { // fail
			response = String.format("%d;FAIL;Invalid Username or Password;", LOGIN_RES); 
		}
		return response;
	}
	
	/**
	 * Create a root directory on AWS for a new registered user
	 * @param rootname
	 * @return success of not
	 */
	public boolean createAWSRoot(String rootname) {
		try {    
	        // create a file to setup the folder on S3
			File newDir = new File(rootname);
			newDir.mkdirs();	
			String setupPath = rootname + "\\" + rootname + ".data";
			File setupFile = new File(setupPath); 
			setupFile.createNewFile();
			
			String bucketName = "SafeBox";
	        String key = setupPath.replace("\\", "/");
	        
            fileStorage.putObject(bucketName, key, setupFile);
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
	 * Register method
	 * @param fields methodId;username;password
	 * @return response
	 */
	public String register(String [] fields) {
		String response;
		if( userMap.addUserInfo(fields[1], fields[2], this.socket) ) { // success
			// create a root directory in AWS for the new user
			if( !createAWSRoot(fields[1])) {
				//userMap.deleteUserInfo(fields[1]); // delete user info in the map
				response = String.format("%d;FAIL;Cannot create root directory in AWS;", REGISTER_RES); 
			} else {
				// initialize in UserFileMap after success
				UserFileMap ufm = new UserFileMap(fields[1]);
				globalMap.put(fields[1], ufm);
				response = String.format("%d;OK;%s;", REGISTER_RES, fields[1]); 
			}
		} else { // fail
			response = String.format("%d;FAIL;Username %s Already Exists;", REGISTER_RES, fields[1]); 
		}	
		return response;
	}
	
	public String setKey(String [] fields) {
		String response;
		if(userMap.updatePublicKey(fields[1], fields[2], fields[3])) {
			response = String.format("%d;OK;", SETKEY_RES); 
		} else { // fail
			response = String.format("%d;FAIL;Cannot find user %s;", SETKEY_RES, fields[1]); 
		}	
		return response;
	}
	
	
	/**
	 * Log out method
	 * @param fields methodId;username
	 * @return response
	 */
	public String logout(String [] fields) {
		String response;
		if( userMap.updateSocket(fields[1], null) ) {
			System.out.println(fields[1] + " logout");
			response = String.format("%d;OK;", LOGOUT_RES); 
		} else { // fail
			response = String.format("%d;FAIL;No such user %s exists;", LOGOUT_RES, fields[1]); 
		}
		return response;
	}
	
	/**
	 * push a new create file to a sharing fiend
	 * @param owner
	 * @param parentPath
	 * @param filename
	 * send to friend: methodId;idDir\owner\filepath;
	 */
	public void pushPut(int filetype, String owner, String parentPath, String filename) {
		String filepath;
		
		if(parentPath.equals("null")) {
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		
		Vector<String> friendList = globalMap.get(owner).getFriendList(filepath);
		String push = String.format("%d;%d\\%s\\%s\\%s", PUSH_PUT, filetype, owner, parentPath, filename);
		System.out.println("Push " + push);
		for(String f : friendList) { // for every friend, send push
			UserInfo u = userMap.getUserInfo(f);
			Socket friendSocket = u.getSocket();
			if(friendSocket != null) { // user is online
				try {
					PrintWriter toFriend =
						new PrintWriter( friendSocket.getOutputStream() );
					toFriend.println(push); // send push to friend
					toFriend.flush(); 
					
					System.out.println("Push put to " + u.getUsername() + " " + push);
				}  catch (IOException e) {
					continue;
				}
			}
		}
	}
	
	
	/**
	 * push a remove notification to a sharing friend
	 * @param owner
	 * @param parentPath
	 * @param filename
	 * send to friend: methodId;owner;isDir;parentPath;filename;
	 */
	public void pushRemove(int filetype, String owner, String parentPath, String filename) {
		String filepath;
		
		if(parentPath.equals("null")) {
			filepath = filename;
		} else {
			filepath = String.format("%s\\%s", parentPath, filename);
		}
		
		Vector<String> friendList = globalMap.get(owner).getFriendList(filepath);
		String push = String.format("%d;%s;%d;%s;%s", PUSH_REMOVE, owner, filetype, parentPath, filename);
		for(String f : friendList) { // for every friend, send push
			UserInfo u = userMap.getUserInfo(f);
			Socket friendSocket = u.getSocket();
			if(friendSocket != null) { // user is online
				try {
					PrintWriter toFriend =
						new PrintWriter( friendSocket.getOutputStream() );
					toFriend.println(push); // send push to friend
					toFriend.flush(); 
				}  catch (IOException e) {
					continue;
				}
			}
		}
		
	}
	
	/**
	 * Create Directory Method
	 * @param fields methodId;ownername;parentPath;dirName
	 * @return response
	 */
	public String createDir(String [] fields) {
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		if( user != null ) {
			String message = globalMap.get(fields[1]).addNewFile(DIR, fields[2], fields[3], fields[1]); // type, parent path, filename, owner name
			if(message == null) {
				pushPut(DIR, fields[1], fields[2], fields[3]);
				response = String.format("%d;OK;%s;%s;", CREATEDIR_RES, fields[2], fields[3]); 
			} else {
				response = String.format("%d;FAIL;%s;%s;%s;", CREATEDIR_RES, message, fields[2], fields[3]); 
			}
		} else {
			response = String.format("%d;FAIL;No such user %s exists;%s;%s;", CREATEDIR_RES, fields[1], fields[2], fields[3]); 
		}
		return response;
	}
	
	/**
	 * Delete directory method
	 * @param fields methodId;ownerName;parentPath;dirName;
	 * @return response
	 */
	public String deleteDir(String [] fields) {
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		if( user != null ) {
			pushRemove(DIR, fields[1], fields[2], fields[3]);
			String message = globalMap.get(fields[1]).deleteFile(DIR, fields[2], fields[3], fields[1]); // type, parent path, filename, owner name
			if(message == null) {
				response = String.format("%d;OK;%s;%s;", DELETEDIR_RES, fields[2], fields[3]); 
			} else {
				response = String.format("%d;FAIL;%s;%s;%s;", DELETEDIR_RES, message, fields[2], fields[3]); 
			}
		} else {
			response = String.format("%d;FAIL;No such user %s exists;%s;%s;", DELETEDIR_RES, fields[1], fields[2], fields[3]); 
		}
		return response;
	}
	
	
	/**
	 * Put new file
	 * @param fields methodId;ownerName;parentPath;filename;
	 * @return response
	 */
	public String putFile(String [] fields) {
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		if( user != null ) {
			String message = globalMap.get(fields[1]).addNewFile(FILE, fields[2], fields[3], fields[1]); // type, parent path, filename, owner name
			if(message == null) {
				pushPut(FILE, fields[1], fields[2], fields[3]);
				response = String.format("%d;OK;%s;%s;", PUTFILE_RES, fields[2], fields[3]); 
			} else {
				response = String.format("%d;FAIL;%s;%s;%s;", PUTFILE_RES, message, fields[2], fields[3]); 
			}
		} else {
			response = String.format("%d;FAIL;No such user %s exists;%s;%s;", PUTFILE_RES, fields[1], fields[2], fields[3]); 
		}
		return response;
	}
	
	
	/**
	 * Remove file method
	 * @param fields methodId;ownerName;parentPath;filename;
	 * @return response
	 */
	public String removeFile(String [] fields) {
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		if( user != null ) {
			pushRemove(FILE, fields[1], fields[2], fields[3]);
			String message = globalMap.get(fields[1]).deleteFile(FILE, fields[2], fields[3], fields[1]); // type, parent path, filename, owner name
			if(message == null) {
				response = String.format("%d;OK;%s;%s;", REMOVEFILE_RES, fields[2], fields[3]); 
			} else {
				response = String.format("%d;FAIL;%s;%s;%s;", REMOVEFILE_RES, message, fields[2], fields[3]); 
			}
		} else {
			response = String.format("%d;FAIL;No such user %s exists;%s;%s;", REMOVEFILE_RES, fields[1], fields[2], fields[3]); 
		}
		return response;
	}
	
	
	/**
	 * Share a directory with friend
	 * @param fields methodId;ownerName;parentPath;fileName;shareFriendName;
	 * @return response
	 */
	public String shareDirRequest(String [] fields) {  // owner name, parent path, filename, friend name
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		UserInfo friend = userMap.getUserInfo(fields[4]); // get share friend info
		if( user != null && friend != null ) {
			String request = String.format("%d;%s;%s;%s;%s;%s;", SHAREDIR_REQ, fields[1], fields[2], fields[3], user.getModulus(), user.getExponent()); // type, owner name, parent path, filename,
			
			// add share structure info
			String message = globalMap.get(fields[4]).addShareFile(fields[2], fields[3], globalMap.get(fields[1]).getFileMap().get(fields[1]), fields[1]); // add share file recursively
			globalMap.get(fields[4]).printFileMap();
			message = globalMap.get(fields[1]).addShareInfo(fields[2], fields[3], fields[4]); // parent path, filename, share friend name
			if(message != null) {
				response = String.format("%d;FAIL;%s;", SHAREDIR_RES, message); 
				return response;
			}
			
			Socket friendSocket = friend.getSocket();
			if(friendSocket == null) { // user is logout
				friend.addRequest(request); // add this request to wait list		
			} else {
				try {
					PrintWriter toFriend =
						new PrintWriter( friendSocket.getOutputStream() );
					toFriend.println(request); // send request to friend
					toFriend.flush(); 
				}  catch (IOException e) {
					response = String.format("%d;FAIL;Cannot send request to user %s;", SHAREDIR_RES, fields[4]);
					return response;
				}
			}
			response = String.format("%d;OK;%s;%s;%s;%s;%s;", SHAREDIR_RES, fields[2], fields[3], fields[4], friend.getModulus(), friend.getExponent()); 
		} else {
			if(user == null) {
				response = String.format("%d;FAIL;No such user %s exists;", SHAREDIR_RES, fields[1]); 
			} else {
				response = String.format("%d;FAIL;No such user %s exists;", SHAREDIR_RES, fields[4]);
			}
		}
		return response;
	}
	
/****
	public String accept(String[] fields) { // accept share directory, add structure to map; username, parent path, filename, owner name
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		UserInfo owner = userMap.getUserInfo(fields[4]); // get owner info
		if( user != null && owner != null ) {
			String res = String.format("%d;OK;%s;%s;%s;", SHAREDIR_RES, fields[1], fields[2], fields[3]); // type, friend name, parent path, filename
			
			// add share structure info
			String message = globalMap.get(fields[1]).addNewFile(DIR, fields[2], fields[3], fields[4]); // type, parent path, filename, owner name
			message = globalMap.get(fields[4]).addShareInfo(fields[2], fields[3], fields[1]); // parent path, filename, share friend name
			if(message != null) {
				response = String.format("%d;FAIL;%s;", ACCEPT_RES, message); 
				return response;
			}
			
			Socket ownerSocket = owner.getSocket();
			if(ownerSocket == null) { // user is logout
				owner.addRequest(res); // add this request to wait list		
			} else {
				try {
					PrintWriter toOwner =
						new PrintWriter( ownerSocket.getOutputStream() );
					toOwner.println(res); // send request to friend
					toOwner.flush(); 
				}  catch (IOException e) {
					response = String.format("%d;FAIL;Cannot send response to owner %s;",ACCEPT_RES, fields[2]);
					return response;
				}
			}
			response = String.format("%d;OK;", ACCEPT_RES); 
		} else {
			if(user == null) {
				response = String.format("%d;FAIL;No such user %s exists;", ACCEPT_RES, fields[1]); 
			} else {
				response = String.format("%d;FAIL;No such user %s exists;", ACCEPT_RES, fields[4]);
			}
		}
		return response;
	}
	
	public String reject(String[] fields) { // reject share directory
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		UserInfo owner = userMap.getUserInfo(fields[4]); // get owner info
		if( user != null && owner != null ) {
			String res = String.format("%d;FAIL;%s;%s;%s;", SHAREDIR_RES, fields[1], fields[2], fields[3]); // type, friend name, parent path, filename
			
			Socket ownerSocket = owner.getSocket();
			if(ownerSocket == null) { // user is logout
				owner.addRequest(res); // add this request to wait list		
			} else {
				try {
					PrintWriter toOwner =
						new PrintWriter( ownerSocket.getOutputStream() );
					toOwner.println(res); // send request to friend
					toOwner.flush(); 
				}  catch (IOException e) {
					response = String.format("%d;FAIL;Cannot send response to owner %s;",REJECT_RES, fields[4]);
					return response;
				}
			}
			response = String.format("%d;OK;", REJECT_RES); 
		} else {
			if(user == null) {
				response = String.format("%d;FAIL;No such user %s exists;", REJECT_RES, fields[1]); 
			} else {
				response = String.format("%d;FAIL;No such user %s exists;", REJECT_RES, fields[4]);
			}
		}
		return response;
	}

****/
	
	/**
	 * Unshare a directory
	 * @param fields methodId;ownerName;parentPath;fileName;shareFriendName;
	 * @return response
	 */
	public String unshareDir(String [] fields) { 
		String response;
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		UserInfo friend = userMap.getUserInfo(fields[4]); // get share friend info
		if( user != null && friend != null ) {
			String notification = String.format("%d;%s;%s;%s;", UNSHAREDIR_NOTI, fields[1], fields[2], fields[3]); // type, owner name, parent path, filename,
			Socket friendSocket = friend.getSocket();
			
			// delete share structure info
			String message = globalMap.get(fields[4]).deleteFile(DIR, fields[2], fields[3], fields[1]); // parent path, filename, owner name
			message = globalMap.get(fields[1]).deleteShareInfo(fields[2], fields[3], fields[4]);
			if(message != null) {
				response = String.format("%d;FAIL;%s;", UNSHAREDIR_RES, message); 
				return response;
			}
			
			if(friendSocket == null) { // user is logout
				friend.addRequest(notification); // add this notification to wait list		
			} else {
				try {
					PrintWriter toFriend =
						new PrintWriter( friendSocket.getOutputStream() );
					toFriend.println(notification); // send request to friend
					System.out.println("Unshare Notification: " + notification);
					toFriend.flush(); 
				}  catch (IOException e) {
					response = String.format("%d;FAIL;Cannot send notification to user %s;", UNSHAREDIR_RES, fields[4]);
					return response;
				}
			}
			response = String.format("%d;OK;%s;%s;%s;", UNSHAREDIR_RES, fields[2], fields[3], fields[4]); 
		} else {
			if(user == null) {
				response = String.format("%d;FAIL;No such user %s exists;", UNSHAREDIR_RES, fields[1]); 
			} else {
				response = String.format("%d;FAIL;No such user %s exists;", UNSHAREDIR_RES, fields[4]);
			}
		}
		return response;
	}
	
	
	/**
	 * Send synchronize information when user login
	 * @param fields
	 * @return
	 */
	public String sync(String [] fields) {
		//UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		String response;
		
		String fileList = globalMap.get(fields[1]).getFileList();
		response = String.format("%d;OK%s", SYNC_RES, fileList);
		
		return response;
	}
	
	/**
	 * Share AES key has been uploaded to AWS, send a notification along with all the share files
	 * @param fields
	 */
	public void shareAESKey(String [] fields) {
		UserInfo user = userMap.getUserInfo(fields[1]); // get user info
		UserInfo friend = userMap.getUserInfo(fields[4]); // get share friend info
		if( user != null && friend != null ) {
			String filepath;
			if(fields[2].equals("null")) {
				filepath = fields[3];
			} else {
				filepath = String.format("%s\\%s", fields[2], fields[3]);
			}
			String fileList = globalMap.get(fields[1]).getFileList("", filepath);
			String request = String.format("%d;%s%s", PUSH_AES_KEY, fields[1], fileList); // type, owner name, parent path, filename
			
			Socket friendSocket = friend.getSocket();
			if(friendSocket != null) { // user is online
				try {
					PrintWriter toFriend =
						new PrintWriter( friendSocket.getOutputStream() );
					toFriend.println(request); // send request to friend
					toFriend.flush(); 
					System.out.println(user.getUsername() + " share AES to " + friend.getUsername() + ":\n" + request);
				}  catch (IOException e) {
					// do nothing
				}
			}
		} 
	}
	
	
	public void run() {
		while (running) {
			
			try {
				// initialize
				System.out.println(this.socket.toString());
				System.out.println("Waiting for requests... ");
				String request = fromClient.readLine();
				System.out.println("Received request from " + this.id + " : " + request);
				String[] fields = request.split(";");
				int method = Integer.parseInt(fields[0]); // method Id
				
				// call method based on request
				String response = null;
				switch (method) {
				   case REGISTER: 
					   response = register(fields);
					   break;
				   case SETKEY:
					   response = setKey(fields);
					   break;
				   case LOGIN: 
					   response = login(fields);
					   break;
				   case LOGOUT: 
					   response = logout(fields);
					   break;
				   case CREATEDIR:
					   response = createDir(fields);
				       break;
				   case DELETEDIR:
					   response = deleteDir(fields);
					   break;
				   case PUTFILE:
					   response = putFile(fields);
					   break;
				   case REMOVEFILE:
					   response = removeFile(fields);
					   break;
				   case SHAREDIR:
					   response = shareDirRequest(fields);
					   break;
				   case UNSHAREDIR:
					   response = unshareDir(fields);
					   break;
				   case SHARE_AES_KEY:
					   shareAESKey(fields);
					   continue;
				/***
				   case ACCEPT:
					   response = accept(fields);
					   break;
				   case REJECT:
					   response = reject(fields);
					   break;
				   ***/
				   case SYNC:
					   response = sync(fields);
					   break;
				   case EXIT:
					   running = false;
					   //response = String.format("%d;OK;", EXIT_RES);
					   break;
				   default: System.out.println("Invalid Request."); break;
				}
				System.out.println(response);
				toClient.println(response);
            	toClient.flush();
				
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("Lost Connection to ." + this.socket);
				this.running = false;
			}
		}
	}

}
