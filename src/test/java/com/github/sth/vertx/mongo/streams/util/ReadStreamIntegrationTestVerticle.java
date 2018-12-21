package com.github.sth.vertx.mongo.streams.util;

import com.github.sth.vertx.mongo.streams.GridFSInputStream;
import com.github.sth.vertx.mongo.streams.GridFSOutputStream;
import com.github.sth.vertx.mongo.streams.GridFSReadStream;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.gridfs.GridFSBucket;
import com.mongodb.async.client.gridfs.GridFSBuckets;
import com.mongodb.async.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple verticle, which accepts multipart file upload and serves the uploaded files.
 */
public class ReadStreamIntegrationTestVerticle extends AbstractVerticle {

    private HttpServer httpServer;
    private MongoClient mongoClient;

    public final static String DB_NAME = "vertx-mongo-streams-integration-test";

    private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");

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
                    GridFSInputStream gridFSInputStream = GridFSInputStream.create(vertx);

                    // when the upload has finished, notify the GridFSInputStream
                    fileUpload.endHandler((Void aVoid) -> {
                        gridFSInputStream.end();
                                System.out.println("end handler closed: " + request.isEnded());
                    });

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
                                            .put("md5", file.getMD5());
                                    request.response().end(json.toString());
                                }
                            });
                        }
                    });
                });

            } else if (HttpMethod.GET.equals(request.method())) {

                ObjectId objectId = new ObjectId(request.uri().substring(1));

                String range = request.getHeader("range");
                Long offset = null;
                Long end = null;

                HttpServerResponse response = request.response();

                if (range != null) {
                    Matcher m = RANGE.matcher(range);
                    if (m.matches()) {
                        try {
                            String part = m.group(1);
                            // offset cannot be empty
                            offset = Long.parseLong(part);
                            // offset must fall inside the limits of the file
                            if (offset < 0) {
                                throw new IndexOutOfBoundsException();
                            }
                            // length can be empty
                            part = m.group(2);
                            if (part != null && part.length() > 0) {
                                // ranges are inclusive
                                end = Long.parseLong(part);
                                // end offset must not be smaller than start offset
                                if (end < offset) {
                                    throw new IndexOutOfBoundsException();
                                }
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            response.setStatusCode(416).end();
                            return;
                        }
                    }
                }

                response.setChunked(true);

                Long finalOffset = offset;
                Long finalEnd = end;

                gridFSBucket.find(Filters.eq("_id", objectId)).first((GridFSFile file, Throwable t2) -> {
                    if (t2 != null) {
                        t2.printStackTrace();
                        request.response().setStatusCode(500).end();
                    } else {

                        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(objectId);

                        skip(vertx, downloadStream, finalOffset).setHandler((AsyncResult<Void> result) -> {

                            GridFSReadStream readStream;
                            if (finalEnd != null) {
                                readStream = GridFSReadStream.create(vertx, downloadStream, finalEnd - finalOffset, 256);
                            } else {
                                readStream = GridFSReadStream.create(vertx, downloadStream, 256);
                            }
                            Pump.pump(readStream, response).start();

                            readStream.endHandler((aVoid) -> {
                                response.setStatusCode(200);
                                response.end();
                            });
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

    static Future<Void> skip(Vertx vertx, GridFSDownloadStream stream, Long bytes) {
        if (bytes == null) {
            return Future.succeededFuture();
        }
        Future<Void> future = Future.future();
        Context context = vertx.getOrCreateContext();
        stream.skip(bytes, (Long skipped, Throwable t) -> {

            context.runOnContext((aVoid) -> {
                if (t != null) {
                    future.fail(t);
                } else {
                    future.complete();
                }
            });
        });
        return future;
    }
}
