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

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Future;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.subjects.AsyncSubject;
import ws.wamp.jawampa.client.ClientConfiguration;
import ws.wamp.jawampa.client.SessionEstablishedState;
import ws.wamp.jawampa.client.StateController;
import ws.wamp.jawampa.internal.ArgArrayBuilder;
import ws.wamp.jawampa.internal.Promise;
import ws.wamp.jawampa.internal.UriValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides the client-side functionality for WAMP.<br>
 * The {@link WampClient} allows to make remote procedure calls, subscribe to
 * and publish events and to register functions for RPC.<br>
 * It has to be constructed through a {@link WampClientBuilder} and can not
 * directly be instantiated.
 */
public class WampClient {
    
    /** Base type for all possible client states */
    public interface State {
    }
    
    /** The session is not connected */
    public static class DisconnectedState implements State {
        private final Throwable disconnectReason;
        
        public DisconnectedState(Throwable closeReason) {
            this.disconnectReason = closeReason;
        }
        
        /**
         * Returns an optional reason that describes why the client got
         * disconnected from the server. This can be null if the client
         * requested the disconnect or if the client was never connected
         * to a server.
         */
        public Throwable disconnectReason() {
            return disconnectReason;
        }
        
        @Override
        public String toString() {
            return "Disconnected";
        }
    }
    
    /** The session is trying to connect to the router */
    public static class ConnectingState implements State {
        @Override
        public String toString() {
            return "Connecting";
        }
    }
    
    /**
     * The client is connected to the router and the session was established
     */
    public static class ConnectedState implements State {
        
        private final long sessionId;
        private final ObjectNode welcomeDetails;
        private final EnumSet<WampRoles> routerRoles;
        
        public ConnectedState(long sessionId, ObjectNode welcomeDetails, EnumSet<WampRoles> routerRoles) {
            this.sessionId = sessionId;
            this.welcomeDetails = welcomeDetails;
            this.routerRoles = routerRoles;
        }
        
        /** Returns the sessionId that was assigned to the client by the router */
        public long sessionId() {
            return sessionId;
        }
        
        /**
         * Returns the details of the welcome message that was sent from the router
         * to the client
         */
        public ObjectNode welcomeDetails() {
            return welcomeDetails.deepCopy();
        }
        
        /**
         * Returns the roles that the router implements
         */
        public Set<WampRoles> routerRoles() {
            return EnumSet.copyOf(routerRoles);
        }
        
        @Override
        public String toString() {
            return "Connected";
        }
    }
    
    final StateController stateController;
    final ClientConfiguration clientConfig;
    
    /** Returns the URI of the router to which this client is connected */
    public URI routerUri() {
        return clientConfig.routerUri();
    }

    /** Returns the name of the realm on the router */
    public String realm() {
        return clientConfig.realm();
    }   
    
    WampClient(ClientConfiguration clientConfig)
    {
        this.clientConfig = clientConfig;
        // Create a new stateController
        this.stateController = new StateController(clientConfig);
    }

    /**
     * Opens the session<br>
     * This should be called after a subscription on {@link #statusChanged}
     * was installed.<br>
     * If the session was already opened this has no effect besides
     * resetting the reconnect counter.<br>
     * If the session was already closed through a call to {@link #close}
     * no new connect attempt will be performed.
     */
    public void open() {
        stateController.open();
    }

    /**
     * Closes the session.<br>
     * It will not be possible to open the session again with {@link #open} for safety
     * reasons. If a new session is required a new {@link WampClient} should be built
     * through the used {@link WampClientBuilder}.
     */
    public Observable<Void> close() {
        stateController.initClose();
        return getTerminationObservable();
    }

    /**
     * An Observable that allows to monitor the connection status of the Session.
     */
    public Observable<State> statusChanged() {
        return stateController.statusObservable();
    }
    
