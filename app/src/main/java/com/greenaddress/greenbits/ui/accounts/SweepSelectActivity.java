package com.greenaddress.greenbits.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.send.ScanActivity;

import java.util.ArrayList;
import java.util.List;

import static com.greenaddress.greenapi.Session.getSession;

public class SweepSelectActivity extends LoggedActivity implements SweepAdapter.OnAccountSelected {

    private RecyclerView mRecyclerView;
    private final List<SubaccountData> mSubaccounts = new ArrayList<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweep_selection);
        setTitleBackTransparent();

        mRecyclerView = findViewById(R.id.accountsList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new SweepAdapter(mSubaccounts, this));

        refresh();
    }

    private void refresh() {
        startLoading();
        Observable.just(getSession())
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .map((session) -> {
            return getSession().getSubAccounts(this);
        }).subscribe((subaccounts) -> {
            stopLoading();
            mSubaccounts.clear();
            mSubaccounts.addAll(subaccounts);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }, (final Throwable e) -> {
            stopLoading();
        });
    }

    @Override
    public void onAccountSelected(final int account) {
        final Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(PrefKeys.SWEEP, true);
        setActiveAccount(mSubaccounts.get(account).getPointer());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
