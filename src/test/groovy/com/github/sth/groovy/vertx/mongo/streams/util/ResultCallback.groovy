package com.github.sth.groovy.vertx.mongo.streams.util;

import com.mongodb.async.SingleResultCallback;

/**
 * SingleResultCallback for synchronous use.
 *
 * @param <T> Type of result
 */
public class ResultCallback<T> implements SingleResultCallback<T> {

    T result;
    Throwable throwable;
    private boolean succeeded;

    @Override
    public void onResult(T t, Throwable throwable) {
        this.result = t;
        this.throwable = throwable;
        if (throwable == null) {
            this.succeeded = true;
        }
    }

    public T getResult() {
        return this.result;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    /**
     * Method to get state of callback.
     * @return true if the invocation has been successful.
     */
    public boolean succeeded() {
        return this.succeeded && this.throwable == null;
    }
}
