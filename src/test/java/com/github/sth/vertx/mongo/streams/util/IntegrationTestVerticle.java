package com.github.sth.vertx.mongo.streams.util;

import com.github.sth.vertx.mongo.streams.GridFSInputStream;
import com.github.sth.vertx.mongo.streams.GridFSOutputStream;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.gridfs.GridFSBucket;
import com.mongodb.async.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import org.bson.types.ObjectId;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple verticle, which accepts multipart file upload and serves the uploaded files.
 */
public class IntegrationTestVerticle extends AbstractVerticle {

    private HttpServer httpServer;
    private MongoClient mongoClient;

    public final static String DB_NAME = "vertx-mongo-streams-integration-test";

    private String md5(byte[] bytes) throws RuntimeException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            return DatatypeConverter.printHexBinary(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Future<Void> future) {

        // setup GridFS
        mongoClient = MongoClients.create();

        MongoDatabase db = mongoClient.getDatabase(DB_NAME);
        GridFSBucket gridFSBucket = GridFSBuckets.create(db, "test-bucket'");

        // setup the HttpServer
        httpServer = vertx.createHttpServer().requestHandler((HttpServerRequest request) -> {

            if (HttpMethod.POST.equals(request.method())) {

                request.setExpectMultipart(true);

                request.uploadHandler((HttpServerFileUpload fileUpload) -> {

                    // create a GridFSInputStream
                    GridFSInputStream gridFSInputStream = GridFSInputStream.create();

                    // when the upload has finished, notify the GridFSInputStream
                    fileUpload.endHandler((Void aVoid) -> gridFSInputStream.end());

                    // just use a Pump to stream all the data
                    Pump.pump(fileUpload, gridFSInputStream).start();

                    gridFSBucket.uploadFromStream(fileUpload.filename(), gridFSInputStream, (ObjectId id, Throwable t) -> {
                        if (t != null) {
                            t.printStackTrace();
                            request.response().setStatusCode(500).end();
                        } else {
                            gridFSBucket.find(Filters.eq("_id", id)).first((GridFSFile file, Throwable t2) -> {
                                if (t2 != null) {
                                    t2.printStackTrace();
                                    request.response().setStatusCode(500).end();
                                } else {
                                    JsonObject json = new JsonObject()
                                            .put("id", id.toString())
                                            .put("vertxMd5", md5(gridFSInputStream.getInBytes()))
                                            .put("md5", file.getMD5());
                                    request.response().end(json.toString());
                                }
                            });
                        }
                    });
                });

            } else if (HttpMethod.GET.equals(request.method())) {

                ObjectId objectId = new ObjectId(request.uri().substring(1));

                HttpServerResponse response = request.response();
                response.setChunked(true);

                gridFSBucket.find(Filters.eq("_id", objectId)).first((GridFSFile file, Throwable t2) -> {
                    if (t2 != null) {
                        t2.printStackTrace();
                        request.response().setStatusCode(500).end();
                    } else {
                        System.out.println("server: download gridFS file md5:\t" + file.getMD5());
                        GridFSOutputStream outputStream = GridFSOutputStream.create(response);
                        gridFSBucket.downloadToStream(objectId, outputStream, (Long bytesRead, Throwable t) -> {
                            response.setStatusCode(200);
                            response.end();
                        });
                    }
                });

            }

        }).listen(vertx.getOrCreateContext().config().getInteger("http.port"), (AsyncResult<HttpServer> result) -> {
            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        mongoClient.close();
        httpServer.close((AsyncResult<Void> result) -> future.complete());
    }
}
