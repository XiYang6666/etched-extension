package top.xiyang6666.etched_extension.listener

import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.network.PacketDistributor
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.Network
import top.xiyang6666.etched_extension.packet.EBNRApiPacket

class PlayerListener {
    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        Network.CHANNEL.send(
            PacketDistributor.PLAYER.with { event.entity as ServerPlayer },
            EBNRApiPacket(Config.ebnrApi.get())
        )
    }
}