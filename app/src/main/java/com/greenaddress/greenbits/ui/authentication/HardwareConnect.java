package com.greenaddress.greenbits.ui.authentication;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.blockstream.gdk.data.Device;
import com.blockstream.gdk.data.DeviceSupportsAntiExfilProtocol;
import com.blockstream.gdk.data.DeviceSupportsLiquid;
import com.btchip.BTChipConstants;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.Bridge;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;
import com.greenaddress.greenbits.wallets.JadeHWWallet;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.greenaddress.jade.JadeAPI;
import com.greenaddress.jade.entities.JadeError;
import com.satoshilabs.trezor.Trezor;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HardwareConnect {
    private static final String TAG = HardwareConnect.class.getSimpleName();

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    void onJade(final HardwareConnectInteraction interaction, final JadeAPI jade) {
        // Connect to jade (using background thread)
        mDisposables.add(Observable.just(jade)
                .subscribeOn(Schedulers.computation())
                .map(JadeAPI::connect)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rslt -> {
                            if (rslt) {
                                // Connected - ok to proceed to fw check, pin, login etc.
                                onJadeConnected(interaction, jade);
                            } else {
                                Log.e(TAG, "Failed to connect to Jade");
                                interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Exception connecting to Jade");
                            interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
                        }
                )
        );
    }

    private void reconnectSession(final HardwareConnectInteraction interaction) throws Exception {
        Log.d(TAG, "(re-)connecting gdk session)");
        interaction.getSession().disconnect();
        connect(interaction);
    }

    public void connect(final HardwareConnectInteraction interaction) throws Exception {
        final String network = PreferenceManager.getDefaultSharedPreferences(interaction.getContext()).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
        interaction.getSession().setNetwork(network);
        Bridge.INSTANCE.connect(interaction.getContext(), interaction.getSession().getNativeSession(), network, interaction.getHwwallet());
    }

    private void onJadeConnected(final HardwareConnectInteraction interaction, final JadeAPI jade) {
        mDisposables.add(Single.just(interaction.getSession())
                .subscribeOn(Schedulers.computation())

                // Connect GDKSession first (on a background thread), as we use httpRequest() as part of
                // Jade login (to access firmware server and to interact with the pinserver).
                // This also acts as a handy check that we have network connectivity before we start.
                .map(session -> {
                    reconnectSession(interaction);
                    return session;
                })
                .doOnError(throwable -> Log.e(TAG, "Exception connecting GDK - " + throwable))

                // Then create JadeHWWallet instance and authenticate (with pinserver) still on background thread
                .doOnSuccess(session -> Log.d(TAG, "Creating Jade HW Wallet)"))
                .map(session -> new Device("Jade", true, true, false,
                        DeviceSupportsLiquid.Lite,
                        DeviceSupportsAntiExfilProtocol.Optional))
                .map(device -> {
                    final JadeHWWallet jadeWallet = new JadeHWWallet(jade, interaction.getNetworkData(), device, Bridge.INSTANCE.getHardwareQATester());
                    return jadeWallet;
                })
                .flatMap(jadeWallet -> jadeWallet.authenticate(interaction.getContext(), interaction, interaction.getSession()))

                // If all succeeded, set as current hw wallet and login ... otherwise handle error/display error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        jadeWallet -> {
                            interaction.showInstructions(R.string.id_logging_in);
                            interaction.setHwwallet(jadeWallet);
                            doLogin(interaction, false);
                        },
                        throwable -> {
                            Log.e(TAG, "Connecting to Jade HW wallet got error: " + throwable);
                            if (throwable instanceof JadeError) {
                                final JadeError jaderr = (JadeError)throwable;
                                if (jaderr.getCode()  == JadeError.UNSUPPORTED_FIRMWARE_VERSION) {
                                    interaction.showInstructions(R.string.id_outdated_hardware_wallet);
                                } else if (jaderr.getCode()  == JadeError.CBOR_RPC_NETWORK_MISMATCH) {
                                    interaction.showInstructions(R.string.id_the_network_selected_on_the);
                                } else {
                                    // Error from Jade hw - show the hw error message as a toast
                                    interaction.showError(jaderr.getMessage());
                                    interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
                                }
                            } else if ("GDK_ERROR_CODE -1 GA_connect".equals(throwable.getMessage())) {
                                interaction.showInstructions(R.string.id_unable_to_contact_the_green);
                            } else {
                                interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
                            }
                            jade.disconnect();
                        }
                )
        );
    }

    void onTrezor(final HardwareConnectInteraction interaction, UsbManager usbManager, UsbDevice usb) {
        final Trezor t;
        t = Trezor.getDevice(usbManager, Collections.singletonList(usb));

        if (interaction.getNetworkData().getLiquid()) {
            interaction.showInstructions(R.string.id_hardware_wallet_support_for);
            return;
        }
        if (t == null)
            return;

        final List<Integer> version = t.getFirmwareVersion();
        final int vendorId = t.getVendorId();
        Log.d(TAG,"Trezor Version: " + version + " vendorid:" + vendorId + " productid:" + t.getProductId());

        // Min allowed: v1.6.0 & v2.1.0
        final boolean isFirmwareOutdated = version.get(0) < 1 ||
                (version.get(0) == 1 && version.get(1) < 6) ||
                (version.get(0) == 1 && version.get(1) == 6 && version.get(2) < 0) ||
                (version.get(0) == 2 && version.get(1) < 1);
        if (isFirmwareOutdated) {
            interaction.showFirmwareOutdated(() -> onTrezorConnected(interaction, t), null);
            return;
        }

        // All good
        onTrezorConnected(interaction, t);
    }

    private void onTrezorConnected(final HardwareConnectInteraction interaction, final Trezor t) {
        Log.d(TAG, "Creating Trezor HW wallet");
        final Device device = new Device("Trezor", false , false, false, DeviceSupportsLiquid.None, DeviceSupportsAntiExfilProtocol.None);
        interaction.setHwwallet(new TrezorHWWallet(t, interaction.getNetworkData(), device));

        doLogin(interaction, true);
    }

    void onLedger(final HardwareConnectInteraction interaction, final BTChipTransport transport, final boolean hasScreen) {
        interaction.showInstructions(R.string.id_logging_in);
        final String pin = interaction.getPin();

        transport.setDebug(BuildConfig.DEBUG);
        try {
            final BTChipDongle dongle = new BTChipDongle(transport, hasScreen);
            try {
                // This should only be supported by the Nano X
                final BTChipDongle.BTChipApplication application = dongle.getApplication();
                Log.d(TAG, "Ledger application:" + application);

                if (application.getName().contains("OLOS")) {
                    interaction.showInstructions(R.string.id_ledger_dashboard_detected);
                    closeLedger(transport);
                    return;
                }

                final boolean netMainnet = interaction.getNetworkData().getMainnet();
                final boolean netLiquid = interaction.getNetworkData().getLiquid();
                final boolean hwMainnet = !application.getName().contains("Test");
                final boolean hwLiquid = application.getName().contains("Liquid");
                Log.d(TAG, "Ledger application:" + application.getName() + ", network is mainnet:"+ netMainnet);

                if (netMainnet != hwMainnet || netLiquid != hwLiquid) {
                    // We using the wrong app, prompt the user to open the right app.
                    interaction.showInstructions(R.string.id_the_network_selected_on_the);
                    closeLedger(transport);
                    return;
                }
            } catch (final Exception e) {
                // Log but otherwise ignore
                Log.e(TAG, "Error trying to get Ledger application details: " + e);
            }

            // We don't ask for firmware version while in the dashboard, since the Ledger Nano X would return invalid status
            final BTChipDongle.BTChipFirmware fw = dongle.getFirmwareVersion();
            Log.d(TAG, "BTChip/Ledger firmware version " + fw);

            boolean isFirmwareOutdated = true;
            if (fw.getArchitecture() == BTChipDongle.BTCHIP_ARCH_LEDGER_1 && fw.getMajor() > 0) {
                // Min allowed: v1.0.4
                isFirmwareOutdated = (fw.getMajor() == 1 && fw.getMinor() < 0) ||
                        (fw.getMajor() == 1 && fw.getMinor() == 0 && fw.getPatch() < 4);
            } else if (fw.getArchitecture() == BTChipDongle.BTCHIP_ARCH_NANO_SX && fw.getMajor() > 0) {
                // Min allowed: v1.3.7
                isFirmwareOutdated = (fw.getMajor() == 1 && fw.getMinor() < 3) ||
                        (fw.getMajor() == 1 && fw.getMinor() == 3 && fw.getPatch() < 7);
            }

            if (isFirmwareOutdated) {
                interaction.showFirmwareOutdated(() -> onLedgerConnected(interaction, dongle, pin),
                        () -> closeLedger(transport));
                return;
            }

            // All good
            onLedgerConnected(interaction, dongle, pin);
        } catch (final BTChipException e) {
            if (e.getSW() != BTChipConstants.SW_INS_NOT_SUPPORTED)
                e.printStackTrace();

            if (e.getSW() == 0x6faa) {
                interaction.showInstructions(R.string.id_please_disconnect_your_ledger);
            } else {
                interaction.showInstructions(R.string.id_ledger_dashboard_detected);
            }
            closeLedger(transport);
        }
    }

    private void onLedgerConnected(final HardwareConnectInteraction interaction, final BTChipDongle dongle, final String pin) {
        final SettableFuture<Integer> pinCB = SettableFuture.create();

        final boolean havePin = !TextUtils.isEmpty(pin);
        Log.d(TAG, "Creating Ledger HW wallet" + (havePin ? " with PIN" : ""));
        final Device device = new Device("Ledger", true,false, false, DeviceSupportsLiquid.Lite, DeviceSupportsAntiExfilProtocol.None);
        interaction.setHwwallet(new BTChipHWWallet(dongle, havePin ? pin : null, pinCB, interaction.getNetworkData(), device));
        doLogin(interaction, true);
    }

    private void closeLedger(final BTChipTransport transport) {
        try {
            transport.close();
        } catch (final BTChipException ignored) {}
    }

    void onLedgerError(final HardwareConnectInteraction interaction, final BTChipTransport transport) {
        interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
        closeLedger(transport);
    }

    private void doLogin(final HardwareConnectInteraction interaction, final boolean bReConnectSession) {
        mDisposables.add(Observable.just(interaction.getSession())
                .observeOn(Schedulers.computation())
                .map((session) -> {

                    final String network = PreferenceManager
                            .getDefaultSharedPreferences(interaction.getContext())
                            .getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");

                    Bridge.INSTANCE.loginWithDevice(interaction.getContext(), interaction.getSession().getNativeSession(), network, bReConnectSession, interaction.getHwwallet(), new HardwareCodeResolver(interaction, interaction.getHwwallet()));

                    return session;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((session) -> {
                    interaction.onLoginSuccess();
                }, (final Throwable e) -> {
                    e.printStackTrace();
                    interaction.getSession().disconnect();

                    // If the error is the Anti-Exfil validation violation we show that prominently.
                    // Otherwise show a generic error and reconnect/retry message.
                    final String idValidationFailed = interaction.getContext().getResources().getResourceEntryName(R.string.id_signature_validation_failed_if);
                    if (idValidationFailed.equals(e.getMessage())) {
                        interaction.showInstructions(R.string.id_signature_validation_failed_if);
                    } else {
                        interaction.showError(interaction.getContext().getString(R.string.id_error_logging_in_with_hardware));
                        interaction.showInstructions(R.string.id_please_reconnect_your_hardware);
                    }
                })
        );
    }

    public void onDestroy() {
        mDisposables.dispose();
    }
}
