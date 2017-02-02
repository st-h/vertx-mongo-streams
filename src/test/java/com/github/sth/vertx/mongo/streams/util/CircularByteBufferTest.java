package com.github.sth.vertx.mongo.streams.util;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by antonio on 31/01/2017.
 */
public class CircularByteBufferTest {


    @Test
    public void fillAndDrainSucceeds() throws Exception {
        CircularByteBuffer buffer = new CircularByteBuffer(10);
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        IntStream.range(0, 10).forEach(b -> byteBuffer.put((byte) b));
        byteBuffer.flip();
        buffer.fillFrom(byteBuffer);
        byteBuffer.clear();
        buffer.drainInto(byteBuffer);
        byteBuffer.flip();
        IntStream.range(0, 10).forEach(b -> assertEquals(b, byteBuffer.get()));
        assertEquals(0, buffer.remaining());
        assertEquals(16, buffer.capacity());
    }

    @Test
    public void resizeSucceeds() throws Exception {
        CircularByteBuffer buffer = new CircularByteBuffer(10);
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        IntStream.range(0, 100).forEach(b -> byteBuffer.put((byte) b));
        byteBuffer.flip();
        buffer.fillFrom(byteBuffer);
        byteBuffer.clear();
        buffer.drainInto(byteBuffer);
        byteBuffer.flip();
        IntStream.range(0, 100).forEach(b -> assertEquals(b, byteBuffer.get()));
        assertEquals(0, buffer.remaining());
        assertEquals(128, buffer.capacity());
    }

    @Test
    public void resizeWhenWrappedSucceeds() throws Exception {
        CircularByteBuffer buffer = new CircularByteBuffer(16);
        ByteBuffer byteBuffer16 = ByteBuffer.allocate(16);
        IntStream.range(0, 16).forEach(b -> byteBuffer16.put((byte) b));
        byteBuffer16.flip();
        buffer.fillFrom(byteBuffer16);
        assertEquals(16, buffer.remaining());
        byteBuffer16.clear();
        ByteBuffer byteBuffer8 = ByteBuffer.allocate(8);
        buffer.drainInto(byteBuffer8);
        assertEquals(8, buffer.remaining());
        ByteBuffer byteBuffer32 = ByteBuffer.allocate(32);
        IntStream.range(16, 48).forEach(b -> byteBuffer32.put((byte) b));
        byteBuffer32.flip();
        buffer.fillFrom(byteBuffer32);
        ByteBuffer byteBuffer = ByteBuffer.allocate(40);
        buffer.drainInto(byteBuffer);
        byteBuffer.flip();
        IntStream.range(8, 48).forEach(b -> assertEquals(b, byteBuffer.get()));
        assertEquals(0, buffer.remaining());
        assertEquals(64, buffer.capacity());
    }
}