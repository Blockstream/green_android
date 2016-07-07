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

import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.connection.ICompletionCallback;
import ws.wamp.jawampa.connection.IWampConnectionFuture;
import ws.wamp.jawampa.connection.WampConnectionPromise;

/**
 * The client is waiting for a no longer used connection to close
 */
public class WaitingForDisconnectState implements ClientState, ICompletionCallback<Void> {
    
    private final StateController stateController;
    private int nrReconnectAttempts;
    WampConnectionPromise<Void> closePromise = new WampConnectionPromise<Void>(this, null);
    
    public WaitingForDisconnectState(StateController stateController, int nrReconnectAttempts) {
        this.stateController = stateController;
        this.nrReconnectAttempts = nrReconnectAttempts;
    }
    
    public int nrReconnectAttempts() {
        return nrReconnectAttempts;
    }
    
    @Override
    public void onEnter(ClientState lastState) {
        
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }
    
    @Override
    public void initClose() {
        if (nrReconnectAttempts != 0) {
            // Cancelling a reconnect triggers a state transition
            nrReconnectAttempts = 0;
            stateController.setExternalState(new WampClient.DisconnectedState(null));
        }
    }
    
    /**
     * Gets the associated promise that should be completed when the
     * connection finally closes.
     */
    public WampConnectionPromise<Void> closePromise() {
        return closePromise;
    }

    @Override
    public void onCompletion(IWampConnectionFuture<Void> future) {
        // Is called once the disconnect from the previous transport has happened
        if (nrReconnectAttempts == 0) {
            DisconnectedState newState = new DisconnectedState(stateController, null);
            stateController.setState(newState);
        } else {
            WaitingForReconnectState newState = new WaitingForReconnectState(stateController, nrReconnectAttempts);
            stateController.setState(newState);
        }
    }

    
}