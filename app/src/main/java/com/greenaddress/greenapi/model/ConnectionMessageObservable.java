package com.greenaddress.greenapi.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Observable;

public class ConnectionMessageObservable extends Observable {
    private JsonNode networkNode;

    public void setNetworkNode(final JsonNode network) {
        networkNode = network;
        setChanged();
        notifyObservers();
    }

    public boolean isValid() {
        return networkNode != null;
    }

    public boolean isOnline() {
        return networkNode.get("connected").asBoolean();
    }

    public long waitingMs() {
        return networkNode.get("waiting").asLong() * 1000;
    }

    public boolean isLoginRequired() {
        return networkNode.get("login_required").asBoolean(false);
    }
}
