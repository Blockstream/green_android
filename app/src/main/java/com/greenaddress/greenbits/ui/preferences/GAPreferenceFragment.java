package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.greenaddress.Bridge;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.LoggedActivity;

import static android.content.Context.MODE_PRIVATE;

@Deprecated
public class GAPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = GAPreferenceFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {}

    private static final Preference.OnPreferenceChangeListener onPreferenceChanged =
        new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    protected void bindPreferenceSummaryToValue(final Preference preference) {
        preference.setOnPreferenceChangeListener(onPreferenceChanged);
        // Trigger the listener immediately with the preference's
        // current value.
        final String currentVal = cfg().getString(preference.getKey(), "");
        onPreferenceChanged.onPreferenceChange(preference, currentVal);
    }

    protected < T > T find(final String preferenceName) {
        return (T) findPreference(preferenceName);
    }

    protected void logout() {
        if (getActivity() instanceof LoggedActivity)
            ((LoggedActivity) getActivity()).logout(null);
        else if (getActivity() instanceof GaPreferenceActivity)
            ((GaPreferenceActivity) getActivity()).logout();
    }

    protected boolean openURI(final String uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        return false;
    }

    public SharedPreferences cfg() {
        return getContext().getSharedPreferences(network(), MODE_PRIVATE);
    }

    public String network() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(PrefKeys.NETWORK_ID_ACTIVE,
                                                                                     "mainnet");
    }
    public NetworkData getNetwork() {
        return Bridge.INSTANCE.getCurrentNetworkData(getContext());
    }

    public boolean warnIfOffline(final Activity activity) {
        /*if (getConnectionManager().isOffline()) {
            UI.toast(activity, R.string.id_connection_failed, Toast.LENGTH_LONG);
            return true;
           }*/
        return false;
    }

    // Returns true if we are being restored without an activity or service
    protected boolean isZombie() {
        return getActivity() == null;
    }

    public Session getSession() {
        return Session.getSession();
    }
}
