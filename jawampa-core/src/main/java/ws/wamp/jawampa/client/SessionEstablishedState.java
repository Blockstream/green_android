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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.AsyncSubject;
import rx.subscriptions.Subscriptions;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.CallFlags;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.PublishFlags;
import ws.wamp.jawampa.Reply;
import ws.wamp.jawampa.Request;
import ws.wamp.jawampa.SubscriptionFlags;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampMessages;
import ws.wamp.jawampa.WampRoles;
import ws.wamp.jawampa.WampMessages.AbortMessage;
import ws.wamp.jawampa.WampMessages.CallMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;
import ws.wamp.jawampa.WampMessages.ErrorMessage;
import ws.wamp.jawampa.WampMessages.EventMessage;
import ws.wamp.jawampa.WampMessages.GoodbyeMessage;
import ws.wamp.jawampa.WampMessages.InvocationMessage;
import ws.wamp.jawampa.WampMessages.PublishedMessage;
import ws.wamp.jawampa.WampMessages.RegisterMessage;
import ws.wamp.jawampa.WampMessages.RegisteredMessage;
import ws.wamp.jawampa.WampMessages.ResultMessage;
import ws.wamp.jawampa.WampMessages.SubscribeMessage;
import ws.wamp.jawampa.WampMessages.SubscribedMessage;
import ws.wamp.jawampa.WampMessages.UnregisterMessage;
import ws.wamp.jawampa.WampMessages.UnregisteredMessage;
import ws.wamp.jawampa.WampMessages.UnsubscribeMessage;
import ws.wamp.jawampa.WampMessages.UnsubscribedMessage;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.WampMessages.WelcomeMessage;
import ws.wamp.jawampa.connection.IConnectionController;
import ws.wamp.jawampa.connection.IWampConnectionPromise;
import ws.wamp.jawampa.internal.IdGenerator;
import ws.wamp.jawampa.internal.IdValidator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The client is connected to the router and the session was established
 */
public class SessionEstablishedState implements ClientState {
    
    enum PubSubState {
        Subscribing,
        Subscribed,
        Unsubscribing,
        Unsubscribed
    }
    
    enum RegistrationState {
        Registering,
        Registered,
        Unregistering,
        Unregistered
    }
    
    static class RequestMapEntry {
        public final int requestType;
        public final AsyncSubject<?> resultSubject;
        
        public RequestMapEntry(int requestType, AsyncSubject<?> resultSubject) {
            this.requestType = requestType;
            this.resultSubject = resultSubject;
        }
    }
    
    static class SubscriptionMapEntry {
        public PubSubState state;
        public final SubscriptionFlags flags;
        public long subscriptionId = 0;
        
        public final List<Subscriber<? super PubSubData>> subscribers
            = new ArrayList<Subscriber<? super PubSubData>>();
        
        public SubscriptionMapEntry(SubscriptionFlags flags, PubSubState state) {
            this.flags = flags;
            this.state = state;
        }
    }
    
    static class RegisteredProceduresMapEntry {
        public RegistrationState state;
        public long registrationId = 0;
        public final Subscriber<? super Request> subscriber;
        
        public RegisteredProceduresMapEntry(Subscriber<? super Request> subscriber, RegistrationState state) {
            this.subscriber = subscriber;
            this.state = state;
        }
    }
    
    private final StateController stateController;
    final long sessionId;
    final ObjectNode welcomeDetails;
    final EnumSet<WampRoles> routerRoles;
    
    /** The currently active connection */
    IConnectionController connectionController;
    
    HashMap<Long, RequestMapEntry> requestMap = 
        new HashMap<Long, RequestMapEntry>();

    EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>> subscriptionsByFlags =
        new EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>>(SubscriptionFlags.class);
    HashMap<Long, SubscriptionMapEntry> subscriptionsBySubscriptionId =
        new HashMap<Long, SubscriptionMapEntry>();
    
