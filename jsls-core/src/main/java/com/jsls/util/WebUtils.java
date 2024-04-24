package com.jsls.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.jsls.core.Verifiable;

public class WebUtils {

    public static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    public static void download(HttpServletResponse response, String fullUrl, String originalFilename) {
        HttpUtils.request(fullUrl, resp -> {
            try {
                download(resp.getEntity().getContent(), originalFilename, response);
            } catch (UnsupportedOperationException e) {
                throw new RuntimeException("下载文件异常：" + e.getMessage(), e);
            } catch (IOException e) {
                throw new RuntimeException("下载文件异常：" + e.getMessage(), e);
            }
        });
    }

    /**
     * 发送文件
     * 
     * @param in
     * @param fileName
     * @param response
     */
    public static void download(InputStream in, String fileName, HttpServletResponse response) {
        downloadHeader(fileName, response);
        sendFile(in, response);
    }

    public static void download(File file, HttpServletResponse response) {
        download(file, null, response);
    }

    public static void download(File file, String fileName, HttpServletResponse response) {
        if (!StringUtils.hasText(fileName)) {
            fileName = file.getName();
        }
        downloadHeader(fileName, response);
        sendFile(IOUtils.useInputStream(file), response);
    }

    public static void download(List<File> fileList, String fileName, HttpServletResponse response) throws IOException {
        setHeader("application/zip;charset=UTF-8", fileName, response);
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

    public static void downloadHeader(String fileName, HttpServletResponse response) {
        setHeader("application/octet-stream", fileName, response);
    }

    public static void setHeader(String contentType, HttpServletResponse response) {
        setHeader(contentType, null, response);
    }

    public static void setHeader(String contentType, String fileName, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(contentType);
        if (StringUtils.hasText(fileName)) {
            response.setHeader("Content-Disposition", "attachment;filename=" + encodingFileName(fileName, response));
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
    public static String encodingFileName(String fileName, HttpServletResponse response) {
        String userAgent = response.getHeader("user-agent");
        try {
            if (StringUtils.hasText(userAgent) && userAgent.contains("Mozilla")) {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            } else {
                fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return fileName;
    }

    public static String getDomain() {
        HttpServletRequest request = getRequest();
        StringBuffer sb = request.getRequestURL();
        return sb.delete(sb.length() - request.getRequestURI().length(), sb.length()).toString();
    }

    public static String getOrigin() {
        HttpServletRequest request = getRequest();
        return request.getHeader("Origin");
    }

    /**
     * 获取IP地址
     * <p>
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr() {
        HttpServletRequest request = getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        try {
            String[] localhost = new String[] { "127.0.0.1", "0:0:0:0:0:0:0:1" };
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if (Arrays.asList(localhost).contains(ip)) {
                    // 根据网卡取本机配置的IP
                    ip = InetAddress.getLocalHost().getHostAddress();
                }
            }
            String comma = ",";
            if (ip.contains(comma)) {
                ip = ip.split(",")[0];
            }
            if (localhost[0].equals(ip)) {
                // 获取本机真正的ip地址
                ip = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
            logger.error("获取IP异常:" + e.getMessage(), e);
            // ignore
        }
        return ip;
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
        if (servletRequestAttributes != null) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            return request;
        }
        return null;
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (servletRequestAttributes != null) {
            HttpServletResponse response = servletRequestAttributes.getResponse();
            return response;
        }
        return null;

    }

}