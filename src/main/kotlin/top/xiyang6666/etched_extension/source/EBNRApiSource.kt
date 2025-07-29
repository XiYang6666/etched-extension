package top.xiyang6666.etched_extension.source

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import gg.moonflower.etched.api.record.TrackData
import gg.moonflower.etched.api.sound.download.SoundDownloadSource
import gg.moonflower.etched.api.util.DownloadProgressListener
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.ResourceManager
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.*

class EBNRApiSource : SoundDownloadSource {
    companion object {
        const val API_NAME = "EBNR-APi"
    }

    data class Artist(
        val id: Int,
        val name: String
    )

    data class SongInfo(
        val id: Int,
        val name: String,
        val artists: List<Artist>,
        val album: AlbumInfo,
    )

    data class AlbumInfo(
        val id: Int,
        val name: String,
        @SerializedName("cover_url")
        val coverUrl: String,
    )

    data class Album(
        val id: Int,
        val name: String,
        val artists: List<Artist>,
        @SerializedName("cover_url")
        val coverUrl: String,
        val songs: List<SongInfo>,
    )

    private fun parseSong(content: String): SongInfo {
        try {
            return Gson().fromJsonTyped(content)
        } catch (_: JsonSyntaxException) {
            throw RuntimeException("Unknown song")
        }
    }

    private fun parseAlbum(content: String): Album {
        try {
            return Gson().fromJsonTyped(content)
        } catch (_: JsonSyntaxException) {
            throw RuntimeException("Unknown song")
        }
    }

    override fun resolveUrl(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<URL> {
        val uri = URI(s)
        val baseApi = Config.ebnrApi
        when (uri.path) {
            "/song" -> return listOf(URL("$baseApi/resolve/$s"))

            "/album" -> Utils.get(URL("$baseApi/album/$uri"), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return album.songs.map { URL("$baseApi/audio/?id=${it.id}") }
            }

            else -> throw RuntimeException("Unknown or unsupported type: ${uri.path}")
        }
    }

    override fun resolveTracks(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<TrackData> {
        val uri = URI(s)
        val baseApi = Config.ebnrApi
        when (uri.path) {
            "/song" -> Utils.get(URL("$baseApi/info/$uri"), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val song = parseSong(content)
                return listOf(
                    TrackData(
                        uri.toString(),
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                )
            }

            "/album" -> Utils.get(URL("$baseApi/album/$uri"), listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return listOf(
                    TrackData(
                        uri.toString(),
                        album.artists.joinToString("/") { it.name },
                        Component.literal(album.name)
                    )
                ) + album.songs.map { song ->
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
        if (uri.path !== "/album") {
            return Optional.empty()
        }
        val baseApi = Config.ebnrApi
        Utils.get(URL("$baseApi/album/$uri"), listener, API_NAME).use { stream ->
            val content = stream.reader().readText()
            val album = parseAlbum(content)
            return Optional.of(album.coverUrl)
        }
    }

    override fun isValidUrl(s: String): Boolean {
        try {
            val uri = URI(s)
            return uri.host == "music.163.com" && setOf("/song", "/playlist", "/album").contains(uri.path)
        } catch (e: URISyntaxException) {
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