    /**
     * Publishes an event under the given topic.
     * @param topic The topic that should be used for publishing the event
     * @param args A list of all positional arguments of the event to publish.
     * These will be get serialized according to the Jackson library serializing
     * behavior.
     * @return An observable that provides a notification whether the event
     * publication was successful. This contains either a single value (the
     * publication ID) and will then be completed or will be completed with
     * an error if the event could not be published.
     */
    public Observable<Long> publish(final String topic, Object... args) {
        return publish(topic, ArgArrayBuilder.buildArgumentsArray(clientConfig.objectMapper(), args), null);
    }

    /**
     * Publishes an event under the given topic.
     * @param topic The topic that should be used for publishing the event
     * @param event The event to publish
     * @return An observable that provides a notification whether the event
     * publication was successful. This contains either a single value (the
     * publication ID) and will then be completed or will be completed with
     * an error if the event could not be published.
     */
    public Observable<Long> publish(final String topic, PubSubData event) {
        if (event != null)
            return publish(topic, event.arguments, event.keywordArguments);
        else
            return publish(topic, null, null);
    }

    /**
     * Publishes an event under the given topic.
     * @param topic The topic that should be used for publishing the event
     * @param arguments The positional arguments for the published event
     * @param argumentsKw The keyword arguments for the published event.
     * These will only be taken into consideration if arguments is not null.
     * @return An observable that provides a notification whether the event
     * publication was successful. This contains either a single value (the
     * publication ID) and will then be completed or will be completed with
     * an error if the event could not be published.
     */
    public Observable<Long> publish(final String topic, final ArrayNode arguments, final ObjectNode argumentsKw)
    {
        return publish(topic, null, arguments, argumentsKw);
    }

    /**
     * Publishes an event under the given topic.
     * @param topic The topic that should be used for publishing the event
     * @param flags Additional publish flags if any. This can be null.
     * @param arguments The positional arguments for the published event
     * @param argumentsKw The keyword arguments for the published event.
     * These will only be taken into consideration if arguments is not null.
     * @return An observable that provides a notification whether the event
     * publication was successful. This contains either a single value (the
     * publication ID) and will then be completed or will be completed with
     * an error if the event could not be published.
     */
    public Observable<Long> publish(final String topic, final EnumSet<PublishFlags> flags, final ArrayNode arguments,
        final ObjectNode argumentsKw)
    {
        final AsyncSubject<Long> resultSubject = AsyncSubject.create();
        
        try {
            UriValidator.validate(topic, clientConfig.useStrictUriValidation());
        }
        catch (WampError e) {
            resultSubject.onError(e);
            return resultSubject;
        }
         
        stateController.scheduler().execute(new Runnable() {
            @Override
            public void run() {
                if (!(stateController.currentState() instanceof SessionEstablishedState)) {
                    resultSubject.onError(new ApplicationError(ApplicationError.NOT_CONNECTED));
                    return;
                }
                // Forward publish into the session
                SessionEstablishedState curState = (SessionEstablishedState)stateController.currentState();
                curState.performPublish(topic, flags, arguments, argumentsKw, resultSubject);
                
            }
        });
        return resultSubject;
    }
    
