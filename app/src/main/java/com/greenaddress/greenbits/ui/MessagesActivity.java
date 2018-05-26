package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import java.util.concurrent.Callable;
import android.util.Log;

public class MessagesActivity extends GaActivity
    implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private TextView mMessageText;
    private CheckBox mAckedCheckBox;
    private Button mContinueButton;
    private MaterialDialog mWaitDialog;
    private final Runnable mDialogCB = new Runnable() { public void run() { mWaitDialog = null; } };

    private String mCurrentMessage;
    private Integer mCurrentMessageId;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_messages);

        mMessageText = UI.find(this, R.id.system_message_text);
        mAckedCheckBox = UI.find(this, R.id.system_message_acked);
        mContinueButton = UI.find(this, R.id.system_message_continue);

        mMessageText.setMovementMethod(LinkMovementMethod.getInstance());
        mAckedCheckBox.setOnCheckedChangeListener(this);
        UI.mapClick(this, R.id.system_message_continue, this);

        processNextMessage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mContinueButton);
        mAckedCheckBox.setOnCheckedChangeListener(null);
    }

    private void processNextMessage() {
        if (mService == null || !mService.isLoggedIn()) {
            toast(R.string.err_send_not_connected_will_resume);
            // FXIME: add a callback to check messages when logged in
            return;
        }

        if (mService.getNextSystemMessageId() != 0) {
            setTitle(R.string.important_message);
            mAckedCheckBox.setChecked(false);
            UI.disable(mAckedCheckBox, mContinueButton);
            // Fetch the next message from the server and show it
            mService.getExecutor().submit(new Callable<Void>() {
                @Override
                public Void call() {
                    final Pair<String, Integer> messageInfo = mService.getNextSystemMessage();
                    if (messageInfo == null) {
                        toast(R.string.err_send_not_connected_will_resume);
                        return null;
                    }
                    mCurrentMessage = messageInfo.first;
                    mCurrentMessageId = messageInfo.second;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mMessageText.setText(mCurrentMessage);
                            UI.enable(mAckedCheckBox);
                        }
                    });
                    return null;
                }
            });
            return;
        }

        final Intent intent = new Intent(MessagesActivity.this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishOnUiThread();
    }

    @Override
    public void onClick(final View v) {
        if (v == mContinueButton) {
            // Sign and ack the current message, then move to the next
            mService.getExecutor().submit(new Callable<Void>() {
                @Override
                public Void call() {
                    if (mService.isHardwareWallet()) {
                        runOnUiThread(new Runnable() { public void run() {
                            final int id = mService.haveUnattendedSigning() ? R.string.signing_ack : R.string.sign_ack_hw;
                            mWaitDialog = UI.popupWait(MessagesActivity.this, id);
                            UI.setDialogCloseHandler(mWaitDialog, mDialogCB);
                        }});
                    }
                    final boolean ok = mService.signAndAckSystemMessage(mCurrentMessageId, mCurrentMessage);
                    runOnUiThread(new Runnable() { public void run() {
                        mWaitDialog = UI.dismiss(MessagesActivity.this, mWaitDialog);
                        if (ok)
                            processNextMessage();
                        else
                            toast(R.string.err_send_not_connected_will_resume);
                    }});
                    return null;
                }
            });
        }
    }

    @Override
    public void onCheckedChanged(final CompoundButton b, final boolean isChecked) {
        if (b == mAckedCheckBox)
            UI.enableIf(isChecked, mContinueButton);
    }
}
