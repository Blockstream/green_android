package com.greenaddress.greenbits;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.SettingsActivity;

import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class GreenAddressApplication extends MultiDexApplication {

    public GaService gaService;
    public SettableFuture<Void> onServiceConnected = SettableFuture.create();
    private boolean mBound = false;
    private ConnectivityObservable connectionObservable = new ConnectivityObservable();
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            final GaBinder binder = (GaBinder) service;
            gaService = binder.gaService;
            mBound = true;
            connectionObservable.setService(gaService);
            onServiceConnected.set(null);
        }

        @Override
        public void onServiceDisconnected(final ComponentName arg0) {
            mBound = false;
            connectionObservable = null;
            onServiceConnected.setException(new GAException(arg0.toString()));
        }
    };

    public ConnectivityObservable getConnectionObservable() {
        return connectionObservable;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        final Intent intent = new Intent(this, GaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void configureSubaccountsFooter(final int curSubaccount, final Activity activity, final TextView accountName, final LinearLayout footer, final LinearLayout clickableArea, final Function<Integer, Void> accountChangedCallback, final View noTwoFacFooter) {
        final Handler handler = new Handler();
        gaService.getTwoFacConfigObservable().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        configureNoTwoFacFooter(noTwoFacFooter, activity);
                    }
                });
            }
        });
        if (gaService.getTwoFacConfig() != null) {
            configureNoTwoFacFooter(noTwoFacFooter, activity);
        }
        if (gaService.getSubaccounts().size() > 0) {
            accountName.setText(getResources().getText(R.string.main_account));
            final ArrayList subaccounts = gaService.getSubaccounts();
            for (Object subaccount : subaccounts) {
                Map<String, ?> subaccountMap = (Map) subaccount;
                final String name = (String) subaccountMap.get("name");
                if (subaccountMap.get("pointer").equals(curSubaccount)) {
                    accountName.setText(name);
                }
            }
            footer.setVisibility(View.VISIBLE);
            clickableArea.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final PopupMenu popup = new PopupMenu(activity, view);
                    int i = 0;
                    popup.getMenu().add(0, i, i, getResources().getText(R.string.main_account));
                    final ArrayList subaccounts = gaService.getSubaccounts();
                    for (Object subaccount : subaccounts) {
                        i += 1;
                        Map<String, ?> subaccountMap = (Map) subaccount;
                        final String name = (String) subaccountMap.get("name");
                        popup.getMenu().add(0, i, i, name);
                    }
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            accountName.setText(item.getTitle());
                            int curSubaccount;
                            if (item.getItemId() == 0) {
                                curSubaccount = 0;
                            } else {
                                curSubaccount = ((Number) ((Map<String, ?>) subaccounts.get(item.getItemId() - 1)).get("pointer")).intValue();
                            }
                            accountChangedCallback.apply(curSubaccount);
                            return false;
                        }
                    });
                    popup.show();
                }
            });
        }
    }

    private void configureNoTwoFacFooter(View noTwoFacFooter, final Activity activity) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean twoFacWarning = sharedPref.getBoolean("twoFacWarning", false);

        if (((Boolean) gaService.getTwoFacConfig().get("any")).booleanValue() || twoFacWarning) {
            noTwoFacFooter.setVisibility(View.GONE);
        } else {
            noTwoFacFooter.setVisibility(View.VISIBLE);
            noTwoFacFooter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.startActivity(new Intent(activity, SettingsActivity.class));
                }
            });
        }
    }
}