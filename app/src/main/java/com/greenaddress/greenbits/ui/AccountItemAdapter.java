package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;

class AccountItemAdapter extends RecyclerView.Adapter<AccountItemAdapter.Item> {

    private final ArrayList<String> mNames;
    private final ArrayList<Integer> mPointers;
    private OnAccountSelected mOnAccountSelected;
    private final GaService mService;

    interface OnAccountSelected {
        void onAccountSelected(int account);
    }

    public AccountItemAdapter(final ArrayList<String> names, final ArrayList<Integer> pointers, final GaService service) {
        mNames = names;
        mPointers = pointers;
        mService = service;
    }

    public void setCallback(final OnAccountSelected cb) {
        mOnAccountSelected = cb;
    }

    @Override
    public Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_accountlistitem, parent, false);
        return new Item(view);
    }

    private void onDisplayBalance(final Item holder, final int position) {
        final Coin balance = mService.getCoinBalance(mPointers.get(position));
        UI.setCoinText(mService, holder.mUnit, holder.mBalance, balance, true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final Item holder, final int position) {
        holder.mName.setText(mNames.get(position));
        onDisplayBalance(holder, position);
        final boolean isCurrent = mPointers.get(position) == mService.getCurrentSubAccount();
        holder.mRadio.setChecked(isCurrent);
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mOnAccountSelected.onAccountSelected(position);
            }
        };
        holder.mView.setOnClickListener(listener);
        // Some Android versions do not pass the radio click to the
        // view, so we override its listener here too.
        holder.mRadio.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return mNames.size();
    }

    public static class Item extends RecyclerView.ViewHolder {

        final View mView;
        final RadioButton mRadio;
        final TextView mName;
        final TextView mUnit;
        final TextView mBalance;

        public Item(final View v) {
            super(v);
            mView = v;
            mRadio = UI.find(v, R.id.radio);
            mName = UI.find(v, R.id.name);
            mUnit = UI.find(v, R.id.mainBalanceUnit);
            mBalance = UI.find(v, R.id.mainBalanceText);
        }
    }
}
