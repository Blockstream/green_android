package com.greenaddress.greenbits.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.components.BottomOffsetDecoration;
import com.greenaddress.greenbits.ui.components.DividerItem;
import com.greenaddress.greenbits.ui.onboarding.SecurityActivity;
import com.greenaddress.greenbits.ui.preferences.GAPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends GAPreferenceFragment {

    private ContextThemeWrapper mContextThemeWrapper;
    private PreferenceCategory mEmptyNotifications;
    private Disposable eventsDisposable;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        Context activityContext = getActivity();

        final PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(activityContext);
        setPreferenceScreen(preferenceScreen);

        final TypedValue themeTypedValue = new TypedValue();
        activityContext.getTheme().resolveAttribute(R.attr.preferenceTheme, themeTypedValue, true);
        mContextThemeWrapper = new ContextThemeWrapper(activityContext, themeTypedValue.resourceId);
        mEmptyNotifications = new PreferenceCategory(mContextThemeWrapper);
        mEmptyNotifications.setTitle(R.string.id_your_notifications_will_be);
    }

    private List<EventData> getEvents() {
        final List<EventData> events = new ArrayList<>(getSession().getNotificationModel().getEvents());
        try {
            final TwoFactorConfigData config = getSession().getTwoFactorConfig();
            final boolean isReset = config.isTwoFactorResetActive();
            final int numMethods = config.getEnabledMethods().size();
            if (!isReset && numMethods == 0) {
                events.add(0, new EventData(R.string.id_set_up_2fa_authentication,
                                            R.string.id_your_wallet_is_not_yet_fully_secured));
            } else if (!isReset && numMethods == 1) {
                events.add(0,new EventData(R.string.id_you_only_have_one_2fa,
                                           R.string.id_please_enable_another_2fa));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            final String systemMessage = getSession().getSystemMessage();
            if (!TextUtils.isEmpty(systemMessage)) {
                // Add to system messages
                events.add(0, new EventData(R.string.id_system_message, R.string.notification_format_string,
                                            systemMessage));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return events;
    }

    private void showEvents(final List<EventData> events) {
        getPreferenceScreen().removeAll();
        for (final EventData e: events) {
            final Preference preference = new Preference(mContextThemeWrapper);
            preference.setTitle(e.getTitle());
            final String description = getDescription(e);
            preference.setSummary(description);
            if (e.getTitle() == R.string.id_system_message) {
                preference.setOnPreferenceClickListener(preference1 -> {
                    final Intent intent = new Intent(getActivity(), MessagesActivity.class);
                    intent.putExtra("message", description);
                    intent.putExtra("event", e);
                    startActivity(intent);
                    return false;
                });
            } else if (e.getTitle() == R.string.id_set_up_2fa_authentication ||
                       e.getTitle() == R.string.id_you_only_have_one_2fa) {
                preference.setOnPreferenceClickListener(preference1 -> {
                    Bridge.INSTANCE.twoFactorAuthentication(getActivity());
                    return false;
                });
            }

            getPreferenceScreen().addPreference(preference);
        }

        if (getPreferenceScreen().getPreferenceCount() == 0) {
            getPreferenceScreen().addPreference(mEmptyNotifications);
        } else {
            getPreferenceScreen().removePreference(mEmptyNotifications);
        }
    }

    private String getDescription(final EventData event) {
        final int d = event.getDescription();
        if (d == R.string.id_new_incoming_transaction_in ||
            d == R.string.id_new_outgoing_transaction_from) {
            final JsonNode tx = (JsonNode) event.getValue();
            try {
                final long satoshi = tx.get("satoshi").asLong(0);
                final String amount = Conversion.getBtc(getSession(), satoshi, true);
                return getString(d, new Object[] {"", amount});
            } catch (final Exception e) {
                Log.e("", "Liquid or Conversion error: " + e.getLocalizedMessage());
                return "";
            }
        } else if (d == R.string.id_new_transaction_involving) {
            try {
                return getString(d, "");
            } catch (final Exception e) {
                return "";
            }
        } else if (d == R.string.notification_format_string ||
                   d == R.string.id_your_wallet_is_locked_for_a_2fa ||
                   d == R.string.id_days_remaining_s ||
                   d == R.string.id_s_blocks_left) {
            try {
                return getString(d, event.getValue());
            } catch (final Exception e) {
                return "";
            }
        }
        try {
            return getString(d);
        } catch (final Exception e) {
            return "";
        }
    }

    private void setup() {
        Observable.just(getSession())
        .observeOn(Schedulers.computation())
        .map(session -> {
            return getEvents();
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(events -> {
            showEvents(events);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isZombie())
            return;
        if (eventsDisposable != null)
            eventsDisposable.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isZombie())
            return;
        eventsDisposable = getSession().getNotificationModel()
                           .getEventsObservable()
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe((list) -> {
            setup();
        });

        setup();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView listView = getListView();
        listView.addItemDecoration(new DividerItem(getContext()));
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        listView.addItemDecoration(bottomOffsetDecoration);
        setDivider(null);
        listView.setFocusable(false);
    }
}
