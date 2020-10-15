package com.greenaddress.greenbits.utils;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.matcher.BoundedMatcher;


import org.hamcrest.Description;

public class RecyclerViewAssertions {
    public static class RecyclerViewItemCount extends BoundedMatcher<View, RecyclerView> {
        public static RecyclerViewItemCount withItemCount(int count) {
            return new RecyclerViewItemCount(count);
        }

        private final int count;

        private RecyclerViewItemCount(int count) {
            super(RecyclerView.class);

            this.count = count;
        }

        @Override
        protected boolean matchesSafely(RecyclerView rv) {
            return rv.getAdapter().getItemCount() == count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("with item count:")
                    .appendValue(count);
        }
    }
}