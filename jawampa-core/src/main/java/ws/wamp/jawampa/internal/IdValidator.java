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

/**
 * Validates whether WAMP IDs that are e.g. used in requests
 * are valid.
 */
public class IdValidator {
    
    public static final long MIN_VALID_ID = 0L;
    public static final long MAX_VALID_ID = 9007199254740992L; // 2^53
    
    /**
     * Returns true if an ID is a valid WAMP ID and
     * false if not.
     * @param id The ID to validate
     */
    public static boolean isValidId(long id) {
        if (id >= MIN_VALID_ID && id <= MAX_VALID_ID) return true;
        return false;
    }
}
