package com.greenaddress.greenbits.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.greenaddress.Bridge;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

public abstract class GAFragment extends Fragment {

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        final GaActivity activity = (GaActivity) getActivity();

        try {
            context.getTheme().applyStyle(ThemeUtils.getThemeFromNetworkId(Bridge.INSTANCE.getCurrentNetwork(getContext()), context,
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
