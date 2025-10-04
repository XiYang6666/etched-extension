package top.xiyang6666.etched_extension.packet

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.minecraft.ChatFormatting
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraftforge.network.NetworkEvent
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
            Utils.asyncWarning {
                try {
                    Utils.etchedGet(URI(this.api).toURL(), null, "").use { stream ->
                        val content = stream.reader().readText()
                        val result = Gson().fromJsonTyped<EbnrApiResult>(content)
                        return@asyncWarning if (result.isVip) null
                        else Component.translatable("message.no_vip").withStyle(ChatFormatting.YELLOW)
                    }
                } catch (e: Exception) {
                    val issueStyle = Style.EMPTY
                        .withClickEvent(
                            ClickEvent(
                                ClickEvent.Action.OPEN_URL,
                                "https://github.com/XiYang6666/etched-extension/issues/4"
                            )
                        )
                        .withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.literal("https://github.com/XiYang6666/etched-extension/issues/4")
                            )
                        ).withColor(ChatFormatting.OBFUSCATED)
                    val issueLink = Component.literal("#4").withStyle(issueStyle)
                    return@asyncWarning Component.translatable("message.bad_api", this.api, e.message, issueLink)
                        .withStyle(ChatFormatting.YELLOW)

                }
            }
        }
        ctx.get().packetHandled = true
    }
}