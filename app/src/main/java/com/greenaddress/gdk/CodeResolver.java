package com.greenaddress.gdk;

import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenbits.ui.GaActivity;

import javax.annotation.Nullable;

public interface CodeResolver {
    SettableFuture<String> hardwareRequest(final HWDeviceRequiredData requiredData);
    SettableFuture<String> code(final String method, @Nullable final Integer attemptsRemaining);
    void dismiss();
}
