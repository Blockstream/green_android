package com.greenaddress.greenbits.ui;

import android.os.Bundle;

public class SubaccountSelectActivity extends LoggedActivity {

    @Override
    protected int getMainViewId() { return R.layout.activity_subaccount_select; }

    @Override
    protected void onCreateWithService(Bundle savedInstanceState) {
        SubaccountSelectFragment fragment = new SubaccountSelectFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment).commit();


    }

}
