/**
 * ModuleKindDetector.kt
 * ----------------------
 * Detects whether a Node.js project is using **ESM (ECMAScript Modules)** or **CommonJS (CJS)**
 * and recommends the appropriate file extension for Sequelize migrations.
 *
 * Detection strategy (in order of precedence):
 *  1) If `package.json` contains `"type": "module"`, prefer **ESM** (default extension: `.js`).
 *  2) If the `migrations/` directory already contains files, honor historical usage:
 *       - Any `.mjs`  → **ESM** with `.mjs`
 *       - Any `.cjs`  → **CJS** with `.cjs`
 *       - Any `.js`   → keep decision from (1); otherwise default to **CJS** with `.js`
 *  3) If nothing is found, fall back to **CJS** with `.js`.
 *
 * Notes:
 *  - For native ESM projects, `.js` is a perfectly valid extension when `"type":"module"` is present.
 *    We still switch to `.mjs` if historical files in `migrations/` already use `.mjs`.
 *  - This detector does **not** parse JSON fully; it uses a simple regex for `"type":"module"`,
 *    which is sufficient for our use case and avoids extra dependencies.
 *  - Intended to be used by migration scaffolding code to decide output syntax and extension.
 *
 * Typical usage:
 * ```kotlin
 * val rootVf: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)
 * val result = ModuleKindDetector.detect(rootVf)
 * // result.kind        -> ModuleKind.ESM or ModuleKind.CJS
 * // result.preferredExt-> ".js" | ".mjs" | ".cjs"
 * // result.reason      -> human-readable rationale for logging/debugging
 * ```
 *
 * Responsibilities:
 *  - Provide a stable, deterministic decision about module kind and preferred extension.
 *  - Offer a clear rationale (`reason`) for observability and debugging.
 *  - Handle monorepo layouts (shallow search in `apps` and `packages`) when `package.json`
 *    is not found at the project root.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.1.0 — Added historical extension inference and shallow monorepo search.
 *
 * @license
 *   MIT License
*/
package core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

/** Enumerates the two Node.js module systems we care about. */
enum class ModuleKind { ESM, CJS }

/**
 * Heuristic detector for the project's module kind with an opinionated
 * recommendation for the migration file extension.
 */
object ModuleKindDetector {
    private val log = Logger.getInstance(ModuleKindDetector::class.java)

    /**
     * Result of the detection process.
     *
     * @property kind         The decided module kind (ESM or CJS).
     * @property reason       A human-readable explanation of how the decision was reached.
     * @property preferredExt Recommended file extension to use for generated files
     *                        (one of: ".js", ".mjs", ".cjs").
     */
    data class Result(
        val kind: ModuleKind,
        val reason: String,
        val preferredExt: String
    )

