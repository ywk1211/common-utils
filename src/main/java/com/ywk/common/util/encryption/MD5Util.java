package com.ywk.common.util.encryption;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;

/**
 * @description: MD5加密
 * @author: yanwenkai
 * @create: 2021-05-07 19:11
 **/
public class MD5Util {

    /**
     * 全局数组
     */
    private final static String[] STR_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    public MD5Util() {
    }

    /**
     * 获取盐值（随机字符串）
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }



    /**
     * 返回形式为数字跟字符串
     * @param bByte
     * @return
     */
    private static String byteToArrayString(byte bByte) {
        int iRet = bByte;
        if (iRet < 0) {
            iRet += 256;
        }
        int iD1 = iRet / 16;
        int iD2 = iRet % 16;
        return STR_DIGITS[iD1] + STR_DIGITS[iD2];
    }


    /**
     * 转换字节数组为16进制字串
     * @param bByte
     * @return
     */
    private static String byteToString(byte[] bByte) {
        StringBuilder sBuffer = new StringBuilder();
        for (int i = 0; i < bByte.length; i++) {
            sBuffer.append(byteToArrayString(bByte[i]));
        }
        return sBuffer.toString();
    }

    public static String getMD5Code(String strObj) throws Exception{
        String resultString = null;
        try {
            resultString = strObj;
            MessageDigest md = MessageDigest.getInstance("MD5");
            // md.digest() 该函数返回值为存放哈希值结果的byte数组
            resultString = byteToString(md.digest(strObj.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw ex;
        }
        return resultString;
    }

    public static String encodePassword(String password, String salt) throws Exception{
        return getMD5Code(password + getMD5Code(salt));
    }

    public static String encodeAdminPassword(String password, String salt) throws Exception{
        return getMD5Code(getMD5Code(password) + salt);
    }

    public static boolean matchesPassword(String salt,String rawPassword, String encodedPassword) throws Exception{
        return encodePassword(rawPassword, salt).equals(encodedPassword);
    }

    public static boolean matchesAdminPassword(String salt,String rawPassword, String encodedPassword) throws Exception{
        return encodeAdminPassword(rawPassword, salt).equals(encodedPassword);
    }

    public static void main(String[] args) throws Exception{
//        String salt = "Mjc1NDk=";
//        String password = "QAZwsx123";
//        String encodePassword = "c68789643b7c7b93f7e43be3ffabbe56";
//
//        boolean b = matchesAdminPassword(salt, password, encodePassword);
//        System.out.println(b);
//        String s = encodeAdminPassword(password, salt);
//        System.out.println(s);

//        String md5Code = getMD5Code("0r1LwtQ8");
        Set<String> set = Sets.newHashSet();
        set.add("13439784987");

        JSONArray array = new JSONArray();
        for (String str : set) {
            JSONObject map = new JSONObject();
            String md5Code = getMD5Code(str);
            map.put("md5", md5Code);
            map.put("mobile", str);
            array.add(map);
        }
        System.out.println(array);
    }
}
