/**
 * Labels.kt
 * ----------
 * Lightweight key-value label loader and template engine for internationalization (i18n).
 *
 * This object provides access to a flat JSON file located at `/i18n/labels.json`,
 * which contains application text strings such as UI titles, tooltips, and messages.
 *
 * Features:
 *  - Loads a simple one-level JSON map (`{ "key": "value" }`) without external libraries.
 *  - Supports placeholder replacement with `{key}` or `{{ key }}` syntax.
 *  - Case-insensitive parameter interpolation (e.g., `{PM}` or `{pm}`).
 *  - Handles escaped Unicode sequences (`\u2022` → `•`) and line breaks (`\n`).
 *
 * Example JSON:
 * ```json
 * {
 *   "appTitle": "Sequelize Runner",
 *   "footerStatusTemplate": "PM: {pm} • Env: {env}"
 * }
 * ```
 *
 * Example usage:
 * ```kotlin
 * val title = Labels.t("appTitle")
 * val footer = Labels.t("footerStatusTemplate", "pm" to "NPM", "env" to "development")
 * println(footer) // → "PM: NPM • Env: development"
 * ```
 *
 * This class is entirely dependency-free and is optimized for plugin environments
 * where startup speed and minimal footprint are critical.
 *
 * @author
 *   Raphaël Sfeir (github.com/raphaelsfeir)
 *
 * @since
 *   1.0.0 — Initial implementation of the lightweight label loader and formatter.
 *
 * @license
 *   MIT License
 */

package i18n

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Provides translation and template interpolation utilities for UI labels.
 *
 * Labels are stored in `/i18n/labels.json` as a flat key-value map.
 * Supports `{key}` or `{{ key }}` placeholders inside label values.
 */
object Labels {

    /** Lazily loaded map of all labels from the JSON file. */
    private val map: Map<String, String> by lazy { load() }

    /** Matches `{key}` or `{{ key }}` placeholders within templates. */
    private val token = Regex("""(?<!\\)\{\{\s*([A-Za-z0-9_.\-]+)\s*\}\}|\{([A-Za-z0-9_.\-]+)\}""")

    /** Matches escaped braces (used to unescape `\{{` or `\}}`). */
    private val unescape = Regex("""\\(\{\{|\}\})""")

    /** Matches Unicode escape sequences like `\u2022`. */
    private val unicode = Regex("""\\u([0-9a-fA-F]{4})""")

    /**
     * Retrieves a localized label by key and optionally performs placeholder replacement.
     *
     * @param key The label key (e.g., `"footerStatusTemplate"`).
     * @param pairs Key-value pairs for placeholder replacement (case-insensitive).
     * @return The resolved string with placeholders replaced. Returns the key itself if not found.
     */
    fun t(key: String, vararg pairs: Pair<String, String>): String {
        val raw = map[key] ?: key
        if (pairs.isEmpty()) return raw
        val valuesLower = pairs.associate { (k, v) -> k.lowercase() to v }
        return interpolate(raw, valuesLower)
    }

    /**
     * Performs template interpolation on the given string using `{key}` or `{{ key }}` placeholders.
     *
     * @param template The template string containing placeholders.
     * @param valuesLower The replacement map (case-insensitive keys).
     * @return The string with placeholders replaced.
     */
    private fun interpolate(template: String, valuesLower: Map<String, String>): String {
        val replaced = token.replace(template) { match ->
            val kOriginal = match.groups[1]?.value ?: match.groups[2]?.value
            val kLower = kOriginal?.lowercase()
            val value = kLower?.let { valuesLower[it] }
            value ?: match.value // leave placeholder if missing
        }
        return unescape.replace(replaced) { mr -> mr.groupValues[1] }
    }

    /**
     * Decodes Unicode escape sequences (e.g., `\u2022`) into their character representation.
     *
     * @param s The string possibly containing Unicode escapes.
     * @return A string with all Unicode escapes decoded.
     */
    private fun decodeUnicodeEscapes(s: String): String =
        unicode.replace(s) { match ->
            val code = match.groupValues[1].toInt(16)
            code.toChar().toString()
        }

    /**
     * Loads labels from `/i18n/labels.json` into memory.
     * This implementation supports only a flat JSON structure.
     *
     * @return A map of label keys to their corresponding string values.
     */
    private fun load(): Map<String, String> {
        val resource = Labels::class.java.getResourceAsStream("/i18n/labels.json") ?: return emptyMap()
        val text = BufferedReader(InputStreamReader(resource, StandardCharsets.UTF_8)).use { it.readText() }

        val result = mutableMapOf<String, String>()
        val body = text.trim().removePrefix("{").removeSuffix("}")
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        // Split JSON content on commas not inside quotes.
        for (ch in body) {
            if (ch == '"') inQuotes = !inQuotes
            if (ch == ',' && !inQuotes) {
                parts.add(sb.toString()); sb.setLength(0)
            } else sb.append(ch)
        }
        if (sb.isNotEmpty()) parts.add(sb.toString())

        // Parse key-value pairs manually (lightweight alternative to a full JSON parser)
        parts.forEach { item ->
            val kv = item.split(':', limit = 2)
            if (kv.size == 2) {
                val k = kv[0].trim().trim('"')
                var v = kv[1].trim().trim(',').trim().trim('"')
                // Decode Unicode (\uXXXX) and escaped newlines (\n)
                v = decodeUnicodeEscapes(v).replace("\\n", "\n")
                if (k.isNotEmpty()) result[k] = v
            }
        }

        return result
    }
}
