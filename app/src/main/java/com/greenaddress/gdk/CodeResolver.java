package com.greenaddress.gdk;

import androidx.annotation.Nullable;

import com.blockstream.gdk.HardwareWalletResolver;
import com.google.common.util.concurrent.SettableFuture;

public interface CodeResolver extends HardwareWalletResolver {
    SettableFuture<String> code(final String method, @Nullable final Integer attemptsRemaining);
    void dismiss();
}
