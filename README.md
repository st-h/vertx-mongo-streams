[![Build Status](https://travis-ci.org/st-h/vertx-mongo-streams.svg?branch=master)](https://travis-ci.org/st-h/vertx-mongo-streams)
[![codecov](https://codecov.io/gh/st-h/vertx-mongo-streams/branch/master/graph/badge.svg)](https://codecov.io/gh/st-h/vertx-mongo-streams)
[![BCH compliance](https://bettercodehub.com/edge/badge/st-h/vertx-mongo-streams?branch=master)](https://bettercodehub.com/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.st-h/vertx-mongo-streams/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.st-h/vertx-mongo-streams)
# GridFS stream helper for Vert.x 3.x and MongoDB async driver in Java and Groovy
Helpers to stream data between vert.x ReadStream (e.g. HttpServerFileUpload), WriteStream (e.g. HttpServerResponse) and MongoDB AsyncInputStream and AsyncOutputStream.

Pull Requests are welcome.

---

# Prerequisites
- Java 8
- mongodb async driver >= 3.3.0
- vert.x >= 3.4.0

# Install
mvn:
```
 <groupId>com.github.st-h</groupId>
 <artifactId>vertx-mongo-streams</artifactId>
 <version>1.3.0</version>
```
 
gradle:
```
com.github.st-h:vertx-mongo-streams:1.3.0
```

# Usage

Since vert.x 3.4.0 usage within java and groovy is identical.

### Upload
The GridFSInputStream allows to directly Pump data from a vert.x ReadStream (e.g. HttpServerFileUpload) to MongoDB AsyncInputStream.

Just create a new instance using the `GridFSInputStream.create()` method and use a `Pump` to transfer the data. Call the `end()` method when all data has been made available. The internal queue size can be changed using the `setWriteQueueMaxSize()` method.

This snippet creates a fully working http server that persists a file to GridFS: 

```java
import com.github.sth.vertx.mongo.streams.GridFSInputStream;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.gridfs.GridFSBucket;
import com.mongodb.async.client.gridfs.GridFSBuckets;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.streams.Pump;

public class UploadVerticle extends AbstractVerticle {

  private HttpServer httpServer;
  
  @Override
  public void start(Future fut) {

    // setup GridFS
    MongoDatabase db = MongoClients.create().getDatabase("test");
    GridFSBucket gridFSBucket = GridFSBuckets.create(db, "test-bucket");

    // setup the HttpServer
    httpServer = vertx.createHttpServer().requestHandler(request -> {

      request.setExpectMultipart(true);

      request.uploadHandler(fileUpload -> {

        // create a GridFSInputStream
        GridFSInputStream gridFSInputStream = GridFSInputStream.create();

        // when the upload has finished, notify the GridFSInputStream
        fileUpload.endHandler(endHandler -> gridFSInputStream.end());

        // just use a Pump to stream all the data
        Pump.pump(fileUpload, gridFSInputStream).start();

        gridFSBucket.uploadFromStream(fileUpload.filename(), gridFSInputStream, (id, t) -> {
          if (t != null) {
            // failed to persist
            request.response().setStatusCode(500).end();
          } else {
            // sucessfully persisted with ID: id
            request.response().end("uploaded: " + id);
          }
        });
      });
    }).listen(8080, res -> {
        if (res.succeeded()) {
          fut.complete();
        } else {
          fut.fail(res.cause());
        }
    });

  }

  @Override
  public void stop(Future fut) {
    httpServer.close( res -> fut.complete());
  }
}
```

### Download
The GridFSOutputStream allows write to a vert.x WriteStream via the mongo drivers downloadToStream() method:

```java
GridFSOutputStream outputStream = GridFSOutputStream.create(httpServerResponse)
gridFS.downloadToStream(objectId, outputStream, (bytesRead, t) -> {

    ...

})
```

# Build
## Integration Test

If you want to build this library yourself, the integration test requires a running mongodb on default port 27017. If you are using docker, you could just use the commands from `.travis.yml` to launch a docker container running a mongo instance and expose the default port:
```
docker pull mongo
docker run -d -p 127.0.0.1:27017:27017 mongo
```

# Acknowledgments

Thanks to [antofar](https://github.com/antofar) for contributing an improved implementation of GridFSInputStream

---

