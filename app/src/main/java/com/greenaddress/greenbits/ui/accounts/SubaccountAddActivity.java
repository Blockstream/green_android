package com.greenaddress.greenbits.ui.accounts;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;

import androidx.fragment.app.Fragment;

public class SubaccountAddActivity extends LoggedActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subaccount_add);

        final NetworkData network = getNetwork();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(network.getIcon());
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
        setTitle(network.getName());

        final Fragment fragment = new SubaccountAddFragment();
        getSupportFragmentManager().beginTransaction()
        .add(R.id.fragment_container, fragment).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.cancel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_cancel:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
