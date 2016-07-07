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

package ws.wamp.jawampa.client;

import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.connection.IWampConnectionListener;

/**
 * Connection listener for that receives and processes WampMessages and state
 * events from the pipeline.<br>
 * A new instance of this is created for each connection attempt.<br>
 * All events must be rescheduled to the eventLoop, because it might be the case that the
 * events are fired from another thread.
 */
class ClientConnectionListener implements IWampConnectionListener {
    private final StateController stateController;

    ClientConnectionListener(StateController stateController) {
        this.stateController = stateController;
    }

    @Override
    public void messageReceived(final WampMessage message) {
        // Remark: No need to check for connection. The controller guarantees that after close()
        // messages will no longer be forwarded
        stateController.onMessage(message);
    }

    @Override
    public void transportClosed() {
        // Remark: No need to check for connection. The controller guarantees that after close()
        // messages will no longer be forwarded
        transportError(new ApplicationError(ApplicationError.TRANSPORT_CLOSED));
    }

    @Override
    public void transportError(final Throwable cause) {
        // Remark: No need to check for connection. The controller guarantees that after close()
        // messages will no longer be forwarded
        stateController.onConnectionClosed(cause);
    }
}