package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.List;

public class WordsFragment extends Fragment {

    public static WordsFragment newInstance(final List<String> words, final int offset) {
        // Pass arguments to fragment
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("words", new ArrayList<String>(words));
        bundle.putInt("offset", offset);
        final WordsFragment fragment = new WordsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_words, container, false);

        // Get arguments: words and offset
        final Bundle b = this.getArguments();
        if (b == null)
            return view;
        final List<String> words = b.getStringArrayList("words");
        final int offset = b.getInt("offset");
        final int index = offset / 6;

        final ProgressBar progressBar = UI.find(view, R.id.progressBar);
        progressBar.setProgress(25+index*25);

        // Setup words recyclerview
        final WordsViewAdapter wordsViewAdapter = new WordsViewAdapter(getContext(), words, offset);
        final RecyclerView wordsRecyclerView = UI.find(view, R.id.wordsRecyclerView);
        wordsRecyclerView.setHasFixedSize(true);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        wordsRecyclerView.setAdapter(wordsViewAdapter);
        return view;
    }

    class WordsViewAdapter extends RecyclerView.Adapter<WordsViewAdapter.TextViewHolder> {

        private List<String> mData;
        private int mOffset;
        private LayoutInflater mInflater;

        WordsViewAdapter(final Context context, final List<String> data, final int offset) {
            mInflater = LayoutInflater.from(context);
            mData = data;
            mOffset = offset;
        }

        @Override
        public WordsViewAdapter.TextViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = mInflater.inflate(R.layout.list_element_word, parent, false);
            return new WordsViewAdapter.TextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final WordsViewAdapter.TextViewHolder holder, final int position) {
            if (position > mData.size())
                return;
            holder.numericText.setText(String.valueOf(position + mOffset + 1));
            holder.wordText.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public List<String> getItems() {
            return mData;
        }

        public class TextViewHolder extends RecyclerView.ViewHolder {
            public TextView wordText;
            public TextView numericText;

            TextViewHolder(final View itemView) {
                super(itemView);
                wordText = UI.find(itemView, R.id.wordText);
                numericText = UI.find(itemView, R.id.numericText);
            }
        }
    }
}