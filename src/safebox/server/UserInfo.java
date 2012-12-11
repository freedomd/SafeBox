/**
 * 
 */
package safebox.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rongdi Huang
 *
 */
public class UserInfo {
	private String username;
	private String password;
	private Socket socket;
	private List<String> requests;
	
	public UserInfo(String username, String password, Socket socket) {
		this.username = username;
		this.password = password;
		this.socket = socket;
		this.requests = new ArrayList<String>();
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
	
	public List<String> getUnhandledRequests() {
		return this.requests;
	}
	
	public void addRequest(String request) {
		this.requests.add(request);
	}
	
	public void clearRequestsList(){
		this.requests.clear();
	}
}
