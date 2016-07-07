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

/**
 * Callback interface for WAMP connections.<br>
 * The connection will notify the connection user (e.g. the Client or Router)
 * about connection events through this interface.
 */
public interface IWampConnectionListener {
    /** Signals that the connection got closed gracefully */
    void transportClosed();
    
    /** Signals that the connection encountered an error and got closed */
    void transportError(Throwable cause);
    
    /** A message is received from the WAMP connection */
    void messageReceived(WampMessage message);
}
