package com.jsls.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.poi.ss.usermodel.Workbook;
import org.jack.common.config.WaterMarkConfig;
import org.jack.common.core.Timer;
import org.jack.common.core.Verifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.DocumentException;

public class WebUtils {

    public static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

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

    /**
     * 下载简单(纯单表)的Excel模板
     * 
     */
    public static void exportSimpleExcelTemplate(HttpServletResponse response, String fileName, String headLine) {
        Map<String, Object> model = new HashMap<>();
        List<String> headList = Arrays.asList(headLine.trim().split("\\s+"));
        model.put("headList", headList);
        exportExcel(model, "commonSimpleTemplate.xlsx", fileName, response);
    }

    /**
     * 导出excel
     * 
     * @param model
     * @param templateName
     * @param fileName
     * @param response
     */
    public static void exportExcel(Map<String, Object> model,
            String templateName, String fileName, HttpServletResponse response) {
        downloadHeader(response, fileName);
        try {
            OutputStream out = response.getOutputStream();
            ExcelUtils.transformExcel(templateName, out, model);
            IOUtils.closeQuietly(out);
        } catch (Exception e) {
            logger.error("导出" + fileName + "异常：" + e.getMessage(), e);
        }
    }

    public static void responseFile(HttpServletResponse response, InputStream in, String fileName, String waterMark)
            throws IOException, DocumentException {
        ContentType contentType = useContentType(fileName);
        if (contentType != null) {
            responseFile(response, in, contentType, waterMark);
        } else {
            download(in, fileName, response);
        }
    }

    public static void responseFile(HttpServletResponse response, InputStream in, ContentType contentType,
            String waterMark)
            throws IOException, DocumentException {
        response.setContentType(contentType.getMimeType());
        OutputStream out = response.getOutputStream();
        WaterMarkConfig waterMarkConfig = SpringContextHolder.getBean(WaterMarkConfig.class);
        waterMarkConfig.applyWaterMark(contentType, waterMark, in, out);
    }

    public static void responseFile(HttpServletResponse response, File file) {
        responseFile(response, file, null);
    }

    public static void responseFile(HttpServletResponse response, File file, String fileName) {
        if (!StringUtils.hasText(fileName)) {
            fileName = file.getName();
        }
        responseFile(response, IOUtils.useInputStream(file), fileName);
    }

    public static void responseFile(HttpServletResponse response, InputStream in, String fileName) {
        ContentType contentType = useContentType(fileName);
        if (contentType == null) {
            downloadHeader(response, fileName);
        } else {
            setHeader(response, contentType.getMimeType());
        }
        sendFile(in, response);
    }

    /**
     * 响应文件 默认水印 SHANGHAITRUST
     * 
     * @param response
     * @param templateName
     * @param model
     * @param fileName
     */
    public static void responsePdf(HttpServletResponse response, String templateName, Object model, String fileName,
            String password) {
        responsePdf(response, templateName, model, fileName, WaterMarkConfig.DEFAULT_WATER_MARK, password);
    }

    /**
     * 响应文件
     * 
     * @param response
     * @param templateName
     * @param model
     * @param fileName
     * @param waterMarkName
     */
    public static void responsePdf(HttpServletResponse response, String templateName, Object model, String fileName,
            String waterMarkName, String password) {
        String html = RenderUtils.freeMarkerRender(model, templateName);
        if (StringUtils.hasText(fileName)) {
            downloadHeader(response, fileName);
        } else {
            setHeader(response, CONTENT_TYPE_PDF.getMimeType());
        }
        try {
            PdfUtils.export(html, response.getOutputStream(), waterMarkName, password);
        } catch (IOException e) {
            throw new RuntimeException("导出PDF异常：" + e.getMessage(), e);
        }
    }

