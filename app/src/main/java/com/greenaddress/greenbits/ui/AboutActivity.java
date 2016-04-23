package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends ActionBarActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Element versionElement = new Element();
        versionElement.setTitle(String.format(
                "GitHub %s: %s, %s",
                getString(R.string.app_version),
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE));

        final Intent github = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/greenaddress/greenbits/releases"));

        versionElement.setIntent(github);

        final AboutPage aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getString(R.string.greenaddress_headline))
                .setImage(R.drawable.logo_big)
                .addEmail("info@greenaddress.it")
                .addFacebook("GreenAddressIT")
                .addTwitter("GreenAddress")
                .addYoutube("UCcTlQ46wcp-pmwAg_Rj1DHQ");

        final String installer = getPackageManager().getInstallerPackageName(getPackageName());
        if ("com.android.vending".equals(installer)) {
            aboutPage.addPlayStore(getPackageName());
        }

        setContentView(aboutPage.addItem(versionElement).create());
    }
}
