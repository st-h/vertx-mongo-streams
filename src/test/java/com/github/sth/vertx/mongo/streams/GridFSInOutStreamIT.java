package com.github.sth.vertx.mongo.streams;

import com.github.sth.vertx.mongo.streams.util.ByteUtil;
import com.github.sth.vertx.mongo.streams.util.IntegrationTestVerticle;
import com.mongodb.async.client.MongoClients;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class GridFSInOutStreamIT {

    Vertx vertx;
    int port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
        vertx.deployVerticle(IntegrationTestVerticle.class.getName(), options, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async(2);
        MongoClients.create().getDatabase(IntegrationTestVerticle.DB_NAME).drop((Void aVoid, Throwable t) -> {
            async.countDown();
        });

        vertx.close((AsyncResult<Void> result) -> async.countDown());
        async.awaitSuccess(10000);
    }

    @Test
    public void testUploadAndDownload(TestContext context) {
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024);
        uploadDownload(context, bytes);
    }

    @Test
    public void testUploadAndDownloadLarge(TestContext context) {
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024 * 17);
        uploadDownload(context, bytes);
    }

    private String md5(byte[] bytes) throws RuntimeException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            return DatatypeConverter.printHexBinary(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadDownload(TestContext context, byte[] bytes) throws RuntimeException {

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

        client.get(port, "localhost", "/" + id[0], (HttpClientResponse response) -> {
            response.bodyHandler((Buffer body) -> {
                byte[] downloaded = body.getBytes();
                String downloadedMD5 = md5(downloaded);
                context.assertEquals(downloadedMD5, serverMD5[0], "downloaded file md5 does not match md5 calculated on server");
                context.assertEquals(uploadedMD5, serverMD5[0], "uploaded file md5 does not match md5 calculated on server");
                context.assertTrue(Arrays.equals(downloaded, bytes), "uploaded and download file content differs!");
                downloadAsync.complete();
            });
        }).end();
    }
}
