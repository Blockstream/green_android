package com.greenaddress.greenbits.ui.accounts;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SubaccountDataObservable;
import com.greenaddress.greenbits.ui.GAFragment;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.components.BottomOffsetDecoration;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class SubaccountSelectFragment extends GAFragment implements Observer, AccountAdapter.OnAccountSelected {

    private static final String TAG = SubaccountSelectFragment.class.getSimpleName();

    private RecyclerView mAccountsView;
    private Model mModel;

    private List<SubaccountData> mSubaccountList = new ArrayList<>();

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        if (getModel() == null) {
            ((LoggedActivity) getActivity()).toFirst();
            return rootView;
        }
        mModel = getModel();

        mAccountsView = UI.find(rootView, R.id.accountsList);
        mAccountsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        mAccountsView.addItemDecoration(bottomOffsetDecoration);

        final AccountAdapter accountsAdapter = new AccountAdapter(mSubaccountList, this,
                                                                  getResources(), getActivity(), getModel());
        mAccountsView.setAdapter(accountsAdapter);
        accountsAdapter.notifyDataSetChanged();

        return rootView;
    }

    private void onUpdateSubaccounts(final SubaccountDataObservable observable) {
        final List<SubaccountData> list = observable.getSubaccountDataList();
        if (list == null)
            return;
        mSubaccountList.clear();
        mSubaccountList.addAll(list);
        UI.showIf(mSubaccountList.size() == 1, UI.find(getGaActivity(), R.id.clickOnTheCard));

        getActivity().runOnUiThread(() -> mAccountsView.getAdapter().notifyDataSetChanged());
    }

    private void onUpdateBalance(final BalanceDataObservable observable) {
        getActivity().runOnUiThread(() -> mAccountsView.getAdapter().notifyDataSetChanged());
    }

    @Override
    public void update(final Observable observable, final Object o) {
        Log.d(TAG, "update " + observable);
        if (observable instanceof SubaccountDataObservable) {
            Log.d(TAG,"Update subaccounts");
            onUpdateSubaccounts((SubaccountDataObservable) observable);
            detachObservers();
            attachObservers();
        } else if (observable instanceof BalanceDataObservable) {
            Log.d(TAG,"Update balance " + ((BalanceDataObservable) observable).getSubaccount());
            onUpdateBalance((BalanceDataObservable) observable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
        if (getModel() == null)
            return;
        detachObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (getModel() == null)
            return;
        if (getConnectionManager().isPostLogin())
            attachObservers();

    }

    public void attachObservers() {
        final SubaccountDataObservable subaccountObservable = mModel.getSubaccountDataObservable();
        subaccountObservable.addObserver(this);
        onUpdateSubaccounts(subaccountObservable);
        for (final SubaccountData s : subaccountObservable.getSubaccountDataList()) {
            mModel.getBalanceDataObservable(s.getPointer()).addObserver(this);
        }
    }

    public void detachObservers() {
        final SubaccountDataObservable subaccountObservable = mModel.getSubaccountDataObservable();
        subaccountObservable.deleteObserver(this);
        for (final SubaccountData s : subaccountObservable.getSubaccountDataList()) {
            final Observable obsBalance = mModel.getBalanceDataObservable(s.getPointer());
            if (obsBalance != null)
                obsBalance.deleteObserver(this);
        }
    }

    @Override
    public void onAccountSelected(final int subaccount) {
        getModel().getActiveAccountObservable().setActiveAccount(subaccount);
        if (getConnectionManager().isLoginWithPin()) {
            cfg().edit().putInt(PrefKeys.ACTIVE_SUBACCOUNT, subaccount).apply();
        }
        getGaActivity().finish();
        getGaActivity().overridePendingTransition(0,0);
    }

    @Override
    public void onNewSubaccount() {
        final MaterialDialog dialog = UI.popup(getActivity(), R.string.id_standard_account)
                                      .content(R.string.id_standard_accounts_allow_you_to)
                                      .input(getString(R.string.id_name), "", new MaterialDialog.InputCallback() {
            @Override
            public void onInput(@NonNull final MaterialDialog dialog, final CharSequence input) {
                if (input.length() == 0)
                    return;
                try {
                    GDKSession.getSession().createSubAccount(getActivity(), input.toString(), "2of2").resolve(null,
                                                                                                              null);
                    getModel().getSubaccountDataObservable().refresh();
                } catch (final Exception e) {
                    Toast.makeText(getContext(), R.string.id_operation_failure, Toast.LENGTH_LONG).show();
                }
            }
        }).build();
        UI.showDialog(dialog);
    }
}
