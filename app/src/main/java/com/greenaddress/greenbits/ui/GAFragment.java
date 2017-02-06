package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

abstract class GAFragment extends Fragment {
    private GreenAddressApplication mApp;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        mApp = (GreenAddressApplication) activity.getApplication();
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected GaService getGAService() {
        return mApp.mService;
    }
}
