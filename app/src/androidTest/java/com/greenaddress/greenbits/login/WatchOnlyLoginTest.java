package com.greenaddress.greenbits.login;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WatchOnlyLoginTest extends AbstractLoginTest {
    @Test
    public void loginWatchOnlyTestnet() {
        setNetwork("Testnet");
        loginWatchOnly("testnet", "testnet");
    }
}
