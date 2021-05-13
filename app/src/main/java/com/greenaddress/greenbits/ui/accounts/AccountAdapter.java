package com.greenaddress.greenbits.ui.accounts;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.blockstream.gdk.data.AccountType;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SubaccountData> mSubaccountList;
    private final OnAccountSelected mOnAccountSelected;
    private final boolean showNewButton;
    private final GaActivity mActivity;

    public interface OnAccountSelected {
        void onAccountSelected(int account);
        void onNewSubaccount();
    }

    public AccountAdapter(final GaActivity activity,
                          final List<SubaccountData> subaccountList,
                          final OnAccountSelected cb, final boolean showNewAccount) {
        mActivity = activity;
        mSubaccountList = subaccountList;
        mOnAccountSelected = cb;
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
            final long satoshi = subaccount.getSatoshi().containsKey("btc") ? subaccount.getSatoshi().get("btc") : 0;
            final long pointer = subaccount.getPointer();
            final Resources res = holder.itemView.getResources();
            final String defaultName = pointer == 0 ? res.getString(R.string.id_main_account) : res.getString(R.string.id_account) + " " + pointer;

            h.name.setText(subaccount.getNameWithDefault(defaultName));
            switch (AccountType.Companion.byGDKType(subaccount.getType())){
                case BIP84_SEGWIT:
                    h.type.setVisibility(View.VISIBLE);
                    h.type.setText(R.string.segwit);
                    break;
                case BIP44_LEGACY:
                case BIP49_SEGWIT_WRAPPED:
                    h.type.setVisibility(View.VISIBLE);
                    h.type.setText(R.string.legacy);
                    break;
                case AMP_ACCOUNT:
                    h.type.setVisibility(View.VISIBLE);
                    h.type.setText(R.string.id_amp_account);
                    break;
                case TWO_OF_THREE:
                    h.type.setVisibility(View.VISIBLE);
                    h.type.setText(R.string.id_2of3_account);
                    break;
                default:
                    h.type.setVisibility(View.GONE);
            }

            try {
                final String valueBitcoin = Conversion.getBtc(mActivity.getSession(), satoshi, false);
                final String valueFiat = Conversion.getFiat(mActivity.getSession(), satoshi, true);
                h.mainBalanceText.setText(valueBitcoin);
                h.mainBalanceUnitText.setText(" " + Conversion.getBitcoinOrLiquidUnit(mActivity.getSession()));
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
        final TextView type;
        final TextView mainBalanceText;
        final TextView mainBalanceUnitText;
        final TextView mainLocalBalanceText;

        Account(final View v) {
            super(v);
            name = UI.find(v, R.id.name);
            type = UI.find(v, R.id.type);
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
