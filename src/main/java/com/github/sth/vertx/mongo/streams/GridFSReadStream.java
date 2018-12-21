package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.client.gridfs.GridFSDownloadStream;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public interface GridFSReadStream extends ReadStream<Buffer> {

    static GridFSReadStream create(Vertx vertx, GridFSDownloadStream downloadStream) {
        return new GridFSReadStreamImpl(vertx, downloadStream, 8192, null);
    }

    static GridFSReadStream create(Vertx vertx, GridFSDownloadStream downloadStream, long bytesToRead) {
        return new GridFSReadStreamImpl(vertx, downloadStream, 8192, bytesToRead);
    }

    static GridFSReadStream create(Vertx vertx, GridFSDownloadStream downloadStream, int batchSize) {
        return new GridFSReadStreamImpl(vertx, downloadStream, batchSize, null);
    }

    static GridFSReadStream create(Vertx vertx, GridFSDownloadStream downloadStream, long bytesToRead, int batchSize) {
        return new GridFSReadStreamImpl(vertx, downloadStream, batchSize, bytesToRead);
    }
}
