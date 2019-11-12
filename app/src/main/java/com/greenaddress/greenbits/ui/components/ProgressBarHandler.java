package com.greenaddress.greenbits.ui.components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import androidx.constraintlayout.widget.ConstraintLayout;

public class ProgressBarHandler {
    final private ProgressBar mProgressBar;
    private TransitionDrawable mTransStartLoading;
    private TransitionDrawable mTransStopLoading;
    final private ConstraintLayout cl;
    final private TextView mLabel;
    final private static int DEFAULT_DURATION = 300;

    public ProgressBarHandler(final Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.progress_bar, null);
        mProgressBar = setup(v);
        cl = UI.find(v, R.id.constraintLayout);
        mLabel = UI.find(v, R.id.progressBarLabel);
        ((Activity)context).addContentView(v, new ViewGroup.LayoutParams(
                                               ViewGroup.LayoutParams.MATCH_PARENT,
                                               ViewGroup.LayoutParams.MATCH_PARENT));
        stop();
    }

    public void start() {
        start("");
    }

    public void start(final String label) {
        cl.setVisibility(View.VISIBLE);
        cl.getBackground().setAlpha(170);
        mProgressBar.setVisibility(View.VISIBLE);
        mTransStartLoading.startTransition(DEFAULT_DURATION);
        mLabel.setText(label);
    }

    public void stop() {
        cl.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mTransStopLoading.startTransition(DEFAULT_DURATION);
    }

    public boolean isLoading() {
        return mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE;
    }

    public void setMessage(final String label) {
        if (mProgressBar != null) {
            mLabel.setText(label);
        }
    }

    protected ProgressBar setup(final View view) {
        final ProgressBar progressBar = UI.find(view, R.id.progressBar);
        final int mBackgroundColor = view.getResources().getColor(R.color.green);
        final ColorDrawable[] color1 = {new ColorDrawable(mBackgroundColor),
                                        new ColorDrawable(Color.WHITE)};
        mTransStartLoading = new TransitionDrawable(color1);
        final ColorDrawable[] color2 = {new ColorDrawable(Color.WHITE), new
                                        ColorDrawable(mBackgroundColor)};
        mTransStopLoading = new TransitionDrawable(color2);

        // set progressbar for API < lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setBackgroundColor(Color.WHITE);
            progressBar.getIndeterminateDrawable().setColorFilter(
                mBackgroundColor, PorterDuff.Mode.SRC_IN);
        }
        progressBar.setIndeterminate(true);
        return progressBar;
    }
}
