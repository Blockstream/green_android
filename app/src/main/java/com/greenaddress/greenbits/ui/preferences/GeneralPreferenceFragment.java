package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.Html;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.ui.PinSaveActivity;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.List;

public class GeneralPreferenceFragment extends GAPreferenceFragment {
    private static final int PINSAVE = 1337;
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
        final Preference altime = findPreference("altime");
        altime.setSummary(String.format("%d %s", timeout, getResources().getString(R.string.autologout_time_default)));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                try {
                    final Integer altimeout = Integer.parseInt(newValue.toString());
                    mService.setUserConfig("altimeout", altimeout, true);
                    preference.setSummary(String.format("%d %s", altimeout, getResources().getString(R.string.autologout_time_default)));
                    return true;
                } catch (@NonNull final Exception e) {
                    // not set
                }
                return false;
            }
        });

        // -- handle mnemonics

        final String mnemonic = mService.getMnemonics();
        if (mnemonic != null) {
            findPreference("mnemonic_passphrase").setSummary(getString(R.string.touch_to_display));
            findPreference("mnemonic_passphrase").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    getPreferenceManager().findPreference("mnemonic_passphrase").setSummary(mnemonic);
                    return false;
                }
            });
        }

        // -- handle pin
        if (mnemonic != null) {
            findPreference("reset_pin").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent pinSaveActivity = new Intent(getActivity(), PinSaveActivity.class);
                    pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", mnemonic);
                    startActivityForResult(pinSaveActivity, PINSAVE);
                    return false;
                }
            });
        } else {
            getPreferenceScreen().removePreference(findPreference("reset_pin"));
        }

        // -- handle currency and bitcoin denomination
        final ListPreference fiatCurrency = (ListPreference) findPreference("fiat_key");
        final ListPreference bitcoinDenomination = (ListPreference)  findPreference("denomination_key");
        final ArrayList<String> units = new ArrayList<>();

        units.add("BTC");
        units.add("mBTC");
        units.add(Html.fromHtml("&micro;").toString() + "BTC");
        units.add("bits");

        bitcoinDenomination.setEntries(units.toArray(new String[4]));
        bitcoinDenomination.setEntryValues(units.toArray(new String[4]));
        bitcoinDenomination.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
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

        Futures.addCallback(
                mService.getCurrencyExchangePairs(),
                new FutureCallback<List<List<String>>>() {
                    @Override
                    public void onSuccess(final List<List<String>> result) {
                        final Activity activity = getActivity();
                        if (activity != null && result != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
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

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        t.printStackTrace();
                    }
                });

        // -- handle opt-in rbf
        if (!mService.getLoginData().rbf) {
            getPreferenceScreen().removePreference(findPreference("optin_rbf"));
        } else {
            final CheckBoxPreference optin_rbf = (CheckBoxPreference) findPreference("optin_rbf");
            optin_rbf.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    // disable until server confirms set
                    optin_rbf.setEnabled(false);

                    Futures.addCallback(
                            mService.setUserConfig("replace_by_fee", newValue, false),
                            new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(final Boolean result) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            optin_rbf.setChecked((Boolean) newValue);
                                            optin_rbf.setEnabled(true);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            optin_rbf.setEnabled(true);
                                        }
                                    });
                                }
                            });
                    return false;
                }
            });
            final Boolean replace_by_fee = (Boolean) mService.getUserConfig("replace_by_fee");
            ((CheckBoxPreference) findPreference("optin_rbf")).setChecked(replace_by_fee);
        }
        getActivity().setResult(getActivity().RESULT_OK, null);
    }
}
