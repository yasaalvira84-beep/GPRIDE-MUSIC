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
