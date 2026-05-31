package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.ui.EyeBreakApp
import com.example.ui.EyeViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Eye Break Assistant", appName)
  }

  @Test
  fun `app renders dashboard without crash`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = EyeViewModel(app)
    
    composeTestRule.setContent {
      MyApplicationTheme {
        EyeBreakApp(viewModel = viewModel)
      }
    }
    
    composeTestRule.onRoot().assertExists()
  }
}

