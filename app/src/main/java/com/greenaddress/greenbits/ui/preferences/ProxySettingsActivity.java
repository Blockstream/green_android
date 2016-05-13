package com.greenaddress.greenbits.ui.preferences;
import com.greenaddress.greenbits.ui.R;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.List;


public class ProxySettingsActivity extends GaPreferenceActivity {

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_proxyheaders, target);
    }
}
