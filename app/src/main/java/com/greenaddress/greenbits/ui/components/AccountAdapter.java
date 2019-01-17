package com.greenaddress.greenbits.ui.components;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Item> {

    private final ArrayList<SubaccountData> mSubaccountList;
    private final OnAccountSelected mOnAccountSelected;
    private final GaService mService;
    private final Resources mResources;

    public interface OnAccountSelected {
        void onAccountSelected(int account);
    }

    public AccountAdapter(final ArrayList<SubaccountData> subaccountList, final GaService service,
                          final OnAccountSelected cb, final Resources resources) {
        mSubaccountList = subaccountList;
        mService = service;
        mOnAccountSelected = cb;
        mResources = resources;
    }

    @Override
    public Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.list_element_account, parent, false);
        return new Item(view);
    }

    @Override
    public void onBindViewHolder(final Item holder, final int position) {
        Log.d(this.getClass().getName(),"Update position " + String.valueOf(position));

        // Set click listener
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mOnAccountSelected != null)
                    mOnAccountSelected.onAccountSelected(holder.getAdapterPosition());
            }
        };

        // Get subaccount info
        final int pointer = mSubaccountList.get(position).getPointer();
        final SubaccountData subaccount = mSubaccountList.get(position);
        final BalanceData balance = mService.getModel().getBalanceDataObservable(pointer).getBalanceData();

        // Setup subaccount info
        holder.mAccountView.setTitle(subaccount.getName());
        if (balance != null && mService.getModel().getSettings() != null) {
            holder.mAccountView.setBalance(mService, balance);
        }
        holder.mAccountView.hideActions();
        holder.mAccountView.listMode(false);
        holder.mAccountView.setIcon(mResources.getDrawable(mService.getNetwork().getIcon()));
        holder.mAccountView.setOnClickListener(listener);
        holder.mView.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return mSubaccountList.size();
    }

    public static class Item extends RecyclerView.ViewHolder {

        final View mView;
        final AccountView mAccountView;

        public Item(final View v) {
            super(v);
            mView = v;
            mAccountView = new AccountView(mView.getContext());
            mAccountView.setView(v);
        }
    }
}
