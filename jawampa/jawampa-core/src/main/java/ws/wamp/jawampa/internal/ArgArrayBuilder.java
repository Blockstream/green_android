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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Utility class to build a Jackson argument array out of an arbitrary list
 * of positional arguments.
 */
public class ArgArrayBuilder {
    /**
     * Builds an ArrayNode from all positional arguments in a WAMP message.<br>
     * If there are no positional arguments then null will be returned, as
     * WAMP requires no empty arguments list to be transmitted.
     * @param args All positional arguments
     * @return An ArrayNode containing positional arguments or null
     */
    public static ArrayNode buildArgumentsArray(ObjectMapper objectMapper, Object... args) {
        if (args.length == 0) return null;
        // Build the arguments array and serialize the arguments
        final ArrayNode argArray = objectMapper.createArrayNode();
        for (Object arg : args) {
            argArray.addPOJO(arg);
        }
        return argArray;
    }
}
