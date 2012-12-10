package safebox.client;

import java.math.BigInteger;
import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;


public class SafeKey {
	private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey;
	private Key desFileKey, desPKKey;
	private String desPKString;
	
	public SafeKey(String desPKString) throws InvalidKeySpecException {
		this.desPKString = desPKString;
		genRSAKeys();
		genDESFileKey();
		genDESPKKey(this.desPKString);
	}

	public RSAPublicKey getPublicKey() {
		return publicKey;
	}

	public RSAPrivateKey getPrivateKey() {
		return privateKey;
	}

	public Key getDesFileKey() {
		return desFileKey;
	}

	public Key getDesPKKey() {
		return desPKKey;
	}

	/**
	 * Generate the RSA key pairs for encrypting the DES file key
	 * @throws InvalidKeySpecException 
	 */
	private void genRSAKeys() throws InvalidKeySpecException {
		try {
			KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");

			SecureRandom random = new SecureRandom();
			String seed = ((Long)System.currentTimeMillis()).toString();
			random.setSeed(seed.getBytes());			
			keygen.initialize(1024, random);
			
			KeyPair kp = keygen.generateKeyPair();
			publicKey = (RSAPublicKey) kp.getPublic();
			privateKey = (RSAPrivateKey) kp.getPrivate();
			
			String s = publicKey.getModulus().toString();
			String s2 = publicKey.getPublicExponent().toString();
			BigInteger n = new BigInteger(s);
			BigInteger e = new BigInteger(s2);
			
			System.out.println("reconstruct modulus: " + n.toString());
			System.out.println("reconstruct expo: " + e.toString());
			
			java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(n, e);
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        RSAPublicKey pk2 = (RSAPublicKey) keyFactory.generatePublic(spec);
	        
	        
	        System.out.println("reconstruct public key: " + pk2);
			
			System.out.println("RSA public key: " + publicKey);
			System.out.println("RSA private key: " + privateKey);
			
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Failed to generate RSA key pairs!\n" + e.toString());
		}
	}
	
	/**
	 * Generate the DES file key for encrypting files
	 */
	private void genDESFileKey() {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance("DES");
			
			SecureRandom random = new SecureRandom();
			String seed = ((Long)System.currentTimeMillis()).toString();
			random.setSeed(seed.getBytes());			
			keygen.init(random);
			
			desFileKey = keygen.generateKey();
			System.out.println("DES file key: " + desFileKey);
		} catch (Exception e) {
			System.out.println("Failed to generate DES key for encrypting files!\n" + e.toString());
		}
	}
	
	/**
	 * Generate the DES key for encrypting RSA private key
	 * @param strKey
	 */
	private void genDESPKKey(String strKey) {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance("DES");
			
			SecureRandom random = new SecureRandom(strKey.getBytes());		
			keygen.init(random);
			
			desPKKey = keygen.generateKey();
			System.out.println("DES pk key: " + desPKKey);
			
		} catch (Exception e) {
			System.out.println("Failed to generate DES key for encrypting files!\n" + e.toString());
		}
	}
	
	
	public void rsaEncryptFile(String srcFile, String dstFile) {
		
	}
	
	
	
}
