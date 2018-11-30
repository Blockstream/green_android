package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class EventDataObservable extends Observable implements Observer {
    private List<EventData> mEventDataList = new ArrayList<>();
    private GDKSession mSession;
    private ListeningExecutorService mExecutor;

    public boolean hasEvents() {
        return !mEventDataList.isEmpty();
    }

    private EventDataObservable() {}

    public EventDataObservable(final GDKSession session, final ListeningExecutorService executor) {
        mSession = session;
        mExecutor = executor;
        refresh();
    }

    public void refresh() {
        mExecutor.submit(() -> {
            // Add system messages
            final String systemMessage = mSession.getSystemMessage();
            if (systemMessage != null && !systemMessage.isEmpty()) {
                pushEvent(new EventData(R.string.id_system_message,
                                        R.string.notification_format_string,
                                        systemMessage));
            }
        });
    }

    public List<EventData> getEventDataList() {
        // the list is a read only copy, use pushEvent to add stuff here
        return new ArrayList<>(mEventDataList);
    }

    public void pushEvent(final EventData eventData) {
        if (mEventDataList.contains(eventData))
            return;
        if (findTx(eventData) != null)
            return;
        Log.d("OBS", "pushEvent(" +  eventData + ")");
        mEventDataList.add(eventData);
        setChanged();
        notifyObservers();
    }

    private EventData findTx(final String hash) {
        for (final EventData e : mEventDataList) {
            if (e.getTitle() == R.string.id_new_transaction &&
                    hash.equals( ((TransactionData)e.getValue()).getTxhash()))
                return e;
        }
        return null;
    }

    private EventData findTx(final EventData eventData) {
        if (eventData.getTitle() != R.string.id_new_transaction)
            return null;
        final TransactionData tx = (TransactionData) eventData.getValue();
        final String txhash = tx.getTxhash();
        return findTx(txhash);
    }

    public void removeTx(final String txhash) {
        final EventData tx = findTx(txhash);
        if (tx != null)
            remove(tx);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof TwoFactorConfigDataObservable) {
            final TwoFactorConfigData twoFactorData =
                ((TwoFactorConfigDataObservable) observable).getTwoFactorConfigData();
            if (twoFactorData.isTwoFactorResetDisputed())
                pushEvent(new EventData(R.string.id_twofactor_authentication,
                                        R.string.id_warning_wallet_locked_by));
            else if (twoFactorData.getTwoFactorResetDaysRemaining() != null) {
                final Integer days = twoFactorData.getTwoFactorResetDaysRemaining();
                pushEvent(new EventData(R.string.id_twofactor_authentication,
                                        R.string.id_warning_wallet_locked_for,
                                        days));
            } // TODO not show in watch only or twoFactorData null
            else if (!twoFactorData.isAnyEnabled() ) {
                removeType(R.string.id_set_up_twofactor_authentication);
                pushEvent(new EventData(R.string.id_set_up_twofactor_authentication,
                                        R.string.id_your_wallet_is_not_yet_fully));
            } else if (twoFactorData.getEnabledMethods().size() == 1) {
                removeType(R.string.id_set_up_twofactor_authentication);
                pushEvent(new EventData(R.string.id_set_up_twofactor_authentication,
                                        R.string.id_you_only_have_one_twofactor));
            } else {
                removeType(R.string.id_set_up_twofactor_authentication);
            }
            notifyObservers();
        }
    }

    public void remove(final EventData e) {
        mEventDataList.remove(e);
        setChanged();
        notifyObservers();
    }

    private void removeType(final int idToRemove) {
        List<EventData> toRemove = new ArrayList<>();
        for (EventData current : mEventDataList) {
            if (idToRemove == current.getTitle()) {
                toRemove.add(current);
            }
        }
        mEventDataList.removeAll(toRemove);
        setChanged();
    }
}
