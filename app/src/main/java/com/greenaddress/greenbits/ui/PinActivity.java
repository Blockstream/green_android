package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.PinData;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;

import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;


public class PinActivity extends ActionBarActivity implements Observer {

    private Menu menu;

    private void login(final CircularProgressButton pinLoginButton, final String ident, final EditText pinText, final TextView pinError) {
        Futures.addCallback(getGAApp().onServiceConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                loginAfterServiceConnected(pinLoginButton, ident, pinText, pinError);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(PinActivity.this, "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterServiceConnected(final CircularProgressButton pinLoginButton, final String ident, final EditText pinText, final TextView pinError) {
        if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.CONNECTED)) {
            Toast.makeText(PinActivity.this, "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
            return;
        }

        final GaService gaService = getGAService();

        final PinData pinData = new PinData(ident,
                getSharedPreferences("pin", MODE_PRIVATE).getString("encrypted", null));

        pinLoginButton.setIndeterminateProgressMode(true);
        pinLoginButton.setProgress(50);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                return gaService.pinLogin(pinData, pinText.getText().toString());
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                final SharedPreferences.Editor editor = getSharedPreferences("pin", MODE_PRIVATE).edit();
                editor.putInt("counter", 0);
                editor.apply();
                if (getCallingActivity() == null) {
                    final Intent mainActivity = new Intent(PinActivity.this, TabbedMainActivity.class);
                    startActivity(mainActivity);
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                String message = t.getMessage();
                final SharedPreferences pref = getSharedPreferences("pin", MODE_PRIVATE);
                final int counter = pref.getInt("counter", 0) + 1;
                if (t instanceof GAException) {
                    final SharedPreferences.Editor editor = pref.edit();
                    if (counter < 3) {
                        editor.putInt("counter", counter);
                        message = getString(R.string.attemptsLeftLong, 3 - counter);
                    } else {
                        message = getString(R.string.attemptsFinished);
                        editor.clear();
                    }

                    editor.apply();
                }
                final String tstMsg = message;

                PinActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pinText.setText("");
                        Toast.makeText(PinActivity.this, tstMsg, Toast.LENGTH_LONG).show();

                        pinLoginButton.setProgress(0);


                        if (counter >= 3) {
                            final Intent firstScreenActivity = new Intent(PinActivity.this, FirstScreenActivity.class);
                            startActivity(firstScreenActivity);
                            finish();
                        } else {
                            pinError.setVisibility(View.VISIBLE);
                            pinError.setText(getString(R.string.attemptsLeft, 3 - counter));
                        }
                    }
                });
            }
        }, gaService.es);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        final EditText pinText = (EditText) findViewById(R.id.pinText);
        final CircularProgressButton pinLoginButton = (CircularProgressButton) findViewById(R.id.pinLoginButton);
        final TextView pinError = (TextView) findViewById(R.id.pinErrorText);

        final String ident = getSharedPreferences("pin", MODE_PRIVATE).getString("ident", null);
        pinText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (event == null || !event.isShiftPressed()) {
                                // the user is done typing.
                                login(pinLoginButton, ident, pinText, pinError);
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                }
        );
        if (ident != null) {
            pinLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    login(pinLoginButton, ident, pinText, pinError);

                }
            });
        } else {
            final Intent firstScreenActivity = new Intent(this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);

        if (getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN) || getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGINGIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(PinActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common_menu, menu);
        this.menu = menu;
        return true;
    }

    public void setPlugVisible(final boolean visible) {
        if(menu!=null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final MenuItem item = menu.findItem(R.id.network_unavailable);
                    item.setVisible(visible);
                    final Animation rotateAnim = AnimationUtils.loadAnimation(PinActivity.this, R.anim.rotation);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.network_unavailable) {
            Toast.makeText(PinActivity.this, getGAApp().getConnectionObservable().getState().toString() , Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        // connectivity changed
        final ConnectivityObservable.State currentState = getGAApp().getConnectionObservable().getState();
        if (menu != null) {
            setPlugVisible(currentState != ConnectivityObservable.State.CONNECTED && currentState != ConnectivityObservable.State.LOGGEDIN && currentState != ConnectivityObservable.State.LOGGINGIN);
        }
    }
}
