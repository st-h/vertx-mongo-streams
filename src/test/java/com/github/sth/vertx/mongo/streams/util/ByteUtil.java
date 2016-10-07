package com.github.sth.vertx.mongo.streams.util;

import java.util.Random;

public class ByteUtil {

    /**
     * Generate Random Bytes of specified length.
     * @param length the number of bytes to create
     * @return array of random bytes
     */
    public static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        new Random().nextBytes(b);
        return b;
    }
}
