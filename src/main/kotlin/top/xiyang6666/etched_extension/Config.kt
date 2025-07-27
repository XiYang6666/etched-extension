package top.xiyang6666.etched_extension

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.config.ModConfigEvent

@EventBusSubscriber(modid = EtchedExtension.MODID, bus = EventBusSubscriber.Bus.MOD)
object Config {
    private val BUILDER = ForgeConfigSpec.Builder()

    //    private val DEFAULT_METING_API =
//        BUILDER.comment("默认 meting 服务器").define("default_meting_api", "https://api.injahow.cn/meting/")
    val SPEC: ForgeConfigSpec = BUILDER.build()

//    var defaultMetingApi: String = DEFAULT_METING_API.get()

    @SubscribeEvent
    fun onLoad(event: ModConfigEvent?) {
        if (event?.config?.getSpec() == SPEC) bake()
    }

    fun bake() {
//        defaultMetingApi = DEFAULT_METING_API.get()
    }
}
