package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

abstract class GAFragment extends Fragment {
    private GreenAddressApplication gaApp;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        gaApp = (GreenAddressApplication) activity.getApplication();
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected GreenAddressApplication getGAApp() {
        return gaApp;
    }

    protected GaService getGAService() {
        return gaApp.mService;
    }
}
