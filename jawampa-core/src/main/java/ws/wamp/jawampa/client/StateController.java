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
import java.util.concurrent.ScheduledExecutorService;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClient.DisconnectedState;
import ws.wamp.jawampa.WampClient.State;
import ws.wamp.jawampa.WampMessages.WampMessage;

public class StateController {
    private boolean isCompleted = false;
    
    private ClientState currentState = new InitialState(this);
    private ClientConfiguration clientConfig;
    
    private final ScheduledExecutorService scheduler;
    private final Scheduler rxScheduler;
    
    /** The current externally visible status */
    private State extState = new DisconnectedState(null);
    /** Holds the final value with which {@link WampClient#status} will be completed */
    private Throwable closeError = null;
    /** Observable that provides the external state */
    private BehaviorSubject<State> statusObservable = BehaviorSubject.create(extState);
    
    public StateController(ClientConfiguration clientConfig) {
        this.clientConfig = clientConfig;
        this.scheduler = clientConfig.connectorProvider.createScheduler();
        this.rxScheduler = Schedulers.from(scheduler);
    }
    
    public ClientConfiguration clientConfig() {
        return clientConfig;
    }
    
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }
    
    public Scheduler rxScheduler() {
        return rxScheduler;
    }
    
    public Observable<State> statusObservable() {
        return statusObservable;
    }
    
    public void setExternalState(State newState) {
        extState = newState;
        statusObservable.onNext(extState);
    }
    
    public void setCloseError(Throwable closeError) {
        this.closeError = closeError;
    }
    
    /**
     * Tries to schedule a runnable on the provided scheduler.<br>
     * Rejected executions will be suppressed.
     * 
     * @param action The action to schedule.
     */
    public void tryScheduleAction(Runnable action) {
        try {
            scheduler.submit(action);
        } catch (RejectedExecutionException e) {}
    }

    public ClientState currentState() {
        return currentState;
    }
    
    public void setState(ClientState newState) {
        ClientState lastState = currentState;
        if (lastState != null) lastState.onLeave(newState);
        currentState = newState;
        newState.onEnter(lastState);
    }
    
    /**
     * Is called when the underlying connection received a message from the remote side.
     * @param message The received message
     */
    void onMessage(WampMessage message) {
        if (currentState instanceof SessionEstablishedState)
            ((SessionEstablishedState)currentState).onMessage(message);
        else if (currentState instanceof HandshakingState)
            ((HandshakingState)currentState).onMessage(message);
    }
    
    /**
     * Is called if the underlying connection was closed from the remote side.
     * Won't be called if the user issues the close, since the client will then move
     * to the {@link WaitingForDisconnectState} directly.
     * @param closeReason An optional reason why the connection closed.
     */
    void onConnectionClosed(Throwable closeReason) {
        if (currentState instanceof SessionEstablishedState)
            ((SessionEstablishedState)currentState).onConnectionClosed(closeReason);
        else if (currentState instanceof HandshakingState)
            ((HandshakingState)currentState).onConnectionClosed(closeReason);
    }
    
    /**
     * Initiates the open process.<br>
     * If open was initiated before nothing will happen.
     */
    public void open() {
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                if (!(currentState instanceof InitialState)) return;
                // Try to connect afterwards
                // This guarantees that the external state will always
                // switch to connecting, even when the attempt immediately
                // fails
                int nrConnects = clientConfig.totalNrReconnects();
                if (nrConnects == 0) nrConnects = 1;
                ConnectingState newState =
                    new ConnectingState(StateController.this, nrConnects);
                setState(newState);
            }
        });
    }
    
    /**
     * Initiates the close process.<br>
     * Will be called on {@link WampClient#close()} of the client
     */
    public void initClose() {
        tryScheduleAction(new Runnable() {
            @Override
            public void run() {
                if (isCompleted) return;// Check if already closed
                isCompleted = true;
                
                // Initialize the close sequence
                // The state will try to move to the final state
                currentState.initClose();
            }
        });
    }
    
    /**
     * Performs the shutdown once the statemachine is in it's terminal state.
     */
    public void performShutdown() {
        if (closeError != null)
            statusObservable.onError(closeError);
        else
            statusObservable.onCompleted();
        scheduler.shutdown();
    }
}
