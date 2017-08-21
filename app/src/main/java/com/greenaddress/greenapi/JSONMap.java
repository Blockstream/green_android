package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.Serializable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class JSONMap implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = getDateFormat();

    private static SimpleDateFormat getDateFormat() {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    public final Map<String, Object> mData; // Public for setting only

    public JSONMap() { mData = new HashMap(); }
    public JSONMap(final Map<String, Object> jsonMap) { mData = jsonMap; }

    public static List<JSONMap> fromList(final List list) {
        final List<JSONMap> result = new ArrayList<>(list.size());
        for (final Object o : list)
            result.add(new JSONMap((Map<String, Object>) o));
        return result;
    }

    public boolean containsKey(final String k) { return mData.containsKey(k); }

    public String getKey(final String k, final String altKey) {
        return containsKey(k) ? k : altKey;
    }

    public <T> T get(final String k, final T def) {
        return containsKey(k) ? (T) mData.get(k) : def;
    }

    public <T> T get(final String k) {
        return (T) mData.get(k);
    }

    public JSONMap getMap(final String k) {
        try {
            final String v = get(k, null);
            if (v == null)
                return null;
            return new JSONMap(new MappingJsonFactory().getCodec().readValue(v, Map.class));
        } catch (final IOException e) {
            return null;
        }
    }

    public boolean getBool(final String k) {
        // Not present, present but null or false are false, otherwise true
        if (!containsKey(k))
            return false;
        return Boolean.TRUE.equals(get(k));
    }

    public String getString(final String k) {
       final Object o = containsKey(k) ? get(k) : null;
       return o == null ? null : o.toString();
    }

    public Integer getInt(final String k) {
        return getInt(k, null);
    }

    public Integer getInt(final String k, final Integer def) {
        final String v = getString(k);
        return v == null ? def : Integer.valueOf(v);
    }

    public Date getDate(final String k) throws ParseException {
        final String v = getString(k);
        return v == null ? null : DATE_FORMAT.parse(v);
    }

    public Sha256Hash getHash(final String k) {
        final String v = getString(k);
        return v == null ? null : Sha256Hash.wrap(v);
    }

    public Long getLong(final String k) {
        final String v = getString(k);
        return v == null ? null : Long.valueOf(v);
    }

    public Double getDouble(final String k) {
        final String v = getString(k);
        return v == null ? null : Double.valueOf(v);
    }

    public Coin getCoin(final String k) {
        final Long v = getLong(k);
        return v == null ? null : Coin.valueOf(v);
    }

    public BigInteger getBigInteger(final String k) {
        final String v = getString(k);
        return v == null ? null : new BigInteger(v);
    }

    public byte[] getBytes(final String k) {
        final String v = getString(k);
        return v == null ? null : Wally.hex_to_bytes(v);
    }

    public String toString() {
        return mData.toString();
    }

    public void putBytes(final String k, final byte[] v) {
        mData.put(k, Wally.hex_from_bytes(v));
    }
}
