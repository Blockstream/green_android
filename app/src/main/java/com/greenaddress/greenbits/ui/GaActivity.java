package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.blockstream.DeviceBrand;
import com.blockstream.gdk.data.Network;
import com.blockstream.libwally.Wally;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.HWWalletBridge;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.authentication.TrezorPassphraseActivity;
import com.greenaddress.greenbits.ui.authentication.TrezorPinActivity;
import com.greenaddress.greenbits.ui.components.ProgressBarHandler;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;


/**
 * Base class for activities within the application.
 *
 * Provides access to the main Application and Service objects along with
 * support code to handle service initialization, error handling etc.
 */
public abstract class GaActivity extends AppCompatActivity implements HWWalletBridge {
    public static final int
            REQUEST_BITCOIN_URL_LOGIN = 1,
            REQUEST_TX_DETAILS = 2,
            REQUEST_BITCOIN_URL_SEND = 3,
            REQUEST_SELECT_ASSET = 4,
            REQUEST_SELECT_SUBACCOUNT = 5;

    public static final int HARDWARE_PIN_REQUEST = 59212;
    public static final int HARDWARE_PASSPHRASE_REQUEST = 21392;

    protected static final String TAG = GaActivity.class.getSimpleName();
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_BLE_SELECTED = "android.hardware.ble.action.ACTION_BLE_SELECTED";

    private ProgressBarHandler mProgressBarHandler;
    private final SparseArray<SingleEmitter<String>> mHwEmitter = new SparseArray<>();

    public Bundle getMetadata() {
        Bundle metadata = null;
        try {
            metadata =
                getPackageManager().getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException ignored) {}

        return metadata;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.d(TAG, "onCreate -> " + this.getClass().getSimpleName());
        setTheme(ThemeUtils.getThemeFromNetworkId(Bridge.INSTANCE.getCurrentNetworkId(this), this,
                                                  getMetadata()));

        super.onCreate(savedInstanceState);
        final int viewId = getMainViewId();
        if (viewId != UI.INVALID_RESOURCE_ID)
            setContentView(viewId);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mProgressBarHandler != null)
            mProgressBarHandler.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /** Override to provide the main view id */
    protected int getMainViewId() { return UI.INVALID_RESOURCE_ID; }

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

    protected static boolean isHexSeed(final String hexSeed) {
        if (hexSeed.endsWith("X") && hexSeed.length() == 129) {
            try {
                Wally.hex_to_bytes(hexSeed.substring(0, 128));
                return true;
            } catch (final Exception e) {}
        }
        return false;
    }

    public void hideKeyboardFrom(final View v) {
        final View toHideFrom = v == null ? getCurrentFocus() : v;
        if (toHideFrom != null) {
            final InputMethodManager imm;
            imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(toHideFrom.getWindowToken(), 0);
        }
    }

    protected boolean isPermissionGranted(final int[] granted, final int msgId) {
        if (granted == null || granted.length == 0 || granted[0] != PackageManager.PERMISSION_GRANTED) {
            UI.toast(this, msgId, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    protected void setAppNameTitle() {
        setTitleWithNetwork(R.string.app_name);
    }

    protected void setTitleWithNetwork(final int resource) {
        final NetworkData networkData = Bridge.INSTANCE.getCurrentNetworkData(this);
        if (networkData == null || getSupportActionBar() == null) {
            setTitle(resource);
            return;
        }

        final String netname = networkData.getName();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(networkData.getIcon());
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

    public ProgressBarHandler getProgressBarHandler() {
        return mProgressBarHandler;
    }

    public boolean isLoading() {
        return mProgressBarHandler != null && mProgressBarHandler.isLoading();
    }

    private Single<String> hwRequest(final int requestType) {
        try {
            return Single.create(emitter -> {
                mHwEmitter.put(HARDWARE_PIN_REQUEST, emitter);

                startActivityForResult(new Intent(this,
                                requestType == HARDWARE_PIN_REQUEST ?
                                        TrezorPinActivity.class :
                                        TrezorPassphraseActivity.class),
                        requestType);
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public Single<String> requestPinMatrix(DeviceBrand deviceBrand){
        return hwRequest(HARDWARE_PIN_REQUEST);
    }

    public Single<String> requestPassphrase(DeviceBrand deviceBrand){
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
            toast.setDuration(iconId == R.drawable.ledger_device ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            toast.show();
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == HARDWARE_PIN_REQUEST || requestCode == HARDWARE_PASSPHRASE_REQUEST) {
            Log.d(TAG,"onActivityResult " + requestCode);
            SingleEmitter<String> emitter = mHwEmitter.get(requestCode);
            if(emitter != null){
                if(resultCode == RESULT_OK){
                    emitter.onSuccess(data.getStringExtra(String.valueOf(requestCode)));
                }else{
                    emitter.tryOnError(new Exception("id_action_canceled"));
                }
            }


        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public SharedPreferences cfg() {
        return getSharedPreferences(network(), MODE_PRIVATE);
    }

    protected String network() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }

    NetworkData cachedNetworkData = null;
    protected NetworkData getNetwork() {
        if(cachedNetworkData == null){
            cachedNetworkData = Bridge.INSTANCE.getCurrentNetworkData(this);
        }
        return cachedNetworkData;
    }

    public Session getSession() {
        return Session.getSession();
    }
}
