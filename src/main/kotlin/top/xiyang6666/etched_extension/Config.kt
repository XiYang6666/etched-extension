package top.xiyang6666.etched_extension

import net.neoforged.neoforge.common.ModConfigSpec


object Config {
    val CONFIG: EtchedExtensionConfig
    val SPEC: ModConfigSpec

    init {
        val pair = ModConfigSpec.Builder().configure(::EtchedExtensionConfig)
        CONFIG = pair.left
        SPEC = pair.right
    }

    class EtchedExtensionConfig(builder: ModConfigSpec.Builder) {
        val ebnrApi = builder
            .comment("EvenBetterNeteaseResolver api")
            .define("ebnr_api", "https://ebnr.xiyang6666.top")
    }
}
