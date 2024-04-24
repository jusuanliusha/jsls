package com.jsls.core;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.ContentType;
import org.springframework.util.StringUtils;

import com.itextpdf.text.DocumentException;
import com.jsls.util.IOUtils;
import com.jsls.util.StyleUtils;
import com.jsls.util.WaterMarkUtils;

import lombok.Data;

@Data
public class WaterMark {
    public static final String DEFAULT_WATER_MARK = "JUSUANLIUSHA";
    private String text = DEFAULT_WATER_MARK;
    private String color;

    public void apply(ContentType contentType, InputStream in, OutputStream out) {
        String waterMark = this.text;
        try {
            if (!StringUtils.hasText(waterMark)) {
                IOUtils.copy(in, out);
                return;
            }
            Color color = StyleUtils.convertColor(this.color);
            if (ExportBiz.CONTENT_TYPE_PDF.equals(contentType)) {
                WaterMarkUtils.addTxtWaterMaker(in, out, waterMark, color);
            } else if (ExportBiz.CONTENT_TYPE_MSWORD.equals(contentType)) {
                WaterMarkUtils.wordWaterMark(in, out, color, waterMark);
            } else if (ContentType.IMAGE_JPEG.equals(contentType)
                    || ContentType.IMAGE_GIF.equals(contentType)
                    || ContentType.IMAGE_PNG.equals(contentType)) {
                String formatName = "JPG";
                if (ContentType.IMAGE_PNG.equals(contentType)) {
                    formatName = "PNG";
                } else if (ContentType.IMAGE_GIF.equals(contentType)) {
                    formatName = "GIF";
                }
                WaterMarkUtils.mark(in, out, formatName, color, waterMark);
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
}
