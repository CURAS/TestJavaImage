package testjavaimage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public class ImageTool {

    public static BufferedImage readImg(String imgFilePath) {
        BufferedImage img = null;
        try {
            FileInputStream in = new FileInputStream(imgFilePath);
            img = ImageIO.read(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }

    public static void writeImg(BufferedImage img, String path, String formatName) {
        try {
            FileOutputStream out = new FileOutputStream(path);
            ImageIO.write(img, formatName, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int[][] getPixels(BufferedImage img) {
        int[][] pixels = new int[img.getHeight()][img.getWidth()];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                pixels[i][j] = img.getRGB(j, i);
            }
        }

        return pixels;
    }

    public static void setPixels(BufferedImage img, int[][] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                img.setRGB(j, i, pixels[i][j]);
            }
        }
    }

    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 2;
    public static final int ALPHA = 3;

    public static int[][] getChannel(int[][] pixels, int channel) {
        int[][] data = new int[pixels.length][pixels[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                data[i][j] = (pixels[i][j] >> (8 * channel)) & 0xff;
            }
        }

        return data;
    }

    public static void setChannel(int[][] pixels, int[][] data, int channel) {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                pixels[i][j] &= ~(0xff << (8 * channel));
                pixels[i][j] |= ((data[i][j] & 0xff) << (8 * channel));
            }
        }
    }

    public static int[][] toIntArray(double[][] array) {
        int[][] intArray = new int[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                intArray[i][j] = (int) array[i][j];
            }
        }
        return intArray;
    }

    public static double[][] toDoubleArray(int[][] array) {
        double[][] doubleArray = new double[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                doubleArray[i][j] = array[i][j];
            }
        }
        return doubleArray;
    }

}

class UnsupportedColorTypeException extends Exception {

    UnsupportedColorTypeException(String message) {
        super(message);
    }
}
