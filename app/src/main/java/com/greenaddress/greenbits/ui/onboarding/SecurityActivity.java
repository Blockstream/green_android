package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.model.TwoFactorConfigDataObservable;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.twofactor.TwoFactorActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class SecurityActivity extends LoggedActivity implements View.OnClickListener, Observer {
    private static final int REQUEST_2FA = 100;
    private ViewAdapter mMethodsAdapter;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (getModel() == null) {
            toFirst();
            return;
        }

        setContentView(R.layout.activity_onboarding_security);
        setTitle("");
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));

        final String[] choices = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] methods = getResources().getStringArray(R.array.twoFactorMethods);
        final Integer[] images = {
            R.drawable.ic_2fa_email,
            R.drawable.ic_2fa_sms,
            R.drawable.ic_2fa_call,
            R.drawable.ic_2fa_google
        };

        if (!isFromOnboarding()) {
            UI.hide(UI.find(this, R.id.nextButton));
            setTitleBack();
        }

        mMethodsAdapter = new ViewAdapter(this,
                                          Arrays.asList(methods),
                                          Arrays.asList(choices),
                                          Arrays.asList(images));
        final RecyclerView wordsRecyclerView = UI.find(this, R.id.twoFactorsRecyclerView);
        wordsRecyclerView.setHasFixedSize(true);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        wordsRecyclerView.setAdapter(mMethodsAdapter);
        initEnabledMethods();
    }

    private boolean isFromOnboarding() {
        return getIntent().getBooleanExtra("from_onboarding", false);
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        UI.mapClick(this, R.id.nextButton, this);
        if (getModel() != null) {
            initEnabledMethods();
            getModel().getTwoFactorConfigDataObservable().addObserver(this);
        }
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        UI.unmapClick(UI.find(this, R.id.nextButton));
        if (getModel() != null)
            getModel().getTwoFactorConfigDataObservable().deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View view) {
        goToTabbedMainActivity();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_2FA && resultCode == RESULT_OK) {
            getModel().getTwoFactorConfigDataObservable().refresh();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TwoFactorConfigDataObservable) {
            initEnabledMethods();
        }
    }

    private void initEnabledMethods() {
        final TwoFactorConfigData twoFactorConfig = getModel().getTwoFactorConfig();
        if (twoFactorConfig == null)
            return;
        final List<String> enabledMethods = twoFactorConfig.getEnabledMethods();
        mMethodsAdapter.setEnabled(enabledMethods);
        if (isFromOnboarding() && twoFactorConfig.getAllMethods().size() == enabledMethods.size()) {
            // The user has enabled all methods, so continue to the main activity
            finish();
            goToTabbedMainActivity();
        }
    }

    @Override
    public void onBackPressed() {
        if (isFromOnboarding())
            goToTabbedMainActivity();
        else
            super.onBackPressed();
    }

    class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder> {

        private final List<String> mMethods;
        private final List<String> mChoices;
        private final List<Integer> mImages;
        private final LayoutInflater mInflater;
        private Set<String> mEnabled;

        ViewAdapter(final Context context, final List<String> methods,
                    final List<String> choices, final List<Integer> images) {
            mInflater = LayoutInflater.from(context);
            mMethods = methods;
            mChoices = choices;
            mImages = images;
            mEnabled = new HashSet<>();
        }

        @Override
        public ViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = mInflater.inflate(R.layout.list_element_two_factor, parent, false);
            return new ViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewAdapter.ViewHolder holder, final int position) {
            final String method = mMethods.get(position);
            holder.nameText.setText(mChoices.get(position));
            holder.imageView.setImageResource(mImages.get(position));
            final boolean isEnabled = mEnabled.contains(method);
            holder.enabled.setChecked(isEnabled);
            holder.enabled.setOnClickListener((v1) -> {
                if (getGAApp().warnIfOffline(SecurityActivity.this)) {
                    return;
                }
                final Intent intent = new Intent(SecurityActivity.this, TwoFactorActivity.class);
                intent.putExtra("method", method);
                intent.putExtra("enable", !isEnabled);
                startActivityForResult(intent, REQUEST_2FA);
            });
        }

        @Override
        public int getItemCount() {
            return mChoices.size();
        }

        public void setEnabled(final List<String> methods) {
            mEnabled = new HashSet<>(methods);
            runOnUiThread(() -> mMethodsAdapter.notifyDataSetChanged());
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView nameText;
            public ImageView imageView;
            public Switch enabled;

            ViewHolder(final View itemView) {
                super(itemView);
                nameText = UI.find(itemView, R.id.nameText);
                imageView = UI.find(itemView, R.id.imageView);
                enabled = UI.find(itemView, R.id.switchEnabled);
            }
        }
    }
}
