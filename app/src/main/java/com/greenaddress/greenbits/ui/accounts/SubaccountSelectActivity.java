package com.greenaddress.greenbits.ui.accounts;

import android.os.Bundle;
import android.util.SparseArray;
import android.widget.TextView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.ThemeUtils;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class SubaccountSelectActivity extends LoggedActivity implements Observer {
    private TextView mTotalAmountBtc;
    private TextView mTotalAmountFiat;

    @Override
    protected int getMainViewId() { return R.layout.activity_subaccount_select; }

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }

        UI.preventScreenshots(this);

        SubaccountSelectFragment fragment = new SubaccountSelectFragment();
        getSupportFragmentManager().beginTransaction()
        .add(R.id.fragment_container, fragment).commit();
        mTotalAmountBtc = findViewById(R.id.total_amount_btc);
        mTotalAmountFiat = findViewById(R.id.total_amount_fiat);
    }

    private void initTotalAmount() {
        final SparseArray<BalanceDataObservable> balanceObservables = mService.getModel().getBalanceDataObservables();
        long totalSatoshi = 0L;
        for (int i = 0; i < balanceObservables.size(); i++) {
            final long satoshi = balanceObservables.valueAt(i).getBtcBalanceData();
            totalSatoshi += satoshi;
        }
        final BalanceData balanceReq = new BalanceData();
        balanceReq.setSatoshi(totalSatoshi);
        try {
            final BalanceData total = getSession().convertBalance(balanceReq);
            final String btcString = mService.getValueString(total.toObjectNode(), false, true);
            final String fiatString = mService.getValueString(total.toObjectNode(), true, true);
            mTotalAmountBtc.setTextColor(ThemeUtils.resolveColorAccent(this));
            mTotalAmountBtc.setText(btcString);
            mTotalAmountFiat.setText(" ≈ " + fiatString);
        } catch (final RuntimeException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null)
            return;
        if (mService.isDisconnected()) {
            return;
        }
        initTotalAmount();
        final SparseArray<BalanceDataObservable> balanceObservables = mService.getModel().getBalanceDataObservables();
        for (int i = 0; i < balanceObservables.size(); i++) {
            balanceObservables.valueAt(i).addObserver(this);
        }
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null || mService.getModel() == null)
            return;
        final SparseArray<BalanceDataObservable> balanceObservables = mService.getModel().getBalanceDataObservables();
        for (int i = 0; i < balanceObservables.size(); i++) {
            balanceObservables.valueAt(i).deleteObserver(this);
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        super.update(observable, o);
        if (observable instanceof BalanceDataObservable) {
            initTotalAmount();
        }
    }
}
