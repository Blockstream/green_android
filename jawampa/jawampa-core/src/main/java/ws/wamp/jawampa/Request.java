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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import ws.wamp.jawampa.WampMessages.ErrorMessage;
import ws.wamp.jawampa.WampMessages.YieldMessage;
import ws.wamp.jawampa.client.SessionEstablishedState;
import ws.wamp.jawampa.client.StateController;
import ws.wamp.jawampa.connection.IWampConnectionPromise;
import ws.wamp.jawampa.internal.ArgArrayBuilder;
import ws.wamp.jawampa.internal.UriValidator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Holds the arguments for a WAMP remote procedure call and provides methods
 * to send responses to the caller.<br>
 * Either {@link #reply(ArrayNode, ObjectNode)} or 
 * {@link #replyError(String, ArrayNode, ObjectNode)}} should be called in
 * order to send a positive or negative response back to the caller.
 */
public class Request {
    
    final StateController stateController;
    final SessionEstablishedState session;
    final long requestId;
    final ArrayNode arguments;
    final ObjectNode keywordArguments;
    final ObjectNode details;
    
    volatile int replySent = 0;
    
    private static final AtomicIntegerFieldUpdater<Request> replySentUpdater;
    static {
        replySentUpdater = AtomicIntegerFieldUpdater.newUpdater(Request.class, "replySent");
    }
    
    public ArrayNode arguments() {
        return arguments;
    }
    
    public ObjectNode keywordArguments() {
        return keywordArguments;
    }
    
    public ObjectNode details() {
    	return details;
    }

    public Request(StateController stateController, SessionEstablishedState session, 
                   long requestId, ArrayNode arguments, ObjectNode keywordArguments, ObjectNode details)
    {
        this.stateController = stateController;
        this.session = session;
        this.requestId = requestId;
        this.arguments = arguments;
        this.keywordArguments = keywordArguments;
        this.details = details;
    }
    
    /**
     * Send an error message in response to the request.<br>
     * If this is called more than once then the following invocations will
     * have no effect. Respones will be only sent once.
     * @param error The ApplicationError that shoul be serialized and sent
     * as an exceptional response. Must not be null.
     */
    public void replyError(ApplicationError error) throws ApplicationError{
        if (error == null || error.uri == null) throw new NullPointerException();
        replyError(error.uri, error.args, error.kwArgs);
    }
    
    /**
     * Send an error message in response to the request.<br>
     * This version of the function will use Jacksons object mapping
     * capabilities to transform the argument objects in a JSON argument
     * array which will be sent as the positional arguments of the call.
     * If keyword arguments are needed then this function can not be used.<br>
     * If this is called more than once then the following invocations will
     * have no effect. Respones will be only sent once.
     * @param errorUri The error message that should be sent. This must be a
     * valid WAMP Uri.
     * @param args The positional arguments to sent in the response
     */
    public void replyError(String errorUri, Object... args) throws ApplicationError{
        replyError(errorUri, ArgArrayBuilder.buildArgumentsArray(stateController.clientConfig().objectMapper(), args), null);
    }
    
    /**
     * Send an error message in response to the request.<br>
     * If this is called more than once then the following invocations will
     * have no effect. Respones will be only sent once.
     * @param errorUri The error message that should be sent. This must be a
     * valid WAMP Uri.
     * @param arguments The positional arguments to sent in the response
     * @param keywordArguments The keyword arguments to sent in the response
     */
    public void replyError(String errorUri, ArrayNode arguments, ObjectNode keywordArguments) throws ApplicationError {
        int replyWasSent = replySentUpdater.getAndSet(this, 1);
        if (replyWasSent == 1) return;
        
        UriValidator.validate(errorUri, false);
        
        final ErrorMessage msg = new ErrorMessage(WampMessages.InvocationMessage.ID, 
                                                  requestId, null, errorUri,
                                                  arguments, keywordArguments);
         
        stateController.tryScheduleAction(new Runnable() {
            @Override
            public void run() {
                if (stateController.currentState() != session) return;
                session.connectionController().sendMessage(msg, IWampConnectionPromise.Empty);
            }
        });
    }
    
    /**
     * Send a normal response to the request.<br>
     * If this is called more than once then the following invocations will
     * have no effect. Responses will be only sent once.
     * @param arguments The positional arguments to sent in the response
     * @param keywordArguments The keyword arguments to sent in the response
     */
    public void reply(ArrayNode arguments, ObjectNode keywordArguments) {
        int replyWasSent = replySentUpdater.getAndSet(this, 1);
        if (replyWasSent == 1) return;
        
        final YieldMessage msg = new YieldMessage(requestId, null,
                                                  arguments, keywordArguments);
         
        stateController.tryScheduleAction(new Runnable() {
            @Override
            public void run() {
                if (stateController.currentState() != session) return;
                session.connectionController().sendMessage(msg, IWampConnectionPromise.Empty);
            }
        });
    }
    
    /**
     * Send a normal response to the request.<br>
     * This version of the function will use Jacksons object mapping
     * capabilities to transform the argument objects in a JSON argument
     * array which will be sent as the positional arguments of the call.
     * If keyword arguments are needed then this function can not be used.<br>
     * If this is called more than once then the following invocations will
     * have no effect. Respones will be only sent once.
     * @param arguments The positional arguments to sent in the response
     * @param keywordArguments The keyword arguments to sent in the response
     */
    public void reply(Object... args) {
        reply(ArgArrayBuilder.buildArgumentsArray(
            stateController.clientConfig().objectMapper(),args), null);
    }

}
