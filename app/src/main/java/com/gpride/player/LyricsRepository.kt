package com.gpride.player

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Generating : LyricsState
    data class Found(val lyrics: Lyrics) : LyricsState
    data class Error(val message: String) : LyricsState
}

private class LyricsException(message: String) : Exception(message)

/**
 * Membuat lirik otomatis dari suara lagu: berkas audio ditranskripsi oleh model Whisper melalui
 * layanan (Groq/OpenAI) memakai kunci API milik pengguna, hasilnya berupa teks berwaktu yang
 * disimpan sebagai berkas LRC di folder aplikasi. Pembuatan berjalan di scope sendiri sehingga
 * tidak terhenti saat layar lirik ditutup.
 */
class LyricsRepository(private val context: Context) {
    private val dir = File(context.filesDir, "lyrics_ai").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow<Map<String, LyricsState>>(emptyMap())

    /** Status per lagu untuk pembuatan yang sedang/baru berjalan. */
    val status: StateFlow<Map<String, LyricsState>> = _status.asStateFlow()

    init {
        // Folder cache versi lama (pencarian lirik) tidak dipakai lagi.
        File(context.filesDir, "lyrics").deleteRecursively()
    }

    private fun lrcFile(key: String) = File(dir, "$key.lrc")

    private fun txtFile(key: String) = File(dir, "$key.txt")

    private fun setStatus(key: String, state: LyricsState) {
        _status.update { it + (key to state) }
    }

    /** Lirik tersimpan untuk lagu ini (berwaktu .lrc atau lirik biasa .txt), atau null. */
    fun cached(key: String): Lyrics? {
        lrcFile(key).takeIf { it.exists() }?.let { f ->
            val lines = parseLrc(f.readText())
            if (lines.isNotEmpty()) return Lyrics(lines, synced = true)
        }
        txtFile(key).takeIf { it.exists() }?.let { f ->
            val lines = plainLyrics(f.readText())
            if (lines.isNotEmpty()) return Lyrics(lines, synced = false)
        }
        return null
    }

    /** Teks mentah lirik tersimpan untuk diisi ke editor; kosong bila belum ada. */
    fun rawText(key: String): String {
        val f = lrcFile(key).takeIf { it.exists() } ?: txtFile(key).takeIf { it.exists() }
        return f?.readText().orEmpty()
    }

    /**
     * Menyimpan teks hasil sunting/impor. Bila ada penanda waktu "[mm:ss.xx]" disimpan sebagai LRC berwaktu,
     * selain itu sebagai lirik biasa. Mengembalikan false bila teks kosong.
     */
    fun save(key: String, text: String): Boolean {
        val clean = text.replace("\uFEFF", "").trim()
        if (clean.isEmpty()) return false
        val lines = parseLrc(clean)
        if (lines.isNotEmpty()) {
            lrcFile(key).writeText(segmentsToLrc(lines))
            txtFile(key).delete()
        } else {
            txtFile(key).writeText(clean)
            lrcFile(key).delete()
        }
        cached(key)?.let { setStatus(key, LyricsState.Found(it)) }
        return true
    }

    fun clear(key: String) {
        lrcFile(key).delete()
        txtFile(key).delete()
        _status.update { it - key }
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
        _status.value = emptyMap()
    }

