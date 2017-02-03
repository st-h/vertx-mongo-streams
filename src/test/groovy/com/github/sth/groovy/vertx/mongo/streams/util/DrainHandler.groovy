package com.github.sth.groovy.vertx.mongo.streams.util;

import io.vertx.core.Handler;

/**
 * A simple DrainHandler to use for tests.
 */
public class DrainHandler implements Handler<Void> {

    boolean called;

    public boolean wasCalled() {
        return this.called;
    }

    @Override
    public void handle(Void aVoid) {
        this.called = true;
    }
}
