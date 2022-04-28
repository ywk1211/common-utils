package com.ywk.common.util.encryption;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


/**
 * @date 2021年10月14日14:38:35
 * @author yanwenkai
 */
public class DESUtil {

	private final static SecureRandom SR = new SecureRandom();
	private static SecretKeyFactory keyFactory;
	private static KeyGenerator kg;

	static {
		try {
			keyFactory = SecretKeyFactory.getInstance("DES");
			kg = KeyGenerator.getInstance("DES");
			kg.init(SR);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static byte[] generateKey() {
		SecretKey key = kg.generateKey();
		return key.getEncoded();
	}

	public static byte[] encode(byte[] secureKey, byte[] data) throws RuntimeException {
		try {
			DESKeySpec dks = new DESKeySpec(secureKey);
			SecretKey key = keyFactory.generateSecret(dks);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, key, SR);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static byte[] decode(byte[] secureKey, byte[] data) throws RuntimeException {

		DESKeySpec dks;
		try {
			dks = new DESKeySpec(secureKey);
			SecretKey key = keyFactory.generateSecret(dks);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, key, SR);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}

	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println(new String(DesUtil.generateKey(), StandardCharsets.UTF_8));
	}

}
