package com.greenaddress.greenbits.ui;

import android.support.v7.app.AppCompatActivity;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public abstract class ActionBarActivity extends AppCompatActivity {
    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}
