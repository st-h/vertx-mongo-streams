/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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

  @Override
  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> arg0) {
    ((io.vertx.core.streams.WriteStream) delegate).exceptionHandler(arg0);
    return this;
  }

  @Override
  public WriteStream<Buffer> write(Buffer arg0) {
    ((io.vertx.core.streams.WriteStream) delegate).write(arg0 != null ? (io.vertx.core.buffer.Buffer)arg0.getDelegate() : null);
    return this;
  }

  @Override
  public void end(Buffer t) {
    ((io.vertx.core.streams.WriteStream) delegate).end(t != null ? (io.vertx.core.buffer.Buffer)t.getDelegate() : null);
  }

  @Override
  public boolean writeQueueFull() {
    def ret = ((io.vertx.core.streams.WriteStream) delegate).writeQueueFull();
    return ret;
  }

  @Override
  public WriteStream<Buffer> drainHandler(Handler<Void> arg0) {
    ((io.vertx.core.streams.WriteStream) delegate).drainHandler(arg0);
    return this;
  }

  @Override
  public void end() {
    ((io.vertx.core.streams.WriteStream) delegate).end();
  }

  @Override
  public GridFSInputStream setWriteQueueMaxSize(int i) {
    ((io.vertx.core.streams.WriteStream) delegate).setWriteQueueMaxSize(i);
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

  @Override
  void read(ByteBuffer byteBuffer, SingleResultCallback<Integer> singleResultCallback) {
    ((AsyncInputStream) delegate).read(byteBuffer, singleResultCallback);
  }

  @Override
  void close(SingleResultCallback<Void> singleResultCallback) {
    ((AsyncInputStream) delegate).close(singleResultCallback);
  }
}
