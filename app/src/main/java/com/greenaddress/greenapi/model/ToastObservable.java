package com.greenaddress.greenapi.model;

import android.content.res.Resources;
import android.util.Log;

import java.util.Observable;

public class ToastObservable extends Observable {
    private int messageId;
    private Object[] args;

    public String getMessage(final Resources resources) {
        if (args == null)
            return resources.getString(messageId);
        else
            return resources.getString(messageId, args);
    }

    public void setMessage(final int messageId) {
        setMessage(messageId, null);
    }

    public void setMessage(final int messageId, final Object[] args) {
        Log.d("OBS", "ToastObservable.setMessage(" +  messageId + "," + args + ")");
        this.messageId = messageId;
        this.args = args;
        setChanged();
        notifyObservers();
    }
}
