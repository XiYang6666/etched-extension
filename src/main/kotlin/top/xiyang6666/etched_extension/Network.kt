package top.xiyang6666.etched_extension

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import top.xiyang6666.etched_extension.packet.EBNRApiPacket

object Network {
    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToClient(
            EBNRApiPacket.TYPE,
            EBNRApiPacket.STREAM_CODEC,
            EBNRApiPacket::handle
        )
    }
}