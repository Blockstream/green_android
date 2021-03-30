package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class DisplayMnemonicActivity extends LoggedActivity {

    public static final int MNEMONIC_LENGTH = 24;

    private final TextView[] mTextViews = new TextView[MNEMONIC_LENGTH];
    private final TextView[] mTextViewsCount = new TextView[MNEMONIC_LENGTH];

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_mnemonic);
        setTitleBackTransparent();
        UI.preventScreenshots(this);

        setUpTable(R.id.mnemonic24, 1);

        setMnemonic(getSession().getMnemonicPassphrase());
    }

    private void setMnemonic(final String mnemonic) {
        final String cleanMnemonic = mnemonic.trim().replaceAll("(\\r|\\n)", "").toLowerCase();

        final String[] words = cleanMnemonic.split(" ");
        for (int i = 0; i < words.length; ++i) {
            mTextViews[i].setText(words[i]);
            mTextViewsCount[i].setVisibility(View.VISIBLE);
        }
    }

    private void setUpTable(final int id, final int startWordNum) {
        int wordNum = startWordNum;
        final TableLayout table = UI.find(this, id);

        for (int y = 0; y < table.getChildCount(); ++y) {
            final TableRow row = (TableRow) table.getChildAt(y);

            for (int x = 0; x < row.getChildCount() / 2; ++x) {
                TextView counter = ((TextView) row.getChildAt(x * 2));
                counter.setText(String.valueOf(wordNum));
                counter.setVisibility(View.GONE);


                TextView me = (TextView) row.getChildAt(x * 2 + 1);
                me.setInputType(0);
                me.addTextChangedListener(new UI.TextWatcher() {
                    @Override
                    public void afterTextChanged(final Editable s) {
                        super.afterTextChanged(s);
                        final String original = s.toString();
                        final String trimmed = original.trim();
                        if (!trimmed.isEmpty() && !trimmed.equals(original)) {
                            me.setText(trimmed);
                        }
                    }
                });
                registerForContextMenu(me);

                mTextViews[wordNum - 1] = me;
                mTextViewsCount[wordNum - 1] = counter;
                ++wordNum;
            }
        }
    }

}
