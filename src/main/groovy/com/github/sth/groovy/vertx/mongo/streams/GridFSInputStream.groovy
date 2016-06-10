package com.github.sth.groovy.vertx.mongo.streams

import com.mongodb.async.SingleResultCallback;
import groovy.transform.CompileStatic
import io.vertx.lang.groovy.InternalHelper
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.streams.WriteStream
import io.vertx.core.Handler
import com.mongodb.async.client.gridfs.AsyncInputStream

import java.nio.ByteBuffer

/**
 * GridFSInputStream which bridges the gap between vertx's {@link io.vertx.groovy.core.streams.WriteStream}
 * and mongodb's {@link com.mongodb.async.client.gridfs.AsyncInputStream}.
*/
@CompileStatic
public class GridFSInputStream implements WriteStream<Buffer>, AsyncInputStream {
  private final def com.github.sth.vertx.mongo.streams.GridFSInputStream delegate;
  public GridFSInputStream(Object delegate) {
    this.delegate = (com.github.sth.vertx.mongo.streams.GridFSInputStream) delegate;
  }
  public Object getDelegate() {
    return delegate;
  }

  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    ((io.vertx.core.streams.WriteStream) delegate).exceptionHandler(handler);
    return this;
  }

  public WriteStream<Buffer> write(Buffer buffer) {
    ((io.vertx.core.streams.WriteStream) delegate).write(buffer != null ? (io.vertx.core.buffer.Buffer)buffer.getDelegate() : null);
    return this;
  }

  public void end(Buffer buffer) {
    ((io.vertx.core.streams.WriteStream) delegate).end(buffer != null ? (io.vertx.core.buffer.Buffer)buffer.getDelegate() : null);
  }

  public boolean writeQueueFull() {
    def ret = ((io.vertx.core.streams.WriteStream) delegate).writeQueueFull();
    return ret;
  }

  public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
    ((io.vertx.core.streams.WriteStream) delegate).drainHandler(handler);
    return this;
  }

  public void end() {
    ((io.vertx.core.streams.WriteStream) delegate).end();
  }

  /**
   * Sets the maximum internal buffer size.
   * @param size the size.
   * @return {@link com.github.sth.groovy.vertx.mongo.streams.GridFSInputStream}
   */
  public GridFSInputStream setWriteQueueMaxSize(int size) {
    ((io.vertx.core.streams.WriteStream) delegate).setWriteQueueMaxSize(size);
    return this;
  }
  /**
   * Create a GridFSInputStream
   * @return the stream
   */
  public static GridFSInputStream create() {
    def ret = InternalHelper.safeCreate(com.github.sth.vertx.mongo.streams.GridFSInputStream.create(),
            GridFSInputStream.class);
    return ret;
  }

  void read(ByteBuffer byteBuffer, SingleResultCallback<Integer> singleResultCallback) {
    ((AsyncInputStream) delegate).read(byteBuffer, singleResultCallback);
  }

  void close(SingleResultCallback<Void> singleResultCallback) {
    ((AsyncInputStream) delegate).close(singleResultCallback);
  }
}
