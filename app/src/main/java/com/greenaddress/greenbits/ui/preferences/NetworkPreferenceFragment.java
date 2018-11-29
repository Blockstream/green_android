package com.greenaddress.greenbits.ui.preferences;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceCategory;

/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NetworkPreferenceFragment extends GAPreferenceFragment {
    private static final String TAG = GAPreferenceFragment.class.getSimpleName();
    private static final int QRSCANNER = 1338;
    private static final int CAMERA_PERMISSION = 150;

    private ListPreference mNetworkCustomRemove;
    private MultiSelectListPreference mNetworkSelector;
    private PreferenceCategory mCustomNetworksCategory;
    private CheckBoxPreference mCustomNetworksEnabled;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
/*
        if (mService == null) {
            Log.d(TAG, "Avoiding create on null service");
            return;
        }

        addPreferencesFromResource(R.xml.pref_network);
        setHasOptionsMenu(true);

        final Preference host = find(PrefKeys.PROXY_HOST);
        host.setOnPreferenceChangeListener(mListener);
        host.setSummary(mService.getProxyHost());
        final Preference port = find(PrefKeys.PROXY_PORT);
        port.setSummary(mService.getProxyPort());
        port.setOnPreferenceChangeListener(mListener);
        final Preference torEnabled  = find(PrefKeys.TOR_ENABLED);
        if (mService.getNetwork().getWampOnionUrl() == null)
            torEnabled.setEnabled(false);
        else {
            torEnabled.setSummary(getString(R.string.id_tor_will_use_1s_and_only_work, mService.getNetwork().getWampOnionUrl()));
            torEnabled.setOnPreferenceChangeListener((preference, o) -> {
                if (mService != null)
                    mService.getConnectionManager().disconnect();
                return true;
            });
        }
        /*
        // Network selector
        mNetworkSelector = find("network_enabled");
        mNetworkSelector.setOnPreferenceChangeListener((preference, selectedPreferencesObject) -> {
            final Set<String> selectedPreferences = (Set<String>) selectedPreferencesObject;
            if (selectedPreferences.isEmpty()) {
                UI.toast(getActivity(), R.string.id_one_network_must_be_selected, Toast.LENGTH_LONG);
                selectedPreferences.add("Bitcoin");
            }
            if (selectedPreferences.size() == 1) {
                mService.cfgGlobal("network").edit().putString("network_id_active", selectedPreferences.toArray()[0].toString()).apply();
                mService.updateActiveNetworkFromPreference();
                Log.d(TAG,"Changed active network to " + mService.getNetwork().getName());
                mService.reconnect();
            }
            mService.cfgGlobal("network").edit().putStringSet("network_enabled", selectedPreferences).apply();
            mNetworkSelector.setSummary( Joiner.on(", ").join(selectedPreferences) );

            return true;
        });
        final Set<String> selectedPreferences = mService.cfgGlobal("network").getStringSet("network_enabled", new HashSet<>());
        mNetworkSelector.setSummary( Joiner.on(", ").join(selectedPreferences) );

        mNetworkCustomRemove = find("custom_networks_remove");
        mNetworkCustomRemove.setOnPreferenceChangeListener((preference, newValue) -> {
            removeNetwork(newValue.toString());
            return true;
        });

        final EditTextPreference networkCustomAdd = find("custom_networks_add");
        networkCustomAdd.setOnPreferenceChangeListener((preference, newValue) -> {
            if (URLUtil.isValidUrl(newValue.toString()))
                addNetworkFromUrl(newValue.toString());
            else
                UI.toast(getActivity(), R.string.id_invalid_url, Toast.LENGTH_LONG);
            return true;
        });

        final Preference networkCustomAddQr = find("custom_networks_add_qr");
        networkCustomAddQr.setOnPreferenceClickListener(preference -> {
            onScanClicked();
            return true;
        });

        mCustomNetworksEnabled = find("custom_networks_enabled");
        mCustomNetworksCategory = find("custom_networks_category");
        if (!mCustomNetworksEnabled.isChecked())
            getPreferenceScreen().removePreference(mCustomNetworksCategory);

        mCustomNetworksEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
            if (find("custom_networks_category") == null)
                getPreferenceScreen().addPreference(mCustomNetworksCategory);
            else
                getPreferenceScreen().removePreference(mCustomNetworksCategory);
            return true;
        });

        refreshNetworks();
    }

    private HashMap<String, NetworkData> getStandardNetworks() {
        final HashMap<String, NetworkData> networks = GDKSession.getNetworks();
        return networks;
    }

    private HashMap<String, NetworkData> getCustomNetworks() {
        final HashMap<String, NetworkData> networks = new HashMap<String, NetworkData>();
        final Set<String> tags = mService.cfgGlobal("network").getStringSet("network_customs", new HashSet<>());
        for (final String n : tags) {
            final String networkConfig = mService.cfgGlobal("network")
                    .getString(String.format("network_%s_json", n), null);
            try {
                networks.put(n, new ObjectMapper().readValue(networkConfig, NetworkData.class));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return networks;
    }

    private Set<String> getNames(final HashMap<String, NetworkData> networks) {
        final Set<String> networksName = new HashSet<>();
        for (final NetworkData n : networks.values())
            if (n != null)
                networksName.add(n.getName());
        return networksName;
    }

    private void refreshNetworks() {
        final Set<String> networks = getNames(getCustomNetworks());

        final String[] customs = networks.toArray(new String[networks.size()]);
        mNetworkCustomRemove.setEntries(customs);
        mNetworkCustomRemove.setEntryValues(customs);

        networks.addAll(getNames(getStandardNetworks()));
        final String[] all =  networks.toArray(new String[networks.size()]);
        mNetworkSelector.setEntries(all);
        mNetworkSelector.setEntryValues(all);
    }

    private final Preference.OnPreferenceChangeListener mListener = (preference, o) -> {
        preference.setSummary(o.toString());
        if (mService != null)
            mService.getConnectionManager().disconnect();
        return true;
    };

    private void addNetworkFromUrl(final String url){
        final OkHttpClient httpClient = new OkHttpClient();
        final Request request = new Request.Builder().url(url)
                .addHeader("Accept", "application/json").build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                UI.toast(getActivity(), R.string.id_cant_connect_to_this_url, Toast.LENGTH_LONG);
            }

            @Override
            public void onResponse(final Response response) {
                try {
                    addNetwork(response.body().string());
                } catch (final Exception e) {
                    UI.toast(getActivity(), R.string.id_invalid_network_configuration, Toast.LENGTH_LONG);
                }
            }
        });
    }

    private void addNetwork(final String networkConfig) {
        final NetworkData network;
        try {
            network = new ObjectMapper().readValue(networkConfig, NetworkData.class);
        } catch (final Exception e) {
            UI.toast(getActivity(), R.string.id_invalid_network_configuration, Toast.LENGTH_LONG);
            e.printStackTrace();
            return;
        }

        final HashMap<String, NetworkData> networks = getCustomNetworks();
        networks.putAll(getStandardNetworks());
        if (networks.containsKey(network.getNetwork()) ||
                getNames(networks).contains(network.getName())) {
            UI.toast(getActivity(), R.string.id_custom_network_with_this_name, Toast.LENGTH_LONG);
            return;
        }

        UI.toast(getActivity(), R.string.id_custom_network_added_enable_it, Toast.LENGTH_LONG);

        mService.cfgGlobalEdit("network")
                .putStringSet("network_customs", networks.keySet())
                .apply();

        mService.cfgGlobalEdit("network")
                .putString(String.format("network_%s_json", network.getName()), networkConfig)
                .apply();

        refreshNetworks();
    }

    private void removeNetwork(final String networkName) {
        final Set<String> customNetworksNew = mService.cfgGlobal("network").getStringSet("network_customs", new HashSet<>());
        if (!customNetworksNew.contains(networkName)) {
            UI.toast(getActivity(), R.string.id_custom_network_not_found, Toast.LENGTH_LONG);
            return;
        }
        final Set<String> networkSelectorPreferences = mService.cfgGlobal("network").getStringSet("network_enabled", new HashSet<>());
        if (networkSelectorPreferences.contains(networkName)) {
            UI.toast(getActivity(), R.string.id_cannot_remove_enabled_network, Toast.LENGTH_LONG);
            return;
        }
        customNetworksNew.remove(networkName);
        networkSelectorPreferences.remove(networkName);

        mService.cfgGlobalEdit("network")
                .putStringSet("network_customs", customNetworksNew)
                .apply();

        final String networkNameJsonPref = String.format("network_%s_json", networkName);

        Log.d(TAG, "Deleting json custom network");
        mService.cfgGlobalEdit("network")
                .remove(networkNameJsonPref)
                .apply();

        Log.d(TAG, "Deleting preferences");
        mService.cfg("pin", networkName).edit().clear().apply();
        mService.cfg("DEFAULT_COPY", networkName).edit().clear().apply();
        mService.cfg("service", networkName).edit().clear().apply();
        mService.cfg("CONFIG", networkName).edit().clear().apply();
        mService.cfg("SPV", networkName).edit().clear().apply();

        Log.d(TAG, "Deleting chain file");
        mService.getSPVChainFile(networkName).delete();

        refreshNetworks();
    }

    private void onScanClicked() {
        final String[] perms = { "android.permission.CAMERA" };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
                getActivity().checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(perms, CAMERA_PERMISSION);
        else {
            final Intent scanner = new Intent(getActivity(), ScanActivity.class);
            startActivityForResult(scanner, QRSCANNER);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case QRSCANNER:
                if (data != null && data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT) != null) {
                    final String networkConfig = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                    addNetwork(networkConfig);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] granted) {
        if (requestCode == CAMERA_PERMISSION &&
                isPermissionGranted(granted, R.string.id_please_enable_camera))
            startActivityForResult(new Intent(getActivity(), ScanActivity.class), QRSCANNER);
    }

    protected boolean isPermissionGranted(final int[] granted, final int msgId) {
        if (granted == null || granted.length == 0 || granted[0] != PackageManager.PERMISSION_GRANTED) {
            UI.toast(getActivity(), msgId, Toast.LENGTH_SHORT);
            return false;
        }
        return true;*/
    }
}
