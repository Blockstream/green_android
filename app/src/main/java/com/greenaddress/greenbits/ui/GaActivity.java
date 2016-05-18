package com.greenaddress.greenbits.ui;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity {

    private static final String TAG = GaActivity.class.getSimpleName();
    private static final int INVALID_RESOURCE_ID = 0;
    private boolean mServiceAvailable = false;

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate -> " + this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != INVALID_RESOURCE_ID)
            setContentView(viewId);

        // Call onCreateWithService() on the GUI thread once our service
        // becomes available. In most cases this will execute immediately.
        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                GaActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        final GaActivity self = GaActivity.this;
                        Log.d(TAG, "onCreateWithService -> " + self.getClass().getSimpleName());
                        self.mServiceAvailable = true;
                        self.onCreateWithService(savedInstanceState,
                                                 self.getGAApp().getConnectionObservable().getState());
                    }
                });
            }
            @Override
            public void onFailure(final Throwable t) {
                Log.d(TAG, "onServiceAttached exception:", t);
                t.printStackTrace();
            }
        });
    }

    @Override
    final public void onPause() {
        Log.d(TAG, "onPause -> " + getClass().getSimpleName() +
              (mServiceAvailable ? "" : " (no attached service)"));
        super.onPause();
        if (mServiceAvailable) {
            getGAApp().getConnectionObservable().decRef();
            onPauseWithService();
        }
    }

    @Override
    final public void onResume() {
        Log.d(TAG, "onResume -> " + getClass().getSimpleName() +
              (mServiceAvailable ? "" : " (no attached service)"));
        super.onResume();
        if (mServiceAvailable)
            onResumeWithService(getGAApp().getConnectionObservable().incRef());
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return INVALID_RESOURCE_ID; };

    /** Override to provide onCreate/onResume/onPause processing.
      * When called, our service is guaranteed to be available. */
    abstract protected void onCreateWithService(final Bundle savedInstanceState,
                                                final ConnectivityObservable.ConnectionState cs);
    protected void onPauseWithService() { }
    protected void onResumeWithService(final ConnectivityObservable.ConnectionState cs) { }

    // Utility methods

    protected void mapClick(final int id, final View.OnClickListener fn) {
        findViewById(id).setOnClickListener(fn);
    }

    protected void mapClick(final int id, final Intent activityIntent) {
        mapClick(id, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                startActivity(activityIntent);
            }
        });
    }

    protected void setMenuItemVisible(final Menu m, final int id, final boolean visible) {
        if (m == null)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MenuItem item = m.findItem(id);
                if (item != null)
                    item.setVisible(visible);
            }
        });
    }

    private void toastImpl(final int id, final int len) {
        runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(GaActivity.this, id, len); }
        });
    }

    private void toastImpl(final String s, final int len) {
        runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(GaActivity.this, s, len); }
        });
    }

    public void toast(final Throwable t, final Button reenable) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (reenable != null)
                    reenable.setEnabled(true);
                t.printStackTrace();
                Toast.makeText(GaActivity.this, t.getMessage(), Toast.LENGTH_LONG);
            }
        });
    }
    public void toast(final Throwable t) { toast(t, null); }
    public void toast(final int id) { toastImpl(id, Toast.LENGTH_LONG); }
    public void toast(final String s) { toastImpl(s, Toast.LENGTH_LONG); }
    public void shortToast(final int id) { toastImpl(id, Toast.LENGTH_SHORT); }
    public void shortToast(final String s) { toastImpl(s, Toast.LENGTH_SHORT); }

    public static MaterialDialog.Builder Popup(Activity a, final String title, final int pos, final int neg) {
        MaterialDialog.Builder b;
        b = new MaterialDialog.Builder(a)
                              .title(title)
                              .titleColorRes(R.color.white)
                              .positiveColorRes(R.color.accent)
                              .negativeColorRes(R.color.accent)
                              .contentColorRes(R.color.white)
                              .theme(Theme.DARK);
       if (pos != INVALID_RESOURCE_ID)
           b.positiveText(pos);
       if (neg != INVALID_RESOURCE_ID)
           return b.negativeText(neg);
       return b;
    }

    public static MaterialDialog.Builder Popup(Activity a, final String title, final int pos) {
        return Popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder Popup(Activity a, final String title) {
        return Popup(a, title, android.R.string.ok, android.R.string.cancel);
    }
}
