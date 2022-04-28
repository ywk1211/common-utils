package com.ywk.common.util.encryption;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @description: 对称加密AES
 * @author: yanwenkai
 * @date: 2022/4/11 17:04
 **/
public class AESUtil {


    public static byte[] encBytes(byte[] srcBytes, byte[] key,
                                  byte[] newIv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(newIv);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(srcBytes);
    }

    public static String encText(String sSrc, byte[] key, byte[] newIv)
            throws Exception {
        byte[] srcBytes = sSrc.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encBytes(srcBytes, key, newIv);
        return Base64.encode(encrypted);
    }

    public static byte[] decBytes(byte[] srcBytes, byte[] key, byte[] newIv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec iv = new IvParameterSpec(newIv);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return cipher.doFinal(srcBytes);
    }

    public static String decText(String sSrc, byte[] key, byte[] newIv)
            throws Exception {
        byte[] srcBytes = Base64.decode(sSrc);
        byte[] decrypted = decBytes(srcBytes, key, newIv);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        String s = "13598789063";
//        byte[] key = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6};
//        byte[] ivk = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        byte[] key = "DLZYOQIFJYtt6ryc".getBytes();
        byte[] key = "2Coh6wr7nEvyCXjx".getBytes();
        for (byte b : key) {
            System.out.print(b+",");
        }
        System.out.println();
//        byte[] ivk = "g3MnmT6JSAWBsAuB".getBytes();
        byte[] ivk = "ppLks3NDoqeFW4PK".getBytes();
        for (byte b : ivk) {
            System.out.print(b+",");
        }
        System.out.println();
        try {
            System.out.println(new String(key));
            System.out.println(new String(ivk));
            String enc = encText(s, key, ivk);
            System.out.println(enc);
            String dec = decText(enc, key, ivk);
            System.out.println(dec);

            // If there is only one key and one iv, use AesInstance for better performance,but generally the iv need change everyTime

            // AesInstance ai = AesInstance.getInstance(key, ivk);
            // enc = ai.encText(s);
            // System.out.println(enc);
            // dec = ai.decText(enc);
            // System.out.println(dec);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
