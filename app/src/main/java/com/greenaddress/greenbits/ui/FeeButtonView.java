package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FeeButtonView extends RelativeLayout {
    private TextView mTitle;
    private TextView mSummary;
    private ImageView mImage;
    private RelativeLayout mLayout;
    private boolean mNext=false;

    public FeeButtonView(final Context context) {
        super(context);
        setup(context);
    }

    public FeeButtonView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTitle.setTextColor(enabled ?
                getResources().getColor(R.color.white) :
                getResources().getColor(R.color.grey_light)
                );

    }

    private void setup(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_fee_button, this, true);

        final View view = getRootView();
        mTitle = view.findViewById(R.id.title);
        mSummary = view.findViewById(R.id.summary);
        mImage = view.findViewById(R.id.image);
        mLayout = view.findViewById(R.id.layout);
    }

    public void init(final String title, final String summary, final boolean next) {
        mTitle.setText(title);
        mSummary.setText(summary);
        mNext = next;
        mImage.setImageDrawable(getDrawable());
    }

    public void setSummary(final String summary) {
        mSummary.setText(summary);
    }


    private Drawable getDrawable() {
        return mNext ?
                getResources().getDrawable(R.drawable.ic_navigate_next_black_24dp) :
                getResources().getDrawable(android.R.color.transparent);
    }

    public void setSelected(final boolean selected) {
        mImage.setImageDrawable(selected ?
                getResources().getDrawable(R.drawable.ic_done) :
                getDrawable());
        mLayout.setBackground(selected ?
                getResources().getDrawable(R.drawable.fee_button_selected) :
                getResources().getDrawable(R.drawable.fee_button_unselected));
    }
}
