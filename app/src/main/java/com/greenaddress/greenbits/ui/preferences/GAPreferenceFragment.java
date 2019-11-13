package com.greenaddress.greenbits.ui.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import static android.content.Context.MODE_PRIVATE;

public class GAPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = GAPreferenceFragment.class.getSimpleName();

    protected GaService mService;
    private GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        mApp = (GreenAddressApplication) getActivity().getApplication();
        mService = mApp.mService;
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {}

    private static final Preference.OnPreferenceChangeListener onPreferenceChanged =
        new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            preference.setSummary(value.toString());
            return true;
        }
    };

    protected void bindPreferenceSummaryToValue(final Preference preference) {
        preference.setOnPreferenceChangeListener(onPreferenceChanged);
        // Trigger the listener immediately with the preference's
        // current value.
        final String currentVal = cfg().getString(preference.getKey(), "");
        onPreferenceChanged.onPreferenceChange(preference, currentVal);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected < T > T find(final String preferenceName) {
        return (T) findPreference(preferenceName);
    }

    protected void logout() {
        if (getActivity() instanceof LoggedActivity)
            ((LoggedActivity) getActivity()).logout();
        else if (getActivity() instanceof GaPreferenceActivity)
            ((GaPreferenceActivity) getActivity()).logout();
    }

    protected boolean openURI(final String uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        return false;
    }

    public SharedPreferences cfg() {
        return getContext().getSharedPreferences(network(), MODE_PRIVATE);
    }

    public String network() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(PrefKeys.NETWORK_ID_ACTIVE,
                                                                                     "mainnet");
    }
    public NetworkData getNetwork() {
        return mApp.getCurrentNetworkData();
    }

    public GreenAddressApplication getGAApp() {
        return mApp;
    }

    public Model getModel() {
        return mApp.getModel();
    }

    protected ConnectionManager getConnectionManager() {
        return getGAApp().getConnectionManager();
    }

    public boolean warnIfOffline(final Activity activity) {
        if (getConnectionManager().isOffline()) {
            UI.toast(activity, R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
            return true;
        }
        return false;
    }
}
