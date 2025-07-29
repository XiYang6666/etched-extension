package top.xiyang6666.etched_extension

import com.mojang.logging.LogUtils
import gg.moonflower.etched.api.sound.download.SoundSourceManager
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.slf4j.Logger
import top.xiyang6666.etched_extension.source.EBNRApiSource
import top.xiyang6666.etched_extension.source.MetingApiSource

@Mod(EtchedExtension.MODID)
class EtchedExtension {
    companion object {
        const val MODID: String = "etched_extension"
        val LOGGER: Logger = LogUtils.getLogger()
    }

    init {
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC)

        modEventBus.addListener { event: FMLCommonSetupEvent ->
            this.commonSetup(event)
        }
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {

        SoundSourceManager.registerSource(MetingApiSource())
        SoundSourceManager.registerSource(EBNRApiSource())

        LOGGER.info("Using ebnr api: ${Config.ebnrApi.get()}")
    }
}
