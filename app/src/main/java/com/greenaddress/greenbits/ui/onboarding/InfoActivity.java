package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class InfoActivity extends LoginActivity implements View.OnClickListener {

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_info);
        setTitleBackTransparent();
        setTitle("");
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        UI.mapClick(this, R.id.continueButton, this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        UI.unmapClick(UI.find(this, R.id.continueButton));
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

    public void onClick(final View view){
        startActivity(new Intent(this, WordsActivity.class));
    }

}
