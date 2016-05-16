package com.greenaddress.greenbits.ui;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
    private static final int NO_MAIN_VIEW = 0; // Invalid resource id
    private boolean mServiceAvailable = false;

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != NO_MAIN_VIEW)
            setContentView(viewId);

        // Call onCreateWithService() on the GUI thread once our service
        // becomes available. In most cases this will execute immediately.
        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                GaActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        GaActivity.this.mServiceAvailable = true;
                        GaActivity.this.onCreateWithService(savedInstanceState);
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
        Log.d(TAG, "onPause: service " + (mServiceAvailable ? "available" : "not available"));
        super.onPause();
        if (mServiceAvailable) {
            getGAApp().getConnectionObservable().decRef();
            onPauseWithService();
        }
    }

    @Override
    final public void onResume() {
        Log.d(TAG, "onResume: service " + (mServiceAvailable ? "available" : "not available"));
        super.onResume();
        if (mServiceAvailable) {
            getGAApp().getConnectionObservable().incRef();
            onResumeWithService();
        }
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return NO_MAIN_VIEW; };

    /** Override to provide onCreate/onResume/onPause processing.
      * When called, our service is guaranteed to be available. */
    abstract protected void onCreateWithService(final Bundle savedInstanceState);
    protected void onPauseWithService() { }
    protected void onResumeWithService() { }

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

    public void toast(final int id) { Toast.makeText(this, id, Toast.LENGTH_LONG).show(); }
    public void toast(final String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    public void shortToast(final int id) { Toast.makeText(this, id, Toast.LENGTH_SHORT).show(); }
    public void shortToast(final String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
