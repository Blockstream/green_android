package com.greenaddress.greenbits.ui;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.snackbar.Snackbar;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public abstract class LoggedActivity extends GaActivity {

    private Timer mTimer = new Timer();
    private Snackbar mSnackbar;
    private Timer mOfflineTimer = new Timer();
    private Long mTryingAt = 0L;

    private Disposable networkDisposable, transactionDisposable,
                       logoutDisposable;

    @Override
    public void onResume() {
        super.onResume();

        if (!Bridge.INSTANCE.isSessionConnected() ||
                getSession() == null ||
                getSession().getSettings() == null) {
            exit(Bridge.INSTANCE.getActiveWalletId());
            return;
        }

        // check network status on resume
        final JsonNode networkNode = getSession().getNotificationModel().getNetworkNode();
        if (networkNode != null)
            updateNetwork(networkNode);

        transactionDisposable = getSession().getNotificationModel().getTransactionObservable()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(txNode -> {
            UI.toast(this, R.string.id_new_transaction, Toast.LENGTH_LONG);
        });

        final Observable<JsonNode> networkObservable = getSession().getNotificationModel().getNetworkObservable()
                                                       .observeOn(AndroidSchedulers.mainThread())
                                                       .map(network -> {
            updateNetwork(network);
            return network;
        }).filter(network -> {
            return network.get("connected").asBoolean(false) &&
            network.get("login_required").asBoolean(false);
        }).observeOn(Schedulers.computation());

        final HWWallet hwWallet = getSession().getHWWallet();
        if (hwWallet == null) {
            networkDisposable = networkObservable.map(network -> {
                getSession().disconnect();
                return network;
            }).subscribe(res -> {
                exit(null);
            }, (final Throwable e) -> {
                e.printStackTrace();
                exit(null);
            });
        } else {
            networkDisposable = networkObservable.map(network -> {
                return getSession();
            }).map(session -> {
                return session.login(hwWallet.getDevice());
            }).flatMap(c -> {
                return Observable.just(c).map(call -> {
                    return call.resolve(new HardwareCodeResolver(this, hwWallet), null);
                }).doOnError(throwable -> {
                    Log.e(TAG, "Throwable " + throwable.getMessage());
                });
            }).subscribe(res -> {
                onOnline();
            }, (final Throwable e) -> {
                e.printStackTrace();
                exit(null);
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        cancelOfflineTimer();

        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }

        if (networkDisposable != null)
            networkDisposable.dispose();
        if (transactionDisposable != null)
            transactionDisposable.dispose();
        if (logoutDisposable != null)
            logoutDisposable.dispose();
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

    private void exit(Long walletId) {
        if (isFinishing())
            return;

        Bridge.INSTANCE.navigateToLogin(this, walletId);

        finish();
    }

    protected String getBitcoinUnitClean() throws Exception {
        return Conversion.getUnitKey(getSession());
    }

    // for btc and fiat
    protected void setAmountText(final EditText amountText, final boolean isFiat,
                                 final ObjectNode currentAmount) throws Exception {
        final NumberFormat btcNf = Conversion.getNumberFormat(getSession());
        setAmountText(amountText, isFiat, currentAmount, btcNf, getNetwork().getPolicyAsset());
    }

    // for liquid assets and l-btc
    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final String asset) throws Exception {

        NumberFormat nf = Conversion.getNumberFormat(getSession());
        if (!getNetwork().getPolicyAsset().equals(asset) && asset != null) {
            final AssetInfoData assetInfoData = getSession().getRegistry().getAssetInfo(asset);
            final int precision = assetInfoData == null ? 0 : assetInfoData.getPrecision();
            nf = Conversion.getNumberFormat(precision);
        }

        setAmountText(amountText, isFiat, currentAmount, nf, asset);
    }

    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final NumberFormat btcOrAssetNf, final String asset) throws Exception {
        final NumberFormat us = Conversion.getNumberFormat(8, Locale.US);
        final String source = currentAmount.get(getNetwork().getPolicyAsset().equals(asset) ? getBitcoinUnitClean() : asset).asText();
        final String btc = btcOrAssetNf.format(us.parse(source));

        if(isFiat) {
            final NumberFormat fiatNf = Conversion.getNumberFormat(2);
            final String fiat = fiatNf.format(us.parse(currentAmount.get("fiat").asText()));
            amountText.setText(fiat);
        }else{
            amountText.setText(btc);
        }
    }

    protected int getActiveAccount() {
        return Bridge.INSTANCE.getActiveAccount();
    }
}
