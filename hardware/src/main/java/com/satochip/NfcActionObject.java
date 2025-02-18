package com.satochip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blockstream.common.gdk.data.Network;
import com.blockstream.common.gdk.device.HardwareWalletInteraction;
import com.blockstream.common.gdk.device.SignMessageResult;

import java.util.ArrayList;
import java.util.List;

public class NfcActionObject {

    public NfcActionType actionType = NfcActionType.none;
    public NfcActionStatus actionStatus = NfcActionStatus.none;

    // getXpubs
    // List<String> getXpubs(@NonNull Network network, @NonNull List<? extends List<Integer>> paths, @Nullable HardwareWalletInteraction hwInteraction)
    public Network networkParam = null;
    public List<? extends List<Integer>> pathsParam = new ArrayList<>();
    public List<String> xpubsResult = new ArrayList<>();

    // signMessage
    // SignMessageResult signMessage(@NonNull List<Integer> path, @NonNull String message, boolean useAeProtocol, @Nullable String aeHostCommitment, @Nullable String aeHostEntropy, @Nullable HardwareWalletInteraction hwInteraction)
    public List<Integer> pathParam = new ArrayList<>();
    public String messageParam = "";
    public String signatureResult = "";

    // signTransaction
    //public List<? extends List<Integer>> pathsParam = new ArrayList<>();
    public List<byte[]> hashesParam = new ArrayList<>();
    public List<String> signaturesResult = new ArrayList<>();


}
