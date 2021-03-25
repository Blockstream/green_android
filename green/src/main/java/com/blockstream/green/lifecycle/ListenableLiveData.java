package com.blockstream.green.lifecycle;

import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/*
 * Invoke a listener every time LiveData is changed
 */
public class ListenableLiveData<T> extends MutableLiveData<T> {

    private final Function1<T, Unit> mListener;

    public ListenableLiveData(T value, @NotNull Function1<T, Unit> listener) {
        super(value);
        mListener = listener;
        mListener.invoke(value);
    }

    @Override
    public void postValue(T value) {
        super.postValue(value);
        mListener.invoke(value);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
        mListener.invoke(value);
    }
}