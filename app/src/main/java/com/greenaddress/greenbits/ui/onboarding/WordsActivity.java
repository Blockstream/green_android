package com.greenaddress.greenbits.ui.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.Arrays;
import java.util.List;

public class WordsActivity extends LoginActivity implements View.OnClickListener {
    private static final int REQUEST_SELECTION = 100;

    private String mMnemonic;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private String mSignUpMnemonic;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_words);
        setTitleBackTransparent();
        setTitle("");
        UI.preventScreenshots(this);

        // Generate mnemonic from GDK
        mMnemonic = getSignUpMnemonic();

        // Set up the action bar.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = UI.find(this, R.id.viewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Initialization
        mViewPager.setCurrentItem(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        UI.mapClick(this, R.id.nextButton, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        UI.unmapClick(UI.find(this, R.id.nextButton));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.onboarding_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
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
        if (mViewPager == null)
            super.onBackPressed();
        else if (mViewPager.getCurrentItem() == 0)
            super.onBackPressed();
        else
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECTION) {
            if (mViewPager == null)
                super.onBackPressed();
            else
                mViewPager.setCurrentItem(0);
        }
    }

    @Override
    public void onClick(final View view) {
        if (mViewPager.getCurrentItem() == mSectionsPagerAdapter.getCount() - 1) {
            final Intent intent = new Intent(this, SelectionActivity.class);
            intent.setData(Uri.parse(mMnemonic));
            startActivityForResult(intent, REQUEST_SELECTION);
        } else {
            final int page = mViewPager.getCurrentItem() + 1;
            mViewPager.setCurrentItem(page);
        }
    }

    public String getSignUpMnemonic() {
        if (mSignUpMnemonic == null)
            mSignUpMnemonic = GDKSession.generateMnemonic("en");
        return mSignUpMnemonic;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final int FRAGMENTS_NUMBER = 4;
        private final int WORDS_FOR_FRAGMENT = 6;

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int index) {
            if (index < FRAGMENTS_NUMBER) {
                // pass splitted words list to fragment
                final int start = index * WORDS_FOR_FRAGMENT;
                final int end = start + WORDS_FOR_FRAGMENT;
                final List<String> words = Arrays.asList(mMnemonic.split(" ")).subList(start, end);
                return WordsFragment.newInstance(words, start);
            }
            return null;
        }

        @Override
        public int getCount() {
            return FRAGMENTS_NUMBER;
        }
    }

}