package com.greenaddress.greenbits.ui;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class CircularButton extends CardView {

    private LinearLayout mLinearLayout;

    private static float DEFAULT_RADIUS = 3;
    private static float DEFAULT_ELEVATION = 5;
    private static int DEFAULT_DURATION = 300;

    private ProgressBar mProgressBar;
    private Button mButton;
    private TransitionDrawable mTransStartLoading;
    private TransitionDrawable mTransStopLoading;

    public CircularButton(Context context) {
        super(context);
    }

    public CircularButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularButton);

        setLayoutTransition(new LayoutTransition());

        setRadius(getPx(DEFAULT_RADIUS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(getPx(DEFAULT_ELEVATION));
        }

        mLinearLayout = new LinearLayout(context);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);

        // set selectable background
        final TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                typedValue, true);
        mLinearLayout.setBackgroundResource(typedValue.resourceId);

        // create button
        mButton = new Button(context);
        mButton.setBackgroundColor(Color.TRANSPARENT);
        mButton.setClickable(false);
        final String text = typedArray.getString(R.styleable.CircularButton_text);
        mButton.setText(text);
        mButton.setTextColor(typedArray.getColor(R.styleable.CircularButton_textColor, Color.BLACK));

        // create progressbar
        mProgressBar = new ProgressBar(context);
        mProgressBar.setVisibility(View.GONE);

        // animation transaction
        final LayoutTransition layoutTransition = getLayoutTransition();
        layoutTransition.setDuration(DEFAULT_DURATION);
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        this.setOnClickListener(view -> {
            if (isClickable()) {
                startLoading();
            }
        });

        // set background color animations
        int backgroundColor = typedArray.getColor(R.styleable.CardView_cardBackgroundColor,
                Color.WHITE);
        final ColorDrawable[] color1 = {new ColorDrawable(backgroundColor),
                new ColorDrawable(Color.WHITE)};
        mTransStartLoading = new TransitionDrawable(color1);
        final ColorDrawable[] color2 = {new ColorDrawable(Color.WHITE), new
                ColorDrawable(backgroundColor)};
        mTransStopLoading = new TransitionDrawable(color2);

        mLinearLayout.addView(mButton);
        mLinearLayout.addView(mProgressBar);
        addView(mLinearLayout);
    }

    public CircularButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private float getPx(float dim) {
        final Resources resources = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dim, resources.getDisplayMetrics());
    }

    public void startLoading() {
        setClickable(false);
        mButton.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        setRadius(getPx(23));
        mLinearLayout.setBackground(mTransStartLoading);
        mTransStartLoading.startTransition(DEFAULT_DURATION);
    }

    public void stopLoading() {
        mButton.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        setRadius(getPx(DEFAULT_RADIUS));
        setClickable(true);
        mLinearLayout.setBackground(mTransStopLoading);
        mTransStopLoading.startTransition(DEFAULT_DURATION);
    }

    public void setComplete(final Boolean complete) {
        if (complete)
            stopLoading();
        else
            startLoading();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setComplete(enabled);
    }

    public boolean isLoading() {
        return !isClickable();
    }
}
