package com.greenaddress.greenapi.model;

import android.content.res.Resources;
import android.util.Log;

import java.util.Objects;
import java.util.Observable;

public class ConnectionMessageObservable extends Observable {
    private boolean offline=false;
    private int messageId;
    private Object[] args;

    public String getMessage(final Resources resources) {
        if (args == null)
            return resources.getString(messageId);
        else
            return resources.getString(messageId, args);
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOnline() {
        this.messageId = 0;
        this.args = null;
        this.offline = false;
        setChanged();
        notifyObservers();
    }

    public void setMessage(final int messageId) {
        setMessage(messageId, null);
    }

    public void setMessage(final int messageId, final Object[] args) {
        if(args == null || !(messageId==this.messageId && Objects.deepEquals(args,this.args))) {
            Log.d("OBS", "ConnectionMessageObservable.setMessage(" + messageId + "," + args + ")");
            this.messageId = messageId;
            this.args = args;
            this.offline = true;
            setChanged();
            notifyObservers();
        }
    }
}
