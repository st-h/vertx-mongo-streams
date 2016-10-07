package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.client.gridfs.AsyncOutputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

public interface GridFSOutputStream extends AsyncOutputStream {

    /**
     * Create a {@link GridFSOutputStream}.
     *
     * @param writeStream the stream to write data retrieved from GridFS to
     * @return the stream
     */
    static GridFSOutputStream create(WriteStream<Buffer> writeStream) {
        return new GridFSOutputStreamImpl(writeStream);
    }
}
