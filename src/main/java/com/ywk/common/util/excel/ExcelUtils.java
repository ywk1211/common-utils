package com.ywk.common.util.excel;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ywk.common.util.excel.annotation.ExcelColumn;
import com.ywk.common.util.excel.annotation.ExcelSheet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author yanwenkai
 * @Date 2020/10/14 3:14 下午
 * @Description Excel 导入导出工具类
 */
@Slf4j
public class ExcelUtils {

    private final static String EXCEL2003 = "xls";
    private final static String EXCEL2007 = "xlsx";

    /**
     * 读取excel
     *
     * @param cls
     * @param originalFileName
     * @param inputStream
     * @param <T>
     * @return
     */
    public static <T> List<T> readExcel(Class<T> cls, String originalFileName, InputStream inputStream) {
        // org.springframework.web.multipart.MultipartFile file.getOriginalFileName()
        String fileName = originalFileName;
        if (!fileName.matches("^.+\\.(?i)(xls)$") && !fileName.matches("^.+\\.(?i)(xlsx)$")) {
            log.error("上传文件格式不正确");
        }
        List<T> dataList = new ArrayList<>();
        Workbook workbook = null;
        try {
            // org.springframework.web.multipart.MultipartFile file.getInputStream()
            InputStream is = inputStream;
            if (fileName.endsWith(EXCEL2007)) {
                workbook = new XSSFWorkbook(is);
            }
            if (fileName.endsWith(EXCEL2003)) {
                workbook = new HSSFWorkbook(is);
            }
            if (workbook != null) {
                //类映射  注解 value-->bean columns
                Map<String, List<Field>> classMap = Maps.newHashMap();
                List<Field> fields = Stream.of(cls.getDeclaredFields()).collect(Collectors.toList());
                fields.forEach(field -> {
                            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                            if (annotation != null) {
                                String value = annotation.value();
                                if (StringUtils.isBlank(value)) {
                                    return;
                                }
                                if (!classMap.containsKey(value)) {
                                    classMap.put(value, Lists.newArrayList());
                                }
                                field.setAccessible(true);
                                classMap.get(value).add(field);
                            }
                        }
                );
                //索引-->columns
                Map<Integer, List<Field>> reflectionMap = new HashMap<>(16);
                //默认读取第一个sheet
                Sheet sheet = workbook.getSheetAt(0);

                boolean firstRow = true;
                for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    //首行  提取注解
                    if (firstRow) {
                        for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                            Cell cell = row.getCell(j);
                            String cellValue = getCellValue(cell);
                            if (classMap.containsKey(cellValue)) {
                                reflectionMap.put(j, classMap.get(cellValue));
                            }
                        }
                        firstRow = false;
                    } else {
                        //忽略空白行
                        if (row == null) {
                            continue;
                        }
                        try {
                            T t = cls.newInstance();
                            //判断是否为空白行
                            boolean allBlank = true;
                            for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                                if (reflectionMap.containsKey(j)) {
                                    Cell cell = row.getCell(j);
                                    String cellValue = getCellValue(cell);
                                    if (StringUtils.isNotBlank(cellValue)) {
                                        allBlank = false;
                                    }
                                    List<Field> fieldList = reflectionMap.get(j);
                                    fieldList.forEach(x -> {
                                                try {
                                                    handleField(t, cellValue, x);
                                                } catch (Exception e) {
                                                    log.error(String.format("reflect field:%s value:%s exception!", x.getName(), cellValue), e);
                                                }
                                            }
                                    );
                                }
                            }
                            if (!allBlank) {
                                dataList.add(t);
                            } else {
                                log.warn(String.format("row:%s is blank ignore!", i));
                            }
                        } catch (Exception e) {
                            log.error(String.format("parse row:%s exception!", i), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("parse excel exception!", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    log.error("parse excel exception!", e);
                }
            }
        }
        return dataList;
    }

    private static <T> void handleField(T t, String value, Field field) throws Exception {
        Class<?> type = field.getType();
        if (type == void.class || StringUtils.isBlank(value)) {
            return;
        }
        if (type == Object.class) {
            field.set(t, value);
            //数字类型
        } else if (type.getSuperclass() == null || type.getSuperclass() == Number.class) {
            if (type == int.class || type == Integer.class) {
                field.set(t, NumberUtils.toInt(value));
            } else if (type == long.class || type == Long.class) {
                field.set(t, NumberUtils.toLong(value));
            } else if (type == byte.class || type == Byte.class) {
                field.set(t, NumberUtils.toByte(value));
            } else if (type == short.class || type == Short.class) {
                field.set(t, NumberUtils.toShort(value));
            } else if (type == double.class || type == Double.class) {
                field.set(t, NumberUtils.toDouble(value));
            } else if (type == float.class || type == Float.class) {
                field.set(t, NumberUtils.toFloat(value));
            } else if (type == char.class || type == Character.class) {
                field.set(t, CharUtils.toChar(value));
            } else if (type == boolean.class) {
                field.set(t, BooleanUtils.toBoolean(value));
            } else if (type == BigDecimal.class) {
                field.set(t, new BigDecimal(value));
            }
        } else if (type == Boolean.class) {
            field.set(t, BooleanUtils.toBoolean(value));
        } else if (type == Date.class) {
            //
            field.set(t, value);
        } else if (type == String.class) {
            field.set(t, value);
        } else {
            Constructor<?> constructor = type.getConstructor(String.class);
            field.set(t, constructor.newInstance(value));
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                return HSSFDateUtil.getJavaDate(cell.getNumericCellValue()).toString();
            } else {
                return BigDecimal.valueOf(cell.getNumericCellValue()).toString();
            }
        } else if (cell.getCellType() == CellType.STRING) {
            return StringUtils.trimToEmpty(cell.getStringCellValue());
        } else if (cell.getCellType() == CellType.FORMULA) {
            return StringUtils.trimToEmpty(cell.getCellFormula());
        } else if (cell.getCellType() == CellType.BLANK) {
            return "";
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == CellType.ERROR) {
            return "ERROR";
        } else {
            return cell.toString().trim();
        }

    }

