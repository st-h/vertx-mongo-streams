package com.github.sth.vertx.mongo.streams;

import com.github.sth.vertx.mongo.streams.util.ByteUtil;
import com.github.sth.vertx.mongo.streams.util.DrainHandler;
import com.github.sth.vertx.mongo.streams.util.ResultCallback;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Arrays;

@RunWith(VertxUnitRunner.class)
public class GridFSInputStreamTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    /**
     * Test write all data before any data is consumed. The stream is ended before a new read callback has been
     * made available.
     */
    @Test
    public void happyPathReadAfterWrite(TestContext context) {

        GridFSInputStream inputStream = GridFSInputStream.create(rule.vertx());

        Buffer buffer1 = Buffer.buffer(ByteUtil.randomBytes(2048));
        Buffer buffer2 = Buffer.buffer(ByteUtil.randomBytes(2048));

        // write data to the stream
        inputStream.write(buffer1);
        inputStream.write(buffer2);
        // signal that no more data is to be expected
        inputStream.end();

        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        ResultCallback<Integer> resultCallback = new ResultCallback<>();

        // consume the stream
        inputStream.read(byteBuffer, resultCallback);

        context.assertTrue(resultCallback.succeeded());
        context.assertEquals(4096, resultCallback.getResult());

        context.assertTrue(Arrays.equals(buffer1.getBytes(), Arrays.copyOfRange(byteBuffer.array(), 0, 2048)));
        context.assertTrue(Arrays.equals(buffer2.getBytes(), Arrays.copyOfRange(byteBuffer.array(), 2048, 4096)));

        // on the next invocation the mongo driver should be signaled that no more data is available
        resultCallback = new ResultCallback<>();
        inputStream.read(byteBuffer, resultCallback);

        context.assertTrue(resultCallback.succeeded());
        context.assertEquals(0, resultCallback.getResult());
    }

    /**
     * Test reads between writes. The stream is ended after a new read callback has been made available.
     */
    @Test
    public void happyPathReadBetweenWrites() {
        GridFSInputStream inputStream = GridFSInputStream.create(rule.vertx());

        Buffer buffer1 = Buffer.buffer(ByteUtil.randomBytes(2048));
        Buffer buffer2 = Buffer.buffer(ByteUtil.randomBytes(2048));

        // write data to the stream
        inputStream.write(buffer1);

        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        ResultCallback<Integer> resultCallback = new ResultCallback<>();

        // consume the stream
        inputStream.read(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(2048, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(buffer1.getBytes(), Arrays.copyOf(byteBuffer.array(), 2048)));

        // write data to the stream
        inputStream.write(buffer2);

        // consume the stream
        inputStream.read(byteBuffer, resultCallback);

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(2048, resultCallback.getResult(), 0);
        Assert.assertTrue(Arrays.equals(buffer1.getBytes(), Arrays.copyOf(byteBuffer.array(), 2048)));

        // on the next invocation the mongo driver should be signaled that no more data is available
        resultCallback = new ResultCallback<>();
        inputStream.read(byteBuffer, resultCallback);

        // signal that no more data is to be expected
        inputStream.end();

        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(0, resultCallback.getResult(), 0);
    }

    /**
     * Test that a full queue is reported correctly.
     */
    @Test
    public void testWriteQueueFull() {
        GridFSInputStream inputStream = GridFSInputStream.create(rule.vertx());

        // empty queue should not be full
        Assert.assertFalse(inputStream.writeQueueFull());

        Buffer buffer = Buffer.buffer(ByteUtil.randomBytes(4096));

        // write data to the stream
        inputStream.write(buffer);
        Assert.assertFalse(inputStream.writeQueueFull());

        // write more data to the stream: 8191 < 8192
        buffer = Buffer.buffer(ByteUtil.randomBytes(4095));
        inputStream.write(buffer);
        Assert.assertFalse(inputStream.writeQueueFull());

        // queue should be full (8192 bytes in queue)
        buffer = Buffer.buffer(ByteUtil.randomBytes(1));
        inputStream.write(buffer);
        Assert.assertTrue(inputStream.writeQueueFull());
    }

    /**
     * Test that the drain handler is called when the queue is emptied to half its capacity.
     */
    @Test
    public void testDrainHandler(TestContext context) {
        Async async = context.async();
        Vertx vertx = rule.vertx();
        GridFSInputStream inputStream = GridFSInputStream.create(vertx);

        Buffer buffer = Buffer.buffer(ByteUtil.randomBytes(8192));

        // write more data than the queue has capacity
        inputStream.write(buffer);

        // set a drain handler
        DrainHandler handler = new DrainHandler();
        inputStream.drainHandler(handler);

        // read 4096 bytes, one byte before drain handler should be called as it is below half its capacity
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        ResultCallback<Integer> resultCallback = new ResultCallback<>();

        // consume the stream
        inputStream.read(byteBuffer, resultCallback);
        Assert.assertTrue(resultCallback.succeeded());
        Assert.assertEquals(4096, resultCallback.getResult(), 0);
        Assert.assertFalse(handler.wasCalled());

        // read one more byte
        byteBuffer = ByteBuffer.allocate(1);
        inputStream.read(byteBuffer, resultCallback);

        // drain handler should be called
        vertx.runOnContext((h) -> {
            Assert.assertTrue(resultCallback.succeeded());
            Assert.assertEquals(1, resultCallback.getResult(), 0);
            Assert.assertTrue(handler.wasCalled());
            async.complete();
        });
    }

    /**
     * Test that if multiple callbacks are provided without fulfilling previously available throws an exception
     */
    @Test
    public void testMultipleReadCallbacksShouldFail() {
        GridFSInputStream inputStream = GridFSInputStream.create(rule.vertx());

        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
        ResultCallback<Integer> resultCallback1 = new ResultCallback<>();
        inputStream.read(byteBuffer, resultCallback1);

        Assert.assertFalse(resultCallback1.succeeded());
        Assert.assertNull(resultCallback1.getThrowable());

        ResultCallback<Integer> resultCallback2 = new ResultCallback<>();
        inputStream.read(byteBuffer, resultCallback2);
        Assert.assertFalse(resultCallback2.succeeded());
        Assert.assertEquals(RuntimeException.class, resultCallback2.getThrowable().getClass());
    }

    /**
     * Test that the writeQueue size is set correctly.
     */
    @Test
    public void testSetWriteQueue() {
        GridFSInputStream inputStream = GridFSInputStream.create(rule.vertx());

        inputStream.setWriteQueueMaxSize(2);

        Buffer buffer = Buffer.buffer(ByteUtil.randomBytes(1));
        inputStream.write(buffer);

        Assert.assertFalse(inputStream.writeQueueFull());

        inputStream.write(buffer);

        Assert.assertTrue(inputStream.writeQueueFull());
    }
}
