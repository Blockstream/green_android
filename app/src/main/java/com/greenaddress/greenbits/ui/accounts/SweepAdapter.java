package com.greenaddress.greenbits.ui.accounts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.R;

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
        holder.mSubaccountTextView.setText(mSubaccountList.get(position).getName().equals("") ?
                                           holder.itemView.getContext().getString(R.string.id_main_account)
                                           : mSubaccountList.get(position).getName());

    }

    @Override
    public int getItemCount() {
        return mSubaccountList == null ? 0 : mSubaccountList.size();
    }

    public class Item extends RecyclerView.ViewHolder {

        final TextView mSubaccountTextView;

        public Item(final View itemView) {
            super(itemView);
            mSubaccountTextView = itemView.findViewById(R.id.subaccount_title);
        }
    }
}
