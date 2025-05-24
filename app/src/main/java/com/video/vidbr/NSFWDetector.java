package com.video.vidbr;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NSFWDetector {

    private Interpreter tflite;
    private static final int IMAGE_SIZE = 224; // ajuste conforme o modelo

    public NSFWDetector(AssetManager assetManager) throws IOException {
        tflite = new Interpreter(loadModelFile(assetManager, "nsfw.tflite"));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(IMAGE_SIZE * IMAGE_SIZE * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        resized.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int pixel : intValues) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            byteBuffer.putFloat(r / 255.0f);
            byteBuffer.putFloat(g / 255.0f);
            byteBuffer.putFloat(b / 255.0f);
        }

        return byteBuffer;
    }

    /**
     * Retorna todas as 5 probabilidades.
     */
    public float[] detectNSFW(Bitmap bitmap) {
        ByteBuffer input = convertBitmapToByteBuffer(bitmap);
        float[][] output = new float[1][5]; // 5 classes
        tflite.run(input, output);
        return output[0];
    }

    public String[] getLabels() {
        return new String[] {
                "Drawings (SFW)",
                "Hentai (NSFW)",
                "Neutral (SFW)",
                "Porn (NSFW)",
                "Sexy (NSFW)"
        };
    }
}
