package com.jsls.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtils {
    private static Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    private static final int BUFFER_SIZE = 1024 * 8;

    /**
     * 压缩成ZIP 方法1
     * 
     * @param source           压缩文件夹路径
     * @param out              压缩文件输出流
     * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;
     *                         false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     */
    public static void toZip(File source, OutputStream out, boolean KeepDirStructure) {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = useZipOutputStream(out);
        try {
            compress(source, zos, source.getName(), KeepDirStructure);
            logger.info("压缩完成 耗时:{}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("压缩zip异常:" + e.getMessage());
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    /**
     * 压缩成ZIP 方法2
     * 
     * @param srcFiles 需要压缩的文件列表
     * @param out      压缩文件输出流
     */
    public static void toZip(List<File> srcFiles, OutputStream out) {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = useZipOutputStream(out);
        try {
            for (File srcFile : srcFiles) {
                compress(srcFile, zos, srcFile.getName(), true);
            }
            logger.info("压缩完成 耗时:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new RuntimeException("压缩zip异常:" + e.getMessage());
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    /**
     * 递归压缩方法
     * 
     * @param sourceFile       源文件
     * @param zos              zip输出流
     * @param name             压缩后的名称
     * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;
     *                         false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     */
    public static void compress(File sourceFile, ZipOutputStream zos, String name,
            boolean KeepDirStructure) throws IOException {
        if (sourceFile.isFile()) {
            zos.putNextEntry(new ZipEntry(name));
            FileInputStream in = IOUtils.useInputStream(sourceFile);
            IOUtils.write(in, zos, BUFFER_SIZE);
            IOUtils.closeQuietly(in);
            zos.closeEntry();
            return;
        }
        File[] listFiles = sourceFile.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (File file : listFiles) {
                if (KeepDirStructure) {
                    // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                    // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                    compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                } else {
                    compress(file, zos, file.getName(), KeepDirStructure);
                }
            }
            return;
        }
        if (KeepDirStructure) {
            zos.putNextEntry(new ZipEntry(name + "/"));
            zos.closeEntry();
        }
    }

    public static ZipOutputStream useZipOutputStream(OutputStream out) {
        return new ZipOutputStream(out);
    }
}