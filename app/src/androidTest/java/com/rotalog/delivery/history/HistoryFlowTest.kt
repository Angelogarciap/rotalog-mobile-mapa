package com.rotalog.delivery.history

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rotalog.delivery.MainActivity
import com.rotalog.delivery.RotalogApplication
import com.rotalog.delivery.data.DemoRoutes
import com.rotalog.delivery.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(100) // Let the device render thread present the settled frame.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scale = (context.resources.configuration.fontScale * 100).toInt()
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$name-$scale.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/RotaLogReview")
        }
        val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(context.contentResolver.openOutputStream(uri)).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    @Test fun reviewCompletedHistoryInBothThemesWithoutCurrentRoute() {
        compose.onNodeWithText("Explorar demonstração").performScrollTo().performClick()
        compose.onNode(hasText("Histórico") and hasClickAction()).performClick()
        val settings = (compose.activity.application as RotalogApplication).container.settings
        val previous = DemoRoutes.history().single()
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            runBlocking { settings.setTheme(theme) }
            compose.waitForIdle()
            compose.onNodeWithText("Seu caminho, entrega por entrega.").performScrollTo().assertIsDisplayed()
            capture("${theme.name.lowercase()}-history")
            compose.onNodeWithText(previous.code).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("${previous.completedStops} de ${previous.stops.size} paradas concluídas").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Registro demonstrativo").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(DemoRoutes.assigned().code).assertDoesNotExist()
        }
    }
}
