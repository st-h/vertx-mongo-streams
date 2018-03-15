package com.github.sth.vertx.mongo.streams;

import com.github.sth.vertx.mongo.streams.util.CircularByteBuffer;
import com.mongodb.async.SingleResultCallback;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * <p>GridFSInputStreamImpl class.</p>
 *
 * @version $Id: $Id
 */
public class GridFSInputStreamImpl implements GridFSInputStream {

    private final Vertx vertx;
    private int writeQueueMaxSize;
    private final CircularByteBuffer buffer;
    private Handler<Void> drainHandler;
    private volatile boolean closed = false;
    private SingleResultCallback<Integer> pendingCallback;
    private ByteBuffer outputBuffer;

    private List<Byte> inBytes = new ArrayList<>();
    private List<Byte> outBytes = new ArrayList<>();

    public GridFSInputStreamImpl(final Vertx vertx) {
        buffer = new CircularByteBuffer(8192);
        writeQueueMaxSize = buffer.capacity();
        this.vertx = vertx;
    }

    public GridFSInputStreamImpl(final Vertx vertx, final int queueSize) {
        buffer = new CircularByteBuffer(queueSize);
        writeQueueMaxSize = queueSize;
        this.vertx = vertx;
    }

    public byte[] getInBytes() {
        byte[] bytes = new byte[inBytes.size()];
        for (int i = 0; i < inBytes.size(); i++) {
            bytes[i] = inBytes.get(i);
        }
        return bytes;
    }

    public byte[] getWrittenBytes() {
        byte[] bytes = new byte[outBytes.size()];
        for (int i = 0; i < outBytes.size(); i++) {
            bytes[i] = outBytes.get(i);
        }
        return bytes;
    }

    public void read(ByteBuffer b, SingleResultCallback<Integer> c) {
        synchronized (buffer) {
            //If nothing pending and the stream is still open, store the callback for future processing
            if (buffer.isEmpty() && !closed) {
                storeCallback(b, c);
            } else {
                doCallback(b, c);
            }
        }
    }

    private void storeCallback(final ByteBuffer b, final SingleResultCallback<Integer> c) {
        if (pendingCallback != null && pendingCallback != c) {
            c.onResult(null, new RuntimeException("mongo provided a new buffer or callback before the previous " +
                    "one has been fulfilled"));
        }
        this.outputBuffer = b;
        this.pendingCallback = c;
    }

    private void doCallback(final ByteBuffer b, final SingleResultCallback<Integer> c) {
        pendingCallback = null;
        outputBuffer = null;
        final int bytesWritten = buffer.drainInto(b);
        c.onResult(bytesWritten, null);
        // if there is a drain handler and the buffer is less than half full, call the drain handler
        if (drainHandler != null && buffer.remaining() < writeQueueMaxSize / 2) {
            final Handler<Void> handler = drainHandler;
            vertx.runOnContext(v -> handler.handle(null));
            drainHandler = null;
        }
    }


    public WriteStream<Buffer> write(Buffer inputBuffer) {
        if (closed) throw new IllegalStateException("Stream is closed");
        final byte[] bytes = inputBuffer.getBytes();

        for (int i = 0; i < bytes.length; i++) {
            inBytes.add(bytes[i]);
        }
        final ByteBuffer wrapper = ByteBuffer.wrap(bytes);
        synchronized (buffer) {
            if (pendingCallback != null) {
                int bytesWritten = writeOutput(wrapper);
                if (bytesWritten > 0) doCallback(bytesWritten);
            }
            // Drain content left in the input buffer
            buffer.fillFrom(wrapper);
        }
        return this;
    }

    private int writeOutput(final ByteBuffer wrapper) {
        // First we drain the pending buffer
        int bytesWritten = buffer.drainInto(outputBuffer);
        final int remaining = outputBuffer.remaining();
        if (remaining > 0) {
            // If more space left in the output buffer we directly drain the input buffer
            final int newBytesWritten = remaining > wrapper.capacity() ? wrapper.capacity() : remaining;
            // Store current limit to restore it in case we don't drain then whole buffer
            final int limit = wrapper.limit();
            wrapper.limit(newBytesWritten);
            outputBuffer.put(wrapper);
            wrapper.limit(limit);
            bytesWritten += newBytesWritten;
        }
        return bytesWritten;
    }


    private void doCallback(final int bytesWritten) {
        SingleResultCallback<Integer> c = pendingCallback;
        outputBuffer = null;
        pendingCallback = null;
        c.onResult(bytesWritten, null);
    }

    public void close(SingleResultCallback<Void> singleResultCallback) {
        closed = true;
        singleResultCallback.onResult(null, null);
    }

    public void end() {
        synchronized (buffer) {
            if (pendingCallback != null) {
                final int bytesWritten = buffer.drainInto(outputBuffer);
                doCallback(bytesWritten);
            }
            this.closed = true;
        }
    }

    public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    public GridFSInputStream setWriteQueueMaxSize(int i) {
        writeQueueMaxSize = i;
        return this;
    }

    public boolean writeQueueFull() {
        return buffer.remaining() >= writeQueueMaxSize;
    }

    public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
        this.drainHandler = handler;
        return this;
    }

}
