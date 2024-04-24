package com.jsls.util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.HeaderFooter;
import com.spire.doc.Section;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ShapeLineStyle;
import com.spire.doc.documents.ShapeType;
import com.spire.doc.fields.ShapeObject;

/**
 * 給图片/PDF添加水印
 * 
 * @author 张伟
 *
 */
public class WaterMarkUtils {
    public static void addImgWaterMaker(String inputFile, String outputFile, String imgFilePath)
            throws FileNotFoundException, BadElementException, MalformedURLException, DocumentException, IOException {
        addImgWaterMaker(new PdfReader(inputFile), new FileOutputStream(outputFile),
                com.itextpdf.text.Image.getInstance(imgFilePath));
    }

    public static void addImgWaterMaker(InputStream in, OutputStream out, com.itextpdf.text.Image image)
            throws IOException, DocumentException {
        addImgWaterMaker(new PdfReader(in), out, image);
    }

    public static void addImgWaterMaker(PdfReader reader, OutputStream out, com.itextpdf.text.Image image)
            throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, out);
        PdfGState gs1 = new PdfGState();
        // 设置透明度
        gs1.setFillOpacity(0.7f);
        // 获取PDF页数
        int num = reader.getNumberOfPages();
        PdfContentByte under;
        for (int i = 1; i <= num; i++) {
            PdfContentByte pdfContentByte = stamper.getOverContent(i);
            // 获得PDF最顶层
            under = stamper.getOverContent(i);
            pdfContentByte.setGState(gs1);
            // 行
            for (int y = 0; y < 10; y++) {
                // 列
                for (int x = 0; x < 8; x++) {
                    // 设置旋转角度
                    image.setRotationDegrees(30);// 旋转 角度
                    // 设置等比缩放
                    under.setColorFill(BaseColor.GRAY);
                    image.scaleToFit(80, 120);
                    image.setRotation(30);
                    image.setAbsolutePosition(60 + 140 * x, 110 * y);
                    pdfContentByte.addImage(image);
                }
            }
        }
        stamper.close();
        reader.close();
    }

    /**
     * PDF 添加水印
     * 
     * @param inputFile
     * @param outputFile
     * @param waterMarkName
     * @throws FileNotFoundException
     * @throws DocumentException
     * @throws IOException
     */
    public static void addTxtWaterMaker(String inputFile, String outputFile, String waterMarkName)
            throws FileNotFoundException, DocumentException, IOException {
        // 使用系统字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        addTxtWaterMaker(new PdfReader(inputFile), new FileOutputStream(outputFile), waterMarkName, baseFont,
                Color.LIGHT_GRAY);
    }

    public static void addTxtWaterMaker(String inputFile, String outputFile, String waterMarkName, Color color)
            throws FileNotFoundException, DocumentException, IOException {
        // 使用系统字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        addTxtWaterMaker(new PdfReader(inputFile), new FileOutputStream(outputFile), waterMarkName, baseFont, color);
    }

    public static void addTxtWaterMaker(InputStream in, OutputStream out, String waterMarkName, Color color)
            throws DocumentException, IOException {
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        addTxtWaterMaker(new PdfReader(in), out, waterMarkName, baseFont, color);
    }

    /**
     * PDF 添加水印
     * 
     * @param reader
     * @param out
     * @param waterMarkName
     * @param baseFont
     * @param color
     * @throws DocumentException
     * @throws IOException
     */
    public static void addTxtWaterMaker(PdfReader reader, OutputStream out, String waterMarkName, BaseFont baseFont,
            Color color) throws DocumentException, IOException {
        PdfStamper stamper = new PdfStamper(reader, out);
        Rectangle pageRect;
        PdfGState gs = new PdfGState();
        // 设置文字透明度
        gs.setFillOpacity(0.2f);
        gs.setStrokeOpacity(0.2f);
        // 获取pdf总页数
        int total = reader.getNumberOfPages() + 1;
        JLabel label = new JLabel();
        FontMetrics metrics;
        int textH;
        int textW;
        label.setText(waterMarkName);
        metrics = label.getFontMetrics(label.getFont());
        // 得到文字的宽高
        textH = metrics.getHeight();
        textW = metrics.stringWidth(label.getText());
        PdfContentByte under;
        for (int i = 1; i < total; i++) {
            pageRect = reader.getPageSizeWithRotation(i);
            // 得到一个覆盖在上层的水印文字
            under = stamper.getOverContent(i);
            under.saveState();
            under.setGState(gs);
            under.beginText();
            // 设置水印文字颜色
            under.setColorFill(new BaseColor(color.getRGB()));
            // 设置水印文字和大小
            under.setFontAndSize(baseFont, 30);
            // 这个position主要是为了在换行加水印时能往右偏移起始坐标
            int position = 0;
            int interval = -3;
            for (int height = interval + textH; height < pageRect.getHeight(); height = height + textH * 9) {
                for (int width = interval + textW - position * 300; width < pageRect.getWidth() + textW; width = width
                        + textW) {
                    // 添加水印文字，水印文字成25度角倾斜
                    under.showTextAligned(Element.ALIGN_LEFT, waterMarkName, width - textW, height - textH, 30);
                }
                position++;
            }
            // 添加水印文字
            under.endText();
        }
        // 关闭流
        stamper.close();
        reader.close();
    }

    /**
     * 图片添加水印
     * 
     * @param srcImgPath       需要添加水印的图片的路径
     * @param outImgPath       添加水印后图片输出路径
     * @param markContentColor 水印文字的颜色
     * @param waterMarkContent 水印的文字
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void mark(String srcImgPath, String outImgPath, Color markContentColor, String waterMarkContent)
            throws FileNotFoundException, IOException {
        int fldi=srcImgPath.indexOf(".");
        String formatName="JPG";
        if(fldi>=0){
            formatName=srcImgPath.substring(fldi+1).toUpperCase();
        }
        mark(new FileInputStream(srcImgPath), new FileOutputStream(outImgPath),formatName, markContentColor, waterMarkContent);
    }

    public static void mark(InputStream in, OutputStream out,String formatName, Color markContentColor, String waterMarkContent)
            throws IOException {
        // 多个文字水印
        ImageWaterTool.Builder builder=new ImageWaterTool.Builder();
        builder.setFontColor(markContentColor)
                // 设置水印文字
                .setFontText(waterMarkContent)
                // 设置多个文字之间的距离
                .setSpacing(100).setAngdeg(-30)
                .setFontSize(30).setFontName("微软雅黑")
                // 设置水印的透明度
                .setAlpha(0.2f).build().addManyFontWater(ImageIO.read(in), out,formatName);
    }
    public static void wordWaterMark(String srcFileName, String targetFileName,Color markContentColor, String waterMarkContent)
            throws FileNotFoundException {
        wordWaterMark(new FileInputStream(srcFileName), new FileOutputStream(targetFileName), markContentColor, waterMarkContent);
    }
    public static void wordWaterMark(InputStream in, OutputStream out,Color markContentColor, String waterMarkContent){
        //加载示例文档
        Document doc = new Document();
        doc.loadFromStream(in, FileFormat.Auto);
        //添加艺术字并设置大小
        ShapeObject shape = new ShapeObject(doc, ShapeType.Text_Plain_Text);
        shape.setWidth(150);
        shape.setHeight(30);
        //设置艺术字文本内容、位置及样式
        shape.setVerticalPosition(30);
        shape.setHorizontalPosition(20);
        shape.setRotation(315);
        shape.getWordArt().setFontFamily("Microsoft YaHei");
        shape.getWordArt().setText(waterMarkContent);
        shape.setFillColor(markContentColor);
        shape.setLineStyle(ShapeLineStyle.Single);
        shape.setStrokeColor(new Color(192, 192, 192, 255));
        shape.setStrokeWeight(2);
        Section section;
        HeaderFooter header;
        for (int n = 0; n < doc.getSections().getCount(); n++) {
            section = doc.getSections().get(n);
            //获取section的页眉
            header = section.getHeadersFooters().getHeader();
            Paragraph paragraph1;
            for (int i = 0; i < 4; i++) {
                //添加段落到页眉
                paragraph1 = header.addParagraph();
                for (int j = 0; j < 3; j++) {
                    //复制艺术字并设置多行多列位置
                    shape = (ShapeObject) shape.deepClone();
                    shape.setVerticalPosition(50 + 150 * i);
                    shape.setHorizontalPosition(20 + 160 * j);
                    paragraph1.getChildObjects().add(shape);
                }
            }
        }
        //保存文档
        doc.saveToFile(out, FileFormat.Docx_2013);
    }
}
