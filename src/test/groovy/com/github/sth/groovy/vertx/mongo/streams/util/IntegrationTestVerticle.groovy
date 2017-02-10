package com.github.sth.groovy.vertx.mongo.streams.util

import com.github.sth.groovy.vertx.mongo.streams.GridFSInputStream
import com.github.sth.groovy.vertx.mongo.streams.GridFSOutputStream
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoClient
import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoDatabase
import com.mongodb.async.client.gridfs.GridFSBucket
import com.mongodb.async.client.gridfs.GridFSBuckets
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.core.http.HttpServerFileUpload
import io.vertx.groovy.core.http.HttpServerRequest
import io.vertx.groovy.core.http.HttpServerResponse
import io.vertx.groovy.core.streams.Pump
import io.vertx.groovy.core.streams.WriteStream
import io.vertx.lang.groovy.GroovyVerticle
import org.bson.types.ObjectId

/**
 * A simple verticle, which accepts multipart file upload and serves the uploaded files.
 */
public class IntegrationTestVerticle extends GroovyVerticle {

    private HttpServer httpServer
    private MongoClient mongoClient

    public final static String DB_NAME = 'vertx-mongo-streams-integration-test'

    @Override
    public void start(Future<Void> future) {

        // setup GridFS
        mongoClient = MongoClients.create()

        MongoDatabase db = mongoClient.getDatabase(DB_NAME)
        GridFSBucket gridFSBucket = GridFSBuckets.create(db, 'test-bucket')

        // setup the HttpServer
        httpServer = vertx.createHttpServer().requestHandler({ HttpServerRequest request ->

            if (HttpMethod.POST.equals(request.method())) {

                request.setExpectMultipart(true)

                request.uploadHandler({ HttpServerFileUpload fileUpload ->

                    // create a GridFSInputStream
                    GridFSInputStream gridFSInputStream = GridFSInputStream.create();

                    // when the upload has finished, notify the GridFSInputStream
                    fileUpload.endHandler { gridFSInputStream.end() }

                    // just use a Pump to stream all the data
                    Pump.pump(fileUpload, gridFSInputStream as WriteStream<Buffer>).start();

                    gridFSBucket.uploadFromStream(fileUpload.filename(), gridFSInputStream, { ObjectId id, Throwable t ->
                        if (t != null) {
                            request.response().setStatusCode(500).end()
                        } else {
                            request.response().end(id.toString());
                        }
                    } as SingleResultCallback<ObjectId>);
                })

            } else if (HttpMethod.GET.equals(request.method())) {

                ObjectId objectId = new ObjectId(request.uri().substring(1))

                HttpServerResponse response = request.response()
                response.setChunked(true)

                GridFSOutputStream outputStream = GridFSOutputStream.create(response)
                gridFSBucket.downloadToStream(objectId, outputStream, { Long bytesRead, Throwable t ->

                    response.setStatusCode(200)
                    response.end()

                } as SingleResultCallback<Long>)
            }

        }).listen((Integer) vertx.getOrCreateContext().config().get('port'), { AsyncResult<HttpServer> result ->
            if (result.succeeded()) {
                future.complete()
            } else {
                future.fail(result.cause())
            }
        })
    }

    @Override
    public void stop(Future<Void> future) {
        mongoClient.close()
        httpServer.close({
            AsyncResult<Void> result -> future.complete()
        } as Handler<AsyncResult<Void>>)
    }
}