    /** Mulai membuat lirik; hasil/kesalahan muncul lewat [status]. Diabaikan jika sedang berjalan untuk lagu yang sama. */
    fun generate(key: String, uri: String, prefs: LyricsPrefs) {
        if (_status.value[key] is LyricsState.Generating) return
        setStatus(key, LyricsState.Generating)
        scope.launch {
            val result = try {
                doGenerate(key, uri, prefs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LyricsException) {
                LyricsState.Error(e.message ?: "Gagal membuat lirik.")
            } catch (e: IOException) {
                LyricsState.Error("Koneksi bermasalah atau waktu habis. Periksa internet lalu coba lagi.")
            } catch (e: org.json.JSONException) {
                LyricsState.Error("Balasan layanan tidak dapat dibaca.")
            } catch (e: SecurityException) {
                LyricsState.Error("File lagu tidak dapat diakses.")
            }
            setStatus(key, result)
        }
    }

    private fun doGenerate(key: String, uriString: String, prefs: LyricsPrefs): LyricsState {
        if (prefs.apiKey.isBlank()) throw LyricsException("Kunci API belum diisi.")
        val provider = LyricsProviders[prefs.provider.coerceIn(0, LyricsProviders.lastIndex)]
        val uri = Uri.parse(uriString)
        val audio = readAudio(uri)
        val name = displayName(uri)
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val body = transcribe(provider, prefs, name, mime, audio)
        val lines = parseSegments(body)
        if (lines.isEmpty()) {
            throw LyricsException("Tidak ada vokal yang terdeteksi di lagu ini (mungkin instrumental).")
        }
        lrcFile(key).writeText(segmentsToLrc(lines))
        txtFile(key).delete()
        return LyricsState.Found(Lyrics(lines, synced = true))
    }

    private fun readAudio(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: throw LyricsException("File lagu tidak dapat dibuka.")
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_AUDIO_BYTES) {
                    throw LyricsException("File lagu lebih dari 25 MB, melebihi batas layanan transkripsi.")
                }
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        val name = fromProvider?.takeIf { it.isNotBlank() } ?: "audio.mp3"
        // Layanan menebak format dari ekstensi; hindari karakter yang merusak header multipart.
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private fun transcribe(provider: LyricsProvider, prefs: LyricsPrefs, name: String, mime: String, audio: ByteArray): String {
        val boundary = "----gpride${System.currentTimeMillis()}"
        val head = ByteArrayOutputStream()
        fun field(fieldName: String, value: String) {
            head.write("--$boundary\r\nContent-Disposition: form-data; name=\"$fieldName\"\r\n\r\n$value\r\n".toByteArray())
        }
        field("model", provider.model)
        field("response_format", "verbose_json")
        field("temperature", "0")
        if (prefs.language.isNotBlank()) field("language", prefs.language)
        head.write(
            ("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\n" +
                "Content-Type: $mime\r\n\r\n").toByteArray(),
        )
        val tail = "\r\n--$boundary--\r\n".toByteArray()
        val headBytes = head.toByteArray()

        val conn = URL(provider.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 180_000
            conn.setFixedLengthStreamingMode(headBytes.size + audio.size + tail.size)
            conn.setRequestProperty("Authorization", "Bearer ${prefs.apiKey.trim()}")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.outputStream.use { out ->
                out.write(headBytes)
                out.write(audio)
                out.write(tail)
            }
            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            val serverMessage = runCatching {
                val text = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                JSONObject(text).getJSONObject("error").getString("message")
            }.getOrNull()
            throw LyricsException(
                when (code) {
                    401 -> "Kunci API ditolak. Periksa kunci di Pengaturan > Lirik otomatis."
                    413 -> "File lagu terlalu besar untuk layanan transkripsi."
                    429 -> "Batas pemakaian layanan tercapai. Coba lagi beberapa saat lagi."
                    else -> "Layanan menolak permintaan ($code)" + (serverMessage?.let { ": $it" } ?: ".")
                },
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun parseSegments(body: String): List<LyricLine> {
        val segments = JSONObject(body).optJSONArray("segments")
            ?: throw LyricsException("Layanan tidak mengembalikan penanda waktu.")
        val out = ArrayList<LyricLine>()
        for (i in 0 until segments.length()) {
            val seg = segments.getJSONObject(i)
            val text = seg.optString("text", "").trim()
            if (text.isEmpty() || isLikelyHallucination(text)) continue
            if (seg.optDouble("no_speech_prob", 0.0) > NO_SPEECH_THRESHOLD) continue
            out.add(LyricLine((seg.optDouble("start", 0.0) * 1000).toLong(), text))
        }
        return out
    }

    private companion object {
        const val MAX_AUDIO_BYTES = 24_000_000
        const val NO_SPEECH_THRESHOLD = 0.6
    }
}
