package com.greenaddress.greenbits.ui.preferences;
import android.annotation.TargetApi;
import android.os.Build;

import com.greenaddress.greenbits.ui.R;

import java.util.List;


public class NetworkSettingsActivity extends GaPreferenceActivity {

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(final List<Header> target) {
        loadHeadersFromResource(R.xml.pref_networkheaders, target);
    }
}
