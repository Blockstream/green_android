package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.GaService;

public class WatchOnlyLoginActivity extends GaActivity {

    private static final int REQUEST_SIGNUP = 0;
    private EditText mUsernameText;
    private EditText mPasswordText;
    private CircularProgressButton mLoginButton;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_watchonly);

        mUsernameText = UI.find(this, R.id.input_user);
        mPasswordText = UI.find(this, R.id.input_password);
        mLoginButton = UI.find(this, R.id.btn_login);

        mLoginButton.setIndeterminateProgressMode(true);
        mLoginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                login();
            }
        });

        mPasswordText.setOnEditorActionListener(
                UI.getListenerRunOnEnter(new Runnable() {
                    @Override
                    public void run() {
                        login();
                    }
                })
        );
    }

    private void login() {
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
                startActivity(new Intent(WatchOnlyLoginActivity.this, TabbedMainActivity.class));
                finishOnUiThread();
            }

            @Override
            public void onFailure(final Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onLoginFailed(getString(R.string.error_username_not_found_or_wrong_password));
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                finish();
            }
        }
    }

    @Override
    public void onResumeWithService() {

        final GaService service = mService;
        if (service.isLoggedOrLoggingIn()) {
            // already logged in, could be from different app via intent
            startActivity(new Intent(this, TabbedMainActivity.class));
            finish();
        }
    }

    private void onLoginBegin() {
        mLoginButton.setProgress(50);
        mUsernameText.setEnabled(false);
        mPasswordText.setEnabled(false);
    }

    private void onLoginFailed(final String msg) {
        mLoginButton.setProgress(0);
        mUsernameText.setEnabled(true);
        mPasswordText.setEnabled(true);
        if (msg != null) mPasswordText.setError(msg);
    }

    private boolean validate() {

        if (mUsernameText.getText().length() == 0) {
            mUsernameText.setError(getString(R.string.enter_valid_username));
            return false;
        } else {
            mUsernameText.setError(null);
        }

        if (mPasswordText.getText().length() == 0) {
            mPasswordText.setError(getString(R.string.enter_valid_password));
            return false;
        } else {
            mPasswordText.setError(null);
        }

        return true;
    }
}
