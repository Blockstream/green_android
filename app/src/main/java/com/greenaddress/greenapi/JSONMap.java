package com.greenaddress.greenapi;

import org.bitcoinj.core.Sha256Hash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JSONMap implements Serializable {

    private final Map<String, Object> mData; // Public for setting only

    public JSONMap() { mData = new HashMap(); }


    private boolean containsKey(final String k) { return mData.containsKey(k); }

    public String getKey(final String k, final String altKey) {
        return containsKey(k) ? k : altKey;
    }

    public <T> T get(final String k, final T def) {
        return containsKey(k) ? (T) mData.get(k) : def;
    }

    public <T> T get(final String k) {
        return (T) mData.get(k);
    }

    public String getString(final String k) {
       final Object o = containsKey(k) ? get(k) : null;
       return o == null ? null : o.toString();
    }

    public Sha256Hash getHash(final String k) {
        final String v = getString(k);
        return v == null ? null : Sha256Hash.wrap(v);
    }

    public String toString() {
        return mData.toString();
    }
}
