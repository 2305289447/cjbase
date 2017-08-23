package com.xcz.afcs.util;

import com.xcz.afcs.util.enums.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;

public class ImageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ImageUtil.class);

    private static final byte[] HEAD_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    private static final byte[] HEAD_JPEG1 = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1};

    private static final byte[] HEAD_PNG = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};

    private static final byte[] HEAD_GIF = {(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38};

    private static final byte[] HEAD_BMP = {(byte) 0x42, (byte) 0x4D};

    private static String[] supportedImageTypeNames;

    static {
        init();
    }

    private static void init() {
        ImageType[] imageTypes = ImageType.values();
        supportedImageTypeNames = new String[imageTypes.length];
        for (int i = 0; i < imageTypes.length; i++) {
            supportedImageTypeNames[i] = imageTypes[i].getName();
        }
    }

    public static String[] getSupportedImageTypeNames() {
        return supportedImageTypeNames;
    }

    public static ImageType getImageType(byte[] imageBytes) {
        if (null != imageBytes) {
            if (startsWith(imageBytes, HEAD_JPEG) || startsWith(imageBytes, HEAD_JPEG1)) {
                return ImageType.JPG;
            } else if (startsWith(imageBytes, HEAD_PNG)) {
                return ImageType.PNG;
            } else if (startsWith(imageBytes, HEAD_GIF)) {
                return ImageType.GIF;
            } else if (startsWith(imageBytes, HEAD_BMP)) {
                return ImageType.BMP;
            }
        }
        return null;
    }

    private static boolean startsWith(byte[] testBytes, byte[] head) {
        if (testBytes.length >= head.length) {
            for (int i = 0; i < head.length; i++) {
                if (testBytes[i] != head[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     * 缩放图像（按比例缩放）
     *
     * @param srcImageFile  源图像文件
     * @param destImageFile 缩放后的图像文件
     * @param scale         缩放比例
     * @param flag          缩放选择:true 放大; false 缩小;
     */
    public final static void scale(File srcImageFile, File destImageFile, int scale, boolean flag) {
        try {
            BufferedImage src = ImageIO.read(srcImageFile); // 读入文件
            int width = src.getWidth(); // 得到源图宽
            int height = src.getHeight(); // 得到源图长
            if (flag) {// 放大
                width = width * scale;
                height = height * scale;
            } else {// 缩小
                width = width / scale;
                height = height / scale;
            }
            Image image = src.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图
            g.dispose();
            ImageIO.write(tag, ImageType.JPG.getSuffix(), destImageFile);// 输出到文件流

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 缩放图像（按高度和宽度缩放）
     *
     * @param srcImageFile  源图像文件地址
     * @param destImageFile 缩放后的图像地址
     * @param height        缩放后的高度
     * @param width         缩放后的宽度
     * @param fixedColor    比例不对时补充的颜色，不补充为<code>null</code>
     */
    public final static void scale(File srcImageFile, File destImageFile, int height, int width, Color fixedColor) {
        try {
            double ratio = 0.0; // 缩放比例
            BufferedImage bi = ImageIO.read(srcImageFile);
            Image itemp = bi.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
            // 计算比例
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue() / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (null != fixedColor) {// 补白
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(fixedColor);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null))
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2, itemp.getWidth(null), itemp.getHeight(null), fixedColor, null);
                else
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0, itemp.getWidth(null), itemp.getHeight(null), fixedColor, null);
                g.dispose();
                itemp = image;
            }
            ImageIO.write((BufferedImage) itemp, ImageType.JPG.getSuffix(), destImageFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 图像切割(按指定起点坐标和宽高切割)
     *
     * @param srcImageFile  源图像地址
     * @param destImageFile 切片后的图像地址
     * @param x             目标切片起点坐标X
     * @param y             目标切片起点坐标Y
     * @param width         目标切片宽度
     * @param height        目标切片高度
     */
    public final static void cut(File srcImageFile, File destImageFile, int x, int y, int width, int height) {
        try {
            // 读取源图像
            BufferedImage bi = ImageIO.read(srcImageFile);
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > 0 && srcHeight > 0) {
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                // 四个参数分别为图像起点坐标和宽高
                // 即: CropImageFilter(int x,int y,int width,int height)
                ImageFilter cropFilter = new CropImageFilter(x, y, width, height);
                Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), cropFilter));
                BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics g = tag.getGraphics();
                g.drawImage(img, 0, 0, width, height, null); // 绘制切割后的图
                g.dispose();
                // 输出为文件
                ImageIO.write(tag, ImageType.JPG.getSuffix(), destImageFile);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 图像切割（指定切片的行数和列数）
     *
     * @param srcImageFile 源图像地址
     * @param descDir      切片目标文件夹
     * @param rows         目标切片行数。默认2，必须是范围 [1, 20] 之内
     * @param cols         目标切片列数。默认2，必须是范围 [1, 20] 之内
     */
    public final static void cutByRowsAndCols(File srcImageFile, File descDir, int rows, int cols) {
        try {
            if (rows <= 0 || rows > 20) rows = 2; // 切片行数
            if (cols <= 0 || cols > 20) cols = 2; // 切片列数
            // 读取源图像
            BufferedImage bi = ImageIO.read(srcImageFile);
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > 0 && srcHeight > 0) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int destWidth = srcWidth; // 每张切片的宽度
                int destHeight = srcHeight; // 每张切片的高度
                // 计算切片的宽度和高度
                if (srcWidth % cols == 0) {
                    destWidth = srcWidth / cols;
                } else {
                    destWidth = (int) Math.floor(srcWidth / cols) + 1;
                }
                if (srcHeight % rows == 0) {
                    destHeight = srcHeight / rows;
                } else {
                    destHeight = (int) Math.floor(srcWidth / rows) + 1;
                }
                // 循环建立切片
                // 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高
                        // 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight, destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 绘制缩小后的图
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, ImageType.JPG.getSuffix(), new File(descDir, "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 图像切割（指定切片的宽度和高度）
     *
     * @param srcImageFile 源图像地址
     * @param descDir      切片目标文件夹
     * @param destWidth    目标切片宽度。默认200
     * @param destHeight   目标切片高度。默认150
     */
    public final static void cut(File srcImageFile, File descDir, int destWidth, int destHeight) {
        try {
            if (destWidth <= 0) destWidth = 200; // 切片宽度
            if (destHeight <= 0) destHeight = 150; // 切片高度
            // 读取源图像
            BufferedImage bi = ImageIO.read(srcImageFile);
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > destWidth && srcHeight > destHeight) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int cols = 0; // 切片横向数量
                int rows = 0; // 切片纵向数量
                // 计算切片的横向和纵向数量
                if (srcWidth % destWidth == 0) {
                    cols = srcWidth / destWidth;
                } else {
                    cols = (int) Math.floor(srcWidth / destWidth) + 1;
                }
                if (srcHeight % destHeight == 0) {
                    rows = srcHeight / destHeight;
                } else {
                    rows = (int) Math.floor(srcHeight / destHeight) + 1;
                }
                // 循环建立切片
                // 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高
                        // 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight, destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 绘制缩小后的图
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, "JPEG", new File(descDir, "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 图像类型转换：GIF=》JPG、GIF=》PNG、PNG=》JPG、PNG=》GIF(X)、BMP=》PNG
     *
     * @param srcImageFile  源图像文件
     * @param formatName    包含格式非正式名称的 String：如JPG、JPEG、GIF等
     * @param destImageFile 目标图像文件
     */
    public final static void convert(File srcImageFile, String formatName, File destImageFile) {
        try {
            BufferedImage src = ImageIO.read(srcImageFile);
            ImageIO.write(src, formatName, destImageFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 彩色转为黑白
     *
     * @param srcImageFile  源图像地址
     * @param destImageFile 目标图像地址
     */
    public final static void gray(File srcImageFile, File destImageFile) {
        try {
            BufferedImage src = ImageIO.read(srcImageFile);
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            src = op.filter(src, null);
            ImageIO.write(src, ImageType.JPG.getSuffix(), destImageFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 给图片添加文字水印
     *
     * @param pressText     水印文字
     * @param srcImageFile  源图像地址
     * @param destImageFile 目标图像地址
     * @param fontName      水印的字体名称
     * @param fontStyle     水印的字体样式，例如Font.BOLD
     * @param color         水印的字体颜色
     * @param fontSize      水印的字体大小
     * @param x             修正值
     * @param y             修正值
     * @param alpha         透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     */
    public final static void pressText(String pressText, File srcImageFile, File destImageFile, String fontName, int fontStyle, Color color, int fontSize, int x, int y, float alpha) {
        try {
            Image src = ImageIO.read(srcImageFile);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            ImageIO.write(image, ImageType.JPG.getSuffix(), destImageFile);// 输出到文件流
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 给图片添加图片水印
     *
     * @param pressImgFile  水印图片
     * @param srcImageFile  源图像文件
     * @param destImageFile 目标图像文件
     * @param x             修正值。 默认在中间，偏移量相对于中间偏移
     * @param y             修正值。 默认在中间，偏移量相对于中间偏移
     * @param alpha         透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     */
    public final static void pressImage(File pressImgFile, File srcImageFile, File destImageFile, int x, int y, float alpha) {
        try {
            Image src = ImageIO.read(srcImageFile);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            // 水印文件
            Image pressImg = ImageIO.read(pressImgFile);
            int pressImgWidth = pressImg.getWidth(null);
            int pressImgHeight = pressImg.getHeight(null);
            x += (width - pressImgWidth) / 2;
            y += (height - pressImgHeight) / 2;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            g.drawImage(pressImg, x, y, pressImgWidth, pressImgHeight, null);
            // 水印文件结束
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", destImageFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //---------------------------------------------------------------------------------------------------------------- Private method start

    /**
     * 计算text的长度（一个中文算两个字符）
     *
     * @param text 文本
     * @return 字符长度，如：text="中国",返回 2；text="test",返回 2；text="中国ABC",返回 4.
     */
    private final static int getLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            if (new String(text.charAt(i) + "").getBytes().length > 1) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length / 2;
    }


}
