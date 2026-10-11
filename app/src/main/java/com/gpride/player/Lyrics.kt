package com.gpride.player

/** Satu baris lirik. [timeMs] bernilai 0 untuk lirik tanpa penanda waktu. */
data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(val lines: List<LyricLine>, val synced: Boolean)

private val LRC_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/** Mengurai format LRC ("[mm:ss.xx] teks"). Tag metadata seperti [ar:...] dilewati. */
fun parseLrc(raw: String): List<LyricLine> {
    val out = ArrayList<LyricLine>()
    for (line in raw.lineSequence()) {
        val tags = LRC_TAG.findAll(line).toList()
        if (tags.isEmpty()) continue
        val text = line.substring(tags.last().range.last + 1).trim()
        for (m in tags) {
            val minutes = m.groupValues[1].toLong()
            val seconds = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val millis = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            out.add(LyricLine((minutes * 60 + seconds) * 1000 + millis, text))
        }
    }
    out.sortBy { it.timeMs }
    return out
}

/** Lirik biasa (tanpa waktu) menjadi daftar baris. */
fun plainLyrics(raw: String): List<LyricLine> =
    raw.lines().map { LyricLine(0L, it.trimEnd()) }.dropWhile { it.text.isBlank() }.dropLastWhile { it.text.isBlank() }

/** Indeks baris yang sedang dinyanyikan pada [positionMs]; -1 bila belum sampai baris pertama. */
fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var lo = 0
    var hi = lines.size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (lines[mid].timeMs <= positionMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return ans
}

private val NOISE = Regex(
    """\s*[(\[][^)\]]*(official|lyrics?|lirik|video|audio|mv|hd|hq|visualizer)[^)\]]*[)\]]""",
    RegexOption.IGNORE_CASE,
)

/** Membersihkan judul dari embel-embel seperti "(Official Video)" agar pencarian lebih cocok. */
fun cleanTitle(title: String): String =
    title.replace(NOISE, "").replace(Regex("""\.(mp3|m4a|flac|wav|ogg|opus|aac)$""", RegexOption.IGNORE_CASE), "").trim()

/** MediaStore memakai "<unknown>" bila artis tidak ada. */
fun cleanArtist(artist: String?): String? =
    artist?.trim()?.takeIf { it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true) }

/** Layanan transkripsi berformat OpenAI-compatible (endpoint /audio/transcriptions). */
data class LyricsProvider(val label: String, val url: String, val model: String)

val LyricsProviders: List<LyricsProvider> = listOf(
    LyricsProvider("Groq", "https://api.groq.com/openai/v1/audio/transcriptions", "whisper-large-v3"),
    LyricsProvider("OpenAI", "https://api.openai.com/v1/audio/transcriptions", "whisper-1"),
)

/** Kode bahasa Whisper; string kosong berarti dideteksi otomatis. */
val LyricsLanguages: List<Pair<String, String>> = listOf(
    "" to "Otomatis",
    "id" to "Indonesia",
    "en" to "Inggris",
    "ms" to "Melayu",
)

data class LyricsPrefs(
    val provider: Int = 0,
    val apiKey: String = "",
    val language: String = "",
    /** Pengguna sudah menyetujui pengunggahan audio ke penyedia terpilih. */
    val consent: Boolean = false,
)

/** Kunci cache lirik: ID MediaStore bila ada, selain itu hash artis+judul. */
fun lyricsKey(songId: Long?, artist: String?, title: String): String =
    songId?.toString() ?: "t${"$artist|$title".hashCode()}"

/** "mm:ss.xx" untuk format LRC. */
fun formatLrcTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L)
    val minutes = total / 60_000
    val seconds = (total % 60_000) / 1000
    val centis = (total % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}

fun segmentsToLrc(lines: List<LyricLine>): String =
    lines.joinToString("\n") { "[${formatLrcTime(it.timeMs)}]${it.text.replace('\n', ' ').trim()}" }

private val HALLUCINATIONS = listOf(
    "thanks for watching",
    "thank you for watching",
    "terima kasih telah menonton",
    "terima kasih sudah menonton",
    "subtitles by",
    "subtitle by",
    "amara.org",
)

/** Whisper kerap "mengarang" kalimat penutup video saat bagian instrumental; buang yang jelas begitu. */
fun isLikelyHallucination(text: String): Boolean {
    val t = text.lowercase()
    return HALLUCINATIONS.any { t.contains(it) }
}
