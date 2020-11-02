package com.greenaddress.gdk;

import com.google.common.util.concurrent.SettableFuture;

import java.util.List;

public interface MethodResolver {
    SettableFuture<String> method(final List<String> methods);
    void dismiss();
}
