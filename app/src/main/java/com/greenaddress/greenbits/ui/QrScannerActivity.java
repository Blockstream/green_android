package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.google.zxing.Result;
import com.greenaddress.greenbits.GreenAddressApplication;

import java.util.Observable;
import java.util.Observer;


public class QrScannerActivity extends ActionBarActivity implements ZXingScannerView.ResultHandler, Observer {

    ZXingScannerView mScannerView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().addObserver(this);

        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        ((GreenAddressApplication) getApplication()).getConnectionObservable().deleteObserver(this);

        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(final Result result) {
        final Intent intent = new Intent();
        intent.putExtra("com.greenaddress.greenbits.QrText", result.getText());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void update(Observable observable, Object data) {

    }
}
