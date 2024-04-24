package com.jsls.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.documents.TextSelection;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.fields.TextRange;

public class WordUtils {
    private static Logger logger = LoggerFactory.getLogger(WordUtils.class);

    /**
     * 导出Word
     * 
     * @param model
     * @param template
     * @param out
     */
    public static void exportWord(Map<String, Object> model, String template, OutputStream out) {
        transformWord(template, model::get, out);
    }

    /**
     * 导出Word
     * 
     * @param <M>
     * @param model
     * @param template
     * @param out
     */
    public static <M> void exportWord(M model, String template, OutputStream out) {
        BeanWrapper bw = new BeanWrapperImpl(model);
        transformWord(template, bw::getPropertyValue, out);
    }

    /**
     * @desc word生成 并保存到本地服务器
     * @date 2023/4/3 9:35
     */
    public static void transformWord(String template, Function<String, ?> valFn, OutputStream out) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("template/" + template);
        Document doc = new Document();
        doc.loadFromStream(is, FileFormat.Docx);
        replaceSpecialWord(doc, valFn);
        try {
            PipedInputStream pipin = new PipedInputStream();
            PipedOutputStream pipout = new PipedOutputStream(pipin);
            Thread thread = new Thread(() -> {
                try {
                    removeEvaluatonWarningWord(pipin, out);
                } catch (Exception e) {
                    logger.error("生成word异常：" + e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            doc.saveToFile(pipout, FileFormat.Docx);
            thread.join();
        } catch (Exception e) {
            logger.error("生成word异常：" + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static DocPicture useDocPicture(Document doc, File image, float width, float height) throws IOException {
        return useDocPicture(doc, IOUtils.useInputStream(image), width, height);
    }

    public static DocPicture useDocPicture(Document doc, InputStream image, float width, float height) {
        DocPicture pic = new DocPicture(doc);
        pic.loadImage(image);
        pic.setWidth(width);
        pic.setHeight(height);
        return pic;
    }

    /**
     * @return void
     * @throws InvalidFormatException
     * @throws FileNotFoundException
     * @desc 去除注册码提示:Evaluation Warning: The document was created with Spire.Doc for
     *       JAVA.
     * @date 2023/4/4 9:06
     */
    private static void removeEvaluatonWarningWord(InputStream in, OutputStream out)
            throws IOException, InvalidFormatException {
        XWPFDocument doc = new XWPFDocument(OPCPackage.open(in));
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        if (paragraphs.size() < 1){
            doc.close();
            return;
        }
        XWPFParagraph firstParagraph = paragraphs.get(0);
        if (firstParagraph.getText().contains("Spire.Doc")) {
            doc.removeBodyElement(doc.getPosOfParagraph(firstParagraph));
        }
        doc.write(out);
        out.close();
        doc.close();
    }

    /**
     * 替换Word文件中 ${} 标识的特殊字符 <br>
     * <strong>注意：如果存在部分特殊表示无法替换，请尝试将 ${}
     * 的整个字符串复制到word中，有可能word没有将${}识别为一个整体</strong>
     * 
     * @param doc: Sprire Document
     * @param map: 占位符${} 与 需要替换的为字符串的对应关系
     */
    private static void replaceSpecialWord(Document doc, Function<String, ?> valFn) {
        // 正则表达式，匹配所有的占位符 ${}
        Pattern pattern = Pattern.compile("\\$\\{(.*)?}");
        // 根据正则表达式获取所有文本
        TextSelection[] allPattern = doc.findAllPattern(pattern);
        // 逐个替换占位符
        for (TextSelection textSelection : allPattern) {
            String stext = textSelection.getSelectedText();
            if (!StringUtils.hasText(stext)) {
                continue;
            }
            String paramName = stext.substring(2, stext.length() - 1).trim();
            Object utext = valFn.apply(paramName);
            if (Objects.isNull(utext)) {
                continue;
            }
            if (utext instanceof DocPicture) {// 插入图片
                DocPicture pic = (DocPicture) utext;
                TextRange range = textSelection.getAsOneRange();
                int index = range.getOwnerParagraph().getChildObjects().indexOf(range);
                range.getOwnerParagraph().getChildObjects().insert(index, pic);
                range.getOwnerParagraph().getChildObjects().remove(range);
            } else {
                String tmp = utext.toString();
                doc.replace(stext, tmp, true, true);
            }
        }
    }

    /**
     * 替换Word文档中的同一占位符，为多张不同的图片，每一个占位符对应一张图片， 如果占位符比图片数量多则该占位符会被写为空字符串
     * 
     * @param doc:         Spire Document
     * @param matchString: 占位符
     * @param imgList:     图片输入流集合
     * @date 2021/10/22 20:09
     */
    public static void replaceTextToImage(Document doc, String matchString, List<InputStream> imgList) {
        TextSelection[] selections = doc.findAllString(matchString, true, true);
        int total = imgList.size();
        int count = 0;
        for (TextSelection selection : selections) {
            if (count < total && imgList.get(count) != null) {
                DocPicture pic = new DocPicture(doc);
                pic.loadImage(imgList.get(count));
                pic.setWidth(500f); // 设置图片宽高
                pic.setHeight(400f);
                TextRange range = selection.getAsOneRange();
                int index = range.getOwnerParagraph().getChildObjects().indexOf(range);
                range.getOwnerParagraph().getChildObjects().insert(index, pic);
                range.getOwnerParagraph().getChildObjects().remove(range);
                count++;
            } else {
                // 如果已近没有了图片则将所有占位符替换为空
                doc.replace(selection.getSelectedText(), "", true, true);
                break;
            }
        }
    }
}
