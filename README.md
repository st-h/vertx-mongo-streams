# GridFS stream helper for Vert.x 3.x and MongoDB async driver in Java and Groovy

A stream helper to Pump data from a vert.x ReadStream (e.g. HttpServerFileUpload) to MongoDB AsyncInputStream.

Pull Requests are welcome.

---

## Prerequisites

- mongodb async driver >= 3.3.0
- vert.x >= 3.0.0

## Install

mvn:
```
 <groupId>com.github.sth</groupId>
 <artifactId>vertx-mongo-streams</artifactId>
 <version>1.0.0</version>
```
 
gradle:
```
com.github.sth:vertx-mongo-streams:1.0.0
```

## Usage

Just create a new instance using the `create()` method and use a `Pump` to transfer the data. Call the `end()` method when all data has been made available. 
The internal queue size can be changed using the `setWriteQueueMaxSize()` method.

### java
the java implementation is found in package `com.github.sth.vertx.mongo.streams`

### groovy
the groovy implementation is found in package `com.github.sth.groovy.vertx.mongo.streams`

```groovy
httpServerRequest.uploadHandler({ HttpServerFileUpload fileUpload ->
  
  GridFSInputStream fileInputStream = GridFSInputStream.create()
  
  fileUpload.endHandler {
    fileInputStream.end()
  }
  
  Pump.pump(fileUpload, fileInputStream).start()
  gridFSBucket.uploadFromStream("filename", fileInputStream, { ObjectId id, Throwable t ->
    ...
  } as SingleResultCallback<ObjectId>)
})
```

---

