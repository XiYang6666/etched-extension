package top.xiyang6666.etched_extension

import net.minecraftforge.common.ForgeConfigSpec

object Config {
    object Common {
        private val BUILDER = ForgeConfigSpec.Builder()

        val ebnrApi: ForgeConfigSpec.ConfigValue<String> = BUILDER
            .comment("EvenBetterNeteaseResolver api")
            .define("ebnr_api", "https://ebnr.xiyang6666.top")

        val SPEC: ForgeConfigSpec = BUILDER.build()
    }

    object Client {
        private val BUILDER = ForgeConfigSpec.Builder()
        val showWarnings: ForgeConfigSpec.BooleanValue = BUILDER
            .comment("Configure whether to display API and download related errors")
            .define("showWarnings", true)
        val albumCover = AlbumCover(BUILDER)
        val SPEC: ForgeConfigSpec = BUILDER.build()

        class AlbumCover(builder: ForgeConfigSpec.Builder) {
            val colorDivisions: ForgeConfigSpec.IntValue
            val coverResolution: ForgeConfigSpec.ConfigValue<String>

            init {
                builder
                    .comment("Configuration for customizing Etched's album covers")
                    .push("album_cover")
                colorDivisions = builder
                    .comment(
                        "Controls the color quantization of the album cover. Lower values mean more colors and higher fidelity.",
                        "Higher values create a more pixelated, retro look.",
                        "1 = Max fidelity (no color reduction).",
                        "16 = Etched default.",
                        "Recommended range: 1-64"
                    )
                    .defineInRange("colorDivisions", 1, 1, 256)

                coverResolution = builder
                    .comment(
                        "Determines which album cover resource to use.",
                        "The cover data is defined in assets/etched_extension/textures/item/album_cover_properties_{coverResolution}.json.",
                        "Examples: 16, 32, 64 — each corresponds to a different cover resolution."
                    )
                    .define("coverResolution", "32")
                builder.pop()
            }
        }
    }
}
