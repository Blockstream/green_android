package com.greenaddress.greenbits.ui.components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.greenaddress.greenbits.ui.R;

public class ProgressBarHandler {
    final private ProgressBar mProgressBar;
    private TransitionDrawable mTransStartLoading;
    private TransitionDrawable mTransStopLoading;
    final private FrameLayout fl;
    final private static int DEFAULT_DURATION = 300;

    public ProgressBarHandler(final Context context) {
        mProgressBar = setup(context);

        fl = new FrameLayout(context);
        fl.setClickable(true);
        fl.setFocusable(true);
        final RelativeLayout rl = new RelativeLayout(context);
        rl.setLayoutParams(new ViewGroup.LayoutParams(
                               ViewGroup.LayoutParams.MATCH_PARENT,
                               ViewGroup.LayoutParams.MATCH_PARENT));
        fl.addView(rl);
        rl.setGravity(Gravity.CENTER);
        rl.addView(mProgressBar);
        ((Activity)context).addContentView(fl, new ViewGroup.LayoutParams(
                                               ViewGroup.LayoutParams.MATCH_PARENT,
                                               ViewGroup.LayoutParams.MATCH_PARENT));
        stop();
    }

    public void start() {
        fl.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mTransStartLoading.startTransition(DEFAULT_DURATION);
    }

    public void stop() {
        fl.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mTransStopLoading.startTransition(DEFAULT_DURATION);
    }

    public boolean isLoading() {
        return mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE;
    }

    protected ProgressBar setup(final Context context) {
        final ProgressBar progressBar = new ProgressBar(context);
        final int mBackgroundColor = context.getResources().getColor(R.color.green);
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
