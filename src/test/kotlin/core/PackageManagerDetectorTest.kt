package core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackageManagerDetectorTest {

    @Test
    fun `detect falls back to NPM when no lockfiles`() {
        val pm = PackageManagerDetector.detect(root = null)
        assertEquals(PackageManager.NPM, pm)
    }

    @Test
    fun `command builds npx on unix and npx cmd on win (npm)`() {
        val args = listOf("sequelize-cli", "db:migrate", "--env", "development")

        val winCmd = withOs("Windows 11") { PackageManagerDetector.command(PackageManager.NPM, args) }
        val unixCmd = withOs("Linux") { PackageManagerDetector.command(PackageManager.NPM, args) }

        assertEquals("""npx.cmd sequelize-cli db:migrate --env development""", winCmd)
        assertEquals("""npx sequelize-cli db:migrate --env development""", unixCmd)
    }

    @Test
    fun `pnpm uses dlx wrapper`() {
        val args = listOf("sequelize-cli", "seed:generate", "--name", "x", "--env", "test")
        val winCmd = withOs("Windows 10") { PackageManagerDetector.command(PackageManager.PNPM, args) }
        val unixCmd = withOs("Mac OS X") { PackageManagerDetector.command(PackageManager.PNPM, args) }

        assertEquals("""pnpm.cmd dlx sequelize-cli seed:generate --name x --env test""", winCmd)
        assertEquals("""pnpm dlx sequelize-cli seed:generate --name x --env test""", unixCmd)
    }

    /** Helper to simulate OS for this call only */
    private inline fun <T> withOs(osName: String, block: () -> T): T {
        val key = "os.name"
        val prev = System.getProperty(key)
        System.setProperty(key, osName)
        return try { block() } finally {
            if (prev == null) System.clearProperty(key) else System.setProperty(key, prev)
        }
    }
}
