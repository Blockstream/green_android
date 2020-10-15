package com.greenaddress.greenbits.send;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.greenaddress.greenbits.login.AbstractLoginTest;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.utils.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SendTest extends AbstractLoginTest {

    public void setReceiverAddress(String address) {
        onView(withId(R.id.sendButton)).check(matches(isDisplayed()));
        onView(withId(R.id.sendButton)).perform(click());
        onView(withId(R.id.addressEdit)).perform(typeText(address));
        onView(withId(R.id.addressEdit)).perform(closeSoftKeyboard());
        onView(withId(R.id.nextButton)).perform(click());
    }

    public String getReceiveAddress() {
        String address;
        onView(withId(R.id.receiveButton)).perform(click());
        address = TestUtils.getText(withId(R.id.receiveAddressText));
        pressBack();
        return address;
    }

    @Test
    public void addressTest() {
        String address, addressShown;
        setNetwork("Testnet");
        createNewWallet();
        // get an address
        address = getReceiveAddress();
        setReceiverAddress(address);
        // check address matches the one typed
        assert address == TestUtils.getText(withId(R.id.receiveAddressText));
    }

    @Test
    public void bitcoinUriTest() {
        String uriAmount = "1.2345";
        String bitcoinUri;
        double amount = Double.parseDouble(uriAmount);
        setNetwork("Testnet");
        createNewWallet();
        bitcoinUri = "bitcoin:" + getReceiveAddress() + "?amount=" + uriAmount;
        setReceiverAddress(bitcoinUri);
        assert amount == Double.parseDouble(TestUtils.getText(withId(R.id.receiveAddressText)));
    }

    @Test
    public void sendAllTest() {
        String address;
        String[] mnemonic = BuildConfig.TESTNET_SEED.split(" ");
        double amountAvailable;
        setNetwork("Testnet");
        restoreWallet(mnemonic);
        address = getReceiveAddress();
        setReceiverAddress(address);
        // check available balance is higher than max send amount
        amountAvailable = Double.parseDouble(TestUtils.getText(withId(R.id.accountBalanceText))
                .split(" ")[0]);
        onView(withId(R.id.sendallButton)).perform(click());
        assert amountAvailable > Double.parseDouble(TestUtils.getText(withId(R.id.receiveAddressText)));
    }

    @Test
    public void sendConfirmAmountTest() {
        String uriAmount = "0.00001";
        double confirmedAmount;
        String[] mnemonic = BuildConfig.TESTNET_SEED.split(" ");
        setNetwork("Testnet");
        restoreWallet(mnemonic);
        String bitcoinUri = "bitcoin:" + getReceiveAddress() + "?amount=" + uriAmount;
        setReceiverAddress(bitcoinUri);
        onView(withId(R.id.nextButton)).perform(click());
        confirmedAmount = Double.parseDouble(TestUtils.getText(withId(R.id.sendAmount)).split(" ")[0]);
        assert confirmedAmount == Double.parseDouble(uriAmount);
    }
}
