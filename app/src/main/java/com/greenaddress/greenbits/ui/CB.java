package com.greenaddress.greenbits.ui;

import android.widget.Button;

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


    /** A FutureCallback that shows a toast (and optionally
     *  enables a button) on failure
     */
    public static class Toast<T> extends NoOp<T> {

       final GaActivity mActivity;
       final Button mEnabler;

       Toast(final GaActivity activity) {
           super();
           mActivity = activity;
           mEnabler = null;
       }

       Toast(final GaActivity activity, Button enabler) {
           super();
           mActivity = activity;
           mEnabler = enabler;
       }

       @Override
       final public void onFailure(final Throwable t) {
           mActivity.toast(t, mEnabler);
       }
    }

    /** A runnable that takes 1 argument */
    public interface Runnable1T<T> {
        void run(final T arg);
    }
}
