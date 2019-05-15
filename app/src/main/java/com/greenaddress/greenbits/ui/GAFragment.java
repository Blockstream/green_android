package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

abstract class GAFragment extends Fragment {
    private GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mApp = (GreenAddressApplication) getActivity().getApplication();

        context.getTheme().applyStyle(ThemeUtils.getThemeFromNetworkId(mApp.mService.getNetwork(), context, null),
                                      true);
    }

    protected boolean isDisconnected() {
        return getGAService() != null && getGAService().isDisconnected();
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected GaService getGAService() {
        return mApp.mService;
    }
}
