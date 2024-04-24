package com.jsls.util;

import java.awt.FontMetrics;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFEncryption;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfPageEvent;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jsls.core.WaterMark;

public class PdfUtils {
	private static final Logger logger = LoggerFactory.getLogger(RenderUtils.class);

	public static void transformPdf(String html, OutputStream os, WaterMark waterMark, String password) {
		transformPdf(html, os, waterMark.getText(), password);
	}

	public static void transformPdf(String html, OutputStream os, String waterMarkName, String password) {
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
		ITextRenderer renderer = new ITextRenderer();
		ITextFontResolver fontResolver = renderer.getFontResolver();
		try {
			fontResolver.addFont("font/msyh.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
			fontResolver.addFont("font/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
		} catch (DocumentException e) {
			logger.error("应用自定义字体异常：" + e.getMessage(), e);
			logger.error("导出pdf失败：" + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("应用自定义字体异常：" + e.getMessage(), e);
			logger.error("导出pdf失败：" + e.getMessage(), e);
		}
		renderer.setDocumentFromString(html);
		if (StringUtils.hasText(password)) {
			setPDFEncryption(password, password, renderer);
		}
		renderer.layout();
		try {
			renderer.createPDF(os, false);
		} catch (DocumentException e) {
			logger.error("导出pdf失败：" + e.getMessage(), e);
			throw new RuntimeException("导出pdf失败：" + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("导出pdf失败：" + e.getMessage(), e);
			throw new RuntimeException("导出pdf失败：" + e.getMessage(), e);
		}
		if (StringUtils.hasText(waterMarkName)) {
			try {
				waterMark(renderer.getWriter(), waterMarkName);
			} catch (IOException e) {
				logger.error("添加水印异常：" + e.getMessage(), e);
				throw new RuntimeException("导出pdf失败：" + e.getMessage(), e);
			} catch (DocumentException e) {
				logger.error("添加水印异常：" + e.getMessage(), e);
				throw new RuntimeException("导出pdf失败：" + e.getMessage(), e);
			}
		}
		renderer.finishPDF();
	}

	private static void setPDFEncryption(String password, String adminPassword, ITextRenderer render) {
		PDFEncryption pdfEncryption = new PDFEncryption();
		// 用户密码
		pdfEncryption.setUserPassword(password.getBytes());
		// 管理员密码
		pdfEncryption.setOwnerPassword(adminPassword.getBytes());
		// 用户权限
		pdfEncryption.setAllowedPrivileges(PdfWriter.ALLOW_PRINTING);
		// 加密类型
		pdfEncryption.setEncryptionType(PdfWriter.STANDARD_ENCRYPTION_128);
		render.setPDFEncryption(pdfEncryption);
	}

	public static void convert(String html, OutputStream os, String waterMarkName)
			throws IOException, DocumentException {
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		if (StringUtils.hasText(waterMarkName)) {
			waterMark(writer, waterMarkName);
		}
		document.open();
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, new StringReader(html));
		document.close();
	}

	public static void waterMark(PdfWriter pdfWriter, final String waterMarkName)
			throws IOException, DocumentException {
		pdfWriter.setPageEvent(new PdfPageEvent() {
			@Override
			public void onOpenDocument(PdfWriter pdfWriter, Document document) {
				logger.info("onOpenDocument");
			}

			@Override
			public void onStartPage(PdfWriter pdfWriter, Document document) {
				logger.info("onStartPage");
			}

			@Override
			public void onEndPage(PdfWriter pdfWriter, Document document) {
				int interval = -20;
				// 检查字体文件存不存在
				// 创建字体
				BaseFont font = null;
				try {
					font = BaseFont.createFont("font/msyh.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
				} catch (DocumentException | IOException e) {

				}
				// 设置水印透明度
				PdfGState gs = new PdfGState();
				// 设置填充字体不透明度为0.04f
				gs.setFillOpacity(0.02f);
				gs.setStrokeOpacity(0.02f);
				PdfContentByte content = pdfWriter.getDirectContentUnder();
				JLabel label = new JLabel();
				FontMetrics metrics;
				label.setText(waterMarkName);
				metrics = label.getFontMetrics(label.getFont());
				int textH = metrics.getHeight();
				int textW = metrics.stringWidth(label.getText());
				Rectangle pageRect = pdfWriter.getPageSize();
				content.beginText();
				content.setFontAndSize(font, 48);
				content.setGState(gs);
				// 设置水印颜色
				content.setColorFill(BaseColor.BLACK);
				// 水印文字成45度角倾斜
				for (int height = interval + textH; height < pageRect.getHeight(); height = height + textH * 8) {
					for (int width = interval + textW; width < pageRect.getWidth() + textW; width = width + textW * 3) {
						content.showTextAligned(Element.ALIGN_LEFT, waterMarkName, width,
								height - textH, 45);
					}
				}
				content.endText();
			}

			@Override
			public void onCloseDocument(PdfWriter pdfWriter, Document document) {
				logger.info("onCloseDocument");
			}

			@Override
			public void onParagraph(PdfWriter pdfWriter, Document document, float v) {
				logger.info("onParagraph");

			}

			@Override
			public void onParagraphEnd(PdfWriter pdfWriter, Document document, float v) {
				logger.info("onParagraphEnd");
			}

			@Override
			public void onChapter(PdfWriter pdfWriter, Document document, float v, Paragraph paragraph) {
				logger.info("onChapter");
			}

			@Override
			public void onChapterEnd(PdfWriter pdfWriter, Document document, float v) {
				logger.info("onChapterEnd");
			}

			@Override
			public void onSection(PdfWriter pdfWriter, Document document, float v, int i, Paragraph paragraph) {
				logger.info("onSection");
			}

			@Override
			public void onSectionEnd(PdfWriter pdfWriter, Document document, float v) {
				logger.info("onSectionEnd");
			}

			@Override
			public void onGenericTag(PdfWriter pdfWriter, Document document, Rectangle rectangle, String s) {
				logger.info("onGenericTag");
			}
		});
	}
}
