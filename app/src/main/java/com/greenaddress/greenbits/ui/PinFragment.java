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

    private boolean isSixDigit;
    private OnPinListener mPinCallback;
    private PinEntryView mPinEntryView;
    private TextView mPinLongText;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        isSixDigit = getArguments() == null || getArguments().getBoolean("is_six_digit", true);

        // Disable android keyboard
        getGaActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final View rootView;
        if (isSixDigit()) {
            // pin fixed of six digits
            rootView = inflater.inflate(R.layout.fragment_pin_six_digits, container, false);
            mPinEntryView =  UI.find(rootView, R.id.pin_entry);
            mPinEntryView.requestFocus();
            mPinEntryView.disableKeyboard();
            mPinEntryView.setOnPinEnteredListener(pin -> afterPinInserted());
            mPinEntryView.setOnTouchListener((view, motionEvent) -> true);  // prevent the keyboard from popping out

        } else {
            // long pin text
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // API 21
                mPinLongText.setShowSoftInputOnFocus(false);
            else // API 11-20
                mPinLongText.setTextIsSelectable(true);
            rootView = inflater.inflate(R.layout.fragment_pin_text, container, false);
            mPinLongText = UI.find(rootView, R.id.pinText);
            final CircularButton mPinButton = UI.find(rootView, R.id.pinLoginButton);
            mPinButton.setOnClickListener(this);
        }

        // SKIP
        final TextView skipButton = rootView.findViewById(R.id.skipButton);
        boolean isOnBoarding = getArguments() == null ||  getArguments().getBoolean("skip_visible", false);
        skipButton.setVisibility(isOnBoarding ? View.VISIBLE : View.INVISIBLE);
        skipButton.setOnClickListener(view -> getGaActivity().goToTabbedMainActivity());

        // Load custom keyboard
        final KeyboardView mKeyboardView = UI.find(rootView, R.id.keyboardView);
        mKeyboardView.setKeyboard(new Keyboard(getActivity(), R.xml.keyboard));
        mKeyboardView.setOnKeyboardActionListener(this);
        mKeyboardView.setPreviewEnabled(false);
        return rootView;
    }

    protected boolean isSixDigit(){
        return isSixDigit;
    }

    public void clear() {
        setEnabled(false);
        if (isSixDigit())
            mPinEntryView.clearText();
        else
            mPinLongText.setText("");
        setEnabled(true);
    }

    public void setEnabled(final boolean enabled) {
        if (isSixDigit())
            mPinEntryView.setEnabled(enabled);
        else
            mPinLongText.setEnabled(enabled);
    }

    public String getPin(){
        if (isSixDigit())
            return mPinEntryView.getText().toString();
        else
            return mPinLongText.getText().toString();
    }

    private void afterPinInserted(){
        if (mPinCallback == null)
            return;
        mPinCallback.onPinInserted(getPin());
    }

    @Override
    public void onClick(final View v) {
        afterPinInserted();
    }

    @Override
    public void onPress(final int i) {}

    @Override
    public void onRelease(final int i) {}

    @Override
    public void onKey(final int primaryCode, final int[] keyCodes) {
        final Editable editable;
        final View view;
        if (isSixDigit()) {
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

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mPinCallback = (OnPinListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement OnHeadlineSelectedListener");
        }
    }
}
