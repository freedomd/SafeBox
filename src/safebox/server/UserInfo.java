/**
 * 
 */
package safebox.server;

import java.net.Socket;

/**
 * @author Rongdi Huang
 *
 */
public class UserInfo {
	private String username;
	private String password;
	private Socket socket;
	
	public UserInfo(String username, String password, Socket socket) {
		this.username = username;
		this.password = password;
		this.socket = socket;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public void resetSocket(Socket socket) {
		this.socket = socket;
	}
}
