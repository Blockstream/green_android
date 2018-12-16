package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.components.PinEntryView;

public class PinFragment extends GAFragment implements View.OnClickListener, KeyboardView.OnKeyboardActionListener {

    private boolean mIsSixDigit;
    private OnPinListener mPinCallback;
    private PinEntryView mPinEntryView;
    private TextView mPinLongText;
    private CircularButton mPinButton;
    private TextView mSkipText;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        mIsSixDigit = getArguments() == null || getArguments().getBoolean("is_six_digit", true);

        // Disable android keyboard
        getGaActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final View rootView;
        if (mIsSixDigit) {
            // PIN fixed at six digits length
            rootView = inflater.inflate(R.layout.fragment_pin_six_digits, container, false);
            mPinEntryView =  UI.find(rootView, R.id.pin_entry);
            mPinEntryView.requestFocus();
            mPinEntryView.disableKeyboard();
            mPinEntryView.setOnPinEnteredListener(pin -> afterPinInserted());
            mPinEntryView.setOnTouchListener((view, motionEvent) -> true);  // prevent keyboard from popping out
        } else {
            // long pin text (supported for upgrading existing installs)
            rootView = inflater.inflate(R.layout.fragment_pin_text, container, false);
            mPinLongText = UI.find(rootView, R.id.pinText);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // API 21
                mPinLongText.setShowSoftInputOnFocus(false);
            else
                mPinLongText.setTextIsSelectable(true);
            mPinButton = UI.mapClick(rootView, R.id.pinLoginButton, this);
        }

        // SKIP
        mSkipText = UI.mapClick(rootView, R.id.skipButton, this);
        final boolean isOnBoarding = getArguments() == null ||  getArguments().getBoolean("skip_visible", false);
        UI.showIf(isOnBoarding, mSkipText, View.INVISIBLE);

        // Load custom keyboard
        final KeyboardView kv = UI.find(rootView, R.id.keyboardView);
        kv.setKeyboard(new Keyboard(getActivity(), R.xml.keyboard));
        kv.setOnKeyboardActionListener(this);
        kv.setPreviewEnabled(false);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UI.unmapClick(mPinButton);
        UI.unmapClick(mSkipText);
    }

    public void clear() {
        setEnabled(false);
        if (mIsSixDigit)
            mPinEntryView.clearText();
        else
            mPinLongText.setText("");
        setEnabled(true);
    }

    public void setEnabled(final boolean enabled) {
        UI.enableIf(enabled, mIsSixDigit ? mPinEntryView : mPinLongText);
    }

    public String getPin(){
        if (mIsSixDigit)
            return mPinEntryView.getText().toString();
        return mPinLongText.getText().toString();
    }

    private void afterPinInserted(){
        if (mPinCallback != null)
            mPinCallback.onPinInserted(getPin());
    }

    @Override
    public void onClick(final View v) {
        if (v == mPinButton)
            afterPinInserted();
        else if (v == mSkipText)
            getGaActivity().goToTabbedMainActivity();
    }

    @Override
    public void onPress(final int i) {}

    @Override
    public void onRelease(final int i) {}

    @Override
    public void onKey(final int primaryCode, final int[] keyCodes) {
        final Editable editable;
        final View view;
        if (mIsSixDigit) {
            editable = mPinEntryView.getText();
            view = mPinEntryView;
        } else {
            editable = mPinLongText.getEditableText();
            view = mPinLongText;
        }
        if (primaryCode >= 0 && primaryCode <= 9)
            editable.append(String.valueOf(primaryCode));
        else if (primaryCode == -2)
            clear();
        else if (primaryCode == -1)
            view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
    }

    @Override
    public void onText(final CharSequence charSequence) {}

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}

    public interface OnPinListener {
        void onPinInserted(final String pin);
        void onPinBackPressed();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        try {
            mPinCallback = (OnPinListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPinListener");
        }
    }
}
