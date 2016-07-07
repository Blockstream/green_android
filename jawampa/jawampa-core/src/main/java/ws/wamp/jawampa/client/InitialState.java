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

package ws.wamp.jawampa.client;


/**
 * The client is just initialized.<br>
 * There was no attempt to connect.
 */
public class InitialState implements ClientState {
    private final StateController stateController;

    InitialState(StateController stateController) {
        this.stateController = stateController;
    }

    @Override
    public void onEnter(ClientState lastState) {
        
    }

    @Override
    public void onLeave(ClientState newState) {
        
    }
    
    @Override
    public void initClose() {
        DisconnectedState nextState = new DisconnectedState(stateController, null);
        stateController.setState(nextState);
    }
}