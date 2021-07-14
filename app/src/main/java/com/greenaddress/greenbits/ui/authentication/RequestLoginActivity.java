package com.greenaddress.greenbits.ui.authentication;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.arch.core.util.Function;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.DeviceBrand;
import com.blockstream.gdk.data.Network;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerDeviceBLE;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.NetworkSwitchListener;
import com.greenaddress.greenbits.ui.accounts.SwitchNetworkFragment;
import com.greenaddress.greenbits.wallets.FirmwareInteraction;
import com.greenaddress.greenbits.wallets.LedgerBLEAdapter;
import com.greenaddress.jade.JadeAPI;
import com.greenaddress.jade.JadeBleImpl;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.schedulers.Schedulers;

@AndroidEntryPoint
public class RequestLoginActivity extends LoginActivity implements NetworkSwitchListener, FirmwareInteraction, HardwareConnectInteraction {
    private static final String TAG = RequestLoginActivity.class.getSimpleName();

    private static final int VENDOR_BTCHIP     = 0x2581;
    private static final int VENDOR_LEDGER     = 0x2c97;
    private static final int VENDOR_TREZOR     = 0x534c;
    private static final int VENDOR_TREZOR_V2  = 0x1209;
    private static final int VENDOR_JADE       = 0x10c4;

    private UsbManager mUsbManager;
    private UsbDevice mUsb;

    private BluetoothDevice mBleDevice;

    private TextView mInstructionsText;
    private Dialog mPinDialog;
    private Integer mVendorId;
    private Boolean mInLedgerDashboard;

    private Button mActiveNetwork;
    private Button mButtonContinue;
    private Button mButtonConnectionSettings;
    private TextView mSinglesigWarning;
    private Network networkData;
    private CompositeDisposable mDisposables;
    private HardwareConnect mHardwareConnect;
    private String mPin;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBackTransparent();

        mHardwareConnect = new HardwareConnect();

        mInLedgerDashboard = false;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mInstructionsText = UI.find(this, R.id.first_login_instructions);
        mActiveNetwork = UI.find(this, R.id.activeNetwork);
        mButtonContinue = UI.find(this, R.id.buttonContinue);
        mButtonConnectionSettings = UI.find(this, R.id.buttonConnectionSettings);
        mSinglesigWarning = UI.find(this, R.id.singleSigWarning);

        mActiveNetwork.setOnClickListener(v -> {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(null);

            // Create and show the dialog.
            DialogFragment newFragment = SwitchNetworkFragment.newInstance();
            newFragment.show(ft, "dialog");
        });

        mButtonContinue.setOnClickListener(v -> {
             continueToConnect();
             mInstructionsText.setVisibility(View.VISIBLE);
             mActiveNetwork.setVisibility(View.GONE);
             mSinglesigWarning.setVisibility(View.GONE);
        });

        mButtonConnectionSettings.setOnClickListener(v -> {
            Bridge.INSTANCE.appSettingsDialog(this);
        });

        UI.showIf(Bridge.INSTANCE.isDevelopmentFlavor(), mSinglesigWarning);

        networkData = getNetworkV4();

