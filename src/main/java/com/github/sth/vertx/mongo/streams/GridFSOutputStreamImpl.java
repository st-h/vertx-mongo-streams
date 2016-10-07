package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.SingleResultCallback;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

import java.nio.ByteBuffer;

public class GridFSOutputStreamImpl implements GridFSOutputStream {

    WriteStream<Buffer> writeStream;

    GridFSOutputStreamImpl(WriteStream<Buffer> writeStream) {
        this.writeStream = writeStream;
    }

    @Override
    public void write(ByteBuffer byteBuffer, SingleResultCallback<Integer> singleResultCallback) {

        byte[] bytes = byteBuffer.array();
        Buffer buffer = Buffer.buffer(bytes);

        writeStream.write(buffer);

        singleResultCallback.onResult(bytes.length, null);
    }

    @Override
    public void close(SingleResultCallback<Void> singleResultCallback) {
        singleResultCallback.onResult(null, null);
    }
}
