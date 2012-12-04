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

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		try {
		String dirPath;
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//
//		System.out.println("CreateDir:\nEnter the directory path(under the root dir): ");
		dirPath = String.format("zz\\newDir");
		System.out.println(dirPath);
		
		File newDir = new File(dirPath);
		if (!newDir.exists()) {
			newDir.mkdir();
			String setupFile = dirPath + "\\zz.data"; 
			File setup = new File(setupFile);
			try {
				setup.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(setupFile);
			System.out.println("New directory created in local successfully, " + dirPath);
	        System.out.println(newDir.isDirectory());
			AWSCredentials c = new BasicAWSCredentials("AKIAIXKDAUTHKZNTDMFA", "XixuCVDrlemEH9SE3aPCVRym5V8CgXpp9y+nHRrQ");
			AmazonS3 fileStorage = new AmazonS3Client(c);
			String bucketName = "SafeBox";
	        String key = setupFile.replace("\\", "/");
	        System.out.println("Uploading a new object to S3 from a file\n");
	        fileStorage.putObject(bucketName, key, setup);
         System.out.println("Uploading to S3 succeed!");
		} else {
			System.out.println("The directory's name is duplicate in the path, failed to create in local, " + dirPath);
		}

		
		

		} catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
		
		
	}

}
