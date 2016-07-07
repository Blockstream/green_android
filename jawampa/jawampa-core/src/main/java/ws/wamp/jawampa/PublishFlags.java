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

public enum PublishFlags {
    /**
     * Set the exclude_me flag on the Publish message to false.<br>
     * This will cause that the published message will also be delivered to the publishing client.
     */
    DontExcludeMe,
    /**
     * Require an acknowledge from the Wamp router for the publication of the event.<br>
     * If the flag is not sent every publish request will be reported as successful if a connection
     * between client and router is established. If the flag is set the response from the router
     * to a publish message will be used as a result for a publish request.
     */
    RequireAcknowledge;
}
