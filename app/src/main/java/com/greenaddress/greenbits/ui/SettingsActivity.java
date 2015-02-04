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

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Observable;
import java.util.Observer;

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_notification);

        PreferenceCategory fakeHeaderMore = new PreferenceCategory(this);
        fakeHeaderMore.setTitle(R.string.pref_header_more);
        getPreferenceScreen().addPreference(fakeHeaderMore);
        addPreferencesFromResource(R.xml.pref_more);

        final String mnemonic = ((GreenAddressApplication) getApplication()).gaService.getMnemonics();
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

        if (((GreenAddressApplication) getApplication()).gaService.isBTChip()) {
            getPreferenceScreen().removePreference(getPreferenceManager().findPreference("ledger_wallet"));
        }

        final EditTextPreference altime = (EditTextPreference) getPreferenceManager().findPreference("altime");

        int timeout = 5;
        try {
            timeout = (int) ((GreenAddressApplication) getApplication()).gaService.getAppearanceValue("altimeout");
        } catch (final Exception e) {
            // not set
        }
        altime.setSummary(Integer.toString(timeout) + getString(R.string.autologout_time_default));

        altime.setText(Integer.toString(timeout));
        altime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {

                try {
                    ((GreenAddressApplication) getApplication()).gaService.setAppearanceValue("altimeout", Integer.parseInt(newValue.toString()), true);
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
                editor.commit();

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
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    @Override
    public void onResume() {
        super.onResume();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().deleteObserver(this);
    }
}
