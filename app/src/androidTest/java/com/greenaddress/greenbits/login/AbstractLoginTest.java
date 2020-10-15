package com.greenaddress.greenbits.login;

import android.Manifest;
import android.app.Activity;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
//import androidx.v4.app.DialogFragment;
//import androidx.v7.preference.Preference;
//import androidx.v7.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.authentication.FirstScreenActivity;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.NetworkSettingsActivity;
import com.greenaddress.greenbits.ui.accounts.SwitchNetworkAdapter;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.greenaddress.greenbits.ui.onboarding.SelectionFragment;

import androidx.test.espresso.matcher.BoundedMatcher;
import android.widget.ProgressBar;
import android.widget.Switch;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;


import static android.content.Context.MODE_PRIVATE;
import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.greenaddress.greenbits.utils.WaitForLoading.waitForLoading;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public abstract class AbstractLoginTest extends TestCase {

    @Rule
    public ActivityTestRule<FirstScreenActivity> activityRule
            = new ActivityTestRule<>(FirstScreenActivity.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    static Matcher<RecyclerView.ViewHolder> matchButtonText(final String text) {
        return new BoundedMatcher<RecyclerView.ViewHolder, SwitchNetworkAdapter.ViewHolder>(SwitchNetworkAdapter.ViewHolder.class) {
            @Override
            public void describeTo(Description description) {}

            @Override
            protected boolean matchesSafely(SwitchNetworkAdapter.ViewHolder item) {
                return item.getText().equals(text);
            }
        };
    }

    @Before
    public void setup_regtest() {
        JsonNode customGdkNetwork = ((ObjectNode) GDK.get_networks()).get("regtest");
        ObjectNode regtest = (ObjectNode) customGdkNetwork;
        regtest.remove("wamp_url");
        regtest.put("name", "customregtest");
        regtest.put("wamp_url", "");
        GDK.register_network("customregtest", regtest);
    }

    static Matcher<RecyclerView.ViewHolder> matchCorrectWord() {
        return new BoundedMatcher<RecyclerView.ViewHolder, SelectionFragment.SelectionViewAdapter.ViewHolder>(SelectionFragment.SelectionViewAdapter.ViewHolder.class) {
            @Override
            public void describeTo(Description description) {}

            @Override
            protected boolean matchesSafely(SelectionFragment.SelectionViewAdapter.ViewHolder item) {
                return item.wordButton.isSelected(); // in debug mode the correct word is "selected"
            }
        };
    }

    private Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    public void setNetwork(final String networkName) {
        onView(withId(R.id.settingsButton)).perform(click());
        Matcher<RecyclerView.ViewHolder> networkSettingsMatcher = matchButtonText(networkName);
        onView(withId(R.id.networksRecyclerView)).perform(actionOnHolderItem(networkSettingsMatcher, click()));
        onView(withId(R.id.selectNetworkButton)).perform(scrollTo(), click());
    }

    protected void setPin(final String pin) {
        onView(withContentDescription("PIN")).perform(typeText(pin));
        onView(withContentDescription("PIN")).perform(typeText(pin));
    }

    public void createNewWallet() {
        onView(withId(R.id.loginButton)).perform(click());
        // activity_onboarding_info
        onView(withId(R.id.continueButton)).perform(scrollTo(), click());
        // activity_onboarding_words
        for (int i = 0; i < 4; i++) {
            onView(withId(R.id.nextButton)).perform(click());
        }
        // activity_onboard_selection
        Matcher<RecyclerView.ViewHolder> mnemonicWordMatcher = matchCorrectWord();
        for (int i = 0; i < 4; i++) {
            onView(withId(R.id.selectionRecyclerView)).perform(actionOnHolderItem(mnemonicWordMatcher, click()));
        }
        IdlingRegistry.getInstance().register(waitForLoading());
        setPin("000000");
        IdlingRegistry.getInstance().register(waitForLoading());
        // activity_onboard_security
        onView(withId(R.id.nextButton)).perform(click());

    }

    protected void disablePIN(final String network) {
        InstrumentationRegistry.getTargetContext().getSharedPreferences(network + "_pin", MODE_PRIVATE).edit().clear().commit();
    }

	void loginWatchOnly(final String user, final String password) {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(allOf(withId(R.id.title), withText("Watch-only login"), isDisplayed()))
                .perform(click());
        IdlingRegistry.getInstance().register(waitForLoading());

        // type watch-only user and password
        onView(withId(R.id.input_user)).perform(typeText(user));
        onView(withId(R.id.input_user)).perform(closeSoftKeyboard());
        onView(withId(R.id.input_password)).perform(typeText(password));
        onView(withId(R.id.input_password)).perform(closeSoftKeyboard());

        // click login button
        onView(withId(R.id.btn_login)).perform(click());
    }

    public void restoreWallet(final String[] mnemonic) {
        int[] rows = {0, 1, 2, 3, 4, 5, 6, 7};
        int[] columns = {1, 3, 5};
        int mnemonicIndex = 0;

        onView(withId(R.id.restoreButton)).perform(click());

        // Enter mnemonic passphrase
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < columns.length; j++) {
                ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                        childAtPosition(
                                childAtPosition(withId(R.id.mnemonic24), rows[i]),
                                columns[j]));
                appCompatMultiAutoCompleteTextView.perform(scrollTo(),
                        replaceText(mnemonic[mnemonicIndex]), closeSoftKeyboard());
                mnemonicIndex++;
            }
        }

        // Restore and set a PIN
        onView(withId(R.id.mnemonicOkButton)).perform(click());
        IdlingRegistry.getInstance().register(waitForLoading());
        setPin("000000");
        IdlingRegistry.getInstance().register(waitForLoading());
        // activity_onboard_security
        onView(withId(R.id.nextButton)).perform(click());
    }
}
