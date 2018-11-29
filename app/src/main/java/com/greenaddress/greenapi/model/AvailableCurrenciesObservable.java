package com.greenaddress.greenapi.model;

import android.util.Log;
import android.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

public class AvailableCurrenciesObservable extends Observable {
    private Map<String, Object> availableCurrencies;
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;

    public AvailableCurrenciesObservable(final GDKSession session, final ListeningExecutorService executor) {
        mSession = session;
        mExecutor = executor;
        refresh();
    }

    public void refresh() {
        mExecutor.submit(() -> {
            try {
                Map<String, Object> availableCurrencies = mSession.getAvailableCurrencies();
                setAvailableCurrencies(availableCurrencies);
            } catch (JsonProcessingException e) {
                Log.e("OBS", "getAvailableCurrencies error " +  e.getMessage());
            }
        });
    }

    public Map<String, Object> getAvailableCurrencies() {
        return availableCurrencies;
    }

    public List<String> getAvailableCurrenciesAsFormattedList(final String format) {
        if (getAvailableCurrenciesAsPairs() == null)
            return null;
        final List<String> list = new ArrayList<>();
        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs()) {
            list.add(String.format(format, pair.first, pair.second));
        }
        return list;
    }

    public List<String> getAvailableCurrenciesAsList() {
        if (getAvailableCurrenciesAsPairs() == null)
            return null;
        final List<String> list = new ArrayList<>();
        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs()) {
            list.add(String.format("%s %s", pair.first, pair.second));
        }
        return list;
    }

    private List<Pair<String, String>> getAvailableCurrenciesAsPairs() {
        if (availableCurrencies == null)
            return null;
        final List<Pair<String, String>> ret = new LinkedList<>();
        final Map<String, ArrayList<String>> perExchange = (Map) availableCurrencies.get("per_exchange");

        for (final String exchange : perExchange.keySet())
            for (final String currency : perExchange.get(exchange))
                ret.add(new Pair<>(currency, exchange));

        Collections.sort(ret, (lhs, rhs) -> lhs.first.compareTo(rhs.first));
        return ret;
    }

    public void setAvailableCurrencies(Map<String, Object> availableCurrencies) {
        Log.d("OBS", "setAvailableCurrencies(" +  availableCurrencies + ")");
        this.availableCurrencies = availableCurrencies;
        setChanged();
        notifyObservers();
    }

}
