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

public class MessageType {

    public static final byte HELLO = 1;
    public static final byte WELCOME = 2;
    public static final byte ABORT = 3;
    public static final byte CHALLENGE = 4;
    public static final byte AUTHENTICATE = 5;
    public static final byte GOODBYE = 6;
    public static final byte HEARTBEAT = 7;
    public static final byte ERROR = 8;
    
    public static final byte PUBLISH = 16;
    public static final byte PUBLISHED = 17;
    
    public static final byte SUBSCRIBE = 32;
    public static final byte SUBSCRIBED = 33;
    public static final byte UNSUBSCRIBE = 34;
    public static final byte UNSUBSCRIBED = 35;
    public static final byte EVENT = 36;
    
    public static final byte CALL = 48;
    public static final byte CANCEL = 49;
    public static final byte RESULT = 50;
    
    public static final byte REGISTER = 64;
    public static final byte REGISTERED = 65;
    public static final byte UNREGISTER = 66;
    public static final byte UNREGISTERED = 67;
    public static final byte INVOCATION = 68;
    public static final byte INTERRUPT = 69;
    public static final byte YIELD = 70;

}
