package com.greenaddress.greenbits.receive;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.greenaddress.greenbits.login.AbstractLoginTest;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.utils.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReceiveTest extends AbstractLoginTest {
    @Test
    public void getNewAddressTest() {
        String address, new_address;
        // create and login into a new testnet wallet
        setNetwork("Testnet");
        createNewWallet();

        onView(withId(R.id.receiveButton)).perform(click());
        address = TestUtils.getText(withId(R.id.receiveAddressText));
        // generate new addresses and check its different from the previous one
        onView(withId(R.id.action_generate_new)).perform(click());
        new_address = TestUtils.getText(withId(R.id.receiveAddressText));
        assert !(address == new_address);
    }

    @Test
    public void bitcoinUriTest() {
        String typedAmount = "12345";
        String uriAmount;
        // create and login into a new testnet wallet
        setNetwork("Testnet");
        createNewWallet();
        // type an amount in the text field
        onView(withId(R.id.receiveButton)).perform(click());
        onView(withId(R.id.amountEditText)).perform(typeText(typedAmount));
        // check that typed and shown correspond
        uriAmount = TestUtils.getText(withId(R.id.receiveAddressText)).split("=")[1];
        assert uriAmount == typedAmount;
    }
}
