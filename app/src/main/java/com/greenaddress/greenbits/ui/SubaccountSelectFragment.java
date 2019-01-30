package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.SubaccountDataObservable;
import com.greenaddress.greenbits.ui.components.AccountAdapter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class SubaccountSelectFragment extends GAFragment implements Observer, AccountAdapter.OnAccountSelected {

    private static final String TAG = SubaccountSelectFragment.class.getSimpleName();

    private RecyclerView mAccountsView;
    private Model mModel;

    private final ArrayList<SubaccountData> mSubaccountList = new ArrayList<>();

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        mModel = getGAService().getModel();

        mAccountsView = UI.find(rootView, R.id.accountsList);
        mAccountsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        mAccountsView.addItemDecoration(bottomOffsetDecoration);

        final AccountAdapter accountsAdapter = new AccountAdapter(mSubaccountList, getGAService(), this, getResources(), getActivity());
        mAccountsView.setAdapter(accountsAdapter);
        accountsAdapter.notifyDataSetChanged();

        return rootView;
    }

    private void onUpdateSubaccounts(final SubaccountDataObservable observable) {
        final List<SubaccountData> subaccounts = observable.getSubaccountDataList();
        if (subaccounts == null)
            return;

        UI.showIf(subaccounts.size() == 1, UI.find(getGaActivity(), R.id.clickOnTheCard));

        mSubaccountList.clear();
        for (final SubaccountData s : subaccounts) {
            s.setName(s.getNameWithDefault(getString(R.string.id_main)));
            mSubaccountList.add(s);
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAccountsView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    private void onUpdateBalance(final BalanceDataObservable observable) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAccountsView.getAdapter().notifyDataSetChanged();
            }
        });
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
        detachObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (getGAService() != null && getGAService().getConnectionManager().isPostLogin())
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
        getGAService().getModel().getActiveAccountObservable().setActiveAccount(subaccount);
        if(getGAService().getConnectionManager().isLoginWithPin()) {
            getGAService().cfg().edit().putInt(PrefKeys.ACTIVE_SUBACCOUNT, subaccount).apply();
        }
        getGaActivity().finish();
    }

}
