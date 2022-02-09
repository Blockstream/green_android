package com.blockstream.green.lifecycle;

import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/*
 * An easy way to identify if a value has been set
 */
public class PendingLiveData<T> extends MutableLiveData<T> {

    private Boolean mIsPending = true;

    public PendingLiveData() {
    }

    public PendingLiveData(T value) {
        super(value);
        mIsPending = false;
    }

    @Override
    public void postValue(T value) {
        mIsPending = false;
        super.postValue(value);
    }

    @Override
    public void setValue(T value) {
        mIsPending = false;
        super.setValue(value);
    }

    public Boolean isPending(){
        return mIsPending;
    }

    public Boolean isReadyAndNull(){
        return !mIsPending && getValue() == null;
    }
}