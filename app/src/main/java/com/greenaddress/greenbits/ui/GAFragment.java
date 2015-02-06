package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenbits.GreenAddressApplication;

abstract public class GAFragment extends Fragment {
    @Override
    public void onResume() {
        super.onResume();
        if (((GreenAddressApplication) getActivity().getApplication()).gaService == null) {
            getActivity().finish();
            return;
        }
        onGAResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (((GreenAddressApplication) getActivity().getApplication()).gaService == null) {
            getActivity().finish();
            return null;
        }
        return onGACreateView(inflater, container, savedInstanceState);
    }

    abstract View onGACreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);
    void onGAResume() { };
}
