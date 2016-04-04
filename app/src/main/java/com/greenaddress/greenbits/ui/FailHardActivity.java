package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

public class FailHardActivity extends AppCompatActivity {
    private final static String TAG = FailHardActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail_hard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent i = getIntent();
        Log.e(TAG, i.getStringExtra("errorTitle"));
        Log.e(TAG, i.getStringExtra("errorContent"));

        (new MaterialDialog.Builder(this)
                .title(i.getStringExtra("errorTitle"))
                .content(i.getStringExtra("errorContent"))
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.white)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .positiveText("CLOSE")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull final DialogAction which) {
                        final Intent main = new Intent(Intent.ACTION_MAIN);
                        main.addCategory(Intent.CATEGORY_HOME);
                        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(main);
                        finish();
                    }
                })).build().show();
    }
}
