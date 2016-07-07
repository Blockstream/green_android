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

import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ws.wamp.jawampa.auth.client.ClientSideAuthentication;
import ws.wamp.jawampa.client.ClientConfiguration;
import ws.wamp.jawampa.connection.IWampClientConnectionConfig;
import ws.wamp.jawampa.connection.IWampConnector;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.internal.UriValidator;

/**
 * WampClientBuilder is the builder object which allows to create
 * {@link WampClient} objects.<br>
 * New clients have to be configured with the member methods before
 * a client can be created with the {@link #build build()} method.
 */
public class WampClientBuilder {
    
    String uri;
    String realm;
    
    int nrReconnects = 0;
    int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;
    boolean useStrictUriValidation = false;
    boolean closeOnErrors = true;
    EnumSet<WampRoles> roles;
    List<WampSerialization> serializations = new ArrayList<WampSerialization>();
    String authId = null;
    List<ClientSideAuthentication> authMethods = new ArrayList<ClientSideAuthentication>();
    
    IWampClientConnectionConfig connectionConfiguration = null;
    IWampConnectorProvider connectorProvider = null;

    SocketAddress proxyAddress = null;
    
    /** The default reconnect interval in milliseconds.<br>This is set to 5s */
    public static final int DEFAULT_RECONNECT_INTERVAL = 5000;
    /** The minimum reconnect interval in milliseconds.<br>This is set to 100ms */
    public static final int MIN_RECONNECT_INTERVAL = 100;

    /**
     * Construct a new WampClientBuilder object.
     */
    public WampClientBuilder() {
        // Add the default roles
        roles = EnumSet.of(
                WampRoles.Caller,
                WampRoles.Callee,
                WampRoles.Publisher,
                WampRoles.Subscriber);
        
        WampSerialization.addDefaultSerializations(serializations);
    }
    
    /**
     * Builds a new {@link WampClient WAMP client} from the information given in this builder.<br>
     * At least the uri of the router and the name of the realm have to be
     * set for a proper operation.
     * @return The created WAMP client
     * @throws WampError if any parameter is not invalid
     */
    public WampClient build() throws Exception {
        if (uri == null)
            throw new ApplicationError(ApplicationError.INVALID_URI);
        
        URI routerUri;
        try {
        	routerUri = new URI(uri);
        }
        catch (Throwable t) {
        	throw new ApplicationError(ApplicationError.INVALID_URI);
        }
        
        if (realm == null)
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        
        try {
            UriValidator.validate(realm, useStrictUriValidation);
        } catch (Exception e) {
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        }
        
        if (roles.size() == 0) {
            throw new ApplicationError(ApplicationError.INVALID_ROLES);
        }
        
        // Build the roles array from the roles set
        WampRoles[] rolesArray = new WampRoles[roles.size()];
        int i = 0;
        for (WampRoles r : roles) {
            rolesArray[i] = r;
            i++;
        }

        if (serializations.size() == 0) {
            throw new ApplicationError(ApplicationError.INVALID_SERIALIZATIONS);
        }
        
        if (connectorProvider == null)
            throw new ApplicationError(ApplicationError.INVALID_CONNECTOR_PROVIDER);
        
        // Build a connector that can be used by the client afterwards
        // This can throw!
        IWampConnector connector =
            connectorProvider.createConnector(routerUri, connectionConfiguration, serializations, proxyAddress);
        
        ClientConfiguration clientConfig =
            new ClientConfiguration(
                closeOnErrors, authId, authMethods, routerUri, realm,
                useStrictUriValidation, rolesArray, nrReconnects, reconnectInterval,
                connectorProvider, connector);
        
        return new WampClient(clientConfig);
    }
    
