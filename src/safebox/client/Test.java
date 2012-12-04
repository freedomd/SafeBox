package safebox.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import safebox.file.SafeFile;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class Test {
	public static boolean deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files != null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                if (!deleteFolder(f)) return false;	                	
	            } else {
	                if (!f.delete()) return false;
	                else {
	                	System.out.println(f.getPath());
	                }
	            }
	        }
	    }
	    return folder.delete();
	}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		try {
			// String dirPath;
			// // BufferedReader br = new BufferedReader(new
			// InputStreamReader(System.in));
			// //
			// //
			// System.out.println("CreateDir:\nEnter the directory path(under the root dir): ");
			// dirPath = String.format("zz\\newDir");
			// System.out.println(dirPath);
			//
			// File newDir = new File(dirPath);
			// if (!newDir.exists()) {
			// System.out.println("not exist");
			// if (newDir.mkdirs()) {
			// String setupFile = dirPath + "\\zz.data";
			// String file1 = dirPath + "\\file1.txt";
			// String file2 = dirPath + "\\file2.txt";
			// File setup = new File(setupFile);
			// File f1 = new File(file1);
			// File f2 = new File(file2);
			// try {
			// setup.createNewFile();
			// f1.createNewFile();
			// f2.createNewFile();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// System.out.println(setupFile);
			// System.out.println(file1);
			// System.out.println(file2);
			// System.out.println("New directory created in local successfully, "
			// + dirPath + " , " + setupFile + " , " + file1 + " , " + file2);
			//
			// //System.out.println(newDir.isDirectory());
			// AWSCredentials c = new
			// BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA",
			// "XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
			// AmazonS3 fileStorage = new AmazonS3Client(c);
			// String bucketName = "SafeBox";
			// String key = setupFile.replace("\\", "/");
			// String key1 = file1.replace("\\", "/");
			// String key2 = file2.replace("\\", "/");
			//
			// System.out.println("Uploading a new object to S3 from a file\n");
			// fileStorage.putObject(bucketName, key, setup);
			// fileStorage.putObject(bucketName, key1, setup);
			// fileStorage.putObject(bucketName, key2, setup);
			//
			//
			// System.out.println("Uploading to S3 succeed!");
			// }
			// } else {
			// System.out.println("The directory's name is duplicate in the path, failed to create in local, "
			// + dirPath);
			// }

//			String dirPath = "zz\\newDir";
//			AWSCredentials c = new BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA",
//					"XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
//			AmazonS3 fileStorage = new AmazonS3Client(c);
//			String bucketName = "SafeBox";
//			String key = dirPath.replace("\\", "/");
//
//			// delete all the files contained in the object directory
//			ObjectListing objs = fileStorage.listObjects("SafeBox", key);
//			for (S3ObjectSummary objectSummary : objs.getObjectSummaries()) {
//				fileStorage.deleteObject(bucketName, objectSummary.getKey());
//			}
//			System.out.println("The directory deleted successfully on AWS, "
//					+ dirPath);
//			return;
			
			
//			String dirPath = "zz";
//			File deleteDir = new File(dirPath);
//			if (deleteDir.exists()) {
//				// delete the directory and all files contained in it in local
//				if (!deleteFolder(deleteDir)) {
//					System.out.println("Error in deleting files on local machine, " + dirPath);
//					return;
//				}
//				System.out.println("Delete on local machine successfully");
//
//			} else {
//				System.out.println("The directory does not exist in the path, failed to delete in local, " + dirPath);
//				return;
//			}
			String username = "zz";
			String dirName = "newDir";
			//String dirPath = parentPath + "\\" + dirName;
			String parentPath = username + "\\" + dirName;
			String toServerPath = parentPath.substring(username.length() + 1);
			//String os = String.format("%d;%s;%s;%s", CREATEDIR, user.getUsername(), toServerPath, dirName);
			System.out.println("parentPath: " + parentPath + ", toServerPath: " + toServerPath);
		} catch (AmazonServiceException ase) {
			System.out
					.println("Caught an AmazonServiceException, which means your request made it "
							+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			return;
		} catch (AmazonClientException ace) {
			System.out
					.println("Caught an AmazonClientException, which means the client encountered "
							+ "a serious internal problem while trying to communicate with S3, "
							+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}

}
