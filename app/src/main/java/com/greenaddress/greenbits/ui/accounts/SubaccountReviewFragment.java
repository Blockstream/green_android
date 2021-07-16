package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.greenaddress.greenbits.ui.GAFragment;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import androidx.fragment.app.FragmentTransaction;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


import static com.greenaddress.greenbits.ui.accounts.SubaccountAddFragment.*;

public class SubaccountReviewFragment extends GAFragment {

    public static SubaccountReviewFragment newInstance(final int type) {
        final Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        final SubaccountReviewFragment fragment = new SubaccountReviewFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_subaccount_review, container, false);
        final TextInputEditText textInputEditText = UI.find(rootView, R.id.accountName);
        final Button button = UI.find(rootView, R.id.createButton);

        final Bundle bundle = getArguments();
        if (bundle == null) {
            getActivity().finish();
            return rootView;
        }
        final int accountType = bundle.getInt("type", SIMPLE_ACCOUNT);

        textInputEditText.addTextChangedListener(new UI.TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                button.setEnabled(s != null && s.length() != 0);
            }
        });

        button.setOnClickListener(v -> {
            createAccount(ACCOUNT_TYPES[accountType], textInputEditText.getText());
        });

        if (accountType == AUTHORIZED_ACCOUNT) {
            textInputEditText.setText(getResources().getText(R.string.id_amp_account));
            textInputEditText.setEnabled(false);
            button.setEnabled(true);
        } else {
            textInputEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            UI.find(rootView, R.id.receivingIdTitle).setVisibility(View.GONE);
            UI.find(rootView, R.id.receivingIdValue).setVisibility(View.GONE);
        }

        final int[] accounts = {R.string.id_standard_account, R.string.id_amp_account};
        final TextView accountTypeValue = UI.find(rootView, R.id.accountTypeValue);
        accountTypeValue.setText(getString(accounts[accountType]));

        final String[] titles = {getString(R.string.id_standard_account), getString(
                                     R.string.id_amp_account)};
        final String[] descriptions = { getString(R.string.id_standard_accounts_allow_you_to),
                                        String.format("%s\n\n%s", getString(R.string.id_amp_accounts_are_only_available),
                                                      getString(R.string.id_twofactor_protection_does_not))};
        UI.find(rootView, R.id.accountTypeTitle).setOnClickListener(v -> {
            final SubaccountPopup s = SubaccountPopup.getInstance(titles[accountType], descriptions[accountType]);
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.addToBackStack(null);
            s.show(ft, "dialog");
        });
        UI.find(rootView, R.id.receivingIdTitle).setOnClickListener( v -> {
            final SubaccountPopup s =
                SubaccountPopup.getInstance(getString(R.string.id_amp_id),
                                            getString(R.string.id_provide_your_amp_id_to_the));
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.addToBackStack(null);
            s.show(ft, "dialog");
        });
        return rootView;
    }

    private void createAccount(final String type, final Editable input) {
        final GaActivity activity = getGaActivity();
        Observable.just(getSession())
        .subscribeOn(Schedulers.computation())
        .map((session) -> {
            return getSession().createSubAccount(input.toString(), type).resolve(new HardwareCodeResolver(activity), null);
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe((session) -> {
            activity.finish();
        }, (final Throwable e) -> {
            Toast.makeText(activity, R.string.id_operation_failure,
                           Toast.LENGTH_LONG).show();
        });
    }
}
