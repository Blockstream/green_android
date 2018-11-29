package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends LoggedActivity {

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        UI.preventScreenshots(this);

        final Bundle bundle = new Bundle();
        final MainFragment fragment = new MainFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, fragment)
        .commit();
    }
}
