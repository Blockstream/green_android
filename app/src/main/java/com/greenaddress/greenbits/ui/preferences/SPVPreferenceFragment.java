package com.greenaddress.greenbits.ui.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
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

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SPVPreferenceFragment extends GAPreferenceFragment {

    @Nullable
    private Observer wiFiObserver = null;

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
                final SharedPreferences spvPreferences = getActivity().getSharedPreferences("SPV", Context.MODE_PRIVATE);

                // stop SPV if enabled

                if (spvPreferences.getBoolean("enabled", true)) {
                    try {
                        if (gaService.spv.isPeerGroupRunning()) {
                            gaService.spv.stopSPVSync();
                        }
                        gaService.spv.tearDownSPV();
                    } catch (final NullPointerException e) {
                        // ignore
                    }

                }

                // delete all spv data

                gaService.spv.resetSpv();

                // restart spv if it was enabled

                if (spvPreferences.getBoolean("enabled", true)) {
                    gaService.spv.setUpSPV();
                    if (gaService.getCurBlock() - gaService.spv.getSpvHeight() > 1000) {
                        if (gApp.getConnectionObservable().isWiFiUp()) {
                            gaService.spv.startSpvSync();
                        } else {
                            // no wifi - do we want to sync?
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    askUserForSpvNoWiFi();
                                }
                            });
                        }
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gaService.spv.startSpvSync();
                            }
                        });

                    }
                }
                return false;
            }
        });

        final CheckBoxPreference spvEnabled = (CheckBoxPreference) findPreference("spvEnabled");
        final SharedPreferences spvPreferences = getActivity().getSharedPreferences("SPV", Context.MODE_PRIVATE);
        final EditTextPreference trusted_peer = (EditTextPreference) getPreferenceManager().findPreference("trusted_peer");
        trusted_peer.setEnabled(spvPreferences.getBoolean("enabled", true));
        spvEnabled.setChecked(spvPreferences.getBoolean("enabled", true));
        spvEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {

                class SPVButtonPrefAsync extends AsyncTask<Object, Object, Object> {

                    @Nullable
                    @Override
                    protected Object doInBackground(final Object[] params) {
                        final Boolean nowEnabled = (Boolean) newValue;

                        if (nowEnabled) {
                            gaService.setSpvWiFiDialogShown(false);
                            gaService.spv.setUpSPV();
                            if (gaService.getCurBlock() - gaService.spv.getSpvHeight() > 1000) {
                                if (gApp.getConnectionObservable().isWiFiUp()) {
                                    gaService.spv.startSpvSync();
                                } else {
                                    // no wifi - do we want to sync?
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            askUserForSpvNoWiFi();
                                        }
                                    });
                                }
                            } else {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        gaService.spv.startSpvSync();
                                    }
                                });

                            }

                        } else {
                            if (gaService.spv.isPeerGroupRunning()) {
                                gaService.spv.stopSPVSync();
                            }
                            gaService.spv.tearDownSPV();
                        }
                        return null;
                    }
                }

                final Boolean nowEnabled = (Boolean) newValue;
                final SharedPreferences.Editor editor = spvPreferences.edit();
                editor.putBoolean("enabled", nowEnabled);
                editor.apply();
                trusted_peer.setEnabled(nowEnabled);

                new SPVButtonPrefAsync().execute();
                return true;
            }
        });

        final SharedPreferences trustedPreferences = getActivity().getSharedPreferences("TRUSTED", Context.MODE_PRIVATE);
        trusted_peer.setText(trustedPreferences.getString("address", ""));
        trusted_peer.setSummary(trustedPreferences.getString("address", ""));
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
                            .title(getResources().getString(R.string.enterValidAddressTitle))
                            .content(getResources().getString(R.string.enterValidAddressText))
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

                if (trustedPreferences.getString("address", "").equals(newValue)) {
                    return false;
                }
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
                                    .title(getResources().getString(R.string.enterValidAddressTitleTorDisabled))
                                    .content(getResources().getString(R.string.enterValidAddressTextTorDisabled))
                                    .positiveColorRes(R.color.accent)
                                    .negativeColorRes(R.color.white)
                                    .titleColorRes(R.color.white)
                                    .contentColorRes(android.R.color.white)
                                    .theme(Theme.DARK)
                                    .positiveText("OK")
                                    .build().show();
                            return true;
                        }

                        final SharedPreferences.Editor editor = trustedPreferences.edit();
                        editor.putString("address", newString);
                        editor.apply();

                        gaService.setAppearanceValue("trusted_peer_addr", newString, true);
                        trusted_peer.setSummary(newString);
                        new SPVAsync().execute();
                    }
                    else {
                        new MaterialDialog.Builder(SPVPreferenceFragment.this.getActivity())
                                .title(getResources().getString(R.string.changingWarnOnionTitle))
                                .content(getResources().getString(R.string.changingWarnOnionText))
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
                                        final SharedPreferences.Editor editor = trustedPreferences.edit();
                                        editor.putString("address", newString);
                                        editor.apply();

                                        gaService.setAppearanceValue("trusted_peer_addr", newString, true);
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
    }
    private void askUserForSpvNoWiFi() {
        gaService.setSpvWiFiDialogShown(true);
        new MaterialDialog.Builder(SPVPreferenceFragment.this.getActivity())
                .title(getResources().getString(R.string.spvNoWiFiTitle))
                .content(getResources().getString(R.string.spvNoWiFiText))
                .positiveText(R.string.spvNoWiFiSyncAnyway)
                .negativeText(R.string.spvNoWifiWaitForWiFi)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.white)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                        gaService.spv.startSpvSync();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                        makeWiFiObserver();
                    }
                })
                .build().show();
    }

    private void makeWiFiObserver() {
        if (wiFiObserver != null) return;
        final ConnectivityObservable connObservable = gApp.getConnectionObservable();
        if (connObservable.isWiFiUp()) {
            gaService.spv.startSpvSync();
            return;
        }
        wiFiObserver = new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                if (connObservable.isWiFiUp()) {
                    gaService.spv.startSpvSync();
                    connObservable.deleteObserver(wiFiObserver);
                    wiFiObserver = null;
                }
            }
        };
        connObservable.addObserver(wiFiObserver);
    }
}