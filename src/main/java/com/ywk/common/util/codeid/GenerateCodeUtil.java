package com.ywk.common.util.codeid;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;

/**
 * @description: 生成唯一code
 * @author: yanwenkai
 * @create: 2021-06-04 10:01
 **/
public class GenerateCodeUtil {

    /**
     * 十六进制转十进制
     * @param hexId
     * @return
     */
    public static Long change(String hexId) {
        BigInteger bigInteger = new BigInteger(hexId, 16);
        return bigInteger.longValue();
    }

    public static void main(String[] args) throws Exception {
//        String hexId = "60b5af3c dd7206 2fa3 ee4717";
//        String hexId = "60b5af3cdd72062fa3ee4717";
        String hexId = "60b5af3cdd72062fa3ee4714";
        String userCode = userCodeByUserCd(hexId);
        String userCd = userCodeToUserCd(userCode);
        System.out.println(userCode);
        System.out.println(userCd);
        System.out.println(userCd.equals(hexId));

        long snowflakeId = SnowflakeIdUtil.getSnowflakeId();
        System.out.println(snowflakeId);
    }

    public static String userCodeByUserCd(String userCd) {
        if (userCd.length() != 24) {
            return StringUtils.EMPTY;
        }
        String timeHex = userCd.substring(0, 8);
        Long time = change(timeHex);
        String timeStr = toSerialCode(time, 6);

        String onlySignHex = userCd.substring(8, 14);
        Long onlySignNum = change(onlySignHex);
        String onlyStr = toSerialCode(onlySignNum, 5);

        String pidHex = userCd.substring(14, 18);
        Long pidNum = change(pidHex);
        String pidStr = toSerialCode(pidNum, 4);

        String randomHex = userCd.substring(18, 24);
        Long randomNum = change(randomHex);
        String randomStr = toSerialCode(randomNum, 5);

        return timeStr + onlyStr + pidStr + randomStr;
    }

    public static String userCodeToUserCd(String userCode) {
        if (userCode.length() != 20) {
            return StringUtils.EMPTY;
        }

        String timeCode = userCode.substring(0, 6);
        long timeId = codeToId(timeCode);
        String timeHexStr = Long.toHexString(timeId);

        String onlyCode = userCode.substring(6, 11);
        long onlyId = codeToId(onlyCode);
        String onlyHexStr = Long.toHexString(onlyId);

        String pidCode = userCode.substring(11, 15);
        long pid = codeToId(pidCode);
        String pidHexStr = Long.toHexString(pid);

        String randomCode = userCode.substring(15, 20);
        long randomId = codeToId(randomCode);
        String randomHexStr = Long.toHexString(randomId);

        return timeHexStr + onlyHexStr + pidHexStr + randomHexStr;
    }

    /**
     * 进制
     */
    private static final char[] CHARS = new char[]{'q', 'w', 'e', '8', 's', '2', 'd', 'z', 'x', '9', 'c', '7', 'p', '5', 'k', '3', 'm', 'j', 'u', 'f', 'r', '4', 'v', 'y', 't', 'n', '6', 'b', 'g', 'h', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    /**
     * 定义一个字符用来补全邀请码长度（该字符前面是计算出来的邀请码，后面是用来补全用的）
     */
    private static final char CHAR_A = 'a';

    /**
     * 补位字符串
     */
    private static final String APPEND_STR = "ATgsGhj";

    /**
     * 进制长度
     */
    private static final int BIN_LEN = CHARS.length;

    /**
     * 生成code 码
     * @param id
     * @param s
     * @return
     */
    public static String toSerialCode(long id, int s) {
        char[] buf = new char[32];
        int charPos = 32;

        while ((id / BIN_LEN) > 0) {
            int ind = (int) (id % BIN_LEN);
            buf[--charPos] = CHARS[ind];
            id /= BIN_LEN;
        }
        buf[--charPos] = CHARS[(int) (id % BIN_LEN)];
        String str = new String(buf, charPos, (32 - charPos));


        //不够长度的自动随机补全
        if (str.length() < s) {
            str += CHAR_A;
            str += String.valueOf(APPEND_STR.subSequence(0, s - str.length()));
        }
        return str;
    }

    /**
     * 根据code码生成ID
     * @param code
     * @return
     */
    public static long codeToId(String code) {
        char[] chs = code.toCharArray();
        long res = 0L;
        for (int i = 0; i < chs.length; i++) {
            int ind = 0;
            for (int j = 0; j < BIN_LEN; j++) {
                if (chs[i] == CHARS[j]) {
                    ind = j;
                    break;
                }
            }
            if (chs[i] == CHAR_A) {
                break;
            }
            if (i > 0) {
                res = res * BIN_LEN + ind;
            } else {
                res = ind;
            }
        }
        return res;
    }

}
