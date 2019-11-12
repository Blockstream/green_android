package com.greenaddress.greenapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Observable;

public class TorProgressObservable extends Observable {
    private JsonNode json;

    public JsonNode get() {
        return json;
    }

    public void set(final JsonNode json) {
        this.json = json;
        setChanged();
        notifyObservers();
    }
}
