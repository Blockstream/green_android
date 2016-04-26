package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends ActionBarActivity {

    private Element getGitHubElement() {
        final Element gitHubElement = new Element();
        gitHubElement.setTitle(getString(mehdi.sakout.aboutpage.R.string.about_github));
        gitHubElement.setIcon(mehdi.sakout.aboutpage.R.drawable.about_icon_github);
        gitHubElement.setColor(ContextCompat.getColor(this, mehdi.sakout.aboutpage.R.color.github_color));
        final String release = "https://github.com/greenaddress/GreenBits/releases/tag/r%s";
        final Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(String.format(release, BuildConfig.VERSION_NAME)));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        gitHubElement.setIntent(intent);
        return gitHubElement;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AboutPage aboutPage = new AboutPage(this)
                .setDescription(getString(R.string.greenaddress_headline))
                .setImage(R.drawable.logo_big)
                .addEmail("info@greenaddress.it")
                .addFacebook("GreenAddressIT")
                .addTwitter("GreenAddress")
                .addYoutube("UCcTlQ46wcp-pmwAg_Rj1DHQ");
        final String pkgName = getPackageName();
        final String installer = getPackageManager().getInstallerPackageName(pkgName);
        if ("com.android.vending".equals(installer)) {
            aboutPage.addPlayStore(pkgName);
        }
        setContentView(aboutPage.addItem(getGitHubElement()).create());
        setTitle(String.format("%s %s",
                getString(R.string.app_name),
                getString(R.string.app_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_TYPE)));
    }
}
