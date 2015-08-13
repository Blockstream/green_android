package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.btchip.BTChipConstants;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipPublicKey;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.btchip.utils.Dump;
import com.btchip.utils.KeyUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.LoginData;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FirstScreenActivity extends ActionBarActivity implements Observer {
	
	private static final String NVM_PATH = "nvm.bin";
	private static final String TAG = "GreenTee";
	private static boolean tuiCall;
	private BTChipTransportFactory transportFactory;
	
	private static final int CONNECT_TIMEOUT = 2000;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);

        final Button loginButton = (Button) findViewById(R.id.firstLogInButton);
        final Button signupButton = (Button) findViewById(R.id.firstSignUpButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Intent mnemonicActivity = new Intent(FirstScreenActivity.this, MnemonicActivity.class);
                startActivity(mnemonicActivity);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Intent signUpActivity = new Intent(FirstScreenActivity.this, SignUpActivity.class);
                startActivity(signUpActivity);
            }
        });

        final TextView madeBy = (TextView) findViewById(R.id.firstMadeByText);

        madeBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://greenaddress.it"));
                startActivity(browserIntent);
            }
        });
           
        Log.d(TAG, "Create FirstScreenActivity : TUI " + tuiCall);
        if (tuiCall || (transportFactory != null)) {
        	return;
        }
        
        // Check if a TEE is supported
        final GaService gaService = getGAService();        
        gaService.es.submit(new Callable<Object>() {
            @Override
            public Object call() {
            	//transportFactory = new LedgerTransportTEEProxyFactory(FirstScreenActivity.this);
            	transportFactory = new LedgerTransportTEEProxyFactory(getApplicationContext());
            	final LedgerTransportTEEProxy teeTransport = (LedgerTransportTEEProxy)transportFactory.getTransport();
            	byte[] nvm = teeTransport.loadNVM(NVM_PATH);
            	teeTransport.setDebug(true);
            	if (nvm != null) {
            		teeTransport.setNVM(nvm);
            	}
            	boolean initialized = false;
				// Check if the TEE can be connected
				final LinkedBlockingQueue<Boolean> waitConnected = new LinkedBlockingQueue<Boolean>(1);
				boolean result = transportFactory.connect(FirstScreenActivity.this, new BTChipTransportFactoryCallback() {

					@Override
					public void onConnected(boolean success) {
						try {
							waitConnected.put(success);
						}
						catch(InterruptedException e) {							
						}						
					}
					
				});
				if (result) {
					try {
						initialized = waitConnected.poll(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
					}
					catch(InterruptedException e) {						
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
            				int attempts = dongle.getVerifyPinRemainingAttempts();
            				teeReady = (attempts != 0);
            			}
            			catch(Exception e) {            				
            			}
            		}
            		Log.d(TAG, "TEE ready " + teeReady);
            		if (!teeReady) {
            			FirstScreenActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
		                        new MaterialDialog.Builder(FirstScreenActivity.this)
		                        .title("Ledger Wallet Trustlet")
		                        .content("Ledger Wallet Trustlet is available - do you want to use it to register ?")
		                        .positiveColorRes(R.color.accent)
		                        .negativeColorRes(R.color.white)
		                        .titleColorRes(R.color.white)
		                        .contentColorRes(android.R.color.white)
		                        .theme(Theme.DARK)
		                        .positiveText("OK")
		                        .negativeText("CANCEL")
		                        .callback(new MaterialDialog.ButtonCallback() {
		                        	@Override
		                        	public void onPositive(MaterialDialog materialDialog) {
		                        		proceedTEE(teeTransport, dongle, true);
		                        	}
		                        	
		                        	public void onNegative(MaterialDialog materialDialog) {
		                        		try {
		                        			teeTransport.close();
		                        		}
		                        		catch(Exception e) {		                        			
		                        		}
		                        	}
		                        })                        
		                        .build().show();            																			
							}
            				
            			});
            		}
            		else {
            			proceedTEE(teeTransport, dongle, false);
            		}
            	}
            	return null;
            }
        });        
    }      
    
    private void proceedTEE(final LedgerTransportTEEProxy transport, final BTChipDongle dongle, final boolean setup) {
        final GaService gaService = getGAService();        
    	gaService.es.submit(new Callable<Object>() {
            @Override
            public Object call() {
            	tuiCall = true;
            	BTChipPublicKey masterPublicKey = null, loginPublicKey = null;
            	Log.d(TAG, "TEE setup " + setup);
            	if (setup) {
            		try {
            			// Not setup ? Create the wallet
            			dongle.setup(new BTChipDongle.OperationMode[] { BTChipDongle.OperationMode.WALLET}, 
            					new BTChipDongle.Feature[] { BTChipDongle.Feature.RFC6979 }, // TEE doesn't need NO_2FA_P2SH
            					Network.NETWORK.getAddressHeader(), 
            					Network.NETWORK.getP2SHHeader(), 
            					new byte[4], null,
            					null, 
            					null, null);
            			// Save the encrypted image
            			transport.writeNVM(NVM_PATH, transport.requestNVM().get());
            		}
            		catch(Exception e) {
            			Log.d(TAG, "Setup exception", e);
            			try {
            				transport.close();
            			}
            			catch(Exception e1) {            				
            			}
            			FirstScreenActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {            			
								Toast.makeText(FirstScreenActivity.this, "Trustlet setup failed", Toast.LENGTH_LONG).show();
							}
            			});
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
            	}
            	catch(Exception e) {
            		Log.d(TAG, "PIN exception", e);
            		try {
            			transport.writeNVM(NVM_PATH, transport.requestNVM().get());
            		}
            		catch(Exception e1) {            			
            		}        			
            		try {
            			transport.close();
            		}
            		catch(Exception e1) {            			
            		}
        			FirstScreenActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {            			
							Toast.makeText(FirstScreenActivity.this, "Trustlet PIN validation failed", Toast.LENGTH_LONG).show();
						}
        			});
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
            		}
            		catch(Exception e) {
            			Toast.makeText(FirstScreenActivity.this, "Trustlet login failed", Toast.LENGTH_LONG).show();
            			tuiCall = false;
            			return null;
            		}
            	}            	    	
            	// And finally login
            	final BTChipPublicKey masterPublicKeyFixed = masterPublicKey;
            	final BTChipPublicKey loginPublicKeyFixed = loginPublicKey;
                Futures.addCallback(getGAApp().onServiceConnected, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        final GaService gaService = getGAService();

                        Futures.addCallback(Futures.transform(gaService.onConnected, new AsyncFunction<Void, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                            	if (!setup) {
                            		Log.d(TAG, "TEE login");
                            		return gaService.login(new BTChipHWWallet(dongle));
                            	}
                            	else {
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
                            public void onFailure(final Throwable t) {
                            	Log.d(TAG, "login failed", t);
                                if (t instanceof LoginFailed) {
                                    // login failed - most likely not paired
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        	/*
                                            new MaterialDialog.Builder(RequestLoginActivity.this)
                                                    .title(getResources().getString(R.string.trezor_login_failed))
                                                    .content(getResources().getString(R.string.trezor_login_failed_details))
                                                    .build().show();
                                            */
                                        }
                                    });
                                } else {
                                    FirstScreenActivity.this.finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
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
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.first_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
	getGAApp().getConnectionObservable().addObserver(this);
       
        final ConnectivityObservable.State state = getGAApp().getConnectionObservable().getState();
        //FIXME : recheck state, properly handle TEE link anyway
        //if (state.equals(ConnectivityObservable.State.LOGGEDIN) || state.equals(ConnectivityObservable.State.LOGGINGIN)) {
        if (state.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(FirstScreenActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
            return;
        }
        if (getSharedPreferences("pin", MODE_PRIVATE).getString("ident", null) != null) {
            final Intent tabbedMainActivity = new Intent(FirstScreenActivity.this, PinActivity.class);
            startActivity(tabbedMainActivity);
            finish();
        }        
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {

    }
}
