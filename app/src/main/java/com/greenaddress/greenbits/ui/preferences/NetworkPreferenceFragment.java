package com.greenaddress.greenbits.ui.preferences;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ui.R;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NetworkPreferenceFragment extends GAPreferenceFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_network);
        setHasOptionsMenu(true);

        final Preference host = findPreference("proxy_host");
        host.setOnPreferenceChangeListener(mListener);
        host.setSummary(mService.getProxyHost());
        final Preference port = findPreference("proxy_port");
        port.setSummary(mService.getProxyPort());
        port.setOnPreferenceChangeListener(mListener);
        final Preference torEnabled  = findPreference("tor_enabled");
        if (Network.GAIT_ONION == null)
            torEnabled.setEnabled(false);
        else {
            torEnabled.setSummary(getString(R.string.torSummary, Network.GAIT_ONION));
            torEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object o) {
                    mService.disconnect(true);
                    return true;
                }
            });
        }
}

    private final Preference.OnPreferenceChangeListener mListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object o) {
            preference.setSummary(o.toString());
            mService.disconnect(true);
            return true;
        }
    };
}
