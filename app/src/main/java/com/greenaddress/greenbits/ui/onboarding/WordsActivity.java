package com.greenaddress.greenbits.ui.onboarding;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

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
    private Dialog mMnemonicDialog;
    private ImageView mQrCodeBitmap;
    private ImageView mProgressImage;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_words);
        setTitleBackTransparent();
        setTitle("");
        UI.preventScreenshots(this);

        // Generate mnemonic from GDK
        mMnemonic = mService.getSignUpMnemonic();

        // Set up the action bar.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = UI.find(this, R.id.viewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Initialization
        mViewPager.setCurrentItem(0);
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        UI.mapClick(this, R.id.nextButton, this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        UI.unmapClick(UI.find(this, R.id.nextButton));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMnemonicDialog = UI.dismiss(this, mMnemonicDialog);
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
        case R.id.action_qr:
            onQrCodeButtonClicked();
            return true;
        case R.id.action_nfc:
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

    private void onQrCodeButtonClicked() {
        if (mMnemonicDialog == null) {
            final View v = UI.inflateDialog(this, R.layout.dialog_qrcode);
            mQrCodeBitmap = UI.find(v, R.id.qrInDialogImageView);
            mQrCodeBitmap.setLayoutParams(UI.getScreenLayout(this, 0.8));
            mMnemonicDialog = new Dialog(this);
            mMnemonicDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            mMnemonicDialog.setContentView(v);
        }
        mMnemonicDialog.show();
        final BitmapDrawable bd = new BitmapDrawable(getResources(), mService.getSignUpQRCode());
        bd.setFilterBitmap(false);
        mQrCodeBitmap.setImageDrawable(bd);
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