package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.preferences.SettingsActivity;


public class MainExchanger extends GaActivity {

    private final static int REQUEST_SETTINGS = 0;
    private MainFragment mMainFragment;

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_exchanger);
        mMainFragment = new MainFragment();
        mMainFragment.setIsExchanger(true);
        mMainFragment.setPageSelected(true);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_transactions, mMainFragment, "tag")
                .disallowAddToBackStack()
                .commit();

        final Context context = this;
        final Button sellBtn = UI.find(this, R.id.sell_btn);
        sellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, SellActivity.class));
            }
        });
        final Button buyBtn = UI.find(this, R.id.buy_btn);
        buyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, BuyActivity.class));
            }
        });

        if (GaService.IS_ELEMENTS) {
            sellBtn.setText(R.string.cash_in);
            buyBtn.setText(R.string.cash_out);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SETTINGS:
                mService.updateBalance(mService.getCurrentSubAccount());
                startActivity(new Intent(this, TabbedMainActivity.class));
                finish();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = mService.isWatchOnly() ? R.menu.watchonly : R.menu.menu_exchanger;
        getMenuInflater().inflate(id, menu);
        return true;
    }

    @Override
    public void onResumeWithService() {
        if (mService.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
        }
    }
}
