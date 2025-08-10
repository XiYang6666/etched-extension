package top.xiyang6666.etched_extension.listener

import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.network.PacketDistributor
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.packet.EBNRApiPacket

object PlayerListener {
    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        PacketDistributor.sendToPlayer(event.entity as ServerPlayer, EBNRApiPacket(Config.CONFIG.ebnrApi.get()))
    }
}