// core/ModuleKindDetector.kt
package core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

enum class ModuleKind { ESM, CJS }

object ModuleKindDetector {
    private val log = Logger.getInstance(ModuleKindDetector::class.java)

    data class Result(
        val kind: ModuleKind,
        val reason: String,
        /** Extension à utiliser pour la migration: ".js" | ".mjs" | ".cjs" */
        val preferredExt: String
    )

    /**
     * Détecte le type de modules du projet.
     * Règles:
     *  1) Si package.json contient "type":"module" => ESM (par défaut .js)
     *  2) Si un fichier existe dans migrations/ :
     *       - s'il y a un .mjs => ESM .mjs
     *       - s'il y a un .cjs => CJS .cjs
     *       - s'il y a un .js  => conserve la décision précédente (ESM .js si 1), sinon CJS .js)
     *  3) Sinon fallback: CJS .js
     */
    fun detect(projectRoot: VirtualFile?): Result {
        if (projectRoot == null) {
            log.warn("[ModuleKindDetector] projectRoot is null → fallback CJS .js (reason=no-root)")
            return Result(ModuleKind.CJS, "no-root", ".js")
        }

        log.info("[ModuleKindDetector] detect() root=${projectRoot.path}")

        var fromPkg: ModuleKind? = null
        var extFromPkg = ".js"

        // 1) package.json -> "type":"module" => ESM (.js)
        val pkgAtRoot = projectRoot.findChild("package.json")
        val pkgFile = pkgAtRoot ?: findPackageJsonShallow(projectRoot)

        if (pkgFile != null) {
            runCatching {
                val bytes = pkgFile.contentsToByteArray()
                val text = String(bytes, StandardCharsets.UTF_8)
                val preview = text.take(200).replace("\n", "\\n")
                log.debug("[ModuleKindDetector] package.json found at=${pkgFile.path}, preview=\"$preview\"")

                val isESM = Regex(""""type"\s*:\s*"module"""").containsMatchIn(text)
                log.info("[ModuleKindDetector] package.json:type=module? $isESM")
                if (isESM) {
                    fromPkg = ModuleKind.ESM
                    extFromPkg = ".js" // ESM natif → .js par défaut (on basculera en .mjs si historique)
                }
            }.onFailure {
                log.warn("[ModuleKindDetector] Failed reading package.json at ${pkgFile.path}: ${it.message}")
            }
        } else {
            log.info("[ModuleKindDetector] No package.json found at root nor shallow dirs")
        }

        // 2) Heuristique sur migrations/
        val migDir = projectRoot.findFileByRelativePath("migrations")
        if (migDir != null && migDir.isDirectory) {
            val children = migDir.children?.toList().orEmpty()
            val names = children.joinToString { it.name }
            log.debug("[ModuleKindDetector] migrations dir=${migDir.path}, children=[$names]")

            val hasMjs = children.any { it.name.endsWith(".mjs", ignoreCase = true) }
            val hasCjs = children.any { it.name.endsWith(".cjs", ignoreCase = true) }
            val hasJs = children.any { it.name.endsWith(".js", ignoreCase = true) }

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

        // 3) Pas d'indices dans migrations : on se base sur package.json, sinon fallback CJS .js
        val r = when (fromPkg) {
            ModuleKind.ESM -> Result(ModuleKind.ESM, "package.json:type=module", extFromPkg)
            else -> Result(ModuleKind.CJS, "default", ".js")
        }
        log.info("[ModuleKindDetector] final decide ${r.kind} ${r.preferredExt} (reason=${r.reason})")
        return r
    }

    /**
     * Recherche de secours (monorepo léger)
     * (profondeur 1 pour rester rapide).
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
