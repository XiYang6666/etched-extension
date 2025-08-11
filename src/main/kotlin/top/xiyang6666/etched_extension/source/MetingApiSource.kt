package top.xiyang6666.etched_extension.source

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import gg.moonflower.etched.api.record.TrackData
import gg.moonflower.etched.api.sound.download.SoundDownloadSource
import gg.moonflower.etched.api.util.DownloadProgressListener
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.ResourceManager
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.*
import java.util.*

class MetingApiSource : SoundDownloadSource {

    companion object {
        private const val API_NAME = "Meting-Api"
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


    private fun parseApiResult(content: String): List<ApiTrackRecord> {
        try {
            return Gson().fromJsonTyped(content)
        } catch (_: JsonSyntaxException) {
            throw RuntimeException("Unknown song")
        }
    }


    override fun resolveUrl(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<URL> {
        val uri = URI(s)
        val queryMap = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        } ?: emptyMap()
        when (queryMap["type"]) {
            "playlist", "song" -> Utils.get(uri.toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = parseApiResult(content)
                return data.map { URI(it.url).toURL() }
            }

            "url" -> {
                val sb = StringBuilder()
                sb.append(uri.scheme).append("://").append(uri.host).append(uri.path)
                if (queryMap.isNotEmpty()) sb.append("?")
                queryMap.entries.joinTo(sb, "&") { (key, value) ->
                    if (key == "type") "type=song" else "$key=$value"
                }
                val songUrl = sb.toString()
                return resolveUrl(songUrl, listener, proxy)
            }

            else -> throw RuntimeException("Unknown or unsupported type in Meting URL: ${queryMap["type"]}")
        }
    }

    override fun resolveTracks(
        s: String, listener: DownloadProgressListener?, proxy: Proxy
    ): List<TrackData> {
        println("-> resolveTracks($s)")
        val uri = URI(s)
        val queryMap = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        } ?: emptyMap()
        when (queryMap["type"]) {
            "playlist" -> Utils.get(uri.toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = parseApiResult(content)
                val playListInfo = TrackData(uri.toString(), "Unknown", Component.literal("playlist ${queryMap["id"]}"))
                val tracks = data.map { TrackData(it.url, it.artist, Component.literal(it.name)) }
                return listOf(playListInfo) + tracks
            }

            "song" -> Utils.get(uri.toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val data: List<ApiTrackRecord> = parseApiResult(content)
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
                return resolveTracks(songUrl, listener, proxy)
            }

            else -> throw RuntimeException("Unknown or unsupported type in Meting URL: ${queryMap["type"]}")
        }
    }

    override fun resolveAlbumCover(
        s: String, listener: DownloadProgressListener?, proxy: Proxy, manager: ResourceManager
    ): Optional<String> {
        return Utils.get(URI(s).toURL(), listener, API_NAME).use { inputStream ->
            val content = inputStream.reader().readText()
            listener?.progressStartLoading()
            val data: List<ApiTrackRecord> = parseApiResult(content)
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
