package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;

public class WatchOnlyLoginActivity extends LoginActivity implements View.OnClickListener {

    private final static String CFG = "WATCH_ONLY_CREDENTIALS";

    private EditText mUsernameText;
    private EditText mPasswordText;
    private CircularProgressButton mLoginButton;
    private CheckBox mRememberCheckBox;


    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_watchonly);

        mUsernameText = UI.find(this, R.id.input_user);
        mPasswordText = UI.find(this, R.id.input_password);
        mLoginButton = UI.find(this, R.id.btn_login);
        mRememberCheckBox = UI.find(this, R.id.remember_watch_only);

        mLoginButton.setIndeterminateProgressMode(true);
        mLoginButton.setOnClickListener(this);

        final TextView.OnEditorActionListener listener;
        listener = UI.getListenerRunOnEnter(new Runnable() { public void run() { onLoginButtonClicked(); } });
        mPasswordText.setOnEditorActionListener(listener);

        final String username = mService.cfg(CFG).getString("username", "");
        final boolean haveUser = !username.isEmpty();
        mUsernameText.setText(username);
        mRememberCheckBox.setChecked(haveUser);
        if (haveUser && mPasswordText.requestFocus())
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        mRememberCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                if (!isChecked) {
                    mService.cfgEdit(CFG).putString("username", "").apply();
                    return;
                }

                UI.popup(WatchOnlyLoginActivity.this, R.string.remember_warn_title)
                        .content(R.string.remember_warn_content)
                        .canceledOnTouchOutside(false)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                mRememberCheckBox.setChecked(false);
                            }
                        })
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dlg, final DialogAction which) {
                                mService.cfgEdit(CFG).putString("username", UI.getText(mUsernameText)).apply();
                            }
                        }).build().show();
            }
        });
    }

    @Override
    public void onClick(final View v) {
        if (v == mLoginButton)
            onLoginButtonClicked();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mLoginButton);
    }

    private void onLoginButtonClicked() {
        if (mLoginButton.getProgress() != 0)
            return;

        if (!validate()) {
            onLoginFailed(null);
            return;
        }

        if (!mService.isConnected()) {
            toast(R.string.err_send_not_connected_will_resume);
            return;
        }

        final String username = UI.getText(mUsernameText);
        final String password = UI.getText(mPasswordText);

        onLoginBegin();

        final ListenableFuture<LoginData> future = mService.watchOnlyLogin(username, password);

        Futures.addCallback(future, new FutureCallback<LoginData>() {

            @Override
            public void onSuccess(final LoginData result) {
                onLoginSuccess();
            }

            @Override
            public void onFailure(final Throwable t) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        onLoginFailed(getString(R.string.error_username_not_found_or_wrong_password));
                    }
                });
            }
        });
    }

    private void onLoginBegin() {
        mLoginButton.setProgress(50);
        mUsernameText.setEnabled(false);
        mPasswordText.setEnabled(false);
        mRememberCheckBox.setEnabled(false);
        final String usr = !mRememberCheckBox.isChecked() ? "" : UI.getText(mUsernameText);
        mService.cfgEdit(CFG).putString("username", usr).apply();
    }

    private void onLoginFailed(final String msg) {
        mLoginButton.setProgress(0);
        mUsernameText.setEnabled(true);
        mPasswordText.setEnabled(true);
        mRememberCheckBox.setEnabled(true);
        if (msg != null) mPasswordText.setError(msg);
    }

    private boolean validate() {

        if (mUsernameText.getText().length() == 0) {
            mUsernameText.setError(getString(R.string.enter_valid_username));
            return false;
        }

        mUsernameText.setError(null);

        if (mPasswordText.getText().length() == 0) {
            mPasswordText.setError(getString(R.string.enter_valid_password));
            return false;
        }

        mPasswordText.setError(null);

        return true;
    }
}
