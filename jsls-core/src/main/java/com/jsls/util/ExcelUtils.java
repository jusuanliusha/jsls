package com.jsls.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jxls.transform.poi.JxlsPoiTemplateFillerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.jsls.core.Importable;
import com.jsls.core.Importable.Position;
import com.jsls.core.Pair;
import com.jsls.core.Result;
import com.jsls.core.Verifiable;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class ExcelUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    public static List<List<String>> readExcel(MultipartFile file, String sheetName) throws IOException {
        String fileName = file.getOriginalFilename();
        if (StringUtils.hasText(fileName) && fileName.toLowerCase().endsWith(".csv")) {
            return readCSV(file.getInputStream());
        }
        Workbook webbook = loadWorkbook(file);
        return readWorkbook(webbook, sheetName);
    }

    public static List<List<String>> readExcel(File file, String sheetName) throws IOException {
        String fileName = file.getName();
        if (fileName.toLowerCase().endsWith(".csv")) {
            InputStream in = new FileInputStream(file);
            List<List<String>> csvData = readCSV(in);
            in.close();
            return csvData;
        }
        Workbook webbook = loadWorkbook(file);
        return readWorkbook(webbook, sheetName);
    }

    public static Workbook loadWorkbook(MultipartFile file) throws IOException {
        return loadWorkbook(file.getOriginalFilename(), file.getInputStream());
    }

    public static Workbook loadWorkbook(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        Workbook workbook = loadWorkbook(file.getName(), in);
        in.close();
        return workbook;
    }

    public static List<List<String>> readWorkbook(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        List<List<String>> rowList = new ArrayList<>();
        readSheet(sheet, rowList);
        return rowList;
    }

    public static void readSheet(Sheet sheet, List<List<String>> rowList) {
        if (sheet == null || sheet.getLastRowNum() < 1) {
            return;
        }
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row a = sheet.getRow(rowNum);
            if (a == null) {
                continue;
            }

            int cellLength = a.getLastCellNum();

            Row xssfRow = sheet.getRow(rowNum);
            // Read the cell
            if (xssfRow != null) {
                List<String> rowContent = new ArrayList<>();
                for (int cellNum = 0; cellNum < cellLength; cellNum++) {
                    String value = getValue(xssfRow.getCell(cellNum));
                    rowContent.add(value);
                }
                rowList.add(rowContent);
            }
        }
    }

    /**
     * 获取单元格的值
     * 
     * @param xssfCell 单元格对象
     * @return String 单元格值
     */
    private static String getValue(Cell xssfCell) {
        String cellValue = "";
        if (null == xssfCell) {
            return cellValue;
        }
        CellType cellType = xssfCell.getCellType();
        switch (cellType) {
            case STRING:
                cellValue = xssfCell.getStringCellValue();
                break;
            case BOOLEAN:
                cellValue = String.valueOf(xssfCell.getBooleanCellValue());
                break;
            case NUMERIC:
                if (xssfCell.getCellStyle().getDataFormatString().indexOf("%") > -1) {// 百分比
                    cellValue = String.valueOf(xssfCell.getNumericCellValue());
                    break;
                }
                if (DateUtil.isCellDateFormatted(xssfCell)) {// 日期
                    return cellValue = new SimpleDateFormat("yyyy-MM-dd")
                            .format(DateUtil.getJavaDate(xssfCell.getNumericCellValue()));
                }
                String formatVal = getFormatVal(10);// 数值
                DecimalFormat df = new DecimalFormat(formatVal);
                cellValue = df.format(xssfCell.getNumericCellValue());
                break;
            case FORMULA:
                try {
                    if (DateUtil.isCellDateFormatted(xssfCell)) {// 日期
                        return new SimpleDateFormat("yyyy-MM-dd")
                                .format(DateUtil.getJavaDate(xssfCell.getNumericCellValue()));
                    }
                } catch (IllegalStateException e) {
                    cellValue = xssfCell.getStringCellValue();
                    break;
                }
                try {
                    if (xssfCell.getCellStyle().getDataFormatString().indexOf("%") > -1) {// 百分比
                        cellValue = String.valueOf(xssfCell.getNumericCellValue());
                        break;
                    }
                    String formatVal1 = getFormatVal(10);// 数值
                    DecimalFormat df1 = new DecimalFormat(formatVal1);
                    cellValue = df1.format(xssfCell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    // cellValue = String.valueOf(xssfCell.getRichStringCellValue());
                    cellValue = "";
                }
                break;
            case BLANK:// 空
                cellValue = "";
                break;
            default:
                cellValue = "";
                break;
        }
        return cellValue;
    }

    public static String getFormatVal(int length) {
        String val = "###0";
        if (length > 0) {
            val = val + ".";
        }
        for (int i = 0; i < length; i++) {
            val = val + "0";
        }
        return val;
    }

    /**
     * 解析csv文件
     *
     * @param file csv文件
     * @return 数组
     * @throws UnsupportedEncodingException
     */
    public static List<List<String>> readCSV(InputStream in) throws UnsupportedEncodingException {
        List<List<String>> list = new ArrayList<List<String>>();
        CSVReader csvReader = new CSVReaderBuilder(
                new BufferedReader(new InputStreamReader(new BOMInputStream(in), "utf-8"))).build();// 带有BOM的csv会导致验证表头失败
        Iterator<String[]> iterator = csvReader.iterator();
        while (iterator.hasNext()) {
            String[] next = iterator.next();
            List<String> row = new ArrayList<String>();
            for (String item : next) {
                row.add(item);
            }
            list.add(row);
        }
        return list;
    }

    /**
     * 导出CSV 如果数据包含逗号或换行,需要用""包裹
     * 
     * @param <D>
     * @param rowConsumer
     * @param dataList
     * @param out
     * @param heads
     */
    public static <D> void exportCsv(BiConsumer<D, BiConsumer<Integer, Object>> rowConsumer, Collection<D> dataList,
            OutputStream out,
            String... heads) {
        OutputStreamWriter writer = new OutputStreamWriter(out);
        try {
            writer.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));// 带BOM头否则Excel打开乱码
            for (String cell : heads) {
                writer.append(cell).append(",");
            }
            writer.append("\n");
        } catch (IOException e) {
            logger.error("写入CSV文件BOM头失败:" + e.getMessage(), e);
        }
        try {
            for (D data : dataList) {
                Map<Integer, Object> cellValueMap = new HashMap<>();
                rowConsumer.accept(data, cellValueMap::put);
                if (cellValueMap.isEmpty()) {
                    continue;
                }
                Integer[] indexs = cellValueMap.keySet().toArray(new Integer[0]);
                Arrays.sort(indexs);
                for (int i = 0; i <= indexs[indexs.length - 1]; i++) {
                    Object item = cellValueMap.get(i);
                    if (item != null) {
                        writer.append(item.toString());
                    }
                    writer.append(",");
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            logger.error("导出CSV文件异常:" + e.getMessage(), e);
        }
        IOUtils.closeQuietly(writer);
    }

    /**
     * 导出Excel
     * 
     * @param workbook
     * @param out
     */
    public static void exportExcel(Workbook workbook, OutputStream out) {
        try {
            if (workbook instanceof SXSSFWorkbook) {// 解决大数量导出异常的问题
                workbook.write(out);
            } else if (workbook instanceof XSSFWorkbook) {
                SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook((XSSFWorkbook) workbook);
                sxssfWorkbook.write(out);
                IOUtils.closeQuietly(sxssfWorkbook);
            } else {
                workbook.write(out);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("导出Excel失败：" + e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static Workbook loadWorkbook(String fileName, InputStream in) throws IOException {
        if (fileName.endsWith(".xlsx")) {
            XSSFWorkbook xssfWorkbook = null;
            try {
                xssfWorkbook = new XSSFWorkbook(in);
            } catch (IOException e) {
                logger.info(fileName + "读取文件失败");
                throw e;
            }
            return xssfWorkbook;
        } else if (fileName.endsWith(".xls")) {
            HSSFWorkbook hssfWorkbook = null;
            try {
                hssfWorkbook = new HSSFWorkbook(in);
            } catch (IOException e) {
                logger.info(fileName + "读取文件失败");
                throw e;
            }
            return hssfWorkbook;
        } else {
            throw new IllegalArgumentException("不支持的文件类型！");
        }
    }

    /**
     * 导出Excel
     * 
     * @param model
     * @param templateName
     * @param out
     */
    public static <M> void exportExcel(M model, String templateName, OutputStream out) {
        Map<String, Object> modelMap = new HashMap<>();
        ValueUtils.fillMap(model, modelMap);
        exportExcel(modelMap, templateName, out);
    }

    /**
     * 导出Excel
     * 
     * @param model
     * @param templateName
     * @param out
     */
    public static void exportExcel(Map<String, Object> model, String templateName, OutputStream out) {
        try {
            transformExcel(templateName, out, model);
        } catch (Exception e) {
            logger.error("导出Excel失败：" + e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * 转换excel
     * 
     * @param templateName
     * @param out
     * @param model
     * @return
     */
    public static void transformExcel(String templateName, File out, Map<String, Object> model) {
        transformExcel(useTemplateInputStream(templateName), IOUtils.useOutputStream(out), model);
    }

    /**
     * 转换excel
     * 
     * @param templateName
     * @param out
     * @param model
     * @return
     */
    public static void transformExcel(String templateName, OutputStream out, Map<String, Object> model) {
        transformExcel(useTemplateInputStream(templateName), out, model);
    }

    /**
     * 转换excel
     * 
     * @param xls
     * @param out
     * @param model
     * @return
     */
    public static void transformExcel(File xls, File out, Map<String, Object> model) {
        transformExcel(IOUtils.useInputStream(xls), IOUtils.useOutputStream(out), model);
    }

    /**
     * 转换excel
     * 
     * @param templateName
     * @param os
     * @param model
     * @throws IOException
     */
    public static void transformExcel(InputStream is, OutputStream os, Map<String, Object> model) {
        JxlsPoiTemplateFillerBuilder.newInstance()
                .withTemplate(is)
                .build()
                .fill(model, () -> os);
    }

    public static InputStream useTemplateInputStream(String templateName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ExcelUtils.class.getClassLoader();
        }
        return classLoader.getResourceAsStream("templates/" + templateName);
    }

    public static boolean isExcelFile(String fileName) {
        if (fileName.matches("^.+\\.(?i)(xls|xlsx)$")) {
            return true;
        }
        return false;
    }

    public static <T> Result<List<Pair<Position, T>>> tableToModelList(List<Pair<Position, List<Object>>> list,
            Class<T> clazz, Map<String, String> headMap) {
        List<Pair<Position, T>> modelList = new ArrayList<Pair<Position, T>>();
        if (CollectionUtils.isEmpty(list) || list.size() < 2) {
            return Result.success(modelList);
        }
        Map<Integer, String> fieldMap = new HashMap<Integer, String>();
        List<Object> head = list.get(0).getV2();
        Position headPosition = list.get(0).getV1();
        int ln = list.size();
        int hi = -1;
        for (Object field : head) {
            hi++;
            String property = headMap.get(field.toString().trim());
            if (StringUtils.hasText(property)) {
                fieldMap.put(hi + headPosition.getCellIndex(), property);
            }
        }
        fieldMap = Collections.unmodifiableMap(fieldMap);
        Position nextRowPosition = headPosition.bottom();
        int fieldCount = head.size();
        for (int i = 1; i < ln; i++) {
            List<Object> row = list.get(i).getV2();
            Position rowPosition = list.get(i).getV1();
            if (!nextRowPosition.cellRegion(fieldCount).inRegion(rowPosition.cellRegion(row.size()))) {
                logger.info("nextRowPosition:" + nextRowPosition + " fieldCount:" + fieldCount + ",rowPosition:"
                        + rowPosition + " size:" + row.size());
                return Result.fail(nextRowPosition + "表格数据读取错误");
            } else {
                nextRowPosition = nextRowPosition.bottom();
            }
            int j = -1;
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(clazz);
            for (Object value : row) {
                int ci = ++j + rowPosition.getCellIndex();
                if (fieldMap.containsKey(ci)) {
                    beanWrapper.setPropertyValue(fieldMap.get(ci), value);
                }
            }
            Pair<Position, T> rowPair = new Pair<Position, T>();
            rowPair.setV1(rowPosition);
            rowPair.setV2((T) beanWrapper.getRootInstance());
            modelList.add(rowPair);
        }
        return Result.success(modelList);
    }

    public static List<Pair<Position, List<Object>>> excelToList(InputStream inputStream, int sheetIndex)
            throws EncryptedDocumentException, IOException {
        Workbook workbook = WorkbookFactory.create(inputStream);
        List<Pair<Position, List<Object>>> data = exportData(workbook.getSheetAt(sheetIndex));
        workbook.close();
        inputStream.close();
        return data;
    }

    public static List<Pair<Position, List<Object>>> excelToList(InputStream inputStream, String sheetName)
            throws EncryptedDocumentException, IOException {
        Workbook workbook = WorkbookFactory.create(inputStream);
        List<Pair<Position, List<Object>>> data = exportData(workbook.getSheet(sheetName));
        workbook.close();
        inputStream.close();
        return data;
    }

    public static List<Pair<Position, List<Object>>> exportData(Sheet sheet) {
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        return exportData(sheet, firstRowNum, lastRowNum);
    }

    public static List<Pair<Position, List<Object>>> exportData(Sheet sheet, int firstRowNum, int lastRowNum) {
        List<Pair<Position, List<Object>>> data = new ArrayList<Pair<Position, List<Object>>>();
        for (int i = firstRowNum; i <= lastRowNum; i++) {// 循环获取工作表的每一行
            Row sheetRow = sheet.getRow(i);
            if (sheetRow == null) {
                continue;
            }
            data.add(exportRowData(sheetRow, i));
        }
        return data;
    }

    public static <B> Result<List<Pair<Position, B>>> exportData(Sheet sheet, Class<B> clazz,
            Map<String, String> headMap, boolean stopIfFail) {
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        return exportData(sheet, firstRowNum, lastRowNum, clazz, headMap, stopIfFail);
    }

    public static <B extends Importable> Result<List<Pair<Position, B>>> exportData(Sheet sheet, int firstRowNum,
            int lastRowNum, Class<B> clazz, boolean stopIfFail) {
        List<Pair<Position, B>> data = new ArrayList<Pair<Position, B>>();
        List<String> emptyMessages = new ArrayList<String>();
        List<String> messages = new ArrayList<String>();
        for (int i = firstRowNum + 1; i <= lastRowNum; i++) {// 循环获取工作表的每一行
            Row sheetRow = sheet.getRow(i);
            Pair<Position, List<Object>> rowData = exportRowData(sheetRow, i);
            B bean = BeanUtils.instantiateClass(clazz);
            bean.applyRow(rowData.getV2().toArray());
            Pair<Position, B> rowBean = new Pair<Position, B>(rowData.getV1(), bean);
            Result<Void> beanResult = bean.validate(Importable.MODE_IMPORT);
            if (!beanResult.isSuccess()) {
                String message = "第" + (rowBean.getV1().getRowIndex() + 1) + "行" + beanResult.getMessage();
                if (stopIfFail) {
                    return Result.fail(message);
                } else if (isEmptyRow(rowData.getV2())) {
                    emptyMessages.add(message);
                } else {
                    if (!emptyMessages.isEmpty()) {
                        messages.addAll(emptyMessages);
                        emptyMessages.clear();
                    }
                    messages.add(message);
                }
            } else if (!emptyMessages.isEmpty()) {
                messages.addAll(emptyMessages);
                emptyMessages.clear();
            } else if (messages.isEmpty()) {
                data.add(rowBean);
            }
        }
        if (!messages.isEmpty()) {
            return Result.fail(messages.toString());
        }
        return Result.success(data);
    }

    public static <B> Result<List<Pair<Position, B>>> exportData(Sheet sheet, int firstRowNum, int lastRowNum,
            Class<B> clazz, Map<String, String> headMap, boolean stopIfFail) {
        List<Pair<Position, B>> data = new ArrayList<Pair<Position, B>>();
        Row headRow = sheet.getRow(firstRowNum);
        Pair<Position, List<Object>> head = exportRowData(headRow, firstRowNum);
        Result<Map<Integer, String>> hr = matchHead(head, headMap);
        if (hr.isSuccess()) {
            return hr.copy();
        }
        Map<Integer, String> propertyMap = hr.getData();
        Position nextRowPosition = head.getV1().bottom();
        int fieldCount = head.getV2().size();
        List<String> emptyMessages = new ArrayList<String>();
        List<String> messages = new ArrayList<String>();
        for (int i = firstRowNum + 1; i <= lastRowNum; i++) {// 循环获取工作表的每一行
            Row sheetRow = sheet.getRow(i);
            Pair<Position, List<Object>> rowData = exportRowData(sheetRow, i);
            if (!nextRowPosition.cellRegion(fieldCount).inRegion(rowData.getV1().cellRegion(rowData.getV2().size()))) {
                return Result.fail("读取数据错误:" + rowData.getV1().toString());
            }
            nextRowPosition = nextRowPosition.bottom();
            Pair<Position, B> rowBean = convertToBean(rowData, clazz, propertyMap);
            B bean = rowBean.getV2();
            if (!(bean instanceof Verifiable)) {
                data.add(rowBean);
                continue;
            }
            Verifiable verifiable = (Verifiable) bean;
            Result<Void> beanResult = verifiable.validate(Verifiable.MODE_IMPORT);
            if (!beanResult.isSuccess()) {
                String message = "第" + (rowBean.getV1().getRowIndex() + 1) + "行" + beanResult.getMessage();
                if (stopIfFail) {
                    return Result.fail(message);
                } else if (isEmptyRow(rowData.getV2())) {
                    emptyMessages.add(message);
                } else {
                    if (!emptyMessages.isEmpty()) {
                        messages.addAll(emptyMessages);
                        emptyMessages.clear();
                    }
                    messages.add(message);
                }
            } else if (!emptyMessages.isEmpty()) {
                messages.addAll(emptyMessages);
                emptyMessages.clear();
            } else if (messages.isEmpty()) {
                data.add(rowBean);
            }
        }
        if (!messages.isEmpty()) {
            return Result.fail(messages.toString());
        }
        return Result.success(data);
    }

    private static Result<Map<Integer, String>> matchHead(Pair<Position, List<Object>> head,
            Map<String, String> headMap) {
        Map<Integer, String> propertyMap = new HashMap<>();
        int cellIndex = head.getV1().getCellIndex();
        List<Object> values = head.getV2();
        for (Object value : values) {
            String columnName = (String) value;
            if (!StringUtils.hasText(columnName)) {
                return Result.fail("列名读取错误！");
            }
            if (headMap.containsKey(columnName)) {
                propertyMap.put(cellIndex, headMap.get(columnName));
            }
            cellIndex++;
        }
        return Result.success(propertyMap);
    }

    public static <B> Pair<Position, B> convertToBean(Pair<Position, List<Object>> rowData, Class<B> clazz,
            Map<Integer, String> propertyMap) {
        Pair<Position, B> rowBean = new Pair<>();
        rowBean.setV1(rowData.getV1());
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(clazz);
        int currentCellIndex = rowBean.getV1().getCellIndex();
        for (Object value : rowData.getV2()) {
            if (propertyMap.containsKey(currentCellIndex)) {
                beanWrapper.setPropertyValue(propertyMap.get(currentCellIndex), value);
            }
            currentCellIndex++;
        }
        rowBean.setV2((B) beanWrapper.getWrappedInstance());
        return rowBean;
    }

    public static boolean isEmptyRow(List<Object> row) {
        if (CollectionUtils.isEmpty(row)) {
            return true;
        }
        for (Object cell : row) {
            if (cell != null) {
                if (!(cell instanceof String)) {
                    return false;
                } else if (StringUtils.hasText((String) cell)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Pair<Position, List<Object>> exportRowData(Row sheetRow, int i) {
        if (sheetRow == null) {
            Pair<Position, List<Object>> row = new Pair<Position, List<Object>>();
            row.setV1(new Position(i, 0));
            row.setV2(new ArrayList<Object>());
            return row;
        }
        int firstCellNum = sheetRow.getFirstCellNum();
        int lastCellNum = sheetRow.getLastCellNum();
        Position position = new Position(i, firstCellNum);
        List<Object> rowData = new ArrayList<Object>();
        for (int j = firstCellNum; j < lastCellNum; j++) {// 循环获取每一列
            Cell cell = sheetRow.getCell(j);
            if (cell == null) {
                rowData.add(null);
                continue;
            }
            CellType cellType = cell.getCellType();
            if (CellType.STRING.equals(cellType)) {
                rowData.add(cell.getStringCellValue());
            } else if (CellType.NUMERIC.equals(cellType)) {
                rowData.add(cell.getNumericCellValue());
            } else if (CellType.BOOLEAN.equals(cellType)) {
                rowData.add(cell.getBooleanCellValue());
            } else {
                rowData.add(null);
            }
        }
        Pair<Position, List<Object>> row = new Pair<Position, List<Object>>();
        row.setV1(position);
        row.setV2(rowData);
        return row;
    }
}