    /**
     * Registers a procedure at the router which will afterwards be available
     * for remote procedure calls from other clients.<br>
     * The actual registration will only happen after the user subscribes on
     * the returned Observable. This guarantees that no RPC requests get lost.
     * Incoming RPC requests will be pushed to the Subscriber via it's
     * onNext method. The Subscriber can send responses through the methods on
     * the {@link Request}.<br>
     * If the client no longer wants to provide the method it can call
     * unsubscribe() on the Subscription to unregister the procedure.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The name of the procedure which this client wants to
     * provide.<br>
     * Must be valid WAMP URI.
     * @return An observable that can be used to provide a procedure.
     */
    public Observable<Request> registerProcedure(final String topic) {
        return Observable.create(new OnSubscribe<Request>() {
            @Override
            public void call(final Subscriber<? super Request> subscriber) {
                try {
                    UriValidator.validate(topic, clientConfig.useStrictUriValidation());
                }
                catch (WampError e) {
                    subscriber.onError(e);
                    return;
                }

                stateController.scheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        // If the Subscriber unsubscribed in the meantime we return early
                        if (subscriber.isUnsubscribed()) return;
                        // Set subscription to completed if we are not connected
                        if (!(stateController.currentState() instanceof SessionEstablishedState)) {
                            subscriber.onCompleted();
                            return;
                        }
                        // Forward publish into the session
                        SessionEstablishedState curState = (SessionEstablishedState)stateController.currentState();
                        curState.performRegisterProcedure(topic, subscriber);
                    }
                });
            }
        });
    }
    
    /**
     * Returns an observable that allows to subscribe on the given topic.<br>
     * The actual subscription will only be made after subscribe() was called
     * on it.<br>
     * This version of makeSubscription will automatically transform the
     * received events data into the type eventClass and will therefore return
     * a mapped Observable. It will only look at and transform the first
     * argument of the received events arguments, therefore it can only be used
     * for events that carry either a single or no argument.<br>
     * Received publications will be pushed to the Subscriber via it's
     * onNext method.<br>
     * The client can unsubscribe from the topic by calling unsubscribe() on
     * it's Subscription.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The topic to subscribe on.<br>
     * Must be valid WAMP URI.
     * @param eventClass The class type into which the received event argument
     * should be transformed. E.g. use String.class to let the client try to
     * transform the first argument into a String and let the return value of
     * of the call be Observable&lt;String&gt;.
     * @return An observable that can be used to subscribe on the topic.
     */
    public <T> Observable<T> makeSubscription(final String topic, final Class<T> eventClass) {
        return makeSubscription(topic, SubscriptionFlags.Exact, eventClass);
    }

    /**
     * Returns an observable that allows to subscribe on the given topic.<br>
     * The actual subscription will only be made after subscribe() was called
     * on it.<br>
     * This version of makeSubscription will automatically transform the
     * received events data into the type eventClass and will therefore return
     * a mapped Observable. It will only look at and transform the first
     * argument of the received events arguments, therefore it can only be used
     * for events that carry either a single or no argument.<br>
     * Received publications will be pushed to the Subscriber via it's
     * onNext method.<br>
     * The client can unsubscribe from the topic by calling unsubscribe() on
     * it's Subscription.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The topic to subscribe on.<br>
     * Must be valid WAMP URI.
     * @param flags Flags to indicate type of subscription. This cannot be null.
     * @param eventClass The class type into which the received event argument
     * should be transformed. E.g. use String.class to let the client try to
     * transform the first argument into a String and let the return value of
     * of the call be Observable&lt;String&gt;.
     * @return An observable that can be used to subscribe on the topic.
     */
    public <T> Observable<T> makeSubscription(final String topic, SubscriptionFlags flags, final Class<T> eventClass)
    {
        return makeSubscription(topic, flags).map(new Func1<PubSubData,T>() {
            @Override
            public T call(PubSubData ev) {
                if (eventClass == null || eventClass == Void.class) {
                    // We don't need a value
                    return null;
                }

                if (ev.arguments == null || ev.arguments.size() < 1)
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_VALUE));

                JsonNode eventNode = ev.arguments.get(0);
                if (eventNode.isNull()) return null;

                T eventValue;
                try {
                    eventValue = clientConfig.objectMapper().convertValue(eventNode, eventClass);
                } catch (IllegalArgumentException e) {
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.INVALID_VALUE_TYPE));
                }
                return eventValue;
            }
        });
    }
    
    /**
     * Returns an observable that allows to subscribe on the given topic.<br>
     * The actual subscription will only be made after subscribe() was called
     * on it.<br>
     * makeSubscriptionWithDetails will automatically transform the
     * received events data into the type eventClass and will therefore return
     * a mapped Observable of type EventDetails. It will only look at and transform the first
     * argument of the received events arguments, therefore it can only be used
     * for events that carry either a single or no argument.<br>
     * Received publications will be pushed to the Subscriber via it's
     * onNext method.<br>
     * The client can unsubscribe from the topic by calling unsubscribe() on
     * it's Subscription.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The topic to subscribe on.<br>
     * Must be valid WAMP URI.
     * @param flags Flags to indicate type of subscription. This cannot be null.
     * @param eventClass The class type into which the received event argument
     * should be transformed. E.g. use String.class to let the client try to
     * transform the first argument into a String and let the return value of
     * of the call be Observable&lt;EventDetails&lt;String&gt;&gt;.
     * @return An observable of type EventDetails that can be used to subscribe on the topic.
     * EventDetails contains topic and message. EventDetails.topic can be useful in getting 
     * the complete topic name during wild card or prefix subscriptions 
     */
    public <T> Observable<EventDetails<T>> makeSubscriptionWithDetails(final String topic, SubscriptionFlags flags, final Class<T> eventClass)
    {
        return makeSubscription(topic, flags).map(new Func1<PubSubData,EventDetails<T>>() {
            @Override
            public EventDetails<T> call(PubSubData ev) {
                if (eventClass == null || eventClass == Void.class) {
                    // We don't need a value
                    return null;
                }
                
                //get the complete topic name 
                //which may not be the same as method parameter 'topic' during wildcard or prefix subscriptions 
                String actualTopic = null;
                if(ev.details != null && ev.details.get("topic") != null){
                	actualTopic = ev.details.get("topic").asText();
                }

                if (ev.arguments == null || ev.arguments.size() < 1)
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_VALUE));

                JsonNode eventNode = ev.arguments.get(0);
                if (eventNode.isNull()) return null;

                T eventValue;
                try {
                    eventValue = clientConfig.objectMapper().convertValue(eventNode, eventClass);
                } catch (IllegalArgumentException e) {
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.INVALID_VALUE_TYPE));
                }
                return new EventDetails<T>(eventValue, actualTopic);
            }
        });
    }
    
    /**
     * Returns an observable that allows to subscribe on the given topic.<br>
     * The actual subscription will only be made after subscribe() was called
     * on it.<br>
     * Received publications will be pushed to the Subscriber via it's
     * onNext method.<br>
     * The client can unsubscribe from the topic by calling unsubscribe() on
     * it's Subscription.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The topic to subscribe on.<br>
     * Must be valid WAMP URI.
     * @return An observable that can be used to subscribe on the topic.
     */
    public Observable<PubSubData> makeSubscription(final String topic) {
        return makeSubscription(topic, SubscriptionFlags.Exact);
    }

    /**
     * Returns an observable that allows to subscribe on the given topic.<br>
     * The actual subscription will only be made after subscribe() was called
     * on it.<br>
     * Received publications will be pushed to the Subscriber via it's
     * onNext method.<br>
     * The client can unsubscribe from the topic by calling unsubscribe() on
     * it's Subscription.<br>
     * If the connection closes onCompleted will be called.<br>
     * In case of errors during subscription onError will be called.
     * @param topic The topic to subscribe on.<br>
     * Must be valid WAMP URI.
     * @param flags Flags to indicate type of subscription. This cannot be null.
     * @return An observable that can be used to subscribe on the topic.
     */
    public Observable<PubSubData> makeSubscription(final String topic, final SubscriptionFlags flags) {
        return Observable.create(new OnSubscribe<PubSubData>() {
            @Override
            public void call(final Subscriber<? super PubSubData> subscriber) {
                try {
                    if (flags == SubscriptionFlags.Exact) {
                        UriValidator.validate(topic, clientConfig.useStrictUriValidation());
                    } else if (flags == SubscriptionFlags.Prefix) {
                        UriValidator.validatePrefix(topic, clientConfig.useStrictUriValidation());
                    } else if (flags == SubscriptionFlags.Wildcard) {
                        UriValidator.validateWildcard(topic, clientConfig.useStrictUriValidation());
                    }
                }
                catch (WampError e) {
                    subscriber.onError(e);
                    return;
                }

                stateController.scheduler().execute(new Runnable() {
                    @Override
                    public void run() {
                        // If the Subscriber unsubscribed in the meantime we return early
                        if (subscriber.isUnsubscribed()) return;
                        // Set subscription to completed if we are not connected
                        if (!(stateController.currentState() instanceof SessionEstablishedState)) {
                            subscriber.onCompleted();
                            return;
                        }
                        // Forward performing actual subscription into the session
                        final SessionEstablishedState curState = (SessionEstablishedState)stateController.currentState();
                        curState.performSubscription(topic, flags, subscriber);
                    }
                });
            }
        });
    }

    /**
     * Performs a remote procedure call through the router.<br>
     * The function will return immediately, as the actual call will happen
     * asynchronously.
     * @param procedure The name of the procedure to call. Must be a valid WAMP
     * Uri.
     * @param arguments A list of all positional arguments for the procedure call
     * @param argumentsKw All named arguments for the procedure call
     * @return An observable that provides a notification whether the call was
     * was successful and the return value. If the call is successful the
     * returned observable will be completed with a single value (the return value).
     * If the remote procedure call yields an error the observable will be completed
     * with an error.
     */
    public Observable<Reply> call(final String procedure,
                                  final ArrayNode arguments,
                                  final ObjectNode argumentsKw)
    {
    	return call(procedure, null, arguments, argumentsKw);
    }
    
    
    /**
     * Performs a remote procedure call through the router.<br>
     * The function will return immediately, as the actual call will happen
     * asynchronously.
     * @param procedure The name of the procedure to call. Must be a valid WAMP
     * Uri.
     * @param flags Additional call flags if any. This can be null.
     * @param arguments A list of all positional arguments for the procedure call
     * @param argumentsKw All named arguments for the procedure call
     * @return An observable that provides a notification whether the call was
     * was successful and the return value. If the call is successful the
     * returned observable will be completed with a single value (the return value).
     * If the remote procedure call yields an error the observable will be completed
     * with an error.
     */
    public Observable<Reply> call(final String procedure,
                                  final EnumSet<CallFlags> flags,
                                  final ArrayNode arguments,
                                  final ObjectNode argumentsKw)
    {
        final AsyncSubject<Reply> resultSubject = AsyncSubject.create();
        
        try {
            UriValidator.validate(procedure, clientConfig.useStrictUriValidation());
        }
        catch (WampError e) {
            resultSubject.onError(e);
            return resultSubject;
        }
         
        stateController.scheduler().execute(new Runnable() {
            @Override
            public void run() {
                if (!(stateController.currentState() instanceof SessionEstablishedState)) {
                    resultSubject.onError(new ApplicationError(ApplicationError.NOT_CONNECTED));
                    return;
                }

                // Forward performing actual call into the session
                SessionEstablishedState curState = (SessionEstablishedState)stateController.currentState();
                curState.performCall(procedure, flags, arguments, argumentsKw, resultSubject);
            }
        });
        return resultSubject;
    }
    
    /**
     * Performs a remote procedure call through the router.<br>
     * The function will return immediately, as the actual call will happen
     * asynchronously.
     * @param procedure The name of the procedure to call. Must be a valid WAMP
     * Uri.
     * @param args The list of positional arguments for the remote procedure call.
     * These will be get serialized according to the Jackson library serializing
     * behavior.
     * @return An observable that provides a notification whether the call was
     * was successful and the return value. If the call is successful the
     * returned observable will be completed with a single value (the return value).
     * If the remote procedure call yields an error the observable will be completed
     * with an error.
     */
    public Observable<Reply> call(final String procedure, Object... args)
    {
        // Build the arguments array and serialize the arguments
        return call(procedure, ArgArrayBuilder.buildArgumentsArray(clientConfig.objectMapper(), args), null);
    }
    
    /**
     * Performs a remote procedure call through the router.<br>
     * The function will return immediately, as the actual call will happen
     * asynchronously.<br>
     * This overload of the call function will automatically map the received
     * reply value into the specified Java type by using Jacksons object mapping
     * facilities.<br>
     * Only the first value in the array of positional arguments will be taken
     * into account for the transformation. If multiple return values are required
     * another overload of this function has to be used.<br>
     * If the expected return type is not {@link Void} but the return value array
     * contains no value or if the value in the array can not be deserialized into
     * the expected type the returned {@link Observable} will be completed with
     * an error.
     * @param procedure The name of the procedure to call. Must be a valid WAMP
     * Uri.
     * @param returnValueClass The class of the expected return value. If the function
     * uses no return values Void should be used.
     * @param args The list of positional arguments for the remote procedure call.
     * These will be get serialized according to the Jackson library serializing
     * behavior.
     * @return An observable that provides a notification whether the call was
     * was successful and the return value. If the call is successful the
     * returned observable will be completed with a single value (the return value).
     * If the remote procedure call yields an error the observable will be completed
     * with an error.
     */
    public <T> Observable<T> call(final String procedure, 
                                  final Class<T> returnValueClass, Object... args)
    {
    	return call(procedure, null, returnValueClass, args);
    }
    
    /**
     * Performs a remote procedure call through the router.<br>
     * The function will return immediately, as the actual call will happen
     * asynchronously.<br>
     * This overload of the call function will automatically map the received
     * reply value into the specified Java type by using Jacksons object mapping
     * facilities.<br>
     * Only the first value in the array of positional arguments will be taken
     * into account for the transformation. If multiple return values are required
     * another overload of this function has to be used.<br>
     * If the expected return type is not {@link Void} but the return value array
     * contains no value or if the value in the array can not be deserialized into
     * the expected type the returned {@link Observable} will be completed with
     * an error.
     * @param procedure The name of the procedure to call. Must be a valid WAMP
     * Uri.
     * @param flags Additional call flags if any. This can be null.
     * @param returnValueClass The class of the expected return value. If the function
     * uses no return values Void should be used.
     * @param args The list of positional arguments for the remote procedure call.
     * These will be get serialized according to the Jackson library serializing
     * behavior.
     * @return An observable that provides a notification whether the call was
     * was successful and the return value. If the call is successful the
     * returned observable will be completed with a single value (the return value).
     * If the remote procedure call yields an error the observable will be completed
     * with an error.
     */
    public <T> Observable<T> call(final String procedure, final EnumSet<CallFlags> flags,
                                  final Class<T> returnValueClass, Object... args)
    {
        return call(procedure, flags, ArgArrayBuilder.buildArgumentsArray(clientConfig.objectMapper(), args), null)
            .map(new Func1<Reply,T>() {
            @Override
            public T call(Reply reply) {
                if (returnValueClass == null || returnValueClass == Void.class) {
                    // We don't need a return value
                    return null;
                }
                
                if (reply.arguments == null || reply.arguments.size() < 1)
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_RESULT));
                    
                JsonNode resultNode = reply.arguments.get(0);
                if (resultNode.isNull()) return null;
                
                T result;
                try {
                    result = clientConfig.objectMapper().convertValue(resultNode, returnValueClass);
                } catch (IllegalArgumentException e) {
                    // The returned exception is an aggregate one. That's not too nice :(
                    throw OnErrorThrowable.from(new ApplicationError(ApplicationError.INVALID_VALUE_TYPE));
                }
                return result;
            }
        });
    }
    
    /**
     * Returns an observable that will be completed with a single value once the client terminates.<br>
     * This can be used to asynchronously wait for completion after {@link #close() close} was called.
     */
    public Observable<Void> getTerminationObservable() {
        final AsyncSubject<Void> termSubject = AsyncSubject.create();
        stateController.statusObservable().subscribe(new Observer<State>() {
            @Override
            public void onCompleted() {
                termSubject.onNext(null);
                termSubject.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                termSubject.onNext(null);
                termSubject.onCompleted();
            }

            @Override
            public void onNext(State t) { }
        });
        return termSubject;
    }
    
    /**
     * Returns a future that will be completed once the client terminates.<br>
     * This can be used to wait for completion after {@link #close() close} was called.
     */
    public Future<Void> getTerminationFuture() {
        final Promise<Void> p = new Promise<Void>();
        stateController.statusObservable().subscribe(new Observer<State>() {
            @Override
            public void onCompleted() {
                p.resolve(null);
            }

            @Override
            public void onError(Throwable e) {
                p.resolve(null);
            }

            @Override
            public void onNext(State t) { }
        });
        return p.getFuture();
    }

}
