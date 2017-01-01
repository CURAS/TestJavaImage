package testjavaimage;

import java.awt.image.BufferedImage;

public class Entrance {

    final static private String inputPath = "D:\\colors.jpg";
    final static private String outputPath = "D:\\colors2.bmp";

    public static void main(String[] args) throws UnsupportedColorTypeException {
        BufferedImage image, markedImage;
        int[] bits, bits2;

        image = ImageTool.readImg(inputPath);
        bits = Watermark.infoToArray("2014220201029");
        markedImage = Watermark.makeMarkedImage(image, bits, 2);
        ImageTool.writeImg(markedImage, outputPath, "bmp");

        markedImage = ImageTool.readImg(outputPath);
        bits2 = Watermark.extractMarkFromImage(markedImage);

        System.out.println(Watermark.arrayToInfo(bits2));
    }
}
