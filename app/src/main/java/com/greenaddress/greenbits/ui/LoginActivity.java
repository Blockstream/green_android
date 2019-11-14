package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;

import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenapi.model.TorProgressObservable;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;

import java.util.Observable;
import java.util.Observer;

public abstract class LoginActivity extends GaActivity implements Observer {

    protected void onLoggedIn() {
        final Intent intent = new Intent(LoginActivity.this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setData(getIntent().getData());
        intent.setAction(getIntent().getAction());
        startActivity(intent);
        finishOnUiThread();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getConnectionManager() != null) {
            final ConnectionManager cm = getConnectionManager();
            cm.clearPreviousLoginError();
        }
        if (getGAApp().getTorProgressObservable() != null) {
            getGAApp().getTorProgressObservable().addObserver(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getGAApp().getTorProgressObservable() != null) {
            getGAApp().getTorProgressObservable().deleteObserver(this);
        }
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof TorProgressObservable) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int progress = ((TorProgressObservable) observable).get().get("progress").asInt(0);
                    final ProgressBarHandler pbar = getProgressBarHandler();
                    if (pbar != null)
                        pbar.setMessage(String.format("%s %d%",getString(R.string.id_tor_status),
                                                      String.valueOf(progress)));
                }
            });
        }
    }

    protected int getCode(final Exception e) {
        try {
            final String stringCode = e.getMessage().split(" ")[1];
            return Integer.parseInt(stringCode);
        } catch (final Exception ignored) {}
        return 1;
    }
}
