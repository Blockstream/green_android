package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.authentication.FirstScreenActivity;
import com.greenaddress.greenbits.ui.authentication.PinActivity;
import com.greenaddress.greenbits.ui.authentication.TrezorPassphraseActivity;
import com.greenaddress.greenbits.ui.authentication.TrezorPinActivity;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;

/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity {

    public static final int HARDWARE_PIN_REQUEST = 59212;
    public static final int HARDWARE_PASSPHRASE_REQUEST = 21392;

    private static final String TAG = GaActivity.class.getSimpleName();
    protected static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

    // Both of these variables are only assigned in the UI thread.
    // mService is available to all derived classes as soon as
    // onCreateWithService() is called. Once assigned it does not
    // change so may be read from background threads.
    private boolean mResumed;
    public GaService mService;
    protected ProgressBarHandler mProgressBarHandler;
    private SparseArray<SettableFuture<String>> mHwFunctions = new SparseArray<>();

    private GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected Bundle getMetadata() {
        Bundle metadata = null;
        try {
            metadata =
                getPackageManager().getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return metadata;
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate -> " + this.getClass().getSimpleName());

        setTheme(ThemeUtils.getThemeFromNetworkId(GaService.getNetworkFromId(GaService.getCurrentNetworkId(this)), this,
                                                  getMetadata()));

        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != UI.INVALID_RESOURCE_ID)
            setContentView(viewId);

        // Call onCreateWithService() on the GUI thread once our service
        // becomes available. In most cases this will execute immediately.
        Futures.addCallback(getGAApp().onServiceAttached, new CB.Op<Void>() {
            @Override
            public void onSuccess(final Void result) {
                GaActivity.this.runOnUiThread(() -> {
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
                });
            }
        });
    }

    @Override
    final public void onPause() {
        Log.d(TAG, getLogMessage("onPause"));
        super.onPause();
        mResumed = false;
        if (mProgressBarHandler != null)
            mProgressBarHandler.stop();
        if (mService != null) {
            mService.decRef();
            onPauseWithService();
        }
    }

    @Override
    final public void onResume() {
        Log.d(TAG, getLogMessage("onResume"));
        super.onResume();
        mResumed = true;
        if (mService != null) {
            mService.incRef();
            onResumeWithService();
        }
    }

    private String getLogMessage(final String caller) {
        return caller + " -> " + getClass().getSimpleName() +
               (mService == null ? " (no attached service)" : "");
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return UI.INVALID_RESOURCE_ID; }

    /** Override to provide onCreate/onResume/onPause processing.
     * When called, our service is guaranteed to be available. */
    abstract protected void onCreateWithService(final Bundle savedInstanceState);
    protected void onPauseWithService() { }
    protected void onResumeWithService() { }

    // Utility methods

    public void finishOnUiThread() {
        runOnUiThread(GaActivity.this::finish);
    }

    protected void setMenuItemVisible(final Menu m, final int id, final boolean visible) {
        if (m == null)
            return;
        runOnUiThread(() -> {
            final MenuItem item = m.findItem(id);
            if (item != null)
                item.setVisible(visible);
        });
    }

    protected boolean isHexSeed(final String hexSeed) {
        if (hexSeed.endsWith("X") && hexSeed.length() == 129) {
            try {
                Wally.hex_to_bytes(hexSeed.substring(0, 128));
                return true;
            } catch (final Exception e) {}
        }
        return false;
    }

    protected void hideKeyboardFrom(final View v) {
        final View toHideFrom = v == null ? getCurrentFocus() : v;
        if (toHideFrom != null) {
            final InputMethodManager imm;
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(toHideFrom.getWindowToken(), 0);
        }
    }

    public void toast(final Throwable t) { UI.toast(this, t, null); }
    public void toast(final int id) { UI.toast(this, id, Toast.LENGTH_LONG); }
    public void toast(final int id, final Button reenable) { UI.toast(this, getString(id), reenable); }
    public void toast(final String s) { UI.toast(this, s, Toast.LENGTH_LONG); }
    public void shortToast(final int id) { UI.toast(this, id, Toast.LENGTH_SHORT); }

    protected boolean isPermissionGranted(final int[] granted, final int msgId) {
        if (granted == null || granted.length == 0 || granted[0] != PackageManager.PERMISSION_GRANTED) {
            shortToast(msgId);
            return false;
        }
        return true;
    }

    protected void setAppNameTitle() {
        setTitleWithNetwork(R.string.app_name);
    }

    protected void setTitleWithNetwork(final int resource) {
        if (mService == null || mService.getNetwork() == null || getSupportActionBar() == null) {
            setTitle(resource);
            return;
        }

        final String netname = mService.getNetwork().getName();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(mService.getNetwork().getIcon());
        if (!"Bitcoin".equals(netname))
            setTitle(String.format(" %s %s",
                                   netname, getString(resource)));
        else
            setTitle(resource);
    }

    public void setTitleBackTransparent() {
        setTitleBack();
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
    }

    public void setTitleBack() {
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    protected Model getModel() {
        return mService.getModel();
    }

    public void startLoading() {
        startLoading("");
    }

    public void startLoading(final String label) {
        runOnUiThread(() -> {
            if (mProgressBarHandler == null)
                mProgressBarHandler = new ProgressBarHandler(GaActivity.this);
            mProgressBarHandler.start(label);
        });
    }

    public void stopLoading() {
        if (mProgressBarHandler == null)
            return;
        runOnUiThread(() -> mProgressBarHandler.stop());
    }

    public boolean isLoading() {
        return mProgressBarHandler != null && mProgressBarHandler.isLoading();
    }

    private String hwRequest(final int requestType) {
        try {
            mHwFunctions.put(requestType, SettableFuture.create());
            startActivityForResult(new Intent(this,
                                              requestType == HARDWARE_PIN_REQUEST ?
                                              TrezorPinActivity.class :
                                              TrezorPassphraseActivity.class),
                                   requestType);
            return mHwFunctions.get(requestType).get();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // TOD: Handle BTCHip PIN stuff here too
    public String pinMatrixRequest(final HWWallet hw) {
        return hwRequest(HARDWARE_PIN_REQUEST);
    }

    public String passphraseRequest(final HWWallet hw) {
        return hwRequest(HARDWARE_PASSPHRASE_REQUEST);
    }

    public void interactionRequest(final HWWallet hw) {
        final int iconId = hw.getIconResourceId();
        runOnUiThread(() -> {
            final LayoutInflater inflater = getLayoutInflater();
            final View v = inflater.inflate(R.layout.hardware_toast,
                                            findViewById(R.id.hw_toast_container));
            ((ImageView) UI.find(v, R.id.hwToastIcon)).setImageResource(iconId);

            final Toast toast = new Toast(this);
            toast.setView(v);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.setDuration(iconId == R.drawable.ic_ledger ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            toast.show();
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == HARDWARE_PIN_REQUEST || requestCode == HARDWARE_PASSPHRASE_REQUEST) {
            Log.d(TAG,"onActivityResult " + requestCode);
            mHwFunctions.get(requestCode).set(resultCode ==
                                              RESULT_OK ? data.getStringExtra(String.valueOf(
                                                                                  requestCode)) : null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected void goToTabbedMainActivity() {
        final Intent intent = new Intent(this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    static public Intent createToFirstIntent(final Context ctx) {
        final Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Logout to first screen
        intent.setClass(ctx, FirstScreenActivity.class);
        return intent;
    }
}
