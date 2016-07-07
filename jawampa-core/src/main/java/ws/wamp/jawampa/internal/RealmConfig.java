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

package ws.wamp.jawampa.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ws.wamp.jawampa.WampRoles;

/**
 * Allows to configure realms that are exposed by routers
 */
public class RealmConfig {

    public final Set<WampRoles> roles;
    public final boolean useStrictUriValidation;
    
    public Set<WampRoles> roles() {
        return roles;
    }
    
    public RealmConfig(Set<WampRoles> roles, boolean useStrictUriValidation)  {
        // Copy the roles
        this.roles = Collections.unmodifiableSet(new HashSet<WampRoles>(roles));
        this.useStrictUriValidation = useStrictUriValidation;
    }

}