    /**
     * Sets the address of the router to which the new client shall connect.
     * @param uri The address of the router, e.g. ws://wamp.ws/ws
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withUri(String uri) {
        this.uri = uri;
        return this;
    }
    
    /**
     * Sets the name of the realm on the router which shall be used for the session.
     * @param realm The name of the realm to which shall be connected.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withRealm(String realm) {
        this.realm = realm;
        return this;
    }
    
    /**
     * Adjusts the roles that this client should have in the session.<br>
     * By default a client will have all roles (caller, callee, publisher, subscriber).
     * Use this function to adjust the roles if not all are needed.
     * @param roles The set of roles that the client should fulfill in the session.
     * At least one role is required, otherwise the session can not be established.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withRoles(WampRoles[] roles) {
        this.roles.clear();
        if (roles == null) return this; // Will throw on build()
        for (WampRoles role : roles) {
            this.roles.add(role);
        }
        return this;
    }
    
    /**
     * Assigns a connector provider to the WampClient. This provider will be used to
     * establish connections to the server.<br>
     * By using a different ConnectorProvider a different transport framework can be used
     * for data exchange between client and server.
     * @param The {@link IWampConnectorProvider} that should be used
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withConnectorProvider(IWampConnectorProvider provider) {
        this.connectorProvider = provider;
        return this;
    }

    public WampClientBuilder withProxyAddress(SocketAddress proxyAddress) {
        this.proxyAddress = proxyAddress;
        return this;
    }

    /**
     * Assigns additional configuration data for a connection that should be used.<br>
     * The type of this configuration data depends on the used {@link IWampConnectorProvider}.<br>
     * Depending on the provider this might be null or not.
     * @param The {@link IWampClientConnectionConfig} that should be used
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withConnectionConfiguration(IWampClientConnectionConfig configuration) {
        this.connectionConfiguration = configuration;
        return this;
    }

    /**
     * Adjusts the serializations that this client supports in the session.<br>
     * By default a client will have all serializations (JSON, MessagePack).<br>
     * The order of the list indicates client preference to the router during
     * negotiation.<br>
     * Use this function to adjust the serializations if not all are needed or
     * a different order is desired.
     * @param serializations The set of serializations that the client supports.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withSerializations(WampSerialization[] serializations) throws ApplicationError {
        this.serializations.clear();
        if (serializations == null) return this; // Will throw on build()
        for (WampSerialization serialization : serializations) {
            if (serialization == WampSerialization.Invalid)
                throw new ApplicationError(ApplicationError.INVALID_SERIALIZATIONS);
            if (!this.serializations.contains(serialization))
                this.serializations.add(serialization);
        }
        return this;
    }

    /**
     * Allows to activate or deactivate the validation of all WAMP Uris according to the
     * strict URI validation rules which are described in the WAMP specification.
     * By default the loose Uri validation rules will be used.
     * @param useStrictUriValidation true if strict Uri validation rules shall be applied,
     * false if loose Uris shall be used.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withStrictUriValidation(boolean useStrictUriValidation) {
        this.useStrictUriValidation = useStrictUriValidation;
        return this;
    }
    
    /**
     * Sets whether the client should be closed when an error besides
     * a connection loss happens.<br>
     * Other reasons are the reception of invalid messages from the remote
     * or the abortion of the session by the remote.<br>
     * When this flag is set to true no reconnect attempts will be performed.
     * <br>
     * The default is true. This is to avoid endless reconnects in case of
     * a malfunctioning remote. 
     * @param closeOnErrors True if the client should be closed on such
     * errors, false if not.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withCloseOnErrors(boolean closeOnErrors) {
        this.closeOnErrors = closeOnErrors;
        return this;
    }
    
    
    /**
     * Sets the amount of reconnect attempts to perform to a dealer.
     * @param nrReconnects The amount of connects to perform. Must be > 0.
     * @return The {@link WampClientBuilder} object
     * @throws WampError if the nr of reconnects is negative
     */
    public WampClientBuilder withNrReconnects(int nrReconnects) throws ApplicationError {
        if (nrReconnects < 0) 
            throw new ApplicationError(ApplicationError.INVALID_PARAMETER);
        this.nrReconnects = nrReconnects;
        return this;
    }
    
    /**
     * Sets the amount of reconnect attempts to perform to a dealer
     * to infinite.
     * @return The {@link WampClientBuilder} object
     */
    public WampClientBuilder withInfiniteReconnects() {
        this.nrReconnects = -1;
        return this;
    }
    
    /**
     * Sets the amount of time that should be waited until a reconnect attempt is performed.<br>
     * The default value is {@link #DEFAULT_RECONNECT_INTERVAL}.
     * @param interval The interval that should be waited until a reconnect attempt<br>
     * is performed. The interval must be bigger than {@link #MIN_RECONNECT_INTERVAL}.
     * @param unit The unit of the interval
     * @return The {@link WampClientBuilder} object
     * @throws WampError If the interval is invalid
     */
    public WampClientBuilder withReconnectInterval(int interval, TimeUnit unit) throws ApplicationError {
        long intervalMs = unit.toMillis(interval);
        if (intervalMs < MIN_RECONNECT_INTERVAL || intervalMs > Integer.MAX_VALUE)
            throw new ApplicationError(ApplicationError.INVALID_RECONNECT_INTERVAL);
        this.reconnectInterval = (int)intervalMs;
        return this;
    }

   /**
    * Set the authId to use. If not called, no authId is used.
    * @param authId the authId
    * @return The {@link WampClientBuilder} object
    */
   public WampClientBuilder withAuthId(String authId) {
       this.authId = authId;
       return this;
   }

   /**
    * Use a specific auth method. Can be called multiple times to specify multiple
    * supported auth methods. If this method is not called, anonymous auth is used.
    * @param authMethod The {@link ClientSideAuthentication} to add
    * @return The {@link WampClientBuilder} object
    */
   public WampClientBuilder withAuthMethod(ClientSideAuthentication authMethod) {
       this.authMethods.add( authMethod );
       return this;
   }

}
