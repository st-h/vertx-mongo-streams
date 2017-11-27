package com.github.sth.vertx.mongo.streams

import com.github.sth.vertx.mongo.streams.util.ByteUtil
import com.github.sth.vertx.mongo.streams.util.ResultCallback;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer

import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import io.vertx.core.buffer.Buffer;

public class GridFSOutputStreamTest {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 4;

    /**
     * Test that bytes are written to the provided WriteStream correctly and the callback returns
     * the expected result.
     */
    @Test
    public void happyPathWrite() {

        byte[] bytes = ByteUtil.randomBytes(2048);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ResultCallback<Integer> resultCallback = new ResultCallback<>();
        WriteStream<Buffer> writeStream = new FakeWriteStream<Buffer>()
        GridFSOutputStream outputStream = GridFSOutputStream.create(writeStream);

        outputStream.write(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(2048, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(writeStream.received[0].getBytes(), bytes));
    }

    /**
     * Test that bytes are written to the provided WriteStream correctly and the callback returns
     * the expected result.
     */
    @Test
    public void happyPathWriteWithNotAlignedBuffer() {

        byte[] bytes = "123456789ABCDEF".getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        ResultCallback<Integer> resultCallback = new ResultCallback<>();
        WriteStream<Buffer> writeStream = new FakeWriteStream<Buffer>()
        GridFSOutputStream outputStream = GridFSOutputStream.create(writeStream);

        outputStream.write(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(bytes.length, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(writeStream.received[0].getBytes(), bytes));
    }

    /**
     * Test that the callback returns the expected result when the stream is closed.
     */
    @Test
    public void happyPathClose() {

        ResultCallback<Void> resultCallback = new ResultCallback<>();
        WriteStream<Buffer> writeStream = new FakeWriteStream<Buffer>()
        GridFSOutputStream outputStream = GridFSOutputStream.create(new FakeWriteStream<Buffer>());

        outputStream.close(resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertNull(writeStream.received[0]);
    }


    private class FakeWriteStream<T> implements WriteStream<T> {

        int maxSize;
        List<T> received = new ArrayList<>();
        Handler<Void> drainHandler;

        void clearReceived() {
            boolean callDrain = writeQueueFull();
            received = new ArrayList<>();
            if (callDrain && drainHandler != null) {
                drainHandler.handle(null);
            }
        }

        public FakeWriteStream setWriteQueueMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public boolean writeQueueFull() {
            return received.size() >= maxSize;
        }

        public FakeWriteStream drainHandler(Handler<Void> handler) {
            this.drainHandler = handler;
            return this;
        }

        public FakeWriteStream write(T data) {
            received.add(data);
            return this;
        }

        public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
            return this;
        }

        @Override
        public void end() {
        }
    }
}
