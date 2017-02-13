package com.greenaddress.greenapi;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.Serializable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class JSONMap implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = getDateFormat();

    private static SimpleDateFormat getDateFormat() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df;
    }

    private final Map<String, Object> mData;

    public JSONMap(final Map<String, Object> jsonMap) { mData = jsonMap; }

    public boolean containsKey(final String k) { return mData.containsKey(k); }

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

    public Boolean getBool(final String k) { return get(k); }
    public Integer getInt(final String k) { return get(k); }

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

    public Coin getCoin(final String k) {
        return Coin.valueOf(getLong(k));
    }

    public String toString() {
        return mData.toString();
    }
}
