package org.jack.common.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.jack.common.config.WaterMarkConfig;
import org.jack.common.util.ExcelUtils;
import org.jack.common.util.IOUtils;
import org.jack.common.util.PdfUtils;
import org.jack.common.util.RenderUtils;
import org.jack.common.util.WebUtils;
import org.jack.common.util.WordUtils;
import org.jack.common.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.Data;

@Data
public class ExportBiz {
    public static final Logger logger = LoggerFactory.getLogger(ExportBiz.class);
    public static final String EXPORT_DEFAULT = "DEFAULT";
    private HttpServletResponse response;
    private String rootPath;
    private String subPath;
    private String waterMark = WaterMarkConfig.DEFAULT_WATER_MARK;
    private String password;
    private Map<String, String> templateMap = new HashMap<>();
    private Set<String> fileList = new HashSet<>();

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
     * 打包zip下载
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
        WebUtils.download(response, useExportFile(fileName), downloadName);
    }

    public void responseSingleFile(HttpServletResponse response) {
        if (!CollectionUtils.isEmpty(fileList) && fileList.size() == 1) {
            responseFile(response, fileList.iterator().next());
        }
    }

    public void responseFile(HttpServletResponse response, String fileName) {
        WebUtils.responseFile(response, useExportFile(fileName));
    }

    public OutputStream useOutputStream(String fileName, String type) {
        if (response != null) {
            return useOutputStream(fileName, type, response);
        }
        OutputStream out = IOUtils.useOutputStream(useExportFile(fileName));
        fileList.add(fileName);
        return out;
    }

    public OutputStream useOutputStream(String fileName, String type, HttpServletResponse response) {
        if (StringUtils.hasText(fileName)) {
            WebUtils.downloadHeader(response, fileName);
        } else if ("pdf".equals(type)) {
            response.setContentType(WebUtils.CONTENT_TYPE_PDF.getMimeType());
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
