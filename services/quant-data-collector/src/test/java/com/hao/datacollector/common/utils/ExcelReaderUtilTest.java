package com.hao.datacollector.common.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderUtilTest {

    private File testExcelFile;
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // 创建测试用的Excel文件
        testExcelFile = tempDir.resolve("test.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            Cell headerCell1 = headerRow.createCell(0);
            headerCell1.setCellValue("证券代码");
            Cell headerCell2 = headerRow.createCell(1);
            headerCell2.setCellValue("公司简介");
            Cell headerCell3 = headerRow.createCell(2);
            headerCell3.setCellValue("数值");
            Cell headerCell4 = headerRow.createCell(3);
            headerCell4.setCellValue("日期");
            Cell headerCell5 = headerRow.createCell(4);
            headerCell5.setCellValue("布尔值");
            
            // 创建数据行1
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("000001");
            dataRow1.createCell(1).setCellValue("测试公司1");
            
            Cell numericCell1 = dataRow1.createCell(2);
            numericCell1.setCellValue(123.45);
            
            Cell dateCell1 = dataRow1.createCell(3);
            dateCell1.setCellValue(new java.util.Date());
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            dateCell1.setCellStyle(dateCellStyle);
            
            Cell boolCell1 = dataRow1.createCell(4);
            boolCell1.setCellValue(true);
            
            // 创建数据行2
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("000002");
            dataRow2.createCell(1).setCellValue("测试公司2");
            dataRow2.createCell(2).setCellValue(456.78);
            
            Cell dateCell2 = dataRow2.createCell(3);
            dateCell2.setCellValue(new java.util.Date());
            dateCell2.setCellStyle(dateCellStyle);
            
            Cell boolCell2 = dataRow2.createCell(4);
            boolCell2.setCellValue(false);
            
            // 保存Excel文件
            try (FileOutputStream fileOut = new FileOutputStream(testExcelFile)) {
                workbook.write(fileOut);
            }
        }
    }
    
    @Test
    void testReadExcel() {
        // 测试读取Excel文件
        List<Map<String, String>> result = ExcelReaderUtil.readExcel(testExcelFile);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // 验证第一行数据
        Map<String, String> row1 = result.get(0);
        assertEquals("000001", row1.get("证券代码"));
        assertEquals("测试公司1", row1.get("公司简介"));
        assertTrue(row1.get("数值").contains("123.45"));
        assertNotNull(row1.get("日期"));
        assertEquals("true", row1.get("布尔值"));
        
        // 验证第二行数据
        Map<String, String> row2 = result.get(1);
        assertEquals("000002", row2.get("证券代码"));
        assertEquals("测试公司2", row2.get("公司简介"));
        assertTrue(row2.get("数值").contains("456.78"));
        assertNotNull(row2.get("日期"));
        assertEquals("false", row2.get("布尔值"));
    }
    
    @Test
    void testReadHeaders() {
        // 测试读取表头
        List<String> headers = ExcelReaderUtil.readHeaders(testExcelFile);
        
        // 验证结果
        assertNotNull(headers);
        assertEquals(5, headers.size());
        assertEquals("证券代码", headers.get(0));
        assertEquals("公司简介", headers.get(1));
        assertEquals("数值", headers.get(2));
        assertEquals("日期", headers.get(3));
        assertEquals("布尔值", headers.get(4));
    }
    
    @Test
    void testReadExcelWithEmptyFile() throws Exception {
        // 创建一个空的Excel文件
        File emptyFile = new File(testExcelFile.getParentFile(), "empty.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("EmptySheet");
            try (FileOutputStream fileOut = new FileOutputStream(emptyFile)) {
                workbook.write(fileOut);
            }
        }
        
        // 测试读取空Excel文件
        List<Map<String, String>> result = ExcelReaderUtil.readExcel(emptyFile);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testReadHeadersWithEmptyFile() throws Exception {
        // 创建一个空的Excel文件
        File emptyFile = new File(testExcelFile.getParentFile(), "empty.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("EmptySheet");
            try (FileOutputStream fileOut = new FileOutputStream(emptyFile)) {
                workbook.write(fileOut);
            }
        }
        
        // 测试读取空Excel文件的表头
        List<String> headers = ExcelReaderUtil.readHeaders(emptyFile);
        
        // 验证结果
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }
    
    @Test
    void testReadExcelWithNullCells() throws Exception {
        // 创建带有空单元格的Excel文件
        File fileWithNullCells = new File(testExcelFile.getParentFile(), "null_cells.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("NullCellsSheet");
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("列1");
            headerRow.createCell(1).setCellValue("列2");
            
            // 创建数据行，第二列为空
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("值1");
            // 不创建第二列的单元格，使其为null
            
            try (FileOutputStream fileOut = new FileOutputStream(fileWithNullCells)) {
                workbook.write(fileOut);
            }
        }
        
        // 测试读取带有空单元格的Excel文件
        List<Map<String, String>> result = ExcelReaderUtil.readExcel(fileWithNullCells);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("值1", result.get(0).get("列1"));
        assertEquals("", result.get(0).get("列2")); // 空单元格应该返回空字符串
    }
}