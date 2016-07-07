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

import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLException;

import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.connection.IPendingWampConnection;
import ws.wamp.jawampa.connection.IPendingWampConnectionListener;
import ws.wamp.jawampa.connection.IWampClientConnectionConfig;
import ws.wamp.jawampa.connection.IWampConnection;
import ws.wamp.jawampa.connection.IWampConnectionListener;
import ws.wamp.jawampa.connection.IWampConnectionPromise;
import ws.wamp.jawampa.connection.IWampConnector;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.WampSerialization;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * Returns factory methods for the establishment of WAMP connections between
 * clients and routers.<br>
 */
public class NettyWampClientConnectorProvider implements IWampConnectorProvider {

    @Override
    public ScheduledExecutorService createScheduler() {
        NioEventLoopGroup scheduler = new NioEventLoopGroup(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "WampClientEventLoop");
                t.setDaemon(true);
                return t;
            }
        });
        return scheduler;
    }

    @Override
    public IWampConnector createConnector(final URI uri,
            IWampClientConnectionConfig configuration,
            List<WampSerialization> serializations,
            final SocketAddress proxyAddress) throws Exception {

        String scheme = uri.getScheme();
        scheme = scheme != null ? scheme : "";
        
        // Check if the configuration is a netty configuration.
        // However null is an allowed value
        final NettyWampConnectionConfig nettyConfig;
        if (configuration instanceof NettyWampConnectionConfig) {
            nettyConfig = (NettyWampConnectionConfig) configuration;
        } else if (configuration != null) {
            throw new ApplicationError(ApplicationError.INVALID_CONNECTION_CONFIGURATION);
        } else {
            nettyConfig = null;
        }
        
        if (scheme.equalsIgnoreCase("ws") || scheme.equalsIgnoreCase("wss")) {
            
            // Check the host and port field for validity
            if (uri.getHost() == null || uri.getPort() == 0) {
                throw new ApplicationError(ApplicationError.INVALID_URI);
            }
            
            // Initialize SSL when required
            final boolean needSsl = uri.getScheme().equalsIgnoreCase("wss");
            final SslContext sslCtx0;
            if (needSsl && (nettyConfig == null || nettyConfig.sslContext() == null)) {
                // Create a default SslContext when we got none provided through the constructor
                try {
                    sslCtx0 = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
                }
                catch (SSLException e) {
                    throw e;
                }
            } else if (needSsl) {
                sslCtx0 = nettyConfig.sslContext();
            } else {
                sslCtx0 = null;
            }
            
            final String subProtocols = WampSerialization.makeWebsocketSubprotocolList(serializations);

            final int maxFramePayloadLength = (nettyConfig == null )? NettyWampConnectionConfig.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH : nettyConfig.getMaxFramePayloadLength();

            // Return a factory that creates a channel for websocket connections
            return new IWampConnector() {
                @Override
                public IPendingWampConnection connect(final ScheduledExecutorService scheduler,
                        final IPendingWampConnectionListener connectListener,
                        final IWampConnectionListener connectionListener) {
                    
                    // Use well-known ports if not explicitly specified
                    final int port;
                    if (uri.getPort() == -1) {
                        if (needSsl) port = 443;
                        else port = 80;
                    } else port = uri.getPort();
                    
                    final WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, subProtocols,
                        false, new DefaultHttpHeaders(), maxFramePayloadLength);
                    
                    /**
                     * Netty handler for that receives and processes WampMessages and state
                     * events from the pipeline.
                     * A new instance of this is created for each connection attempt.
                     */
                    final ChannelHandler connectionHandler = new SimpleChannelInboundHandler<WampMessage>() {
                        boolean connectionWasEstablished = false;
                        /** Guard to prevent forwarding events aftert the channel was closed */
                        boolean wasClosed = false;
                        
                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            if (wasClosed) return;
                            wasClosed = true;
                            if (connectionWasEstablished) {
                                connectionListener.transportClosed();
                            } else {
                                // The transport closed before the websocket handshake was completed
                                connectListener.connectFailed(new ApplicationError(ApplicationError.TRANSPORT_CLOSED));
                            }
                        }
                        
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            if (wasClosed) return;
                            wasClosed = true;
                            if (connectionWasEstablished) {
                                connectionListener.transportError(cause);
                            } else {
                                // The transport closed before the websocket handshake was completed
                                connectListener.connectFailed(cause);
                            }
                            super.exceptionCaught(ctx, cause);
                        }
                        
                        @Override
                        public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
                            if (wasClosed) return;
                            if (evt instanceof ConnectionEstablishedEvent) {
                                ConnectionEstablishedEvent ev = (ConnectionEstablishedEvent)evt;
                                final WampSerialization serialization = ev.serialization();
                                
                                IWampConnection connection = new IWampConnection() {
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
                                        // sendRemaining is ignored. Remaining data is always sent
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
                                };
                                
                                connectionWasEstablished = true;
                                
                                // Connection to the remote host was established
                                // However the WAMP session is not established until the handshake was finished
                                connectListener.connectSucceeded(connection);
                            }
                        }

                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, WampMessage msg) throws Exception {
                            if (wasClosed) return;
                            assert (connectionWasEstablished);
                            connectionListener.messageReceived(msg);
                        }
                    };
                    
                    // If the assigned scheduler is a netty eventloop use this
                    final EventLoopGroup nettyEventLoop;
                    if (scheduler instanceof EventLoopGroup) {
                        nettyEventLoop = (EventLoopGroup)scheduler;
                    } else {
                        connectListener.connectFailed(new ApplicationError(ApplicationError.INCOMATIBLE_SCHEDULER));
                        return IPendingWampConnection.Dummy;
                    }
                    
                    Bootstrap b = new Bootstrap();
                    b.group(nettyEventLoop)
                     .channel(NioSocketChannel.class)
                     .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (proxyAddress != null) {
                            p.addLast(new HttpProxyHandler(proxyAddress));
                        }
                        if (sslCtx0 != null) {
                            p.addLast(sslCtx0.newHandler(ch.alloc(),
                                                         uri.getHost(),
                                                         port));
                        }
                        p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator(8192),
                            new WebSocketClientProtocolHandler(handshaker, false),
                            new WebSocketFrameAggregator(WampHandlerConfiguration.MAX_WEBSOCKET_FRAME_SIZE),
                            new WampClientWebsocketHandler(handshaker),
                            connectionHandler);
                        }
                    });
                    
                    final ChannelFuture connectFuture = b.connect(uri.getHost(), port);
                    connectFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                // Do nothing. The connection is only successful when the websocket handshake succeeds
                            } else {
                                // Remark: Might be called directly in addListener
                                // Therefore addListener should be the last call
                                // Remark2: This branch will be taken upon cancellation.
                                // This is required by the contract.
                                connectListener.connectFailed(future.cause());
                            }
                        }
                    });
                    
                    // Return the connection in progress with the ability for cancellation
                    return new IPendingWampConnection() {
                        @Override
                        public void cancelConnect() {
                            connectFuture.cancel(false);
                        }
                    };
                }
            };
        }
        
        throw new ApplicationError(ApplicationError.INVALID_URI);
    }
}
