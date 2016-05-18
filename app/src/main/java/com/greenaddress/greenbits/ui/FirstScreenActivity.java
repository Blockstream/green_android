package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipPublicKey;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.btchip.utils.Dump;
import com.btchip.utils.KeyUtils;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.ProxySettingsActivity;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FirstScreenActivity extends GaActivity {
    @NonNull
    private static final String NVM_PATH = "nvm.bin";
    @NonNull
    private static final String TAG = FirstScreenActivity.class.getSimpleName();
    private static boolean tuiCall;
    private BTChipTransportFactory transportFactory;
    private static final int CONNECT_TIMEOUT = 2000;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_screen; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateWithService");

        mapClick(R.id.firstLogInButton, new Intent(this, MnemonicActivity.class));
        mapClick(R.id.firstSignUpButton, new Intent(this, SignUpActivity.class));
        final Uri homepage = Uri.parse("https://greenaddress.it");
        mapClick(R.id.firstMadeByText, new Intent(Intent.ACTION_VIEW, homepage));

        Log.d(TAG, "Create FirstScreenActivity : TUI " + tuiCall);
        if (tuiCall || (transportFactory != null)) {
            return;
        }

        // Check if a TEE is supported
        getGAService().es.submit(new Callable<Object>() {
            @Nullable
            @Override
            public Object call() {
                transportFactory = new LedgerTransportTEEProxyFactory(getApplicationContext());
                final LedgerTransportTEEProxy teeTransport = (LedgerTransportTEEProxy) transportFactory.getTransport();
                byte[] nvm = teeTransport.loadNVM(NVM_PATH);
                teeTransport.setDebug(true);
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
                        } catch (@NonNull final InterruptedException e) {
                        }
                    }

                });
                if (result) {
                    try {
                        initialized = waitConnected.poll(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (@NonNull final InterruptedException e) {
                    }
                    if (initialized) {
                        initialized = teeTransport.init();
                    }
                }
                Log.d(TAG, "TEE init " + initialized);
                if (initialized) {
                    final BTChipDongle dongle = new BTChipDongle(teeTransport, true);
                    // Prompt for use (or use immediately if an NVM file exists and the application is ready)
                    // If ok, attempt setup, then verify PIN, then login to gait backend
                    boolean teeReady = false;
                    if (nvm != null) {
                        try {
                            final int attempts = dongle.getVerifyPinRemainingAttempts();
                            teeReady = (attempts != 0);
                        } catch (@NonNull final Exception e) {
                        }
                    }
                    Log.d(TAG, "TEE ready " + teeReady);
                    if (!teeReady) {
                        FirstScreenActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Popup(FirstScreenActivity.this, "Ledger Wallet Trustlet")
                                        .content("Ledger Wallet Trustlet is available - do you want to use it to register ?")
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                                                proceedTEE(teeTransport, dongle, true);
                                            }
                                        })
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                                                try {
                                                    teeTransport.close();
                                                } catch (@NonNull final Exception e) {
                                                }
                                            }
                                        }).build().show();
                            }

                        });
                    } else {
                        proceedTEE(teeTransport, dongle, false);
                    }
                }
                return null;
            }
        });
    }

    private void proceedTEE(@NonNull final LedgerTransportTEEProxy transport, @NonNull final BTChipDongle dongle, final boolean setup) {
        final GaService gaService = getGAService();
        gaService.es.submit(new Callable<Object>() {
            @Nullable
            @Override
            public Object call() {
                tuiCall = true;
                BTChipPublicKey masterPublicKey = null, loginPublicKey = null;
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
                    } catch (@NonNull final Exception e) {
                        Log.d(TAG, "Setup exception", e);
                        try {
                            transport.close();
                        } catch (@NonNull final Exception e1) {
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
            		catch(Exception e) {
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
                } catch (@NonNull final Exception e) {
                    Log.d(TAG, "PIN exception", e);
                    try {
                        transport.writeNVM(NVM_PATH, transport.requestNVM().get());
                    } catch (@NonNull final Exception e1) {
                    }
                    try {
                        transport.close();
                    } catch (@NonNull final Exception e1) {
                    }
                    FirstScreenActivity.this.toast("Trustlet PIN validation failed");
                    tuiCall = false;
                    return null;
                }
                // If a new key was set up, register it
                if (setup) {
                    try {
                        masterPublicKey = dongle.getWalletPublicKey("");
                        loginPublicKey = dongle.getWalletPublicKey("18241'");
                        Log.d(TAG, "TEE derived MPK " + Dump.dump(masterPublicKey.getPublicKey()) + " " + Dump.dump(masterPublicKey.getChainCode()));
                        Log.d(TAG, "TEE derived LPK " + Dump.dump(loginPublicKey.getPublicKey()) + " " + Dump.dump(loginPublicKey.getChainCode()));
                    } catch (Exception e) {
                        FirstScreenActivity.this.toast("Trustlet login failed");
                        tuiCall = false;
                        return null;
                    }
                }
                // And finally login
                final BTChipPublicKey masterPublicKeyFixed = masterPublicKey;
                final BTChipPublicKey loginPublicKeyFixed = loginPublicKey;
                Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final @Nullable Void result) {
                        final GaService gaService = getGAService();

                        Futures.addCallback(Futures.transform(gaService.onConnected, new AsyncFunction<Void, LoginData>() {
                            @NonNull
                            @Override
                            public ListenableFuture<LoginData> apply(@NonNull final Void input) throws Exception {
                                if (!setup) {
                                    Log.d(TAG, "TEE login");
                                    return gaService.login(new BTChipHWWallet(dongle));
                                } else {
                                    Log.d(TAG, "TEE signup");
                                    return gaService.signup(new BTChipHWWallet(dongle), KeyUtils.compressPublicKey(masterPublicKeyFixed.getPublicKey()), masterPublicKeyFixed.getChainCode(), KeyUtils.compressPublicKey(loginPublicKeyFixed.getPublicKey()), loginPublicKeyFixed.getChainCode());
                                }
                            }
                        }), new FutureCallback<LoginData>() {
                            @Override
                            public void onSuccess(@Nullable final LoginData result) {
                                Log.d(TAG, "Success");
                                final Intent main = new Intent(FirstScreenActivity.this, TabbedMainActivity.class);
                                startActivity(main);
                                FirstScreenActivity.this.finish();
                            }

                            @Override
                            public void onFailure(@NonNull final Throwable t) {
                                Log.d(TAG, "login failed", t);
                                if (!(t instanceof LoginFailed)) {
                                    FirstScreenActivity.this.finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        Log.d(TAG, "login crashed", t);
                        t.printStackTrace();
                    }
                });


                tuiCall = false;
                return null;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Disable proxy until fully working
        // getMenuInflater().inflate(R.menu.proxy_menu, menu);
        return true;
    }

    private void startNewActivity(final Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.proxy_preferences) {
            startNewActivity(ProxySettingsActivity.class);
            return true;
        }
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onResumeWithService(final ConnectivityObservable.State state) {
        //FIXME : recheck state, properly handle TEE link anyway
        if (state.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            startNewActivity(TabbedMainActivity.class);
            finish();
        } else if (getGAService().cfg("pin").getString("ident", null) != null) {
            startNewActivity(PinActivity.class);
            finish();
        }
    }
}
