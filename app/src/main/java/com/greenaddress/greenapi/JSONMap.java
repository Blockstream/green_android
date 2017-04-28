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
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class JSONMap implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = getDateFormat();

    private static SimpleDateFormat getDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    public final Map<String, Object> mData; // Public for setting only

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
    public Integer getInt(final String k) { return get(k); }
    public Integer getInt(final String k, final Integer def) {
       final Integer v = get(k, null);
       return v != null ? v : def;
    }

    public String getString(final String k) { return get(k); }

    public Date getDate(final String k) throws ParseException {
        final String date = get(k);
        return DATE_FORMAT.parse(date);
    }

    public Sha256Hash getHash(final String k) {
        final String hash = get(k);
        return Sha256Hash.wrap(hash);
    }

    public Long getLong(final String k) {
        final String v = get(k);
        return Long.valueOf(v);
    }

    public float getFloat(final String k) {
        final String v = get(k);
        return Float.valueOf(v);
    }

    public Coin getCoin(final String k) {
        return Coin.valueOf(getLong(k));
    }

    public BigInteger getBigInteger(final String k) {
        return new BigInteger(String.valueOf(get(k)));
    }

    public byte[] getBytes(final String k) {
        return Wally.hex_to_bytes(getString(k));
    }

    public String toString() {
        return mData.toString();
    }

    public void putBytes(final String k, final byte[] v) {
        mData.put(k, Wally.hex_from_bytes(v));
    }
}
