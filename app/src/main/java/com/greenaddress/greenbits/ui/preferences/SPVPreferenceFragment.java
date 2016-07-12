package com.greenaddress.greenbits.ui.preferences;

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
                mService.resetSPV();
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
                mService.setSPVEnabled((Boolean) newValue);
                return true;
            }
        });

        final boolean setTextValue = true;
        setTrustedPeersPreference(mService.getTrustedPeers(), setTextValue);
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

    private void setTrustedPeersPreference(final String peers, final boolean setTextValue) {

        if (peers.isEmpty()) {
            mTrustedPeer.setSummary(R.string.trustedspvExample);
            return;
        }
        if (setTextValue)
            mTrustedPeer.setText(peers);
        mTrustedPeer.setSummary(peers);
    }

    private void setTrustedPeers(final String peers) {
        final boolean setTextValue = false;
        setTrustedPeersPreference(peers, setTextValue);
        mService.setTrustedPeers(peers);
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {

        if (mService.getTrustedPeers().equals(newValue))
            return false;

        try {
            final String peers = newValue.toString().trim().replaceAll("\\s","");

            if (peers.isEmpty()) {
                setTrustedPeers(peers);
                return true;
            }

            for (final String peer: peers.split(","))
                if (isBadAddress(peer))
                    return true;

            if (peers.toLowerCase().contains(".onion")) {

                if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    (mService.getProxyHost() == null || mService.getProxyPort() == null)) {
                    // Certain ciphers have been deprecated in API 23+, breaking Orchid
                    // and HS connectivity (Works with Orbot socks proxy if set)
                    GaActivity.popup(getActivity(), R.string.enterValidAddressTitleTorDisabled, android.R.string.ok)
                              .content(R.string.enterValidAddressTextTorDisabled).build().show();
                    return true;
                }

                setTrustedPeers(peers);
                return true;
            }

            // Force the user to confirm that they want to use a non-Tor host
            GaActivity.popup(getActivity(), R.string.changingWarnOnionTitle)
                      .content(R.string.changingWarnOnionText)
                      .onPositive(new MaterialDialog.SingleButtonCallback() {
                          @Override
                          public void onClick(final MaterialDialog dlg, final DialogAction which) {
                              setTrustedPeers(peers);
                          }
                      }).build().show();
            return true;
        } catch (final Exception e) {
            return false; // not set
        }
    }
}
