package top.xiyang6666.etched_extension.packet

import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import top.xiyang6666.etched_extension.EtchedExtension
import java.util.function.Supplier


data class EBNRApiPacket(val api: String) {
    constructor(buf: FriendlyByteBuf) : this(buf.readUtf())

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(this.api)
    }

    fun handle(ctx: Supplier<NetworkEvent.Context>) {
        ctx.get().enqueueWork {
            EtchedExtension.clientEbnrApi = this.api
            EtchedExtension.LOGGER.debug("Synchronized server ebnr api: ${this.api}")
        }
        ctx.get().packetHandled = true
    }
}