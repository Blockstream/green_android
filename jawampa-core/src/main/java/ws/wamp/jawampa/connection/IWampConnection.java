/*
 * Copyright 2015 Matthias Einwag
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

package ws.wamp.jawampa.connection;

import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.WampSerialization;

/**
 * An established connection to remote peer which is able to transmit WAMP
 * messages.
 */
public interface IWampConnection {
    /**
     * Returns the serialization method that this connection supports.
     * this must be available immediately and may not change during the
     * lifecycle of the transport.
     */
    WampSerialization serialization();
    
    /**
     * Returns whether only a single message can be sent through the connection and
     * the result must be awaited afterwards or whether the connection supports queuing
     * and {@link IWampConnection#sendMessage(WampMessage, ICompletionCallback)}
     * and {@link IWampConnection#sendMessageAndClose(WampMessage, ICompletionCallback)}
     * can be called arbitrarily often.
     * @return true if only one message can be sent at once, false otherwise
     */
    boolean isSingleWriteOnly();
    
    /**
     * Send a message through the connection.<br>
     * This may not throw.
     * 
     * @param message The message to send
     * @param promise The promise that must be fulfilled by the connection once
     * the message was sent or when an error occured during sending.
     */
    void sendMessage(WampMessage message, IWampConnectionPromise<Void> promise);
    
    /**
     * Close the connection independent of it's current state.<br>
     * This method must be callable even when a message is currently transmitted.<br>
     * <br>
     * jawampa guarantees that close will be only called once in the lifecycle of
     * a connection.<br> 
     * The implementation of the connection must guarantee that the call gets
     * acknowledged through the promise and that after acknowledgement no further
     * events will be sent to the configured {@link IWampConnectionListener}.<br>
     * <br>
     * This may not throw.
     * 
     * @param sendRemaining Whether already queued messages should be sent or the
     * transport should be closed immediately
     * @param promise The promise that must be fulfilled once the connection got
     * closed.
     */
    void close(boolean sendRemaining, IWampConnectionPromise<Void> promise);
}
