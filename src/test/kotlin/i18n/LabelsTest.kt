package i18n

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LabelsTest {

    @Test
    fun `returns key when missing`() {
        val s = Labels.t("unknown.key")
        assertEquals("unknown.key", s)
    }

    @Test
    fun `simple fetch from json`() {
        val title = Labels.t("appTitle")
        assertTrue(title.isNotBlank())
    }

    @Test
    fun `template replacement works`() {
        val s = Labels.t("footerStatusTemplate", "pm" to "NPM", "env" to "development")
        assertEquals("PM: NPM • Env: development", s)
    }
}
