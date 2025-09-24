package i18n

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Tiny flat JSON loader for key -> string labels, from /i18n/labels.json.
 * No external deps; supports simple { "key": "value" } maps.
 */
object Labels {
    private val map: Map<String, String> by lazy { load() }

    fun t(key: String, vararg pairs: Pair<String, String>): String {
        val raw = map[key] ?: key
        if (pairs.isEmpty()) return raw
        var out = raw
        pairs.forEach { (k, v) -> out = out.replace("{$k}", v) }
        return out
    }

    private fun load(): Map<String, String> {
        val resource = Labels::class.java.getResourceAsStream("/i18n/labels.json") ?: return emptyMap()
        val text = BufferedReader(
            InputStreamReader(resource, StandardCharsets.UTF_8)
        ).use { it.readText() }

        val result = mutableMapOf<String, String>()
        val body = text.trim().removePrefix("{").removeSuffix("}")
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in body) {
            if (ch == '"') inQuotes = !inQuotes
            if (ch == ',' && !inQuotes) {
                parts.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) parts.add(sb.toString())

        parts.forEach { item ->
            val kv = item.split(':', limit = 2)
            if (kv.size == 2) {
                val k = kv[0].trim().trim('"')
                val v = kv[1].trim().trim(',').trim().trim('"').replace("\\n", "\n")
                if (k.isNotEmpty()) result[k] = v
            }
        }
        return result
    }
}
