package core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvManagerTest {

    @Test
    fun `default env is development`() {
        val s = EnvManager.State()
        assertEquals("development", s.environment)
    }

    @Test
    fun `setter updates state`() {
        val mgr = EnvManager() // By default : internal status
        mgr.set("production")
        assertEquals("production", mgr.get())
    }
}
