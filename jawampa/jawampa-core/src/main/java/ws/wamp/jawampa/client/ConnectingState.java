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

import java.util.concurrent.RejectedExecutionException;

import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.connection.IConnectionController;
import ws.wamp.jawampa.connection.IPendingWampConnection;
import ws.wamp.jawampa.connection.IPendingWampConnectionListener;
import ws.wamp.jawampa.connection.IWampConnection;
import ws.wamp.jawampa.connection.IWampConnectionPromise;
import ws.wamp.jawampa.connection.QueueingConnectionController;

/** The session is trying to connect to the router */
public class ConnectingState implements ClientState, IPendingWampConnectionListener {
    
    private final StateController stateController;
    /** The currently active connection */
    IConnectionController connectionController;
    /** The current connection attempt */
    IPendingWampConnection connectingCon;
    /** Whether the connection attempt is cancelled */
    boolean isCancelled = false;
    /** How often connects should be attempted */
    int nrConnectAttempts;
    
    public ConnectingState(StateController stateController, int nrConnectAttempts) {
        this.stateController = stateController;
        this.nrConnectAttempts = nrConnectAttempts;
    }
    
    @Override
    public void onEnter(ClientState lastState) {
        if (lastState instanceof InitialState) {
            stateController.setExternalState(new WampClient.ConnectingState());
        }
        
        // Check for valid number of connects
        assert (nrConnectAttempts != 0);
        // Decrease remaining number of reconnects if it's not infinite
        if (nrConnectAttempts > 0) nrConnectAttempts--;
        
        // Starts an connection attempt to the router
        connectionController =
            new QueueingConnectionController(stateController.scheduler(), new ClientConnectionListener(stateController));
        
        try {
            connectingCon =
                stateController.clientConfig().connector().connect(stateController.scheduler(), this, connectionController);
        } catch (Exception e) {
            // Catch exceptions that can happen during creating the channel
            // These are normally signs that something is wrong with our configuration
            // Therefore we don't trigger retries
            stateController.setCloseError(e);
            stateController.setExternalState(new WampClient.DisconnectedState(e));
            DisconnectedState newState = new DisconnectedState(stateController, e);
            // This is a reentrant call to setState. However it works as onEnter is the last call in setState
            stateController.setState(newState);
        }
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }
    
    @Override
    public void connectSucceeded(final IWampConnection connection) {
        try {
            stateController.scheduler().execute(new Runnable() {
                @Override
                public void run() {
                    if (!isCancelled) {
                        // Our new channel is connected
                        connectionController.setConnection(connection);
                        HandshakingState newState = new HandshakingState(stateController, connectionController, nrConnectAttempts);
                        stateController.setState(newState);
                    } else {
                        // We we're connected but aren't interested in the channel anymore
                        // The client should close
                        // Therefore we close the new channel
                        stateController.setExternalState(new WampClient.DisconnectedState(null));
                        WaitingForDisconnectState newState = new WaitingForDisconnectState(stateController, nrConnectAttempts);
                        connection.close(false, newState.closePromise());
                        stateController.setState(newState);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            connection.close(false, IWampConnectionPromise.Empty);
        }
    }
    
    @Override
    public void connectFailed(final Throwable cause) {
        stateController.tryScheduleAction(new Runnable() {
            @Override
            public void run() {
                if (!isCancelled) {
                    // Try reconnect if possible, otherwise announce close
                    if (nrConnectAttempts != 0) { // Reconnect is allowed
                        WaitingForReconnectState nextState = new WaitingForReconnectState(stateController, nrConnectAttempts);
                        stateController.setState(nextState);
                    } else {
                        stateController.setExternalState(new WampClient.DisconnectedState(cause));
                        DisconnectedState nextState = new DisconnectedState(stateController, cause);
                        stateController.setState(nextState);
                    }
                } else {
                    // Connection cancel attempt was successfully cancelled.
                    // This is the final state
                    stateController.setExternalState(new WampClient.DisconnectedState(null));
                    DisconnectedState nextState = new DisconnectedState(stateController, null);
                    stateController.setState(nextState);
                }
            }
        });
    }

    @Override
    public void initClose() {
        if (isCancelled) return;
        isCancelled = true;
        connectingCon.cancelConnect();
    }
}