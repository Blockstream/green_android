package com.greenaddress.greenbits.ui.preferences;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.notifications.NotificationsFragment;

import static com.greenaddress.greenapi.Session.getSession;

public class PreferencesActivity extends LoggedActivity {

    private static final String TAG = PreferencesActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null){
            final Fragment preferenceFragment;
            if (getSession().isTwoFAReset())
                preferenceFragment = new ResetActivePreferenceFragment();
            else if (getSession().isWatchOnly())
                preferenceFragment = new WatchOnlyPreferenceFragment();
            else
                preferenceFragment = new GeneralPreferenceFragment();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, preferenceFragment).commit();
        }

        setTitleBackTransparent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing())
            return;
    }
}
