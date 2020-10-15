package com.greenaddress.greenbits.login;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RestoreWalletTest extends AbstractLoginTest {
    @Test
    public void TestRestoreWallet() {
        String[] mnemonic = BuildConfig.TESTNET_SEED.split(" ");
        setNetwork("Testnet");
        restoreWallet(mnemonic);
        onView(withId(R.id.receiveButton)).check(matches(isDisplayed()));
    }
}