    public static void download(HttpServletResponse response, String fullUrl, String originalFilename) {
        try {
            HttpUtils.request(fullUrl, resp -> {
                try {
                    download(resp.getEntity().getContent(), originalFilename, response);
                } catch (UnsupportedOperationException e) {
                    throw new RuntimeException("下载文件异常：" + e.getMessage(), e);
                } catch (IOException e) {
                    throw new RuntimeException("下载文件异常：" + e.getMessage(), e);
                }
            });
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送文件
     * 
     * @param in
     * @param fileName
     * @param response
     */
    public static void download(InputStream in, String fileName, HttpServletResponse response) {
        downloadHeader(response, fileName);
        sendFile(in, response);
    }

    public static void download(HttpServletResponse response, File file) {
        download(response, file, null);
    }

    public static void download(HttpServletResponse response, File file, String fileName) {
        if (!StringUtils.hasText(fileName)) {
            fileName = file.getName();
        }
        downloadHeader(response, fileName);
        sendFile(IOUtils.useInputStream(file), response);
    }

    public static void download(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        setHeader(response, "application/vnd.ms-excel;charset=UTF-8", fileName);
        workbook.write(response.getOutputStream());
    }

    public static void download(HttpServletResponse response, List<File> fileList, String fileName) throws IOException {
        setHeader(response, "application/zip;charset=UTF-8", fileName);
        ZipUtils.toZip(fileList, response.getOutputStream());
    }

    public static void sendJsonp(HttpServletResponse response, String callback, Object data) {
        send("<script>" + callback + "(" + ValueUtils.toJSONString(data) + ")" + "</script>", response);
    }

    /**
     * 发送文件流
     * 
     * @param in
     * @param response
     */
    public static void sendFile(InputStream in, HttpServletResponse response) {
        try {
            IOUtils.copy(in, response.getOutputStream());
        } catch (IOException e) {
            logger.error("导出文件异常：" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送响应数据
     * 
     * @param obj
     * @param response
     */
    public static void send(Object obj, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        try {
            PrintWriter pw = response.getWriter();
            if (obj instanceof CharSequence) {
                String text = obj.toString();
                String contentType = "text/plain;charset=UTF-8";
                if (Verifiable.matchFind(text, "<\\w+[^>]*>")) {
                    contentType = "text/html;charset=UTF-8";
                }
                response.setContentType(contentType);
                pw.print(text);
                return;
            }
            response.setContentType("application/json;charset=UTF-8");
            pw.print(ValueUtils.toJSONString(obj));
            IOUtils.closeQuietly(pw);
        } catch (IOException e) {
            logger.error("发送数据异常：" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void downloadHeader(HttpServletResponse response, String fileName) {
        setHeader(response, "application/octet-stream", fileName);
    }

    public static void setHeader(HttpServletResponse response, String contentType) {
        setHeader(response, contentType, null);
    }

    public static void setHeader(HttpServletResponse response, String contentType, String fileName) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(contentType);
        if (StringUtils.hasText(fileName)) {
            response.setHeader("Content-Disposition", "attachment;filename=" + encodingFileName(response, fileName));
        }
    }

    public static String useFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        try {
            originalFilename = URLDecoder.decode(originalFilename, "UTF-8");
        } catch (Exception e) {
            // none
        }
        return originalFilename;
    }

    /**
     * 字符编码
     */
    public static String encodingFileName(HttpServletResponse response, String fileName) {
        String userAgent = response.getHeader("user-agent");
        try {
            if (org.apache.commons.lang3.StringUtils.contains(userAgent, "MSIE")
                    || org.apache.commons.lang3.StringUtils.contains(userAgent, "rv:11.0")) {
                fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            } else if (org.apache.commons.lang3.StringUtils.contains(userAgent, "Mozilla")) {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            } else {
                fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
        }
        return fileName;
    }

    public static String generateFileName(String originalFilename) {
        return generateFileName(originalFilename, null);
    }

    public static String generateFileName(String originalFilename, String prefix) {
        String timer = DateUtils.formatDate(new Date(Timer.uniqueTime()), "yyyyMMddHHmmssSSS");
        boolean filePrefix = false;
        if (!StringUtils.hasText(prefix)) {
            filePrefix = true;
            prefix = originalFilename;
        }
        String suffix = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            if (filePrefix) {
                prefix = originalFilename.substring(0, lastDotIndex);
            }
            suffix = originalFilename.substring(lastDotIndex).toLowerCase();
        }
        return prefix + "_" + timer + suffix;
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

    public static boolean isAjax(HttpServletRequest request) {
        String requestType = request.getHeader("X-Requested-With");
        return StringUtils.hasText(requestType);
    }

    public static String getHeader(String head) {
        return getHeader(head, getRequest());
    }

    public static String getHeader(String head, HttpServletRequest request) {
        return request.getHeader(head);
    }

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        return request;
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();
        return response;
    }

}