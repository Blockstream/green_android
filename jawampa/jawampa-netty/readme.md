jawampa-netty
=============

This repository contains a netty-based connection provider for the jawampa WAMP
protocol library.

Install
-------

Declare the following dependency for the jawampa-netty connection provider:

    <dependency>
        <groupId>ws.wamp.jawampa</groupId>
        <artifactId>jawampa-netty</artifactId>
        <version>0.4.1</version>
    </dependency>

As this will automatically add a dependency on `ws.wamp.jawampa.core` is it
sufficient to only add this single dependency.


Client configuration
--------------------

To create WAMP clients that use the provider an instance of the
`NettyWampClientConnectorProvider` must be created and used in the
`WampClientBuilder`.

Additional netty-specific configuration arguments can be used by creating an
instance of the `NettyWampConnectionConfig` class, configuring it and assigning
it to the `WampClientBuilder`. This step is optional, default-configured
clients can also be created without advanced configuration options. However
e.g. proper SSL configuration will require it.


**Example:**

~~~~java
IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
NettyWampConnectionConfig connectionConfiguration = new NettyWampConnectionConfig();
// ... set connectionConfiguration properties

final WampClient client;
try {
    // Create a builder and configure the client
    WampClientBuilder builder = new WampClientBuilder();
    builder.withConnectorProvider(connectorProvider)
           .withConnectionConfiguration(connectionConfiguration)
           .withUri("ws://localhost:8080/wamprouter")
           .withRealm("examplerealm")
           .withInfiniteReconnects()
           .withReconnectInterval(5, TimeUnit.SECONDS);
    // Create a client through the builder. This will not immediatly start
    // a connection attempt
    client = builder.build();
} catch (WampError e) {
    // Catch exceptions that will be thrown in case of invalid configuration
    System.out.println(e);
    return;
}
~~~~


Router configuration
--------------------

In order to allow the router to listen on a port, accept incoming connections
one or more Netty servers have to be started which use the router as their final
request handler.

### Netty HTTP / WebSocket server integration

In order to connect a `WampRouter` to a WebSocket server **jawampa** provides a
custom Netty `ChannelHandler` that performs this job:
The `WampServerWebsocketHandler`  
This handler can be integrated into the users HTTP pipeline and allows to expose
the WAMP router functionality under a chosen path like `/wamp`.
The handler has to be inserted above the basic HTTP handlers like
`HttpServerCodec` and `HttpObjectAggregator` (because it
expects to read an HTTP upgrade request) but below the users handlers which
might service HTTP requests to other pathes or provide other websocket on other
pathes.  
If the `WampServerWebsocketHandler` encounters a websocket upgrade request that
targets the configured WAMP router path it will restructure the pipeline:  
All handlers including and above the `WampServerWebsocketHandler` will be
removed and other handlers that are required to provide WAMP functionality will
be inserted instead. These will transform WebSocket frames into WAMP messages
and forward them to the `WampRouter`.

The pipeline transformation will look like:

~~~~

 +---------------------------+            +---------------------------+
 |     User HTTPhandler      |            |        WAMP Router        |
 +------------++-------------+            +-------------++------------+
              ||                                        ||
 +------------++-------------+            +-------------++------------+
 | WampServerWebsocketHandler|            |     WAMP Deserializer     |
 +------------++-------------+            +-------------++------------+
              ||                                        ||
 +------------++-------------+            +-------------++------------+
 | WampServerWebsocketHandler|  ------->  |     WAMP Serializer       |
 +------------++-------------+            +-------------++------------+
              ||                                        ||
 +------------++-------------+            +-------------++------------+
 |   HttpObjectAggregator    |            |     WebSocket Decoder     |
 +------------++-------------+            +-------------++------------+
              ||                                        ||
 +------------++-------------+            +-------------++------------+
 |     HttpServerCodec       |            |     WebSocket Encoder     |
 +---------------------------+            +---------------------------+

~~~~

For use cases where the user does only want to quickly startup a WAMP router
without the need for exposing other HTTP and websocket functionality in parallel
a simply default implementation for a Netty websocket server is included, which
implements a standard HTTP pipeline that exposes the WAMP router on one path
and provides only a simply index.html page in parallel.
This implementation is available through the `SimpleWampWebsocketListener` class
.

With this class a WAMP router that serves on all IPv4 interfaces on port 8080
on path and provides WAMP routing functionality on path `/wamp` can be started
as follows:

~~~~java
WampRouter router = new WampRouterBuilder().addRealm("realm1").build();
SimpleWampWebsocketListener server =
  new SimpleWampWebsocketListener(router, URI.create("ws://0.0.0.0:8080/wamp"), null);
server.start();
~~~~

To shut down the server both the HTTP server/listener as well as the the WAMP
router must be shut down:
~~~~java
server.stop();
router.close().toBlocking().last(); // Blocks until router is fully shut down
~~~~