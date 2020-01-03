package com.greenaddress.greenbits.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.SubaccountsDataObservable;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.ThemeUtils;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.components.BottomOffsetDecoration;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class SubaccountSelectActivity extends LoggedActivity implements Observer, AccountAdapter.OnAccountSelected {

    private TextView mTotalAmountBtc;
    private TextView mTotalAmountFiat;
    private RecyclerView mRecyclerView;
    private final List<SubaccountData> mSubaccountList = new ArrayList<>();
    private static final int REQUEST_CREATE_SUBACCOUNT = 101;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (modelIsNullOrDisconnected())
            return;
        setContentView(R.layout.activity_subaccount_select);

        UI.preventScreenshots(this);

        mTotalAmountBtc = findViewById(R.id.total_amount_btc);
        mTotalAmountFiat = findViewById(R.id.total_amount_fiat);

        mRecyclerView = UI.find(this, R.id.accountsList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        final boolean isWatchonly = getConnectionManager().isWatchOnly();
        final AccountAdapter accountsAdapter = new AccountAdapter(mSubaccountList, this, !isWatchonly, getModel());
        mRecyclerView.addItemDecoration(bottomOffsetDecoration);
        mRecyclerView.setAdapter(accountsAdapter);
        accountsAdapter.notifyDataSetChanged();

        updateBalance();
        updateSubaccounts();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;
        attachObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing())
            return;
        detachObservers();
    }

    public void attachObservers() {
        final SparseArray<BalanceDataObservable> balanceObservables = getModel().getBalanceDataObservables();
        final SubaccountsDataObservable subaccountObservable = getModel().getSubaccountsDataObservable();
        subaccountObservable.addObserver(this);
        for (final SubaccountData o : subaccountObservable.getSubaccountsDataList()) {
            getModel().getBalanceDataObservable(o.getPointer()).addObserver(this);
        }
        for (int i = 0; i < balanceObservables.size(); i++) {
            balanceObservables.valueAt(i).addObserver(this);
        }
    }

    public void detachObservers() {
        final SparseArray<BalanceDataObservable> balanceObservables = getModel().getBalanceDataObservables();
        final SubaccountsDataObservable subaccountObservable = getModel().getSubaccountsDataObservable();
        subaccountObservable.deleteObserver(this);
        for (final SubaccountData o : subaccountObservable.getSubaccountsDataList()) {
            final Observable obsBalance = getModel().getBalanceDataObservable(o.getPointer());
            if (obsBalance != null)
                obsBalance.deleteObserver(this);
        }
        for (int i = 0; i < balanceObservables.size(); i++) {
            balanceObservables.valueAt(i).deleteObserver(this);
        }
    }

    @Override
    public void update(final Observable observable, final Object o) {
        super.update(observable, o);
        runOnUiThread(() -> {
            updateBalance();
            updateSubaccounts();
        });
    }

    private void updateBalance() {
        final SparseArray<BalanceDataObservable> balanceObservables = getModel().getBalanceDataObservables();
        long totalSatoshi = 0L;
        for (int i = 0; i < balanceObservables.size(); i++) {
            final long satoshi = balanceObservables.valueAt(i).getBtcBalanceData();
            totalSatoshi += satoshi;
        }
        final BalanceData balanceReq = new BalanceData();
        balanceReq.setSatoshi(totalSatoshi);
        try {
            final BalanceData total = getSession().convertBalance(balanceReq);
            final String btcString = getModel().getBtc(total, true);
            final String fiatString = getModel().getFiat(total, true);
            mTotalAmountBtc.setTextColor(ThemeUtils.resolveColorAccent(this));
            mTotalAmountBtc.setText(btcString);
            mTotalAmountFiat.setText(" ≈ " + fiatString);
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(this, R.string.id_you_are_not_connected_please, Toast.LENGTH_LONG);
        }
    }

    private void updateSubaccounts() {
        final SubaccountsDataObservable observable = getModel().getSubaccountsDataObservable();
        final List<SubaccountData> list = observable.getSubaccountsDataList();
        if (list == null)
            return;
        mSubaccountList.clear();
        mSubaccountList.addAll(list);
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }


    @Override
    public void onAccountSelected(final int subaccount) {
        getModel().getActiveAccountObservable().setActiveAccount(subaccount);
        if (getConnectionManager().isLoginWithPin()) {
            cfg().edit().putInt(PrefKeys.ACTIVE_SUBACCOUNT, subaccount).apply();
        }
        finishOnUiThread();
        overridePendingTransition(0,0);
    }

    @Override
    public void onNewSubaccount() {
        startActivityForResult(new Intent(this, SubaccountAddActivity.class), REQUEST_CREATE_SUBACCOUNT);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_SUBACCOUNT) {
            getModel().getSubaccountsDataObservable().refresh();
        }
    }
}
