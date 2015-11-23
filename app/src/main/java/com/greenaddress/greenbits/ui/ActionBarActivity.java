package com.greenaddress.greenbits.ui;

import android.support.v7.app.AppCompatActivity;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public abstract class ActionBarActivity extends AppCompatActivity {
    GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    GaService getGAService() {
        return getGAApp().gaService;
    }
}
