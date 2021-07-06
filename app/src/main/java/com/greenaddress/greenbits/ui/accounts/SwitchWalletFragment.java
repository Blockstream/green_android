package com.greenaddress.greenbits.ui.accounts;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.greenaddress.Bridge;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import javax.annotation.Nullable;

public class SwitchWalletFragment extends BottomSheetDialogFragment implements WalletSwitchListener {

    public static SwitchWalletFragment newInstance() {
        return new SwitchWalletFragment();
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
        final View view = inflater.inflate(R.layout.switch_wallet_dialog_fragment, container, false);

        final RecyclerView recyclerView = UI.find(view, R.id.switch_network_recycler);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL) {
            @Override
            public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
                // Do not draw the divider
            }
        });

        // final NetworkData networkData = Bridge.INSTANCE.getCurrentNetworkData(getContext());
        recyclerView.setAdapter(new SwitchWalletAdapter(getContext(), Bridge.INSTANCE.getWallets(), this));
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        UI.find(view, R.id.buttonLogout).setOnClickListener(v -> {
            final LoggedActivity activity = (LoggedActivity) getActivity();
            activity.logout(null);
            dismiss();
        });

        return view;
    }

    @Override
    public void onWalletClick(@Nullable Long walletId) {
        final LoggedActivity activity = (LoggedActivity) getActivity();
        if(walletId != null){
            activity.logout(walletId);
        }
        dismiss();
    }
}
