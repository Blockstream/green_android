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

import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ws.wamp.jawampa.WampSerialization;

/**
 * Provides connectors that are used by WAMP clients to connect to servers.
 */
public interface IWampConnectorProvider {
    
    /**
     * Creates and returns a scheduler for the client.<br>
     * The scheduler is suitable as a scheduler for the client transports which
     * are created by the factory
     */
    ScheduledExecutorService createScheduler();
    
    /**
     * Create a connector that can be used by a client to connect to a server later on.
     * @param uri The uri to which the client should connect
     * @param configuration Additional configuration information for the connection.<br>
     * The type of the configuration depends on the {@link IWampConnectorProvider}.
     * @param serializations The serializations that the connection should support in
     * preferred order.<br> 
     * Must not be null.
     * @return A connector which can be used by the client to connect to the server.
     * @throws Exception If no suitable connector can be created with this configuration
     */
    IWampConnector createConnector(URI uri,
                                   IWampClientConnectionConfig configuration,
                                   List<WampSerialization> serializations,
                                   SocketAddress proxyAddress) throws Exception;
}
