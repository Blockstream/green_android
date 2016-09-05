package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class SPVPreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceChangeListener,
               Preference.OnPreferenceClickListener {

    private EditTextPreference mTrustedPeer;
    private Preference mResetSPV;
    private CheckBoxPreference mSPVEnabled;
    private CheckBoxPreference mSPVSyncOnMobile;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_spv);
        setHasOptionsMenu(true);

        mTrustedPeer = find("trusted_peer");
        mResetSPV = find("reset_spv");
        mSPVEnabled = find("spvEnabled");
        mSPVSyncOnMobile = find("spvSyncMobile");

        if (mService.isWatchOnly()) {
            // Do not allow editing of SPV prefs from watch only logins
            mTrustedPeer.setEnabled(false);
            mResetSPV.setEnabled(false);
            mSPVEnabled.setEnabled(false);
            mSPVSyncOnMobile.setEnabled(false);
            UI.toast(getActivity(), R.string.spvSettingsWatchOnly, Toast.LENGTH_LONG);
        } else {
            // Initialise values and bindings for preference changes
            bindPreferenceSummaryToValue(mTrustedPeer);

            mSPVEnabled.setChecked(mService.isSPVEnabled());
            mSPVSyncOnMobile.setChecked(mService.isSPVSyncOnMobileEnabled());

            mTrustedPeer.setEnabled(mService.isSPVEnabled());
            final boolean setTextValue = true;
            setTrustedPeersPreference(mService.getSPVTrustedPeers(), setTextValue);

            mResetSPV.setOnPreferenceClickListener(this);
            mSPVEnabled.setOnPreferenceChangeListener(this);
            mSPVSyncOnMobile.setOnPreferenceChangeListener(this);
            mTrustedPeer.setOnPreferenceChangeListener(this);
        }

        getActivity().setResult(Activity.RESULT_OK, null);
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
        mService.setSPVTrustedPeersAsync(peers);
    }

    private boolean onSPVEnabledChanged(final Boolean newValue) {
        mTrustedPeer.setEnabled(newValue);
        mService.setSPVEnabledAsync(newValue);
        return true;
    }

    private boolean onSPVSyncOnMobileChanged(final Boolean newValue) {
        mService.setSPVSyncOnMobileEnabledAsync(newValue);
        return true;
    }

    private boolean onTrustedPeerChange(final String newValue) {

        final String peers = newValue.trim().replaceAll("\\s","");
        if (mService.getSPVTrustedPeers().equals(peers))
            return false;

        if (peers.isEmpty()) {
            setTrustedPeers(peers);
            return true;
        }

        for (final String peer: peers.split(","))
            if (GaService.isBadAddress(peer)) {
                UI.popup(getActivity(), R.string.enterValidAddressTitle, android.R.string.ok)
                        .content(R.string.enterValidAddressText).build().show();
                return false;
            }

        if (peers.toLowerCase().contains(".onion")) {
            // Tor address
            if (!mService.isProxyEnabled()) {
                UI.popup(getActivity(), R.string.enterValidAddressTitleTorDisabled, android.R.string.ok)
                  .content(R.string.enterValidAddressTextTorDisabled).build().show();
               return false;
            }
            setTrustedPeers(peers);
            return true;
        }

        // Force the user to confirm that they want to use a non-Tor host
        UI.popup(getActivity(), R.string.changingWarnOnionTitle)
          .content(R.string.changingWarnOnionText)
          .onPositive(new MaterialDialog.SingleButtonCallback() {
              @Override
              public void onClick(final MaterialDialog dlg, final DialogAction which) {
                  setTrustedPeers(peers);
              }
          }).build().show();
        return true;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference == mSPVEnabled)
            return onSPVEnabledChanged((Boolean) newValue);
        if (preference == mSPVSyncOnMobile)
            return onSPVSyncOnMobileChanged((Boolean) newValue);
        if (preference == mTrustedPeer)
            return onTrustedPeerChange((String) newValue);
        return false;
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preference == mResetSPV)
            mService.resetSPVAsync();
        return false;
    }
}
