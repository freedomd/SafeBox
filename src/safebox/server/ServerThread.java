/**
 * 
 */
package safebox.server;
import java.io.*;
import java.net.*;

/**
 * @author Rongdi Huang
 *
 */
public class ServerThread extends Thread {
	private int id;
	private Socket socket = null;
	private boolean running;
	private UserInfoMap userMap;
	private BufferedReader fromClient;
	//private DataOutputStream toClient;
	private PrintWriter toClient;
	
	
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
		 			  SHAREDIR_REQ = 8, SHAREDIR_RES = 9,  	UNSHAREDIR_NOTI = 10,	UNSHAREDIR_RES = 11,
		 			  PUSH_PUT = 12, 	PUSH_REMOVE = 13, 	SYNC_RES = 99; 

	public ServerThread(Socket client, int clientNum, UserInfoMap userMap) {
		this.id = clientNum;
		this.socket = client;
		this.running = true;
		this.userMap = userMap;
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
					   if( userMap.addUserInfo(fields[1], fields[2], this.socket) ) { // success
						   response = String.format("%d;OK;", REGISTER_RES); 
					   } else { // fail
						   response = String.format("%d;FAIL;Username Already Exists;", REGISTER_RES); 
					   }	
					   break;
				   case LOGIN: 
					   if( userMap.authenticate(fields[1], fields[2]) ) { // success
						   // update socket
						   if( userMap.updateSocket(fields[1], this.socket) ) {
							   response = String.format("%d;OK;", LOGIN_RES); 
						   } else {
							   response = String.format("%d;FAIL;No such user exists;", LOGIN_RES); 
						   }
					   } else { // fail
						   response = String.format("%d;FAIL;Invalid Username or Password;", LOGIN_RES); 
					   }
					   break;
				   case LOGOUT: 
					   if( userMap.updateSocket(fields[1], null) ) {
						   response = String.format("%d;OK;", LOGOUT_RES); 
					   } else { // fail
						   response = String.format("%d;FAIL;No such user exists;", LOGOUT_RES); 
					   }
					   break;
				   case EXIT:
					   running = false;
					   //response = String.format("%d;OK;", EXIT_RES);
					   break;
				   default: System.out.println("Invalid Request."); break;
				}
				//System.out.println(response);
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
