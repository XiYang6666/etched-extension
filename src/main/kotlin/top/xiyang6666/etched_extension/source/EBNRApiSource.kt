package top.xiyang6666.etched_extension.source

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import gg.moonflower.etched.api.record.TrackData
import gg.moonflower.etched.api.sound.download.SoundDownloadSource
import gg.moonflower.etched.api.util.DownloadProgressListener
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.ResourceManager
import org.apache.commons.lang3.exception.ExceptionUtils
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.EtchedExtension
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

class EBNRApiSource : SoundDownloadSource {
    companion object {
        private const val API_NAME = "EBNR-APi"
    }

    data class Artist(
        val id: Long, val name: String
    )

    data class SongInfo(
        val id: Long,
        val name: String,
        val artists: List<Artist>,
        val album: AlbumInfo,
    )

    data class AlbumInfo(
        val id: Long,
        val name: String,
        @SerializedName("cover_url") val coverUrl: String,
    )

    data class Album(
        val id: Long,
        val name: String,
        val artists: List<Artist>,
        @SerializedName("cover_url") val coverUrl: String,
        val songs: List<SongInfo>,
    )

    data class Creator(
        @SerializedName("user_id") val userId: Long,
        val nickname: String,
    )

    data class Playlist(
        val id: Long,
        val name: String,
        @SerializedName("cover_url") val coverUrl: String,
        val creator: Creator,
        val tracks: List<SongInfo>,
    )

    data class Audio(
        val url: String?
    )

    private fun parseSong(content: String): SongInfo = Gson().fromJsonTyped(content)
    private fun parseAlbum(content: String): Album = Gson().fromJsonTyped(content)
    private fun parsePlaylist(content: String): Playlist = Gson().fromJsonTyped(content)
    private fun parseAudio(content: String): Audio = Gson().fromJsonTyped(content)

    override fun resolveUrl(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<URL> {
        val uri = URI(s)
        // 这个函数是客户端执行的
        val baseApi = EtchedExtension.clientEbnrApi.removeSuffix("/")
        if (uri.path != "/song") throw RuntimeException("Not a song url: $uri")
        Utils.asyncWarning {
            val client: HttpClient = HttpClient.newHttpClient()
            val infoReq = HttpRequest.newBuilder(URI("$baseApi/info/$s"))
                .GET()
                .setHeader("User-Agent", "Etched-Extension")
                .build()
            val audioReq = HttpRequest.newBuilder(URI("$baseApi/audio/$s"))
                .GET()
                .setHeader("User-Agent", "Etched-Extension")
                .build()
            val infoFuture = client.sendAsync(
                infoReq, HttpResponse.BodyHandlers.ofString()
            )
            val audioFuture = client.sendAsync(
                audioReq, HttpResponse.BodyHandlers.ofString()
            )
            try {
                val audio = parseAudio(audioFuture.get().body())
                if (audio.url != null) return@asyncWarning null
                val info = parseSong(infoFuture.get().body())
                return@asyncWarning Component.translatable("message.vip_song", info.name)
            } catch (e: Exception) {
                EtchedExtension.LOGGER.warn(ExceptionUtils.getStackTrace(e))
                null
            }
        }
        return listOf(URI("$baseApi/resolve/$s").toURL())
    }

    override fun resolveTracks(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<TrackData> {
        val uri = URI(s)
        val baseApi = Config.Common.ebnrApi.get().removeSuffix("/")
        when (uri.path) {
            "/song" -> Utils.get(URI("$baseApi/info/$uri").toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val song = parseSong(content)
                return listOf(
                    TrackData(
                        uri.toString(), song.artists.joinToString("/") { it.name }, Component.literal(song.name)
                    )
                )
            }

            "/album" -> Utils.get(URI("$baseApi/album/$uri").toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return listOf(
                    TrackData(
                        uri.toString(), album.artists.joinToString("/") { it.name }, Component.literal(album.name)
                    )
                ) + album.songs.map { song ->
                    TrackData(
                        "https://music.163.com/song?id=${song.id}",
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                }
            }

            "/playlist" -> Utils.get(URI("$baseApi/playlist/$uri").toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val playlist = parsePlaylist(content)
                return listOf(
                    TrackData(
                        uri.toString(), playlist.creator.nickname, Component.literal(playlist.name)
                    )
                ) + playlist.tracks.map { song ->
                    TrackData(
                        "https://music.163.com/song?id=${song.id}",
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                }
            }

            else -> throw RuntimeException("Unknown or unsupported type: ${uri.path}")
        }
    }

    override fun resolveAlbumCover(
        s: String, listener: DownloadProgressListener?, proxy: Proxy, manager: ResourceManager
    ): Optional<String> {
        val uri = URI(s)
        // 这个函数是客户端执行的
        val baseApi = EtchedExtension.clientEbnrApi.removeSuffix("/")
        when (uri.path) {
            "/album" -> Utils.get(URI("$baseApi/album/$uri").toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return Optional.of(album.coverUrl)
            }

            "/playlist" -> Utils.get(URI("$baseApi/playlist/$uri").toURL(), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val playlist = parsePlaylist(content)
                return Optional.of(playlist.coverUrl)
            }

            else -> return Optional.empty()
        }
    }

    override fun isValidUrl(s: String): Boolean {
        try {
            val uri = URI(s)
            return uri.host == "music.163.com" && setOf("/song", "/playlist", "/album").contains(uri.path)
        } catch (_: URISyntaxException) {
            return false
        }
    }

    override fun isTemporary(s: String): Boolean {
        return true
    }

    override fun getApiName(): String {
        return "ebnr-api"
    }
}