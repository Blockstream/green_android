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

/**
 * A controller that manages the state of a connection.<br>
 * The controller queues all incoming messages through a scheduler onto a desired thread.<br>
 * It also guarantees that after a close() call no more messages from the connection will
 * be forwarded.
 */
public interface IConnectionController extends IWampConnection, IWampConnectionListener {
    /** Returns the wrapped connection object */
    IWampConnection connection();
    
    /**
     * Sets the underlying connection.<br>
     * This <b>must</b> be called before any message is pushed into the listener!
     * @param connection The connection to use
     */
    void setConnection(IWampConnection connection);
    
    /** Returns the wrapped connection listener object */
    IWampConnectionListener connectionListener();
}
