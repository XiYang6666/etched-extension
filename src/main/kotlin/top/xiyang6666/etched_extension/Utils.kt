package top.xiyang6666.etched_extension

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.moonflower.etched.api.util.DownloadProgressListener
import gg.moonflower.etched.api.util.ProgressTrackingInputStream
import gg.moonflower.etched.core.Etched
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.neoforged.fml.ModList
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

object Utils {
    val minecraftVersion: String = SharedConstants.getCurrentVersion().name
    val etchedVersion: String = ModList.get().getModContainerById(Etched.MOD_ID).get().modInfo.version.toString()
    val UserAgent =
        "MinecraftJava/$minecraftVersion Etched/$etchedVersion Etched-Extension/${EtchedExtension.version}"

    inline fun <reified T> Gson.fromJsonTyped(json: String): T = fromJson(json, object : TypeToken<T>() {}.type)
    inline fun <reified T> Gson.fromJsonTyped(reader: Reader): T = fromJson(reader, object : TypeToken<T>() {}.type)

    fun etchedGet(url: URL, listener: DownloadProgressListener?, apiName: String): InputStream {
        val questionComponent = Component.translatable("sound_source.etched.requesting", Component.literal(apiName))
        listener?.progressStartRequest(questionComponent)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", UserAgent)
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) throw RuntimeException("Could not resolve: $url (HTTP $responseCode)")
        val size = connection.contentLengthLong
        return if (size != -1L && listener != null) {
            ProgressTrackingInputStream(connection.inputStream, size, listener)
        } else {
            connection.inputStream
        }
    }

    fun etchedGet(url: String, listener: DownloadProgressListener?, apiName: String): InputStream =
        etchedGet(URI(url).toURL(), listener, apiName)

    fun asyncGet(url: String): CompletableFuture<HttpResponse<String>> {
        val client: HttpClient = HttpClient.newHttpClient()
        val req = HttpRequest.newBuilder(URI(url))
            .GET()
            .setHeader("User-Agent", UserAgent)
            .build()
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
    }

    fun asyncWarning(body: () -> Component?) {
        if (!Config.Client.showWarnings.get()) return
        val instance = Minecraft.getInstance()
        CompletableFuture.supplyAsync(body).thenApply {
            if (it == null) return@thenApply
            instance.submit { instance.player?.sendSystemMessage(it) }
        }
    }

    fun verifyUrl(str: String) = runCatching { URI(str).toURL() }.getOrNull() != null

    data class NeteaseLinkInfo(
        val type: Type,
        val id: Long
    ) {
        enum class Type { SONG, ALBUM, PLAYLIST }

        fun normalize() = "https://music.163.com/${type.name.lowercase()}?id=$id"
    }


    fun parseNeteaseLink(link: String): NeteaseLinkInfo? {
        val uri = runCatching { URI(link) }.getOrElse { return null }

        // 验证 scheme
        val scheme = uri.scheme.orEmpty()
        if (scheme.isNotEmpty() && scheme !in listOf("http", "https")) return null

        // 验证 host
        val host = when {
            scheme.isEmpty() && uri.host.isNullOrEmpty() -> {
                if (!uri.path.orEmpty().matches(Regex("^(?:y\\.)?music\\.163\\.com.*"))) return null
                "music.163.com"
            }

            else -> uri.host ?: return null
        }

        if (host !in listOf("music.163.com", "y.music.163.com")) return null

        // 解析优先级：1. 路径+ID格式 2. 路径+查询参数 3. 片段(fragment)
        return parseDirectPathFormat(uri.path)
               ?: parsePathWithQuery(uri.path, uri.query)
               ?: parseFragment(uri.fragment)
    }

    /**
     * 解析直接包含ID的路径格式:
     * - /song/123456
     * - /m/song/123456
     */
    private fun parseDirectPathFormat(path: String?): NeteaseLinkInfo? {
        if (path.isNullOrEmpty()) return null

        Regex("^/?(?:m/)?(song|album|playlist)/(\\d+)(?:/.*)?$").find(path)?.let { match ->
            val type = parseType(match.groupValues[1]) ?: return null
            val id = match.groupValues[2].toLongOrNull() ?: return null
            return NeteaseLinkInfo(type, id)
        }

        return null
    }

    /**
     * 解析路径+查询参数格式:
     * - /song?id=123456
     * - /m/song?id=123456
     */
    private fun parsePathWithQuery(path: String?, query: String?): NeteaseLinkInfo? {
        if (path.isNullOrEmpty() || query.isNullOrEmpty()) return null

        // 从路径中提取类型
        val type = extractTypeFromPath(path) ?: return null

        // 从查询参数中提取ID
        val id = extractIdFromQuery(query) ?: return null

        return NeteaseLinkInfo(type, id)
    }

    /**
     * 解析片段(fragment)格式:
     * - #/song/123456
     * - #/song?id=123456
     * - #/m/song/123456
     * - #/m/song?id=123456
     */
    private fun parseFragment(fragment: String?): NeteaseLinkInfo? {
        if (fragment.isNullOrEmpty()) return null

        // 尝试片段中的直接路径格式
        parseDirectPathFormat(fragment)?.let { return it }

        // 尝试片段中的路径+查询参数格式
        if (fragment.contains("?")) {
            val pathPart = fragment.substringBefore("?")
            val queryPart = fragment.substringAfter("?")
            parsePathWithQuery(pathPart, queryPart)?.let { return it }
        }

        return null
    }

    private fun extractTypeFromPath(path: String): NeteaseLinkInfo.Type? {
        Regex("^/?(?:m/)?(song|album|playlist)(?:/.*)?$").find(path)?.let { match ->
            return parseType(match.groupValues[1])
        }
        return null
    }

    private fun extractIdFromQuery(query: String): Long? {
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .firstOrNull { it.first == "id" }
            ?.second
            ?.toLongOrNull()
    }

    private fun parseType(typeString: String): NeteaseLinkInfo.Type? {
        return when (typeString.lowercase()) {
            "song" -> NeteaseLinkInfo.Type.SONG
            "album" -> NeteaseLinkInfo.Type.ALBUM
            "playlist" -> NeteaseLinkInfo.Type.PLAYLIST
            else -> null
        }
    }
}