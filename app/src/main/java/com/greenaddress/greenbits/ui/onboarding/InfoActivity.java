package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class InfoActivity extends LoginActivity implements View.OnClickListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_info);
        setTitleBackTransparent();
        setTitle("");

        final TextView tosByProceedingText = UI.find(this, R.id.tosByProceedingText);
        final String link =
            String.format("<a href=\"https://blockstream.com/green/terms/\">%s</a>",
                          getString(R.string.id_terms_of_service));
        final String fullString = getString(R.string.id_by_proceeding_to_the_next_steps, link);

        tosByProceedingText.setText(Html.fromHtml(fullString));
        tosByProceedingText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onResume() {
        super.onResume();
        UI.mapClick(this, R.id.continueButton, this);
    }

    @Override
    public void onPause() {
        super.onPause();
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
