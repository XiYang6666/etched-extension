package top.xiyang6666.etched_extension

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.config.ModConfigEvent

@EventBusSubscriber(modid = EtchedExtension.MODID, bus = EventBusSubscriber.Bus.MOD)
object Config {
    private val BUILDER = ForgeConfigSpec.Builder()

    private val EBNR_API = BUILDER
        .comment("ebnr api 地址")
        .define("ebnr_api", "https://ebnr.xiyang6666.top")
    val SPEC: ForgeConfigSpec = BUILDER.build()

    var ebnrApi = EBNR_API.get()

    @SubscribeEvent
    fun onLoad(event: ModConfigEvent?) {
        if (event?.config?.getSpec() == SPEC) bake()
    }

    fun bake() {
        ebnrApi = EBNR_API.get()
    }
}
