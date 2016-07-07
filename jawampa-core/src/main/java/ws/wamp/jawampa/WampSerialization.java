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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Possible serialization methods for WAMP
 */
public enum WampSerialization {
    /** Used for cases where the serialization could not be negotiated */
    Invalid("", true, null),
    /** Use the JSON serialization */
    Json("wamp.2.json", true, new ObjectMapper()),
    /** Use the MessagePack serialization */
    MessagePack("wamp.2.msgpack", false, new ObjectMapper(new MessagePackFactory()));

    private final String stringValue;
    private final boolean isText;
    private final ObjectMapper objectMapper;

    WampSerialization(String stringValue, boolean isText, ObjectMapper objectMapper) {
        this.stringValue = stringValue;
        this.isText = isText;
        this.objectMapper = objectMapper;
    }

    public boolean isText() {
        return isText;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    public static WampSerialization fromString(String serialization) {
        if (serialization == null) return Invalid;
        else if (serialization.equals("wamp.2.json")) return Json;
        else if (serialization.equals("wamp.2.msgpack")) return MessagePack;
        return Invalid;
    }
    
    public static String makeWebsocketSubprotocolList(List<WampSerialization> serializations) {
        StringBuilder subProtocolBuilder = new StringBuilder();
        boolean first = true;
        for (WampSerialization serialization : serializations) {
            if (!first) subProtocolBuilder.append(',');
            first = false;
            subProtocolBuilder.append(serialization.toString());
        }

        return subProtocolBuilder.toString();
    }

    public static void addDefaultSerializations(List<WampSerialization> serializations) {
        serializations.add(Json);
        serializations.add(MessagePack);
    }
    
    private final static List<WampSerialization> defaultSerializationList;
    static {
        List<WampSerialization> l = new ArrayList<WampSerialization>();
        addDefaultSerializations(l);
        defaultSerializationList = Collections.unmodifiableList(l);
    }
    
    public static List<WampSerialization> defaultSerializations() {
         return defaultSerializationList;
    }
}