        mDisposables = new CompositeDisposable();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onUsbAttach(final UsbDevice usb) {
        Log.d(TAG, "onUsbAttach");
        if (mUsb != null && mUsb == usb) {
            Log.d(TAG, "onUsbAttach with existing USB");
            return;
        }
        mUsb = usb;
        mInLedgerDashboard = false;
        if (usb == null)
            return;

        final ImageView hardwareIcon = UI.find(this, R.id.hardwareIcon);
        mVendorId = usb.getVendorId();
        Log.d(TAG, "Vendor: " + mVendorId + " Product: " + usb.getProductId());

        switch (mVendorId) {
        case VENDOR_JADE:
            hardwareIcon.setImageResource(R.drawable.blockstream_jade_device);
            final JadeAPI jadeAPI = JadeAPI.createSerial(getSession(), mUsbManager, mUsb, 115200);
            mHardwareConnect.onJade(this, jadeAPI);
            return;

        case VENDOR_TREZOR:
        case VENDOR_TREZOR_V2:
            hardwareIcon.setImageResource(R.drawable.trezor_device);
            mHardwareConnect.onTrezor(this, mUsbManager, mUsb);
            return;

        case VENDOR_BTCHIP:
        case VENDOR_LEDGER:
            hardwareIcon.setImageResource(R.drawable.ledger_device);
            BTChipTransport transport = null;
            try {
                transport = BTChipTransportAndroid.open(mUsbManager, mUsb);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if (transport == null) {
                showInstructions(R.string.id_please_reconnect_your_hardware);
                return;
            }

            if (BTChipTransportAndroid.isLedgerWithScreen(usb)) {
                // User entered PIN on-device
                mHardwareConnect.onLedger(this, transport, true);
            } else {
                // Prompt for PIN to unlock device before setting it up
                showLedgerPinDialog(transport);
            }
        }
    }

    private void onBleAttach(final ParcelUuid serviceId, final BluetoothDevice btDevice) {
        final RxBleDevice bleDevice = RxBleClient.create(this).getBleDevice(btDevice.getAddress());
        if (serviceId == null || bleDevice == null || bleDevice.getName() == null) {
            mBleDevice = null;
            onNoHardwareWallet();
            return;
        }

        mBleDevice = btDevice;
        Log.d(TAG, "onBleAttach " + bleDevice.getName() + " (" + bleDevice.getMacAddress() + ")");
        final ImageView hardwareIcon = UI.find(this, R.id.hardwareIcon);

        // Check service id matches a supported hw wallet
        if (JadeBleImpl.IO_SERVICE_UUID.equals(serviceId.getUuid())) {
            // Blockstream Jade
            hardwareIcon.setImageResource(R.drawable.blockstream_jade_device);

            // Create JadeAPI on BLE device
            final JadeAPI jadeAPI = JadeAPI.createBle(getSession(), bleDevice);
            mHardwareConnect.onJade(this, jadeAPI);

        } else if (LedgerDeviceBLE.SERVICE_UUID.equals(serviceId.getUuid())) {
            // Ledger (Nano X)
            hardwareIcon.setImageResource(R.drawable.ledger_device);

            // Ledger BLE adapter will call the 'onLedger' function when the BLE connection is established
            // LedgerBLEAdapter.connectLedgerBLE(this, btDevice, this::onLedger, this::onLedgerError);

            LedgerBLEAdapter.connectLedgerBLE(this, btDevice, (final BTChipTransport transport, final boolean hasScreen) -> {
                mHardwareConnect.onLedger(this, transport, hasScreen);
            }, (final BTChipTransport transport) -> {
                mHardwareConnect.onLedgerError(this, transport);
            });
        } else {
            mBleDevice = null;
            onNoHardwareWallet();
        }
    }

    private void showLedgerPinDialog(final BTChipTransport transport) {
        mPinDialog = UI.dismiss(this, mPinDialog);

        final View v = UI.inflateDialog(this, R.layout.dialog_btchip_pin);

        mPinDialog = UI.popup(this, R.string.id_pin)
                     .customView(v, true)
                     .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                     .onPositive((dialog, which) -> {
                         mPin = UI.getText(v, R.id.btchipPINValue);
                         mDisposables.add(Observable.just(getSession())
                                 .observeOn(Schedulers.computation())
                                 .subscribe(session -> mHardwareConnect.onLedger(this, transport, false))
                         );
                     })
                     .onNegative((dialog, which) -> {
                         UI.toast(this, R.string.id_no_pin_provided_exiting, Toast.LENGTH_LONG);
                         finish();
                     }).build();

        UI.mapEnterToPositive(mPinDialog, R.id.btchipPINValue);
        UI.showDialog(mPinDialog);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPinDialog = UI.dismiss(this, mPinDialog);
        if (mDisposables != null) {
            mDisposables.dispose();
        }

        mHardwareConnect.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mActiveNetwork.setText(getString(R.string.id_s_network, networkData.getName()));

    }

    private void continueToConnect(){
        mButtonContinue.setVisibility(View.GONE);
        mButtonConnectionSettings.setVisibility(View.GONE);

        final Intent intent = getIntent();

        if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            onUsbAttach(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
        }

        if (ACTION_BLE_SELECTED.equalsIgnoreCase(intent.getAction())) {
            onBleAttach(intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID),
                    Objects.requireNonNull(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
        }

        if (mUsb != null || mBleDevice != null || mInLedgerDashboard) {
            // Continue displaying instructions until the user opens the
            // correct wallet app, or log in completes/errors out
            return;
        }

        // No hardware wallet after all
        onNoHardwareWallet();
    }

    private void onNoHardwareWallet() {
        finish();
    }

    public void showInstructions(final int resId) {
        runOnUiThread(() -> {
            mInstructionsText.setText(resId);
            UI.show(mInstructionsText);
        });
    }

    @Override
    public void onNetworkClick(Network nd) {
        networkData = nd;
        mActiveNetwork.setText(getString(R.string.id_s_network, networkData.getName()));
        Bridge.INSTANCE.setCurrentNetwork(this, networkData.getNetwork());
    }

    @NotNull
    @Override
    public Context context() {
        return this;
    }

    @Override
    public void showError(@NotNull String error) {
        UI.toast(this, error, Toast.LENGTH_LONG);
    }

    @NotNull
    @Override
    public Network getConnectionNetwork() {
        return networkData;
    }

    @Override
    public void onLoginSuccess() {
        super.onLoggedIn();
    }

    @Override
    public void askForFirmwareUpgrade(DeviceBrand deviceBrand, @Nullable String version, boolean isUpgradeRequired, @Nullable Function<Boolean, Void> callback) {
        runOnUiThread(() -> {
            if(deviceBrand == DeviceBrand.Blockstream){
                UI.popup(this, isUpgradeRequired ? R.string.id_new_jade_firmware_required : R.string.id_new_jade_firmware_available, R.string.id_continue, R.string.id_cancel)
                        .content(getString(R.string.id_install_version_s, version))
                        .onNegative((dialog, which) -> {
                            callback.apply(false);
                        })
                        .onPositive((dialog, which) -> {
                            callback.apply(true);
                        })
                        .build()
                        .show();
            }else{
                runOnUiThread(() -> {
                    showInstructions(R.string.id_outdated_hardware_wallet);

                    if(!isUpgradeRequired){
                        final MaterialDialog d;
                        d = UI.popup(RequestLoginActivity.this, R.string.id_warning, R.string.id_continue, R.string.id_cancel)
                                .content(R.string.id_outdated_hardware_wallet)
                                .onNegative((dialog, which) -> { if (callback != null) { callback.apply(false); } } )
                                .onPositive((dialog, which) -> { if (callback != null) { callback.apply(true); } } )
                                .build();
                        UI.setDialogCloseHandler(d, this::finishOnUiThread);
                        d.show();
                    }
                });
            }
        });
    }

    @NotNull
    @Override
    public Single<String> requestPin(@NotNull DeviceBrand deviceBrand) {
        return Single.just(mPin);
    }
}
