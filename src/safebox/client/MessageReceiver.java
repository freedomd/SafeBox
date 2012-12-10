package safebox.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MessageReceiver extends Thread{
	private SafeBoxClient client;
	private BufferedReader inFromServer;
	
	/* server requests */
	static final int  REGISTER_RES = 1,	LOGIN_RES = 2,	LOGOUT_RES = 3, EXIT_RES = 100, 
		 			  CREATEDIR_RES = 4,DELETEDIR_RES = 5,	
		 			  PUTFILE_RES = 6, 	REMOVEFILE_RES = 7, 
		 			  SHAREDIR_REQ = 8, SHAREDIR_REQ_RES = 14, SHAREDIR_RES = 9,  UNSHAREDIR_NOTI = 10,	UNSHAREDIR_RES = 11,
		 			  PUSH_PUT = 12, 	PUSH_REMOVE = 13, 	SYNC_RES = 99,
					  ACCEPT_RES = 15,  REJECT_RES = 16;
	
	public MessageReceiver(SafeBoxClient client) {
		try {
			this.client = client;
			inFromServer = new BufferedReader(new InputStreamReader(client.getClientSocket().getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				String[] temp = inFromServer.readLine().split(";");
				int method_ID = Integer.parseInt(temp[0]);
				String result = temp[1];
				
				switch (method_ID) {
					case REGISTER_RES: 
						//System.out.println(result);
						if (result.equals("OK")) {
							System.out.println("Register succeed! Logged in!");
							client.setLogin(true);
							client.setUser(temp[2]); // Once a user logged in successful, the user information will be fetched to the local machine
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case LOGIN_RES:
						if (result.equals("OK")) {
							System.out.println("Login succeed!");
							client.setLogin(true);
							client.setUser(temp[2]); // Once a user logged in successful, the user information will be fetched to the local machine
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case LOGOUT_RES:
						if (result.equals("OK")) {
							System.out.println("Logout succeed!");
							client.clearUser();
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case CREATEDIR_RES:
						if (result.equals("OK")) {
							String dirPath;
							if (!temp[2].equals("null")) {
								dirPath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // username\parentPath\dirName
							} else {
								dirPath = client.getUser().getUsername() + "\\" + temp[3];
							}
							System.out.println("Creating directory to server succeed, " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case DELETEDIR_RES:
						if (result.equals("OK")) {
							String dirPath;
							if (!temp[2].equals("null")) {
								dirPath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // username\parentPath\dirName
							} else {
								dirPath = client.getUser().getUsername() + "\\" + temp[3];
							}
							System.out.println("Deleting diretory from server succeed, " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case PUTFILE_RES:
						if (result.equals("OK")) {
							String filePath;
							if (!temp[2].equals("null")) {
								filePath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\fileName
							} else {
								filePath = client.getUser().getUsername() + "\\" + temp[3];
							}
							System.out.println("Createing file to server succeed, " + filePath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case REMOVEFILE_RES:
						if (result.equals("OK")) {
							String filePath;
							if (!temp[2].equals("null")) {
								filePath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\fileName
							} else {
								filePath = client.getUser().getUsername() + "\\" + temp[3];
							}
							System.out.println("Deleting file from server succeed, " + filePath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case SHAREDIR_RES:
						if (result.equals("OK")) {
							String dirPath, friendName;
							if (!temp[2].equals("null")) {
								dirPath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = client.getUser().getUsername() + "\\" + temp[3];
							}
							friendName = temp[4];
							client.shareDirAccepted(dirPath, friendName);
							System.out.println("Share directory request accepted from " + friendName + ", " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case UNSHAREDIR_RES:
						if (result.equals("OK")) {
							String dirPath, friendName;
							if (!temp[2].equals("null")) {
								dirPath = client.getUser().getUsername() + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = client.getUser().getUsername() + "\\" + temp[3];
							}
							friendName = temp[4];
							//client.unshareDirAccepted(dirPath, friendName);
							System.out.println("Unshare directory request accepted from " + friendName + ", " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case SHAREDIR_REQ:
						if (!temp[1].equals("null")) { // owner is not null
							String ownerName, dirPath;
							if (!temp[2].equals("null")) {
								dirPath = temp[1] + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = temp[1] + "\\" + temp[3];
							}
							//client.shareDirReq(ownerName, dirPath);
							//System.out.println("Share directory request from " + ownerName + ", " + dirPath);
						} else {
							System.out.println("Owner is null, illegal request!");
						}
						break;
					case UNSHAREDIR_NOTI:
						if (!temp[1].equals("null")) { // owner is not null
							String ownerName, dirPath;
							if (!temp[2].equals("null")) {
								dirPath = temp[1] + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = temp[1] + "\\" + temp[3];
							}
							//client.unshareDirNOTI(ownerName, dirPath);
							//System.out.println("Share directory request from " + ownerName + ", " + dirPath);
						} else {
							System.out.println("Owner is null, illegal request!");
						}
						break;
					case ACCEPT_RES:
						if (result.equals("OK")) {
							String ownerName, dirPath;
							if (!temp[2].equals("null")) {
								dirPath = temp[1] + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = temp[1] + "\\" + temp[3];
							}
							//client.getSharedDir(ownerName, dirPath);
							//System.out.println("Accepted share directory request from " + ownerName + " successfully, " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					case REJECT_RES:
						if (result.equals("OK")) {
							String ownerName, dirPath;
							if (!temp[2].equals("null")) {
								dirPath = temp[1] + "\\" + temp[2] + "\\" + temp[3]; // path: username\parentPath\dirName
							} else {
								dirPath = temp[1] + "\\" + temp[3];
							}
							//System.out.println("Rejected share directory request from " + ownerName + " successfully, " + dirPath);
						} else {
							String failMessage = temp[2];
							System.out.println(failMessage);
						}
						break;
					default:
						break;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
