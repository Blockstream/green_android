package com.greenaddress.greenbits.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;

import java.util.Observable;
import java.util.Observer;

public class SPVPreferenceFragment extends GAPreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_spv);
        setHasOptionsMenu(true);
        bindPreferenceSummaryToValue(findPreference("trusted_peer"));

        final Preference reset_spv = getPreferenceManager().findPreference("reset_spv");
        reset_spv.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                mService.spv.reset();
                return false;
            }
        });

        final CheckBoxPreference spvEnabled = (CheckBoxPreference) findPreference("spvEnabled");
        final EditTextPreference trusted_peer = (EditTextPreference) getPreferenceManager().findPreference("trusted_peer");
        final boolean enabled = mService.isSPVEnabled();
        trusted_peer.setEnabled(enabled);
        spvEnabled.setChecked(enabled);
        spvEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                trusted_peer.setEnabled((Boolean) newValue);

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

        final String address = mService.cfg("TRUSTED").getString("address", "");

        if (!address.isEmpty()) {
            trusted_peer.setText(address);
            trusted_peer.setSummary(address);
        } else {
            trusted_peer.setSummary(R.string.trustedspvExample);
        }
        trusted_peer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            class SPVAsync extends AsyncTask<Object, Object, Object>{

                @Override
                protected Object doInBackground(Object[] params) {
                    boolean alreadySyncing = mService.spv.stopSPVSync();
                    mService.spv.setUpSPV();
                    if (alreadySyncing)
                        mService.spv.startSpvSync();
                    return null;
                }
            }

            boolean isBadAddress(final String s) {
                try {
                    final int idx = s.indexOf(":");
                    if (idx != -1)
                        Integer.parseInt(s.substring(idx + 1));
                } catch (@NonNull final NumberFormatException e) {
                    return true;
                }

                if (s.isEmpty() || s.contains("."))
                    return false;

                GaActivity.Popup(SPVPreferenceFragment.this.getActivity(),
                                 getString(R.string.enterValidAddressTitle), android.R.string.ok)
                          .content(R.string.enterValidAddressText).build().show();
                return true;
            }

            @Override
            public boolean onPreferenceChange(final Preference preference, @NonNull final Object newValue) {

                if (mService.cfg("TRUSTED").getString("address", "").equals(newValue))
                    return false;

                try {
                    final String newString = newValue.toString().trim().replaceAll("\\s","");
                    for (final String s: newString.split(","))
                        if (isBadAddress(s))
                            return true;

                    final String newLower = newString.toLowerCase();

                    if (newString.isEmpty() || newLower.contains(".onion")) {

                        final int currentapiVersion = android.os.Build.VERSION.SDK_INT;

                        if (currentapiVersion >= 23 && newLower.contains(".onion") &&
                            (mService.getProxyHost() == null || mService.getProxyPort() == null)) {
                            // Certain ciphers have been deprecated in API 23+, breaking Orchid
                            // and HS connectivity.
                            // but work with Orbot socks if set
                            GaActivity.Popup(SPVPreferenceFragment.this.getActivity(),
                                             getString(R.string.enterValidAddressTitleTorDisabled), android.R.string.ok)
                                      .content(R.string.enterValidAddressTextTorDisabled).build().show();
                            return true;
                        }

                        mService.cfgEdit("TRUSTED").putString("address", newString).apply();

                        mService.setUserConfig("trusted_peer_addr", newString, true);
                        if (!newString.isEmpty())
                            trusted_peer.setSummary(newString);
                        else
                            trusted_peer.setSummary(R.string.trustedspvExample);

                        new SPVAsync().execute();
                    }
                    else {
                        GaActivity.Popup(SPVPreferenceFragment.this.getActivity(),
                                         getString(R.string.changingWarnOnionTitle))
                                  .content(R.string.changingWarnOnionText)
                                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                                      @Override
                                      public void onClick(final @NonNull MaterialDialog dlg, final @NonNull DialogAction which) {
                                          new SPVAsync().execute();
                                          mService.cfgEdit("TRUSTED").putString("address", newString).apply();
                                          mService.setUserConfig("trusted_peer_addr", newString, true);
                                          trusted_peer.setSummary(newString);
                                      }
                                  }).build().show();
                    }

                    return true;
                } catch (@NonNull final Exception e) {
                    // not set
                }
                return false;
            }
        });
        getActivity().setResult(getActivity().RESULT_OK, null);
    }
}
