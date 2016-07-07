jawampa
=======

- is a library that brings support for the **Web Application Messaging
  Protocol [[WAMP](http://wamp.ws/)]** to Java.  
- provides WAMPv2 client side functionality as well as server side
  functionality and supports all currently defined WAMPv2 roles
  (caller, callee, publisher, subscriber, broker, dealer).
- provides a pluggable transport layer. Connection providers and servers
  which use different networking mechanisms and low-level libraries can be
  built and plugged into jawampa.
- exposes the client-side user-interface through
  [**RxJava**](https://github.com/ReactiveX/RxJava) Observables, which enable
  powerful compositions of different asynchronous operations and provide an
  easy solution for delegating data handling between different threads.  
  Observables are also used in places where only single return values are
  expected and Futures would be sufficient - However the common use of
  Observables provides less dependencies and allows to schedule continuations
  for all kinds of asynchronous operations in a consistent way.
- is compatible with Java6. However the examples in this document use Java8
  syntax for convenience.


Install
-------

Declare the following dependency for the base library:

    <dependency>
        <groupId>ws.wamp.jawampa</groupId>
        <artifactId>jawampa-core</artifactId>
        <version>0.4.1</version>
    </dependency>

However as the core library of jawampa does not provide a transport layer users
should typically use a jawampa transport provider library (e.g.
`jawampa-netty` - see subdirectory) as a depency.  
This will automatically also add a dependency on `jawampa-core`.


WAMP client API (`WampClient`)
------------------------------

The client-side API is exposed through the `WampClient` object.  
`WampClient`s must be created through `WampClientBuilder` objects, which allow
to configure the created clients.

There are 3 mandatory parameters that have to be set through the builder:
- A connector provider which describes the framework which will be used for
  establishing a connection to the WAMP router. An example is the
  `NettyWampClientConnectorProvider` which is described in the documentation
  of the jawampa-netty subproject.
- The URI that describes the address of the WAMP router
- The realm that the client should join on the router

Additionally there exist some optional parameters, which for example allow to
activate automatic reconnects between the client and the router or allow to
configure how the client should behave in case of communication errors.


**Example:**

~~~~java
final WampClient client;
try {
    // Create a builder and configure the client
    WampClientBuilder builder = new WampClientBuilder();
    builder.withConnectorProvider(connectorProvider)
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

The `WampClient` object provides the RxJava `Observable` `statusChanged()` that
notifies the user about the current status of the session between the client
and the router, which can be either `DisconnectedState`, `ConnectingState` or
`ConnectedState`. The application can monitor this `Observable` to detect when
other steps should be performed (e.g. subscribe to topics or register functions
after connect).

The `onNext()` status notification method of the Subscriber will be called on
the `WampClient`s thread. However it can easily be delegated to a Scheduler or
EventLoop of the host application be using the `Observable.observerOn()`
member function.

`statusChanged()` returns a `BehaviorObservable`, therefore it will immediatly
send a notification about the current state to subscribers on subscribe and not
only in case of state changes.

**Example:**

~~~~java

client.statusChanged()
      .observeOn(applicationScheduler)
      .subscribe((WampClient.State newState) -> {
        if (newState instanceof WampClient.ConnectedState) {
          // Client got connected to the remote router
          // and the session was established
        } else if (newState instanceof WampClient.DisconnectedState) {
          // Client got disconnected from the remoute router
          // or the last possible connect attempt failed
        } else if (newState instanceof WampClient.ConnectingState) {
          // Client starts connecting to the remote router
        }});
~~~~

In order to start the connection between a client and a router the clients
`open()` member function has to be called. This will lead to the first
connection attempt and a state change from `DisconnectedState` to
`ConnectingState`.

When the client is no longer needed is **must** be closed with the `close()`
member function. This will shutdown the connection to the remote router and stop
all reconnect attempts. After a `WampClient` was closed it can not be reopened
again. Instead of this a new instance of the `WampClient` should be created if
necessary.

The close process is also asynchronous. Therefore a call to `close()` does not
guarantee an immediate close of the client. However the `close()` call returns
an `Observable` which can be used to wait until the client was successfully
closed. 

**Example for a typical session lifecycle:**

~~~~java
WampClient client = builder.build();
client.statusChanged().subscribe(...);
client.open();

// ...
// use the client here
// ...

// Wait synchronously for the client to close
// On environments like UI thread asynchronous waiting should be preferred
client.close().toBlocking().last();
~~~~

### Performing procedure procedure calls

Remote procedure calls can be performed through the various `call` member
functions of the `WampClient`.

All overloaded version of `call` require the
name of the procedure that should be called (and which must be a valid WAMP Uri)
as the first parameter. All versions of `call()` return an `Observable` which
is used to transfer the result of the function call in an asynchronous fashion
to the caller. It is a hot observable, which means the call will be made
indepently of whether someone subscribes to it or not. However the result will
be cached in the Observable, which means that also late subscribers will be able
to retrieve the result.

- If the procedure call succeeds the subscribers `onNext` method will be called
  with the result and followed by an `onCompleted` call.  
- If the remote procedure call fails then the subscribers `onError()` method will
  be called with the occurred error as a parameter.

The different overloads of `call()` allow to provide the arguments to the
procedure in different fashions as well as to retrieve the return value in a
different fashion:

The most explicit signature of call is  
`Observable<Reply> call(String procedure, ArrayNode arguments, ObjectNode
argumentsKw)`  
It allows to pass positional arguments as well as keyword arguments to the
WAMP procedure and will return a structure which will as well contains fields
for the positional and keyword arguments of the call result.  
The arguments and return values use the `ArrayNode` and `ObjectNode` data types
from the Jackson JSON library which describe an array or object of arbitrary
other types.

If only positional arguments are required for the call the simplified variant  
`Observable<Reply> call(String procedure, Object... args)`  
can be used which
allows to pass the positional arguments as a varargs array. It will also
automatically use Jacksons object mapping capabilities to convert all Java POJO
keyword arguments in their `JsonNode` form and create an argument array from
that. This means you can directly use any kind of Java objects as function
parameters as long as they can be properly serialized and deserialized by
Jackson. For more complex data structures you might need to use annotations to
instruct the serializer.

The last variant of `call()` provides some further convenience and has the
following signature: `<T> Observable<T> call(String procedure,  Class<T>
returnValueClass, Object... args)`.  
It can be used when the procedure provides none or a single positional return
value. Then you can specify the type of the expected return value in the second
arguments and `call` will automatically try to map the first result argument
of the procedure call into the required type. This will also be done through
Jackson object mapping.

With this simplification you can call remote procedures and listen for return
values in the following way (with Java8):

~~~~java
Observable<String> result = client.call("myservice.concat", String.class, "Hello nr ", 123);
// Subscribe for the result
// onNext will be called in case of success with a String value
// onError will be called in case of errors with a Throwable
result.observeOn(applicationScheduler)
      .subscribe((txt) -> System.out.println(txt),
                 (err) -> System.err.println(err));

~~~~


### Providing remote procedures to other clients

With **WAMP** all clients that are connected to a router are able to provide
procedures that can be used by any other client.

**jawampa** exposes this functionality though the `registerProcedure()` member
function. `registerProcedure` will return an `Observable` which will be used to
receive incoming function calls. Each incoming request to the registered
procedure name will be pushed to the Subscribers `onNext` method in form of a
`Request` class. The application can retrieve and process requests on any thread
through `observeOn` and can send responses to the request with the `Request`
classes member functions. The procedure will only be registered at the router
after `subscribe` was called and will be unregistered at the router if the
subscription was unsubscribed.

If an error occurs during the registration of the procedure at the router the
Subscribers `onError` method will be called to notify about the error. If the
session disconnects (or is disconnected) then the subscription will simply be
completed with `onCompleted`.

**Example for providing a procedure which echos the first integer argument:**

~~~~java
// Provide a procedure
Observable proc = client.registerProcedure("echo.first").subscribe(
    request -> {
        if (request.arguments() == null || request.arguments().size() != 1
         || !request.arguments().get(0).canConvertToLong())
        {
            try {
                request.replyError(new ApplicationError(ApplicationError.INVALID_PARAMETER));
            } catch (ApplicationError e) { }
        } else {
            long a = request.arguments().get(0).asLong();
            request.reply(a);
        }
    },
    e -> System.err.println(e));

// Unregister the procedure
proc.unsubscribe();


~~~~

### Publishing events

The `publish()` function of the `WampClient` can be used to publish an event
with a topic (that must be a valid WAMP Uri) towards the router and thereby to
all other connected WAMP clients.

Similar to `call()` the `publish()` function provides various overloads that
allow to use differnt formats for passing the event arguments.

The `publish()` function returns an `Observable<Long>`. Just like the Observable
returned from `call()` this is also a hot observable and does not need to be
subscribed to perform the publishing. Subscribers that subscribe on the
Observable will either receive a single `onNext()` call which delivers the
`publicationId` or an `onError()` call when the publishing fails. This can for
example be the case when there is no connection to the router.

**Example for publishing an event:**

~~~~java
client.publish("example.event", "Hello ", "from nr ", 28)
      .subscribe(
        publicationId -> { /* Event was published */ },
        err -> { /* Error during publication */});

~~~~


### Subscribing to events

To subscribe on events that are published from other clients on the router
the `makeSubscription()` function of the `WampClient` can be used.
`makeSubscription` will require the topic which the client is interested in and
will return an Observable that can be used to perform the subscription. The
subscription on the topic at the router will only happen after `subscribe()` was
called on the Observable. After that, for each received event the `onNext()`
function of the Subscriber will be called to deliver the event in form of a
`PubSubData` struct that contains the positional as well as the keyword
arguments. If the subscription can not be performed at the router because of
an error `onError()` will be called to deliver that error. If the connection is
closed or get's closed the subscription will be completed with `onCompleted()`.

*Hint:* For most applications it makes sense to perform subscriptions after they
got connected to the router in the `statusChanged()` handler.

There exists also an overload of the `makeSubscription` function which can be
used in the case that the client is only interested in the first positional
argument of the event. It allows to specify the class type of the event data and
will automatically try to transform the received event into this data type
through Jacksons object mapping capabilities. If the client is interested in no
parameter `Void.class` can be used get an `Observable<Void>` which will notifiy
the subscriber when an event without parameters received. If the received event
data can not be converted into the desired format the subscription will be
cancelled and an error will be delivered through the `onError()` function.


**Example for subscribing to an event:**

~~~~java
// Subscribe to an event
Observable<String> eventSubscription =
client.makeSubscription("example.event", String.class)
      .subscribe((s) -> { /* String event received */ },
                 (e) -> { /* Error during subscription or object mapping */ });

// Unsubscribe from the event
eventSubscription.unsubscribe();
~~~~


WAMP server API (`WampRouter`)
------------------------------

**jawampa** provides a WAMP router that can be bundled into your application in
order to avoid installing, configuring and running an external router.
By instantiating a `WampClient` that provides an API as well as a `WampRouter`
in an application a classical server architecture can be mimiced, where the
server listens for connections as well as provides an API.

The `WampRouter` class implements the whole routing and realm logic that is
described in the WAMPv2 basic profile. It can be only be created through the
`WampRouterBuilder` class, which allows to configure a router before
instantiating it. In the current version of **jawampa** the realms that the
router shall expose have to be configured through it.

**Example for configuring and instantiating a router:**

~~~~java

WampRouterBuilder routerBuilder = new WampRouterBuilder();
WampRouter router;
try {
    routerBuilder.addRealm("realm1");
    routerBuilder.addRealm("realm2");
    router = routerBuilder.build();
} catch (ApplicationError e) {
    e.printStackTrace();
    return;
}

~~~~

The router will be directly up-and-running after it was built. However it won't
listen to any connections yet and therefore won't do anything up to this point.

In order for the router to work servers must be set up that accept connections,
register them at the `WampRouter` and then push messages towards it.

The `WampRouter` provides an implementation of the `IWampConnectionAcceptor`
interface which can be used to register a new connection at the client. It can
be queried through the `WampRouter.connectionAcceptor()` getter.  

Registering a new connection at the router is a 2-stage process:

- At first the connection provider must query an instance of a
  `IWampConnectionListener` from the router by calling 
  `connectionAcceptor.createNewConnectionListener();`. This will be the
  interface to which the new connection should push messages after it was
  fully established and registered.  
  The returned interface does not yet occupy any non-garbage-collectible
  resources in the router. Therefore it is not harmful to ignore the return
  value if the connection provider determines that the connection can not be
  properly established.
- In a second step the new connection must be registered at the router by
  calling
  `connectionAcceptor.acceptNewConnection(connection, connectionListener);`
  and thereby providing the sending interface of type `IWampConnection` to the
  router.

The new connection may only send message to the listener once both steps have
been finished. Sending messages earlier causes undefined behavior.

The `WampRouter` will use the provided `IWampConnection` interface in order to
send messages through connections. The connection must guarantee the following
contract to the router:

- The router must be able to call methods on the interface as long as it has
  not called `close(...)` on it.  
  If the connection is already closed or in an errorenous state implementations
  of the interface should answer `sendMessage(...)` calls by rejecting the
  provided promise.
- The router will always call `close(...)` on the interface, even if the
  connection was closed by the remote side before.
- The connection must guarantee that it calls no method on the retrieved
  `IWampConnectionListener` interface after it has acknowledged the close call
  by fulfilling the provided future. The router will take the acknowledgement
  of the `close(...)` call as a sign that all resources owned by the connection
  have been released.


An example implementation of a server that pushes messages towards the router
which is based on the Netty framework can be found in the jawampa-netty
subproject.


### Closing a router

To close a router the `close()` member function has to be called:

~~~~java
router.close.toBlocking().last();
~~~~

This will gracefully close all WAMP sessions established between the router and
clients and will also close the underlying transport channels. If new
connections are made to the router after `close()` it will reject those by
closing them.

Just like the `close()` call on the `WampClient` closing a `WampRouter` is also
an asnychronous process and the the call will return an `Observable` that
signals when the router is fully shutdown.

In order to allow the router to listen on a port, accept incoming connections
one or more servers have to be started which use the router as their final
request handler.



Restrictions
------------

**jawampa** is very young and in a work-in-progress state.  
Therefore the following restrictions apply:

- **jawampa** does not properly support the transmission of binary values as
  required in the WAMP specification. **jawampa** will use Jackson to transform
  data from binary to JSON which will use a base64 encoding, but will not
  prepend the data with a leading 0 byte.
- **jawampa** only supports the **WAMPv2** basic profile and some selected parts
  of the advanced profile. Many advanced profile features are not implemented.
- **jawampa** only supports websocket connections between WAMP clients and
  routers.
- The roles of the client and router are properly transmitted but not taken into
  account for all other actions. E.g. it won't be verified whether a remote peer
  actually provides the needed functionality or not. The assumption is that all
  peers implement all of the roles that apply for them.
