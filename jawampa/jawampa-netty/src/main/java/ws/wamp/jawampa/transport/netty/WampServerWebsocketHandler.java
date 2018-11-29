/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.transport.netty;

import ws.wamp.jawampa.WampRouter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;
import ws.wamp.jawampa.WampSerialization;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.connection.IWampConnection;
import ws.wamp.jawampa.connection.IWampConnectionAcceptor;
import ws.wamp.jawampa.connection.IWampConnectionListener;
import ws.wamp.jawampa.connection.IWampConnectionPromise;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * A websocket server adapter for WAMP that integrates into a Netty pipeline.
 */
public class WampServerWebsocketHandler extends ChannelInboundHandlerAdapter {
    
    final String websocketPath;
    final WampRouter router;
    final IWampConnectionAcceptor connectionAcceptor;
    final List<WampSerialization> supportedSerializations;
    
    WampSerialization serialization = WampSerialization.Invalid;
    boolean handshakeInProgress = false;

    public WampServerWebsocketHandler(String websocketPath, WampRouter router) {
        this(websocketPath, router, WampSerialization.defaultSerializations());
    }

    public WampServerWebsocketHandler(String websocketPath, WampRouter router,
                                      List<WampSerialization> supportedSerializations) {
        this.websocketPath = websocketPath;
        this.router = router;
        this.connectionAcceptor = router.connectionAcceptor();
        this.supportedSerializations = supportedSerializations;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (msg instanceof FullHttpRequest) ? (FullHttpRequest) msg : null;
        
        // Check for invalid http messages during handshake
        if (request != null && handshakeInProgress) {
            request.release();
            sendBadRequestAndClose(ctx, null);
            return;
        }
        
        // Transform this when we have an upgrade for our path,
        // otherwise pass the message
        if (request != null && isUpgradeRequest(request)) {
            try {
                tryWebsocketHandshake(ctx, (FullHttpRequest) msg);
            } finally {
                request.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    
    private boolean isUpgradeRequest(FullHttpRequest request) {
        if (!request.getDecoderResult().isSuccess()) {
            return false;
        }
        
        String connectionHeaderValue = request.headers().get(HttpHeaders.Names.CONNECTION);
        if (connectionHeaderValue == null) {
            return false;
        }
        String[] connectionHeaderFields = StringUtil.split(connectionHeaderValue.toLowerCase(), ',');
        boolean hasUpgradeField = false;
        for (String s : connectionHeaderFields) {
            if (s.trim().equals(HttpHeaders.Values.UPGRADE.toLowerCase())) {
                hasUpgradeField = true;
                break;
            }
        }
        if (!hasUpgradeField) {
            return false;
        }
        
        if (!request.headers().contains(HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET, true)){
            return false;
        }
        
        return request.getUri().equals(websocketPath);
    }
    
    // All methods inside the connection will be called from the WampRouters thread
    // This causes no problems on the ordering since they all will be called from
    // the same thread. And Netty is threadsafe
    static class WampServerConnection implements IWampConnection {
        
        final WampSerialization serialization; 
        ChannelHandlerContext ctx;
        
        public WampServerConnection(WampSerialization serialization) {
            this.serialization = serialization;
        }
        
        @Override
        public WampSerialization serialization() {
            return serialization;
        }
        
        @Override
        public boolean isSingleWriteOnly() {
            return false;
        }
        
        @Override
        public void sendMessage(WampMessage message, final IWampConnectionPromise<Void> promise) {
            ChannelFuture f = ctx.writeAndFlush(message);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess() || future.isCancelled())
                        promise.fulfill(null);
                    else
                        promise.reject(future.cause());
                }
            });
        }
        
