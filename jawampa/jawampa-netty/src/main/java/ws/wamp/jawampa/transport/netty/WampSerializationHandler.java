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

import java.util.List;

import ws.wamp.jawampa.WampMessages.WampMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import ws.wamp.jawampa.WampSerialization;

public class WampSerializationHandler extends MessageToMessageEncoder<WampMessage> {
    
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WampSerializationHandler.class);

    final WampSerialization serialization;
    
    public WampSerialization serialization() {
        return serialization;
    }
    
    public WampSerializationHandler(WampSerialization serialization) {
        this.serialization = serialization;
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WampMessage msg, List<Object> out) throws Exception {
        ByteBuf msgBuffer = Unpooled.buffer();
        ByteBufOutputStream outStream = new ByteBufOutputStream(msgBuffer);
        ObjectMapper objectMapper = serialization.getObjectMapper();
        try {
            JsonNode node = msg.toObjectArray(objectMapper);
            objectMapper.writeValue(outStream, node);

            if (logger.isDebugEnabled()) {
                logger.debug("Serialized Wamp Message: {}", node.toString());
            }

        } catch (Exception e) {
            msgBuffer.release();
            return;
        }

        if (serialization.isText()) {
            TextWebSocketFrame frame = new TextWebSocketFrame(msgBuffer);
            out.add(frame);
        } else {
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(msgBuffer);
            out.add(frame);
        }
    }
}
