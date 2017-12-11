package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.client.gridfs.AsyncInputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * {@link com.github.sth.vertx.mongo.streams.GridFSInputStream} which bridges the gap between vertx's {@link io.vertx.core.streams.WriteStream}
 * and mongodb's {@link com.mongodb.async.client.gridfs.AsyncInputStream}.
 *
 * @version $Id: $Id
 */
public interface GridFSInputStream extends AsyncInputStream, WriteStream<Buffer> {

    /**
     * Signals that all data has been consumed. After this method is called and the internal buffer
     * has been written to the database, the driver will be signalled that all data has been processed.
     */
    @Override
    void end();

    /**
     * Sets the maximum internal buffer size.
     *
     * @param size the size.
     * @return {@link GridFSInputStream}
     */
    @Override
    GridFSInputStream setWriteQueueMaxSize(int size);

    /**
     * Create a {@link com.github.sth.vertx.mongo.streams.GridFSInputStream}.
     *
     * @return the stream
     */
    static GridFSInputStream create() {
        return new GridFSInputStreamImpl();
    }

    /**
     * Create a {@link com.github.sth.vertx.mongo.streams.GridFSInputStream}.
     *
     * @param queueSize the initial queue size
     * @return the stream
     */
    static GridFSInputStream create(int queueSize) {
        return new GridFSInputStreamImpl(queueSize);
    }

    public byte[] getInBytes();

    public byte[] getWrittenBytes();
}
