package com.greenaddress.greenbits.login;

import android.view.View;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.greenaddress.greenbits.ui.R;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.greenaddress.greenbits.utils.RecyclerViewAssertions.RecyclerViewItemCount.withItemCount;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NewTestnetWalletTest extends AbstractLoginTest {
    @Test
    public void newTestnetWallet() {
        setNetwork("Testnet");
        createNewWallet();

        onView(withId(R.id.mainBalanceText)).check(matches(withText("0.00000000")));
        onView(withId(R.id.mainBalanceUnitText)).check(matches(withText(" BTC")));

        onView(withId(R.id.mainTransactionList)).check(matches((Matcher<? super View>) withItemCount(0)));
        // old line:
        //        onView(withId(R.id.mainTransactionList)).check(matches(withItemCount(0)));
    }

}
