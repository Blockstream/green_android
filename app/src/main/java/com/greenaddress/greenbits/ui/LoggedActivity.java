package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.snackbar.Snackbar;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.model.ConnectionMessageObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.ToastObservable;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import static com.greenaddress.gdk.GDKSession.getSession;

public abstract class LoggedActivity extends GaActivity implements Observer {

    private Timer mTimer = new Timer();
    private long mStart = System.currentTimeMillis();
    private Snackbar mSnackbar;
    private Timer mOfflineTimer = new Timer();
    private Long mTryingAt = 0L;

    @Override
    public void onResume() {
        super.onResume();

        final boolean timerExpired = mStart + delayLogoutTimer() < System.currentTimeMillis();
        if (timerExpired || modelIsNullOrDisconnected()) {
            exit();
            return;
        }

        getConnectionManager().addObserver(this);
        getModel().getToastObservable().addObserver(this);
        getModel().getConnMsgObservable().addObserver(this);

        startLogoutTimer();
        update(getModel().getConnMsgObservable());

        // try to reconnect if offline
        if (getConnectionManager().isOffline()) {
            getGAApp().getExecutor().submit(() -> {
                try {
                    getSession().reconnectNow();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (modelIsNullOrDisconnected()) {
            exit();
            return;
        }

        cancelOfflineTimer();
        stopLogoutTimer();
        mStart = System.currentTimeMillis();
        getConnectionManager().deleteObserver(this);
        getModel().getToastObservable().deleteObserver(this);
        getModel().getConnMsgObservable().deleteObserver(this);

        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ConnectionManager) {
            final ConnectionManager cm = getConnectionManager();
            if (cm.isLoginRequired() || cm.isDisconnected()) {
                runOnUiThread(() -> exit());
            }
        } else if (observable instanceof ToastObservable) {
            final ToastObservable tObs = (ToastObservable) observable;
            UI.toast(this, tObs.getMessage(getResources()), Toast.LENGTH_LONG);
        } else if (observable instanceof ConnectionMessageObservable) {
            final ConnectionMessageObservable obs = (ConnectionMessageObservable) observable;
            update(obs);
        }
    }

    private void update(final ConnectionMessageObservable obs) {
        cancelOfflineTimer();

        if (!obs.isValid() || obs.isOnline()) {
            runOnUiThread(() -> { if (mSnackbar != null) { mSnackbar.dismiss(); }});
            return;
        }

        mTryingAt = System.currentTimeMillis() + obs.waitingMs();
        mOfflineTimer = new Timer();
        mOfflineTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final long remainingSec = (mTryingAt - System.currentTimeMillis()) / 1000;
                if (remainingSec < 0) {
                    cancelOfflineTimer();
                    return;
                }
                runOnUiThread(() -> {
                    // Update snackbar message on main thread
                    if (mSnackbar == null)
                        mSnackbar = Snackbar.make(findViewById(
                                                      android.R.id.content), R.string.id_you_are_not_connected,
                                                  Snackbar.LENGTH_INDEFINITE);
                    mSnackbar.setText(getString(R.string.id_not_connected_connecting_in_ds_, remainingSec));
                    mSnackbar.show();
                });
            }
        }, 0, 100);
    }

    private void cancelOfflineTimer() {
        if (mOfflineTimer != null) {
            mOfflineTimer.cancel();
            mOfflineTimer.purge();
        }
    }

    public void logout() {
        getGAApp().getExecutor().execute(() -> {
            startLoading();
            getConnectionManager().disconnect();
            stopLoading();
            runOnUiThread(() -> exit());
        });
    }

    private void exit() {
        if (isFinishing())
            return;
        final Intent intent = GaActivity.createToFirstIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        stopLogoutTimer();
        startLogoutTimer();
        return super.dispatchTouchEvent(ev);
    }

    private int delayLogoutTimer() {
        if (getModel() != null && getModel().getSettings() != null) {
            return getModel().getSettings().getAltimeout()  * 60 * 1000;
        }
        final String altimeString = cfg().getString(PrefKeys.ALTIMEOUT, "5");
        return Integer.parseInt(altimeString) * 60 * 1000;
    }

    private void startLogoutTimer() {
        stopLogoutTimer();
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> exit());
            }
        }, delayLogoutTimer());
        mTimer = timer;
    }

    private void stopLogoutTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }

    public boolean modelIsNullOrDisconnected() {
        return getModel() == null || getConnectionManager() == null || getConnectionManager().isDisconnected();
    }

    protected String getBitcoinUnitClean() {
        return getModel().getUnitKey();
    }

    // for btc and fiat
    protected void setAmountText(final EditText amountText, final boolean isFiat,
                                 final ObjectNode currentAmount) throws ParseException {
        final NumberFormat btcNf = getModel().getNumberFormat();
        setAmountText(amountText, isFiat, currentAmount, btcNf, false);

    }

    // for liquid assets and l-btc
    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final String asset) throws ParseException {

        NumberFormat nf = getModel().getNumberFormat();
        boolean isAsset = false;
        if (!"btc".equals(asset) && asset != null) {
            final AssetInfoData assetInfoData = getModel().getAssetsObservable().getAssetsInfos().get(asset);
            int precision = assetInfoData == null ? 0 : assetInfoData.getPrecision();
            nf = Model.getNumberFormat(precision);
            isAsset = true;
        }

        setAmountText(amountText, isFiat, currentAmount, nf, isAsset);
    }

    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final NumberFormat btcOrAssetNf, final boolean isAsset) throws ParseException {
        final NumberFormat us = Model.getNumberFormat(8, Locale.US);
        final NumberFormat fiatNf = Model.getNumberFormat(2);
        final String fiat = fiatNf.format(us.parse(currentAmount.get("fiat").asText()));
        final String source = currentAmount.get(isAsset ? "btc" : getBitcoinUnitClean()).asText();
        final String btc = btcOrAssetNf.format(us.parse(source));
        amountText.setText(isFiat ? fiat : btc);
    }

    protected void removeUtxosIfTooBig(final ObjectNode transactionFromUri) {
        if (transactionFromUri.toString().length() <= 200000)
            return;
        if (transactionFromUri.has("utxos")) {
            transactionFromUri.remove("utxos");
        }
        if (transactionFromUri.get("send_all").asBoolean() && transactionFromUri.has("used_utxos")) {
            transactionFromUri.remove("used_utxos");
        }
    }
}
