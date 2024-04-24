package com.jsls.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.springframework.util.StringUtils;

public class ImageWaterTool {
    // ++++++++++++++++++++++++++ 配置参数
    // +++++++++++++++++++++++++++++++++++++++++++++++++++
    /** 文字水印内容 */
    private String fontText = null;
    /** 图片水印内容 */
    private String imgText = null;
    // ++++++++++++++++++++++++++ 初始化一些参数
    // ++++++++++++++++++++++++++++++++++++++++++++++
    /** 图片宽度 */
    private int width;
    /** 图片高度 */
    private int height;
    /** 画笔操作 */
    private Graphics2D graphics2D = null;
    /** 图片缓冲区 */
    private BufferedImage bufferedImage = null;

    // +++++++++++++++++++++++++++++ 自定义的水印参数
    // ++++++++++++++++++++++++++++++++++++++++
    /** 文字字体 */
    private String fontName = null;
    /** 文字样式 */
    private int fontStyle;
    /** 文字大小 */
    private int fontSize;
    /** 文字颜色 */
    private Color fontColor;
    /** 透明度 */
    private float alpha;
    /** 外框宽度 */
    private int borderWidth;
    /** 旋转高度 */
    private double angdeg;
    /** 间距 */
    private int spacing;

    public ImageWaterTool(Builder builder) {
        this.fontText = builder.fontText;
        this.imgText = builder.imgText;
        this.fontName = builder.fontName;
        this.fontStyle = builder.fontStyle;
        this.fontSize = builder.fontSize;
        this.fontColor = builder.fontColor;
        this.alpha = builder.alpha;
        this.borderWidth = builder.borderWidth;
        this.angdeg = builder.angdeg;
        this.spacing = builder.spacing;
    }

    public static class Builder {

        // ++++++++++++++++++++++++++ 配置参数
        // +++++++++++++++++++++++++++++++++++++++++++++++++++
        /** 文字水印内容 */
        private String fontText = null;
        /** 图片水印内容 */
        private String imgText = null;

        // +++++++++++++++++++++++++++++ 自定义的水印参数
        // ++++++++++++++++++++++++++++++++++++++++
        /** 文字字体 */
        private String fontName = "微软雅黑";
        /** 文字样式 */
        private int fontStyle = Font.BOLD | Font.ITALIC;
        /** 文字大小 */
        private int fontSize = 30;
        /** 文字颜色 */
        private Color fontColor = Color.black;
        /** 透明度 */
        private float alpha = 0.3f;
        /** 外框宽度 */
        private int borderWidth = 20;
        /** 旋转高度 */
        private double angdeg = 30;
        /** 间距 */
        private int spacing = 200;

        public Builder setFontText(String fontText) {
            this.fontText = fontText;
            return this;
        }

        public Builder setImgText(String imgText) {
            this.imgText = imgText;
            return this;
        }

        public Builder setFontName(String fontName) {
            this.fontName = fontName;
            return this;
        }

        public Builder setFontStyle(int fontStyle) {
            this.fontStyle = fontStyle;
            return this;
        }

        public Builder setFontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder setFontColor(Color fontColor) {
            this.fontColor = fontColor;
            return this;
        }

        public Builder setAlpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        public Builder setBorderWidth(int borderWidth) {
            this.borderWidth = borderWidth;
            return this;
        }

        public Builder setAngdeg(double angdeg) {
            this.angdeg = angdeg;
            return this;
        }

        public Builder setSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public ImageWaterTool build() {
            return new ImageWaterTool(this);
        }
    }

    public static enum Position {
        /** 上左 */
        TOP_LEFT(1),
        /** 上右 */
        TOP_RIGHT(2),
        /** 下左 */
        BOTTOM_LEFT(3),
        /** 下右 */
        BOTTOM_RIGHT(4);

        public int type;

