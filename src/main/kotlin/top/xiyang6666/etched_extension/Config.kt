package top.xiyang6666.etched_extension

import net.minecraftforge.common.ForgeConfigSpec

object Config {
    private val BUILDER = ForgeConfigSpec.Builder()

    val ebnrApi: ForgeConfigSpec.ConfigValue<String> = BUILDER
        .comment("EvenBetterNeteaseResolver api")
        .define("ebnr_api", "https://ebnr.xiyang6666.top")

    val SPEC: ForgeConfigSpec = BUILDER.build()
}
