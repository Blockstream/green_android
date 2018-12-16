package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.PricingData;
import com.greenaddress.greenapi.data.NotificationsData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.model.AvailableCurrenciesObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SettingsObservable;
import com.greenaddress.greenapi.model.TwoFactorConfigDataObservable;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.PinSaveActivity;
import com.greenaddress.greenbits.ui.PopupCodeResolver;
import com.greenaddress.greenbits.ui.PopupMethodResolver;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.SendInputFragment;
import com.greenaddress.greenbits.ui.TwoFactorActivity;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.onboarding.SecurityActivity;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static android.app.Activity.RESULT_OK;

public class GeneralPreferenceFragment extends GAPreferenceFragment implements Observer {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    private static final int PINSAVE = 1337;
    public static final int REQUEST_ENABLE_2FA = 2031;

    private Preference mFeeRate;
    private ListPreference mPricing;
    private ListPreference mUnit;
    private ListPreference mRequiredNumBlocks;
    private ListPreference mAltimeout;
    private Preference mTwoFactorPref;
    private Preference mLimitsPref;
    private Preference mSendNLocktimePref;
    private Preference mTwoFactorRequestResetPref;

    private SwitchPreference mEnableNLocktimePref;
    private Preference mDeleteOrConfigurePin;
    private Preference mPassphrase;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_general);
        setHasOptionsMenu(true);
        attachObservers();

        // PIN
        mDeleteOrConfigurePin = find(PrefKeys.DELETE_OR_CONFIGURE_PIN);
        mDeleteOrConfigurePin.setOnPreferenceClickListener(preference -> {
            if (mService.hasPin()) {
                UI.popup(getActivity(), R.string.id_warning)
                .content(R.string.id_deleting_your_pin_will_remove)
                .onPositive((dlg, which) -> {
                    mService.cfgPin().edit().clear().commit();
                    initPinButtonTextAndSummary();
                }).show();
            } else {
                final Intent savePin = PinSaveActivity.createIntent(getActivity(), mService.getMnemonic());
                savePin.putExtra("skip_visible", false);
                startActivityForResult(savePin, PINSAVE);
            }
            return false;
        });

        // Watch-Only Login
        final Preference watchOnlyLogin = find(PrefKeys.WATCH_ONLY_LOGIN);
        try {
            final String username = mService.getWatchOnlyUsername();
            if (username != null) {
                watchOnlyLogin.setSummary(getString(R.string.id_enabled_1s, username));
            } else {
                watchOnlyLogin.setSummary(R.string.id_touch_to_set_up);
            }
        } catch (final Exception e ) {
            e.printStackTrace();
            watchOnlyLogin.setVisible(false);
        }
        watchOnlyLogin.setOnPreferenceClickListener(this::onWatchOnlyLoginClicked);

        // Network & Logout
        final PreferenceCategory cat = find(PrefKeys.NETWORK_CATEGORY);
        cat.setTitle(getString(R.string.id_s_network, mService.getNetwork().getName().toUpperCase()));
        final Preference logout = find(PrefKeys.LOGOUT);
        logout.setOnPreferenceClickListener(preference -> {
            logout.setEnabled(false);
            logout();
            return false;
        });

        // Bitcoin denomination
        mUnit = find(PrefKeys.UNIT);
        mUnit.setEntries(UI.UNITS.toArray(new String[4]));
        mUnit.setEntryValues(UI.UNITS.toArray(new String[4]));
        mUnit.setOnPreferenceChangeListener((preference, newValue) -> {
            final SettingsData settings = mService.getModel().getSettings();
            if (!newValue.equals(settings.getUnit())) {
                settings.setUnit(newValue.toString());
                setUnitSummary(null);
                mService.getExecutor().execute(() -> updateSettings(settings));
                return true;
            }
            return false;
        });

        // Reference exchange rate
        mPricing = find(PrefKeys.PRICING);
        mPricing.setOnPreferenceChangeListener((preference, o) -> {
            final String[] split = o.toString().split(" ");
            final String currency = split[0];
            final String exchange = split[1];
            final Model model = mService.getModel();
            final SettingsData settings = model.getSettings();

            settings.getPricing().setCurrency(currency);
            settings.getPricing().setExchange(exchange);
            setPricingSummary(null);
            mService.getExecutor().execute(() -> updateSettings(settings));

            final TwoFactorConfigDataObservable twoFaData = model.getTwoFactorConfigDataObservable();
            if (twoFaData.getTwoFactorConfigData() != null) {
                setLimitsText(twoFaData.getTwoFactorConfigData().getLimits());
            }
            return true;
        });

        // Transaction priority, i.e. default fees
        mRequiredNumBlocks = find(PrefKeys.REQUIRED_NUM_BLOCKS);
        final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
        mRequiredNumBlocks.setOnPreferenceChangeListener((preference, newValue) -> {
            final int index = mRequiredNumBlocks.findIndexOfValue(newValue.toString());
            final SettingsData settings = mService.getModel().getSettings();
            settings.setRequiredNumBlocks(Integer.parseInt(priorityValues[index]));
            setRequiredNumBlocksSummary(null);
            mService.getExecutor().execute(() -> updateSettings(settings));
            return true;
        });

        // Default custom feerate
        mFeeRate = find(PrefKeys.DEFAULT_FEERATE_SATBYTE);
        setFeeRateSummary();
        mFeeRate.setOnPreferenceClickListener(this::onFeeRatePreferenceClicked);

        // Two-factor Authentication Submenu
        mTwoFactorPref = find(PrefKeys.TWO_FACTOR);
        mTwoFactorPref.setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), SecurityActivity.class);
            startActivity(intent);
            return false;
        });

        // Set two-factor threshold
        mLimitsPref = find(PrefKeys.TWO_FAC_LIMITS);
        mLimitsPref.setOnPreferenceClickListener(this::onLimitsPreferenceClicked);

        // Enable nlocktime recovery emails
        mEnableNLocktimePref = find(PrefKeys.TWO_FAC_N_LOCKTIME_EMAILS);
        mEnableNLocktimePref.setOnPreferenceChangeListener((preference, o) -> {
            final boolean value = (Boolean) o;
            final SettingsData settings = mService.getModel().getSettings();
            if (settings.getNotifications() == null)
                settings.setNotifications(new NotificationsData());
            settings.getNotifications().setEmailOutgoing(value);
            settings.getNotifications().setEmailIncoming(value);
            mService.getExecutor().execute(() -> updateSettings(settings));
            return true;
        });

        // Send nlocktime recovery emails
        mSendNLocktimePref = find(PrefKeys.SEND_NLOCKTIME);
        mSendNLocktimePref.setOnPreferenceClickListener(this::onSendNLocktimeClicked);

        // Cancel two factor reset
        mTwoFactorRequestResetPref = find(PrefKeys.RESET_TWOFACTOR);
        mTwoFactorRequestResetPref.setOnPreferenceClickListener(preference -> prompt2FAChange("reset", true));

        // Mnemonic
        mPassphrase = find(PrefKeys.MNEMONIC_PASSPHRASE);
        if (!mService.getConnectionManager().isHW()) {
            final String mnemonic = mService.getMnemonic();
            if ( mnemonic != null) {
                mPassphrase.setSummary(getString(R.string.id_touch_to_display));
                mPassphrase.setOnPreferenceClickListener(preference -> {
                    if (mPassphrase.getSummary().equals(mnemonic))
                        mPassphrase.setSummary(getString(R.string.id_touch_to_display));
                    else
                        mPassphrase.setSummary(mnemonic);
                    return false;
                });
            }
        }

        // Auto logout timeout
        final int timeout = mService.getAutoLogoutTimeout();
        mAltimeout = find(PrefKeys.ALTIMEOUT);
        mAltimeout.setEntryValues(getResources().getStringArray(R.array.auto_logout_values));
        setTimeoutValues(mAltimeout);
        setTimeoutSummary(timeout);
        mAltimeout.setOnPreferenceChangeListener((preference, newValue) -> {
            final Integer altimeout = Integer.parseInt(newValue.toString());
            final SettingsData settings = mService.getModel().getSettings();
            settings.setAltimeout(altimeout);
            setTimeoutSummary(null);
            mService.getExecutor().execute(() -> updateSettings(settings));
            mService.cfgEdit().putString(PrefKeys.ALTIMEOUT, newValue.toString()); // need to save this, for scheduleDisconnect
            return true;
        });

        findPreference("network_monitor").setVisible(false);

        // SPV_SYNCRONIZATION Syncronization Submenu
        findPreference(PrefKeys.SPV_SYNCRONIZATION).setOnPreferenceClickListener(preference -> {
            final Intent intent = new Intent(getActivity(), SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SPVPreferenceFragment.class.getName() );
            startActivity(intent);
            return false;
        });

        findPreference(PrefKeys.PGP_KEY).setOnPreferenceClickListener(this::onPGPKeyClicked);

        // Version
        final Preference version = find(PrefKeys.VERSION);
        version.setSummary(String.format("%s %s",
                                         getString(R.string.app_name),
                                         getString(R.string.id_version_1s_2s,
                                                   BuildConfig.VERSION_NAME,
                                                   BuildConfig.BUILD_TYPE)));
    }

    private void logout() {
        ((LoggedActivity) getActivity()).logout();
    }

    private void initSummaries() {
        final Model model = mService.getModel();
        final AvailableCurrenciesObservable currenciesObservable = model.getAvailableCurrenciesObservable();
        final SettingsObservable settingsObservable = model.getSettingsObservable();
        final TwoFactorConfigDataObservable twoFaData = model.getTwoFactorConfigDataObservable();

        if (currenciesObservable.getAvailableCurrencies() != null)
            setPricingEntries(currenciesObservable);
        if (settingsObservable.getSettings() != null) {
            setPricingSummary(settingsObservable.getSettings().getPricing());
            mUnit.setSummary(mService.getBitcoinUnit());
            setRequiredNumBlocksSummary(model.getSettings().getRequiredNumBlocks());
            if (twoFaData.getTwoFactorConfigData() != null) {
                setLimitsText(twoFaData.getTwoFactorConfigData().getLimits());
            }
        }
    }

    private void updateSettings(final SettingsData settings) {
        try {
            final GDKTwoFactorCall gdkTwoFactorCall = mService.getSession().changeSettings(
                getActivity(), settings.toObjectNode());
            gdkTwoFactorCall.resolve(GDKTwoFactorCall.EMAIL_RESOLVER, GDKTwoFactorCall.CODE_555555_RESOLVER); // should use null
            mService.getModel().getSettingsObservable().setSettings(settings);
            UI.toast(getActivity(), R.string.id_setting_updated, Toast.LENGTH_LONG);
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(getActivity(), e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private String getDefaultFeeRate() {
        final Long minFeeRateKB = mService.getModel().getFeeObservable().getFees().get(0);
        final String minFeeRateText = String.valueOf(minFeeRateKB / 1000.0);
        return mService.cfg().getString( PrefKeys.DEFAULT_FEERATE_SATBYTE, minFeeRateText);
    }

    private boolean onWatchOnlyLoginClicked(final Preference watchOnlyLogin) {
        final GDKSession session = mService.getSession();
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_watchonly);
        final EditText inputUser = UI.find(v, R.id.input_user);
        try {
            // refetch username
            inputUser.setText(mService.getWatchOnlyUsername());
        } catch (final Exception e) {}
        final EditText inputPassword = UI.find(v, R.id.input_password);
        final MaterialDialog dialog = UI.popup(getActivity(), R.string.id_watchonly_login)
                                      .customView(v, true)
                                      .onPositive((dlg, which) -> {
            final String username = UI.getText(inputUser);
            final String password = UI.getText(inputPassword);
            if (username.isEmpty() && password.isEmpty()) {
                try {
                    //mService.disableWatchOnly(); //TODO gdk
                    UI.toast(getActivity(), R.string.id_watchonly_disabled, Toast.LENGTH_LONG);
                    watchOnlyLogin.setSummary(R.string.id_touch_to_set_up);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                session.setWatchOnly(username, password);
                watchOnlyLogin.setSummary(getString(R.string.id_enabled_1s, username));
            } catch (final Exception e) {
                UI.toast(getActivity(), R.string.id_username_not_available, Toast.LENGTH_LONG);
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onPGPKeyClicked(final Preference pgpKey) {
        final GDKSession session = mService.getSession();
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_pgp_key);
        final EditText inputPGPKey = UI.find(v, R.id.input_pgp_key);
        final SettingsData settings = mService.getModel().getSettings();
        final String oldValue = settings.getPgp() == null ? "" : settings.getPgp();
        try {
            inputPGPKey.setText(oldValue);
        } catch (final Exception e) {}

        final MaterialDialog dialog = UI.popup(getActivity(), R.string.id_pgp_key)
                                      .customView(v, true)
                                      .onPositive((dlg, which) -> {
            final String newValue = UI.getText(inputPGPKey);
            if (!newValue.equals(oldValue)) {
                settings.setPgp(newValue);
                mService.getExecutor().execute(() -> updateSettings(settings));
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onFeeRatePreferenceClicked(final Preference preference) {
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_custom_feerate);
        final EditText rateEdit = UI.find(v, R.id.set_custom_feerate_amount);

        rateEdit.setText(getDefaultFeeRate());
        rateEdit.selectAll();

        final MaterialDialog dialog;
        dialog = UI.popup(getActivity(), R.string.id_set_custom_fee_rate)
                 .customView(v, true)
                 .onPositive((dlg, which) -> {
            try {
                final Long minFeeRateKB = mService.getModel().getFeeObservable().getFees().get(0);
                final String enteredFeeRate = UI.getText(rateEdit);
                final Double enteredFeeRateKB = Double.valueOf(enteredFeeRate) * 1000;

                if (enteredFeeRateKB < minFeeRateKB) {
                    UI.toast(getActivity(), R.string.id_fee_rate_must_be_higher_than, Toast.LENGTH_LONG);
                } else {
                    mService.cfg().edit().putString(PrefKeys.DEFAULT_FEERATE_SATBYTE, enteredFeeRate).apply();
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
            mRequiredNumBlocks.setSummary("");
        else {
            final String[] prioritySummaries = getResources().getStringArray(R.array.fee_target_summaries);
            final String[] priorityValues = getResources().getStringArray(R.array.fee_target_values);
            for (int index = 0; index < priorityValues.length; index++)
                if (currentPriority.equals(Integer.valueOf(priorityValues[index])))
                    mRequiredNumBlocks.setSummary(prioritySummaries[index]);
        }
    }

    private void setTimeoutSummary(final Integer altimeout) {
        if (altimeout == null)
            mAltimeout.setSummary("");
        else {
            final String minutesText = altimeout == 1 ?
                                       "1 " + getString(R.string.id_minute) :
                                       getString(R.string.id_1d_minutes, altimeout);
            mAltimeout.setSummary(minutesText);
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

    private void setPricingEntries(final AvailableCurrenciesObservable observable) {
        final List<String> values = observable.getAvailableCurrenciesAsList();
        final List<String> formatted =
            observable.getAvailableCurrenciesAsFormattedList(getString(R.string.id_s_from_s));
        final String[] valuesArr = values.toArray(new String[values.size()]);
        final String[] formattedArr = formatted.toArray(new String[formatted.size()]);
        mPricing.setEntries(formattedArr);
        mPricing.setEntryValues(valuesArr);
    }

    private void setPricingSummary(final PricingData pricing) {
        final String summary = pricing == null ? "" : String.format(getString(
                                                                        R.string.id_s_from_s),
                                                                    pricing.getCurrency(), pricing.getExchange());
        mPricing.setSummary(summary);
    }

    private void setUnitSummary(final String value) {
        final String summary = value == null ? "" : value;
        mUnit.setSummary(summary);
    }

    private void setFeeRateSummary() {
        final Double aDouble = Double.valueOf(getDefaultFeeRate());
        final String feeRateString = SendInputFragment.getFeeRateString(Double.valueOf(aDouble * 1000).longValue());
        mFeeRate.setSummary(feeRateString);
    }

    private void attachObservers() {
        mService.getModel().getAvailableCurrenciesObservable().addObserver(this);
        mService.getModel().getSettingsObservable().addObserver(this);
        mService.getModel().getTwoFactorConfigDataObservable().addObserver( this);
    }
    private void detachObservers() {
        mService.getModel().getAvailableCurrenciesObservable().deleteObserver(this);
        mService.getModel().getSettingsObservable().deleteObserver(this);
        mService.getModel().getTwoFactorConfigDataObservable().deleteObserver( this);
    }

    @Override
    public void onResume() {
        super.onResume();
        initSummaries();
        initPinButtonTextAndSummary();
        attachObservers();
        updatesVisibilities();
    }

    private void initPinButtonTextAndSummary() {
        final int text = mService.hasPin() ? R.string.id_delete_pin : R.string.id_set_a_new_pin;
        mDeleteOrConfigurePin.setTitle(text);
        final int summaryId =
            mService.hasPin() ? R.string.id_deleting_your_pin_will_require : R.string.id_create_a_pin_to_access_your;
        mDeleteOrConfigurePin.setSummary(summaryId);
    }

    @Override
    public void onPause() {
        super.onPause();
        detachObservers();
    }

    public void updatesVisibilities() {
        if (mService.getConnectionManager() == null ||
            mService.getModel() == null ||
            mService.getModel().getTwoFactorConfig() == null)
            return;

        final boolean isHW = mService.getConnectionManager().isHW();
        mDeleteOrConfigurePin.setVisible(!isHW);
        mPassphrase.setVisible(!isHW);

        final boolean isElements = mService.isElements();
        mFeeRate.setVisible(!isElements);
        mRequiredNumBlocks.setVisible(!isElements);
        mPricing.setVisible(!isElements);
        mUnit.setVisible(!isElements);

        final boolean anyEnabled = mService.getModel().getTwoFactorConfig().isAnyEnabled();
        mLimitsPref.setVisible(anyEnabled);
        mSendNLocktimePref.setVisible(anyEnabled);
        mTwoFactorRequestResetPref.setVisible(anyEnabled);

        final boolean emailConfirmed = mService.getModel().getTwoFactorConfig().getEmail().isConfirmed();
        mEnableNLocktimePref.setVisible(emailConfirmed);
    }

    @Override
    public void update(Observable observable, Object o) {
        getActivity().runOnUiThread(() -> {
            if (observable instanceof AvailableCurrenciesObservable) {
                setPricingEntries((AvailableCurrenciesObservable) observable);
            } else if (observable instanceof SettingsObservable) {
                final SettingsData settings = ((SettingsObservable) observable).getSettings();
                setPricingSummary(settings.getPricing());
                setRequiredNumBlocksSummary(settings.getRequiredNumBlocks());
                setUnitSummary(settings.getUnit());
                setTimeoutSummary(settings.getAltimeout());
                mService.getModel().fireBalances();  // TODO should be just if unit or pricing changes
                updatesVisibilities();
            } else if (observable instanceof TwoFactorConfigDataObservable) {
                final TwoFactorConfigData twoFaData = ((TwoFactorConfigDataObservable) observable).getTwoFactorConfigData();
                setLimitsText(twoFaData.getLimits());
                updatesVisibilities();
            }
        });
    }

    private boolean prompt2FAChange(final String method, final Boolean newValue) {
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
                if (!isFiat && limitsData.get("satoshi").asLong(0) == 0) {
                    mLimitsPref.setSummary(R.string.id_set_twofactor_threshold);
                } else {
                    final String limit = mService.getValueString(limitsData, isFiat, true);
                    mLimitsPref.setSummary(limit);
                }
            } catch (final Exception e) {
                // We can throw because we have been logged out here, e.g. when
                // requesting a two-factor reset and unwinding the activity stack.
                // Since this is harmless, ignore the error here.
            }
        });
    }

    private boolean onLimitsPreferenceClicked(final Preference preference) {
        final View v = UI.inflateDialog(getActivity(), R.layout.dialog_set_limits);
        final Spinner unitSpinner = UI.find(v, R.id.set_limits_currency);
        final EditText amountEdit = UI.find(v, R.id.set_limits_amount);

        final String[] currencies;
        currencies = new String[] {mService.getBitcoinUnit(), mService.getFiatCurrency()};

        final ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        final ObjectNode limitsData = mService.getModel().getTwoFactorConfig().getLimits();
        final boolean isFiat = limitsData.get("is_fiat").asBoolean();
        unitSpinner.setSelection(isFiat ? 1 : 0);
        amountEdit.setText(mService.getValueString(limitsData, isFiat, false));
        amountEdit.selectAll();

        final MaterialDialog dialog;
        dialog = UI.popup(getActivity(), R.string.id_set_twofactor_threshold)
                 .cancelable(false)
                 .customView(v, true)
                 .onPositive((dlg, which) -> {
            try {
                final String unit = unitSpinner.getSelectedItem().toString();
                setSpendingLimits(unit, UI.getText(amountEdit));
                UI.toast(getActivity(), R.string.id_setting_updated, Toast.LENGTH_LONG);
            } catch (final Exception e) {
                UI.toast(getActivity(), "Error setting limits", Toast.LENGTH_LONG);
            }
        }).build();
        UI.showDialog(dialog);
        return false;
    }

    private boolean onSendNLocktimeClicked(final Preference preference) {
        mService.getExecutor().execute(() -> {
            try {
                mService.getSession().sendNlocktimes();
            } catch (final Exception e) {
                // Ignore, user can send again if email fails to arrive
            }
        });
        UI.toast(getActivity(), R.string.id_nlocktime_transaction_request, Toast.LENGTH_SHORT);
        return false;
    }

    private void setSpendingLimits(final String unit, final String amount) {
        final Activity activity = getActivity();

        final boolean isFiat = unit.equals(mService.getFiatCurrency());
        final String amountStr = TextUtils.isEmpty(amount) ? "0" : amount;

        final ObjectNode limitsData = new ObjectMapper().createObjectNode();
        limitsData.set("is_fiat", isFiat ? BooleanNode.TRUE : BooleanNode.FALSE);
        limitsData.set(isFiat ? "fiat" : mService.getUnitKey(), new TextNode(amountStr));

        mService.getExecutor().execute(() -> {
            try {
                final GDKTwoFactorCall call = mService.getSession().twoFactorChangeLimits(getActivity(), limitsData);
                final ObjectNode newLimits =
                    call.resolve(new PopupMethodResolver(activity), new PopupCodeResolver(activity));
                mService.getModel().getTwoFactorConfigDataObservable().updateLimits(newLimits);
                setLimitsText(newLimits);

            } catch (Exception e) {
                UI.toast(activity, R.string.id_operation_failure, Toast.LENGTH_LONG);
            }
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

