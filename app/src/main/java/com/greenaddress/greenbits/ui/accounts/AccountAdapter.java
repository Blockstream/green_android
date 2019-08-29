package com.greenaddress.greenbits.ui.accounts;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Item> {

    private final List<SubaccountData> mSubaccountList;
    private final OnAccountSelected mOnAccountSelected;
    private final GaService mService;
    private final Resources mResources;
    private final Activity mActivity;

    public interface OnAccountSelected {
        void onAccountSelected(int account);
        void onNewSubaccount();
    }

    public AccountAdapter(final List<SubaccountData> subaccountList, final GaService service,
                          final OnAccountSelected cb, final Resources resources, final Activity activity) {
        mSubaccountList = subaccountList;
        mService = service;
        mOnAccountSelected = cb;
        mResources = resources;
        mActivity = activity;
    }

    @Override
    public Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.list_element_account, parent, false);
        return new Item(view);
    }

    @Override
    public void onBindViewHolder(final Item holder, final int position) {
        Log.d(this.getClass().getName(),
              "Update position " + position + " of " + mSubaccountList.size());
        holder.mAccountView.hideActions();
        if (position < mSubaccountList.size()) {
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
            final BalanceData balance = mService.getModel().getBalanceDataObservable(pointer).getBtcBalanceData();

            // Setup subaccount info
            holder.mAccountView.setTitle(subaccount.getNameWithDefault(mResources.getString(R.string.id_main_account)));
            if (balance != null && mService.getModel().getSettings() != null) {
                holder.mAccountView.setBalance(mService, balance);
            }
            holder.mAccountView.listMode(false);
            holder.mAccountView.setOnClickListener(listener);
            holder.mAccountView.showAdd(false);
        } else {
            holder.mAccountView.showAdd(true);
            holder.mAccountView.setOnClickListener( view -> {
                mOnAccountSelected.onNewSubaccount();
            });
        }
    }

    @Override
    public int getItemCount() {
        return mSubaccountList.size() + (mService.isWatchOnly() ? 0 : 1);
    }

    static class Item extends RecyclerView.ViewHolder {

        final AccountView mAccountView;

        Item(final View v) {
            super(v);
            mAccountView = new AccountView(v.getContext());
            mAccountView.setView(v);
        }
    }
}
