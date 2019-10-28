package com.greenaddress.greenbits.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.GAFragment;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.io.IOException;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SubaccountAddFragment extends GAFragment {

    private final int[] mRadioButtonIDs = {R.id.simpleAccountRadio, R.id.authorizedAccountRadio};
    private final int[] mConstraintIDs = {R.id.simpleAccount, R.id.authorizedAccount};

    public static final String[] ACCOUNT_TYPES = {"2of2", "2of2_no_recovery"};
    public static final int SIMPLE_ACCOUNT = 0;
    public static final int AUTHORIZED_ACCOUNT = 1;

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_subaccount_add, container, false);
        final Button button = UI.find(rootView, R.id.continueButton);
        final RadioButton[] radioButtons = new RadioButton[mRadioButtonIDs.length];
        final ConstraintLayout[] constraintLayouts = new ConstraintLayout[mConstraintIDs.length];

        button.setOnClickListener(v -> {
            int type = SIMPLE_ACCOUNT;
            for (int i = 0; i < radioButtons.length; i++) {
                if (radioButtons[i].isChecked())
                    type = i;
            }
            final Fragment fragment = SubaccountReviewFragment.newInstance(type);
            getActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
        });

        for (int i = 0; i < mRadioButtonIDs.length; i++) {
            final RadioButton radioButton = UI.find(rootView, mRadioButtonIDs[i]);
            final ConstraintLayout constraintLayout = UI.find(rootView, mConstraintIDs[i]);
            radioButtons[i] = radioButton;
            constraintLayouts[i] = constraintLayout;

            // Needed because there's no radiogroup
            radioButtons[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    for (int j = 0; j < radioButtons.length; j++)
                        if (buttonView.getId() != mRadioButtonIDs[j])
                            radioButtons[j].setChecked(false);
                }
            });

            constraintLayouts[i].setOnClickListener(v -> radioButton.performClick());
        }

        final boolean isLiquid = getNetwork().getLiquid();

        // Can't have more than one authorized subaccount
        if (!isLiquid || hasAnyAASubaccount()) {
            radioButtons[AUTHORIZED_ACCOUNT].setEnabled(false);
            constraintLayouts[AUTHORIZED_ACCOUNT].setOnClickListener(null);

            final TextView title = UI.find(rootView, R.id.authorizedAccountTitle);
            title.setTextColor(getResources().getColor(R.color.grey_light));
        }

        final TextView[] labels = {UI.find(rootView, R.id.simpleAccountLabel), UI.find(rootView,
                                                                                       R.id.authorizedAccountLabel)};
        final String[] titles = {getString(R.string.id_standard_account), getString(
                                     R.string.id_liquid_securities_account)};
        final String[] descriptions = { getString(R.string.id_standard_accounts_allow_you_to),
                                        String.format("%s\n\n%s", getString(R.string.id_liquid_securities_accounts_are),
                                                      getString(R.string.id_twofactor_protection_does_not))};
        for (int i = 0; i < labels.length; i++) {
            final String title = titles[i];
            final String description = descriptions[i];

            labels[i].setOnClickListener(v -> {
                final SubaccountPopup s = SubaccountPopup.getInstance(title, description);
                final FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                s.show(ft, "dialog");
            });
        }

        return rootView;
    }

    private boolean hasAnyAASubaccount() {
        try {
            final List<SubaccountData> accounts = GDKSession.getSession().getSubAccounts();
            for (final SubaccountData account: accounts) {
                if (account.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT]))
                    return true;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
