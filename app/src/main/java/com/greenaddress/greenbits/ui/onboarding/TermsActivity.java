package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class TermsActivity extends LoginActivity implements View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_terms);
        setTitleBackTransparent();
        setTitle("");

        UI.find(this, R.id.continueButton).setEnabled(false);
        final CheckBox termsCheckbox = UI.find(this, R.id.termsCheckbox);
        termsCheckbox.setOnCheckedChangeListener(this);

        final TextView termsText = UI.find(this, R.id.termsText);
        final String link =
            String.format("<a href=\"https://greenaddress.it/tos/\">%s</a>", getString(R.string.id_terms_of_service));
        termsText.setText(Html.fromHtml(link));
        termsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://greenaddress.it/tos/"));
                startActivity(intent);
            }
        });
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
        startActivity(new Intent(this, InfoActivity.class));
    }

    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
        UI.find(this, R.id.continueButton).setEnabled(b);
    }
}
