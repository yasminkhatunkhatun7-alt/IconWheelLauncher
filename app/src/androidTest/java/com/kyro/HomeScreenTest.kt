package com.kyro

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.kyro.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationTabs() {
        composeTestRule.setContent {
            HomeScreen()
        }

        // Initially HOME tab content should be visible
        composeTestRule.onNodeWithText("TAP TO WAKE").assertIsDisplayed()

        // Click on CHAT tab
        // There are two nodes with text "CHAT": the tab and the content
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("CHAT"))[0].performClick()
    }
}
