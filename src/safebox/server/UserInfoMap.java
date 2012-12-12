/**
 * 
 */
package safebox.server;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rongdi Huang
 *
 */
public class UserInfoMap {
	private ConcurrentHashMap<String, UserInfo> map = new ConcurrentHashMap<String, UserInfo>();
	
	public UserInfo getUserInfo(String username) {
		return map.get(username);
	}
	
	public boolean addUserInfo(String username, String password, Socket socket) {
		if (map.get(username) != null) { // already exists
			return false;
		} else {
			UserInfo newUser = new UserInfo(username, password, socket);
			System.out.println("New User : " + username);
			map.put(username, newUser);
			return true;
		}
	}
	
	public boolean deleteUserInfo(String username) {
		if (map.get(username) != null) { // already exists
			map.remove(username);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean authenticate (String username, String password) {
		System.out.println(username + " login.");
		UserInfo user = map.get(username);
		if (user == null) {
			return false;
		} else{
			//System.out.println(user.getUsername());
			//System.out.println(user.getPassword());
			return user.getPassword().equals(password);
		}
	}
	
	public boolean updateSocket(String username, Socket socket) {
		UserInfo user = map.get(username);
		if (user == null) {
			return false;
		} else{
			user.resetSocket(socket);
			return true;
		}
	}
	
	public boolean updatePublicKey(String username, String modulus, String exponent) {
		
		UserInfo user = map.get(username);
		if (user == null) {
			return false;
		} else{
			System.out.println(username + " new public key:\nmodulus: " + modulus + "\nexponent: " + exponent);
			user.setPublicKey(modulus, exponent);
			return true;
		}
	}
	
}
