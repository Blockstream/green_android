package com.greenaddress.greenapi.model;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class EventDataObservable extends Observable implements Observer {
    private List<EventData> mEventDataList = new LinkedList<>();
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
                pushEvent(new EventData(R.string.id_system_message, R.string.notification_format_string,
                                        new String[] {systemMessage}, new Date(), null));
            }
        });
    }

    public List<EventData> getEventDataList() {
        // the list is a read only copy, use pushEvent to add stuff here
        return new LinkedList<>(mEventDataList);
    }

    public void pushEventIfNotExist(final EventData eventData) {
        if (!mEventDataList.contains(eventData))
            pushEvent(eventData);
    }

    public void pushEvent(final EventData eventData) {
        Log.d("OBS", "pushEvent(" +  eventData + ")");
        mEventDataList.add(eventData);
        setChanged();
        notifyObservers();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof TwoFactorConfigDataObservable) {
            final TwoFactorConfigData twoFactorData =
                ((TwoFactorConfigDataObservable) observable).getTwoFactorConfigData();
            if (twoFactorData.isTwoFactorResetDisputed())
                pushEventIfNotExist(new EventData(R.string.id_twofactor_authentication,
                                                  R.string.id_warning_wallet_locked_by, new String[] {}, new Date(),
                                                  twoFactorData));
            else if (twoFactorData.getTwoFactorResetDaysRemaining() != null) {
                final Integer days = twoFactorData.getTwoFactorResetDaysRemaining();
                pushEventIfNotExist(new EventData(R.string.id_twofactor_authentication,
                                                  R.string.id_warning_wallet_locked_for,
                                                  new String[] {String.valueOf(days)}, new Date(), days));
            } // TODO not show in watch only or twoFactorData null
            else if (!twoFactorData.isAnyEnabled() ) {
                removeIfExist(R.string.id_set_up_twofactor_authentication);
                pushEventIfNotExist(new EventData(R.string.id_set_up_twofactor_authentication,
                                                  R.string.id_your_wallet_is_not_yet_fully, new String[] {}, new Date(),
                                                  null));
            } else if (twoFactorData.getEnabledMethods().size() == 1) {
                removeIfExist(R.string.id_set_up_twofactor_authentication);
                pushEventIfNotExist(new EventData(R.string.id_set_up_twofactor_authentication,
                                                  R.string.id_you_only_have_one_twofactor, new String[] {}, new Date(),
                                                  null));
            } else {
                removeIfExist(R.string.id_set_up_twofactor_authentication);
            }
            notifyObservers();
        }
    }

    public void remove(final EventData e) {
        mEventDataList.remove(e);
        setChanged();
        notifyObservers();
    }

    private void removeIfExist(final int idToRemove) {
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
