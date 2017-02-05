package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity {

    private static final String TAG = GaActivity.class.getSimpleName();

    // Both of these variables are only assigned in the UI thread.
    // mService is available to all derived classes as soon as
    // onCreateWithService() is called. Once assigned it does not
    // change so may be read from background threads.
    private boolean mResumed;
    protected GaService mService;

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate -> " + this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != UI.INVALID_RESOURCE_ID)
            setContentView(viewId);

        // Call onCreateWithService() on the GUI thread once our service
        // becomes available. In most cases this will execute immediately.
        Futures.addCallback(getGAApp().onServiceAttached, new CB.Op<Void>() {
            @Override
            public void onSuccess(final Void result) {
                GaActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        final GaActivity self = GaActivity.this;
                        Log.d(TAG, "onCreateWithService -> " + self.getClass().getSimpleName());
                        self.mService = getGAApp().mService;
                        self.onCreateWithService(savedInstanceState);
                        if (self.mResumed) {
                            // We resumed before the service became available, and so
                            // did not call onResumeWithService() then - call it now.
                            Log.d(TAG, "(delayed)onResumeWithService -> " + self.getClass().getSimpleName());
                            self.mService.incRef();
                            onResumeWithService();
                        }
                    }
                });
            }
        });
    }

    @Override
    final public void onPause() {
        Log.d(TAG, "onPause -> " + getClass().getSimpleName() +
              (mService == null ? " (no attached service)" : ""));
        super.onPause();
        mResumed = false;
        if (mService != null) {
            mService.decRef();
            onPauseWithService();
        }
    }

    @Override
    final public void onResume() {
        Log.d(TAG, "onResume -> " + getClass().getSimpleName() +
              (mService == null ? " (no attached service)" : ""));
        super.onResume();
        mResumed = true;
        if (mService != null) {
            mService.incRef();
            onResumeWithService();
        }
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return UI.INVALID_RESOURCE_ID; }

    /** Override to provide onCreate/onResume/onPause processing.
      * When called, our service is guaranteed to be available. */
    abstract protected void onCreateWithService(final Bundle savedInstanceState);
    protected void onPauseWithService() { }
    protected void onResumeWithService() { }

    // Utility methods

    void finishOnUiThread() {
        runOnUiThread(new Runnable() {
            public void run() {
                GaActivity.this.finish();
            }
        });
    }

    protected void setMenuItemVisible(final Menu m, final int id, final boolean visible) {
        if (m == null)
            return;
        runOnUiThread(new Runnable() {
            public void run() {
                final MenuItem item = m.findItem(id);
                if (item != null)
                    item.setVisible(visible);
            }
        });
    }

    public void toast(final Throwable t) { UI.toast(this, t, null); }
    public void toast(final int id) { UI.toast(this, id, Toast.LENGTH_LONG); }
    public void toast(final String s) { UI.toast(this, s, Toast.LENGTH_LONG); }
    public void shortToast(final int id) { UI.toast(this, id, Toast.LENGTH_SHORT); }
    public void shortToast(final String s) { UI.toast(this, s, Toast.LENGTH_SHORT); }
}
