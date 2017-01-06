package testjavaimage;

import java.awt.image.BufferedImage;

public class Entrance {

    final static private String inputPath = "D:\\lena.jpg";
    final static private String outputPath = "D:\\lena2.jpg";

    public static void main(String[] args) throws UnsupportedColorTypeException {
        BufferedImage image, markedImage;
        int[] bits, bits2;

        image = ImageTool.readImg(inputPath);
        bits = Watermark.infoToArray("2014220201029");
        markedImage = Watermark.makeMarkedImage(image, bits, 100);
        ImageTool.writeImg(markedImage, outputPath, "jpg");

        markedImage = ImageTool.readImg(outputPath);
        bits2 = Watermark.extractMarkFromImage(markedImage);

        for (String s : Watermark.arrayToInfo(bits2)) {
            System.out.println(s);
        }
    }

}
