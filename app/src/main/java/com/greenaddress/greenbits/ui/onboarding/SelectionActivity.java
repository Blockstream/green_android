package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.blockstream.libgreenaddress.GDK;
import com.blockstream.libwally.Wally;

import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;



public class SelectionActivity extends LoginActivity implements View.OnClickListener {
    private String mMnemonic;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_selection);
        setTitleBackTransparent();
        setTitle("");

        UI.preventScreenshots(this);
        // Generate mnemonic from GDK
        try {
            mMnemonic = getIntent().getDataString();
            Wally.bip39_mnemonic_validate(Wally.bip39_get_wordlist("en"), mMnemonic);
        } catch (final Exception e) {
            finish();
            return;
        }

        // Set up the action bar.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Initialization
        mSectionsPagerAdapter.selectFragment(this, 0);
    }

    private void onMnemonicVerified() {
        startLoading();
        Observable.just(getSession())
        .observeOn(Schedulers.computation())
        .map((session) -> {
            session.disconnect();
            connect();
            getSession().registerUser(null, mMnemonic).resolve(null, null);
            session.loginWithMnemonic(mMnemonic, "");
            return session;
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe((session) -> {
            onPostLogin();
            stopLoading();
            final Intent savePin = PinSaveActivity.createIntent(this, mMnemonic);
            startActivity(savePin);
        }, (final Throwable e) -> {
            stopLoading();
            getSession().disconnect();
            final Integer code = getSession().getErrorCode(e.getMessage());
            if (code == GDK.GA_ERROR) {
                UI.toast(this, R.string.id_login_failed, Toast.LENGTH_LONG);
            } else {
                UI.toast(this, R.string.id_connection_failed, Toast.LENGTH_LONG);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final int selected = mSectionsPagerAdapter.getSelected();
        if (selected == 0)
            super.onBackPressed();
        else
            mSectionsPagerAdapter.selectFragment(this, selected - 1);
    }

    @Override
    public void onClick(final View view) {
        if (isLoading()) {
            return;
        }
        final int selected = mSectionsPagerAdapter.getSelected();
        final String wordExpected = mSectionsPagerAdapter.getFragment(selected)
                                    .getArguments().getString("word");

        // Press string selection button
        final String wordSelected = ((Button) view).getText().toString();

        final boolean youWin = wordExpected.equals(wordSelected);
        if (youWin) {
            // if right, continue or finish (if completed)
            if (selected == mSectionsPagerAdapter.getCount() - 1)
                onMnemonicVerified();
            else
                mSectionsPagerAdapter.selectFragment(this, selected + 1);
        } else {
            // if fails, go to the previous page
            UI.toast(this, R.string.id_wrong_choice_check_your, Toast.LENGTH_LONG);
            finish();
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final int FRAGMENTS_NUMBER = 4;
        private final SelectionFragment[] fragments = new SelectionFragment[4];
        private int selected = 0;
        private List<Integer> indexes;
        private List<String> words;

        SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);

            words = Arrays.asList(mMnemonic.split(" "));
            final Random random = new Random();
            final Set<Integer> iSet = new HashSet<>();
            while (iSet.size() < 4) {
                iSet.add(random.nextInt(words.size()));
            }
            this.indexes = new ArrayList<>(iSet);

        }

        @Override
        public Fragment getItem(final int index) {
            if (index < FRAGMENTS_NUMBER) {
                // pass splitted words list to fragment
                final String random = words.get(indexes.get(index));
                fragments[index] = SelectionFragment.newInstance(words, random);
                return fragments[index];
            }
            return null;
        }

        @Override
        public int getCount() {
            return FRAGMENTS_NUMBER;
        }

        int getSelected() {
            return selected;
        }

        SelectionFragment getFragment(final int index) {
            return fragments[index];
        }

        void selectFragment(final AppCompatActivity activity, final int index) {
            selected = index;
            activity.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, getItem(index))
            .commit();
        }
    }
}
