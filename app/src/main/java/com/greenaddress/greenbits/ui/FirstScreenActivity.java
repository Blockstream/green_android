package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Observable;
import java.util.Observer;


public class FirstScreenActivity extends ActionBarActivity implements Observer {

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
        ((GreenAddressApplication) getApplication()).getConnectionObservable().addObserver(this);

        final ConnectivityObservable.State state = ((GreenAddressApplication) getApplication()).getConnectionObservable().getState();
        if (state.equals(ConnectivityObservable.State.LOGGEDIN) || state.equals(ConnectivityObservable.State.LOGGINGIN)) {
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
        ((GreenAddressApplication) getApplication()).getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {

    }
}
