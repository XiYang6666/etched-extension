package top.xiyang6666.etched_extension.packet

import com.google.gson.Gson
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import top.xiyang6666.etched_extension.EtchedExtension
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier


data class EBNRApiPacket(val api: String) {
    constructor(buf: FriendlyByteBuf) : this(buf.readUtf())

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(this.api)
    }

    data class EbnrApiResult(
        val message: String,
        val is_vip: Boolean,
    )

    fun handle(ctx: Supplier<NetworkEvent.Context>) {
        ctx.get().enqueueWork {
            EtchedExtension.clientEbnrApi = this.api
            EtchedExtension.LOGGER.debug("Synchronized server ebnr api: ${this.api}")
            val instance = Minecraft.getInstance()

            CompletableFuture.supplyAsync {
                try {
                    Utils.get(URI(this.api).toURL(), null, "").use { stream ->
                        val content = stream.reader().readText()
                        val result = Gson().fromJsonTyped<EbnrApiResult>(content)
                        return@supplyAsync result.is_vip
                    }
                } catch (_: Exception) {
                    return@supplyAsync false
                }
            }.thenApply {
                if (!it) instance.submit {
                    instance.player?.sendSystemMessage(
                        Component.translatable("message.no_vip").withStyle(ChatFormatting.YELLOW)
                    )
                }
            }
        }
        ctx.get().packetHandled = true
    }
}