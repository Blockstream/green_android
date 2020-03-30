package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.snackbar.Snackbar;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Registry.getRegistry;
import static com.greenaddress.greenapi.Session.getSession;

public abstract class LoggedActivity extends GaActivity {

    private Timer mTimer = new Timer();
    private long mStart = System.currentTimeMillis();
    private Snackbar mSnackbar;
    private Timer mOfflineTimer = new Timer();
    private Long mTryingAt = 0L;

    private Disposable networkDisposable, transactionDisposable,
                       loginDisposable, logoutDisposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkDisposable = getSession().getNotificationModel().getNetworkObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(networkNode -> {
            updateNetwork(networkNode);
        });
        transactionDisposable = getSession().getNotificationModel().getTransactionObservable()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(txNode -> {
            UI.toast(this, R.string.id_new_transaction, Toast.LENGTH_LONG);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkDisposable != null)
            networkDisposable.dispose();
        if (transactionDisposable != null)
            transactionDisposable.dispose();
        if (loginDisposable != null)
            loginDisposable.dispose();
        if (logoutDisposable != null)
            logoutDisposable.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();

        final boolean timerExpired = mStart + delayLogoutTimer() < System.currentTimeMillis();
        if (timerExpired) {
            exit();
            return;
        }

        startLogoutTimer();

        // check network status on resume
        final JsonNode networkNode = getSession().getNotificationModel().getNetworkNode();
        if (networkNode != null)
            updateNetwork(networkNode);
    }

    @Override
    public void onPause() {
        super.onPause();

        cancelOfflineTimer();
        stopLogoutTimer();
        mStart = System.currentTimeMillis();

        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void updateNetwork(final JsonNode networkNode) {
        final boolean connected = networkNode.get("connected").asBoolean();
        if (!connected) {
            final long waitingMs = networkNode.get("waiting").asLong() * 1000;
            onOffline(waitingMs);
            return;
        }

        final boolean isLoginRequired = networkNode.get("login_required").asBoolean(false);
        if (!isLoginRequired) {
            onOnline();
            return;
        }

        final HWWallet hwWallet = getSession().getHWWallet();
        if (hwWallet == null) {
            exit();
            return;
        }

        // try to automatically login just in case of hw
        loginDisposable = Observable.just(getSession())
                          .observeOn(Schedulers.computation())
                          .map((session) -> {
            session.login(hwWallet.getHWDeviceData(), "", "").resolve(null,
                                                                      new HardwareCodeResolver(this, hwWallet));
            session.setHWWallet(hwWallet);
            return session;
        })
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe((session) -> {
            onOnline();
        }, (final Throwable e) -> {
            e.printStackTrace();
            exit();
        });
    }

    private void onOnline() {
        cancelOfflineTimer();
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }

    private void onOffline(final long waitingMs) {
        cancelOfflineTimer();
        mTryingAt = System.currentTimeMillis() + waitingMs;
        mOfflineTimer = new Timer();
        mSnackbar = null;

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
                    if (mSnackbar == null) {
                        mSnackbar = Snackbar.make(findViewById(
                                                      android.R.id.content), R.string.id_you_are_not_connected,
                                                  Snackbar.LENGTH_INDEFINITE);
                        mSnackbar.show();
                    }
                    mSnackbar.setText(getString(R.string.id_not_connected_connecting_in_ds_, remainingSec));
                });
            }
        }, 0, 1000);
    }

    private void cancelOfflineTimer() {
        if (mOfflineTimer != null) {
            mOfflineTimer.cancel();
            mOfflineTimer.purge();
        }
    }

    public void logout() {
        startLoading();
        logoutDisposable = Observable.just(getSession())
                           .subscribeOn(AndroidSchedulers.mainThread())
                           .observeOn(Schedulers.computation())
                           .map(session -> {
            session.disconnect();
            return session;
        }).subscribe(session -> {
            stopLoading();
            exit();
        }, (final Throwable e) -> {
            stopLoading();
            exit();
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
        if (getSession() != null && getSession().getSettings() != null) {
            return getSession().getSettings().getAltimeout()  * 60 * 1000;
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


    protected String getBitcoinUnitClean() {
        return Conversion.getUnitKey();
    }

    // for btc and fiat
    protected void setAmountText(final EditText amountText, final boolean isFiat,
                                 final ObjectNode currentAmount) throws ParseException {
        final NumberFormat btcNf = Conversion.getNumberFormat();
        setAmountText(amountText, isFiat, currentAmount, btcNf, "btc");
    }

    // for liquid assets and l-btc
    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final String asset) throws ParseException {

        NumberFormat nf = Conversion.getNumberFormat();
        if (!"btc".equals(asset) && asset != null) {
            final AssetInfoData assetInfoData = getRegistry().getInfos().get(asset);
            final int precision = assetInfoData == null ? 0 : assetInfoData.getPrecision();
            nf = Conversion.getNumberFormat(precision);
        }

        setAmountText(amountText, isFiat, currentAmount, nf, asset);
    }

    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final NumberFormat btcOrAssetNf, final String asset) throws ParseException {
        final NumberFormat us = Conversion.getNumberFormat(8, Locale.US);
        final NumberFormat fiatNf = Conversion.getNumberFormat(2);
        final String fiat = fiatNf.format(us.parse(currentAmount.get("fiat").asText()));
        final String source = currentAmount.get("btc".equals(asset) ? getBitcoinUnitClean() : asset).asText();
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

    protected int getActiveAccount() {
        final SharedPreferences preferences = getSharedPreferences(network(), MODE_PRIVATE);
        return preferences.getInt(PrefKeys.ACTIVE_SUBACCOUNT, 0);
    }

    protected void setActiveAccount(final int subaccount) {
        final SharedPreferences preferences = getSharedPreferences(network(), MODE_PRIVATE);
        preferences.edit().putInt(PrefKeys.ACTIVE_SUBACCOUNT, subaccount).apply();
    }
}
