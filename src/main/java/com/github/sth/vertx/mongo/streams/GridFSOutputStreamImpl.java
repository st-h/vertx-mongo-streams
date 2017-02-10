package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.SingleResultCallback;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

class GridFSOutputStreamImpl implements GridFSOutputStream {

    private final WriteStream<Buffer> writeStream;

    GridFSOutputStreamImpl(WriteStream<Buffer> writeStream) {
        this.writeStream = writeStream;
    }

    @Override
    public void write(ByteBuffer byteBuffer, SingleResultCallback<Integer> singleResultCallback) {
        //  Buffer does not expose the internal ByteBuffer hence this is the only way to correctly set position and limit
        final ByteBuf byteBuf = copiedBuffer(byteBuffer);
        final Buffer buffer = Buffer.buffer(byteBuf);
        writeStream.write(buffer);
        singleResultCallback.onResult(byteBuf.readableBytes(), null);
    }

    @Override
    public void close(SingleResultCallback<Void> singleResultCallback) {
        singleResultCallback.onResult(null, null);
    }
}
