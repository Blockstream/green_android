package com.satochip;

import com.blockstream.common.gdk.data.Network;

import java.util.ArrayList;
import java.util.List;

public class NfcActionObject {

    public NfcActionType actionType = NfcActionType.none;
    public NfcActionStatus actionStatus = NfcActionStatus.none;

    // getXpubs
    // List<String> getXpubs(@NonNull Network network, @NonNull List<? extends List<Integer>> paths, @Nullable HardwareWalletInteraction hwInteraction) {
    public Network networkParam = null;
    public List<? extends List<Integer>> pathsParam = new ArrayList<>();
    public List<String> xpubsResult = new ArrayList<>();


}
