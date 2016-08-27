package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.PinSaveActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.List;

public class GeneralPreferenceFragment extends GAPreferenceFragment {
    private static final int PINSAVE = 1337;
    private static final String mMicroSymbol = Html.fromHtml("&micro;").toString();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);

        // -- handle timeout
        int timeout = mService.getAutoLogoutMinutes();
        getPreferenceManager().getSharedPreferences().edit()
                              .putString("altime", Integer.toString(timeout))
                              .apply();
        final Preference altime = find("altime");
        altime.setSummary(String.format("%d %s", timeout, getResources().getString(R.string.autologout_time_default)));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                try {
                    final Integer altimeout = Integer.parseInt(newValue.toString());
                    mService.setUserConfig("altimeout", altimeout, true);
                    preference.setSummary(String.format("%d %s", altimeout, getResources().getString(R.string.autologout_time_default)));
                    return true;
                } catch (final Exception e) {
                    // not set
                }
                return false;
            }
        });

        // -- handle mnemonics

        final String mnemonic = mService.getMnemonics();
        if (mnemonic != null) {
            final Preference passphrase = find("mnemonic_passphrase");
            passphrase.setSummary(getString(R.string.touch_to_display));
            passphrase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    passphrase.setSummary(mnemonic);
                    return false;
                }
            });
        }

        // -- handle pin
        final Preference resetPin = find("reset_pin");
        if (mnemonic != null) {
            resetPin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final Intent savePin = PinSaveActivity.createIntent(getActivity(), mnemonic);
                    startActivityForResult(savePin, PINSAVE);
                    return false;
                }
            });
        } else {
            getPreferenceScreen().removePreference(resetPin);
        }

        // -- handle currency and bitcoin denomination
        final ListPreference fiatCurrency = find("fiat_key");
        final ListPreference bitcoinDenomination = find("denomination_key");
        final ArrayList<String> units;
        units = Lists.newArrayList("BTC", "mBTC", mMicroSymbol + "BTC", "bits");

        bitcoinDenomination.setEntries(units.toArray(new String[4]));
        bitcoinDenomination.setEntryValues(units.toArray(new String[4]));
        bitcoinDenomination.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object o) {
                mService.setUserConfig("unit", o.toString(), true);
                bitcoinDenomination.setSummary(o.toString());
                return true;
            }
        });
        final String btcUnit = (String) mService.getUserConfig("unit");
        if (btcUnit == null || btcUnit.equals("bits")) {
            bitcoinDenomination.setSummary("bits");
        } else {
            bitcoinDenomination.setSummary(btcUnit);
        }

        fiatCurrency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object o) {
                final String[] split = o.toString().split(" ");
                mService.setPricingSource(split[0], split[1]);
                fiatCurrency.setSummary(o.toString());
                return true;
            }
        });
        final Preference watchOnlyLogin = find("watch_only_login");
        try {
            final String username = mService.getWatchOnlyUsername();
            if (username != null) {
                watchOnlyLogin.setSummary(getString(R.string.watchOnlyLoginStatus, username));
            } else {
                watchOnlyLogin.setSummary(R.string.watchOnlyLoginSetup);
            }
        } catch (final Exception e ) {
            e.printStackTrace();
        }

        watchOnlyLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_watchonly, null, false);
                final EditText inputUser = UI.find(v, R.id.input_user);
                try {
                    // refetch username
                    inputUser.setText(mService.getWatchOnlyUsername());
                } catch (final Exception e) {}
                final EditText inputPassword = UI.find(v, R.id.input_password);
                final MaterialDialog dialog = UI.popup(getActivity(), R.string.watchOnlyLogin)
                        .customView(v, true)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                final String username = UI.getText(inputUser);
                                final String password = UI.getText(inputPassword);
                                if (username.isEmpty() && password.isEmpty()) {
                                    try {
                                        mService.disableWatchOnly();
                                        UI.toast(getActivity(), R.string.watchOnlyLoginDisabled, Toast.LENGTH_LONG);
                                        watchOnlyLogin.setSummary(R.string.watchOnlyLoginSetup);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return;
                                }
                                try {
                                    mService.registerWatchOnly(username, password);
                                    watchOnlyLogin.setSummary(getString(R.string.watchOnlyLoginStatus, username));

                                } catch (final Exception e) {
                                    UI.toast(getActivity(), R.string.error_username_not_available, Toast.LENGTH_LONG);
                                }
                            }
                        }).build();
                UI.showDialog(dialog);
                return false;
            }
        });

        Futures.addCallback(mService.getCurrencyExchangePairs(), new CB.Op<List<List<String>>>() {
            @Override
            public void onSuccess(final List<List<String>> result) {
                final Activity activity = getActivity();
                if (activity != null && result != null) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            final ArrayList<String> fiatPairs = new ArrayList<>(result.size());

                            for (final List<String> currency_exchange : result) {
                                final boolean current = currency_exchange.get(0).equals(mService.getFiatCurrency())
                                        && currency_exchange.get(1).equals(mService.getFiatExchange());
                                final String pair = String.format("%s %s", currency_exchange.get(0), currency_exchange.get(1));
                                if (current) {
                                    fiatCurrency.setSummary(pair);
                                }
                                fiatPairs.add(pair);
                            }
                            fiatCurrency.setEntries(fiatPairs.toArray(new String[result.size()]));
                            fiatCurrency.setEntryValues(fiatPairs.toArray(new String[result.size()]));
                        }
                    });
                }
            }
        });

        // -- handle opt-in rbf
        final CheckBoxPreference optInRbf = find("optin_rbf");
        if (!mService.getLoginData().rbf) {
            getPreferenceScreen().removePreference(optInRbf);
        } else {
            optInRbf.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    // disable until server confirms set
                    optInRbf.setEnabled(false);

                    Futures.addCallback(
                            mService.setUserConfig("replace_by_fee", newValue, false),
                            new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(final Boolean result) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            optInRbf.setChecked((Boolean) newValue);
                                            optInRbf.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            optInRbf.setEnabled(true);
                                        }
                                    });
                                }
                            });
                    return false;
                }
            });
            final Boolean replace_by_fee = (Boolean) mService.getUserConfig("replace_by_fee");
            optInRbf.setChecked(replace_by_fee);
        }
        getActivity().setResult(Activity.RESULT_OK, null);
    }
}
