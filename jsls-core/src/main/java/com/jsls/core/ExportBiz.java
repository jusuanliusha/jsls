package com.jsls.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import com.itextpdf.text.DocumentException;

import com.jsls.util.StyleUtils;
import com.jsls.util.WaterMarkUtils;
import com.jsls.config.WaterMarkConfig;
import com.jsls.util.ExcelUtils;
import com.jsls.util.IOUtils;
import com.jsls.util.PdfUtils;
import com.jsls.util.RenderUtils;
import com.jsls.util.SpringContextHolder;
import com.jsls.util.WebUtils;
import com.jsls.util.WordUtils;
import com.jsls.util.ZipUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.Data;

@Data
public class ExportBiz {
    public static final Logger logger = LoggerFactory.getLogger(ExportBiz.class);
    public static final String EXPORT_DEFAULT = "DEFAULT";
    public static final ContentType CONTENT_TYPE_PDF = ContentType.create("application/pdf");
    /**
     * doc application/msword 用于Word 97至Word 2003的文档格式。
     */
    public static final ContentType CONTENT_TYPE_MSWORD = ContentType.create("application/msword");
    /**
     * docx application/vnd.openxmlformats-officedocument.wordprocessingml.document
     * 用于Word 2007及以后版本的OpenXML文档格式。
     */
    public static final ContentType CONTENT_TYPE_DOCX = ContentType
            .create("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    /**
     * xls application/vnd.ms-excel 用于Excel 2003及更早版本的二进制工作簿格式。
     */
    public static final ContentType CONTENT_TYPE_MSEXCEL = ContentType.create("application/vnd.ms-excel");
    /**
     * xlsx application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * 用于Excel 2007及以后版本的OpenXML工作簿格式，这是.xlsx文件的标准MIME类型。
     */
    public static final ContentType CONTENT_TYPE_XLSX = ContentType
            .create("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    public static final ContentType CONTENT_TYPE_IMAGE_JPEG = ContentType.IMAGE_JPEG;
    public static final ContentType CONTENT_TYPE_IMAGE_GIF = ContentType.IMAGE_GIF;
    public static final ContentType CONTENT_TYPE_IMAGE_PNG = ContentType.IMAGE_PNG;

    private HttpServletResponse response;
    private String rootPath;
    private String subPath;
    private String waterMark = WaterMarkConfig.DEFAULT_WATER_MARK;
    private String password;
    private Map<String, String> templateMap = new HashMap<>();
    private Set<String> fileList = new HashSet<>();

 
    public void responseFile(File file) {
        responseFile(file, null);
    }

    public void responseFile(File file, String fileName) {
        if (!StringUtils.hasText(fileName)) {
            fileName = file.getName();
        }
        responseFile(IOUtils.useInputStream(file), fileName);
    }
    public void responseFile(InputStream in, String fileName){
        ContentType contentType = useContentType(fileName);
        if (contentType != null) {
            responseFile(in, contentType);
        } else {
            WebUtils.download(in, fileName, response);
        }
    }

    public void responseFile(InputStream in, ContentType contentType)
            throws IOException, DocumentException {
        response.setContentType(contentType.getMimeType());
        OutputStream out = response.getOutputStream();
        WaterMarkConfig waterMarkConfig = SpringContextHolder.getBean(WaterMarkConfig.class);
        applyWaterMark(contentType, useWaterMark(contentType, waterMark), in, out);
    }

   

    /**
     * 响应文件
     * 
     * @param templateName
     * @param model
     * @param fileName
     */
    public void responsePdf(String templateName, Object model, String fileName) {
        String html = RenderUtils.freeMarkerRender(model, templateName);
        if (StringUtils.hasText(fileName)) {
            WebUtils.downloadHeader(fileName, response);
        } else {
            WebUtils.setHeader(CONTENT_TYPE_PDF.getMimeType(), response);
        }
        try {
            PdfUtils.export(html, response.getOutputStream(), waterMark, password);
        } catch (IOException e) {
            throw new RuntimeException("导出PDF异常：" + e.getMessage(), e);
        }
    }

    public void applyWaterMark(ContentType contentType, String waterMark, InputStream in,
            OutputStream out) {
        try {
            if (!StringUtils.hasText(waterMark)) {
                IOUtils.copy(in, out);
                return;
            }
            if (ExportBiz.CONTENT_TYPE_PDF.equals(contentType)) {
                WaterMarkUtils.addTxtWaterMaker(in, out, waterMark, StyleUtils.convertColor(this.getPdfColor()));
            } else if (WebUtils.CONTENT_TYPE_MSWORD.equals(contentType)) {
                WaterMarkUtils.wordWaterMark(in, out, StyleUtils.convertColor(this.getWordColor()), waterMark);
            } else if (ContentType.IMAGE_JPEG.equals(contentType)
                    || ContentType.IMAGE_GIF.equals(contentType)
                    || ContentType.IMAGE_PNG.equals(contentType)) {
                String formatName = "JPG";
                if (ContentType.IMAGE_PNG.equals(contentType)) {
                    formatName = "PNG";
                } else if (ContentType.IMAGE_GIF.equals(contentType)) {
                    formatName = "GIF";
                }
                WaterMarkUtils.mark(in, out, formatName, StyleUtils.convertColor(this.getImageColor()),
                        waterMark);
            } else {
                IOUtils.copy(in, out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    private String useWaterMark(ContentType contentType, String waterMark) {
        if (StringUtils.hasText(waterMark)) {
            return waterMark;
        }
        String username = ActionInfo.currUsername();
        if (!StringUtils.hasText(username)) {
            return waterMark;
        }
        if (ExportBiz.CONTENT_TYPE_PDF.equals(contentType)) {
            return this.getPdfText() + "-" + username;
        } else if (ExportBiz.CONTENT_TYPE_MSWORD.equals(contentType)) {
            return this.getWordText() + "-" + username;
        } else if (ContentType.IMAGE_JPEG.equals(contentType) || ContentType.IMAGE_GIF.equals(contentType)
                || ContentType.IMAGE_PNG.equals(contentType)) {
            return this.getImageText() + "-" + username;
        }
        return waterMark;
    }

    public static ContentType useContentType(String type) {
        ContentType contentType = null;
        if (!StringUtils.hasText(type) || "file".equals(type)) {
            return contentType;
        }
        int ldi = type.lastIndexOf(".");
        if (ldi >= 0) {
            type = type.substring(ldi + 1);
        }
        type = type.toLowerCase();
        if ("pdf".equals(type)) {
            contentType = WebUtils.CONTENT_TYPE_PDF;
        } else if (type.equals("doc")) {
            contentType = WebUtils.CONTENT_TYPE_MSWORD;
        } else if (type.equals("docx") || type.equals("word")) {
            contentType = WebUtils.CONTENT_TYPE_DOCX;
        } else if (type.equals("xls")) {
            contentType = WebUtils.CONTENT_TYPE_MSEXCEL;
        } else if (type.equals("xlsx") || type.equals("excel")) {
            contentType = WebUtils.CONTENT_TYPE_XLSX;
        } else if (type.equals("jpg") || type.equals("jpeg")) {
            contentType = WebUtils.CONTENT_TYPE_IMAGE_JPEG;
        } else if (type.equals("gif")) {
            contentType = WebUtils.CONTENT_TYPE_IMAGE_GIF;
        } else if (type.equals("png")) {
            contentType = WebUtils.CONTENT_TYPE_IMAGE_PNG;
        }
        return contentType;
    }

    public File useExportFile(String fileName) {
        return new File(useExportPath(), fileName);
    }

    public String useSubPath(String fileName) {
        return subPath + fileName;
    }

    public String useExportPath() {
        return rootPath + subPath;
    }

    /**
     * 导出Excel
     * 
     * @param <M>
     * @param model
     * @param fileName
     */
    public <M> void exportExcel(M model, String fileName) {
        exportExcel(model, fileName, useTemplate(EXPORT_DEFAULT));
    }

    /**
     * 导出Excel
     * 
     * @param <M>
     * @param model
     * @param fileName
     * @param template
     */
    public <M> void exportExcel(M model, String fileName, String template) {
        ExcelUtils.exportExcel(model, template, useOutputStream(fileName, "excel"));
    }

     /**
     * 下载简单(纯单表)的Excel模板
     * 
     */
    public void exportSimpleExcelTemplate(HttpServletResponse response, String fileName, String headLine) {
        Map<String, Object> model = new HashMap<>();
        List<String> headList = Arrays.asList(headLine.trim().split("\\s+"));
        model.put("headList", headList);
        exportExcel(model, fileName, "commonSimpleTemplate.xlsx");
    }
    /**
     * 导出Excel
     * 
     * @param model
     * @param fileName
     */
    public void exportExcel(Map<String, Object> model, String fileName) {
        exportExcel(model, fileName, useTemplate(EXPORT_DEFAULT));
    }

    /**
     * 导出Excel
     * 
     * @param model
     * @param fileName
     * @param template
     */
    public void exportExcel(Map<String, Object> model, String fileName, String template) {
        ExcelUtils.exportExcel(model, template, useOutputStream(fileName, "excel"));
    }

    /**
     * 导出PDF
     * 
     * @param <M>
     * @param model
     * @param fileName
     */
    public <M> void exportPdf(M model, String fileName) {
        exportPdf(model, fileName, useTemplate(EXPORT_DEFAULT));
    }

    /**
     * 导出PDF
     * 
     * @param <M>
     * @param model
     * @param fileName
     * @param template
     */
    public <M> void exportPdf(M model, String fileName, String template) {
        String html = RenderUtils.freeMarkerRender(model, template);
        PdfUtils.export(html, useOutputStream(fileName, "pdf"), waterMark, password);
    }

    /**
     * 导出word
     * 
     * @param <M>
     * @param model
     * @param fileName
     */
    public <M> void exportWord(M model, String fileName) {
        exportWord(model, fileName, useTemplate(EXPORT_DEFAULT));
    }

    /**
     * 导出word
     * 
     * @param model
     * @param fileName
     */
    public void exportWord(Map<String, Object> model, String fileName) {
        exportWord(model, fileName, useTemplate(EXPORT_DEFAULT));
    }

    /**
     * 导出word
     * 
     * @param <M>
     * @param model
     * @param fileName
     * @param template
     */
    public <M> void exportWord(M model, String fileName, String template) {
        WordUtils.exportWord(model, template, useOutputStream(fileName, "word"));
    }

    /**
     * 导出word
     * 
     * @param model
     * @param fileName
     * @param template
     */
    public void exportWord(Map<String, Object> model, String fileName, String template) {
        WordUtils.exportWord(model, template, useOutputStream(fileName, "word"));
    }

    /**
     * 批量导出后打包zip下载
     * 
     * @param exportBiz
     * @param fileName
     */
    public static void downloadZip(HttpServletResponse response, String fileName, Collection<ExportBiz> exportBizList) {
        ZipOutputStream zos = null;
        try {
            zos = ZipUtils.useZipOutputStream(useOutputStream(fileName, "zip", response));
            for (ExportBiz exportBiz : exportBizList) {
                exportBiz.toZip(zos, "");
            }
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    /**
     * 将导出的文件打包zip下载
     * 
     * @param exportBiz
     * @param fileName
     */
    public void downloadZip(HttpServletResponse response, String fileName) {
        ZipOutputStream zos = null;
        try {
            zos = ZipUtils.useZipOutputStream(useOutputStream(fileName, "zip", response));
            toZip(zos, "");
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    public void downloadSingleFile(HttpServletResponse response) {
        if (!CollectionUtils.isEmpty(fileList) && fileList.size() == 1) {
            downloadFile(response, fileList.iterator().next());
        }
    }

    public void downloadFile(HttpServletResponse response, String fileName) {
        downloadFile(response, fileName, fileName);
    }

    public void downloadFile(HttpServletResponse response, String fileName, String downloadName) {
        WebUtils.download(useExportFile(fileName), downloadName, response);
    }

    public void responseSingleFile(HttpServletResponse response) {
        if (!CollectionUtils.isEmpty(fileList) && fileList.size() == 1) {
            responseFile(response, fileList.iterator().next());
        }
    }

    public void responseFile(HttpServletResponse response, String fileName) {
        InputStream in = IOUtils.useInputStream(useExportFile(fileName));
        responseFile(response, in, fileName);
    }

    public void responseFile(HttpServletResponse response, InputStream in, String fileName) {
        IOUtils.copy(in, useOutputStream(fileName, useContentType(fileName), response));
    }

    public OutputStream useOutputStream(String fileName, String type) {
        if (response != null) {
            return useOutputStream(fileName, type, response);
        }
        OutputStream out = IOUtils.useOutputStream(useExportFile(fileName));
        fileList.add(fileName);
        return out;
    }

    public static void exportExcel(Workbook workbook, String fileName, HttpServletResponse response)
            throws IOException {
        WebUtils.setHeader("application/vnd.ms-excel;charset=UTF-8", fileName, response);
        workbook.write(response.getOutputStream());
    }

    public static OutputStream useOutputStream(String fileName, ContentType contentType, HttpServletResponse response) {
        if (contentType == null) {
            WebUtils.downloadHeader(fileName, response);
        } else {
            response.setContentType(contentType.getMimeType());
        }
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            WebUtils.logger.error("导出文件失败：" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static OutputStream useOutputStream(String fileName, String type, HttpServletResponse response) {
        if (StringUtils.hasText(fileName)) {
            WebUtils.downloadHeader(fileName, response);
        } else if ("pdf".equals(type)) {
            response.setContentType(CONTENT_TYPE_PDF.getMimeType());
        }
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            WebUtils.logger.error("导出" + type + "失败：" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 是否有已经导出的文件fileName
     * 
     * @param fileName
     * @return
     */
    public boolean hasExported(String fileName) {
        return fileList.contains(fileName);
    }

    /**
     * 是否存储在服务器
     * 
     * @return
     */
    public boolean isStoreOnServer() {
        return response == null;
    }

    /**
     * 使用导出的文件，只有一个文件时返回该文件，否则返回null
     * 
     * @return
     */
    public File useExportFile() {
        if (fileList != null && fileList.size() == 1) {
            return useExportFile(fileList.iterator().next());
        }
        return null;
    }

    public String useTemplate(String exportType) {
        return templateMap.get(exportType);
    }

    public void setTemplate(String exportType, String template) {
        templateMap.put(exportType, template);
    }

    /**
     * 压缩导出的文件
     * 
     * @param zos
     * @param entryName
     * @param fileNames
     */
    public void toZip(ZipOutputStream zos, String entryName, String... fileNames) {
        if (fileNames != null && fileNames.length > 0) {
            for (String fileName : fileNames) {
                File file = useExportFile(fileName);
                if (file.exists()) {
                    try {
                        ZipUtils.compress(file, zos, entryName + fileName, true);
                    } catch (IOException e) {
                        throw new RuntimeException("压缩文件失败:" + e.getMessage(), e);
                    }
                }
            }
        } else if (!CollectionUtils.isEmpty(fileList)) {
            for (String fileName : fileList) {
                File file = useExportFile(fileName);
                if (file.exists()) {
                    try {
                        ZipUtils.compress(file, zos, entryName + fileName, true);
                    } catch (IOException e) {
                        throw new RuntimeException("压缩文件失败:" + e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * 清空导出的文件
     */
    public void clearFiles(String... fileNames) {
        if (fileNames != null && fileNames.length > 0) {
            for (String fileName : fileNames) {
                File file = useExportFile(fileName);
                if (file.exists()) {
                    // 文件删除
                    IOUtils.logger.info("删除文件名：" + file.getName());
                    file.delete();
                }
                fileList.remove(fileName);
            }
        } else if (!CollectionUtils.isEmpty(fileList)) {
            for (String fileName : fileList) {
                File file = useExportFile(fileName);
                if (file.exists()) {
                    // 文件删除
                    IOUtils.logger.info("删除文件名：" + file.getName());
                    file.delete();
                }
            }
            fileList = new HashSet<>();
        }
    }

}
