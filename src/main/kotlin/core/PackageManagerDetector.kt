package core

import com.intellij.openapi.vfs.VirtualFile

object PackageManagerDetector {
    fun detect(root: VirtualFile?): PackageManager =
        when {
            root?.findChild("pnpm-lock.yaml") != null   -> PackageManager.PNPM
            root?.findChild("yarn.lock") != null        -> PackageManager.YARN
            else                                                -> PackageManager.NPM
        }

    /** Build a single shell command string, ready for the IDE Terminal */
    fun command(pm: PackageManager, args: List<String>): String {
        val isWin = System.getProperty("os.name").lowercase().contains("win")
        val bin = when (pm) {
            PackageManager.NPM  -> if (isWin) "npx.cmd"  else "npx"
            PackageManager.YARN -> if (isWin) "yarn.cmd" else "yarn"
            PackageManager.PNPM -> if (isWin) "pnpm.cmd" else "pnpm"
        }
        val body = when (pm) {
            PackageManager.PNPM -> "dlx " + args.joinToString(" ") { shellQuote(it, isWin) }
            else -> args.joinToString(" ") { shellQuote(it, isWin) }
        }
        return "$bin $body"
    }

    private fun shellQuote(s: String, isWin: Boolean): String {
        val needs = s.any { it.isWhitespace() || "\"'&|><".contains(it) }
        return if (!needs) s else if (isWin) "\"${s.replace("\"", "\\\"")}\"" else "'${s.replace("'", "'\\''")}'"
    }
}
