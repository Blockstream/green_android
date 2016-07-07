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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ws.wamp.jawampa.internal.RealmConfig;
import ws.wamp.jawampa.internal.UriValidator;

/**
 * The {@link WampRouterBuilder} must be used to build {@link WampRouter}
 * instances.<br>
 * It allows to configure the instantiated router in detail before creating
 * it through the {@link #build()} method.
 */
public class WampRouterBuilder {
    
    Map<String, RealmConfig> realms = new HashMap<String, RealmConfig>();

    public WampRouterBuilder() {
        
    }
    
    /**
     * Builds and returns a new WAMP router from the given specification
     * @return The created WampRouter
     * @throws ApplicationError if any parameter is not invalid
     */
    public WampRouter build() throws ApplicationError {
        if (realms.size() == 0)
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        
        return new WampRouter(realms);
    }
    
    /**
     * Adds a realm that is available through the router.<br>
     * The realm will provide the Broker and Dealer roles and will use
     * loose Uri validation rules.
     * @param realmName The name of the realm. Must be a valid WAMP Uri.
     * @return This WampRouterBuilder object
     */
    public WampRouterBuilder addRealm(String realmName) throws ApplicationError {
        return addRealm(realmName, new WampRoles[] { WampRoles.Broker, WampRoles.Dealer }, false);
    }
    
    /**
     * Adds a realm that is available through the router
     * @param realmName The name of the realm. Must be a valid WAMP Uri.
     * @param roles The roles that are exposed through the router.<br>
     * Must be Broker, Dealer or Both.
     * @param useStrictUriValidation True if strict Uri validation rules shall
     * be used within this realm, false if loose validation rules shall be applied 
     * @return This WampRouterBuilder object
     */
    public WampRouterBuilder addRealm(String realmName, WampRoles[] roles, boolean useStrictUriValidation) throws ApplicationError {
        if (realmName == null || roles == null)
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        
        // Validate the realm name
        if (!UriValidator.tryValidate(realmName, useStrictUriValidation) || this.realms.containsKey(realmName))
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        
        // Validate and copy the roles
        Set<WampRoles> roleSet = new HashSet<WampRoles>();
        for (WampRoles r : roles) {
            if (r == null)
                throw new ApplicationError(ApplicationError.INVALID_REALM);
            roleSet.add(r);
        }
        
        // Check for at least one role
        if (roleSet.size() == 0)
            throw new ApplicationError(ApplicationError.INVALID_REALM);
        
        RealmConfig realmConfig = new RealmConfig(roleSet, useStrictUriValidation);
        
        // Insert the new realm configuration
        this.realms.put(realmName, realmConfig);
        
        return this;
    }
}
