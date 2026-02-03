package com.koshaq.music.ui

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.koshaq.music.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CheckQueueTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.READ_EXTERNAL_STORAGE"
        )

    @Test
    fun checkQueueTest() {
        val playButton = onView(
            allOf(
                withId(R.id.btnPlay), withContentDescription("play"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.list),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        playButton.perform(click())

        val navQueue = onView(
            allOf(
                withId(R.id.nav_queue), withContentDescription("Queue"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavigation),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        navQueue.perform(click())

        val recyclerView = onView(
            allOf(
                withId(R.id.queueList),
                withParent(withParent(withId(R.id.container))),
                isDisplayed()
            )
        )
        recyclerView.check(matches(isDisplayed()))

        recyclerView.check(matches(hasDescendant(withId(R.id.title))))
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
