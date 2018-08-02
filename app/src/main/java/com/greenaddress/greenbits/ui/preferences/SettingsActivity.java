package com.greenaddress.greenbits.ui.preferences;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.R;
import android.annotation.TargetApi;
import android.os.Build;

import java.util.List;

public class SettingsActivity extends GaPreferenceActivity {

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(final List<Header> target) {
        if (getGAApp().mService.isElements())
            loadHeadersFromResource(R.xml.pref_headers_elements, target);
        else
            loadHeadersFromResource(R.xml.pref_headers, target);
    }
}
