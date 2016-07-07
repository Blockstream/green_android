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
 * Provides the means to accept incoming WAMP connections.<br>
 * The connection acceptance is split into 2 phases:<br>
 * <ul>
 * <li>In the first step the new connection requests a {@link IWampConnectionListener} interface
 * from the acceptor. The instance of the interface is not yet attached to the acceptor
 * and can be safely dropped and garbage collected.
 * <li>In a second step the new connection is registered together with the received listener
 * at the acceptor.
 * </ul>
 */
public interface IWampConnectionAcceptor {
    
    /** Creates a listener for a new incoming connection */
    IWampConnectionListener createNewConnectionListener();
    
    /**
     * Requests the acceptor to accept the new incoming connection.<br>
     * This <b>must</b> be called before any method is called on the {@link IWampConnectionListener}
     * 
     * @param newConnection The connection that is accepted
     * @param connectionListener The listener for the accepted connection.<br>
     * This must match the listener that was retrieved with {@link IWampConnectionAcceptor#createNewConnectionListener()} 
     */
    void acceptNewConnection(IWampConnection newConnection, IWampConnectionListener connectionListener);
}
