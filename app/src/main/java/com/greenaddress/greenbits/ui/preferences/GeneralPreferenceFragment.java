package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.Bridge;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.NotificationsData;
import com.greenaddress.greenapi.data.PricingData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.SweepSelectActivity;
import com.greenaddress.greenbits.ui.components.AmountTextWatcher;
import com.greenaddress.greenbits.ui.onboarding.SecurityActivity;
import com.greenaddress.greenbits.ui.twofactor.PopupMethodResolver;
import com.greenaddress.greenbits.ui.twofactor.TwoFactorActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

@Deprecated
public class GeneralPreferenceFragment extends GAPreferenceFragment {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    public static final int REQUEST_ENABLE_2FA = 2031;
    private static final int REQUEST_2FA = 101;
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private Preference mPinPref;
    private Preference mWatchOnlyLogin;
    private ListPreference mUnitPref;
    private ListPreference mPriceSourcePref;
    private ListPreference mTxPriorityPref;
    private Preference mCustomRatePref;
    private Preference mTwoFactorPref;
    private Preference mLimitsPref;
    private SwitchPreference mLocktimePref;
    private Preference mSetEmail;
    private Preference mSendLocktimePref;
    private Preference mTwoFactorRequestResetPref;
    private Preference mMemonicPref;
    private Preference mSweepPref;
    private ListPreference mTimeoutPref;
    private PreferenceCategory mAccountTitle;
    private Preference mSPV;
    private NetworkData mNetworkData;
    private Disposable mUpdateDisposable;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);

        if (getSession() == null || getSession().getSettings() == null) {
            logout();
            return;
        }

        mNetworkData = getNetwork();
        final SettingsData settings = getSession().getSettings();
        final boolean isLiquid = mNetworkData.getLiquid();
        final boolean isElectrum = mNetworkData.isElectrum();
        TwoFactorConfigData twoFaData = null;
        try {
            twoFaData = getSession().getTwoFactorConfig();
        } catch (final Exception e) { }
        final boolean anyEnabled = twoFaData != null && twoFaData.isAnyEnabled();
        final boolean emailConfirmed = twoFaData != null && twoFaData.getEmail() != null && twoFaData.getEmail().isConfirmed();

        // Pin submenu
        mPinPref = find(PrefKeys.DELETE_OR_CONFIGURE_PIN);
        mPinPref.setVisible(getSession().getHWWallet() == null);
        mPinPref.setOnPreferenceClickListener(preference -> {
            Bridge.INSTANCE.navigateToChangePin(requireActivity());
            return false;
        });

        Boolean isRecoveryConfirmed = Bridge.INSTANCE.getIsRecoveryConfirmed();

        // Watch-Only Login
        mWatchOnlyLogin = find(PrefKeys.WATCH_ONLY_LOGIN);
        mWatchOnlyLogin.setVisible(!isLiquid && isRecoveryConfirmed && !isElectrum);
        mWatchOnlyLogin.setOnPreferenceClickListener((preference) -> onWatchOnlyLoginClicked());
        setupWatchOnlySummary();

        mAccountTitle = find("account_title");
        mAccountTitle.setVisible(!isLiquid);

        // Network & Logout
        final Preference logout = find(PrefKeys.LOGOUT);
        logout.setTitle(getString(R.string.id_s_network, mNetworkData.getName()));
        logout.setSummary(UI.getColoredString(
                              getString(R.string.id_log_out), ContextCompat.getColor(getContext(), R.color.red)));
        logout.setOnPreferenceClickListener(preference -> {
            logout.setEnabled(false);
            logout();
            return false;
        });

        // Bitcoin denomination
        mUnitPref = find(PrefKeys.UNIT);
        mUnitPref.setEntries(mNetworkData.getLiquid() ? UI.LIQUID_UNITS : UI.UNITS);
        mUnitPref.setEntryValues(UI.UNITS);
        mUnitPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!newValue.equals(settings.getUnit())) {
                settings.setUnit(newValue.toString());
                mUpdateDisposable = updateSettings(settings).subscribe( (s) -> {
                    setUnitSummary(newValue.toString());
                }, (e) -> {
                    UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
                });
                return true;
            }
            return false;
        });
        try {
            setUnitSummary(Conversion.getBitcoinOrLiquidUnit(getSession()));
        } catch (final Exception e) { }

        // Reference exchange rate
        mPriceSourcePref = find(PrefKeys.PRICING);
        mPriceSourcePref.setSingleLineTitle(false);
        mPriceSourcePref.setVisible(!isLiquid);
        mPriceSourcePref.setOnPreferenceChangeListener((preference, o) -> onPriceSourceChanged(o));
        setPricingSummary(settings.getPricing());
        try {
            final Map<String, Object> availableCurrencies = getSession().getAvailableCurrencies();
            setPricingEntries(availableCurrencies);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Transaction priority, i.e. default fees
        mTxPriorityPref = find(PrefKeys.REQUIRED_NUM_BLOCKS);
        mTxPriorityPref.setSingleLineTitle(false);
        mTxPriorityPref.setVisible(!isLiquid);
        setRequiredNumBlocksSummary(settings.getRequiredNumBlocks());
        final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
        mTxPriorityPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (warnIfOffline(getActivity())) {
                return false;
            }
            final int index = mTxPriorityPref.findIndexOfValue(newValue.toString());
            settings.setRequiredNumBlocks(Integer.parseInt(priorityValues[index]));
            setRequiredNumBlocksSummary(null);
            mUpdateDisposable = updateSettings(settings).subscribe((s) -> {
                setRequiredNumBlocksSummary(Integer.parseInt(priorityValues[index]));
            }, (e) -> {
                UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
            });
            return true;
        });

        // Default custom feerate
        mCustomRatePref = find(PrefKeys.DEFAULT_FEERATE_SATBYTE);
        mCustomRatePref.setVisible(!isLiquid);
        mCustomRatePref.setOnPreferenceClickListener(this::onFeeRatePreferenceClicked);
        setFeeRateSummary();

        findPreference("category_two_factor").setVisible(!isElectrum);

        // Two-factor Authentication Submenu
        mTwoFactorPref = find(PrefKeys.TWO_FACTOR);
        mTwoFactorPref.setVisible(!isElectrum);
        mTwoFactorPref.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), SecurityActivity.class);
            startActivity(intent);
            return false;
        });

        // Two-Factor expiration period
        final Preference twoFactorCsv = find(PrefKeys.TWO_FACTOR_CSV);
        twoFactorCsv.setVisible(!isLiquid && !isElectrum);
        twoFactorCsv.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), CSVTimeActivity.class);
            startActivity(intent);
            return false;
        });

        // Set two-factor threshold
        mLimitsPref = find(PrefKeys.TWO_FAC_LIMITS);
        mLimitsPref.setOnPreferenceClickListener(this::onLimitsPreferenceClicked);
        mLimitsPref.setVisible(anyEnabled && !isLiquid && !isElectrum);
        if (twoFaData != null)
            setLimitsText(twoFaData.getLimits());

        // Enable nlocktime recovery emails
        mLocktimePref = find(PrefKeys.TWO_FAC_N_LOCKTIME_EMAILS);
        mLocktimePref.setVisible(emailConfirmed && !isLiquid && !isElectrum);
        mLocktimePref.setOnPreferenceChangeListener((preference, o) -> {
            if (warnIfOffline(getActivity())) {
                return false;
            }
            final boolean value = (Boolean) o;
            if (settings.getNotifications() == null)
                settings.setNotifications(new NotificationsData());
            settings.getNotifications().setEmailOutgoing(value);
            settings.getNotifications().setEmailIncoming(value);
            mUpdateDisposable = updateSettings(settings).subscribe((s) -> { }, (e) -> {
                UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
            });
            return true;
        });

        // Set nlocktime email
        mSetEmail = find(PrefKeys.SET_EMAIL);
        mSetEmail.setVisible(!emailConfirmed && !isLiquid && !isElectrum);
        mSetEmail.setOnPreferenceClickListener((preference) -> onSetEmailClicked());

        // Send nlocktime recovery emails
        mSendLocktimePref = find(PrefKeys.SEND_NLOCKTIME);
        mSendLocktimePref.setVisible(emailConfirmed && !isLiquid && !isElectrum);
        mSendLocktimePref.setOnPreferenceClickListener(this::onSendNLocktimeClicked);

        // Cancel two factor reset
        mTwoFactorRequestResetPref = find(PrefKeys.RESET_TWOFACTOR);
        mTwoFactorRequestResetPref.setOnPreferenceClickListener(preference -> prompt2FAChange("reset", true));
        mTwoFactorRequestResetPref.setVisible(anyEnabled && !isLiquid && !isElectrum);

        // Mnemonic
        mMemonicPref = find(PrefKeys.MNEMONIC_PASSPHRASE);
        mMemonicPref.setVisible(getSession().getHWWallet() == null);
        final String touchToDisplay = getString(R.string.id_touch_to_display);
        mMemonicPref.setSummary(touchToDisplay);
        mMemonicPref.setOnPreferenceClickListener(preference -> {
            Bridge.INSTANCE.navigateToBackupRecovery(getActivity());
            return false;
        });

        // Auto logout timeout
        mTimeoutPref = find(PrefKeys.ALTIMEOUT);
        mTimeoutPref.setEntryValues(getResources().getStringArray(R.array.auto_logout_values));
        setTimeoutValues(mTimeoutPref);
        try {
            final int timeout = settings.getAltimeout();
            setTimeoutSummary(timeout);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        mTimeoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
            final Integer altimeout = Integer.parseInt(newValue.toString());
            settings.setAltimeout(altimeout);
            setTimeoutSummary(null);
            mUpdateDisposable = updateSettings(settings).subscribe((s) -> {
                setTimeoutSummary(altimeout);
            }, (e) -> {
                UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
            });
            return true;
        });

        findPreference("network_monitor").setVisible(false);
        findPreference("category_advanced").setVisible(!isElectrum);

        // SPV_SYNCRONIZATION Syncronization Submenu
        mSPV = findPreference(PrefKeys.SPV_SYNCRONIZATION);
        mSPV.setVisible(!isLiquid && !isElectrum);
        mSPV.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SPVPreferenceFragment.class.getName() );
            startActivity(intent);
            return false;
        });

        // sweep from paper wallet
        mSweepPref = find(PrefKeys.SWEEP);
        mSweepPref.setVisible(!isLiquid && !isElectrum);
        mSweepPref.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), SweepSelectActivity.class);
            startActivity(intent);
            return false;
        });

        Preference gpgKeyPref = findPreference(PrefKeys.PGP_KEY);
        gpgKeyPref.setVisible(!isElectrum);
        gpgKeyPref.setOnPreferenceClickListener(this::onPGPKeyClicked);


        // Terms of service
        final Preference termsOfUse = find(PrefKeys.TERMS_OF_USE);
        termsOfUse.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/terms/"));

        // Privacy policy
        final Preference privacyPolicy = find(PrefKeys.PRIVACY_POLICY);
        privacyPolicy.setOnPreferenceClickListener(preference -> openURI("https://blockstream.com/green/privacy/"));

        // Version
        final Preference version = find(PrefKeys.VERSION);
        version.setSummary(String.format("%s %s",
                                         getString(R.string.app_name),
                                         getString(R.string.id_version_1s_2s,
                                                   Bridge.INSTANCE.getVersionName(),
                                                   BuildConfig.BUILD_TYPE)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUpdateDisposable != null)
            mUpdateDisposable.dispose();
    }

    private void setupWatchOnlySummary() {
        mWatchOnlyLogin.setSummary("");
        mUpdateDisposable = Observable.just(getSession())
                            .observeOn(Schedulers.computation())
                            .map((session) -> {
            return session.getWatchOnlyUsername();
        })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((username) -> {
            if (username.isEmpty())
                mWatchOnlyLogin.setSummary(R.string.id_set_up_watchonly_credentials);
            else
                mWatchOnlyLogin.setSummary(getString(R.string.id_enabled_1s, username));
        }, (e) -> {
            e.printStackTrace();
            UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
        });
    }

    private Observable<SettingsData> updateSettings(final SettingsData settings) {
        final Activity activity = getActivity();
        return Observable.just(getSession())
               .observeOn(Schedulers.computation())
               .map((session) -> {
            session.changeSettings(settings.toObjectNode()).resolve(new PopupMethodResolver(activity),null,
                                                                    Bridge.INSTANCE.createTwoFactorResolver(activity));
            return session.refreshSettings();
        }).observeOn(AndroidSchedulers.mainThread())
               .map((settingsData) -> {
            final ObjectNode details = mObjectMapper.createObjectNode();
            details.put("event", "settings");
            details.set("settings", mObjectMapper.valueToTree(settingsData));

            getSession().getNotificationModel().onNewNotification(getSession(), details);
            Bridge.INSTANCE.updateSettingsV4();

            return settingsData;
        }).map((settingsData) -> {
            UI.toast(getActivity(), R.string.id_setting_updated, Toast.LENGTH_LONG);
            return settingsData;
        });
    }

    private String getDefaultFeeRate() {
        Long minFeeRateKB = 1000L;
        try {
            minFeeRateKB = getSession().getFees().get(0);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final String minFeeRateText = String.valueOf(minFeeRateKB / 1000.0);
        return cfg().getString( PrefKeys.DEFAULT_FEERATE_SATBYTE, minFeeRateText);
    }

    private boolean onPriceSourceChanged(final Object o) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        final String[] split = o.toString().split(" ");
        final String currency = split[0];
        final String exchange = split[1];
        final SettingsData settings = getSession().getSettings();

        settings.getPricing().setCurrency(currency);
        settings.getPricing().setExchange(exchange);
        setPricingSummary(settings.getPricing());

        if(!mNetworkData.isElectrum()) {
            mUpdateDisposable = updateSettings(settings)
                    .observeOn(Schedulers.computation())
                    .map((s) -> {
                        final TwoFactorConfigData twoFaData = getSession().getTwoFactorConfig();
                        final ObjectNode limitsData = twoFaData.getLimits();
                        return limitsData;
                    }).observeOn(AndroidSchedulers.mainThread())
                    .subscribe((limitsData) -> {

                        setLimitsText(limitsData);
                        final Integer satoshi = limitsData.get("satoshi").asInt(0);
                        if (satoshi > 0) {
                            UI.popup(
                                    getActivity(),
                                    "Changing reference exchange rate will reset your 2FA threshold to 0. Remember to top-up the 2FA threshold after continuing.")
                                    .show();
                        }
                    }, (final Throwable e) -> {
                        UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
                    });
        }
        return true;
    }

    private boolean onWatchOnlyLoginClicked() {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_watchonly);
        final EditText inputUser = UI.find(v, R.id.input_user);
        try {
            // refetch username
            inputUser.setText(getSession().getWatchOnlyUsername());
        } catch (final Exception e) {}
        final EditText inputPassword = UI.find(v, R.id.input_password);
        final MaterialDialog dialog = UI.popup(getActivity(), R.string.id_watchonly_login)
                                      .customView(v, true)
                                      .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                                      .onPositive((dlg, which) -> {
            final String username = UI.getText(inputUser);
            final String password = UI.getText(inputPassword);
            if (username.isEmpty() || password.isEmpty()) {
                UI.toast(getActivity(), R.string.id_the_password_cant_be_empty, Toast.LENGTH_LONG);
                return;
            }
            mUpdateDisposable = Observable.just(getSession())
                                .observeOn(Schedulers.computation())
                                .map((session) -> {
                session.setWatchOnly(username, password);
                return session;
            }).observeOn(AndroidSchedulers.mainThread())
                                .subscribe((session) -> {
                setupWatchOnlySummary();
            }, (e) -> {
                UI.toast(getActivity(), R.string.id_username_not_available, Toast.LENGTH_LONG);
            });
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onPGPKeyClicked(final Preference pgpKey) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_pgp_key);
        final EditText inputPGPKey = UI.find(v, R.id.input_pgp_key);
        final SettingsData settings = getSession().getSettings();
        final String oldValue = settings.getPgp() == null ? "" : settings.getPgp();
        try {
            inputPGPKey.setText(oldValue);
        } catch (final Exception e) {}

        final MaterialDialog dialog = UI.popup(getActivity(), R.string.id_pgp_key)
                                      .customView(v, true)
                                      .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                                      .onPositive((dlg, which) -> {
            final String newValue = UI.getText(inputPGPKey);
            if (!newValue.equals(oldValue)) {
                settings.setPgp(newValue);
                mUpdateDisposable = updateSettings(settings).subscribe((s) -> {
                    inputPGPKey.setText(settings.getPgp());
                }, (e) -> {
                    UI.toast(getActivity(), R.string.id_invalid_pgp_key, Toast.LENGTH_LONG);
                });
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onFeeRatePreferenceClicked(final Preference preference) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_custom_feerate);
        final EditText rateEdit = UI.find(v, R.id.set_custom_feerate_amount);
        final AmountTextWatcher amountTextWatcher = new AmountTextWatcher(rateEdit);
        final Double aDouble = Double.valueOf(getDefaultFeeRate());
        rateEdit.setHint(String.format("0%s00", amountTextWatcher.getDefaultSeparator()));
        rateEdit.setText(Conversion.getNumberFormat(2).format(aDouble));
        rateEdit.selectAll();
        rateEdit.addTextChangedListener(amountTextWatcher);

        final MaterialDialog dialog;
        dialog = UI.popup(getActivity(), R.string.id_set_custom_fee_rate)
                 .customView(v, true)
                 .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                 .onPositive((dlg, which) -> {
            try {
                final Long minFeeRateKB = getSession().getFees().get(0);
                final String enteredFeeRate = UI.getText(rateEdit);
                final Number parsed = Conversion.getNumberFormat(2).parse(enteredFeeRate);
                final Double enteredFeeRateKB = parsed.doubleValue();

                if (enteredFeeRateKB * 1000 < minFeeRateKB) {
                    UI.toast(getActivity(), getString(R.string.id_fee_rate_must_be_at_least_s,
                                                      String.format("%.2f",(minFeeRateKB/1000.0) )), Toast.LENGTH_LONG);
                } else {
                    cfg().edit().putString(PrefKeys.DEFAULT_FEERATE_SATBYTE, String.valueOf(enteredFeeRateKB)).apply();
                    setFeeRateSummary();
                }
            } catch (final Exception e) {
                UI.toast(getActivity(), "Error setting Fee Rate", Toast.LENGTH_LONG);
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private void setRequiredNumBlocksSummary(final Integer currentPriority) {
        if (currentPriority == null)
            mTxPriorityPref.setSummary("");
        else {
            final String[] prioritySummaries = {prioritySummary(3), prioritySummary(12), prioritySummary(24)};
            final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
            for (int index = 0; index < priorityValues.length; index++)
                if (currentPriority.equals(Integer.valueOf(priorityValues[index])))
                    mTxPriorityPref.setSummary(prioritySummaries[index]);
        }
    }

    private String prioritySummary(final int blocks) {
        final int blocksPerHour = mNetworkData.getLiquid() ? 60 : 6;
        final int n = blocks % blocksPerHour == 0 ? blocks / blocksPerHour : blocks * (60 / blocksPerHour);
        final String confirmationInBlocks = getResources().getString(R.string.id_confirmation_in_d_blocks, blocks);
        final int idTime = blocks % blocksPerHour ==
                           0 ? (blocks == blocksPerHour ? R.string.id_hour : R.string.id_hours) : R.string.id_minutes;
        return String.format("%s, %d %s %s", confirmationInBlocks, n, getResources().getString(idTime),
                             getResources().getString(R.string.id_on_average));
    }

    private void setTimeoutSummary(final Integer altimeout) {
        if (altimeout == null)
            mTimeoutPref.setSummary("");
        else {
            final String minutesText = altimeout == 1 ?
                                       "1 " + getString(R.string.id_minute) :
                                       getString(R.string.id_1d_minutes, altimeout);
            mTimeoutPref.setSummary(minutesText);
        }
    }

    private void setTimeoutValues(final ListPreference preference) {
        final CharSequence[] entries = preference.getEntryValues();
        final int length = entries.length;
        final String[] entryValues = new String[length];
        for (int i = 0; i < length; i++) {
            final int currentMinutes = Integer.valueOf(entries[i].toString());
            final String minutesText = currentMinutes == 1 ?
                                       "1 " + getString(R.string.id_minute) :
                                       getString(R.string.id_1d_minutes, currentMinutes);
            entryValues[i] = minutesText;
        }
        preference.setEntries(entryValues);
    }

    private void setPricingEntries(final Map<String, Object> currencies) {
        final List<String> values = getAvailableCurrenciesAsList(currencies);
        final List<String> formatted =
            getAvailableCurrenciesAsFormattedList(currencies, getString(R.string.id_s_from_s));
        final String[] valuesArr = values.toArray(new String[0]);
        final String[] formattedArr = formatted.toArray(new String[0]);
        mPriceSourcePref.setEntries(formattedArr);
        mPriceSourcePref.setEntryValues(valuesArr);
    }

    public List<String> getAvailableCurrenciesAsFormattedList(final Map<String, Object> currencies,
                                                              final String format) {
        final List<String> list = new ArrayList<>();
        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs(currencies)) {
            list.add(String.format(format, pair.first, pair.second));
        }
        return list;
    }

    public List<String> getAvailableCurrenciesAsList(final Map<String, Object> currencies) {
        if (getAvailableCurrenciesAsPairs(currencies) == null)
            return null;
        final List<String> list = new ArrayList<>();
        for (Pair<String,String> pair : getAvailableCurrenciesAsPairs(currencies)) {
            list.add(String.format("%s %s", pair.first, pair.second));
        }
        return list;
    }

    private List<Pair<String, String>> getAvailableCurrenciesAsPairs(final Map<String, Object> currencies) {
        final List<Pair<String, String>> ret = new LinkedList<>();
        final Map<String, ArrayList<String>> perExchange = (Map) currencies.get("per_exchange");

        for (final String exchange : perExchange.keySet())
            for (final String currency : perExchange.get(exchange))
                ret.add(new Pair<>(currency, exchange));

        Collections.sort(ret, (lhs, rhs) -> lhs.first.compareTo(rhs.first));
        return ret;
    }

    private void setPricingSummary(final PricingData pricing) {
        final String summary = pricing == null ? "" : String.format(getString(
                                                                        R.string.id_s_from_s),
                                                                    pricing.getCurrency(), pricing.getExchange());
        mPriceSourcePref.setSummary(summary);
    }

    private void setUnitSummary(final String value) {
        final String summary = value == null ? "" : value;
        mUnitPref.setSummary(summary);
    }

    private void setFeeRateSummary() {
        final Double aDouble = Double.valueOf(getDefaultFeeRate());
        final String feeRateString = UI.getFeeRateString(Double.valueOf(aDouble * 1000).longValue());
        mCustomRatePref.setSummary(feeRateString);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isZombie())
            return;
        if (getSession() == null || getSession().getSettings() == null) {
            logout();
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isZombie())
            return;
    }

    private boolean prompt2FAChange(final String method, final Boolean newValue) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        // TODO spending limits in two TwoFactorConfigData
        final Intent intent = new Intent(getActivity(), TwoFactorActivity.class);
        intent.putExtra("method", method);
        intent.putExtra("enable", newValue);
        startActivityForResult(intent, REQUEST_ENABLE_2FA);
        return true;
    }

    private void setLimitsText(final ObjectNode limitsData) {
        getActivity().runOnUiThread(() -> {
            try {
                final boolean isFiat = limitsData.get("is_fiat").asBoolean();
                final BalanceData balance = mObjectMapper.treeToValue(limitsData, BalanceData.class);
                if (!isFiat && balance.getSatoshi() == 0) {
                    mLimitsPref.setSummary(R.string.id_set_twofactor_threshold);
                } else if (isFiat) {
                    mLimitsPref.setSummary(Conversion.getFiat(getSession(), balance, true));
                } else {
                    mLimitsPref.setSummary(Conversion.getBtc(getSession(), balance, true));
                }
            } catch (final Exception e) {
                // We can throw because we have been logged out here, e.g. when
                // requesting a two-factor reset and unwinding the activity stack.
                // Since this is harmless, ignore the error here.
            }
        });
    }

    private boolean onLimitsPreferenceClicked(final Preference preference) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_limits);
        final Spinner unitSpinner = UI.find(v, R.id.set_limits_currency);
        final EditText amountEdit = UI.find(v, R.id.set_limits_amount);

        final AmountTextWatcher amountTextWatcher = new AmountTextWatcher(amountEdit);
        amountEdit.setHint(String.format("0%s00", amountTextWatcher.getDefaultSeparator()));
        amountEdit.addTextChangedListener(amountTextWatcher);

        final String[] currencies;
        try {
            currencies = new String[]{Conversion.getBitcoinOrLiquidUnit(getSession()), Conversion.getFiatCurrency(getSession())};
            final ArrayAdapter<String> adapter;
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, currencies);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            unitSpinner.setAdapter(adapter);
        } catch (final Exception e) {
            UI.toast(getActivity(), getString(R.string.id_operation_failure), Toast.LENGTH_SHORT);
            return false;
        }

        try {
            final ObjectNode limitsData = getSession().getTwoFactorConfig().getLimits();
            final boolean isFiat = limitsData.get("is_fiat").asBoolean();
            unitSpinner.setSelection(isFiat ? 1 : 0);
            amountEdit.selectAll();
            final BalanceData balance;
            balance = mObjectMapper.treeToValue(limitsData, BalanceData.class);
            amountEdit.removeTextChangedListener(amountTextWatcher);
            amountEdit.setText(isFiat ? Conversion.getFiat(getSession(), balance, false) :
                        Conversion.getBtc(getSession(), balance, false));
            amountEdit.addTextChangedListener(amountTextWatcher);
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }

        final MaterialDialog dialog;
        dialog = UI.popup(getActivity(), R.string.id_set_twofactor_threshold)
                 .cancelable(false)
                 .customView(v, true)
                 .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                 .onPositive((dlg, which) -> {
            try {
                final String unit = unitSpinner.getSelectedItem().toString();
                final String value = UI.getText(amountEdit);
                final Double doubleValue = Conversion.getNumberFormat(getSession()).parse(value).doubleValue();
                setSpendingLimits(unit, doubleValue.toString());
            } catch (final Exception e) {
                UI.toast(getActivity(), "Error setting limits", Toast.LENGTH_LONG);
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onSendNLocktimeClicked(final Preference preference) {
        if (warnIfOffline(getActivity())) {
            return false;
        }
        mUpdateDisposable = Observable.just(getSession())
                            .observeOn(Schedulers.computation())
                            .map((session) -> {
            session.sendNlocktimes();
            return session;
        })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((username) -> {
            UI.toast(getActivity(), R.string.id_recovery_transaction_request, Toast.LENGTH_SHORT);
        }, (e) -> {
            e.printStackTrace();
            UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
        });
        return false;
    }

    private boolean onSetEmailClicked() {
        final Intent intent = new Intent(getActivity(), TwoFactorActivity.class);
        intent.putExtra("method", "email");
        intent.putExtra("settingEmail", true);
        startActivityForResult(intent, REQUEST_2FA);
        return false;
    }

    private void setSpendingLimits(final String unit, final String amount) {
        final Activity activity = getActivity();
        final ObjectNode limitsData = new ObjectMapper().createObjectNode();
        try {
            final boolean isFiat = unit.equals(Conversion.getFiatCurrency(getSession()));
            final String amountStr = TextUtils.isEmpty(amount) ? "0" : amount;
            limitsData.set("is_fiat", isFiat ? BooleanNode.TRUE : BooleanNode.FALSE);
            limitsData.set(isFiat ? "fiat" : Conversion.getUnitKey(getSession()), new TextNode(amountStr));
        } catch (final Exception e) {
            UI.toast(activity, getString(R.string.id_operation_failure), Toast.LENGTH_SHORT);
            return;
        }

        mUpdateDisposable = Observable.just(getSession())
                            .observeOn(Schedulers.computation())
                            .map((session) -> {
            final GDKTwoFactorCall call = getSession().twoFactorChangeLimits(limitsData);
            final ObjectNode newLimits =
                call.resolve(new PopupMethodResolver(activity), null, Bridge.INSTANCE.createTwoFactorResolver(getActivity()));
            getSession().getTwoFactorConfig().setLimits(newLimits);
            return newLimits;
        })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((newLimits) -> {
            setLimitsText(newLimits);
            UI.toast(getActivity(), R.string.id_setting_updated, Toast.LENGTH_LONG);
        }, (e) -> {
            e.printStackTrace();
            UI.toast(getActivity(), R.string.id_operation_failure, Toast.LENGTH_LONG);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GeneralPreferenceFragment.REQUEST_ENABLE_2FA && resultCode == RESULT_OK
            && data != null && "reset".equals(data.getStringExtra("method"))) {
            logout();
        }
    }
}

