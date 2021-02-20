package com.greenaddress.greenbits.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.spv.SPV;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

public abstract class GAFragment extends Fragment {
    protected GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mApp = (GreenAddressApplication) getActivity().getApplication();
        final GaActivity activity = (GaActivity) getActivity();

        try {
            context.getTheme().applyStyle(ThemeUtils.getThemeFromNetworkId(mApp.getCurrentNetwork(), context,
                                                                           activity.getMetadata()),
                                          true);
        } catch (final Exception e) {
            // Some reports show NullPointer Exception in applying style
            // Applying theme is not mandatory, doing nothing here
        }
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected NetworkData getNetwork() {
        return ((GaActivity) getActivity()).getNetwork();
    }

    protected SPV getSpv() {
        return mApp.getSpv();
    }

    public String network() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(PrefKeys.NETWORK_ID_ACTIVE,
                                                                                     "mainnet");
    }

    // Returns true if we are being restored without an activity or service
    protected boolean isZombie() {
        return getActivity() == null;
    }

    public Session getSession() {
        return Session.getSession();
    }
}
