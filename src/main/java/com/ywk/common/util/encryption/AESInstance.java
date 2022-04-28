package com.ywk.common.util.encryption;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @author yanwenkai
 * @description
 * @date 2022/4/11 17:20
 **/
public class AESInstance {

    private Cipher encCipher =  null;
    private Cipher decCipher =  null;

    private AESInstance(byte[] key, byte[] iv) throws Exception {
        this.encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivc =  new IvParameterSpec(iv);
        this.encCipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivc);
        this.decCipher.init(Cipher.DECRYPT_MODE, skeySpec, ivc);
    }

    public byte[] encBytes(byte[] srcBytes) throws Exception {
        return encCipher.doFinal(srcBytes);
    }

    public byte[] decBytes(byte[] srcBytes) throws Exception {
        return decCipher.doFinal(srcBytes);
    }

    public String encText(String srcStr) throws Exception {
        byte[] srcBytes = srcStr.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encBytes(srcBytes);
        return Base64.encode(encrypted);
    }

    public String decText(String srcStr) throws Exception {
        byte[] srcBytes = Base64.decode(srcStr);
        byte[] decrypted = decBytes(srcBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static AESInstance getInstance(byte[] key, byte[] iv) throws Exception {
        return new AESInstance(key,iv);
    }
}