    public HashMap<String, RegisteredProceduresMapEntry> registeredProceduresByUri = 
            new HashMap<String, RegisteredProceduresMapEntry>();
    HashMap<Long, RegisteredProceduresMapEntry> registeredProceduresById = 
            new HashMap<Long, RegisteredProceduresMapEntry>();
    
    long lastRequestId = IdValidator.MIN_VALID_ID;
    
    public SessionEstablishedState(StateController stateController, IConnectionController connectionController,
            long sessionId, ObjectNode welcomeDetails, EnumSet<WampRoles> routerRoles) {
        this.stateController = stateController;
        this.connectionController = connectionController;
        this.sessionId = sessionId;
        this.welcomeDetails = welcomeDetails;
        this.routerRoles = routerRoles;
        
        subscriptionsByFlags.put(SubscriptionFlags.Exact, new HashMap<String, SubscriptionMapEntry>());
        subscriptionsByFlags.put(SubscriptionFlags.Prefix, new HashMap<String, SubscriptionMapEntry>());
        subscriptionsByFlags.put(SubscriptionFlags.Wildcard, new HashMap<String, SubscriptionMapEntry>());
    }
    
    public IConnectionController connectionController() {
        return connectionController;
    }
    
    @Override
    public void onEnter(ClientState lastState) {
        stateController.setExternalState(new WampClient.ConnectedState(sessionId, welcomeDetails, routerRoles));
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }

    @Override
    public void initClose() {
        closeSession(null, ApplicationError.SYSTEM_SHUTDOWN, false);
    }
    
    void closeSession(Throwable disconnectReason, String optCloseMessageReason, boolean reconnectAllowed) {
        // Send goodbye message with close reason to the remote
        if (optCloseMessageReason != null) {
            GoodbyeMessage msg = new GoodbyeMessage(null, optCloseMessageReason);
            connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
        }
        
        stateController.setExternalState(new WampClient.DisconnectedState(disconnectReason));
        
        int nrReconnectAttempts = reconnectAllowed ? stateController.clientConfig().totalNrReconnects : 0;
        if (nrReconnectAttempts != 0) {
            stateController.setExternalState(new WampClient.ConnectingState());
        }
        
        clearSessionData();
        
        WaitingForDisconnectState newState = new WaitingForDisconnectState(stateController, nrReconnectAttempts);
        connectionController.close(true, newState.closePromise());
        stateController.setState(newState);
    }
    
    void clearSessionData() {
        clearPendingRequests(new ApplicationError(ApplicationError.TRANSPORT_CLOSED));
        clearAllSubscriptions(null);
        clearAllRegisteredProcedures(null);
    }
    
    void clearPendingRequests(Throwable e) {
        for (Entry<Long, RequestMapEntry> entry : requestMap.entrySet()) {
            entry.getValue().resultSubject.onError(e);
        }
        requestMap.clear();
    }
    
    void clearAllSubscriptions(Throwable e) {
        for (HashMap<String, SubscriptionMapEntry> subscriptionByUri : subscriptionsByFlags.values()) {
            for (Entry<String, SubscriptionMapEntry> entry : subscriptionByUri.entrySet()) {
                for (Subscriber<? super PubSubData> s : entry.getValue().subscribers) {
                    if (e == null) s.onCompleted();
                    else s.onError(e);
                }
                entry.getValue().state = PubSubState.Unsubscribed;
            }
            subscriptionByUri.clear();
        }
        subscriptionsBySubscriptionId.clear();
    }
    
    void clearAllRegisteredProcedures(Throwable e) {
        for (Entry<String, RegisteredProceduresMapEntry> entry : registeredProceduresByUri.entrySet()) {
            if (e == null) entry.getValue().subscriber.onCompleted();
            else entry.getValue().subscriber.onError(e);
            entry.getValue().state = RegistrationState.Unregistered;
        }
        registeredProceduresByUri.clear();
        registeredProceduresById.clear();
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
        closeSession(closeReason, null, true);
    }
    
    void onProtocolError() {
        onSessionError(
            new ApplicationError(ApplicationError.PROTCOL_ERROR),
            ApplicationError.PROTCOL_ERROR);
    }
    
