package com.greenaddress.greenbits.ui.preferences;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;

public class SPVPreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    private EditTextPreference mTrustedPeer;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_spv);
        setHasOptionsMenu(true);

        mTrustedPeer = (EditTextPreference) findPreference("trusted_peer");
        bindPreferenceSummaryToValue(mTrustedPeer);

        final Preference resetSPV = findPreference("reset_spv");
        resetSPV.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                mService.spv.reset();
                return false;
            }
        });

        final CheckBoxPreference spvEnabled = (CheckBoxPreference) findPreference("spvEnabled");
        final boolean enabled = mService.isSPVEnabled();
        mTrustedPeer.setEnabled(enabled);
        spvEnabled.setChecked(enabled);
        spvEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                mTrustedPeer.setEnabled((Boolean) newValue);

                new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(final Object[] params) {
                        mService.spv.setEnabled((Boolean) newValue);
                        return null;
                    }
                }.execute();
                return true;
            }
        });

        final String address = mService.getTrustedPeers();

        if (!address.isEmpty()) {
            mTrustedPeer.setText(address);
            mTrustedPeer.setSummary(address);
        } else {
            mTrustedPeer.setSummary(R.string.trustedspvExample);
        }
        mTrustedPeer.setOnPreferenceChangeListener(this);
        getActivity().setResult(getActivity().RESULT_OK, null);
    }

    private boolean isBadAddress(final String s) {
        try {
            final int idx = s.indexOf(":");
            if (idx != -1)
                Integer.parseInt(s.substring(idx + 1));
        } catch (final NumberFormatException e) {
            return true;
        }

        if (s.isEmpty() || s.contains("."))
            return false;

        GaActivity.popup(getActivity(), R.string.enterValidAddressTitle, android.R.string.ok)
                  .content(R.string.enterValidAddressText).build().show();
        return true;
    }

    private void setTrustedPeers(final String peers) {

        mService.setTrustedPeers(peers);
        mService.setUserConfig("trusted_peer_addr", peers, true);
        if (!peers.isEmpty())
            mTrustedPeer.setSummary(peers);
        else
            mTrustedPeer.setSummary(R.string.trustedspvExample);

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object[] params) {
                boolean alreadySyncing = mService.spv.stopSPVSync();
                mService.spv.setUpSPV();
                if (alreadySyncing)
                    mService.spv.startSpvSync();
                return null;
            }
        }.execute();
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {

        if (mService.getTrustedPeers().equals(newValue))
            return false;

        try {
            final String newString = newValue.toString().trim().replaceAll("\\s","");

            if (newString.isEmpty()) {
                setTrustedPeers(newString);
                return true;
            }

            for (final String s: newString.split(","))
                if (isBadAddress(s))
                    return true;

            if (newString.toLowerCase().contains(".onion")) {

                if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    (mService.getProxyHost() == null || mService.getProxyPort() == null)) {
                    // Certain ciphers have been deprecated in API 23+, breaking Orchid
                    // and HS connectivity (Works with Orbot socks proxy if set)
                    GaActivity.popup(getActivity(), R.string.enterValidAddressTitleTorDisabled, android.R.string.ok)
                              .content(R.string.enterValidAddressTextTorDisabled).build().show();
                    return true;
                }

                setTrustedPeers(newString);
                return true;
            }

            // Force the user to confirm that they want to use a non-Tor host
            GaActivity.popup(getActivity(), R.string.changingWarnOnionTitle)
                      .content(R.string.changingWarnOnionText)
                      .onPositive(new MaterialDialog.SingleButtonCallback() {
                          @Override
                          public void onClick(final MaterialDialog dlg, final DialogAction which) {
                              setTrustedPeers(newString);
                          }
                      }).build().show();
            return true;
        } catch (final Exception e) {
            return false; // not set
        }
    }
}
