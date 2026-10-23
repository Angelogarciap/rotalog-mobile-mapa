package com.rotalog.delivery.history

import com.rotalog.delivery.data.DemoRoutes
import org.junit.Assert.*
import org.junit.Test

class HistoryRulesTest {
    @Test fun completedHistoryIsIndependentFromCurrentRoute() {
        val route = DemoRoutes.active()
        assertEquals(1f, DemoRoutes.history().single().progress)
        assertTrue(DemoRoutes.history().none { it.id == route.id })
    }
}
