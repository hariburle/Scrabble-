package com.example.game.dictionary

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Real word meanings, with Wiktionary as the primary online source.
 *
 * Lookup order: the built-in dictionary first (instant, offline), then
 * Wiktionary (https://en.wiktionary.org — free, no key required, and the
 * only source of the three that defines obscure tournament words like
 * WOST and NOINTED), then the Free Dictionary API
 * (https://api.dictionaryapi.dev), then Datamuse as a last fallback.
 *
 * Used by both meaning surfaces: the board word-inspection card and the
 * WordDefinitionDialog. Only called when the built-in dictionary has no
 * real definition; results are cached for the session so a repeat lookup
 * is instant. Pure JVM (no Android types) so it can be unit-tested.
 */
object OnlineDictionary {

    private const val ENDPOINT = "https://api.dictionaryapi.dev/api/v2/entries/en/"
    private const val DATAMUSE_ENDPOINT = "https://api.datamuse.com/words?sp="
    private const val WIKTIONARY_ENDPOINT = "https://en.wiktionary.org/api/rest_v1/page/definition/"
    private const val CONNECT_TIMEOUT_MS = 3000
    private const val READ_TIMEOUT_MS = 3000

    /** What a lookup can come back with. The UI tells these apart so a
     * word the dictionary simply doesn't have never looks like the lookup
     * never ran (the old code collapsed both into null, which the UI then
     * rendered as the generic "official tournament word" line). */
    sealed interface LookupResult {
        data class Found(val definition: String) : LookupResult
        object NotInDictionary : LookupResult
        object LookupFailed : LookupResult
    }

    private val cache = ConcurrentHashMap<String, LookupResult>()

    /**
     * Blocking network call — call off the main thread (e.g. Dispatchers.IO).
     * Tries Wiktionary first (it defines obscure game words the other
     * sources lack, e.g. WOST and NOINTED), then the Free Dictionary API,
     * then Datamuse as a last fallback (Datamuse covers obscure game words
     * the first sources lack, e.g. MASE — but it fuzzy-matches, so only an
     * exact-word entry counts).
     * Returns Found("partOfSpeech: definition") for the first meaning,
     * NotInDictionary when all three sources affirmatively lack the word,
     * or LookupFailed on any other failure (offline, timeout, bad response).
     * Never throws. Found and NotInDictionary are cached for the session;
     * failures are not, so a retry can succeed.
     */
    fun fetchDefinition(word: String): LookupResult {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return LookupResult.LookupFailed
        cache[clean]?.let { return it }
        val wiktionary = fetchFromWiktionary(clean)
        if (wiktionary is LookupResult.Found) {
            cache[clean] = wiktionary
            return wiktionary
        }
        val primary = fetchFromDictionaryApi(clean)
        if (primary is LookupResult.Found) {
            cache[clean] = primary
            return primary
        }
        val fallback = fetchFromDatamuse(clean)
        val result = when {
            fallback is LookupResult.Found -> fallback
            wiktionary is LookupResult.NotInDictionary &&
                primary is LookupResult.NotInDictionary &&
                fallback is LookupResult.NotInDictionary -> LookupResult.NotInDictionary
            else -> LookupResult.LookupFailed
        }
        if (result is LookupResult.Found || result is LookupResult.NotInDictionary) {
            cache[clean] = result
        }
        return result
    }

    private fun fetchFromWiktionary(word: String): LookupResult {
        return try {
            val url = URL(WIKTIONARY_ENDPOINT + URLEncoder.encode(word, "UTF-8"))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ClassicWordArena/1.0")
            }
            try {
                if (connection.responseCode == 404) return LookupResult.NotInDictionary
                if (connection.responseCode != 200) return LookupResult.LookupFailed
                val body = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                    .use { it.readText() }
                return parseWiktionaryFirstDefinition(body)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            LookupResult.LookupFailed
        }
    }

    private fun fetchFromDictionaryApi(word: String): LookupResult {
        return try {
            val url = URL(ENDPOINT + URLEncoder.encode(word, "UTF-8"))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ClassicWordArena/1.0")
            }
            try {
                if (connection.responseCode == 404) return LookupResult.NotInDictionary
                if (connection.responseCode != 200) return LookupResult.LookupFailed
                val body = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                    .use { it.readText() }
                return parseFirstDefinition(body)?.let { LookupResult.Found(it) }
                    ?: LookupResult.LookupFailed
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            LookupResult.LookupFailed
        }
    }

    private fun fetchFromDatamuse(word: String): LookupResult {
        return try {
            val url = URL(DATAMUSE_ENDPOINT + URLEncoder.encode(word, "UTF-8") + "&md=d&max=5")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ClassicWordArena/1.0")
            }
            try {
                if (connection.responseCode != 200) return LookupResult.LookupFailed
                val body = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                    .use { it.readText() }
                return parseDatamuseFirstDefinition(body, word)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            LookupResult.LookupFailed
        }
    }

    /**
     * First definition for the EXACT word from a Datamuse response.
     * Datamuse fuzzy-matches (a lookup for VOGIER returns LOGIER, VOGIE,
     * VOGLER), so an entry only counts when its "word" equals the query —
     * otherwise one word would inherit another word's meaning.
     * Returns NotInDictionary when the response is a well-formed list with
     * no exact-match entry carrying definitions, LookupFailed when the
     * response isn't a list at all.
     */
    internal fun parseDatamuseFirstDefinition(json: String, word: String): LookupResult {
        if (!json.trim().startsWith("[")) return LookupResult.LookupFailed
        var searchFrom = 0
        while (true) {
            val wordKey = json.indexOf("\"word\"", searchFrom)
            if (wordKey < 0) break
            val entryWord = readJsonString(json, json.indexOf(':', wordKey) + 1) ?: break
            if (entryWord.equals(word, ignoreCase = true)) {
                val defsKey = json.indexOf("\"defs\"", wordKey)
                val nextWordKey = json.indexOf("\"word\"", wordKey + 6)
                if (defsKey < 0 || (nextWordKey >= 0 && defsKey > nextWordKey)) {
                    return LookupResult.NotInDictionary
                }
                val bracket = json.indexOf('[', defsKey)
                if (bracket < 0) return LookupResult.NotInDictionary
                var i = bracket + 1
                while (i < json.length && json[i].isWhitespace()) i++
                if (i >= json.length || json[i] == ']') {
                    return LookupResult.NotInDictionary
                }
                val rawDef = readJsonString(json, i)?.trim() ?: return LookupResult.NotInDictionary
                val tab = rawDef.indexOf('\t')
                val found = if (tab >= 0) {
                    val posName = when (rawDef.substring(0, tab)) {
                        "n" -> "noun"
                        "v" -> "verb"
                        "adj" -> "adjective"
                        "adv" -> "adverb"
                        else -> rawDef.substring(0, tab)
                    }
                    "$posName: ${rawDef.substring(tab + 1).trim()}"
                } else {
                    rawDef
                }
                return LookupResult.Found(found)
            }
            searchFrom = wordKey + 6
        }
        return LookupResult.NotInDictionary
    }

    /**
     * First non-empty English definition from a Wiktionary REST response
     * (en.wiktionary.org/api/rest_v1/page/definition/<word>).
     *
     * Shape: {"en":[{"partOfSpeech":"Verb","language":"English",
     * "definitions":[{"definition":"..."}, ...]}, ...], ...}. Definitions
     * carry HTML (links, spans, and occasionally a <style> block whose CSS
     * would leak into the text if tags were merely stripped), and the
     * first entry is sometimes an empty string (e.g. COMPUTER) — so empty
     * definitions are skipped and <style>/<script> blocks are dropped
     * before tags are stripped. Entries are scanned only while their
     * language is English, so a word that exists solely in another
     * language never donates a foreign definition.
     * Returns NotInDictionary when the response is well-formed JSON with
     * no usable English definition, LookupFailed when it isn't JSON.
     */
    internal fun parseWiktionaryFirstDefinition(json: String): LookupResult {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return LookupResult.LookupFailed
        val enKey = trimmed.indexOf("\"en\"")
        if (enKey < 0) return LookupResult.NotInDictionary
        var searchFrom = enKey
        while (true) {
            val posKey = trimmed.indexOf("\"partOfSpeech\"", searchFrom)
            if (posKey < 0) break
            val partOfSpeech = readJsonString(trimmed, trimmed.indexOf(':', posKey) + 1)
            val langKey = trimmed.indexOf("\"language\"", posKey)
            val defsKey = trimmed.indexOf("\"definitions\"", posKey)
            if (langKey < 0 || defsKey < 0) break
            if (langKey > defsKey) break
            val language = readJsonString(trimmed, trimmed.indexOf(':', langKey) + 1)
            if (language != "English") break
            var defSearch = defsKey
            while (true) {
                val defKey = trimmed.indexOf("\"definition\"", defSearch)
                if (defKey < 0) break
                val nextPosKey = trimmed.indexOf("\"partOfSpeech\"", defSearch)
                if (nextPosKey >= 0 && nextPosKey < defKey) break
                val raw = readJsonString(trimmed, trimmed.indexOf(':', defKey) + 1)
                    ?: break
                val clean = stripWiktionaryHtml(raw)
                if (clean.isNotEmpty()) {
                    val pos = partOfSpeech?.lowercase() ?: "definition"
                    return LookupResult.Found("$pos: $clean")
                }
                defSearch = defKey + 12
            }
            searchFrom = posKey + 14
        }
        return LookupResult.NotInDictionary
    }

    /**
     * Strips Wiktionary definition HTML to plain text. <style> and
     * <script> blocks are removed with their contents first (their CSS/JS
     * text is not part of the definition), then all remaining tags, then
     * common entities are decoded and whitespace collapsed.
     */
    internal fun stripWiktionaryHtml(raw: String): String {
        var text = raw
        text = text.replace(Regex("(?s)<style[^>]*>.*?</style>"), "")
        text = text.replace(Regex("(?s)<script[^>]*>.*?</script>"), "")
        text = text.replace(Regex("<[^>]+>"), "")
        text = text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        text = Regex("&#(\\d+);").replace(text) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null) code.toChar().toString() else match.value
        }
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * First part of speech + first definition from a dictionaryapi.dev
     * response. The first "definition" in the document belongs to the first
     * meaning's first entry, which is what we want.
     */
    internal fun parseFirstDefinition(json: String): String? {
        val posIndex = json.indexOf("\"partOfSpeech\"")
        val partOfSpeech = if (posIndex >= 0) {
            readJsonString(json, json.indexOf(':', posIndex) + 1)
        } else {
            null
        }
        val defSearchFrom = if (posIndex >= 0) posIndex else 0
        val defIndex = json.indexOf("\"definition\"", defSearchFrom)
        if (defIndex < 0) return null
        val definition = readJsonString(json, json.indexOf(':', defIndex) + 1) ?: return null
        return if (partOfSpeech != null) "$partOfSpeech: $definition" else definition
    }

    /**
     * Reads the JSON string value starting at/after [from] (skips whitespace
     * and the colon). Handles all standard escapes including \uXXXX.
     * Returns null when no quoted string starts there, or it is unterminated.
     */
    internal fun readJsonString(json: String, from: Int): String? {
        var i = from
        while (i < json.length && (json[i] == ' ' || json[i] == ':' || json[i] == '\n' ||
            json[i] == '\r' || json[i] == '\t')
        ) {
            i++
        }
        if (i >= json.length || json[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < json.length) {
            val ch = json[i]
            if (ch == '"') return sb.toString()
            if (ch == '\\' && i + 1 < json.length) {
                val esc = json[i + 1]
                when (esc) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 5 < json.length) {
                            val code = json.substring(i + 2, i + 6).toIntOrNull(16)
                            sb.append(code?.toChar() ?: '?')
                            i += 4
                        } else {
                            sb.append('?')
                        }
                    }
                    else -> sb.append(esc)
                }
                i += 2
            } else {
                sb.append(ch)
                i++
            }
        }
        return null
    }
}
