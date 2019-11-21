package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.CB;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.ScanForResultActivity;
import com.greenaddress.greenbits.ui.UI;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class SPVPreferenceFragment extends GAPreferenceFragment
    implements Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {

    private static final String TAG = GAPreferenceFragment.class.getSimpleName();
    private static final int QRSCANNER = 1338;
    private static final int CAMERA_PERMISSION = 150;

    private EditTextPreference mTrustedPeer;
    private Preference mResetSPV;
    private Preference mScanSPV;
    private CheckBoxPreference mSPVEnabled;
    private CheckBoxPreference mSPVSyncOnMobile;
    private SPV mSpv;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_spv);
        setHasOptionsMenu(true);

        if (getGAApp().getModel() == null) {
            logout();
            return;
        }

        mTrustedPeer = find("trusted_peer");
        mResetSPV = find("reset_spv");
        mSPVEnabled = find("spvEnabled");
        mSPVSyncOnMobile = find("spvSyncMobile");
        mScanSPV = find("scan_spv");

        mSPVEnabled.setSingleLineTitle(false);
        mSPVSyncOnMobile.setSingleLineTitle(false);
        mSpv = getGAApp().getSpv();
        final boolean isSpvEnabled = cfg().getBoolean(PrefKeys.SPV_ENABLED, false);

        if (getGAApp().isWatchOnly()) {
            // Do not allow editing of SPV_SYNCRONIZATION prefs from watch only logins
            mTrustedPeer.setEnabled(false);
            mResetSPV.setEnabled(false);
            mSPVEnabled.setEnabled(false);
            mSPVSyncOnMobile.setEnabled(false);
            mScanSPV.setEnabled(false);
        } else {
            // Initialise values and bindings for preference changes
            bindPreferenceSummaryToValue(mTrustedPeer);

            mSPVEnabled.setChecked(isSpvEnabled);
            mSPVSyncOnMobile.setChecked(cfg().getBoolean(PrefKeys.SPV_MOBILE_SYNC_ENABLED, false));

            mTrustedPeer.setEnabled(isSpvEnabled);
            mScanSPV.setEnabled(isSpvEnabled);
            final boolean setTextValue = true;
            final String trustedPeers =
                cfg().getString(PrefKeys.TRUSTED_ADDRESS, cfg().getString("trusted_peer", "")).trim();
            setTrustedPeersPreference(trustedPeers, setTextValue);

            mResetSPV.setOnPreferenceClickListener(this);
            mSPVEnabled.setOnPreferenceChangeListener(this);
            mSPVSyncOnMobile.setOnPreferenceChangeListener(this);
            mTrustedPeer.setOnPreferenceChangeListener(this);
            mScanSPV.setOnPreferenceClickListener(this);
        }

        getActivity().setResult(Activity.RESULT_OK, null);
    }

    private void setTrustedPeersPreference(final String peers, final boolean setTextValue) {

        if (peers.isEmpty()) {
            mTrustedPeer.setSummary(R.string.id_example_89014283334011612858333);
            return;
        }
        if (setTextValue)
            mTrustedPeer.setText(peers);
        mTrustedPeer.setSummary(peers);
    }

    private void setTrustedPeers(final String peers) {
        final boolean setTextValue = false;
        setTrustedPeersPreference(peers, setTextValue);
        mSpv.setSPVTrustedPeersAsync(peers);
    }

    private void runOnService(final Runnable runnable) {
        Futures.addCallback(mSpv.onServiceAttached, new CB.Op<Void>() {
            @Override
            public void onSuccess(final Void result) {
                runnable.run();
            }
        });
        if (mSpv.getService() != null) {
            try {
                mSpv.startService(getGAApp());
            } catch (final Exception e) {
                e.printStackTrace();
                Toast.makeText(getGAApp(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            runnable.run();
        }
    }

    private boolean onSPVEnabledChanged(final Boolean newValue) {
        mTrustedPeer.setEnabled(newValue);
        mSpv.setSPVEnabledAsync(newValue);
        mSPVSyncOnMobile.setEnabled(newValue);
        mScanSPV.setEnabled(newValue);
        final String network = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
            PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        final SharedPreferences preferences = getContext().getSharedPreferences(network, MODE_PRIVATE);
        final boolean proxyEnabled = preferences.getBoolean(PrefKeys.PROXY_ENABLED, false);
        final boolean torEnabled = preferences.getBoolean(PrefKeys.TOR_ENABLED, false);
        final String peers =
            preferences.getString(PrefKeys.TRUSTED_ADDRESS, preferences.getString("trusted_peer", "")).trim();

        if (!newValue) {
            mSPVSyncOnMobile.setChecked(false);
            mSpv.setSPVSyncOnMobileEnabledAsync(false);
        } else if ((torEnabled || proxyEnabled) && peers.isEmpty()) {
            if (getActivity() == null) {
                return true;
            }

            final MaterialDialog.Builder builder = UI.popup(
                getActivity(),
                R.string.id_warning_no_trusted_node_set,
                android.R.string.ok)
                                                   .content(R.string.id_spv_synchronization_using_tor)
                                                   .cancelable(false)
                                                   .onAny((dialog,
                                                           which) -> getPreferenceManager().showDialog(mTrustedPeer));
            getActivity().runOnUiThread(builder::show);
        }
        return true;
    }

    private boolean onSPVSyncOnMobileChanged(final Boolean newValue) {
        mSpv.setSPVSyncOnMobileEnabledAsync(newValue);
        return true;
    }

    private boolean isBadAddress(final String s) {
        try {
            if (!s.isEmpty())
                new URI("btc://" + s);
            return false;
        } catch (final URISyntaxException e) {}
        return true;
    }

    private boolean onTrustedPeerChange(final String newValue) {
        final String network = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
            PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        final SharedPreferences preferences = getContext().getSharedPreferences(network, MODE_PRIVATE);
        final String prefPeers =
            preferences.getString(PrefKeys.TRUSTED_ADDRESS, preferences.getString("trusted_peer", "")).trim();
        final String peers = newValue.trim().replaceAll("\\s","");

        if (prefPeers.equals(peers))
            return false;

        if (peers.isEmpty()) {
            setTrustedPeers(peers);
            return true;
        }

        for (final String peer: peers.split(","))
            if (isBadAddress(peer)) {
                UI.popup(getActivity(), R.string.id_invalid_address, android.R.string.ok)
                .content(R.string.id_enter_a_valid_onion_or_ip).build().show();
                return false;
            }

        if (peers.toLowerCase(Locale.US).contains(".onion")) {
            // Tor address
            if (!cfg().getBoolean(PrefKeys.TOR_ENABLED, false)) {
                UI.popup(getActivity(), R.string.id_tor_connectivity_disabled, android.R.string.ok)
                .content(R.string.id_onion_addresses_require_tor).build().show();
                return false;
            }
            setTrustedPeers(peers);
            return true;
        }

        // Force the user to confirm that they want to use a non-Tor host
        UI.popup(getActivity(), R.string.id_warning_nononion_address)
        .content(R.string.id_connecting_to_tor_onion_nodes)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                setTrustedPeers(peers);
            }
        }).build().show();
        return true;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference == mSPVEnabled) {
            runOnService(() -> {
                onSPVEnabledChanged((Boolean) newValue);
            });
            return true;
        }
        if (preference == mSPVSyncOnMobile) {
            runOnService(() -> {
                onSPVSyncOnMobileChanged((Boolean) newValue);
            });
            return true;
        }
        if (preference == mTrustedPeer) {
            runOnService(() -> {
                onTrustedPeerChange((String) newValue);
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preference == mResetSPV) {
            UI.toast(getActivity(), R.string.id_spv_reset_and_restarted, Toast.LENGTH_LONG);
            runOnService(() -> { mSpv.resetSPVAsync(); });
        } else if (preference == mScanSPV) {
            onScanClicked();
        }
        return false;
    }

    private void onScanClicked() {
        final String[] perms = { "android.permission.CAMERA" };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
            getActivity().checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(perms, CAMERA_PERMISSION);
        else {
            final Intent scanner = new Intent(getActivity(), ScanForResultActivity.class);
            startActivityForResult(scanner, QRSCANNER);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] granted) {
        if (requestCode == CAMERA_PERMISSION && granted != null && granted.length > 0 &&
            granted[0] == PackageManager.PERMISSION_GRANTED)
            startActivityForResult(new Intent(getActivity(), ScanForResultActivity.class), QRSCANNER);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QRSCANNER && data != null &&
            data.getStringExtra(ScanForResultActivity.INTENT_EXTRA_RESULT) != null) {
            String address = data.getStringExtra(ScanForResultActivity.INTENT_EXTRA_RESULT);
            address = address.contains("://") ? address.split("://")[1] : address;
            setTrustedPeers(address);
        }
    }


}
