package safebox.client;

import java.math.BigInteger;
import java.net.*;
import java.security.Key;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import safebox.file.SafeFile;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
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
	private Map<String, RSAPublicKey> publicKeyMap;
	private Map<String, SecretKey> aesKeyMap;
	private RSAPrivateKey privateKey;
	private String aesFileString;
	private SecretKey aesFileKey;
	
	/* client requests */
	static final int  REGISTER = 1,		LOGIN = 2,		LOGOUT = 3, 	EXIT = 100, 
					  CREATEDIR = 4, 	DELETEDIR = 5,	SETKEY = 19, SHARE_AES_KEY = 20,
					  PUTFILE = 6, 		REMOVEFILE = 7, 
					  SHAREDIR = 8,		UNSHAREDIR = 9,
					  ACCEPT = 10, 		REJECT = 11, 	SYNC = 99;

	/* server requests */
	static final int  REGISTER_RES = 1,	LOGIN_RES = 2,	LOGOUT_RES = 3, EXIT_RES = 100, 
		 			  CREATEDIR_RES = 4, DELETEDIR_RES = 5,	
		 			  PUTFILE_RES = 6, 	REMOVEFILE_RES = 7, 
		 			  SHAREDIR_REQ = 8, SHAREDIR_REQ_RES = 14, SHAREDIR_RES = 9,  UNSHAREDIR_NOTI = 10,	UNSHAREDIR_RES = 11,
		 			  PUSH_PUT = 12, 	PUSH_REMOVE = 13, SETKEY_RES = 19, PUSH_AES_KEY = 20, SYNC_RES = 99,
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
			publicKeyMap = new ConcurrentHashMap<String, RSAPublicKey>();
			aesKeyMap = new ConcurrentHashMap<String, SecretKey>();
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
	 * Set the four keys
	 * Send the public key to server
	 * Put encrypted private key and AES File String to AWS
	 */
	public void setKey() {
		try {
			user.setSafeKey(); // generate four keys
			
			// send the public key to server
			String mod = user.getSafeKey().getPublicKey().getModulus().toString();
			String expo = user.getSafeKey().getPublicKey().getPublicExponent().toString();
			
			String os = String.format("%d;%s;%s;%s", SETKEY, user.getUsername(), mod, expo);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();
			
			privateKey = user.getSafeKey().getPrivateKey();
			aesFileKey = user.getSafeKey().getAesFileKey();
			aesFileString = user.getSafeKey().getaesFileString();
			

			
			
			// encrypt the private key using AESPKKey, and put it on AWS
			String privatefilePath = user.getUsername() + "\\" + user.getUsername() + "_PRIVATEKEY";
			File privateKeyFile = new File(privatefilePath);
			if (privateKeyFile.createNewFile()) {
				System.out.println("Encrypted private key file is created in local, " + privatefilePath);
			} else {
				System.out.println("Encrypted private key file exists in local, " + privatefilePath);
			}
			
			String privateInfo = privateKey.getModulus().toString() + ";" + privateKey.getPrivateExponent().toString();
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, user.getSafeKey().getAesPKKey());
			// store the encrypted key in Hex
			String encryptedInfo = parseByte2HexStr(cipher.doFinal(privateInfo.getBytes())); 
			
			
			System.out.println("Encrypted private key: " + encryptedInfo);
			
			BufferedWriter output = new BufferedWriter(new FileWriter(privatefilePath));
		    output.write(encryptedInfo);
		    output.close();
		    
		    String bucketName = "SafeBox";
	        String key = privatefilePath.replace("\\", "/");

	        fileStorage.putObject(bucketName, key, privateKeyFile);        
			System.out.println("Encrypted private key file uploaded successfully on AWS, " + privatefilePath);
			
		    if (privateKeyFile.delete()) {
		    	System.out.println("Encrypted private key file is deleted in local, " + privatefilePath);
		    }
		    
		    // encrypt the aesFileString using public key, and put it on AWS
		    String aesfilePath = user.getUsername() + "\\" + user.getUsername() + "_AESKEY";
			File aesStringFile = new File(aesfilePath);
			if (aesStringFile.createNewFile()) {
				System.out.println("Encrypted AES String file is created in local, " + aesfilePath);
			} else {
				System.out.println("Encrypted AES String file exists in local, " + aesfilePath);
			}
			
			Cipher cipherAES = Cipher.getInstance("RSA");
			cipherAES.init(Cipher.ENCRYPT_MODE, user.getSafeKey().getPublicKey());
			// store the encrypted key in Hex
			String encryptedAESInfo = parseByte2HexStr(cipherAES.doFinal(aesFileString.getBytes()));
			
			System.out.println("Encrypted AES File key string: " + encryptedAESInfo);

			BufferedWriter outputAES = new BufferedWriter(new FileWriter(aesfilePath));
		    outputAES.write(encryptedAESInfo);
		    outputAES.close();
		    
	        String keyAES = aesfilePath.replace("\\", "/");

	        fileStorage.putObject(bucketName, keyAES, aesStringFile);        
			System.out.println("Encrypted AES String file uploaded successfully on AWS, " + aesfilePath);
			
		    if (aesStringFile.delete()) {
		    	System.out.println("Encrypted AES String file is deleted in local, " + aesfilePath);
		    }
			
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	public static String parseByte2HexStr(byte buf[]) { 
		StringBuffer sb = new StringBuffer(); 
		for (int i = 0; i < buf.length; i++) { 
			String hex = Integer.toHexString(buf[i] & 0xFF); 
			if (hex.length() == 1) { 
				hex = '0' + hex; 
			} 
			sb.append(hex.toUpperCase()); 
		} 
		return sb.toString(); 
	}

	public static byte[] parseHexStr2Byte(String hexStr) {
		if (hexStr.length() < 1) {
			return null;
		}
		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}
	
	/**
	 * After login, get the private key, aesFileKey and aesFileString from AWS
	 */
	public void getKey() {
		try {    
			// re-generate the AESPKKey
			user.getSafeKey().genAESPKKey(user.getUsername());
			
			// grab the encrypted private key from AWS, decrypt it using AESPKKey
			String privatePath = user.getUsername() + "\\" + user.getUsername() + "_PRIVATEKEY";
			
			String bucketName = "SafeBox";
	        String key = privatePath.replace("\\", "/");

	        S3Object obj = fileStorage.getObject(bucketName, key);        	        
	        System.out.println("The file downloaded successfully on AWS, " + privatePath);			
	        // read the content to a string
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
			String read;
			
			while((read = br.readLine()) != null) {
			    sb.append(read);
			}
			String encryptedPrivate = sb.toString();			
			// decrypt the string
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, user.getSafeKey().getAesPKKey());
			
			byte[] decodedValue = parseHexStr2Byte(encryptedPrivate);
			byte[] decryptedVal = cipher.doFinal(decodedValue);
//			
			String decryptedPrivateInfo = new String(decryptedVal);
			//String decryptedPrivateInfo = cipher.doFinal(encryptedPrivate.getBytes("UTF-8")).toString();			
			// get the modulous and exponent
			String[] temp = decryptedPrivateInfo.split(";");
			String mod = temp[0];
			String expo = temp[1];
			
			BigInteger m = new BigInteger(mod);
			BigInteger e = new BigInteger(expo);			
			// generate the private key 
			RSAPrivateKeySpec spec = new RSAPrivateKeySpec(m, e);
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        privateKey = (RSAPrivateKey) keyFactory.generatePrivate(spec);	        
	        user.getSafeKey().setPrivateKey(privateKey);
			
	        
			// grab the encrypted AES String from AWS, decrypt it using private key, re-generate the AES File String
			String aesPath = user.getUsername() + "\\" + user.getUsername() + "_AESKEY";
	        String keyAES = aesPath.replace("\\", "/");
	        S3Object objAES = fileStorage.getObject(bucketName, keyAES);        	        
			System.out.println("The file downloaded successfully on AWS, " + aesPath);
	        // read the content to a string
			StringBuilder sbAES = new StringBuilder();
			BufferedReader brAES = new BufferedReader(new InputStreamReader(objAES.getObjectContent()));
			String readAES;			
			while((readAES = brAES.readLine()) != null) {
			    sbAES.append(readAES);
			}
			String encryptedAESInfo = sbAES.toString();			
			// decrypt the string
			Cipher cipherAES = Cipher.getInstance("RSA");
			cipherAES.init(Cipher.DECRYPT_MODE, privateKey);
			
			byte[] decodedAESValue = parseHexStr2Byte(encryptedAESInfo);
			byte[] decryptedAESVal = cipherAES.doFinal(decodedAESValue);
//			
			aesFileString = new String(decryptedAESVal);
			
			//aesFileString = cipherAES.doFinal(encrpytedAESInfo.getBytes()).toString();			
			// re-generate the aesFileKey
			KeyGenerator keygen = KeyGenerator.getInstance("AES");			
			SecureRandom random = new SecureRandom(aesFileString.getBytes());			
			keygen.init(256, random);			
			aesFileKey = keygen.generateKey();
			user.getSafeKey().setAesFileKey(aesFileKey);			
			
			
			System.out.println("Private Key: " + privateKey);
			System.out.println("AES File String: " + aesFileString);
			System.out.println("AES File Key:" + aesFileKey);
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
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
				return true;
			}
			
			// record in user's account
			Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
			SafeFile newSafeDir = new SafeFile(1, dirPath, user.getUsername());
			if (!m.containsKey(newSafeDir)) {
				// make sure to create each directory in parentPath in user's account
				if(parentPath.length() != user.getUsername().length() + 1) {
					checkPath(parentPath); // check parent path
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					// add the friend info
					Set<SafeFile> files = m.get(user.getUsername()).keySet();
					for (SafeFile sf : files) {
						if (sf.equals(parentSafeDir) && !sf.getFriendList().isEmpty()) {
							newSafeDir.setFriendList(sf.getFriendList());
							break;
						}
					}
					
					m.get(user.getUsername()).get(parentSafeDir).add(newSafeDir); // add new file to parent
				}				
				// add the directory to the map
				m.get(user.getUsername()).put(newSafeDir, new Vector<SafeFile>()); 
				
				System.out.println("New directory created in local successfully, " + newSafeDir.getFilePath());
				return true;
			} else {
				System.out.println("Directory exists in user's account, " + newSafeDir.getFilePath());
				return true;
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
	public boolean deleteSafeFolder(String userName, SafeFile delFolder) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		if (m.get(userName).containsKey(delFolder)) {
			for(SafeFile delFile : m.get(userName).get(delFolder)) {
				if (m.get(userName).containsKey(delFile)) {
					if (delFile.getIsDir() == 1) {
						return deleteSafeFolder(userName, delFile);
					} else {
						m.get(userName).remove(delFile);
					}
				} else { // a file that is in the delFolder, but not in the entire file map
					return false;
				}
			}
			m.get(userName).remove(delFolder);
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
				return true;
			}	
			
			// delete the directory and all files contained in it from user's account
			SafeFile deleteSafeDir = new SafeFile(1, dirPath, user.getUsername());				
			if (deleteSafeFolder(user.getUsername(), deleteSafeDir)) {				
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
				
				// write something into the file
				String fileContent = filePath + System.currentTimeMillis();
				BufferedWriter output = new BufferedWriter(new FileWriter(filePath));
			    output.write(fileContent);
			    output.close();
			    
			    //System.out.println("File content: " + fileContent);
			    
			} else {
				System.out.println("File exists in local, " + filePath);
				return true;
			}
			
			// record in user's account
			Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
			SafeFile newSafeFile = new SafeFile(0, filePath, user.getUsername()); // isDir, path, owner
			if (!m.get(user.getUsername()).containsKey(newSafeFile)) {
				// make sure to create each directory in parentPath in user's account
				if(parentPath.length() != user.getUsername().length() + 1) {
					checkPath(parentPath); // check parent path
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					// add the friend info
					Set<SafeFile> files = m.get(user.getUsername()).keySet();
					for (SafeFile sf : files) {
						if (sf.equals(parentSafeDir) && !sf.getFriendList().isEmpty()) {
							newSafeFile.setFriendList(sf.getFriendList());
							break;
						}
					}				
					
					m.get(user.getUsername()).get(parentSafeDir).add(newSafeFile); // add new file to parent
				}				
				// add the file to the map
				m.get(user.getUsername()).put(newSafeFile, new Vector<SafeFile>()); 
				
				System.out.println("New File created in local successfully, " + newSafeFile.getFilePath());
				return true;
			} else {
				System.out.println("File exists in user's account, " + newSafeFile.getFilePath());
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
			String filePath;
			if (parentPath.length() == user.getUsername().length() + 1) {
				filePath = parentPath + fileName;
			} else {
				filePath = parentPath + "\\" + fileName;
			}
			
			if(parentPath.length() != user.getUsername().length() + 1) {
				buildPath(parentPath); // create a setup file under each directory in the path on AWS
			}
			
			File newFile = new File(filePath); 
			if (!newFile.exists()) {
				System.out.println("The file does not exist on local machine, failed to upload to AWS, " + filePath);
				return false;
			}
			
			// encrypt the file using AES key
			String encryptedPath = parentPath + "\\encrptyed_" + fileName;
			File encryptedFile = new File(encryptedPath);

			// read original file to a string and encrypt the string
			BufferedReader reader = new BufferedReader( new FileReader(newFile));
		    String line = null;
		    StringBuilder stringBuilder = new StringBuilder();
		    String ls = System.getProperty("line.separator");

		    while( ( line = reader.readLine() ) != null ) {
		        stringBuilder.append( line );
		        stringBuilder.append( ls );
		    }
		    
		    Cipher cipher = Cipher.getInstance("AES");
		    cipher.init(Cipher.ENCRYPT_MODE, aesFileKey);
		    String encryptedString = parseByte2HexStr(cipher.doFinal(stringBuilder.toString().getBytes()));
		    
		    System.out.println("Encrypted file content: " + encryptedString);
		    
		    // write encrypted string to file
		    BufferedWriter writer = new BufferedWriter( new FileWriter(encryptedFile));
		    writer.write(encryptedString);
		    writer.close();
			
			String bucketName = "SafeBox";
	        String key = filePath.replace("\\", "/");
	        
            fileStorage.putObject(new PutObjectRequest(bucketName, key, encryptedFile));
            encryptedFile.delete();
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
				return true;
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
	
	/**
	 * Unshare a directory to a friend
	 */
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
	 * Send the unshare request to server
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
			if (subs == null || subs.isEmpty()) {
				return;
			} else if (subs.contains(sf.getFilePath())) {
				sf.addFriend(friendName);
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
	public void shareDirAccepted(String parentPath, String dirName, String friendName, String mod, String expo) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		Set<SafeFile> safeFiles = m.get(user.getUsername()).keySet();
		String dirPath;
		
		if (!parentPath.equals("null")) {
			dirPath = user.getUsername() + "\\" + parentPath + "\\" + dirName; // path: username\parentPath\dirName
		} else {
			dirPath = user.getUsername() + "\\" + dirName;
		}
		
		for(SafeFile sf : safeFiles) {
			if (sf.getFilePath().equals(dirPath)) {
				sf.addFriend(friendName);
				shareSubFiles(safeFiles, sf.getFilePath(), friendName);
			} 
		}
		
		BigInteger n = new BigInteger(mod);
		BigInteger e = new BigInteger(expo);
				
		RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPublicKey pk = (RSAPublicKey) keyFactory.generatePublic(spec);
			
			publicKeyMap.put(friendName, pk);
			
			// use pk to encrypt aesFileString, then put it on AWS
		    String aesfilePath = user.getUsername() + "\\" + user.getUsername() + "_" + friendName + "_AESKEY";
			File aesStringFile = new File(aesfilePath);
			if (aesStringFile.createNewFile()) {
				System.out.println("Encrypted AES String file is created in local, " + aesfilePath);
			} else {
				System.out.println("Encrypted AES String file exists in local, " + aesfilePath);
			}
			
			Cipher cipherAES = Cipher.getInstance("RSA");
			cipherAES.init(Cipher.ENCRYPT_MODE, pk);
			String encryptedAESInfo = parseByte2HexStr(cipherAES.doFinal(aesFileString.getBytes()));

			BufferedWriter outputAES = new BufferedWriter(new FileWriter(aesfilePath));
		    outputAES.write(encryptedAESInfo);
		    outputAES.close();
		    
	        String keyAES = aesfilePath.replace("\\", "/");

	        fileStorage.putObject("SafeBox", keyAES, aesStringFile);        
			System.out.println("Encrypted AES String file uploaded successfully on AWS, " + aesfilePath);
			
			// send notification to server
			String os = String.format("%d;%s;%s;%s;%s", SHARE_AES_KEY, user.getUsername(), parentPath, dirName, friendName);
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();			
			
		    if (aesStringFile.delete()) {
		    	System.out.println("Encrypted AES String file is deleted in local, " + aesfilePath);
		    }
			
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception exc) {
			System.out.println("Failed to generate RSA key pairs!\n" + exc.toString());
		}
		
		//createShareAESKey(String friendName, String mod, String expo);
	}
	
	/**
	 * Helper method, delete friend from the friend list of each file under parentPath.
	 * @param safeFiles
	 * @param parentPath
	 * @param friendName
	 */
	public void unshareSubFiles(Set<SafeFile> safeFiles, String parentPath, String friendName) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		Vector<SafeFile> subs = m.get(user.getUsername()).get(parentPath);
		
		for(SafeFile sf : safeFiles) {
			if (subs.isEmpty()) {
				return;
			} else if (subs.contains(sf.getFilePath())) {
				sf.deleteFriend(friendName);
				if (sf.getIsDir() == 1) {					
					unshareSubFiles(safeFiles, sf.getFilePath(), friendName);
				}
			}
		}
	}
	
	/**
	 * The friend accepted the unshare request, delete him/her from the friend list of this directory.
	 * @param dirPath
	 * @param friendName
	 */
	public void unshareDirAccepted(String dirPath, String friendName) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		Set<SafeFile> safeFiles = m.get(user.getUsername()).keySet();
		
		for(SafeFile sf : safeFiles) {
			if (sf.getFilePath().equals(dirPath)) {
				sf.deleteFriend(friendName);
				unshareSubFiles(safeFiles, sf.getFilePath(), friendName);
			} 
		}
		
		//createShareAESKey(String friendName, String mod, String expo);
	}
	
	/**
	 * Delete the directory under <ownerName, <dirPath, Vector<subfiles>>> map
	 * @param ownerName
	 * @param parentPath
	 * @param dirPath
	 */
	public void unshareDirNOTI(String ownerName, String parentPath, String dirPath) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		
		SafeFile deleteSafeDir = new SafeFile(1, dirPath, ownerName);				
		if (deleteSafeFolder(ownerName, deleteSafeDir)) {				
			if (!parentPath.equals("null")) {
				SafeFile parentSafeDir = new SafeFile(1, parentPath, ownerName);
				m.get(ownerName).get(parentSafeDir).remove(deleteSafeDir); // delete the directory from its parent's list
			}
			System.out.println("Unshared directory deleted in user's account successfully, " + dirPath);
		} else {
			System.out.println("Failed to delete unshared directory in user's account, " + dirPath);
		}
	}
	
	/**
	 * Delete the pushed file under user's map
	 * @param ownerName
	 * @param isDir
	 * @param parentPath
	 * @param dirPath
	 */
	public void pushRemove(String ownerName, String isDir, String parentPath, String dirPath) {
		Map<String, Map<SafeFile, Vector<SafeFile>>> m = user.getFileMap();
		if (isDir.equals("1")) { // dir			
			SafeFile deleteSafeDir = new SafeFile(1, dirPath, ownerName);				
			if (deleteSafeFolder(ownerName, deleteSafeDir)) {				
				if (!parentPath.equals("null")) {
					SafeFile parentSafeDir = new SafeFile(1, parentPath, ownerName);
					m.get(ownerName).get(parentSafeDir).remove(deleteSafeDir); // delete the directory from its parent's list
				}
			} else {
				System.out.println("Failed to delete pushed directory in user's account, " + dirPath);
			}
		} else { // file			
			SafeFile rmSafeFile = new SafeFile(0, dirPath, ownerName);				
			if (m.get(ownerName).containsKey(rmSafeFile)) {
				m.get(user.getUsername()).remove(rmSafeFile);
				if (!parentPath.equals("null")) {
					SafeFile parentSafeDir = new SafeFile(1, parentPath, user.getUsername());
					m.get(ownerName).get(parentSafeDir).remove(rmSafeFile); // delete the directory from its parent's list
				}
			} else {
				System.out.println("Failed to delete pushed directory in user's account, " + dirPath);
			}
		}
	}
	
	/**
	 * get ownerName's AES File key to decrypt shared files
	 * @param ownerName
	 */
	public void getAESKey(String ownerName) {
		try {		
			String aesKeyPath = ownerName + "\\" + ownerName + "_" + user.getUsername() + "_AESKEY";
			String bucketName = "SafeBox";
	        String key = aesKeyPath.replace("\\", "/");

	        S3Object obj = fileStorage.getObject(bucketName, key); 
	        
	        System.out.println("The file downloaded successfully on AWS, " + aesKeyPath);
	        
	        // read the content to a string
			StringBuilder sbAES = new StringBuilder();
			BufferedReader brAES = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
			String readAES;			
			while((readAES = brAES.readLine()) != null) {
			    sbAES.append(readAES);
			}
			String encryptedAESInfo = sbAES.toString();	
			
			// decrypt the string
			Cipher cipherAES = Cipher.getInstance("RSA");
			cipherAES.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decodedAESValue = parseHexStr2Byte(encryptedAESInfo);
			byte[] decryptedAESVal = cipherAES.doFinal(decodedAESValue);
//			
			String newAESFileString = new String(decryptedAESVal);
			
			//String newAESFileString = cipherAES.doFinal(encrpytedAESInfo.getBytes()).toString();			
			// re-generate the aesFileKey
			KeyGenerator keygen = KeyGenerator.getInstance("AES");			
			SecureRandom random = new SecureRandom(newAESFileString.getBytes());			
			keygen.init(256, random);			
			SecretKey newAESFileKey = keygen.generateKey();
			// put it in the aes key map
	        aesKeyMap.put(ownerName, newAESFileKey);       
	        
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
        	System.out.println("Failed to download from AWS!");
        }
	}
	
	/**
	 * Synchronize request
	 * @param user
	 */
	public void syncReq() {
		try {
			String os = String.format("%d;%s", SYNC, user.getUsername());
			System.out.println(os);
			outToServer.println(os);
			outToServer.flush();
			System.out.println("Synchronize request to server");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Synchronize the files from server and AWS after login
	 */
	public void sync(String filePath) {
		try {
			System.out.println(filePath);

	        String[] dirs = filePath.split("\\\\");
	        String awsPath, parentPath, dirName, fileName, localPath;
	        boolean isDir = false;
	        
	        // get the aws path
	        if (dirs[0].equals("1")) { // directory
	        	isDir = true;
	        	awsPath = dirs[1] + "\\";
	        	if (dirs.length > 3) {
	        		for (int i = 2; i < dirs.length; ++i) {
	        			awsPath += dirs[i] + "\\";
	        		}
	        		awsPath += dirs[dirs.length - 1] + ".data"; 
	        	} else {
	        		awsPath += dirs[dirs.length - 1] + "\\" + dirs[dirs.length - 1] + ".data";
	        	}
	        } else { // file
	        	isDir = false;
	        	awsPath = dirs[1] + "\\";
	        	if (dirs.length > 3) {
	        		for (int i = 2; i < dirs.length - 1; ++i) {
	        			awsPath += dirs[i] + "\\";
	        		}
	        		awsPath += dirs[dirs.length - 1]; 
	        	} else {
	        		awsPath += dirs[dirs.length - 1];
	        	}
	        	
	        }
			
			String bucketName = "SafeBox";
	        String key = awsPath.replace("\\", "/");

	        S3Object obj = fileStorage.getObject(bucketName, key);        
	        
	        // get the local path
    		if (dirs.length > 3) {
	    		parentPath = user.getUsername() + "\\";
	    		for (int i = 2; i < dirs.length - 2; ++i) {
	    			parentPath += dirs[i] + "\\";
	    		}
	    		parentPath += dirs[dirs.length - 2]; 
    		} else {
    			parentPath = user.getUsername() + "\\";
    		}  		
	        if (isDir == true) {
	        	System.out.println("The directory downloaded successfully on AWS, " + filePath);
	    		dirName = dirs[dirs.length - 1];
	    		
	    		createDirToLocal(parentPath, dirName);
	       		if (dirs.length > 3) {
	       			localPath = parentPath + "\\" + dirName;
	       		} else {
	       			localPath = parentPath + dirName;
	       		}

	    		System.out.println("The directory downloaded successfully to local, " + localPath);
	    		
	        } else {
	        	System.out.println("The file downloaded successfully on AWS, " + filePath);
	        	fileName = dirs[dirs.length - 1];
	    		
	    		putFileToLocal(parentPath, fileName);
	       		if (dirs.length > 3) {
	       			localPath = parentPath + "\\" + fileName;
	       		} else {
	       			localPath = parentPath + fileName;
	       		}
	    		
	    		StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
				String read;			
				while((read = br.readLine()) != null) {
				    sb.append(read);
				}
				String encryptedFileInfo = sb.toString();
				
				String ownerName = dirs[1];
				decryption(ownerName, encryptedFileInfo, localPath);
	    		
	    		System.out.println("The file downloaded successfully to local, " + localPath);
	        }
	        
	        
		} catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
        	e.printStackTrace();
        	System.out.println("Failed to download from AWS, " + filePath);
        }
	}
	
	
	/**
	 * Decrypt encrpted string from AWS and write to local file
	 * @param owner get AES key
	 * @param encryptedString
	 * @param localPath
	 * @return
	 */
	public void decryption(String owner, String encryptedString, String localPath) {
		// get AES key first
		SecretKey aesKey;
		if(owner.equals(user.getUsername())) { // self
			aesKey = aesFileKey;
		} else { // other's 
			aesKey = aesKeyMap.get(owner);
			if (aesKey == null) { // not in map yet
				getAESKey(owner);
				aesKey = aesKeyMap.get(owner);
			}
		}
		
		// decryption
		try {
			// open local path
			BufferedWriter output = new BufferedWriter(new FileWriter(localPath));
			Cipher cipherAES = Cipher.getInstance("AES");
			cipherAES.init(Cipher.DECRYPT_MODE, aesKey);
			byte[] decodedAESValue = parseHexStr2Byte(encryptedString);
			byte[] decryptedAESVal = cipherAES.doFinal(decodedAESValue);
//			
			String decryptedString = new String(decryptedAESVal);
			
			//String decryptedString = cipherAES.doFinal(aesFileString.getBytes()).toString();
		
			// write file
			output.write(decryptedString);
			output.close();
			System.out.println("Encrypted String:  " + localPath + ": " + encryptedString);
			System.out.println("Decrypted String:  " + localPath + ": " + decryptedString);
		} catch (Exception e) {
			System.out.println("Decrypt " + localPath + " failed:");
			System.out.println(e.toString());
		}
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
						client.register();  // new user, automatically log in after registration
					} else {
						System.out.println("The user is already logged in!");
					}
					break;
				case LOGIN: 
					if(!client.isLogin) {
						client.login();
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
