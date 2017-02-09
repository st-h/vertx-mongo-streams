package com.github.sth.groovy.vertx.mongo.streams

import com.github.sth.groovy.vertx.mongo.streams.util.ByteUtil
import com.github.sth.groovy.vertx.mongo.streams.util.IntegrationTestVerticle
import io.vertx.core.AsyncResult
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.HttpClientRequest
import io.vertx.groovy.core.http.HttpClientResponse
import io.vertx.groovy.ext.unit.Async
import io.vertx.groovy.ext.unit.TestContext
import io.vertx.groovy.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner.class)
class GridFSInOutStreamIT {

    Vertx vertx;
    int port;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(0)
        port = socket.getLocalPort()
        socket.close()

        Async async = context.async()

        vertx.deployVerticle('groovy:' + IntegrationTestVerticle.class.getName(), ['http.port': port], { AsyncResult result ->

            if (result.failed()) { result.cause().printStackTrace() }

            context.assertTrue(result.succeeded())
            async.complete()
        })

        async.awaitSuccess(10000)
    }

    @After
    public void tearDown(TestContext context) {
        Async async = context.async()
        vertx.close({ AsyncResult result ->
            async.complete()
        });
        async.awaitSuccess(10000)
    }

    @Test
    public void testUploadAndDownload(TestContext context) {
        HttpClient client = vertx.createHttpClient();
        Async async = context.async();
        byte[] bytes = ByteUtil.randomBytes(1024 * 1024 * 8)
        String id = null

        HttpClientRequest request = client.post(8080, 'localhost', '/', { HttpClientResponse response ->

            response.bodyHandler({ Buffer body ->

                id = body.toString()
                context.assertNotNull(body)
                async.complete()
            })
        }).setChunked(true)

        request.headers().add('content-type', 'multipart/form-data; boundary=MyBoundary')

        Buffer buffer = Buffer.buffer();
        buffer.appendString("--MyBoundary\r\n");
        buffer.appendString("Content-Disposition: form-data; name=\"test\"; filename=\"test.jpg\"\r\n");
        buffer.appendString("Content-Type: application/octet-stream\r\n");
        buffer.appendString("Content-Transfer-Encoding: binary\r\n");
        buffer.appendString("\r\n");

        buffer.delegate.appendBytes(bytes);
        buffer.appendString("\r\n");

        buffer.appendString("--MyBoundary--\r\n");

        request.end(buffer)

        async.awaitSuccess()

        async = context.async()

        client.get(8080, 'localhost', '/' +  id, { HttpClientResponse response ->
            response.bodyHandler({ Buffer body ->
                context.assertTrue(Arrays.equals(((io.vertx.core.buffer.Buffer) body.delegate).bytes, bytes))
                async.complete()
            })
        }).end()
    }
}
