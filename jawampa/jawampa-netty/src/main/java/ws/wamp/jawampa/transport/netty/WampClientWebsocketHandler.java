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

import ws.wamp.jawampa.WampError;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import ws.wamp.jawampa.WampSerialization;

public class WampClientWebsocketHandler extends ChannelInboundHandlerAdapter {
    
    final WebSocketClientHandshaker handshaker;
    
    WampSerialization serialization;
    
    public WampSerialization serialization() {
        return serialization;
    }
    
    public WampClientWebsocketHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof CloseWebSocketFrame) {
            //readState = ReadState.Closed;
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg)
                      .addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            // Handshake is completed
            String actualProtocol = handshaker.actualSubprotocol();
            serialization = WampSerialization.fromString(actualProtocol);
            if (serialization == WampSerialization.Invalid) {
                throw new WampError("Invalid Websocket Protocol");
            }
            
            // Install the serializer and deserializer
            ctx.pipeline()
               .addAfter(ctx.name(), "wamp-deserializer", 
                         new WampDeserializationHandler(serialization));
            ctx.pipeline()
               .addAfter(ctx.name(), "wamp-serializer", 
                         new WampSerializationHandler(serialization));
            
            // Fire the connection established event
            ctx.fireUserEventTriggered(new ConnectionEstablishedEvent(serialization));
            
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }
}
