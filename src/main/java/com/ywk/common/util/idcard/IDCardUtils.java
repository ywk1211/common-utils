package com.ywk.common.util.idcard;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * @description: 身份证工具类
 * @author: yanwenkai
 * @create: 2021-05-12 16:42
 **/
public class IDCardUtils {

    private static Pattern pattern = Pattern.compile("^\\d{17}[\\d|X]$");

    /**
     * 省、直辖市代码表，身份证号的前6位为地址信息，我们只验证前两位
     */
    private static final String[] CITY_CODE = {
            "11", "12", "13", "14", "15", "21", "22", "23", "31", "32", "33", "34", "35", "36", "37", "41",
            "42", "43", "44", "45", "46", "50", "51", "52", "53", "54", "61", "62", "63", "64", "65", "71",
            "81", "82", "91"
    };

    /**
     * 每位加权因子
     */
    private static final int[] POWER = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /**
     * 第18位校检码
     **/
    private static final String[] VERIFY_CODE = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

    /**
     * 验证身份证
     * @param idno
     * @return
     */
    public static boolean verify(String idno){
        //1.格式验证
        if(idno == null || !pattern.matcher(idno = idno.toUpperCase()).matches()){
            return false;
        }

        //2.验证省、直辖市代码。市、区不作验证，没有规则限制，数字即可
        if(Arrays.binarySearch(CITY_CODE, idno.substring(0, 2)) == -1){
            return false;
        }

        //3.验证生日,生日可能存在输入20180231这种情况，所以使用Calendar处理校验
        String birthday = idno.substring(6, 14);
        // 如果输入的日期为20180231，通过转换的后realBirthday为20180303
        Date realBirthday = toBirthDay(birthday);
        // 转换失败或不相等
        if(realBirthday == null || !birthday.equals(new SimpleDateFormat("yyyyMMdd").format(realBirthday))){
            return false;
        }

        //4.顺序码不作验证，没有规则限制，数字即可
        //5.验证位验证，计算规则为：身份证前17位数字，对应乘以每位的权重因子，然后相加得到数值X，与11取模获得余数，得到数值Y,通过Y得到校验码。
        String verifyCode = VERIFY_CODE[getPowerSum(idno) % 11];
        if(!verifyCode.equals(idno.substring(17, 18))){
            return false;
        }
        return true;
    }

    /**
     * 取得身份证号前17位与对应的权重值相乘的和
     * @return
     */
    private static int getPowerSum(String cardId){
        int sum = 0;
        // 身份证前17位
        char[] fix17 = cardId.substring(0, 17).toCharArray();
        for(int i = 0 ; i <= 16 ; i++){
            sum += (Integer.parseInt(fix17[i] + "") * POWER[i]);
        }
        return sum;
    }

    /**
     * 转换成日期
     * @param birthday
     * @return
     */
    private static Date toBirthDay(String birthday){
        try{
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, Integer.parseInt(birthday.substring(0, 4)));
            //月份从0开始，所以减1
            calendar.set(Calendar.MONTH, Integer.parseInt(birthday.substring(4, 6)) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(birthday.substring(6, 8)));
            //以下设置意义不大
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar.getTime();
        }catch (Exception e){
            return null;
        }
    }
    /**
     * 从身份证号码中获取生日
     * @param cardId
     * @return null表示idno错误，未获取到生日
     */
    public static Date getBirthDay(String cardId){
        if(!verify(cardId)){
            return null;
        }

        return toBirthDay(cardId.substring(6, 14));
    }

    /**
     * 从身份证号中获取性别
     * @param cardId
     * @return 0:获取失败，1:男，2:女
     */
    public static int getGender(String cardId){
        if(!verify(cardId)){
            return 0;
        }
        // 奇男，偶女
        return (Integer.parseInt(cardId.substring(16, 17)) % 2) == 0 ? 2 : 1;
    }

    public static void main(String[] args) {
        String cardId = "1234567890987654321";
        boolean verify = verify(cardId);
        System.out.println(verify);
        Date birthDay = getBirthDay(cardId);
        System.out.println(birthDay);
        int gender = getGender(cardId);
        System.out.println(gender);
        int powerSum = getPowerSum(cardId);
        System.out.println(powerSum);
    }
}