        @Override
        public void close(boolean sendRemaining, final IWampConnectionPromise<Void> promise) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
            .addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    future.channel()
                        .close()
                        .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess() || future.isCancelled())
                                promise.fulfill(null);
                            else
                                promise.reject(future.cause());
                        }
                    });
                }
            });
        }
    }
    
    private void tryWebsocketHandshake(final ChannelHandlerContext ctx, FullHttpRequest request) {
        String wsLocation = getWebSocketLocation(ctx, request);
        String subProtocols = WampSerialization.makeWebsocketSubprotocolList(supportedSerializations);
        WebSocketServerHandshaker handshaker =
            new WebSocketServerHandshakerFactory(wsLocation,
                                                 subProtocols,
                                                 false,
                                                 WampHandlerConfiguration.MAX_WEBSOCKET_FRAME_SIZE)
                                                .newHandshaker(request);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshakeInProgress = true;
            // The next statement will throw if the handshake gets wrong. This will lead to an
            // exception in the channel which will close the channel (which is OK).
            final ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), request);
            String actualProtocol = handshaker.selectedSubprotocol();
            serialization = WampSerialization.fromString(actualProtocol);
            
            // In case of unsupported websocket subprotocols we close the connection.
            // Won't help us when the client will ignore our protocol response and send
            // invalid packets anyway
            if (serialization == WampSerialization.Invalid) {
                handshakeFuture.addListener(ChannelFutureListener.CLOSE);
                return;
            }
            
            // Remove all handlers after this one - we don't need them anymore since we switch to WAMP
            ChannelHandler last = ctx.pipeline().last();
            while (last != null && last != this) {
                ctx.pipeline().removeLast();
                last = ctx.pipeline().last();
            }
            
            if (last == null) {
                throw new IllegalStateException("Can't find the WAMP server handler in the pipeline");
            }
            
            // Remove the WampServerWebSocketHandler and replace it with the protocol handler
            // which processes pings and closes
            ProtocolHandler protocolHandler = new ProtocolHandler();
            ctx.pipeline().replace(this, "wamp-websocket-protocol-handler", protocolHandler);
            final ChannelHandlerContext protocolHandlerCtx = ctx.pipeline().context(protocolHandler);
            
            // Handle websocket fragmentation before the deserializer
            protocolHandlerCtx.pipeline().addLast(new WebSocketFrameAggregator(WampHandlerConfiguration.MAX_WEBSOCKET_FRAME_SIZE));
            
            // Install the serializer and deserializer
            protocolHandlerCtx.pipeline().addLast("wamp-serializer", 
                new WampSerializationHandler(serialization));
            protocolHandlerCtx.pipeline().addLast("wamp-deserializer", 
                new WampDeserializationHandler(serialization));
            
            // Retrieve a listener for this new connection
            final IWampConnectionListener connectionListener = connectionAcceptor.createNewConnectionListener();
            
            // Create a Wamp connection interface on top of that
            final WampServerConnection connection = new WampServerConnection(serialization);
            
            ChannelHandler routerHandler = new SimpleChannelInboundHandler<WampMessage> () {
                @Override
                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                    // Gets called once the channel gets added to the pipeline
                    connection.ctx = ctx;
                }
                 
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    connectionAcceptor.acceptNewConnection(connection, connectionListener);
                }
                 
                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    connectionListener.transportClosed();
                }
            
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, WampMessage msg) throws Exception {
                    connectionListener.messageReceived(msg);
                }
                 
                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    ctx.close();
                    connectionListener.transportError(cause);
                }
            };
            
            // Install the router in the pipeline
            protocolHandlerCtx.pipeline().addLast("wamp-router", routerHandler);
            
            handshakeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        // The handshake was not successful. 
                        // Close the channel without registering
                        ctx.fireExceptionCaught(future.cause()); // TODO: This is a race condition if the router did not yet accept the connection
                    } else {
                        // We successfully sent out the handshake
                        // Notify the router of that fact
                        ctx.fireChannelActive();
                    }
                }
            });
            
            // TODO: Maybe there are frames incoming before the handshakeFuture is resolved
            // This might lead to frames getting sent to the router before it is activated
        }
    }
    
    private String getWebSocketLocation(ChannelHandlerContext ctx, FullHttpRequest req) {
        String location = req.headers().get(HOST) + websocketPath;
        if (ctx.pipeline().get(SslHandler.class) != null) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            sendBadRequestAndClose(ctx, cause.getMessage());
        } else {
            ctx.close();
        }
    }
    
    private static void sendBadRequestAndClose(ChannelHandlerContext ctx, String message) {
        FullHttpResponse response;
        if (message != null) {
            response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, 
                                                   Unpooled.wrappedBuffer(message.getBytes()));
        } else {
            response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
        }
        ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    public static class ProtocolHandler extends ChannelInboundHandlerAdapter {
        
        enum ReadState {
            Closed,
            Reading,
            Error
        }
        
        ReadState readState = ReadState.Reading;
        
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
        }
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.fireChannelActive();
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            readState = ReadState.Closed;
            ctx.fireChannelInactive();
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Discard messages when we are not reading
            if (readState != ReadState.Reading) {
                ReferenceCountUtil.release(msg);
                return;
            }
            
            // We might receive http requests here when the whe clients sends something after the upgrade
            // request but we have not fully sent out the response and the http codec is still installed.
            // However that would be an error.
            if (msg instanceof FullHttpRequest) {
                ((FullHttpRequest) msg).release();
                WampServerWebsocketHandler.sendBadRequestAndClose(ctx, null);
                return;
            }
            
            if (msg instanceof PingWebSocketFrame) {
                // Respond to Pings with Pongs
                try {
                    ctx.writeAndFlush(new PongWebSocketFrame());
                } finally {
                    ((PingWebSocketFrame) msg).release();
                }
            } else if (msg instanceof CloseWebSocketFrame) {
                // Echo the close and close the connection
                readState = ReadState.Closed;
                ctx.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
                
            } else {
                ctx.fireChannelRead(msg);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Will be called either through an exception in channelRead 
            // or when the websocket handshake fails
            readState = ReadState.Error;
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            ctx.fireExceptionCaught(cause);
        }
    }
}
