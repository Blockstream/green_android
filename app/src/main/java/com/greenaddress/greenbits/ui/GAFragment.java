package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

abstract class GAFragment extends Fragment {
    private GreenAddressApplication gaApp;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        gaApp = (GreenAddressApplication) activity.getApplication();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getGAService() == null) {
            getActivity().finish();
            return;
        }
        try {
            onGAResume();
        } catch (final NullPointerException npe) {
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState) {
        if (getGAService() == null) {
            getActivity().finish();
            return null;
        }
        try {
            return onGACreateView(inflater, container, savedInstanceState);
        } catch (final NullPointerException npe) {
            getActivity().finish();
            return null;
        }
    }

    abstract View onGACreateView(final LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState);

    void onGAResume() {
    }

    protected GreenAddressApplication getGAApp() {
        return gaApp;
    }

    protected GaService getGAService() {
        if (gaApp != null) {
            return gaApp.gaService;
        }
        return null;
    }
}
