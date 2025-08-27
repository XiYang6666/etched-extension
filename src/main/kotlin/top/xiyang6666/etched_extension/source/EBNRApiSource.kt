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
import top.xiyang6666.etched_extension.Utils.NeteaseLinkInfo.Type.*
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.Proxy
import java.net.URI
import java.net.URL
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
        // 这个函数是客户端执行的
        val baseApi = EtchedExtension.clientEbnrApi.removeSuffix("/")
        val linkInfo = Utils.parseNeteaseLink(s) ?: throw RuntimeException("Invalid link: $s")
        val link = linkInfo.normalize()
        if (linkInfo.type != SONG) throw RuntimeException("Not a song link: $s")
        Utils.asyncWarning {
            val infoFuture = Utils.asyncGet("$baseApi/info?id=${linkInfo.id}")
            val audioFuture = Utils.asyncGet("$baseApi/audio?id=${linkInfo.id}")
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
        return listOf(URI("$baseApi/resolve/$link").toURL())
    }

    override fun resolveTracks(s: String, listener: DownloadProgressListener?, proxy: Proxy): List<TrackData> {
        val baseApi = Config.Common.ebnrApi.get().removeSuffix("/")
        val linkInfo = Utils.parseNeteaseLink(s) ?: throw RuntimeException("Invalid link: $s")
        when (linkInfo.type) {
            SONG -> Utils.etchedGet("$baseApi/info?id=${linkInfo.id}", listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val song = parseSong(content)
                return listOf(
                    TrackData(
                        linkInfo.normalize(),
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                )
            }

            ALBUM -> Utils.etchedGet("$baseApi/album?id=${linkInfo.id}", listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return listOf(
                    TrackData(
                        linkInfo.normalize(),
                        album.artists.joinToString("/") { it.name },
                        Component.literal(album.name)
                    )
                ) + album.songs.map { song ->
                    TrackData(
                        Utils.NeteaseLinkInfo(SONG, song.id).normalize(),
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                }
            }

            PLAYLIST -> Utils.etchedGet("$baseApi/playlist?id=${linkInfo.id}", listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val playlist = parsePlaylist(content)
                return listOf(
                    TrackData(
                        linkInfo.normalize(),
                        playlist.creator.nickname,
                        Component.literal(playlist.name)
                    )
                ) + playlist.tracks.map { song ->
                    TrackData(
                        Utils.NeteaseLinkInfo(SONG, song.id).normalize(),
                        song.artists.joinToString("/") { it.name },
                        Component.literal(song.name)
                    )
                }
            }
        }
    }

    override fun resolveAlbumCover(
        s: String, listener: DownloadProgressListener?, proxy: Proxy, manager: ResourceManager
    ): Optional<String> {
        // 这个函数是客户端执行的
        val baseApi = EtchedExtension.clientEbnrApi.removeSuffix("/")
        val linkInfo = Utils.parseNeteaseLink(s) ?: throw RuntimeException("Invalid link: $s")
        when (linkInfo.type) {
            ALBUM -> Utils.etchedGet("$baseApi/album?id=${linkInfo.id}", listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val album = parseAlbum(content)
                return Optional.of(album.coverUrl)
            }

            PLAYLIST -> Utils.etchedGet("$baseApi/playlist?id=${linkInfo.id}", listener, API_NAME).use { stream ->
                val content = stream.reader().readText()
                val playlist = parsePlaylist(content)
                return Optional.of(playlist.coverUrl)
            }

            else -> return Optional.empty()
        }
    }

    override fun isValidUrl(s: String) = Utils.parseNeteaseLink(s) != null
    override fun isTemporary(s: String) = true
    override fun getApiName() = "ebnr-api"
}