    /**
     * Detects the module kind and the preferred migration file extension.
     *
     * Algorithm:
     *  1) Inspect `package.json` for `"type":"module"` → prefer ESM with `.js`.
     *  2) Inspect existing files under `migrations/` (if any) and honor historical extensions:
     *       - `.mjs` → ESM with `.mjs`
     *       - `.cjs` → CJS with `.cjs`
     *       - `.js`  → ESM if step (1) said ESM; otherwise CJS
     *  3) Default to CJS with `.js` when no hints are found.
     *
     * @param projectRoot The project root folder (as a VirtualFile). Can be null.
     * @return [Result] containing the decided [ModuleKind], a rationale, and the preferred extension.
     */
    fun detect(projectRoot: VirtualFile?): Result {
        if (projectRoot == null) {
            log.warn("[ModuleKindDetector] projectRoot is null → fallback CJS .js (reason=no-root)")
            return Result(ModuleKind.CJS, "no-root", ".js")
        }

        log.info("[ModuleKindDetector] detect() root=${projectRoot.path}")

        var fromPkg: ModuleKind? = null
        var extFromPkg = ".js" // When ESM is declared by package.json, default to .js unless history suggests otherwise.

        // (1) Try to read package.json at the root; if absent, try a shallow monorepo search.
        val pkgAtRoot = projectRoot.findChild("package.json")
        val pkgFile = pkgAtRoot ?: findPackageJsonShallow(projectRoot)

        if (pkgFile != null) {
            runCatching {
                val bytes = pkgFile.contentsToByteArray()
                val text = String(bytes, StandardCharsets.UTF_8)
                // Keep a short preview in logs for debugging without flooding.
                val preview = text.take(200).replace("\n", "\\n")
                log.debug("[ModuleKindDetector] package.json found at=${pkgFile.path}, preview=\"$preview\"")

                // Lightweight detection for `"type":"module"`.
                val isESM = Regex(""""type"\s*:\s*"module"""").containsMatchIn(text)
                log.info("[ModuleKindDetector] package.json:type=module? $isESM")
                if (isESM) {
                    fromPkg = ModuleKind.ESM
                    extFromPkg = ".js"
                }
            }.onFailure {
                log.warn("[ModuleKindDetector] Failed reading package.json at ${pkgFile.path}: ${it.message}")
            }
        } else {
            log.info("[ModuleKindDetector] No package.json found at root nor shallow dirs")
        }

        // (2) Inspect the migrations directory for historical extensions.
        val migDir = projectRoot.findFileByRelativePath("migrations")
        if (migDir != null && migDir.isDirectory) {
            val children = migDir.children?.toList().orEmpty()
            val names = children.joinToString { it.name }
            log.debug("[ModuleKindDetector] migrations dir=${migDir.path}, children=[$names]")

            val hasMjs = children.any { it.name.endsWith(".mjs", ignoreCase = true) }
            val hasCjs = children.any { it.name.endsWith(".cjs", ignoreCase = true) }
            val hasJs  = children.any { it.name.endsWith(".js",  ignoreCase = true) }

            log.info("[ModuleKindDetector] hasMjs=$hasMjs, hasCjs=$hasCjs, hasJs=$hasJs")

            if (hasMjs) {
                log.info("[ModuleKindDetector] decide ESM .mjs (reason=existing-.mjs)")
                return Result(ModuleKind.ESM, "existing-.mjs", ".mjs")
            }
            if (hasCjs) {
                log.info("[ModuleKindDetector] decide CJS .cjs (reason=existing-.cjs)")
                return Result(ModuleKind.CJS, "existing-.cjs", ".cjs")
            }
            if (hasJs) {
                // If package.json said ESM, keep ESM with .js; otherwise default to CJS with .js
                val r = if (fromPkg == ModuleKind.ESM) {
                    Result(ModuleKind.ESM, "existing-.js+pkg:module", ".js")
                } else {
                    Result(ModuleKind.CJS, "existing-.js+default-cjs", ".js")
                }
                log.info("[ModuleKindDetector] decide ${r.kind} ${r.preferredExt} (reason=${r.reason})")
                return r
            }
        } else {
            log.debug("[ModuleKindDetector] No migrations/ directory at ${projectRoot.path}")
        }

        // (3) No hints from migrations: rely on package.json; otherwise default to CJS .js
        val r = when (fromPkg) {
            ModuleKind.ESM -> Result(ModuleKind.ESM, "package.json:type=module", extFromPkg)
            else           -> Result(ModuleKind.CJS, "default", ".js")
        }
        log.info("[ModuleKindDetector] final decide ${r.kind} ${r.preferredExt} (reason=${r.reason})")
        return r
    }

    /**
     * Shallow monorepo search for `package.json`.
     *
     * This checks first-level folders under the root named `apps` and `packages`,
     * then returns the first `package.json` found under any direct subfolder.
     * The search is intentionally shallow (depth 1) for performance reasons.
     *
     * Example checked paths:
     *  - `<root>/apps/.../package.json`
     *  - `<root>/packages/.../package.json`
     *
     * @param root The project root directory.
     * @return The first matching `package.json`, or null if none is found.
    */
    private fun findPackageJsonShallow(root: VirtualFile): VirtualFile? {
        val shallowDirs = sequenceOf("apps", "packages")
            .mapNotNull { root.findChild(it) }
            .filter { it.isDirectory }
            .toList()

        for (dir in shallowDirs) {
            dir.children?.forEach { sub ->
                if (sub.isDirectory) {
                    val candidate = sub.findChild("package.json")
                    if (candidate != null) {
                        log.debug("[ModuleKindDetector] found package.json in ${sub.path}")
                        return candidate
                    }
                }
            }
        }
        return null
    }
}