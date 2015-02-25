package com.greenaddress.greenbits.ui;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public abstract class ActionBarActivity extends android.support.v7.app.ActionBarActivity {
    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}
