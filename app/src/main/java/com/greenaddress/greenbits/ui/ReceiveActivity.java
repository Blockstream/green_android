package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

public class ReceiveActivity extends LoggedActivity {

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_receive);
        UI.preventScreenshots(this);
        setTitleBack();

        getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, new ReceiveFragment())
        .commit();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
