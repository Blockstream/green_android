package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.SubaccountDataObservable;
import com.greenaddress.greenbits.ui.components.AccountAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class HomeFragment extends GAFragment implements Observer, AccountAdapter.OnAccountSelected {

    private static final String TAG = HomeFragment.class.getSimpleName();

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
        mAccountsView.setHasFixedSize(true);
        mAccountsView.addItemDecoration(new OverlapDecoration());
        mAccountsView.setLayoutManager(new LinearLayoutManager(getActivity()));

        final AccountAdapter accountsAdapter = new AccountAdapter(mSubaccountList, getGAService(), this);
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
            s.setName(s.getName().isEmpty() ? getString(R.string.id_main) : s.getName());
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
        getGAService().getSession().setCurrentSubaccount(subaccount);
        getGAService().getModel().getActiveAccountObservable().setActiveAccount(subaccount);
    }

    class OverlapDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets (final Rect outRect, final View view, final RecyclerView parent,
                                    final RecyclerView.State state) {
            final int itemPosition = parent.getChildAdapterPosition(view);
            if (itemPosition == 0)
                return;
            final double vertOverlap = 50.0 - convertPixelsToDp(getResources().getDimension(
                                                                    R.dimen.card_size), getContext());
            final int dip = (int) convertDpToPixel((float) vertOverlap, getContext());
            outRect.set(0, dip, 0, 0);
        }
        public float convertPixelsToDp(final float px, final Context context){
            return px /
                   ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        }
        public float convertDpToPixel(final float dp, final Context context){
            final Resources resources = context.getResources();
            final DisplayMetrics metrics = resources.getDisplayMetrics();
            final float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
            return px;
        }
    }
}
