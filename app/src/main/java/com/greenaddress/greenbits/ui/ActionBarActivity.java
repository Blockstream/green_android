package com.greenaddress.greenbits.ui;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public abstract class ActionBarActivity extends AppCompatActivity {
    @NonNull
    public GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    public GaService getGAService() {
        return getGAApp().gaService;
    }
}
