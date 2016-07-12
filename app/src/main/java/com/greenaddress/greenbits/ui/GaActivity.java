package com.greenaddress.greenbits.ui;

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
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.List;

/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity {

    private static final String TAG = GaActivity.class.getSimpleName();
    private static final int INVALID_RESOURCE_ID = 0;

    // Both of these variables are only assigned in the UI thread.
    // mService is available to all derived classes as soon as
    // onCreateWithService() is called. Once assigned it does not
    // change so may be read from background threads.
    private boolean mResumed = false;
    protected GaService mService = null;

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
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
    protected int getMainViewId() { return INVALID_RESOURCE_ID; }

    /** Override to provide onCreate/onResume/onPause processing.
      * When called, our service is guaranteed to be available. */
    abstract protected void onCreateWithService(final Bundle savedInstanceState);
    protected void onPauseWithService() { }
    protected void onResumeWithService() { }

    // Utility methods

    void finishOnUiThread() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GaActivity.this.finish();
            }
        });
    }

    protected View mapClick(final int id, final View.OnClickListener fn) {
        final View v = findViewById(id);
        v.setOnClickListener(fn);
        return v;
    }

    protected View mapClick(final int id, final Intent activityIntent) {
        return mapClick(id, new View.OnClickListener() {
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
            public void run() { Toast.makeText(GaActivity.this, id, len).show(); }
        });
    }

    private void toastImpl(final String s, final int len) {
        runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(GaActivity.this, s, len).show(); }
        });
    }

    public void toast(final Throwable t, final Button reenable) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (reenable != null)
                    reenable.setEnabled(true);
                t.printStackTrace();
                Toast.makeText(GaActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    public void toast(final Throwable t) { toast(t, null); }
    public void toast(final int id) { toastImpl(id, Toast.LENGTH_LONG); }
    public void toast(final String s) { toastImpl(s, Toast.LENGTH_LONG); }
    public void shortToast(final int id) { toastImpl(id, Toast.LENGTH_SHORT); }
    public void shortToast(final String s) { toastImpl(s, Toast.LENGTH_SHORT); }

    public static MaterialDialog.Builder popup(final Activity a, final String title, final int pos, final int neg) {
        final MaterialDialog.Builder b;
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

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos, final int neg) {
        return popup(a, a.getString(title), pos, neg);
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    public static MaterialDialog popupTwoFactorChoice(final Activity a, final GaService service,
                                                      final boolean skip, final CB.Runnable1T<String> callback) {
        final List<String> names = service.getEnabledTwoFacNames(false);

        if (skip || names.size() <= 1) {
           // Caller elected to skip, or no choices are available: don't prompt
           a.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   callback.run(names.isEmpty() ? null : service.getEnabledTwoFacNames(true).get(0));
               }
           });
           return null;
        }

        // Return a pop up dialog to let the user choose.
        String[] namesArray = new String[names.size()];
        namesArray = names.toArray(namesArray);
        return popup(a, R.string.twoFactorChoicesTitle, R.string.choose, R.string.cancel)
                   .items(namesArray)
                   .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                       @Override
                       public boolean onSelection(MaterialDialog dlg, View v, int which, CharSequence text) {
                           final List<String> systemNames = service.getEnabledTwoFacNames(true);
                           callback.run(systemNames.get(which));
                           return true;
                       }
                   }).build();
    }
}
