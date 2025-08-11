package top.xiyang6666.etched_extension

import gg.moonflower.etched.api.sound.download.SoundSourceManager
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import top.xiyang6666.etched_extension.listener.PlayerListener
import top.xiyang6666.etched_extension.source.EBNRApiSource
import top.xiyang6666.etched_extension.source.MetingApiSource

@Mod(EtchedExtension.MODID)
class EtchedExtension(container: ModContainer) {
    companion object {
        const val MODID: String = "etched_extension"
        val LOGGER = LogManager.getLogger(MODID)
        lateinit var clientEbnrApi: String
    }

    init {
        container.registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC)
        container.registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC)
        MOD_BUS.register(Network)
        FORGE_BUS.register(PlayerListener)
        MOD_BUS.addListener { _: FMLCommonSetupEvent ->
            SoundSourceManager.registerSource(MetingApiSource())
            SoundSourceManager.registerSource(EBNRApiSource())

            LOGGER.debug("Server ebnr api: {}", Config.Common.ebnrApi.get())
        }
    }
}
