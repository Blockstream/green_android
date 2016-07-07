/*
 * Copyright 2015 Matthias Einwag
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

package ws.wamp.jawampa.connection;

/**
 * A connection which is currently trying to connect to the remote peer,
 * but which has not yet successfully finished to connect.
 */
public interface IPendingWampConnection {
    
    /**
     *  Cancels the connect attempt<br>
     *  The implementation must guarantee that upon successful cancellation
     *  {@link IPendingWampConnectionListener#connectFailed(Throwable)} is called
     *  to acknowledge the cancellation.
     */
    void cancelConnect();
    
    /** An dummy instance of IPendingWampConnection that supports no cancellation */
    public static final IPendingWampConnection Dummy = new IPendingWampConnection() {
        @Override
        public void cancelConnect() {
        }
    };
}
