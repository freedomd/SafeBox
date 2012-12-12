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
import java.security.spec.*;
import java.util.Random;


import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class SafeKey {
	private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey;
	private SecretKey aesFileKey, aesPKKey;
	private String aesFileString, aesPKString;
	
	public SafeKey() {}
	
	public SafeKey(String aesPKString) throws InvalidKeySpecException {
		this.aesPKString = aesPKString;
		genRSAKeys();
		genAESFileKey();
		genAESPKKey(this.aesPKString);
	}

	public RSAPublicKey getPublicKey() {
		return publicKey;
	}

	public RSAPrivateKey getPrivateKey() {
		return privateKey;
	}

	public SecretKey getAesFileKey() {
		return aesFileKey;
	}

	public SecretKey getAesPKKey() {
		return aesPKKey;
	}
	
	public String getaesFileString() {
		return aesFileString;
	}
	
	public String getaesPKString() {
		return aesPKString;
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
			
			//System.out.println("RSA public key: " + publicKey);
			//System.out.println("RSA private key: " + privateKey);
			
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Failed to generate RSA key pairs!\n" + e.toString());
		}
	}
	
	public void setPrivateKey(RSAPrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public void setAesFileKey(SecretKey aesFileKey) {
		this.aesFileKey = aesFileKey;
	}

	/**
	 * Generate the AES file key for encrypting files
	 */
	private void genAESFileKey() {
		try {
			// randomly generate a 64-bit string, used for generating the AES key
			char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
			StringBuilder sb = new StringBuilder();
			Random randomString = new Random();
			for (int i = 0; i < 64; i++) {
			    char c = chars[randomString.nextInt(chars.length)];
			    sb.append(c);
			}
			
			aesFileString = sb.toString();
			//System.out.println("AES file String: " + aesFileString);
			
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			
			SecureRandom random = new SecureRandom(aesFileString.getBytes());			
			keygen.init(256, random);
			
			aesFileKey = keygen.generateKey();
			//System.out.println("AES file key: " + aesFileKey);
		} catch (Exception e) {
			System.out.println("Failed to generate DES key for encrypting files!\n" + e.toString());
		}
	}
	
	/**
	 * Generate the AES key for encrypting RSA private key
	 * @param strKey
	 */
	public void genAESPKKey(String strKey) {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			
			SecureRandom random = new SecureRandom(strKey.getBytes());		
			keygen.init(256, random);
			
			aesPKKey = keygen.generateKey();
			//System.out.println("AES pk key: " + aesPKKey);
			
		} catch (Exception e) {
			System.out.println("Failed to generate AES key for encrypting files!\n" + e.toString());
		}
	}
	
}
