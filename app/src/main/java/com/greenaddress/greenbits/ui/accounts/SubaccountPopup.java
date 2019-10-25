package com.greenaddress.greenbits.ui.accounts;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class SubaccountPopup extends BottomSheetDialogFragment {

    public static SubaccountPopup getInstance(final String title, final String description) {
        final Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("description", description);
        final SubaccountPopup fragment = new SubaccountPopup();
        fragment.setArguments(bundle);
        return fragment;
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
        final View view = inflater.inflate(R.layout.dialog_help_authorized_subaccount, container, false);
        final TextView title = UI.find(view, R.id.title);
        final TextView description = UI.find(view, R.id.description);

        final Bundle bundle = getArguments();
        if (bundle == null)
            return view;

        title.setText(bundle.getString("title", ""));
        description.setText(bundle.getString("description", ""));
        return view;
    }
}