    public static <T> void writeExcel(HttpServletResponse response, List<T> dataList, Class<T> cls){
        log.info("dataListSize:{}", dataList.size());
        String fileName = "export";
        String sheetName = "Sheet1";
        ExcelSheet excelSheet = cls.getAnnotation(ExcelSheet.class);
        if(excelSheet != null) {
            String value = excelSheet.value();
            String sheet = excelSheet.sheetName();
            if(StringUtils.isNotBlank(value)) {
                fileName = value;
            }
            if(StringUtils.isNotBlank(sheet)) {
                sheetName = sheet;
            }
        }

        Field[] fields = cls.getDeclaredFields();
        List<Field> fieldList = Arrays.stream(fields).filter(field -> {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null && annotation.col() > 0) {
                field.setAccessible(true);
                return true;
            }
            return false;
        }).sorted(Comparator.comparing(field -> {
            int col = 0;
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null) {
                col = annotation.col();
            }
            return col;
        })).collect(Collectors.toList());

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);
        AtomicInteger ai = new AtomicInteger();
        {
            Row row = sheet.createRow(ai.getAndIncrement());
            AtomicInteger aj = new AtomicInteger();
            //写入头部
            fieldList.forEach(field -> {
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                String columnName = "";
                int width = 0;
                if (annotation != null) {
                    columnName = annotation.value();
                    width = annotation.width();
                }
                int colIndex = aj.getAndIncrement();
                Cell cell = row.createCell(colIndex);

                CellStyle cellStyle = wb.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);

                Font font = wb.createFont();
                cellStyle.setFont(font);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(columnName);
                if(width > 0) {
                    // 设置列宽
                    sheet.setColumnWidth(colIndex, width * 256);
                }
            });
        }
        if (CollectionUtils.isNotEmpty(dataList)) {
            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Map<Integer, Row> rowMap = new HashMap<>();
            for (T t : dataList) {
                int rowNum = ai.getAndIncrement();
                Row row = sheet.createRow(rowNum);
                rowMap.put(rowNum, row);

                // List类型字段的最大size，合并单元格使用(rowSpan)
                int maxRowSpan = 1;
                List<Integer> rowSpanColumnList = new ArrayList<>();
                AtomicInteger aj = new AtomicInteger();
                for (Field field : fieldList) {
                    Class<?> type = field.getType();
                    ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                    boolean hAlignCenter = annotation.hAlignCenter();
                    Object value = null;
                    try {
                        value = field.get(t);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int columnNum = aj.getAndIncrement();

                    if (type == List.class) {
                        List valList = (List) value;
                        if(valList.size() == 0) {
                            Cell cell = row.createCell(columnNum);
                            if(hAlignCenter) {
                                cell.setCellStyle(centerStyle);
                            }
                            cell.setCellValue(StringUtils.EMPTY);
                            rowSpanColumnList.add(columnNum);
                            continue;
                        }
                        maxRowSpan = Math.max(maxRowSpan, valList.size());
                        int subRowNum = rowNum;
                        for (Object val : valList) {
                            Row subRow = rowMap.get(subRowNum);
                            if(subRow == null) {
                                subRow = sheet.createRow(subRowNum);
                                rowMap.put(subRowNum, subRow);
                            }
                            Cell cell = subRow.createCell(columnNum);
                            if(hAlignCenter) {
                                cell.setCellStyle(centerStyle);
                            }
                            cell.setCellValue(Optional.ofNullable(val).map(Object::toString).orElse(StringUtils.EMPTY));
                            subRowNum ++;
                        }
                        if(valList.size() == 1) {
                            rowSpanColumnList.add(columnNum);
                        }
                    } else {
                        Cell cell = row.createCell(columnNum);
                        if(hAlignCenter) {
                            cell.setCellStyle(centerStyle);
                        }
                        cell.setCellValue(Optional.ofNullable(value).map(Object::toString).orElse(StringUtils.EMPTY));
                        rowSpanColumnList.add(columnNum);
                    }
                }
                // 合并单元格
                if(maxRowSpan > 1) {
                    int finalMaxRowSpan = maxRowSpan;
                    rowSpanColumnList.forEach(columnNum-> {
                        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + finalMaxRowSpan-1, columnNum, columnNum));
                    });
                    ai.getAndAdd(maxRowSpan - 1);
                }
            }
        }
        //冻结窗格
        wb.getSheet(sheetName).createFreezePane(0, 1, 0, 1);
        //浏览器下载excel
        buildExcelDocument(wb, null, response, fileName);
