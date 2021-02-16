package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class SweepAdapter extends RecyclerView.Adapter<SweepAdapter.Item> {
    private final List<SubaccountData> mSubaccountList;
    private final OnAccountSelected mOnAccountSelected;

    public SweepAdapter(final List<SubaccountData> subaccountList, final OnAccountSelected onAccountSelected) {
        mSubaccountList = subaccountList;
        mOnAccountSelected = onAccountSelected;
    }

    public interface OnAccountSelected {
        void onAccountSelected(final int account);
    }

    @Override
    public SweepAdapter.Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.list_element_sweep, parent, false);
        return new SweepAdapter.Item(view);
    }

    @Override
    public void onBindViewHolder(final Item holder, final int position) {
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnAccountSelected.onAccountSelected(position);
            }
        };

        holder.itemView.setOnClickListener(listener);

        final SubaccountData subaccount = mSubaccountList.get(position);
        final long pointer = subaccount.getPointer();
        final Context ctx = holder.itemView.getContext();
        final String defaultName = pointer == 0 ? ctx.getString(R.string.id_main_account) : ctx.getString(R.string.id_account) + " " + pointer;
        holder.mSubaccountTextView.setText(subaccount.getNameWithDefault(defaultName));
    }

    @Override
    public int getItemCount() {
        return mSubaccountList == null ? 0 : mSubaccountList.size();
    }

    public class Item extends RecyclerView.ViewHolder {

        final TextView mSubaccountTextView;

        public Item(final View itemView) {
            super(itemView);
            mSubaccountTextView = UI.find(itemView, R.id.subaccount_title);
        }
    }
}
