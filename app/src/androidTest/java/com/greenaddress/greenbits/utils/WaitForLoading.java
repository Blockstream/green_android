package com.greenaddress.greenbits.utils;

import androidx.test.espresso.IdlingResource;

import static com.greenaddress.greenbits.utils.TestUtils.getCurrentActivity;

public class WaitForLoading implements IdlingResource {
    private ResourceCallback resourceCallback;
    private boolean isIdle;

    public static WaitForLoading waitForLoading() {
        return new WaitForLoading();
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public boolean isIdleNow() {
        if (isIdle) return true;
        if (getCurrentActivity() == null) return false;

        isIdle = !getCurrentActivity().isLoading();
        if (isIdle) {
            resourceCallback.onTransitionToIdle();
        }
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }
}