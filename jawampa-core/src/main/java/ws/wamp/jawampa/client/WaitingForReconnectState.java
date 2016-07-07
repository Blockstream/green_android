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

import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action0;
import ws.wamp.jawampa.WampClient;

public class WaitingForReconnectState implements ClientState {
    
    private final StateController stateController;
    private int nrReconnectAttempts;
    Subscription reconnectSubscription;
    
    public WaitingForReconnectState(StateController stateController, int nrReconnectAttempts) {
        this.stateController = stateController;
        this.nrReconnectAttempts = nrReconnectAttempts;
    }
    
    @Override
    public void onEnter(ClientState lastState) {
        reconnectSubscription =
            stateController.rxScheduler().createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    if (stateController.currentState() != WaitingForReconnectState.this) return;
                    // Reconnect now
                    ConnectingState newState = new ConnectingState(stateController, nrReconnectAttempts);
                    stateController.setState(newState);
                }
            }, stateController.clientConfig().reconnectInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }

    @Override
    public void initClose() {
        reconnectSubscription.unsubscribe();
        // Current external state is Connecting
        // Move to disconnected
        stateController.setExternalState(new WampClient.DisconnectedState(null));
        // And switch the internal state also to Disconnected
        DisconnectedState newState = new DisconnectedState(stateController, null);
        stateController.setState(newState);
    }
}