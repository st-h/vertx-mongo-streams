package com.github.sth.groovy.vertx.mongo.streams

import com.github.sth.groovy.vertx.mongo.streams.util.ByteUtil;
import com.github.sth.groovy.vertx.mongo.streams.util.ResultCallback;
import io.vertx.groovy.core.streams.WriteStreamImpl;
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
        WriteStreamMock writeStreamMock = new WriteStreamMock();
        GridFSOutputStream outputStream = GridFSOutputStream.create(new WriteStreamImpl<Buffer>(writeStreamMock));

        outputStream.write(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(2048, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(writeStreamMock.buffer.getBytes(), bytes));
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
        WriteStreamMock writeStreamMock = new WriteStreamMock();
        GridFSOutputStream outputStream = GridFSOutputStream.create(new WriteStreamImpl<Buffer>(writeStreamMock));

        outputStream.write(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(bytes.length, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(writeStreamMock.buffer.getBytes(), bytes));
    }

    /**
     * Test that the callback returns the expected result when the stream is closed.
     */
    @Test
    public void happyPathClose() {

        ResultCallback<Void> resultCallback = new ResultCallback<>();
        WriteStreamMock writeStreamMock = new WriteStreamMock();
        GridFSOutputStream outputStream = GridFSOutputStream.create(new WriteStreamImpl<Buffer>(writeStreamMock));

        outputStream.close(resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertNull(writeStreamMock.buffer);
    }

    private static class WriteStreamMock implements WriteStream<Buffer> {

        public Buffer buffer;

        @Override
        public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            return null;
        }

        @Override
        public WriteStream<Buffer> write(Buffer buffer) {
            this.buffer = buffer;
            return this;
        }

        @Override
        public void end() {

        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int i) {
            return null;
        }

        @Override
        public boolean writeQueueFull() {
            return false;
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            return null;
        }
    }
}
