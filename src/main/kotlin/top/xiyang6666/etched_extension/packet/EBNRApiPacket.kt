package top.xiyang6666.etched_extension.packet

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.minecraft.ChatFormatting
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import org.apache.commons.lang3.exception.ExceptionUtils
import top.xiyang6666.etched_extension.EtchedExtension
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.URI
import java.util.function.Supplier


data class EBNRApiPacket(val api: String) {
    constructor(buf: FriendlyByteBuf) : this(buf.readUtf())

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(this.api)
    }

    data class EbnrApiResult(
        @SerializedName("is_vip")
        val isVip: Boolean,
    )

    fun handle(ctx: Supplier<NetworkEvent.Context>) {
        ctx.get().enqueueWork {
            EtchedExtension.clientEbnrApi = this.api
            EtchedExtension.LOGGER.debug("Synchronized server ebnr api: ${this.api}")
            Utils.asyncWarning(Component.translatable("message.no_vip").withStyle(ChatFormatting.YELLOW)) {
                try {
                    Utils.get(URI(this.api).toURL(), null, "").use { stream ->
                        val content = stream.reader().readText()
                        val result = Gson().fromJsonTyped<EbnrApiResult>(content)
                        !result.isVip
                    }
                } catch (e: Exception) {
                    EtchedExtension.LOGGER.warn(ExceptionUtils.getStackTrace(e))
                    true
                }
            }
        }
        ctx.get().packetHandled = true
    }
}