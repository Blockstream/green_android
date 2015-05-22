package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Observer {

    public static final int REQUEST_ENABLE_2FA = 0;
    private String twoFacMethod;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (getGAService() == null) {
            finish();
            return;
        }

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'GreenBits' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_notification);

        // Add 'two factor authentication' preferences.
        PreferenceCategory fakeHeaderSecurity = new PreferenceCategory(this);
        fakeHeaderSecurity.setTitle(R.string.pref_header_twofactor);
        getPreferenceScreen().addPreference(fakeHeaderSecurity);
        addPreferencesFromResource(R.xml.pref_two_factor);

        // Add 'more settings' preferences
        PreferenceCategory fakeHeaderMore = new PreferenceCategory(this);
        fakeHeaderMore.setTitle(R.string.pref_header_more);
        getPreferenceScreen().addPreference(fakeHeaderMore);
        addPreferencesFromResource(R.xml.pref_more);

        final String mnemonic = getGAService().getMnemonics();
        if (mnemonic != null) {
            getPreferenceManager().findPreference("mnemonic_passphrase").setSummary(getString(R.string.touch_to_display));

            getPreferenceManager().findPreference("mnemonic_passphrase").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    getPreferenceManager().findPreference("mnemonic_passphrase").setSummary(mnemonic);
                    return false;
                }
            });
        }

        getPreferenceManager().findPreference("go_to_app_for_more_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("it.greenaddress.cordova");
                if (launchIntent == null) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=it.greenaddress.cordova")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=it.greenaddress.cordova")));
                    }
                } else {
                    startActivity(launchIntent);
                }
                return false;
            }
        });

        getPreferenceManager().findPreference("ledger_wallet").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ledgerwallet.com/r/bead")));
                return false;
            }
        });

        if (getGAService().isBTChip()) {
            getPreferenceScreen().removePreference(getPreferenceManager().findPreference("ledger_wallet"));
        }

        final EditTextPreference altime = (EditTextPreference) getPreferenceManager().findPreference("altime");

        int timeout = 5;
        try {
            timeout = (int) getGAService().getAppearanceValue("altimeout");
        } catch (final Exception e) {
            // not set
        }
        altime.setSummary(Integer.toString(timeout) + getString(R.string.autologout_time_default));

        altime.setText(Integer.toString(timeout));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {

                try {
                    getGAService().setAppearanceValue("altimeout", Integer.parseInt(newValue.toString()), true);
                    altime.setSummary(Integer.parseInt(newValue.toString()) + " minutes");

                    return true;
                } catch (final Exception e) {
                    // not set
                }

                return false;
            }
        });

        final CheckBoxPreference spvEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("spvEnabled");
        final SharedPreferences spvPreferences = getSharedPreferences("SPV", MODE_PRIVATE);
        spvEnabled.setChecked(spvPreferences.getBoolean("enabled", true));
        spvEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                SharedPreferences.Editor editor = spvPreferences.edit();
                editor.putBoolean("enabled", (Boolean) newValue);
                editor.apply();

                new MaterialDialog.Builder(SettingsActivity.this)
                        .title(getResources().getString(R.string.changingRequiresRestartTitle))
                        .content(getResources().getString(R.string.changingRequiresRestartText))
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.white)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK)
                        .positiveText("OK")
                        .build().show();
                return true;
            }
        });

        Map<?, ?> twoFacConfig = getGAService().getTwoFacConfig();

        final CheckBoxPreference emailTwoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFacEmail");
        emailTwoFacEnabled.setChecked(twoFacConfig.get("email").equals(true));
        emailTwoFacEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                change2FA("Email", (Boolean) newValue);
                return false;
            }
        });

        final CheckBoxPreference gauthTwoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFacGauth");
        gauthTwoFacEnabled.setChecked(twoFacConfig.get("gauth").equals(true));
        gauthTwoFacEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                change2FA("Gauth", (Boolean) newValue);
                return false;
            }
        });

        final CheckBoxPreference smsTwoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFacSMS");
        smsTwoFacEnabled.setChecked(twoFacConfig.get("sms").equals(true));
        smsTwoFacEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                change2FA("SMS", (Boolean) newValue);
                return false;
            }
        });

        final CheckBoxPreference twoFacWarning = (CheckBoxPreference) getPreferenceManager().findPreference("twoFacWarning");
        twoFacWarning.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new MaterialDialog.Builder(SettingsActivity.this)
                        .title(getResources().getString(R.string.changingRequiresRestartTitle))
                        .content(getResources().getString(R.string.changingRequiresRestartText))
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.white)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK)
                        .positiveText("OK")
                        .build().show();
                return true;
            }
        });

        final CheckBoxPreference phoneTwoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFacPhone");
        phoneTwoFacEnabled.setChecked(twoFacConfig.get("phone").equals(true));
        phoneTwoFacEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                change2FA("Phone", (Boolean) newValue);
                return false;
            }
        });
    }

    private void change2FA(final String method, final Boolean newValue) {
        if (newValue) {
            final Intent intent = new Intent(this, TwoFactorActivity.class);
            intent.putExtra("method", method.toLowerCase());
            twoFacMethod = method;
            startActivityForResult(intent, REQUEST_ENABLE_2FA);
        } else {
            String[] enabledTwoFacNames = new String[]{};
            final List<String> enabledTwoFacNamesSystem = getGAService().getEnabledTwoFacNames(true);
            if (enabledTwoFacNamesSystem.size() > 1) {
                new MaterialDialog.Builder(this)
                        .title(R.string.twoFactorChoicesTitle)
                        .items(getGAService().getEnabledTwoFacNames(false).toArray(enabledTwoFacNames))
                        .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                disable2FA(method, enabledTwoFacNamesSystem.get(which));
                                return true;
                            }
                        })
                        .positiveText(R.string.choose)
                        .negativeText(R.string.cancel)
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.accent)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK)
                        .build().show();
            } else {
                disable2FA(method, enabledTwoFacNamesSystem.get(0));
            }
        }
    }

    private void disable2FA(final String method, final String withMethod) {
        if (!withMethod.equals("gauth")) {
            final Map<String, String> data = new HashMap<>();
            data.put("method", method.toLowerCase());
            getGAService().requestTwoFacCode(withMethod, "disable_2fa", data);
        }
        final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);
        final EditText twoFacValue = (EditText) inflatedLayout.findViewById(R.id.btchipPINValue);
        final TextView prompt = (TextView) inflatedLayout.findViewById(R.id.btchipPinPrompt);
        final String[] allTwoFac = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] allTwoFacSystem = getResources().getStringArray(R.array.twoFactorChoicesSystem);
        String withMethodName = "";
        int i = 0;
        for (String name : allTwoFacSystem) {
            if (name.equals(withMethod)) {
                withMethodName = allTwoFac[i];
                break;
            }
            i++;
        }
        prompt.setText(new Formatter().format(
                getResources().getString(R.string.twoFacProvideConfirmationCode),
                withMethodName).toString());
        new MaterialDialog.Builder(this)
                .title("2FA")
                .customView(inflatedLayout, true)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.accent)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .positiveText("OK")
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        Map<String, String> twoFacData = new HashMap<>();
                        twoFacData.put("method", withMethod);
                        twoFacData.put("code", twoFacValue.getText().toString());
                        Futures.addCallback(getGAService().disableTwoFac(method.toLowerCase(), twoFacData), new FutureCallback<Boolean>() {
                            @Override
                            public void onSuccess(@Nullable Boolean result) {
                                final CheckBoxPreference twoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFac" + method);
                                twoFacEnabled.setChecked(false);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                t.printStackTrace();
                                Toast.makeText(SettingsActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).build().show();
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final CheckBoxPreference twoFacEnabled = (CheckBoxPreference) getPreferenceManager().findPreference("twoFac" + twoFacMethod);
            twoFacEnabled.setChecked(true);
        }
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    @Override
    public void onResume() {
        super.onResume();

        if (getGAService() == null) {
            finish();
            return;
        }

        getGAApp().getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}
