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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampMessages;
import ws.wamp.jawampa.WampRoles;
import ws.wamp.jawampa.WampMessages.AbortMessage;
import ws.wamp.jawampa.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.WampMessages.WelcomeMessage;
import ws.wamp.jawampa.auth.client.ClientSideAuthentication;
import ws.wamp.jawampa.connection.IConnectionController;
import ws.wamp.jawampa.connection.IWampConnectionPromise;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The state where the WAMP handshake (HELLO, WELCOME, ...) is exchanged.
 */
public class HandshakingState implements ClientState {
    private final StateController stateController;
    /** The currently active connection */
    public final IConnectionController connectionController;
    private int nrReconnectAttempts;
    
    private boolean challengeMsgAllowed = true;
    
    Throwable disconnectReason;
    
    public HandshakingState(StateController stateController, IConnectionController connectionController, int nrReconnectAttempts) {
        this.stateController = stateController;
        this.connectionController = connectionController;
        this.nrReconnectAttempts = nrReconnectAttempts;
    }
    
    @Override
    public void onEnter(ClientState lastState) {
        sendHelloMessage();
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }

    @Override
    public void initClose() {
        closeIncompleteSession(null, ApplicationError.SYSTEM_SHUTDOWN, false);
    }
    
    void closeIncompleteSession(Throwable disconnectReason, String optAbortReason, boolean reconnectAllowed) {
        // Send abort to the remote
        if (optAbortReason != null) {
            AbortMessage msg = new AbortMessage(null, optAbortReason);
            connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
        }
        
        int nrReconnects = reconnectAllowed ? nrReconnectAttempts : 0;
        if (nrReconnects == 0) {
            stateController.setExternalState(new WampClient.DisconnectedState(disconnectReason));
        }
        WaitingForDisconnectState newState = new WaitingForDisconnectState(stateController, nrReconnects);
        connectionController.close(true, newState.closePromise());
        stateController.setState(newState);
    }
    
    void handleProtocolError() {
        handleSessionError(
            new ApplicationError(ApplicationError.PROTCOL_ERROR),
            ApplicationError.PROTCOL_ERROR);
    }
    
    void handleSessionError(ApplicationError error, String closeReason) {
        boolean reconnectAllowed = !stateController.clientConfig().closeClientOnErrors();
        if (!reconnectAllowed) {
            // Record the error that happened during the session
            stateController.setCloseError(error);
        }
        closeIncompleteSession(error, closeReason, reconnectAllowed);
    }
    
    /**
     * Is called if the underlying connection was closed from the remote side.
     * Won't be called if the user issues the close, since the client will then move
     * to the {@link WaitingForDisconnectState} directly.
     * @param closeReason An optional reason why the connection closed.
     */
    void onConnectionClosed(Throwable closeReason) {
        if (closeReason == null)
            closeReason = new ApplicationError(ApplicationError.TRANSPORT_CLOSED);
        closeIncompleteSession(closeReason, null, true);
    }
    
    /**
     * Is called after the low-level connection between the client and the server was established
     */
    void sendHelloMessage() {
        // System.out.println("Session websocket connection established");
        // Connection to the remote host was established
        // However the WAMP session is not established until the handshake was finished
        
        connectionController
        .sendMessage(new WampMessages.HelloMessage(stateController.clientConfig().realm(), stateController.clientConfig().helloDetails()), IWampConnectionPromise.Empty);
    }
    
    void onMessage(WampMessage msg) {
        // We were not yet welcomed
        if (msg instanceof WelcomeMessage) {
            // Receive a welcome. Now the session is established!
            ObjectNode welcomeDetails = ((WelcomeMessage) msg).details;
            long sessionId = ((WelcomeMessage) msg).sessionId;
            
            // Extract the roles of the remote side
            JsonNode roleNode = welcomeDetails.get("roles");
            if (roleNode == null || !roleNode.isObject()) {
                handleProtocolError();
                return;
            }
            
            EnumSet<WampRoles> routerRoles = EnumSet.noneOf(WampRoles.class);
            Iterator<String> roleKeys = roleNode.fieldNames();
            while (roleKeys.hasNext()) {
                WampRoles role = WampRoles.fromString(roleKeys.next());
                if (role != null) routerRoles.add(role);
            }
            
            SessionEstablishedState newState = new SessionEstablishedState(
                stateController, connectionController, sessionId, welcomeDetails, routerRoles);
            stateController.setState(newState);
        }
        else if (msg instanceof ChallengeMessage) {
            if (!challengeMsgAllowed) {
                // Allow Challenge message only a single time
                handleProtocolError();
                return;
            }
            challengeMsgAllowed = false;
            
            ChallengeMessage challenge = (ChallengeMessage) msg;
            String authMethodString = challenge.authMethod;
            List<ClientSideAuthentication> authMethods = stateController.clientConfig().authMethods();
            
            for (ClientSideAuthentication authMethod : authMethods) {
                if (authMethod.getAuthMethod().equals(authMethodString)) {
                    AuthenticateMessage reply =
                        authMethod.handleChallenge(challenge, stateController.clientConfig().objectMapper());
                    if (reply == null) {
                        handleProtocolError();
                    } else {
                        connectionController.sendMessage(reply, IWampConnectionPromise.Empty);
                    }
                    return;
                }
            }
            handleProtocolError();
        }
        else if (msg instanceof AbortMessage) {
            // The remote doesn't want us to connect :(
            AbortMessage abort = (AbortMessage) msg;
            handleSessionError(new ApplicationError(abort.reason), null);
        }
    }
}