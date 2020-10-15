package com.greenaddress.greenbits.utils;

import android.app.Activity;
import android.view.View;

import org.hamcrest.Matcher;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.greenaddress.greenbits.ui.GaActivity;

import java.util.Collection;
import java.util.Iterator;

import androidx.test.espresso.ViewAction;
import android.widget.TextView;
import androidx.test.espresso.UiController;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

public class TestUtils {
    public static GaActivity getCurrentActivity() {
        Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        Iterator<Activity> it = resumedActivity.iterator();
        return (GaActivity) (it.hasNext() ? it.next() : null);
    }

    public static String getText(final Matcher<View> matcher) {
        final String[] textHolder = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "get text from TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view;
                textHolder[0] = tv.getText().toString();
            }
        });
        return textHolder[0];
    }
}
