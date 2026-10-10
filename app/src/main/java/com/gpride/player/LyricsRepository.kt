package com.gpride.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

sealed interface LyricsState {
    data object Loading : LyricsState
    data class Found(val lyrics: Lyrics) : LyricsState
    data object Instrumental : LyricsState
    data object NotFound : LyricsState
    data object Disabled : LyricsState
    data class Error(val message: String) : LyricsState
}

/**
 * Mencari lirik otomatis dari LRCLIB (layanan publik, tanpa akun/kunci API) memakai judul, artis, album,
 * dan durasi lagu. Hasil disimpan di folder aplikasi sehingga lagu yang sama tidak dicari ulang.
 * Berkas musik tidak pernah diunggah; hanya metadata teks yang dikirim.
 */
class LyricsRepository(context: Context) {
    private val dir = File(context.filesDir, "lyrics").apply { mkdirs() }

    private fun file(key: String, ext: String) = File(dir, "$key.$ext")

    private fun readCache(key: String): Lyrics? {
        file(key, "lrc").takeIf { it.exists() }?.let { f ->
            val lines = parseLrc(f.readText())
            if (lines.isNotEmpty()) return Lyrics(lines, synced = true)
        }
        file(key, "txt").takeIf { it.exists() }?.let { f ->
            val lines = plainLyrics(f.readText())
            if (lines.isNotEmpty()) return Lyrics(lines, synced = false)
        }
        return null
    }

    private fun clearEntry(key: String) {
        file(key, "lrc").delete()
        file(key, "txt").delete()
        file(key, "none").delete()
    }

    fun clearCache() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /**
     * @param online izinkan mengambil dari internet bila belum ada di cache.
     * @param force abaikan cache (termasuk catatan "tidak ditemukan") dan cari ulang.
     */
    suspend fun load(
        key: String,
        title: String,
        artist: String?,
        album: String?,
        durationMs: Long,
        online: Boolean,
        force: Boolean = false,
    ): LyricsState = withContext(Dispatchers.IO) {
        if (force) clearEntry(key)
        readCache(key)?.let { return@withContext LyricsState.Found(it) }
        val none = file(key, "none")
        if (none.exists() && System.currentTimeMillis() - none.lastModified() < NOT_FOUND_TTL_MS) {
            return@withContext LyricsState.NotFound
        }
        if (!online) return@withContext LyricsState.Disabled
        try {
            val result = fetch(cleanTitle(title), cleanArtist(artist), album?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }, durationMs)
            when {
                result == null -> {
                    none.writeText("")
                    LyricsState.NotFound
                }
                result.instrumental -> LyricsState.Instrumental
                result.synced != null -> {
                    file(key, "lrc").writeText(result.synced)
                    readCache(key)?.let { LyricsState.Found(it) } ?: LyricsState.NotFound
                }
                result.plain != null -> {
                    file(key, "txt").writeText(result.plain)
                    readCache(key)?.let { LyricsState.Found(it) } ?: LyricsState.NotFound
                }
                else -> LyricsState.NotFound
            }
        } catch (e: IOException) {
            LyricsState.Error("Tidak bisa menghubungi layanan lirik. Periksa koneksi internet.")
        } catch (e: org.json.JSONException) {
            LyricsState.Error("Balasan layanan lirik tidak dapat dibaca.")
        }
    }

    private class Hit(val synced: String?, val plain: String?, val instrumental: Boolean)

    private fun fetch(title: String, artist: String?, album: String?, durationMs: Long): Hit? {
        val durSec = (durationMs / 1000).toInt()
        // 1) pencocokan tepat (judul + artis + durasi)
        if (artist != null && durSec > 0) {
            val q = buildString {
                append("$BASE/get?track_name=").append(enc(title))
                append("&artist_name=").append(enc(artist))
                if (album != null) append("&album_name=").append(enc(album))
                append("&duration=").append(durSec)
            }
            httpGet(q)?.let { body ->
                val hit = toHit(JSONObject(body))
                if (hit != null) return hit
            }
        }
        // 2) pencarian longgar, pilih yang durasinya paling dekat dan punya lirik berwaktu
        val search = buildString {
            append("$BASE/search?track_name=").append(enc(title))
            if (artist != null) append("&artist_name=").append(enc(artist))
        }
        val arr = JSONArray(httpGet(search) ?: return null)
        val candidates = (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val hit = toHit(o) ?: return@mapNotNull null
            val diff = if (durSec > 0 && !o.isNull("duration")) abs(o.getDouble("duration") - durSec) else 0.0
            hit to diff
        }
        return candidates
            .filter { durSec <= 0 || it.second <= MAX_DURATION_DIFF_SEC }
            .sortedWith(compareBy({ it.first.synced == null }, { it.second }))
            .firstOrNull()?.first
    }

    private fun toHit(o: JSONObject): Hit? {
        val synced = o.strOrNull("syncedLyrics")
        val plain = o.strOrNull("plainLyrics")
        val instrumental = !o.isNull("instrumental") && o.optBoolean("instrumental", false)
        if (synced == null && plain == null && !instrumental) return null
        return Hit(synced, plain, instrumental)
    }

    private fun JSONObject.strOrNull(name: String): String? =
        if (isNull(name)) null else getString(name).takeIf { it.isNotBlank() }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Mengembalikan null untuk 404 (tidak ditemukan); melempar IOException untuk kegagalan lain. */
    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "GPRIDE-MUSIC/0.1.0 (https://github.com/gusdiantoilyas-max/GPRIDE-MUSIC)")
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://lrclib.net/api"
        const val NOT_FOUND_TTL_MS = 7L * 24 * 60 * 60 * 1000
        const val MAX_DURATION_DIFF_SEC = 8.0
    }
}
