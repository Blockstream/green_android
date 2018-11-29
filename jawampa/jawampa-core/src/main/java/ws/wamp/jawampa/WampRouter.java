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

package ws.wamp.jawampa;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import ws.wamp.jawampa.WampMessages.*;
import ws.wamp.jawampa.connection.ICompletionCallback;
import ws.wamp.jawampa.connection.IConnectionController;
import ws.wamp.jawampa.connection.IWampConnection;
import ws.wamp.jawampa.connection.IWampConnectionAcceptor;
import ws.wamp.jawampa.connection.IWampConnectionFuture;
import ws.wamp.jawampa.connection.IWampConnectionListener;
import ws.wamp.jawampa.connection.IWampConnectionPromise;
import ws.wamp.jawampa.connection.QueueingConnectionController;
import ws.wamp.jawampa.connection.WampConnectionPromise;
import ws.wamp.jawampa.internal.IdGenerator;
import ws.wamp.jawampa.internal.IdValidator;
import ws.wamp.jawampa.internal.RealmConfig;
import ws.wamp.jawampa.internal.UriValidator;
import ws.wamp.jawampa.internal.Version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The {@link WampRouter} provides Dealer and Broker functionality for the WAMP
 * protocol.<br>
 */
public class WampRouter {
    
    final static Set<WampRoles> SUPPORTED_CLIENT_ROLES;
    static {
        SUPPORTED_CLIENT_ROLES = new HashSet<WampRoles>();
        SUPPORTED_CLIENT_ROLES.add(WampRoles.Caller);
        SUPPORTED_CLIENT_ROLES.add(WampRoles.Callee);
        SUPPORTED_CLIENT_ROLES.add(WampRoles.Publisher);
        SUPPORTED_CLIENT_ROLES.add(WampRoles.Subscriber);
    }
    
    /** Represents a realm that is exposed through the router */
    static class Realm {
        final RealmConfig config;
        final ObjectNode welcomeDetails;
        final Map<Long, ClientHandler> channelsBySessionId = new HashMap<Long, ClientHandler>();
        final Map<String, Procedure> procedures = new HashMap<String, Procedure>();
        
        // Fields that are used for implementing subscription functionality
        final EnumMap<SubscriptionFlags, Map<String, Subscription>> subscriptionsByFlags
                = new EnumMap<SubscriptionFlags, Map<String, Subscription>>(SubscriptionFlags.class);
        final Map<Long, Subscription> subscriptionsById = new HashMap<Long, Subscription>();
        long lastUsedSubscriptionId = IdValidator.MIN_VALID_ID;
        
        public Realm(RealmConfig config) {
            this.config = config;
            subscriptionsByFlags.put(SubscriptionFlags.Exact, new HashMap<String, Subscription>());
            subscriptionsByFlags.put(SubscriptionFlags.Prefix, new HashMap<String, Subscription>());
            subscriptionsByFlags.put(SubscriptionFlags.Wildcard, new HashMap<String, Subscription>());

            // Expose the roles that are configured for the realm
            ObjectMapper objectMapper = new ObjectMapper();
            welcomeDetails = objectMapper.createObjectNode();
            welcomeDetails.put("agent", Version.getVersion());
            ObjectNode routerRoles = welcomeDetails.putObject("roles");
            for (WampRoles role : config.roles) {
                ObjectNode roleNode = routerRoles.putObject(role.toString());
                if (role == WampRoles.Publisher) {
                    ObjectNode featuresNode = roleNode.putObject("features");
                    featuresNode.put("publisher_exclusion", true);
                } else if (role == WampRoles.Subscriber) {
                    ObjectNode featuresNode = roleNode.putObject("features");
                    featuresNode.put("pattern_based_subscription", true);
                }
            }
        }
        
        void includeChannel(ClientHandler channel, long sessionId, Set<WampRoles> roles) {
            channel.realm = this;
            channel.sessionId = sessionId;
            channel.roles = roles;
            channelsBySessionId.put(sessionId, channel);
        }
        
