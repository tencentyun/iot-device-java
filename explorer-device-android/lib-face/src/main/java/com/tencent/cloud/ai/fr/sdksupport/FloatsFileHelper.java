package com.tencent.cloud.ai.fr.sdksupport;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FloatsFileHelper {

    public static void writeFloatsToFile(float[] floats, String filePath) {
        try {
            RandomAccessFile file = new RandomAccessFile(filePath, "rw");
            FileChannel channel = file.getChannel();
            int bytesCount = 4/*one float 4 bytes*/ * floats.length;
            ByteBuffer buffer = ByteBuffer.allocate(bytesCount);
            buffer.clear();
            buffer.asFloatBuffer().put(floats);
            channel.write(buffer);
            buffer.rewind();
            channel.close();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static float[] readFloatsFromFile(String filePath, int floatsCount) {
        float[] floats = new float[floatsCount];
        try {
            RandomAccessFile file = new RandomAccessFile(filePath, "rw");
            FileChannel channel = file.getChannel();
            int bytesCount = 4/*one float 4 bytes*/ * floatsCount;
            ByteBuffer buffer = ByteBuffer.allocate(bytesCount);
            buffer.clear();
            int readByteCount = channel.read(buffer);
            if (readByteCount != bytesCount) {
                throw new IOException(String.format("readByteCount%s != bytesCount%s", readByteCount, bytesCount));
            }
            buffer.rewind();
            buffer.asFloatBuffer().get(floats);
            channel.close();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return floats;
    }
}
