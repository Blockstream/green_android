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
import com.afollestad.materialdialogs.Theme;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.ui.R;

import java.util.Observable;
import java.util.Observer;

public class SPVPreferenceFragment extends GAPreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_spv);
        setHasOptionsMenu(true);
        GAPreferenceFragment.bindPreferenceSummaryToValue(findPreference("trusted_peer"));
        final Preference reset_spv = getPreferenceManager().findPreference("reset_spv");
        reset_spv.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final boolean enabled = gaService.isSPVEnabled();

                if (enabled) {
                    // Stop SPV
                    try {
                        if (gaService.spv.isPeerGroupRunning())
                            gaService.spv.stopSPVSync();
                        gaService.spv.tearDownSPV();
                    } catch (final NullPointerException e) {
                        // ignore
                    }
                }

                // Delete all SPV data
                gaService.spv.resetSpv();

                if (enabled) {
                    // Restart SPV
                    gaService.spv.setUpSPV();
                    // FIXME: enabled under WiFi only
                    gaService.spv.startSpvSync();
                }
                return false;
            }
        });

        final CheckBoxPreference spvEnabled = (CheckBoxPreference) findPreference("spvEnabled");
        final EditTextPreference trusted_peer = (EditTextPreference) getPreferenceManager().findPreference("trusted_peer");
        final boolean enabled = gaService.isSPVEnabled();
        trusted_peer.setEnabled(enabled);
        spvEnabled.setChecked(enabled);
        spvEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                final Boolean nowEnabled = (Boolean) newValue;

                gaService.cfgEdit("SPV").putBoolean("enabled", nowEnabled).apply();
                trusted_peer.setEnabled(nowEnabled);

                new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(final Object[] params) {

                        if (nowEnabled) {
                            gaService.spv.setUpSPV();
                            gaService.spv.startSpvSync();
                        } else {
                            if (gaService.spv.isPeerGroupRunning())
                                gaService.spv.stopSPVSync();
                            gaService.spv.tearDownSPV();
                        }
                        return null;
                    }
                }.execute();
                return true;
            }
        });

        final String address = gaService.cfg("TRUSTED").getString("address", "");

        if (!address.isEmpty()) {
            trusted_peer.setText(address);
            trusted_peer.setSummary(address);
        } else {
            trusted_peer.setSummary(R.string.trustedspvExample);
        }
        trusted_peer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            class SPVAsync extends AsyncTask<Object, Object, Object>{

                @Nullable
                @Override
                protected Object doInBackground(Object[] params) {
                    boolean alreadySyncing = false;
                    if (gaService.spv.isPeerGroupRunning()) {
                        alreadySyncing = true;
                        gaService.spv.stopSPVSync();
                    }
                    gaService.spv.tearDownSPV();
                    System.gc(); //May help save slightly lower heap size devices.
                    gaService.spv.setUpSPV();
                    if (alreadySyncing) {
                        gaService.spv.startSpvSync();
                    }
                    return null;
                }
            }

            boolean isBadAddress(final String s) {
                boolean addrCorrect;
                try {
                    final int idx = s.indexOf(":");
                    if (idx != -1) {
                        Integer.parseInt(s.substring(idx + 1));
                    }
                } catch (@NonNull final NumberFormatException e) {
                    return true;
                }
                addrCorrect = s.isEmpty() || s.contains(".");

                if (!addrCorrect) {
                    new MaterialDialog.Builder(SPVPreferenceFragment.this.getActivity())
                            .title(R.string.enterValidAddressTitle)
                            .content(R.string.enterValidAddressText)
                            .positiveColorRes(R.color.accent)
                            .negativeColorRes(R.color.white)
                            .titleColorRes(R.color.white)
                            .contentColorRes(android.R.color.white)
                            .theme(Theme.DARK)
                            .positiveText("OK")
                            .build().show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onPreferenceChange(final Preference preference, @NonNull final Object newValue) {

                if (gaService.cfg("TRUSTED").getString("address", "").equals(newValue))
                    return false;

                try {
                    final String newString = newValue.toString().trim().replaceAll("\\s","");
                    if (newString.contains(",")) {
                        for (final String s: newString.split(",")) {
                            if (isBadAddress(s)) {
                                return true;
                            }
                        }
                    } else {
                        if (isBadAddress(newString)) {
                            return true;
                        }
                    }

                    final String newLower = newString.toLowerCase();

                    if (newString.isEmpty() || newLower.contains(".onion")) {

                        final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        final String proxyHost = sharedPref.getString("proxy_host", null);
                        final String proxyPort = sharedPref.getString("proxy_port", null);

                        if (currentapiVersion >= 23 &&
                                (newLower.contains(".onion")) && (proxyHost == null || proxyPort == null)) {
                            // Certain ciphers have been deprecated in API 23+, breaking Orchid
                            // and HS connectivity.
                            // but work with Orbot socks if set
                            new MaterialDialog.Builder(SPVPreferenceFragment.this.getActivity())
                                    .title(R.string.enterValidAddressTitleTorDisabled)
                                    .content(R.string.enterValidAddressTextTorDisabled)
                                    .positiveColorRes(R.color.accent)
                                    .negativeColorRes(R.color.white)
                                    .titleColorRes(R.color.white)
                                    .contentColorRes(android.R.color.white)
                                    .theme(Theme.DARK)
                                    .positiveText("OK")
                                    .build().show();
                            return true;
                        }

                        gaService.cfgEdit("TRUSTED").putString("address", newString).apply();

                        gaService.setUserConfig("trusted_peer_addr", newString, true);
                        if (!newString.isEmpty())
                            trusted_peer.setSummary(newString);
                        else
                            trusted_peer.setSummary(R.string.trustedspvExample);

                        new SPVAsync().execute();
                    }
                    else {
                        new MaterialDialog.Builder(SPVPreferenceFragment.this.getActivity())
                                .title(R.string.changingWarnOnionTitle)
                                .content(R.string.changingWarnOnionText)
                                .positiveText("OK")
                                .negativeText("Cancel")
                                .positiveColorRes(R.color.accent)
                                .negativeColorRes(R.color.white)
                                .titleColorRes(R.color.white)
                                .contentColorRes(android.R.color.white)
                                .theme(Theme.DARK)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                                        new SPVAsync().execute();
                                        gaService.cfgEdit("TRUSTED").putString("address", newString).apply();
                                        gaService.setUserConfig("trusted_peer_addr", newString, true);
                                        trusted_peer.setSummary(newString);
                                    }
                                })
                                .build().show();
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
