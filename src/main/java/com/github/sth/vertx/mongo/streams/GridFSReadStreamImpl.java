package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.client.gridfs.GridFSDownloadStream;
import static io.netty.buffer.Unpooled.copiedBuffer;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

import java.nio.ByteBuffer;

public class GridFSReadStreamImpl implements GridFSReadStream {

    private final Context context;

    private Handler<Throwable> exceptionHandler;
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private final GridFSDownloadStream downloadStream;

    private Long bytesToRead;

    private volatile ByteBuf byteBuf; // contains data that has been read, but has not been handled yet

    private ByteBuffer buffer; // gridfs writes into this buffer

    private boolean paused = false;

    public GridFSReadStreamImpl(Vertx vertx, GridFSDownloadStream downloadStream, int batchSize, Long bytesToRead) {
        this.downloadStream = downloadStream;
        this.context = vertx.getOrCreateContext();
        this.bytesToRead = bytesToRead;

        downloadStream.batchSize(batchSize);
        buffer = ByteBuffer.allocate(batchSize);
    }

    @Override
    public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public ReadStream<Buffer> handler(Handler<Buffer> handler) {
        this.dataHandler = handler;
        this.read();
        return this;
    }

    private synchronized void read() {
        if (paused || byteBuf != null) {
            return;
        }

        if (bytesToRead != null && bytesToRead < 0) {
            invokeEndHandler();
            return;
        }

        downloadStream.read(buffer, (Integer read, Throwable t) -> {
            if (t != null) {
                if (exceptionHandler != null) {
                    exceptionHandler.handle(t);
                } else {
                    throw new RuntimeException(t);
                }
            } else {
                final ByteBuf byteBuf;
                buffer.flip();

                if (read < 0) {
                    this.invokeEndHandler();
                    return;
                } else if (bytesToRead != null && bytesToRead < read) {
                    byteBuf = copiedBuffer(buffer).readBytes(bytesToRead.intValue());
                } else {
                    byteBuf = copiedBuffer(buffer).readBytes(read);
                }

                if (bytesToRead != null) {
                    bytesToRead -= read;
                }

                buffer.clear();
                this.byteBuf = byteBuf;
                invokeHandler();
            }
        });
    }

    private synchronized  void invokeEndHandler() {
        if (endHandler == null && exceptionHandler != null) {
            exceptionHandler.handle(new RuntimeException("no end handler has been set on GridFSReadStream"));
            return;
        }
        context.runOnContext((aVoid) -> endHandler.handle(null));
    }

    private synchronized void invokeHandler() {
        context.runOnContext((aVoid) -> {
            if (byteBuf != null && dataHandler != null) {
                dataHandler.handle(Buffer.buffer(byteBuf));
                byteBuf = null;
                read();
            }
        });
    }

    @Override
    public ReadStream<Buffer> pause() {
        this.paused = true;
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        if (paused) {
            this.paused = false;
            this.read();
        }
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }
}
