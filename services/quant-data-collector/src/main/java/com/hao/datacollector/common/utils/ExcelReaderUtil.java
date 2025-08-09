package com.hao.datacollector.common.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ExcelReaderUtil {
    private static List<String> headers = new ArrayList<>();

    public static List<Map<String, String>> readExcel(File file) {
        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // 读取第一个Sheet
            List<Map<String, String>> result = new ArrayList<>();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;

            int columnCount = headerRow.getPhysicalNumberOfCells();
            List<String> headers = new ArrayList<>();

            for (int i = 0; i < columnCount; i++) {
                headers.add(getCellValue(headerRow.getCell(i)));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < columnCount; j++) {
                    String header = headers.get(j);
                    String cellValue = getCellValue(row.getCell(j));
                    rowData.put(header, cellValue);
                }
                result.add(rowData);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("读取Excel文件失败: " + e.getMessage(), e);
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell) ?
                    cell.getDateCellValue().toString() :
                    String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * 只读取Excel第一行表头，返回表头列名列表
     */
    public static List<String> readHeaders(File file) {
        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return Collections.emptyList();

            int columnCount = headerRow.getPhysicalNumberOfCells();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                headers.add(getCellValue(headerRow.getCell(i)));
            }
            return headers;
        } catch (Exception e) {
            throw new RuntimeException("读取Excel表头失败: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        File file = new File("C:\\Users\\lihao\\Desktop\\data\\1.xlsx");
        List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(file);
        // 打印数据
        dataList.forEach(row -> {
            System.out.println("证券代码: " + row.get("证券代码"));
            System.out.println("公司简介: " + row.get("公司简介"));
        });

    }
}
