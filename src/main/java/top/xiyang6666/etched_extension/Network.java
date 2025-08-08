package top.xiyang6666.etched_extension;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import top.xiyang6666.etched_extension.packet.EBNRApiPacket;

public class Network {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EtchedExtension.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                EBNRApiPacket.class,
                EBNRApiPacket::encode,
                EBNRApiPacket::new,
                EBNRApiPacket::handle
        );
    }
}