    void onSessionError(ApplicationError error, String closeReason) {
        boolean reconnectAllowed = !stateController.clientConfig().closeClientOnErrors();
        if (!reconnectAllowed) {
            // Record the error that happened during the session
            stateController.setCloseError(error);
        }
        closeSession(error, closeReason, reconnectAllowed);
    }
    
    void onMessage(WampMessage msg) {
        if (msg instanceof WelcomeMessage) {
            onProtocolError();
        }
        else if (msg instanceof ChallengeMessage) {
            onProtocolError();
        }
        else if (msg instanceof AbortMessage) {
            onProtocolError();
        }
        else if (msg instanceof GoodbyeMessage) {
            // Reply the goodbye
            // We could also use the reason from the msg, but this would be harder
            // to determinate from a "real" error
            onSessionError(
                new ApplicationError(ApplicationError.GOODBYE_AND_OUT),
                ApplicationError.GOODBYE_AND_OUT);
        }
        else if (msg instanceof ResultMessage) {
            ResultMessage r = (ResultMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(r.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.CallMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(r.requestId);
            Reply reply = new Reply(r.arguments, r.argumentsKw);
            @SuppressWarnings("unchecked")
            AsyncSubject<Reply> subject = (AsyncSubject<Reply>)requestInfo.resultSubject;
            subject.onNext(reply);
            subject.onCompleted();
        }
        else if (msg instanceof ErrorMessage) {
            ErrorMessage r = (ErrorMessage)msg;
            if (r.requestType == WampMessages.CallMessage.ID
             || r.requestType == WampMessages.SubscribeMessage.ID
             || r.requestType == WampMessages.UnsubscribeMessage.ID
             || r.requestType == WampMessages.PublishMessage.ID
             || r.requestType == WampMessages.RegisterMessage.ID
             || r.requestType == WampMessages.UnregisterMessage.ID)
            {
                RequestMapEntry requestInfo = requestMap.get(r.requestId);
                if (requestInfo == null) return; // Ignore the error
                // Check whether the request type we sent equals the
                // request type for the error we receive
                if (requestInfo.requestType != r.requestType) {
                    onProtocolError();
                    return;
                }
                requestMap.remove(r.requestId);
                ApplicationError err = new ApplicationError(r.error, r.arguments, r.argumentsKw);
                requestInfo.resultSubject.onError(err);
            }
        }
        else if (msg instanceof SubscribedMessage) {
            SubscribedMessage m = (SubscribedMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(m.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.SubscribeMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(m.requestId);
            @SuppressWarnings("unchecked")
            AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
            subject.onNext(m.subscriptionId);
            subject.onCompleted();
        }
        else if (msg instanceof UnsubscribedMessage) {
            UnsubscribedMessage m = (UnsubscribedMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(m.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.UnsubscribeMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(m.requestId);
            @SuppressWarnings("unchecked")
            AsyncSubject<Void> subject = (AsyncSubject<Void>)requestInfo.resultSubject;
            subject.onNext(null);
            subject.onCompleted();
        }
        else if (msg instanceof EventMessage) {
            EventMessage ev = (EventMessage)msg;
            SubscriptionMapEntry entry = subscriptionsBySubscriptionId.get(ev.subscriptionId);
            if (entry == null || entry.state != PubSubState.Subscribed) return; // Ignore the result
            PubSubData evResult = new PubSubData(ev.details, ev.arguments, ev.argumentsKw);
            // publish the event
            for (Subscriber<? super PubSubData> s : entry.subscribers) {
                s.onNext(evResult);
            }
        }
        else if (msg instanceof PublishedMessage) {
            PublishedMessage m = (PublishedMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(m.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.PublishMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(m.requestId);
            @SuppressWarnings("unchecked")
            AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
            subject.onNext(m.publicationId);
            subject.onCompleted();
        }
        else if (msg instanceof RegisteredMessage) {
            RegisteredMessage m = (RegisteredMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(m.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.RegisterMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(m.requestId);
            @SuppressWarnings("unchecked")
            AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
            subject.onNext(m.registrationId);
            subject.onCompleted();
        }
        else if (msg instanceof UnregisteredMessage) {
            UnregisteredMessage m = (UnregisteredMessage)msg;
            RequestMapEntry requestInfo = requestMap.get(m.requestId);
            if (requestInfo == null) return; // Ignore the result
            if (requestInfo.requestType != WampMessages.UnregisterMessage.ID) {
                onProtocolError();
                return;
            }
            requestMap.remove(m.requestId);
            @SuppressWarnings("unchecked")
            AsyncSubject<Void> subject = (AsyncSubject<Void>)requestInfo.resultSubject;
            subject.onNext(null);
            subject.onCompleted();
        }
        else if (msg instanceof InvocationMessage) {
            InvocationMessage m = (InvocationMessage)msg;
            RegisteredProceduresMapEntry entry = registeredProceduresById.get(m.registrationId);
            if (entry == null || entry.state != RegistrationState.Registered) {
                // Send an error that we are no longer registered
                connectionController.sendMessage(
                    new ErrorMessage(InvocationMessage.ID, m.requestId, null,
                                     ApplicationError.NO_SUCH_PROCEDURE, null, null),
                    IWampConnectionPromise.Empty);
            }
            else {
                // Send the request to the subscriber, which can then send responses
                Request request = new Request(stateController, this, m.requestId, m.arguments, m.argumentsKw, m.details);
                entry.subscriber.onNext(request);
            }
        }
        else {
            // Unknown message
        }
    }
    
    public void performPublish(final String topic, final EnumSet<PublishFlags> flags, final ArrayNode arguments,
        final ObjectNode argumentsKw, AsyncSubject<Long> resultSubject)
    {
        final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
        lastRequestId = requestId;

        ObjectNode options = stateController.clientConfig().objectMapper().createObjectNode();
        if (flags != null && flags.contains(PublishFlags.DontExcludeMe)) {
            options.put("exclude_me", false);
        }
        
        if (flags != null && flags.contains(PublishFlags.RequireAcknowledge)) {
            // An acknowledge from the router in the form of a PUBLISHED or ERROR message
            // is expected. The request is stored in the requestMap and the resultSubject will be
            // completed once a response was received.
            options.put("acknowledge", true);
            requestMap.put(requestId, new RequestMapEntry(WampMessages.PublishMessage.ID, resultSubject));
        } else {
            // No acknowledge will be sent from the router.
            // Treat the publish as a success
            resultSubject.onNext(0L);
            resultSubject.onCompleted();
        }

        final WampMessages.PublishMessage msg =
            new WampMessages.PublishMessage(requestId, options, topic, arguments, argumentsKw);
        
        connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
    }
    
    public void performCall(final String procedure,
            final EnumSet<CallFlags> flags,
            final ArrayNode arguments,
            final ObjectNode argumentsKw,
            final AsyncSubject<Reply> resultSubject)
    {
        final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
        lastRequestId = requestId;
        
        ObjectNode options = stateController.clientConfig().objectMapper().createObjectNode();
        
        boolean discloseMe = flags != null && flags.contains(CallFlags.DiscloseMe) ? true : false;
        if (discloseMe) {
            options.put("disclose_me", discloseMe);
        }
        
        final CallMessage callMsg = new CallMessage(requestId, options, procedure, 
                                              arguments, argumentsKw);
        
        requestMap.put(requestId, new RequestMapEntry(CallMessage.ID, resultSubject));
        connectionController.sendMessage(callMsg, IWampConnectionPromise.Empty);
    }
    
    public void performRegisterProcedure(final String topic, final Subscriber<? super Request> subscriber) {
        // Check if we have already registered a function with the same name
        final RegisteredProceduresMapEntry entry = registeredProceduresByUri.get(topic);
        if (entry != null) {
            subscriber.onError(
                new ApplicationError(ApplicationError.PROCEDURE_ALREADY_EXISTS));
            return;
        }
        
        // Insert a new entry in the subscription map
        final RegisteredProceduresMapEntry newEntry = 
            new RegisteredProceduresMapEntry(subscriber, RegistrationState.Registering);
        registeredProceduresByUri.put(topic, newEntry);

        // Make the subscribe call
        final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
        lastRequestId = requestId;
        final RegisterMessage msg = new RegisterMessage(requestId, null, topic);

        final AsyncSubject<Long> registerFuture = AsyncSubject.create();
        registerFuture
        .observeOn(stateController.rxScheduler())
        .subscribe(new Action1<Long>() {
            @Override
            public void call(Long t1) {
                // Check if we were unsubscribed (through transport close)
                if (newEntry.state != RegistrationState.Registering) return;
                // Registration at the broker was successful
                newEntry.state = RegistrationState.Registered;
                newEntry.registrationId = t1;
                registeredProceduresById.put(t1, newEntry);
                // Add the cancellation functionality to the subscriber
                attachCancelRegistrationAction(subscriber, newEntry, topic);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                // Error on registering
                if (newEntry.state != RegistrationState.Registering) return;
                // Remark: Actually noone can't unregister until this Future completes because
                // the unregister functionality is only added in the success case
                // However a transport close event could set us to Unregistered early
                newEntry.state = RegistrationState.Unregistered;

                boolean isClosed = false;
                if (t1 instanceof ApplicationError &&
                        ((ApplicationError)t1).uri().equals(ApplicationError.TRANSPORT_CLOSED))
                    isClosed = true;
                
                if (isClosed) subscriber.onCompleted();
                else subscriber.onError(t1);

                registeredProceduresByUri.remove(topic);
            }
        });

        requestMap.put(requestId, 
            new RequestMapEntry(RegisterMessage.ID, registerFuture));
        connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
    }
    
    /**
     * Add an action that is added to the subscriber which is executed
     * if unsubscribe is called on a registered procedure.<br>
     * This action will lead to unregistering a provided function at the dealer.
     */
    private void attachCancelRegistrationAction(final Subscriber<? super Request> subscriber,
                                                final RegisteredProceduresMapEntry mapEntry,
                                                final String topic)
    {
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                stateController.scheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mapEntry.state != RegistrationState.Registered) return;
                        
                        mapEntry.state = RegistrationState.Unregistering;
                        registeredProceduresByUri.remove(topic);
                        registeredProceduresById.remove(mapEntry.registrationId);
                        
                        // Make the unregister call
                        final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
                        lastRequestId = requestId;
                        final UnregisterMessage msg = new UnregisterMessage(requestId, mapEntry.registrationId);
                        
                        final AsyncSubject<Void> unregisterFuture = AsyncSubject.create();
                        unregisterFuture
                        .observeOn(stateController.rxScheduler())
                        .subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void t1) {
                                // Unregistration at the broker was successful
                                mapEntry.state = RegistrationState.Unregistered;
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable t1) {
                                // Error on unregister
                            }
                        });
                        
                        requestMap.put(requestId, new RequestMapEntry(
                            UnregisterMessage.ID, unregisterFuture));
                        connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
                    }
                });
            }
        }));
    }
    
    public void performSubscription(final String topic,
            final SubscriptionFlags flags, final Subscriber<? super PubSubData> subscriber) {
        // Check if we are already subscribed at the dealer
        final SubscriptionMapEntry entry = subscriptionsByFlags.get(flags).get(topic);
        if (entry != null) { // We are already subscribed at the dealer
            entry.subscribers.add(subscriber);
            if (entry.state == PubSubState.Subscribed) {
                // Add the cancellation functionality only if we are
                // already subscribed. If not then this will be added
                // once subscription is completed
                attachPubSubCancellationAction(subscriber, entry, topic);
            }
        }
        else { // need to subscribe
            // Insert a new entry in the subscription map
            final SubscriptionMapEntry newEntry = new SubscriptionMapEntry(flags, PubSubState.Subscribing);
            newEntry.subscribers.add(subscriber);
            subscriptionsByFlags.get(flags).put(topic, newEntry);

            // Make the subscribe call
            final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
            lastRequestId = requestId;

            ObjectNode options = null;
            if (flags != SubscriptionFlags.Exact) {
                options = stateController.clientConfig().objectMapper().createObjectNode();
                options.put("match", flags.name().toLowerCase());
            }
            final SubscribeMessage msg = new SubscribeMessage(requestId, options, topic);

            final AsyncSubject<Long> subscribeFuture = AsyncSubject.create();
            subscribeFuture
            .observeOn(stateController.rxScheduler())
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long t1) {
                    // Check if we were unsubscribed (through transport close)
                    if (newEntry.state != PubSubState.Subscribing) return;
                    // Subscription at the broker was successful
                    newEntry.state = PubSubState.Subscribed;
                    newEntry.subscriptionId = t1;
                    subscriptionsBySubscriptionId.put(t1, newEntry);
                    // Add the cancellation functionality to all subscribers
                    // If one is already unsubscribed this will immediately call
                    // the cancellation function for this subscriber
                    for (Subscriber<? super PubSubData> s : newEntry.subscribers) {
                        attachPubSubCancellationAction(s, newEntry, topic);
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable t1) {
                    // Error on subscription
                    if (newEntry.state != PubSubState.Subscribing) return;
                    // Remark: Actually noone can't unsubscribe until this Future completes because
                    // the unsubscription functionality is only added in the success case
                    // However a transport close event could set us to Unsubscribed early
                    newEntry.state = PubSubState.Unsubscribed;

                    boolean isClosed = false;
                    if (t1 instanceof ApplicationError &&
                            ((ApplicationError)t1).uri().equals(ApplicationError.TRANSPORT_CLOSED))
                        isClosed = true;

                    for (Subscriber<? super PubSubData> s : newEntry.subscribers) {
                        if (isClosed) s.onCompleted();
                        else s.onError(t1);
                    }

                    newEntry.subscribers.clear();
                    subscriptionsByFlags.get(flags).remove(topic);
                }
            });

            requestMap.put(requestId, 
                    new RequestMapEntry(SubscribeMessage.ID, 
                            subscribeFuture));
            connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
        }
    }
    
    /**
     * Add an action that is added to the subscriber which is executed
     * if unsubscribe is called. This action will lead to the unsubscription at the
     * broker once the topic subscription at the broker is no longer used by anyone.
     */
    private void attachPubSubCancellationAction(final Subscriber<? super PubSubData> subscriber,
                                                final SubscriptionMapEntry mapEntry,
                                                final String topic)
    {
        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (stateController.scheduler().isShutdown()) {
                    return;
                }
                stateController.scheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        mapEntry.subscribers.remove(subscriber);
                        if (mapEntry.state == PubSubState.Subscribed &&
                            mapEntry.subscribers.size() == 0) 
                        {
                            // We removed the last subscriber and can therefore unsubscribe from the dealer
                            mapEntry.state = PubSubState.Unsubscribing;
                            subscriptionsByFlags.get(mapEntry.flags).remove(topic);
                            subscriptionsBySubscriptionId.remove(mapEntry.subscriptionId);
                            
                            // Make the unsubscribe call
                            final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
                            lastRequestId = requestId;
                            final UnsubscribeMessage msg = 
                                    new UnsubscribeMessage(requestId, mapEntry.subscriptionId);
                            
                            final AsyncSubject<Void> unsubscribeFuture = AsyncSubject.create();
                            unsubscribeFuture
                            .observeOn(stateController.rxScheduler())
                            .subscribe(new Action1<Void>() {
                                @Override
                                public void call(Void t1) {
                                    // Unsubscription at the broker was successful
                                    mapEntry.state = PubSubState.Unsubscribed;
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable t1) {
                                    // Error on unsubscription
                                }
                            });
                            
                            requestMap.put(requestId, new RequestMapEntry(
                                UnsubscribeMessage.ID, unsubscribeFuture));
                            connectionController.sendMessage(msg, IWampConnectionPromise.Empty);
                        }
                    }
                });
            }
        }));
    }
}