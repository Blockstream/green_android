package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.blockstream.libwally.Wally;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.MnemonicHelper;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SelectionFragment extends Fragment {

    private static final int WORDS_SELECTION = 4;
    private TextView hiddenWord;

    public static SelectionFragment newInstance(final List<String> words, final String word) {
        // Pass arguments to fragment
        final Bundle bundle = new Bundle();
        bundle.putStringArrayList("words", new ArrayList<String>(words));
        bundle.putString("word", word);
        final SelectionFragment fragment = new SelectionFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setHiddenWordText(final String text) {
        this.hiddenWord.setText(text);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_selection, container, false);

        // Get arguments: words and offset
        final Bundle bundle = this.getArguments();
        if (bundle == null)
            return view;
        final List<String> words = bundle.getStringArrayList("words");
        final String word = bundle.getString("word");
        final int offset = words.indexOf(word);
        final int start = Math.max(0, Math.min(21, offset - 1));

        final int[] wordsPart = {R.id.word1, R.id.word2, R.id.word3};
        for (int i = 0; i < 3; i++) {
            final TextView wordView = UI.find(view, wordsPart[i]);
            final String currentWord = words.get(i + start);
            if (currentWord.equals(word)) {
                hiddenWord = wordView;
                wordView.setText("______");
                wordView.setTextColor(getResources().getColor(R.color.green));
            }else
                wordView.setText(currentWord);
        }

        final TextView titleText = UI.find(view, R.id.titleText);
        titleText.setText(getString(R.string.id_word_d_of_d, offset + 1, 24));

        // Choose the random words to verify
        final ArrayList<String> dictionary = new ArrayList<>();
        final Random random = new Random();
        final List<String> randoms = new ArrayList<>(WORDS_SELECTION);
        MnemonicHelper.initWordList(dictionary, null);
        for (int i = 0; i < WORDS_SELECTION; i++)
            randoms.add(dictionary.get(random.nextInt(Wally.BIP39_WORDLIST_LEN)));

        if (!randoms.contains(word))
            randoms.set(random.nextInt(WORDS_SELECTION), word);

        final String correctIfDebug = BuildConfig.DEBUG ? word : "";

        // Setup words recyclerview
        final SelectionActivity activity = (SelectionActivity) getActivity();
        final SelectionViewAdapter selectionViewAdapter = new SelectionViewAdapter(getContext(), randoms, correctIfDebug, activity);
        final RecyclerView wordsRecyclerView = UI.find(view, R.id.selectionRecyclerView);
        wordsRecyclerView.setHasFixedSize(true);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        wordsRecyclerView.setAdapter(selectionViewAdapter);
        return view;
    }

    class SelectionViewAdapter extends RecyclerView.Adapter<SelectionViewAdapter.ViewHolder> {

        private final List<String> mData;
        private final LayoutInflater mInflater;
        private final View.OnClickListener mOnClickListener;
        private final String mCorrect;
        private int mSelected = -1;

        SelectionViewAdapter(final Context context, final List<String> data, final String correct,
                             final View.OnClickListener onClickListener) {
            mInflater = LayoutInflater.from(context);
            mData = data;
            mOnClickListener = onClickListener;
            mCorrect = correct;
        }

        @Override
        public SelectionViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = mInflater.inflate(R.layout.list_element_selection, parent, false);
            return new SelectionViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final SelectionViewAdapter.ViewHolder holder, final int position) {
            if (position > mData.size())
                return;
            final String current = mData.get(position);
            holder.wordButton.setText(current);
            holder.wordButton.setSelected(position == mSelected || mCorrect.equals(current));
            holder.wordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    mSelected = holder.getAdapterPosition();
                    notifyDataSetChanged();
                    if (mOnClickListener != null) {
                        mOnClickListener.onClick(view);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public List<String> getItems() {
            return mData;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public Button wordButton;

            ViewHolder(final View itemView) {
                super(itemView);
                wordButton = UI.find(itemView, R.id.wordButton);
            }
        }
    }
}
