package com.ywk.common.util.excel.annotation;

import java.lang.annotation.*;

/**
 * @Author yanwenkai
 * @Date 2020/10/14 3:12 下午
 * @Description excel文件名表名
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelSheet {

    /**
     * Excel标题
     * @return
     */
    String value() default "";

    /**
     * Excel从左往右排列位置
     * @return
     */
    String sheetName() default "";
}
