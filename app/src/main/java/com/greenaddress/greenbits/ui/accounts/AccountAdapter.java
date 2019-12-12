package com.greenaddress.greenbits.ui.accounts;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SubaccountData> mSubaccountList;
    private final OnAccountSelected mOnAccountSelected;
    private final Model mModel;
    private final boolean showNewButton;

    public interface OnAccountSelected {
        void onAccountSelected(int account);
        void onNewSubaccount();
    }

    public AccountAdapter(final List<SubaccountData> subaccountList,
                          final OnAccountSelected cb, final boolean showNewAccount,
                          final Model model) {
        mSubaccountList = subaccountList;
        mOnAccountSelected = cb;
        mModel = model;
        showNewButton = showNewAccount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(viewType, parent, false);
        if (viewType == R.layout.list_element_account) {
            return new Account(view);
        }
        if (viewType == R.layout.list_element_addaccount) {
            return new AddAccount(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder.getItemViewType() == R.layout.list_element_account) {
            final Account h = (Account) holder;
            final SubaccountData subaccount = mSubaccountList.get(position);
            final long satoshi = subaccount.getSatoshi().get("btc");
            try {
                final String valueBitcoin = mModel.getBtc(satoshi, false);
                final String valueFiat = mModel.getFiat(satoshi, true);
                h.name.setText(subaccount.getNameWithDefault(holder.itemView.getResources().getString(R.string.
                                                                                                      id_main_account)));
                h.mainBalanceText.setText(valueBitcoin);
                h.mainBalanceUnitText.setText(" " + mModel.getBitcoinOrLiquidUnit());
                h.mainLocalBalanceText.setText("≈  " + valueFiat);
            } catch (final Exception e) {
                Log.e("", "Conversion error: " + e.getLocalizedMessage());
            }
            h.itemView.setOnClickListener(view -> {
                mOnAccountSelected.onAccountSelected(subaccount.getPointer());
            });
        }
        if (holder.getItemViewType() == R.layout.list_element_addaccount) {
            final AddAccount h = (AddAccount) holder;
            h.itemView.setOnClickListener(view -> {
                mOnAccountSelected.onNewSubaccount();
            });
        }
    }

    @Override
    public int getItemCount() {
        return mSubaccountList.size() + (showNewButton ? 1 : 0);
    }

    @Override
    public int getItemViewType(final int position) {
        return position == getItemCount() - 1 &&
               showNewButton ? R.layout.list_element_addaccount : R.layout.list_element_account;
    }

    static class Account extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView mainBalanceText;
        final TextView mainBalanceUnitText;
        final TextView mainLocalBalanceText;

        Account(final View v) {
            super(v);
            name = UI.find(v, R.id.name);
            mainBalanceText = UI.find(v, R.id.mainBalanceText);
            mainBalanceUnitText = UI.find(v, R.id.mainBalanceUnitText);
            mainLocalBalanceText = UI.find(v, R.id.mainLocalBalanceText);
        }
    }

    static class AddAccount extends RecyclerView.ViewHolder {
        AddAccount(final View v) {
            super(v);
        }
    }
}
