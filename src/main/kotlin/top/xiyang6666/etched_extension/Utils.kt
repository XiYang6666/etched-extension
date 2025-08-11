package top.xiyang6666.etched_extension

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.moonflower.etched.api.util.DownloadProgressListener
import gg.moonflower.etched.api.util.ProgressTrackingInputStream
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture

object Utils {
    inline fun <reified T> Gson.fromJsonTyped(json: String): T = fromJson(json, object : TypeToken<T>() {}.type)
    inline fun <reified T> Gson.fromJsonTyped(reader: Reader): T = fromJson(reader, object : TypeToken<T>() {}.type)

    fun get(url: URL, listener: DownloadProgressListener?, apiName: String): InputStream {
        val questionComponent = Component.translatable("sound_source.etched.requesting", Component.literal(apiName))
        listener?.progressStartRequest(questionComponent)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Etched-Extension")
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) throw RuntimeException("Could not resolve: $url (HTTP $responseCode)")
        val size = connection.contentLengthLong
        return if (size != -1L && listener != null) {
            ProgressTrackingInputStream(connection.inputStream, size, listener)
        } else {
            connection.inputStream
        }
    }

    fun asyncWarning(message: Component, body: () -> Boolean) {
        if (!Config.Client.showWarnings.get()) return
        val instance = Minecraft.getInstance()
        CompletableFuture.supplyAsync(body).thenApply {
            if (!it) return@thenApply
            instance.submit { instance.player?.sendSystemMessage(message) }
        }
    }

    fun asyncWarning(body: () -> Component?) {
        if (!Config.Client.showWarnings.get()) return
        val instance = Minecraft.getInstance()
        CompletableFuture.supplyAsync(body).thenApply {
            if (it == null) return@thenApply
            instance.submit { instance.player?.sendSystemMessage(it) }
        }
    }
}