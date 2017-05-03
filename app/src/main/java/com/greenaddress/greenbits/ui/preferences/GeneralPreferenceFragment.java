package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.PinSaveActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeneralPreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceClickListener {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    private static final int PINSAVE = 1337;
    private Preference mToggleSW;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mService == null || !mService.isLoggedIn()) {
            // If we are restored and our service has not been destroyed, its
            // state is unreliable and our parent activity should shortly
            // be calling finish(). Avoid accessing the service in this case.
            Log.d(TAG, "Avoiding create on logged out service");
            return;
        }

        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);

        // Timeout
        final int timeout = mService.getAutoLogoutMinutes();
        getPreferenceManager().getSharedPreferences().edit()
                              .putString("altime", Integer.toString(timeout))
                              .apply();
        final Preference altime = find("altime");
        altime.setSummary(String.format(Locale.US, "%d %s", timeout, getResources().getString(R.string.autologout_time_default)));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                try {
                    final Integer altimeout = Integer.parseInt(newValue.toString());
                    mService.setUserConfig("altimeout", altimeout, true);
                    preference.setSummary(String.format(Locale.US, "%d %s", altimeout, getResources().getString(R.string.autologout_time_default)));
                    return true;
                } catch (final Exception e) {
                    // not set
                }
                return false;
            }
        });

        // Mnemonics
        final String mnemonic = mService.getMnemonic();
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

        // PIN
        final Preference resetPin = find("reset_pin");
        if (mnemonic == null)
            removePreference(resetPin);
        else {
            resetPin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    final Intent savePin = PinSaveActivity.createIntent(getActivity(), mnemonic);
                    startActivityForResult(savePin, PINSAVE);
                    return false;
                }
            });
        }

        // Currency and bitcoin denomination
        final ListPreference bitcoinDenomination = find("denomination_key");
        if (GaService.IS_ELEMENTS)
            removePreference(bitcoinDenomination);
        else {
            bitcoinDenomination.setEntries(UI.UNITS.toArray(new String[4]));
            bitcoinDenomination.setEntryValues(UI.UNITS.toArray(new String[4]));
            bitcoinDenomination.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object o) {
                    mService.setUserConfig("unit", o.toString(), true);
                    bitcoinDenomination.setSummary(o.toString());
                    return true;
                }
            });
            bitcoinDenomination.setSummary(mService.getBitcoinUnit());
        }

        final ListPreference fiatCurrency = find("fiat_key");
        if (GaService.IS_ELEMENTS)
            removePreference(fiatCurrency);
        else
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
                                    } catch (final Exception e) {
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

                            for (final List<String> pair : result) {
                                final boolean current = pair.get(0).equals(mService.getFiatCurrency()) &&
                                                        pair.get(1).equals(mService.getFiatExchange());
                                final String entry = String.format("%s %s", pair.get(0), pair.get(1));
                                if (current)
                                    fiatCurrency.setSummary(entry);
                                fiatPairs.add(entry);
                            }
                            fiatCurrency.setEntries(fiatPairs.toArray(new String[result.size()]));
                            fiatCurrency.setEntryValues(fiatPairs.toArray(new String[result.size()]));
                        }
                    });
                }
            }
        });

        // Opt-in RBF
        final CheckBoxPreference optInRbf = find("optin_rbf");
        if (GaService.IS_ELEMENTS || !(boolean) mService.getLoginData().get("rbf"))
            removePreference(optInRbf);
        else {
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

        mToggleSW = find("toggle_segwit");
        if (GaService.IS_ELEMENTS)
            removePreference(mToggleSW);
        else {
            mToggleSW.setOnPreferenceClickListener(this);
            setupSWToggle();
        }

        if (GaService.IS_ELEMENTS)
            removePreference("settings_currency");

        getActivity().setResult(Activity.RESULT_OK, null);
    }

    private void setupSWToggle() {
        final boolean segwit = mService.getLoginData().get("segwit_server");
        final boolean userSegwit = mService.isSegwitEnabled();

        mToggleSW.setTitle(userSegwit ? R.string.segwit_disable : R.string.segwit_enable);

        if (!segwit) {
            // Server does not support segwit: Do not show the pref
            getPreferenceScreen().removePreference(mToggleSW);
        } else if (!userSegwit || mService.isSegwitUnlocked()) {
            // User hasn't enabled segwit, or they have but we haven't
            // generated a segwit address yet (that we know of).
            mToggleSW.setEnabled(true);
        } else {
            mToggleSW.setEnabled(false);
        }
    }


    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preference == mToggleSW)
            return onToggleSWClicked();
        return false;
    }

    private boolean onToggleSWClicked() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() { mToggleSW.setEnabled(false); }
        });
        final boolean immediate = true;
        CB.after(mService.setUserConfig("use_segwit", !mService.isSegwitEnabled(), immediate),
                 new CB.Op<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                toggle();
            }
            @Override
            public void onFailure(final Throwable t) {
                super.onFailure(t);
                toggle();
            }
            private void toggle() {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() { setupSWToggle(); }
                });
            }

        });
        return false;
    }

}
