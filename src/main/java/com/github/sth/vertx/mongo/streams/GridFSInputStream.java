package com.github.sth.vertx.mongo.streams;

import com.mongodb.async.client.gridfs.AsyncInputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * GridFSInputStream which bridges the gap between vertx's {@link io.vertx.core.streams.WriteStream}
 * and mongodb's {@link com.mongodb.async.client.gridfs.AsyncInputStream}.
 */
public interface GridFSInputStream extends AsyncInputStream, WriteStream<Buffer> {
  @Override
  void end();

  @Override
  GridFSInputStream setWriteQueueMaxSize(int i);

  /**
   * Create a GridFSInputStream
   *
   * @return the stream
   */
  static GridFSInputStream create() {
    return new GridFSInputStreamImpl();
  }
}
