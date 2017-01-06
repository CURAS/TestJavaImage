package testjavaimage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Watermark {

    public static BufferedImage makeMarkedImage(BufferedImage image, int[] bits, double strength) {
        DCTMachine machine = new DCTMachine();
        BufferedImage markedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        int[][] pixels = ImageTool.getPixels(image);
        double[][] greens = ImageTool.toDoubleArray(ImageTool.getChannel(pixels, ImageTool.GREEN));

        double[][][][] greenBlocks = DCTMachine.blockPartition(greens, 8);
//        greenBlocks = machine.DCT_p(greenBlocks);
        for (int i = 0; i < greenBlocks.length; i++) {
            for (int j = 0; j < greenBlocks[0].length; j++) {
                greenBlocks[i][j] = machine.DCT(greenBlocks[i][j]);
            }
        }

        Watermark.addMark(greenBlocks, bits, strength);

//        greenBlocks = machine.iDCT_p(greenBlocks);
        for (int i = 0; i < greenBlocks.length; i++) {
            for (int j = 0; j < greenBlocks[0].length; j++) {
                greenBlocks[i][j] = machine.iDCT(greenBlocks[i][j]);
            }
        }
        DCTMachine.blockMerge(greenBlocks, greens);

        ImageTool.setChannel(pixels, ImageTool.toIntArray(greens), ImageTool.GREEN);
        ImageTool.setPixels(markedImage, pixels);

        return markedImage;
    }

    public static int[] extractMarkFromImage(BufferedImage image) {
        DCTMachine machine = new DCTMachine();

        int[][] pixels = ImageTool.getPixels(image);
        double[][] greens = ImageTool.toDoubleArray(ImageTool.getChannel(pixels, ImageTool.GREEN));

        double[][][][] greenBlocks = DCTMachine.blockPartition(greens, 8);
//        greenBlocks = machine.DCT_p(greenBlocks);
        for (int i = 0; i < greenBlocks.length; i++) {
            for (int j = 0; j < greenBlocks[0].length; j++) {
                greenBlocks[i][j] = machine.DCT(greenBlocks[i][j]);
            }
        }

        return Watermark.extractMark(greenBlocks);
    }

    public static void addMark(double[][][][] blocks, int[] bits, double strength) {
        final int blockWidth = blocks[0].length;
        final int blockHeight = blocks.length;

        for (int i = 0; i < blockHeight; i++) {
            for (int j = 0; j < blockWidth; j++) {
                addBit(blocks[i][j], bits[(i * blockWidth + j) % bits.length], strength);
            }
        }
    }

    public static int[] extractMark(double[][][][] blocks) {
        final int blockWidth = blocks[0].length;
        final int blockHeight = blocks.length;

        int[] bits = new int[blockWidth * blockHeight];

        for (int i = 0; i < blockHeight; i++) {
            for (int j = 0; j < blockWidth; j++) {
                bits[i * blockWidth + j] = extractBit(blocks[i][j]);
            }
        }

        return bits;
    }

    public static int[] infoToArray(String info) {
        Pattern p = Pattern.compile("[^0-9]");
        Matcher m = p.matcher(info);
        info = m.replaceAll("").trim();

        int checkSum = 0;
        for (char c : info.toCharArray()) {
            checkSum += hexToInt(c);
        }
        checkSum %= 0xf;

        String s = "" + intToHex(0xf) + intToHex(checkSum) + info;
        //System.out.println(s);

        int[] result = new int[s.length() * 4];
        for (int i = 0; i < s.length(); i++) {
            int value = hexToInt(s.charAt(i));
            result[i * 4] = value & 0x1;
            result[i * 4 + 1] = (value >> 1) & 0x1;
            result[i * 4 + 2] = (value >> 2) & 0x1;
            result[i * 4 + 3] = (value >> 3) & 0x1;
        }

        return result;
    }

    public static List<String> arrayToInfo(int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length / 4; i++) {
            int value = array[i * 4] + (array[i * 4 + 1] << 1) + (array[i * 4 + 2] << 2) + (array[i * 4 + 3] << 3);
            sb.append(intToHex(value));
        }

        System.out.println(sb.toString());

        String[] infos = sb.toString().split("f");
        ArrayList<String> checkedInfos = new ArrayList<>();
        for (String info : infos) {
            if (info.length() > 0) {
                int checkSum = 0;
                for (char c : info.substring(1).toCharArray()) {
                    checkSum += hexToInt(c);
                }

                if (checkSum % 0xf == hexToInt(info.charAt(0))) {
                    checkedInfos.add(info.substring(1));
                }
            }
        }

        return checkedInfos;
    }

    private static int hexToInt(char c) {
        int i = 0;
        if (c >= '0' && c <= '9') {
            i = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            i = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            i = c - 'A' + 10;
        }
        return i;
    }

    private static char intToHex(int i) {
        return Integer.toHexString(i).toLowerCase().charAt(0);
    }

    private static void addBit(double[][] block, int bit, double strength) {
        final int size = block.length;
        block[size / 2][size / 2] = crossAverage(block, size / 2, size / 2) + (bit != 0 ? 1 : -1) * strength;
    }

    private static int extractBit(double[][] block) {
        final int size = block.length;
        return block[size / 2][size / 2] - crossAverage(block, size / 2, size / 2) > 0 ? 1 : 0;
    }

    private static double crossAverage(double[][] block, int posX, int posY) {
        return (block[posY + 1][posX] + block[posY - 1][posX] + block[posY][posX + 1] + block[posY][posX - 1]) / 4;
    }
}
