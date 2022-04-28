package com.ywk.common.util.random;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @description: 字符串处理工具类
 * @author: yanwenkai
 * @date 2021年5月8日19:21:07
 */
public class RandomCodeUtil {
    private static SecureRandom secureRandom;
    private static final String BASE_NUMBER = "012340123456789560123456789789012345678901234567891234567890347";
    private static final String BASE_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLOMNOPQRSTUVWXYZ1234567890";

    static {
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            secureRandom = new SecureRandom();
        }
    }

    /**
     * 字符串首字母小写
     *
     * @param str
     * @return
     */
    public static String toLowerCaseFirstOne(String str) {
        if (str == null || "".equals(str)) {
            return str;
        }
        if (Character.isLowerCase(str.charAt(0))) {
            return str;
        } else {
            return Character.toLowerCase(str.charAt(0)) + str.substring(1);
        }
    }

    public static String random(int length, String base) {
        secureRandom.setSeed(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = secureRandom.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String randomNumber(int length) {
        return random(length, BASE_NUMBER);
    }

    public static String randomString(int length) {
        return random(length, BASE_STRING);
    }

    public static void main(String[] args) {
//        String number = randomNumber(10);
//        System.out.println(number);
//        String string = randomString(10);
//        System.out.println(string);
        String s = id2ShareCode(99999999);
        System.out.println(s);
        long l = shareCode2id(s);
        System.out.println(l);
    }

    /** 邀请码最小长度,生成邀请码小于该字段，自动补长  **/
    private static final int MIN_CODE_LENGTH = 8;
    /** 位数不足时自动补长时，充当分隔，该字段为保持唯一性，不放入下方的列表  **/
    private static final String STOP_CHAR = "Z";
    /**
     * 考虑用户体验，此处去掉了 i o 1 0，具体列表内容自由换序
     **/
    private static final String[] CHARS = new String[]{"2", "a", "b", "c", "d", "e", "f", "g", "h", "3", "9", "j", "k", "m", "A", "B", "C", "D", "E", "F", "G", "4", "H", "n", "p", "q", "s", "r", "t", "u", "v", "5", "6", "7", "8", "w", "x", "y", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"};

    private static final int OFFSET = CHARS.length - 1;

    /**
     * 根据 id 生成邀请码
     * 如果是 6 位的邀请码只能支持 754137930 7亿5千万用户, 超出的id 会成为7位及以上的邀请码
     * @param id 用户id
     * @return 邀请码字符串
     */
    public static String id2ShareCode(int id) {
        String code = int2chars(id);
        int tailLength = MIN_CODE_LENGTH - code.length();
        if (tailLength > 1) {
            code = code + STOP_CHAR + codeTail(tailLength - 1);
        } else if (tailLength == 1) {
            code = code + STOP_CHAR;
        }
        return code;
    }

    /**
     * 根据邀请码 获取 id
     * @param code 邀请码
     * @return 用户id
     */
    public static int shareCode2id(String code) {
        int inx = code.indexOf(STOP_CHAR);
        if (inx > 0) {
            code = code.substring(0, inx);
        }
        return chars2int(code);
    }

    /**
     * 获取补长的邀请码（随机）
     * @param len 需要的长度
     * */
    private static String codeTail(int len) {
        String res = "";
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            res += CHARS[r.nextInt(OFFSET)];
        }
        return res;
    }

    private static String int2chars(int id) {
        int x = id / OFFSET;
        int remainder = id % OFFSET;
        if (x == 0) {
            return CHARS[id];
        } else if (x < OFFSET) {
            return CHARS[x] + CHARS[remainder];
        } else {
            return int2chars(x) + CHARS[remainder];
        }
    }

    private static int chars2int(String chars) {
        int res = 0;
        int codeLen = chars.length();
        List<String> totalCharsList = Arrays.asList(CHARS);

        for (int i = 0; i < codeLen; i++) {
            String a = chars.substring(i, i+1);
            if (STOP_CHAR.equals(a)) {
                break;
            }
            if (totalCharsList.contains(a)) {
                res = res * OFFSET + totalCharsList.indexOf(a);
            } else {
                break;
            }
        }
        return res;
    }
}
