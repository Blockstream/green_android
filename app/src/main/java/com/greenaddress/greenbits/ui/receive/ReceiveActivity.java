package com.greenaddress.greenbits.ui.receive;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class ReceiveActivity extends LoggedActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getModel() == null) {
            toFirst();
            return;
        }

        setContentView(R.layout.activity_receive);
        UI.preventScreenshots(this);
        setTitleBackTransparent();

        getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, new ReceiveFragment())
        .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = R.menu.receive_menu;
        getMenuInflater().inflate(id, menu);
        menu.findItem(R.id.action_generate_new).setIcon(R.drawable.ic_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.action_generate_new:
            // Implemented in our fragment
            return false;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
