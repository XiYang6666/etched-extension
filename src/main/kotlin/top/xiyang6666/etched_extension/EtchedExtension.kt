package top.xiyang6666.etched_extension

import com.mojang.logging.LogUtils
import gg.moonflower.etched.api.sound.download.SoundSourceManager
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import top.xiyang6666.etched_extension.listener.PlayerListener
import top.xiyang6666.etched_extension.source.EBNRApiSource
import top.xiyang6666.etched_extension.source.MetingApiSource

@Mod(EtchedExtension.MODID)
class EtchedExtension {
    companion object {
        const val MODID: String = "etched_extension"
        val LOGGER: Logger = LogUtils.getLogger()
        val version = ModList.get().getModContainerById(MODID).get().modInfo.version.toString()
        lateinit var clientEbnrApi: String
    }

    init {
        LOADING_CONTEXT.registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC)
        LOADING_CONTEXT.registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC)
        Network.register()
        FORGE_BUS.register(PlayerListener)
        MOD_BUS.addListener { _: FMLCommonSetupEvent ->
            SoundSourceManager.registerSource(MetingApiSource())
            SoundSourceManager.registerSource(EBNRApiSource())

            LOGGER.debug("Server ebnr api: {}", Config.Common.ebnrApi.get())
        }
    }
}
