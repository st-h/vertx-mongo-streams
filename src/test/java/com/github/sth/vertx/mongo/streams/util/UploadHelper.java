package com.github.sth.vertx.mongo.streams.util;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

public class UploadHelper {

    public static void uploadDownload(Vertx vertx, int port, TestContext context, byte[] bytes) throws RuntimeException {
        uploadDownload(vertx, port, context, bytes, null, null);
    }

    public static void uploadDownload(Vertx vertx, int port, TestContext context, byte[] bytes, Long offset, Long end) throws RuntimeException {

        HttpClient client = vertx.createHttpClient();
        final Async uploadAsync = context.async();
        final String uploadedMD5 = md5(bytes);

        final String[] id = {null};
        final String[] serverMD5 = {null};


        HttpClientRequest request = client.post(port, "localhost", "/", (HttpClientResponse response) -> {

            response.bodyHandler((Buffer body) -> {
                Map resp = Json.decodeValue(body.toString(), Map.class);
                id[0] = (String) resp.get("id");
                serverMD5[0] = ((String) resp.get("md5")).toUpperCase();
                context.assertNotNull(body);

                uploadAsync.complete();
            });
        }).setChunked(true);

        request.headers().add("content-type", "multipart/form-data; boundary=MyBoundary");

        Buffer buffer = Buffer.buffer();
        buffer.appendString("--MyBoundary\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"test\"; filename=\"test.jpg\"\r\n");
        buffer.appendString("Content-Type: application/octet-stream\r\n");
        buffer.appendString("Content-Transfer-Encoding: binary\r\n");
        buffer.appendString("\r\n");

        buffer.appendBytes(bytes);
        buffer.appendString("\r\n");

        buffer.appendString("--MyBoundary--\r\n");

        request.end(buffer);

        uploadAsync.awaitSuccess();

        Async downloadAsync = context.async();

        request = client.get(port, "localhost", "/" + id[0], (HttpClientResponse response) -> {
            response.bodyHandler((Buffer body) -> {
                byte[] downloaded = body.getBytes();
                System.out.println("download size: " + downloaded.length);
                if (offset == null) {
                    String downloadedMD5 = md5(downloaded);
                    context.assertEquals(downloadedMD5, serverMD5[0], "downloaded file md5 does not match md5 calculated on server");
                    context.assertEquals(uploadedMD5, serverMD5[0], "uploaded file md5 does not match md5 calculated on server");
                    context.assertTrue(Arrays.equals(downloaded, bytes), "uploaded and download file content differs!");
                } else {
                    byte[] expected;
                    if (end != null) {
                        expected = Arrays.copyOfRange(bytes, Math.toIntExact(offset), Math.toIntExact(end));
                    } else {
                        expected = Arrays.copyOfRange(bytes, Math.toIntExact(offset), bytes.length);
                    }
                    context.assertTrue(Arrays.equals(downloaded, expected), "uploaded and download file content differs!");
                }
                downloadAsync.complete();
            });
        });
        if (offset != null) {
            if (end != null) {
                request.putHeader("range", String.format("bytes=%d-%d", offset, end));
            } else {
                request.putHeader("range", String.format("bytes=%d-", offset));
            }
        }
        request.end();
    }

    private static String md5(byte[] bytes) throws RuntimeException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            return DatatypeConverter.printHexBinary(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