        Position(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * @function 设置文字或者图片水印位置
     * @param position    添加水印的位置
     * @param waterWidth  添加水印的宽度
     * @param waterHeight 添加水印的高度
     * @param type        true:文字 —— false:图片
     * @return int[] x, y
     */
    private int[] setPosition(int position, int waterWidth, int waterHeight, boolean type) {
        int x = width - waterWidth;
        int y = height - waterHeight;
        switch (position) {
            case 1:
                x = type ? borderWidth : 0;
                y = type ? borderWidth : 0;
                break;
            case 2:
                y = type ? borderWidth : 0;
                break;
            case 3:
                x = type ? borderWidth : 0;
                y = type ? y - borderWidth : y;
                break;
            case 4:
            default:
                x = type ? x - borderWidth : x;
                y = type ? y - borderWidth : y;
                break;
        }
        return new int[] { x, y };
    }

    /**
     * 初始化参数
     */
    private void init(BufferedImage img) {
        width = img.getWidth(null);
        height = img.getHeight(null);
        // 创建图片缓冲区
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 创建绘图工具
        graphics2D = bufferedImage.createGraphics();
        // 将原图放到缓冲区
        graphics2D.drawImage(img, 0, 0, width, height, null);
        img.getGraphics().dispose();
    }

    /**
     * 获取文字位置
     * 
     * @param text
     * @return
     */
    private int getTextLength(String text) {
        int len = text.length();
        for (int i = 0; i < text.length(); i++) {
            String s = String.valueOf(text.charAt(i));
            if (s.getBytes().length > 1) {
                len++;
            }
        }
        return len % 2 == 0 ? len / 2 : len / 2 + 1;
    }

    /**
     * 添加单个文字水印
     * 
     * @throws IOException
     * @param place
     */
    public void addFontWater(BufferedImage img, OutputStream out, Position place,String formatName) throws IOException {
        init(img);
        // 创建文字水印的样式
        graphics2D.setFont(new Font(fontName, fontStyle, fontSize));
        // 文字颜色
        graphics2D.setColor(fontColor);
        // 获取文字位置x，y坐标
        int type = place.getType();
        int[] position = setPosition(type, fontSize * getTextLength(fontText), fontSize, true);
        int x = position[0];
        int y = position[1] + fontSize;
        // 将文字写入指定位置，并关闭画笔资源
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        graphics2D.drawString(fontText, x, y);
        graphics2D.dispose();
        ImageIO.write(bufferedImage, StringUtils.hasText(formatName)?formatName:"JPG", out);
    }

    /**
     * 添加单个图片水印
     * 
     * @throws IOException
     */
    public void addPicWater(BufferedImage img, OutputStream out, Position place,String formatName) throws IOException {
        init(img);
        // 创建图片水印
        BufferedImage imageWater = ImageIO.read(new File(imgText));
        // 获取文字位置x，y坐标
        int type = place.getType();
        int[] position = setPosition(type, imageWater.getWidth(null), imageWater.getHeight(null), false);
        int x = position[0];
        int y = position[1];
        // 将文字写入指定位置，并关闭画笔资源
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        graphics2D.drawImage(imageWater, x, y, null);
        imageWater.getGraphics().dispose();
        graphics2D.dispose();
        ImageIO.write(bufferedImage, StringUtils.hasText(formatName)?formatName:"JPG", out);
    }

    /**
     * 添加多个文字水印
     * 
     * @throws IOException
     */
    public void addManyFontWater(BufferedImage img, OutputStream out,String formatName) throws IOException {
        init(img);
        // 创建文字水印的样式
        graphics2D.setFont(new Font(fontName, fontStyle, fontSize));
        graphics2D.setColor(fontColor);
        // 获取文字宽高
        int waterWidth = fontSize * getTextLength(fontText);
        int waterHeight = fontSize;
        // 将文字写入指定位置，并关闭画笔资源
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        // 将文字旋转30度
        graphics2D.rotate(Math.toRadians(angdeg), width / 2, height / 2);
        // 添加多个
        int x = -width / 2;
        while (x < width * 1.5) {
            int y = -height / 2;
            while (y < height * 1.5) {
                graphics2D.drawString(fontText, x, y);
                y += waterHeight + spacing;
            }
            x += waterWidth + spacing;
        }
        graphics2D.dispose();
        ImageIO.write(bufferedImage, StringUtils.hasText(formatName)?formatName:"JPG", out);
    }

    /**
     * 添加多个图片水印
     * 
     * @throws IOException
     */
    public void addManyPicWater(BufferedImage img, OutputStream out,String formatName) throws IOException {
        // 初始化
        init(img);
        // 创建图片水印
        Image imageWater = ImageIO.read(new File(imgText));
        // 获取水印图片的宽高
        int waterWidth = imageWater.getWidth(null);
        int waterHeight = imageWater.getHeight(null);
        // 将文字写入指定位置，并关闭画笔资源
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
        // 将文字旋转30度
        graphics2D.rotate(Math.toRadians(angdeg), width / 2, height / 2);
        // 添加多个
        int x = -width / 2;
        while (x < width * 1.5) {
            int y = -height / 2;
            while (y < height * 1.5) {
                graphics2D.drawImage(imageWater, x, y, null);
                y += waterHeight + spacing;
            }
            x += waterWidth + spacing;
        }
        imageWater.getGraphics().dispose();
        graphics2D.dispose();
        ImageIO.write(bufferedImage, StringUtils.hasText(formatName)?formatName:"JPG", out);
    }
}
