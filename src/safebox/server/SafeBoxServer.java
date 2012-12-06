/**
 * 
 */
package safebox.server;
//import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rongdi Huang
 *
 */
public class SafeBoxServer {
	static int clientNum = 0; // allocate id to client
	static UserInfoMap userMap = new UserInfoMap(); // user information map
	static Map<String, UserFileMap> globalMap = new ConcurrentHashMap<String, UserFileMap>(); // each user keeps a file map

	public static void main(String argv[]) throws Exception {
		
	   ServerSocket server = new ServerSocket(8000);
	   System.out.println("Welcome to SafeBoxServer.");
	   
	   /*****************************
		 * Hardcode part, remember to delete them
		 */
		userMap.addUserInfo("zz", "123", null);
		UserFileMap ufm = new UserFileMap("zz");
		globalMap.put("zz", ufm);
	   
       while(true) {
          Socket client = server.accept();
          clientNum++;
          ServerThread sThread = new ServerThread(client, clientNum, userMap, globalMap); // create a new thread for this client
          sThread.start(); // run!
       }
      
    }

}
