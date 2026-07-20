package com.mymeetings.qr.generator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class QrCodeGenerator {

    /**
     * Renders a ZXing QR Code Matrix directly into a JavaFX WritableImage.
     * Avoids native SwingFXUtils/javafx-swing dependencies.
     * Maps pixels manually to eliminate rounding margins and enforce exactly a 2px border.
     */
    public static WritableImage generateQrImage(String text, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        // Encode with minimal size to get raw matrix dimensions
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 1, 1, hints);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        int margin = 2; // 2px border spacing
        double activeWidth = width - 2 * margin;
        double activeHeight = height - 2 * margin;

        double moduleWidth = activeWidth / matrixWidth;
        double moduleHeight = activeHeight / matrixHeight;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < margin || x >= width - margin || y < margin || y >= height - margin) {
                    pixelWriter.setColor(x, y, Color.WHITE);
                } else {
                    int mx = (int) ((x - margin) / moduleWidth);
                    int my = (int) ((y - margin) / moduleHeight);
                    mx = Math.min(Math.max(mx, 0), matrixWidth - 1);
                    my = Math.min(Math.max(my, 0), matrixHeight - 1);

                    Color color = bitMatrix.get(mx, my) ? Color.BLACK : Color.WHITE;
                    pixelWriter.setColor(x, y, color);
                }
            }
        }
        return image;
    }

    /**
     * Saves the QR Code directly to a file path as a PNG image. Enforces a 5px margin.
     */
    public static void saveQrToFile(String text, int width, int height, Path filePath) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 1, 1, hints);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();

        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        
        int margin = 5; // 5px margin for files
        double activeWidth = width - 2 * margin;
        double activeHeight = height - 2 * margin;

        double moduleWidth = activeWidth / matrixWidth;
        double moduleHeight = activeHeight / matrixHeight;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < margin || x >= width - margin || y < margin || y >= height - margin) {
                    image.setRGB(x, y, 0xFFFFFF); // White
                } else {
                    int mx = (int) ((x - margin) / moduleWidth);
                    int my = (int) ((y - margin) / moduleHeight);
                    mx = Math.min(Math.max(mx, 0), matrixWidth - 1);
                    my = Math.min(Math.max(my, 0), matrixHeight - 1);

                    int color = bitMatrix.get(mx, my) ? 0x000000 : 0xFFFFFF;
                    image.setRGB(x, y, color);
                }
            }
        }
        javax.imageio.ImageIO.write(image, "PNG", filePath.toFile());
    }
}
