package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.EventData;

public class MessagesActivity extends GaActivity
    implements View.OnClickListener {

    private static final String TAG = MessagesActivity.class.getSimpleName();
    private TextView mMessageText;
    private CheckBox mAckedCheckBox;
    private Button mContinueButton;
    private Button mSkipButton;
    private EventData mCurrentEvent;
    private String mCurrentMessage;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_messages);

        mMessageText = UI.find(this, R.id.system_message_text);
        mAckedCheckBox = UI.find(this, R.id.system_message_acked);
        mContinueButton = UI.find(this, R.id.system_message_continue);
        mSkipButton = UI.find(this, R.id.system_message_skip);

        mMessageText.setMovementMethod(LinkMovementMethod.getInstance());
        UI.mapClick(this, R.id.system_message_continue, this);
        UI.mapClick(this, R.id.system_message_skip, this);

        mCurrentEvent = (EventData) getIntent().getSerializableExtra("event");
        mCurrentMessage = getIntent().getStringExtra("message");

        mMessageText.setText(mCurrentMessage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mContinueButton);
        UI.unmapClick(mSkipButton);
        mAckedCheckBox.setOnCheckedChangeListener(null);
    }

    @Override
    public void onClick(final View v) {
        if (v == mSkipButton)
            finishOnUiThread();
        else if (v == mContinueButton) {
            if (mAckedCheckBox.isChecked()) {
                // Sign and ack the current message, then move to the next
                startLoading();
                mService.getExecutor().execute(() -> {
                    try {
                        final ConnectionManager cm = mService.getConnectionManager();
                        final GDKTwoFactorCall call = mService.getSession().ackSystemMessage(this, mCurrentMessage);
                        call.resolve(null, cm.getHWResolver());
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    //FIXME put this inside the try block when testing with real system messages
                    mService.getModel().getEventDataObservable().remove(mCurrentEvent);
                    finishOnUiThread();
                });
            } else {
                UI.toast(this, R.string.id_please_select_the_checkbox, Toast.LENGTH_LONG);
            }
        }
    }

}
