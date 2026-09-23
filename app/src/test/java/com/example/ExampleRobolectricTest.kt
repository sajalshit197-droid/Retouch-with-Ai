package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.PhotoProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RetouchAI", appName)
  }

  @Test
  fun `verify available filters exist`() {
    assertTrue(PhotoProcessor.AVAILABLE_FILTERS.isNotEmpty())
    assertTrue(PhotoProcessor.AVAILABLE_FILTERS.any { it.id == "warm_studio" })
  }
}

