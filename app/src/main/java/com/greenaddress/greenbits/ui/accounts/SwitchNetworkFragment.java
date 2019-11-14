package com.greenaddress.greenbits.ui.accounts;

import android.graphics.Canvas;
import android.net.Network;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SwitchNetworkFragment extends BottomSheetDialogFragment implements NetworkSwitchListener {

    public static SwitchNetworkFragment newInstance() {
        return new SwitchNetworkFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getDialog().getWindow().setGravity(Gravity.BOTTOM);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.switch_network_dialog_fragment, container, false);

        final GaActivity activity = (GaActivity) getActivity();
        final RecyclerView recyclerView = UI.find(view, R.id.switch_network_recycler);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL) {
            @Override
            public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
                // Do not draw the divider
            }
        });

        final NetworkData networkData = getGAApp().getCurrentNetworkData();
        recyclerView.setAdapter(new SwitchNetworkAdapter(getContext(), GDKSession.getNetworks(),
                                                         networkData,
                                                         networkData.getLiquid(),
                                                         this));
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        return view;
    }

    @Override
    public void onNetworkClick(final NetworkData networkData) {

        final LoggedActivity activity = (LoggedActivity) getActivity();
        final String network = networkData.getNetwork();

        if (!getGAApp().getCurrentNetwork().equals(network)) {

            getGAApp().setCurrentNetwork(network);
            activity.getConnectionManager().setNetwork(network);
            activity.logout();
        }
    }

    public GreenAddressApplication getGAApp(){
        return (GreenAddressApplication) getActivity().getApplication();
    }
}