        void removeChannel(ClientHandler channel, boolean removeFromList) {
            if (channel.realm == null) return;
            
            if (channel.subscriptionsById != null) {
                // Remove the channels subscriptions from our subscription table
                for (Subscription sub : channel.subscriptionsById.values()) {
                    sub.subscribers.remove(channel);
                    if (sub.subscribers.isEmpty()) {
                        // Subscription is no longer used by any client
                        subscriptionsByFlags.get(sub.flags).remove(sub.topic);
                        subscriptionsById.remove(sub.subscriptionId);
                    }
                }
                channel.subscriptionsById.clear();
                channel.subscriptionsById = null;
            }
            
            if (channel.providedProcedures != null) {
                // Remove the clients procedures from our procedure table
                for (Procedure proc : channel.providedProcedures.values()) {
                    // Clear all pending invocations and thereby inform other clients 
                    // that the proc has gone away
                    for (Invocation invoc : proc.pendingCalls) {
                        if (invoc.caller.state != RouterHandlerState.Open) continue;
                        ErrorMessage errMsg = new ErrorMessage(CallMessage.ID, invoc.callRequestId, 
                            null, ApplicationError.NO_SUCH_PROCEDURE, null, null);
                        invoc.caller.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                    }
                    proc.pendingCalls.clear();
                    // Remove the procedure from the realm
                    procedures.remove(proc.procName);
                }
                channel.providedProcedures = null;
                channel.pendingInvocations = null;
            }

            if (removeFromList) {
                channelsBySessionId.remove(channel.sessionId);
            }
            channel.realm = null;
            channel.roles.clear();
            channel.roles = null;
            channel.sessionId = 0;
        }
    }
    
    static class Procedure {
        final String procName;
        final ClientHandler provider;
        final long registrationId;
        final List<Invocation> pendingCalls = new ArrayList<WampRouter.Invocation>();
        
        public Procedure(String name, ClientHandler provider, long registrationId) {
            this.procName = name;
            this.provider = provider;
            this.registrationId = registrationId;
        }
    }
    
    static class Invocation {
        Procedure procedure;
        long callRequestId;
        ClientHandler caller;
        long invocationRequestId;
    }
    
    static class Subscription {
        final String topic;
        final SubscriptionFlags flags;
        final String components[]; // non-null only for wildcard type
        final long subscriptionId;
        final Set<ClientHandler> subscribers;
        
        public Subscription(String topic, SubscriptionFlags flags, long subscriptionId) {
            this.topic = topic;
            this.flags = flags;
            this.components = flags == SubscriptionFlags.Wildcard ? topic.split("\\.", -1) : null;
            this.subscriptionId = subscriptionId;
            this.subscribers = new HashSet<ClientHandler>();
        }
    }
    
    final ScheduledExecutorService eventLoop;
    final Scheduler scheduler;
    
    final ObjectMapper objectMapper = new ObjectMapper();
    
    boolean isDisposed = false;
    AsyncSubject<Void> closedFuture = AsyncSubject.create();
    
    final Map<String, Realm> realms;
    final Set<IConnectionController> idleChannels;
    
    /** The number of connections that have to be closed. This is important for shutdown */
    int connectionsToClose = 0;
    
    /**
     * Returns the (singlethreaded) EventLoop on which this router is running.<br>
     * This is required by other Netty ChannelHandlers that want to forward messages
     * to the router.
     */
    public ScheduledExecutorService eventLoop() {
        return eventLoop;
    }
    
