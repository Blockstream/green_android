package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;

import com.greenaddress.greenbits.GaService;


public class BuyActivity extends GaActivity {
    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        setContentView(R.layout.activity_sell);

        if (GaService.IS_ELEMENTS)
            setTitle(R.string.cash_out);

        final ReceiveFragment buyFragment = new ReceiveFragment();
        buyFragment.setIsExchanger(true);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.sell_fragment, buyFragment, "tag3")
                .disallowAddToBackStack()
                .commit();
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
