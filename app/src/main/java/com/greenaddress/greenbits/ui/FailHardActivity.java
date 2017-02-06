package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class FailHardActivity extends AppCompatActivity {
    private final static String TAG = FailHardActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail_hard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Intent i = getIntent();
        Log.e(TAG, i.getStringExtra("errorTitle") + ":" + i.getStringExtra("errorContent"));

        UI.popup(this, i.getStringExtra("errorTitle"))
                  .content(i.getStringExtra("errorContent"))
                  .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dlg, final DialogAction which) {
                        final Intent main = new Intent(Intent.ACTION_MAIN);
                        main.addCategory(Intent.CATEGORY_HOME);
                        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(main);
                        finish();
                    }
                  }).build().show();
    }
}
