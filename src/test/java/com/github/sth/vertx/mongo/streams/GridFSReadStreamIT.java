package com.github.sth.vertx.mongo.streams;

import com.github.sth.vertx.mongo.streams.util.ByteUtil;
import com.github.sth.vertx.mongo.streams.util.IntegrationTestVerticle;
import com.github.sth.vertx.mongo.streams.util.ReadStreamIntegrationTestVerticle;
import com.github.sth.vertx.mongo.streams.util.UploadHelper;
import com.mongodb.async.client.MongoClients;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class GridFSReadStreamIT {

    Vertx vertx;
    int port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
        vertx.deployVerticle(ReadStreamIntegrationTestVerticle.class.getName(), options, context.asyncAssertSuccess());
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
        UploadHelper.uploadDownload(vertx, port, context, bytes);
    }

    @Test
    public void testUploadAndGetRange(TestContext context) {
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024 );
        UploadHelper.uploadDownload(vertx, port, context, bytes, 711L, 4211L);
    }

    @Test
    public void testUploadAndGetRangeOffset(TestContext context) {
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024 );
        UploadHelper.uploadDownload(vertx, port, context, bytes, 2477L, null);
    }

    @Test
    public void testUploadAndDownloadLarge(TestContext context) {
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024 * 21);
        UploadHelper.uploadDownload(vertx, port, context, bytes);
    }
}
