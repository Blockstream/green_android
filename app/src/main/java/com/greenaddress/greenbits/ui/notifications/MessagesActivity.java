package com.greenaddress.greenbits.ui.notifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.twofactor.PopupCodeResolver;
import com.greenaddress.greenbits.ui.twofactor.PopupMethodResolver;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Session.getSession;

public class MessagesActivity extends LoggedActivity
    implements View.OnClickListener {

    private static final String TAG = MessagesActivity.class.getSimpleName();
    private TextView mMessageText;
    private CheckBox mAckedCheckBox;
    private Button mContinueButton;
    private Button mSkipButton;
    private String mCurrentMessage;
    private Disposable mDisposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        mMessageText = UI.find(this, R.id.system_message_text);
        mAckedCheckBox = UI.find(this, R.id.system_message_acked);
        mContinueButton = UI.find(this, R.id.system_message_continue);
        mSkipButton = UI.find(this, R.id.system_message_skip);

        mMessageText.setMovementMethod(LinkMovementMethod.getInstance());
        UI.mapClick(this, R.id.system_message_continue, this);
        UI.mapClick(this, R.id.system_message_skip, this);

        mCurrentMessage = getIntent().getStringExtra("message");

        mMessageText.setText(mCurrentMessage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mContinueButton);
        UI.unmapClick(mSkipButton);
        mAckedCheckBox.setOnCheckedChangeListener(null);
        if (mDisposable != null)
            mDisposable.dispose();
    }

    @Override
    public void onClick(final View v) {
        if (v == mSkipButton)
            finishOnUiThread();
        else if (v == mContinueButton) {
            if (mAckedCheckBox.isChecked()) {
                // Sign and ack the current message, then move to the next
                startLoading();

                mDisposable = Observable.just(getSession())
                              .subscribeOn(Schedulers.computation())
                              .map((session) -> {
                    final GDKTwoFactorCall call = getSession().ackSystemMessage(mCurrentMessage);
                    call.resolve(null, new HardwareCodeResolver(this));
                    return session;
                }).observeOn(AndroidSchedulers.mainThread())
                              .subscribe((session) -> {
                    stopLoading();
                    finishOnUiThread();
                }, (final Throwable e) -> {
                    stopLoading();
                    UI.toast(this, e.getMessage(), Toast.LENGTH_LONG);
                });
            } else {
                UI.toast(this, R.string.id_please_select_the_checkbox, Toast.LENGTH_LONG);
            }
        }
    }

}
