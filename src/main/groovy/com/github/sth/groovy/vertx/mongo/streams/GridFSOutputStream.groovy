package com.github.sth.groovy.vertx.mongo.streams

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.AsyncOutputStream
import groovy.transform.CompileStatic
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.streams.WriteStream
import io.vertx.lang.groovy.InternalHelper

import java.nio.ByteBuffer

@CompileStatic
class GridFSOutputStream implements AsyncOutputStream {

    private final def com.github.sth.vertx.mongo.streams.GridFSOutputStream delegate;

    public GridFSOutputStream(Object delegate) {
        this.delegate = (com.github.sth.vertx.mongo.streams.GridFSOutputStream) delegate;
    }

    public Object getDelegate() {
        return delegate;
    }

    @Override
    void write(ByteBuffer byteBuffer, SingleResultCallback<Integer> singleResultCallback) {
        delegate.write(byteBuffer, singleResultCallback)
    }

    @Override
    void close(SingleResultCallback<Void> singleResultCallback) {
        delegate.close(singleResultCallback)
    }

    /**
     * Create a GridFSOutputStream
     * @return the stream
     */
    public static GridFSOutputStream create(WriteStream<Buffer> writeStream) {
        def ret = InternalHelper.safeCreate(com.github.sth.vertx.mongo.streams.GridFSOutputStream.create(
                ((io.vertx.core.streams.WriteStream<io.vertx.core.buffer.Buffer>) writeStream.delegate)), GridFSOutputStream.class);
        return ret;
    }
}
