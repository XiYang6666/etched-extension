package top.xiyang6666.etched_extension

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import gg.moonflower.etched.api.record.TrackData
import gg.moonflower.etched.api.sound.download.SoundDownloadSource
import gg.moonflower.etched.api.util.DownloadProgressListener
import gg.moonflower.etched.api.util.ProgressTrackingInputStream
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.ResourceManager
import java.io.IOException
import java.io.InputStream
import java.net.*
import java.util.*

class MetingApiSource : SoundDownloadSource {

    companion object {
        private val API_NAME_COMPONENT = Component.literal("Meting")
        private val REQUESTING_COMPONENT = Component.translatable("sound_source.etched.requesting", API_NAME_COMPONENT)
        private val RESOLVING_TRACKS_COMPONENT = Component.translatable("sound_source.etched.resolving_tracks")
        lateinit var instance: MetingApiSource
    }

    data class ApiTrackRecord(
        val name: String,
        val artist: String,
        val url: String,
        val pic: String,
        val lrc: String,
    )

    init {
        instance = this
    }

    private inline fun <reified T> Gson.fromJsonTyped(json: String): T = fromJson(json, object : TypeToken<T>() {}.type)

    private fun parseApiResult(content: String): List<ApiTrackRecord> {
        try {
            return Gson().fromJsonTyped(content)
        } catch (_: JsonSyntaxException) {
            throw RuntimeException("Unknown song")
        }
    }

    private fun request(url: URL, listener: DownloadProgressListener?, proxy: Proxy): InputStream {
        listener?.progressStartRequest(REQUESTING_COMPONENT)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Etched-Extras/1.0")
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) throw RuntimeException("Could not resolve: $url (HTTP $responseCode)")
        val size = connection.contentLengthLong
        return if (size != -1L && listener != null) {
            ProgressTrackingInputStream(connection.inputStream, size, listener)
        } else {
            connection.inputStream
        }
    }

    @kotlin.Throws(IOException::class)
    override fun resolveUrl(s: String, downloadProgressListener: DownloadProgressListener?, proxy: Proxy): List<URL> {
        val uri = URI(s)
        val queryMap = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        } ?: emptyMap()
        when (queryMap["type"]) {
            "playlist", "song" -> request(uri.toURL(), downloadProgressListener, proxy).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = Gson().fromJsonTyped(content)
                return data.map { URL(it.url) }
            }

            "url" -> {
                val sb = StringBuilder()
                sb.append(uri.scheme).append("://").append(uri.host).append(uri.path)
                if (queryMap.isNotEmpty()) sb.append("?")
                queryMap.entries.joinTo(sb, "&") { (key, value) ->
                    if (key == "type") "type=song" else "$key=$value"
                }
                val songUrl = sb.toString()
                return resolveUrl(songUrl, downloadProgressListener, proxy)
            }

            else -> throw RuntimeException("Unknown or unsupported type in Meting URL: ${queryMap["type"]}")
        }
    }

    @kotlin.Throws(IOException::class, JsonParseException::class)
    override fun resolveTracks(
        s: String, downloadProgressListener: DownloadProgressListener?, proxy: Proxy
    ): List<TrackData> {
        val uri = URI(s)
        val queryMap = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        } ?: emptyMap()
        when (queryMap["type"]) {
            "playlist" -> request(uri.toURL(), downloadProgressListener, proxy).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = Gson().fromJsonTyped(content)
                val playListInfo = TrackData(uri.toString(), "Unknown", Component.literal("playlist ${queryMap["id"]}"))
                val tracks = data.map { TrackData(it.url, it.artist, Component.literal(it.name)) }
                return listOf(playListInfo) + tracks
            }

            "song" -> request(uri.toURL(), downloadProgressListener, proxy).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = Gson().fromJsonTyped(content)
                val songData = data[0]
                return listOf(TrackData(songData.url, songData.artist, Component.literal(songData.name)))
            }

            "url" -> {
                val sb = StringBuilder()
                sb.append(uri.scheme).append("://").append(uri.host).append(uri.path)
                if (queryMap.isNotEmpty()) sb.append("?")
                queryMap.entries.joinTo(sb, "&") { (key, value) ->
                    if (key == "type") "type=song" else "$key=$value"
                }
                val songUrl = sb.toString()
                return resolveTracks(songUrl, downloadProgressListener, proxy)
            }

            else -> throw RuntimeException("Unknown or unsupported type in Meting URL: ${queryMap["type"]}")
        }
    }

    @kotlin.Throws(IOException::class)
    override fun resolveAlbumCover(
        s: String, downloadProgressListener: DownloadProgressListener?, proxy: Proxy, resourceManager: ResourceManager
    ): Optional<String> {
        return request(URL(s), downloadProgressListener, proxy).use { inputStream ->
            val content = inputStream.reader().readText()
            downloadProgressListener?.progressStartLoading()
            val data: List<ApiTrackRecord> = Gson().fromJsonTyped(content)
            data.firstOrNull()?.pic?.takeIf { it.isNotEmpty() }?.let { Optional.of(it) } ?: Optional.empty()
        }
    }

    override fun isValidUrl(s: String): Boolean {
        try {
            val uri = URI(s)
            return uri.path == "/meting/"
        } catch (_: URISyntaxException) {
            return false
        }
    }

    override fun isTemporary(s: String): Boolean {
        return true
    }

    override fun getApiName(): String {
        return "meting-api"
    }
}