    /**
     * Returns the Jackson {@link ObjectMapper} that is used for JSON serialization,
     * deserialization and object mapping by this router.
     */
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    WampRouter(Map<String, RealmConfig> realms) {
        
        // Populate the realms from the configuration
        this.realms = new HashMap<String, Realm>();
        for (Map.Entry<String, RealmConfig> e : realms.entrySet()) {
            Realm info = new Realm(e.getValue());
            this.realms.put(e.getKey(), info);
        }
        
        // Create an eventloop and the RX scheduler on top of it
        this.eventLoop = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "WampRouterEventLoop");
                t.setDaemon(true);
                return t;
            }
        });
        this.scheduler = Schedulers.from(eventLoop);

        idleChannels = new HashSet<IConnectionController>();
    }
    
    /**
     * Tries to schedule a runnable on the underlying executor.<br>
     * Rejected executions will be suppressed.<br>
     * This is useful for cases when the clients EventLoop is shut down before
     * the EventLoop of the underlying connection.
     * 
     * @param action The action to schedule.
     */
    void tryScheduleAction(Runnable action) {
        try {
            eventLoop.submit(action);
        } catch (RejectedExecutionException e) {}
    }
    
    private ICompletionCallback<Void> onConnectionClosed = new ICompletionCallback<Void>() {
        @Override
        public void onCompletion(IWampConnectionFuture<Void> future) {
            tryScheduleAction(new Runnable() {
                @Override
                public void run() {
                    connectionsToClose -= 1;
                    if (isDisposed && connectionsToClose == 0) {
                        eventLoop.shutdown();
                        closedFuture.onNext(null);
                        closedFuture.onCompleted();
                    }
                }
            });
        }
    };
    
    /**
     * Increases the number of connections to close and starts to asynchronously
     * close it. When this has happened {@link WampRouter#onConnectionClosed} will be called.
     */
    private void closeConnection(IConnectionController controller, boolean sendRemaining) {
        connectionsToClose += 1;
        WampConnectionPromise<Void> promise =
            new WampConnectionPromise<Void>(onConnectionClosed, null);
        controller.close(sendRemaining, promise);
    }
    
    /**
     * Closes the router.<br>
     * This will shut down all realm that are registered to the router.
     * All connections to clients on the realm will be closed.<br>
     * However pending calls will be completed through an error message
     * as far as possible.
     * @return Returns an observable that completes when the router is completely shut down.
     */
    public Observable<Void> close() {
        if (eventLoop.isShutdown()) return closedFuture;
        
        tryScheduleAction(new Runnable() {
            @Override
            public void run() {
                if (isDisposed) return;
                isDisposed = true;
                
                // Close all currently connected channels
                for (IConnectionController con : idleChannels) closeConnection(con, true);
                idleChannels.clear();
                
                for (Realm ri : realms.values()) {
                    for (ClientHandler channel : ri.channelsBySessionId.values()) {
                        ri.removeChannel(channel, false);
                        channel.markAsClosed();
                        GoodbyeMessage goodbye = new GoodbyeMessage(null, ApplicationError.SYSTEM_SHUTDOWN);
                        channel.controller.sendMessage(goodbye, IWampConnectionPromise.Empty);
                        closeConnection(channel.controller, true);
                    }
                    ri.channelsBySessionId.clear();
                }
                
                // close is asynchronous. It will wait until all connections are closed
                // Afterwards the eventLoop will be shutDown.
            }
        });
        
        return closedFuture;
    }
    
    enum RouterHandlerState {
        Open,
        Closed
    }
    
    IWampConnectionAcceptor connectionAcceptor = new IWampConnectionAcceptor() {
        @Override
        public IWampConnectionListener createNewConnectionListener() {
            ClientHandler newHandler = new ClientHandler();
            IConnectionController newController = new QueueingConnectionController(eventLoop, newHandler);
            newHandler.controller = newController;
            return newController;
        }

        @Override
        public void acceptNewConnection(final IWampConnection newConnection,
                final IWampConnectionListener connectionListener) {
            try {
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (connectionListener == null
                                || !(connectionListener instanceof QueueingConnectionController)
                                || newConnection == null) {
                            // This is always true if the transport provider does not manipulate the structure
                            // that was sent by the router
                            if (newConnection != null) newConnection.close(false, IWampConnectionPromise.Empty);
                            return;
                        }
                        QueueingConnectionController controller = (QueueingConnectionController)connectionListener;
                        controller.setConnection(newConnection);
                        
                        if (isDisposed) {
                            // Got an incoming connection after the router has already shut down.
                            // Therefore we close the connection
                            closeConnection(controller, false);
                        } else {
                            // Store the controller
                            idleChannels.add(controller);
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                // Close the connection
                // Defer the operation to avoid a cyclic call from the new connection
                // to this method and back
                Runnable r = new Runnable () {
                    @Override
                    public void run() {
                        newConnection.close(false, IWampConnectionPromise.Empty);
                    }
                };
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(r);
                executor.shutdown();
            }
        }
    };
    
    /**
     * Returns the {@link IWampConnectionAcceptor} interface that the router
     * provides in order to be able to accept new connection.
     */
    public IWampConnectionAcceptor connectionAcceptor() {
        return connectionAcceptor;
    }
    
    class ClientHandler implements IWampConnectionListener {
        
        IConnectionController controller;
        public RouterHandlerState state = RouterHandlerState.Open;
        long sessionId;
        Realm realm;
        Set<WampRoles> roles;
        
        /**
         * Procedures that this channel provides.<br>
         * Key is the registration ID, Value is the procedure
         */
        Map<Long, Procedure> providedProcedures;
        
        Map<Long, Invocation> pendingInvocations;
        
        /** The Set of subscriptions to which this channel is subscribed */
        Map<Long, Subscription> subscriptionsById;
        
        long lastUsedId = IdValidator.MIN_VALID_ID;
        
        void markAsClosed() {
            state = RouterHandlerState.Closed;
        }
        
        public ClientHandler() {
        }

        @Override
        public void transportClosed() {
            // Handle in the same way as a close due to an error
            transportError(null);
        }

        @Override
        public void transportError(Throwable cause) {
            if (isDisposed || state != RouterHandlerState.Open) return;
            if (realm != null) {
                closeActiveClient(ClientHandler.this, null);
            } else {
                closePassiveClient(ClientHandler.this);
            }
        }

        @Override
        public void messageReceived(final WampMessage message) {
            if (isDisposed || state != RouterHandlerState.Open) return;
            if (realm == null) {
                onMessageFromUnregisteredChannel(ClientHandler.this, message);
            } else {
                onMessageFromRegisteredChannel(ClientHandler.this, message);
            }
        }
    }
    
    private void onMessageFromRegisteredChannel(ClientHandler handler, WampMessage msg) {
        
        // TODO: Validate roles for all relevant messages
        
        if (msg instanceof HelloMessage || msg instanceof WelcomeMessage) {
            // The client sent hello but it was already registered -> This is an error
            // If the client sends welcome it's also an error
            closeActiveClient(handler, new GoodbyeMessage(null, ApplicationError.INVALID_ARGUMENT));
        } else if (msg instanceof AbortMessage || msg instanceof GoodbyeMessage) {
            // The client wants to leave the realm
            // Remove the channel from the realm
            handler.realm.removeChannel(handler, true);
            // But add it to the list of passive channels
            idleChannels.add(handler.controller);
            // Echo the message in case of goodbye
            if (msg instanceof GoodbyeMessage) {
                GoodbyeMessage reply = new GoodbyeMessage(null, ApplicationError.GOODBYE_AND_OUT);
                handler.controller.sendMessage(reply, IWampConnectionPromise.Empty);
            }
        } else if (msg instanceof CallMessage) {
            // The client wants to call a remote function
            // Verify the message
            CallMessage call = (CallMessage) msg;
            String err = null;
            if (!UriValidator.tryValidate(call.procedure, handler.realm.config.useStrictUriValidation)) {
                // Client sent an invalid URI
                err = ApplicationError.INVALID_URI;
            }
            
            if (err == null && !(IdValidator.isValidId(call.requestId))) {
                // Client sent an invalid request ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            Procedure proc = null;
            if (err == null) {
                proc = handler.realm.procedures.get(call.procedure);
                if (proc == null) err = ApplicationError.NO_SUCH_PROCEDURE;
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(CallMessage.ID, call.requestId, 
                    null, err, null, null);
                handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                return;
            }
            
            // Everything checked, we can forward the call to the provider
            Invocation invoc = new Invocation();
            invoc.callRequestId = call.requestId;
            invoc.caller = handler;
            invoc.procedure = proc;
            invoc.invocationRequestId = IdGenerator.newLinearId(proc.provider.lastUsedId,
                proc.provider.pendingInvocations);
            proc.provider.lastUsedId = invoc.invocationRequestId; 
            
            // Store the invocation
            proc.provider.pendingInvocations.put(invoc.invocationRequestId, invoc);
            // Store the call in the procedure to return error if client unregisters
            proc.pendingCalls.add(invoc);

            // And send it to the provider
            InvocationMessage imsg = new InvocationMessage(invoc.invocationRequestId,
                proc.registrationId, null, call.arguments, call.argumentsKw);
            proc.provider.controller.sendMessage(imsg, IWampConnectionPromise.Empty);
        } else if (msg instanceof YieldMessage) {
            // The clients sends as the result of an RPC
            // Verify the message
            YieldMessage yield = (YieldMessage) msg;
            if (!(IdValidator.isValidId(yield.requestId))) return;
            // Look up the invocation to find the original caller
            if (handler.pendingInvocations == null) return;  // If a client send a yield without an invocation, return
            Invocation invoc = handler.pendingInvocations.get(yield.requestId);
            if (invoc == null) return; // There is no invocation pending under this ID
            handler.pendingInvocations.remove(yield.requestId);
            invoc.procedure.pendingCalls.remove(invoc);
            // Send the result to the original caller
            ResultMessage result = new ResultMessage(invoc.callRequestId, null, yield.arguments, yield.argumentsKw);
            invoc.caller.controller.sendMessage(result, IWampConnectionPromise.Empty);
        } else if (msg instanceof ErrorMessage) {
            ErrorMessage err = (ErrorMessage) msg;
            if (!(IdValidator.isValidId(err.requestId))) {
                return;
            }
            if (err.requestType == InvocationMessage.ID) {
                if (!UriValidator.tryValidate(err.error, handler.realm.config.useStrictUriValidation)) {
                    // The Message provider has sent us an invalid URI for the error string
                    // We better don't forward it but instead close the connection, which will
                    // give the original caller an unknown message error
                    closeActiveClient(handler, new GoodbyeMessage(null, ApplicationError.INVALID_ARGUMENT));
                    return;
                }
                
                // Look up the invocation to find the original caller
                if (handler.pendingInvocations == null) return; // if an error is send before an invocation, do not do anything
                Invocation invoc = handler.pendingInvocations.get(err.requestId);
                if (invoc == null) return; // There is no invocation pending under this ID
                handler.pendingInvocations.remove(err.requestId);
                invoc.procedure.pendingCalls.remove(invoc);
                
                // Send the result to the original caller
                ErrorMessage fwdError = new ErrorMessage(CallMessage.ID, invoc.callRequestId, 
                    null, err.error, err.arguments, err.argumentsKw);
                invoc.caller.controller.sendMessage(fwdError, IWampConnectionPromise.Empty);
            }
            // else TODO: Are there any other possibilities where a client could return ERROR
        } else if (msg instanceof RegisterMessage) {
            // The client wants to register a procedure
            // Verify the message
            RegisterMessage reg = (RegisterMessage) msg;
            String err = null;
            if (!UriValidator.tryValidate(reg.procedure, handler.realm.config.useStrictUriValidation)) {
                // Client sent an invalid URI
                err = ApplicationError.INVALID_URI;
            }
            
            if (err == null && !(IdValidator.isValidId(reg.requestId))) {
                // Client sent an invalid request ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            Procedure proc = null;
            if (err == null) {
                proc = handler.realm.procedures.get(reg.procedure);
                if (proc != null) err = ApplicationError.PROCEDURE_ALREADY_EXISTS;
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(RegisterMessage.ID, reg.requestId, 
                    null, err, null, null);
                handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                return;
            }
            
            // Everything checked, we can register the caller as the procedure provider
            long registrationId = IdGenerator.newLinearId(handler.lastUsedId, handler.providedProcedures);
            handler.lastUsedId = registrationId;
            Procedure procInfo = new Procedure(reg.procedure, handler, registrationId);
            
            // Insert new procedure
            handler.realm.procedures.put(reg.procedure, procInfo);
            if (handler.providedProcedures == null) {
                handler.providedProcedures = new HashMap<Long, WampRouter.Procedure>();
                handler.pendingInvocations = new HashMap<Long, WampRouter.Invocation>();
            }
            handler.providedProcedures.put(procInfo.registrationId, procInfo);
            
            RegisteredMessage response = new RegisteredMessage(reg.requestId, procInfo.registrationId);
            handler.controller.sendMessage(response, IWampConnectionPromise.Empty);
        } else if (msg instanceof UnregisterMessage) {
            // The client wants to unregister a procedure
            // Verify the message
            UnregisterMessage unreg = (UnregisterMessage) msg;
            String err = null;
            if (!(IdValidator.isValidId(unreg.requestId))
             || !(IdValidator.isValidId(unreg.registrationId))) {
                // Client sent an invalid request or registration ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            Procedure proc = null;
            if (err == null) {
                if (handler.providedProcedures != null) {
                    proc = handler.providedProcedures.get(unreg.registrationId);
                }
                // Check whether the procedure exists AND if the caller is the owner
                // If the caller is not the owner it might be an attack, so we don't
                // disclose that the procedure exists.
                if (proc == null) {
                    err = ApplicationError.NO_SUCH_REGISTRATION;
                }
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(UnregisterMessage.ID, unreg.requestId, 
                    null, err, null, null);
                handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                return;
            }
            
            // Mark pending calls to this procedure as failed
            for (Invocation invoc : proc.pendingCalls) {
                handler.pendingInvocations.remove(invoc.invocationRequestId);
                if (invoc.caller.state == RouterHandlerState.Open) {
                    ErrorMessage errMsg = new ErrorMessage(CallMessage.ID, invoc.callRequestId, 
                        null, ApplicationError.NO_SUCH_PROCEDURE, null, null);
                    invoc.caller.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                }
            }
            proc.pendingCalls.clear();

            // Remove the procedure from the realm and the handler
            handler.realm.procedures.remove(proc.procName);
            handler.providedProcedures.remove(proc.registrationId);
            
            if (handler.providedProcedures.size() == 0) {
                handler.providedProcedures = null;
                handler.pendingInvocations = null;
            }
            
            // Send the acknowledge
            UnregisteredMessage response = new UnregisteredMessage(unreg.requestId);
            handler.controller.sendMessage(response, IWampConnectionPromise.Empty);
        } else if (msg instanceof SubscribeMessage) {
            // The client wants to subscribe to a procedure
            // Verify the message
            SubscribeMessage sub = (SubscribeMessage) msg;
            String err = null;

            // Find subscription match type
            SubscriptionFlags flags = SubscriptionFlags.Exact;
            if (sub.options != null) {
                JsonNode match = sub.options.get("match");
                if (match != null) {
                    String matchValue = match.asText();
                    if ("prefix".equals(matchValue)) {
                        flags = SubscriptionFlags.Prefix;
                    } else if ("wildcard".equals(matchValue)) {
                        flags = SubscriptionFlags.Wildcard;
                    }
                }
            }

            if (flags == SubscriptionFlags.Exact) {
               if (!UriValidator.tryValidate(sub.topic, handler.realm.config.useStrictUriValidation)) {
                   // Client sent an invalid URI
                   err = ApplicationError.INVALID_URI;
               }
            } else if (flags == SubscriptionFlags.Prefix) {
               if (!UriValidator.tryValidatePrefix(sub.topic, handler.realm.config.useStrictUriValidation)) {
                   // Client sent an invalid URI
                   err = ApplicationError.INVALID_URI;
               }
            } else if (flags == SubscriptionFlags.Wildcard) {
               if (!UriValidator.tryValidateWildcard(sub.topic, handler.realm.config.useStrictUriValidation)) {
                   // Client sent an invalid URI
                   err = ApplicationError.INVALID_URI;
               }
            }

            if (err == null && !(IdValidator.isValidId(sub.requestId))) {
                // Client sent an invalid request ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(SubscribeMessage.ID, sub.requestId, 
                    null, err, null, null);
                handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                return;
            }
            
            // Create a new subscription map for the client if it was not subscribed before
            if (handler.subscriptionsById == null) {
                handler.subscriptionsById = new HashMap<Long, WampRouter.Subscription>();
            }

            // Search if a subscription from any client on the realm to this topic exists
            Map<String, Subscription> subscriptionMap = handler.realm.subscriptionsByFlags.get(flags);
            Subscription subscription = subscriptionMap.get(sub.topic);
            if (subscription == null) {
                // No client was subscribed to this URI up to now
                // Create a new subscription id
                long subscriptionId = IdGenerator.newLinearId(handler.realm.lastUsedSubscriptionId,
                                                              handler.realm.subscriptionsById);
                handler.realm.lastUsedSubscriptionId = subscriptionId;
                // Create and add the new subscription
                subscription = new Subscription(sub.topic, flags, subscriptionId);
                subscriptionMap.put(sub.topic, subscription);
                handler.realm.subscriptionsById.put(subscriptionId, subscription);
            }

            // We check if the client is already subscribed to this topic by trying to add the
            // new client as a receiver. If the client is already a receiver we do nothing
            // (already subscribed and already stored in handler.subscriptionsById). Calling
            // add to check and add is more efficient than checking with contains first.
            // If the client was already subscribed this will return the same subscriptionId
            // than as for the last subscription.
            // See discussion in https://groups.google.com/forum/#!topic/wampws/kC878Ngc9Z0
            if (subscription.subscribers.add(handler)) {
                // Add the subscription on the client
                handler.subscriptionsById.put(subscription.subscriptionId, subscription);
            }
            
            SubscribedMessage response = new SubscribedMessage(sub.requestId, subscription.subscriptionId);
            handler.controller.sendMessage(response, IWampConnectionPromise.Empty);
        } else if (msg instanceof UnsubscribeMessage) {
            // The client wants to cancel a subscription
            // Verify the message
            UnsubscribeMessage unsub = (UnsubscribeMessage) msg;
            String err = null;
            if (!(IdValidator.isValidId(unsub.requestId))
             || !(IdValidator.isValidId(unsub.subscriptionId))) {
                // Client sent an invalid request or registration ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            Subscription s = null;
            if (err == null) {
                // Check whether such a subscription exists and fetch the topic name
                if (handler.subscriptionsById != null) {
                    s = handler.subscriptionsById.get(unsub.subscriptionId);
                }
                if (s == null) {
                    err = ApplicationError.NO_SUCH_SUBSCRIPTION;
                }
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(UnsubscribeMessage.ID, unsub.requestId, 
                    null, err, null, null);
                handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                return;
            }

            // Remove the channel as an receiver from the subscription
            s.subscribers.remove(handler);
            
            // Remove the subscription from the handler
            handler.subscriptionsById.remove(s.subscriptionId);
            if (handler.subscriptionsById.isEmpty()) {
                handler.subscriptionsById = null;
            }
            
            // Remove the subscription from the realm if no subscriber is left
            if (s.subscribers.isEmpty()) {
                handler.realm.subscriptionsByFlags.get(s.flags).remove(s.topic);
                handler.realm.subscriptionsById.remove(s.subscriptionId);
            }
            
            // Send the acknowledge
            UnsubscribedMessage response = new UnsubscribedMessage(unsub.requestId);
            handler.controller.sendMessage(response, IWampConnectionPromise.Empty);
        } else if (msg instanceof PublishMessage) {
            // The client wants to publish something to all subscribers (apart from himself)
            PublishMessage pub = (PublishMessage) msg;
            // Check whether the client wants an acknowledgement for the publication
            // Default is no
            boolean sendAcknowledge = false;
            JsonNode ackOption = pub.options.get("acknowledge");
            if (ackOption != null && ackOption.asBoolean() == true)
                sendAcknowledge = true;
            
            String err = null;
            if (!UriValidator.tryValidate(pub.topic, handler.realm.config.useStrictUriValidation)) {
                // Client sent an invalid URI
                err = ApplicationError.INVALID_URI;
            }
            
            if (err == null && !(IdValidator.isValidId(pub.requestId))) {
                // Client sent an invalid request ID
                err = ApplicationError.INVALID_ARGUMENT;
            }
            
            if (err != null) { // If we have an error send that to the client
                ErrorMessage errMsg = new ErrorMessage(PublishMessage.ID, pub.requestId, 
                    null, err, null, null);
                if (sendAcknowledge) {
                    handler.controller.sendMessage(errMsg, IWampConnectionPromise.Empty);
                }
                return;
            }
            
            long publicationId = IdGenerator.newRandomId(null); // Store that somewhere?

            // Get the subscriptions for this topic on the realm
            Subscription exactSubscription = handler.realm.subscriptionsByFlags.get(SubscriptionFlags.Exact).get(pub.topic);
            if (exactSubscription != null) {
                publishEvent(handler, pub, publicationId, exactSubscription);
            }

            Map<String, Subscription> prefixSubscriptionMap = handler.realm.subscriptionsByFlags.get(SubscriptionFlags.Prefix);
            for (Subscription prefixSubscription : prefixSubscriptionMap.values()) {
                if (pub.topic.startsWith(prefixSubscription.topic)) {
                    publishEvent(handler, pub, publicationId, prefixSubscription);
                }
            }

            Map<String, Subscription> wildcardSubscriptionMap = handler.realm.subscriptionsByFlags.get(SubscriptionFlags.Wildcard);
            String[] components = pub.topic.split("\\.", -1);
            for (Subscription wildcardSubscription : wildcardSubscriptionMap.values()) {
                boolean matched = true;
                if (components.length == wildcardSubscription.components.length) {
                    for (int i=0; i < components.length; i++) {
                        if (wildcardSubscription.components[i].length() > 0
                                && !components[i].equals(wildcardSubscription.components[i])) {
                            matched = false;
                            break;
                        }
                    }
                }else
                    matched = false;

                if (matched) {
                    publishEvent(handler, pub, publicationId, wildcardSubscription);
                }
            }

            if (sendAcknowledge) {
                PublishedMessage response = new PublishedMessage(pub.requestId, publicationId);
                handler.controller.sendMessage(response, IWampConnectionPromise.Empty);
            }
        }
    }

    private void publishEvent(ClientHandler publisher, PublishMessage pub, long publicationId, Subscription subscription){
        ObjectNode details = null;
        if (subscription.flags != SubscriptionFlags.Exact) {
            details = objectMapper.createObjectNode();
            details.put("topic", pub.topic);
        }

        EventMessage ev = new EventMessage(subscription.subscriptionId, publicationId,
                details, pub.arguments, pub.argumentsKw);

        for (ClientHandler receiver : subscription.subscribers) {
            if (receiver == publisher ) { // Potentially skip the publisher
                boolean skipPublisher = true;
                if (pub.options != null) {
                    JsonNode excludeMeNode = pub.options.get("exclude_me");
                    if (excludeMeNode != null) {
                        skipPublisher = excludeMeNode.asBoolean(true);
                    }
                }
                if (skipPublisher) continue;
            }

            // Publish the event to the subscriber
            receiver.controller.sendMessage(ev, IWampConnectionPromise.Empty);
        }
    }
    
    private void onMessageFromUnregisteredChannel(ClientHandler channelHandler, WampMessage msg)
    {
        // Only HELLO is allowed when a channel is not registered
        if (!(msg instanceof HelloMessage)) {
            // Close the connection
            closePassiveClient(channelHandler);
            return;
        }
        
        HelloMessage hello = (HelloMessage) msg;
        
        String errorMsg = null;
        Realm realm = null;
        if (!UriValidator.tryValidate(hello.realm, false)) {
            errorMsg = ApplicationError.INVALID_URI;
        } else {
            realm = realms.get(hello.realm);
            if (realm == null) {
                errorMsg = ApplicationError.NO_SUCH_REALM;
            }
        }
        
        if (errorMsg != null) {
            AbortMessage abort = new AbortMessage(null, errorMsg);
            channelHandler.controller.sendMessage(abort, IWampConnectionPromise.Empty);
            return;
        }
        
        Set<WampRoles> roles = new HashSet<WampRoles>();
        boolean hasUnsupportedRoles = false;
        
        JsonNode n = hello.details.get("roles");
        if (n != null && n.isObject()) {
            ObjectNode rolesNode = (ObjectNode) n;
            Iterator<String> roleKeys = rolesNode.fieldNames();
            while (roleKeys.hasNext()) {
                WampRoles role = WampRoles.fromString(roleKeys.next());
                if (!SUPPORTED_CLIENT_ROLES.contains(role)) hasUnsupportedRoles = true;
                if (role != null) roles.add(role);
            }
        }
        
        if (roles.size() == 0 || hasUnsupportedRoles) {
            AbortMessage abort = new AbortMessage(null, ApplicationError.NO_SUCH_ROLE);
            channelHandler.controller.sendMessage(abort, IWampConnectionPromise.Empty);
            return;
        }
        
        long sessionId = IdGenerator.newRandomId(realm.channelsBySessionId);

        // Include the channel into the realm
        realm.includeChannel(channelHandler, sessionId, roles);
        // Remove the channel from the idle channel list - It is no longer idle
        idleChannels.remove(channelHandler.controller);

        // Respond with the WELCOME message
        WelcomeMessage welcome = new WelcomeMessage(channelHandler.sessionId, realm.welcomeDetails);
        channelHandler.controller.sendMessage(welcome, IWampConnectionPromise.Empty);
    }
    
    private void closeActiveClient(ClientHandler channel, WampMessage closeMessage) {
        if (channel == null) return;
        
        channel.realm.removeChannel(channel, true);
        channel.markAsClosed();
        
        if (channel.controller != null) {
            if (closeMessage != null)
                channel.controller.sendMessage(closeMessage, IWampConnectionPromise.Empty);
            closeConnection(channel.controller, true);
        }
    }
    
    private void closePassiveClient(ClientHandler channelHandler) {
        idleChannels.remove(channelHandler.controller);
        channelHandler.markAsClosed();
        closeConnection(channelHandler.controller, false);
    }
}
