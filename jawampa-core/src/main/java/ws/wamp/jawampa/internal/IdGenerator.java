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

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains method for generating WAMP IDs
 */
public class IdGenerator {
    
    /**
     * Generates a new ID through a random generator.<br>
     * If the new ID is not valid or is already in use as a key in the provided Map
     * the process will be repeated until a valid iD can be returned.
     * @param controlMap The map which should be optionally checked. If the parameter
     * is not null and contains a key with the new ID value then the ID will not be used. 
     * @return The new ID
     */
    public static long newRandomId(Map<Long, ?> controlMap) {
        for (;;) {
            long l = ThreadLocalRandom.current().nextLong();
            if (l < IdValidator.MIN_VALID_ID || l > IdValidator.MAX_VALID_ID) continue;
            if (controlMap == null || !controlMap.containsKey(l)) return l;
        }
    }
    
    /**
     * Generates a new ID by incrementing an old one.<br>
     * If the new ID is not valid or is already in use as a key in the provided Map
     * the process will be repeated until a valid iD can be returned.
     * @param startValue The starting value for the process. The returned ID will be
     * startValue + 1 unless that value is not valid or already in use.
     * @param controlMap The map which should be optionally checked. If the parameter
     * is not null and contains a key with the new ID value then the ID will not be used. 
     * @return The new ID
     */
    public static long newLinearId(long startValue, Map<Long, ?> controlMap) {
        long val = startValue;
        for (;;) {
            val++;
            if (val > IdValidator.MAX_VALID_ID)
                val = IdValidator.MIN_VALID_ID;
            if (controlMap == null || !controlMap.containsKey(val)) return val;
        }
    }

}
