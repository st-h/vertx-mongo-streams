package com.github.sth.vertx.mongo.streams;

import com.github.sth.vertx.mongo.streams.util.ResultCallback;
import io.vertx.core.buffer.Buffer;
import org.junit.Assert;
import org.junit.Test;
import com.github.sth.vertx.mongo.streams.util.ByteUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A test to detect any race conditions or locking issues.
 */
public class GridFSInputStreamConcurrencyTest {

    private final static int BUFFER_SIZE = 2048;
    private final static int BUFFER_COUNT = 1000;

    @Test
    public void testReadAndWriteConcurrent() throws InterruptedException {
        GridFSInputStream inputStream = GridFSInputStream.create();
        List<Buffer> buffers = new ArrayList<>();
        for (int i = 0; i < BUFFER_COUNT; i++) {
            buffers.add(Buffer.buffer(ByteUtil.randomBytes(BUFFER_SIZE)));
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_COUNT * BUFFER_SIZE);

        WriteThread writeThread = new WriteThread(inputStream, buffers);
        ReadThread readThread = new ReadThread(inputStream, byteBuffer);

        writeThread.start();
        readThread.start();

        while(writeThread.isAlive() && readThread.isAlive()) {
            Thread.sleep(1000);
        }

        for (int i = 0; i < BUFFER_COUNT; i++) {
            Assert.assertTrue(Arrays.equals(buffers.get(i).getBytes(), Arrays.copyOfRange(byteBuffer.array(), i * BUFFER_SIZE, (i + 1) * BUFFER_SIZE)));
        }
    }

    static class WriteThread extends Thread {

        final GridFSInputStream inputStream;
        final List<Buffer> buffers;

        WriteThread(GridFSInputStream inputStream, List<Buffer> buffers) {
            this.inputStream = inputStream;
            this.buffers = buffers;
        }

        @Override
        public void run() {
            for (Buffer buffer : buffers) {
                this.inputStream.write(buffer);
            }
        }
    }

    static class ReadThread extends Thread {

        final ByteBuffer byteBuffer;
        final GridFSInputStream inputStream;

        ReadThread(GridFSInputStream inputStream, ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            ResultCallback<Integer> resultCallback = new ResultCallback<>();
            while (resultCallback.getResult() == null || resultCallback.getResult() != -1) {
                inputStream.read(byteBuffer, resultCallback);
                if (resultCallback.getResult() != null) {
                    resultCallback = new ResultCallback<>();
                }
            }
        }
    }
}