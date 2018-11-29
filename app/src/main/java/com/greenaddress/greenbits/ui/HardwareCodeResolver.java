package com.greenaddress.greenbits.ui;

import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenapi.data.HardwareCodeResolverData;

import java.util.List;

public class HardwareCodeResolver implements CodeResolver {
    private HWWallet hwWallet;


    public HardwareCodeResolver(final HWWallet hwWallet) {
        this.hwWallet = hwWallet;
    }

    @Override
    public SettableFuture<String> hardwareRequest(final GaActivity parent, final HWDeviceRequiredData requiredData) {
        final SettableFuture<String> future = SettableFuture.create();
        final HardwareCodeResolverData data = new HardwareCodeResolverData();

        switch (requiredData.getAction()) {

        case "get_xpubs":
            final List<String> xpubs = hwWallet.getXpubs(parent, requiredData.getPaths());
            data.setXpubs(xpubs);
            break;

        case "sign_message":
            final String derHex = hwWallet.signMessage(parent, requiredData.getPath(), requiredData.getMessage());
            data.setSignature(derHex);
            break;

        case "sign_tx":
            final List<String> derHexSigs;
            derHexSigs = hwWallet.signTransaction(parent, requiredData.getTransaction(),
                                                  requiredData.getSigningInputs(),
                                                  requiredData.getTransactionOutputs(),
                                                  requiredData.getSigningTransactions(),
                                                  requiredData.getSigningAddressTypes());
            data.setSignatures(derHexSigs);
            break;

        default:
            future.set(null);
            return future;
        }
        future.set(data.toString());
        return future;
    }


    @Override
    public SettableFuture<String> code(String method) {
        return null;
    }
}
