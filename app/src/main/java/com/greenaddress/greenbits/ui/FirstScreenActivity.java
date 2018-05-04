package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipPublicKey;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.btchip.utils.KeyUtils;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ui.preferences.NetworkSettingsActivity;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FirstScreenActivity extends LoginActivity {
    private static final String NVM_PATH = "nvm.bin";
    private static final String TAG = FirstScreenActivity.class.getSimpleName();
    private static boolean tuiCall;
    private BTChipTransportFactory transportFactory;
    private static final int CONNECT_TIMEOUT = 2000;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        UI.mapClick(this, R.id.firstLogInButton, new Intent(this, MnemonicActivity.class));
        UI.mapClick(this, R.id.firstSignUpButton, new Intent(this, SignUpActivity.class));
        final Uri homepage = Uri.parse("https://greenaddress.it");
        UI.mapClick(this, R.id.firstMadeByText, new Intent(Intent.ACTION_VIEW, homepage));

        Log.d(TAG, "Create FirstScreenActivity : TUI " + tuiCall);
        if (tuiCall || (transportFactory != null))
            return;

        // Check if a TEE is supported
        mService.getExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() {
                transportFactory = new LedgerTransportTEEProxyFactory(getApplicationContext());
                final LedgerTransportTEEProxy teeTransport = (LedgerTransportTEEProxy) transportFactory.getTransport();
                final byte[] nvm = teeTransport.loadNVM(NVM_PATH);
                teeTransport.setDebug(BuildConfig.DEBUG);
                if (nvm != null) {
                    teeTransport.setNVM(nvm);
                }
                boolean initialized = false;
                // Check if the TEE can be connected
                final LinkedBlockingQueue<Boolean> waitConnected = new LinkedBlockingQueue<>(1);
                final boolean result = transportFactory.connect(FirstScreenActivity.this, new BTChipTransportFactoryCallback() {

                    @Override
                    public void onConnected(final boolean success) {
                        try {
                            waitConnected.put(success);
                        } catch (final InterruptedException e) {
                        }
                    }

                });
                if (result) {
                    try {
                        initialized = waitConnected.poll(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                    }
                    if (initialized)
                        initialized = teeTransport.init();
                }
                Log.d(TAG, "TEE init " + initialized);
                if (initialized) {
                    final BTChipDongle dongle = new BTChipDongle(teeTransport, true);
                    // Prompt for use (or use immediately if an NVM file exists and the application is ready)
                    // If ok, attempt setup, then verify PIN, then login to gait backend
                    boolean teeReady = false;
                    if (nvm != null)
                        try {
                            final int attempts = dongle.getVerifyPinRemainingAttempts();
                            teeReady = (attempts != 0);
                        } catch (final Exception e) {
                        }
                    Log.d(TAG, "TEE ready " + teeReady);
                    if (teeReady)
                        proceedTEE(teeTransport, dongle, false);
                    else {
                        FirstScreenActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                UI.popup(FirstScreenActivity.this, "Ledger Wallet Trustlet")
                                        .content("Ledger Wallet Trustlet is available - do you want to use it to register ?")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                                proceedTEE(teeTransport, dongle, true);
                                            }
                                        })
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                                try {
                                                    teeTransport.close();
                                                } catch (final Exception e) {
                                                }
                                            }
                                        }).build().show();
                            }
                        });
                    }
                }
                return null;
            }
        });
    }

    private void proceedTEE(final LedgerTransportTEEProxy transport, final BTChipDongle dongle, final boolean setup) {

        mService.getExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() {
                tuiCall = true;
                BTChipPublicKey masterPublicKey = null;
                Log.d(TAG, "TEE setup " + setup);
                if (setup) {
                    try {
                        // Not setup ? Create the wallet
                        dongle.setup(new BTChipDongle.OperationMode[]{BTChipDongle.OperationMode.WALLET},
                                new BTChipDongle.Feature[]{BTChipDongle.Feature.RFC6979}, // TEE doesn't need NO_2FA_P2SH
                                Network.NETWORK.getAddressHeader(),
                                Network.NETWORK.getP2SHHeader(),
                                new byte[4], null,
                                null,
                                null, null);
                        // Save the encrypted image
                        transport.writeNVM(NVM_PATH, transport.requestNVM().get());
                    } catch (final Exception e) {
                        Log.d(TAG, "Setup exception", e);
                        try {
                            transport.close();
                        } catch (final Exception e1) {
                        }
                        FirstScreenActivity.this.toast("Trustlet setup failed");
                        tuiCall = false;
                        return null;
                    }
                    // FIXME reopen transport - more stable
                    // (Should not be necessary anyway with the new transport API)
                    /*
                    try {
                        byte[] nvm = transport.requestNVM().get();
                        transport.close();
                        transport.setNVM(nvm);
                        transport.init();
                    }
                    catch(final Exception e) {
                        Log.d(TAG, "Transport reinitialization failed", e);
                        tuiCall = false;
                        return null;
                    }
                    */
                }
                // Verify the PIN
                try {
                    // TODO : handle terminated PIN
                    Log.d(TAG, "verify pin");
                    dongle.verifyPin(new byte[4]);
                    Log.d(TAG, "write NVM after verify pin");
                    transport.writeNVM(NVM_PATH, transport.requestNVM().get());
                } catch (final Exception e) {
                    Log.d(TAG, "PIN exception", e);
                    try {
                        transport.writeNVM(NVM_PATH, transport.requestNVM().get());
                    } catch (final Exception e1) {
                    }
                    try {
                        transport.close();
                    } catch (final Exception e1) {
                    }
                    FirstScreenActivity.this.toast("Trustlet PIN validation failed");
                    tuiCall = false;
                    return null;
                }
                // If a new key was set up, register it
                if (setup) {
                    try {
                        masterPublicKey = dongle.getWalletPublicKey("");
                    } catch (final Exception e) {
                        FirstScreenActivity.this.toast("Trustlet login failed");
                        tuiCall = false;
                        return null;
                    }
                }
                // And finally login
                final BTChipPublicKey masterPublicKeyFixed = masterPublicKey;

                Futures.addCallback(Futures.transformAsync(mService.onConnected, new AsyncFunction<Void, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                        if (!setup) {
                            Log.d(TAG, "TEE login");
                            return mService.login(new BTChipHWWallet(dongle));
                        } else {
                            Log.d(TAG, "TEE signup");
                            return mService.signup(new BTChipHWWallet(dongle), /*mnemonic*/ null, "HW", KeyUtils.compressPublicKey(masterPublicKeyFixed.getPublicKey()), masterPublicKeyFixed.getChainCode());
                        }
                    }
                }), new FutureCallback<LoginData>() {
                    @Override
                    public void onSuccess(final LoginData result) {
                        Log.d(TAG, "Success");
                        onLoginSuccess();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        Log.d(TAG, "login failed", t);
                        if (!(Throwables.getRootCause(t) instanceof LoginFailed))
                            finishOnUiThread();
                    }
                });

                tuiCall = false;
                return null;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.preauth_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.watchonly_preference:
                startActivity(new Intent(FirstScreenActivity.this, WatchOnlyLoginActivity.class));
                return true;
            case R.id.network_preferences:
                startActivity(new Intent(FirstScreenActivity.this, NetworkSettingsActivity.class));
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResumeWithService() {

        // Make a note if the user cancelled PIN entry
        final boolean userCancelled = mService.getUserCancelledPINEntry();
        mService.setUserCancelledPINEntry(false);

        //FIXME : recheck state, properly handle TEE link anyway
        if (mService.isLoggedIn()) {
            onLoginSuccess();
        } else if (mService.cfg("pin").getString("ident", null) != null && !userCancelled) {
            startActivity(new Intent(this, PinActivity.class));
            finish();
        }
    }
}
