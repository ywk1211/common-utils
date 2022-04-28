package com.ywk.common.util.excel.annotation;

import java.lang.annotation.*;

/**
 * @Author yanwenkai
 * @Date 2020/10/14 3:12 下午
 * @Description excel属性标题 位置
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * Excel标题
     * @return
     */
    String value() default "";

    /**
     * 列宽（字符数）
     * @return
     */
    int width() default 0;

    /**
     * 是否水平居中
     * @return
     */
    boolean hAlignCenter() default false;

    /**
     * Excel从左往右排列位置
     * @return
     */
    int col() default 0;
}
