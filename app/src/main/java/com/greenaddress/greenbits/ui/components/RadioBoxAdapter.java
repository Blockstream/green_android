package com.greenaddress.greenbits.ui.components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class RadioBoxAdapter extends RecyclerView.Adapter<RadioBoxAdapter.Item> {

    private final String[] titles;
    private final String[] subtitles;
    private int selected;

    public RadioBoxAdapter(final String[] titles, final String[] subtitles, final int selected) {
        this.titles = titles;
        this.subtitles = subtitles;
        this.selected = selected;
    }

    @Override
    public RadioBoxAdapter.Item onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                          .inflate(R.layout.list_element_csvtime, parent, false);
        return new RadioBoxAdapter.Item(view);
    }

    @Override
    public void onBindViewHolder(final Item holder, final int index) {
        final CheckedTextView cb = UI.find(holder.itemView, android.R.id.text1);
        final TextView subtitle = UI.find(holder.itemView, android.R.id.text2);
        cb.setText(titles[index]);
        cb.setChecked(index == selected);
        subtitle.setText(subtitles[index]);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                selected = index;
                notifyDataSetChanged();
            }
        });
    }

    public int getSelected() {
        return selected;
    }

    @Override
    public int getItemCount() {
        return titles == null ? 0 : titles.length;
    }

    public class Item extends RecyclerView.ViewHolder {

        public Item(final View itemView) {
            super(itemView);
        }
    }
}
