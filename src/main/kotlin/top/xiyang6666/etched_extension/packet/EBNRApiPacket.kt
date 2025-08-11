package top.xiyang6666.etched_extension.packet

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.netty.buffer.ByteBuf
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.EtchedExtension
import top.xiyang6666.etched_extension.Utils
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.net.URI
import java.util.concurrent.CompletableFuture


data class EBNRApiPacket(val api: String) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<EBNRApiPacket>(
            ResourceLocation.fromNamespaceAndPath(
                EtchedExtension.MODID, "ebnr_api"
            )
        )
        val STREAM_CODEC: StreamCodec<ByteBuf, EBNRApiPacket> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EBNRApiPacket::api, ::EBNRApiPacket
        )
    }

    data class EbnrApiResult(
        @SerializedName("is_vip")
        val isVip: Boolean,
    )

    fun handle(ctx: IPayloadContext) {
        ctx.enqueueWork {
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
                    EtchedExtension.LOGGER.warn(e)
                    true
                }
            }
        }
    }

    override fun type() = TYPE
}