//        writeExcel(wb, ".\\default.xlsx");
        log.info("export excel finish ..........");
        //生成excel文件
//        buildExcelFile(".\\default.xlsx",wb);
    }

    public static Map<String,Integer> getExcelHeadWithIndexMap(Class cls){
        Field[] fields = cls.getDeclaredFields();
        Map<String, Integer> stringIntegerHashMap = new HashMap<>();

        AtomicInteger index = new AtomicInteger();
        Arrays.stream(fields).filter(field -> {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null && annotation.col() > 0) {
                field.setAccessible(true);
                return true;
            }
            return false;
        }).sorted(Comparator.comparingInt(o -> o.getAnnotation(ExcelColumn.class).col())).forEach(e -> {
            stringIntegerHashMap.put(e.getName(), index.getAndIncrement());
        });
        return stringIntegerHashMap;
    }

    /**
     * 浏览器下载excel
     * @param wb
     * @param response
     */
    private static  void  buildExcelDocument(Workbook wb, HttpServletResponse response){
        buildExcelDocument(wb, null, response, null);
    }

    /**
     * 浏览器下载excel
     *
     * @param wb
     * @param response
     */
    private static void buildExcelDocument(Workbook wb, String contentType, HttpServletResponse response, String filename) {
        try {
            if (StringUtils.isBlank(filename)) {
                filename = "export";
            }
            if (StringUtils.isBlank(contentType)) {
                contentType = "application/octet-stream";
            }
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename + ".xlsx", "utf-8"));
            response.flushBuffer();
            wb.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeExcel(Workbook wb, String path) {
        OutputStream out = null;
        try {
            File file = new File(path);
            out = new FileOutputStream(file.isDirectory() ? path + "/" + "export" + ".xls" : path);
            wb.write(out);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public static boolean validateExcelTemplate(List<String> dataHeader,Class cls){
        Field[] fields = cls.getDeclaredFields();
        List<String> excelHeadWithIndexMap = Arrays.stream(fields).filter(field -> {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null && annotation.col() > 0) {
                field.setAccessible(true);
                return true;
            }
            return false;
        }).sorted(Comparator.comparingInt(o -> o.getAnnotation(ExcelColumn.class).col()))
                .map(e->e.getAnnotation(ExcelColumn.class).value()).collect(Collectors.toList());

        for(int i=0;i<dataHeader.size();i++){
            if(!excelHeadWithIndexMap.get(i).equals(dataHeader.get(i))){
                return false;
            }
        }
        return true;
    }
    public static List<List<String>> readFromRemoteWithHead(String fileUrl, int sheetIndex) throws IOException {
        List<List<String>> allRows = new ArrayList<>();
        InputStream is = null;
        Workbook wb = null;
        try {
            URL url = new URL(fileUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3 * 60 * 1000);
            is = conn.getInputStream();
            wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(sheetIndex);
            int maxRowNum = sheet.getLastRowNum();
            int minRowNum = sheet.getFirstRowNum();

            // 跳过头，从第二行开始读取
            for (int i = minRowNum; i <= maxRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                List<String> rowData = readLine(row);
                allRows.add(rowData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            if (is != null) {
                is.close();
            }
            if (wb != null && wb instanceof SXSSFWorkbook) {
                SXSSFWorkbook xssfwb = (SXSSFWorkbook) wb;
                xssfwb.dispose();
            }
        }

        return allRows;
    }

    //读取每行数据
    private static List<String> readLine(Row row) {
        short minColNum = row.getFirstCellNum();
        short maxColNum = row.getLastCellNum();
        List<String> dataList = new ArrayList<>();
        for (short colIndex = minColNum; colIndex < maxColNum; colIndex++) {
            Cell cell = row.getCell(colIndex);
            cell.setCellType(CellType.STRING);
            String value;
            if (cell != null) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    value = String.valueOf(cell.getNumericCellValue());
                } else {
                    value = cell.getStringCellValue();
                }
            } else {
                value = "";
            }
            dataList.add(value);
        }

        return dataList;
    }

    public static List<String> getExcelHeadList(Class cls){
        Field[] fields = cls.getDeclaredFields();
        List<String> collect = Arrays.stream(fields).filter(field -> {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation != null && annotation.col() > 0) {
                field.setAccessible(true);
                return true;
            }
            return false;
        }).sorted(Comparator.comparingInt(o -> o.getAnnotation(ExcelColumn.class).col()))
                .map(Field::getName).collect(Collectors.toList());
        return collect;
    }
    public static List<List<String>> readFromRemote(String fileUrl, int sheetIndex) throws IOException {
        List<List<String>> allRows = new ArrayList<>();
        InputStream is = null;
        Workbook wb = null;
        try {
            URL url = new URL(fileUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3 * 60 * 1000);
            is = conn.getInputStream();
            wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(sheetIndex);
            int maxRowNum = sheet.getLastRowNum();
            int minRowNum = sheet.getFirstRowNum();

            // 跳过头，从第二行开始读取
            for (int i = minRowNum + 1; i <= maxRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                List<String> rowData = readLine(row);
                allRows.add(rowData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            if (is != null) {
                is.close();
            }
            if (wb != null && wb instanceof SXSSFWorkbook) {
                SXSSFWorkbook xssfwb = (SXSSFWorkbook) wb;
                xssfwb.dispose();
            }
        }

        return allRows;
    }
}
