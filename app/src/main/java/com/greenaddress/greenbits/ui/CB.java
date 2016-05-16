package com.greenaddress.greenbits.ui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


public final class CB {

    public static <T> void after(ListenableFuture<T> f,
                                 FutureCallback<? super T> cb) {
        Futures.addCallback(f, cb);
    }

    /** A FutureCallback that does nothing */
    public static class NoOp<T> implements FutureCallback<T> {

       @Override
       public void onSuccess(final T result) { /* No-op */ }

       @Override
       public void onFailure(final Throwable t) { /* No-op */ }
    }


    /** A FutureCallback that shows a toast on failure */
    public static class Toast<T> extends NoOp<T> {

       final GaActivity mActivity;

       Toast(final GaActivity activity) {
           super();
           mActivity = activity;
       }

       @Override
       final public void onFailure(final Throwable t) {
           t.printStackTrace();
           mActivity.runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   mActivity.toast(t.getMessage());
               }
           });
       }
    }
}
