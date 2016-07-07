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

import ws.wamp.jawampa.connection.IWampConnectorProvider;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for all WAMP related exceptions that can/may be
 * handled at the application level.
 */
public class ApplicationError extends WampError {
    
    private static final long serialVersionUID = 7520664664586119266L;
    
    final String uri;
    final ArrayNode args;
    final ObjectNode kwArgs;
    
    public String uri() {
        return uri;
    }
    
    public ArrayNode arguments() {
        return args;
    }
    
    public ObjectNode keywordArguments() {
        return kwArgs;
    }
    
    public ApplicationError(String uri) {
        this(uri, null, null);
    }
    
    public ApplicationError(String uri, ArrayNode args) {
        this(uri, args, null);
    }

    public ApplicationError(String uri, ArrayNode args, ObjectNode kwArgs) {
        super(uri);
        if (uri == null) throw new NullPointerException();
        this.uri = uri;
        this.args = args;
        this.kwArgs = kwArgs;
    }
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("ApplicationError(")
         .append(uri)
         .append(", ");
        
        if (args != null) {
            s.append(args);
        }
        else {
            s.append("[]");
        }        
        s.append(", ");
        if (kwArgs != null) {
            s.append(kwArgs);
        }
        else {
            s.append("{}");
        }
        
        s.append(')');
        return s.toString();
    }
    
    /**
     * Peer provided an incorrect URI for a URI-based attribute of a WAMP message
     * such as a realm, topic or procedure.
     */
    public final static String INVALID_URI = "wamp.error.invalid_uri";
    
    /**
     * A Dealer could not perform a call, since not procedure is currently registered
     * under the given URI.
     */
    public final static String NO_SUCH_PROCEDURE = "wamp.error.no_such_procedure";
    
    /**
     * A procedure could not be registered, since a procedure with the given URI is
     * already registered.
     */
    public final static String PROCEDURE_ALREADY_EXISTS = "wamp.error.procedure_already_exists";
    
    /**
     * A Dealer could not perform a unregister, since the given registration is not active.
     */
    public final static String NO_SUCH_REGISTRATION = "wamp.error.no_such_registration";
    
    /**
     * A Broker could not perform a unsubscribe, since the given subscription is not active.
     */
    public final static String NO_SUCH_SUBSCRIPTION = "wamp.error.no_such_subscription";
    
    /**
     * A call failed, since the given argument types or values are not acceptable to the
     * called procedure - in which case the Callee may throw this error. Or a Router
     * performing payload validation checked the payload (args / kwargs) of a call,
     * call result, call error or publish, and the payload did not conform.
     */
    public final static String INVALID_ARGUMENT = "wamp.error.invalid_argument";
    
    /**
     * The Peer is shutting down completely - used as a GOODBYE (or ABORT) reason.
     */
    public final static String SYSTEM_SHUTDOWN = "wamp.error.system_shutdown";
    
    /**
     * The Peer want to leave the realm - used as a GOODBYE reason.
     */
    public final static String CLOSE_REALM = "wamp.error.close_realm";
    
    /**
     * A Peer acknowledges ending of a session - used as a GOOBYE reply reason.
     */
    public final static String GOODBYE_AND_OUT = "wamp.error.goodbye_and_out";

    /**
     * A call, register, publish or subscribe failed, since the session is not authorized
     * to perform the operation.
     */
    public final static String NOT_AUTHORIZED = "wamp.error.not_authorized";
    
    /**
     * A Dealer or Broker could not determine if the Peer is authorized to perform
     * a join, call, register, publish or subscribe, since the authorization operation
     * itself failed. E.g. a custom authorizer did run into an error.
     */
    public final static String AUTHORIZATION_FAILED = "wamp.error.authorization_failed";
    
    /**
     * Peer wanted to join a non-existing realm (and the Router did not allow to auto-create
     * the realm).
     */
    public final static String NO_SUCH_REALM = "wamp.error.no_such_realm";
    
    /**
     * A Peer was to be authenticated under a Role that does not (or no longer) exists on the Router.
     * For example, the Peer was successfully authenticated, but the Role configured does not
     * exists - hence there is some misconfiguration in the Router.
     */
    public final static String NO_SUCH_ROLE = "wamp.error.no_such_role";
    
    /**
     * A Dealer or Callee canceled a call previously issued (WAMP AP).
     */
    public final static String CANCELED = "wamp.error.canceled";
    
    /**
     * A Peer requested an interaction with an option that was disallowed by the Router
     */
    public final static String OPTION_NOT_ALLOWED = "wamp.error.option_not_allowed";
    
    /**
     * A Dealer could not perform a call, since a procedure with the given URI is registered,
     * but Callee Black- and Whitelisting and/or Caller Exclusion lead to the
     * exclusion of (any) Callee providing the procedure (WAMP AP).
     */
    public final static String NO_ELIGIBLE_CALLEE = "wamp.error.no_eligible_callee";
    
    /**
     * A Router rejected client request to disclose its identity (WAMP AP).
     */
    public final static String OPTION_DISALLOWED_DISCLOSE_ME = "wamp.error.option_disallowed.disclose_me";
    
    
    // Library specific errors
    // -----------------------
    
    /** A parameter is invalid */
    public final static String INVALID_PARAMETER = "jawampa.error.invalid_parameter";
    
    /** A required parameter is null */
    public final static String PARAMETER_IS_NULL = "jawampa.error.parameter_is_null";
    
    public final static String INVALID_ROLES = "jawampa.error.invalid_roles";

    public final static String INVALID_SERIALIZATIONS = "jawampa.error.invalid_serializations";
    
    public final static String INVALID_REALM = "jawampa.error.invalid_realm";
    
    public final static String INVALID_MESSAGE = "jawampa.error.invalid_message";
    
    public final static String PROTCOL_ERROR = "jawampa.error.protocol_error";
    
    public final static String NOT_IMPLEMENTED = "jawampa.error.not_implemented";
    
    public final static String NOT_CONNECTED = "jawampa.error.not_connected";
    
    /** The transport between client and server got closed */
    public final static String TRANSPORT_CLOSED = "jawampa.error.transport_closed";
    
    /** The client can not connect to the server */
    public final static String TRANSPORT_CAN_NOT_CONNECT = "jawampa.error.transport_can_not_connect";
    
    public final static String MISSING_RESULT = "jawampa.error.missing_result";
    
    public final static String MISSING_VALUE = "jawampa.error.missing_value";
    
    public final static String INVALID_VALUE_TYPE = "jawampa.error.invalid_value_type";
    
    public final static String INVALID_RECONNECT_INTERVAL = "jawampa.error.invalid_reconnect_interval";
    
    public final static String SESSION_ABORTED = "jawampa.error.session_aborted";
    
    /** The user requested the client to close */
    public final static String CLIENT_CLOSED = "jawampa.error.client_closed";
    
    /** An invalid connector provider (e.g. null) was is used */
    public final static String INVALID_CONNECTOR_PROVIDER = "jawampa.error.invalid_connector_provider";
    
    /**
     * The connection configuration is invalid.<br>
     * This might happen if the type of the connection configuration does not match to
     * the type which the {@link IWampConnectorProvider} expects or if the value
     * is null and the provider expects a non-null value.
     */
    public final static String INVALID_CONNECTION_CONFIGURATION = "jawampa.error.invalid_connection_configuration";
    
    /** A scheduler that is not compatible to a particular connection provider was assigned to it */
    public final static String INCOMATIBLE_SCHEDULER = "jawampa.error.incompatible_scheduler";
}
