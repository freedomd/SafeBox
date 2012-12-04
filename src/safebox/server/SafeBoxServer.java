/**
 * 
 */
package safebox.server;
//import java.io.*;
import java.net.*;

/**
 * @author Rongdi Huang
 *
 */
public class SafeBoxServer {
	static int clientNum = 0; // allocate id to client
	static UserInfoMap userMap = new UserInfoMap(); // user information map

	public static void main(String argv[]) throws Exception {
		
	   ServerSocket server = new ServerSocket(8000);
	   System.out.println("Welcome to SafeBoxServer.");
	   
       while(true) {
          Socket client = server.accept();
          clientNum++;
          ServerThread sThread = new ServerThread(client, clientNum, userMap); // create a new thread for this client
          sThread.start(); // run!
       }
      
    }

}
