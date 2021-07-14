package com.greenaddress.greenbits.ui.accounts;

import com.blockstream.gdk.data.Network;
import com.greenaddress.greenapi.data.NetworkData;

public interface NetworkSwitchListener {
    void onNetworkClick(final Network network);
}
