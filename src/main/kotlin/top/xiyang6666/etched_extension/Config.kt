package top.xiyang6666.etched_extension

import net.neoforged.neoforge.common.ModConfigSpec


object Config {
    object Common {
        private val BUILDER = ModConfigSpec.Builder()
        val ebnrApi: ModConfigSpec.ConfigValue<String> = BUILDER
            .comment("EvenBetterNeteaseResolver api")
            .define("ebnr_api", "https://ebnr.xiyang6666.top")
        val SPEC: ModConfigSpec = BUILDER.build()
    }

    object Client {
        private val BUILDER = ModConfigSpec.Builder()
        val albumCover = AlbumCover(BUILDER)
        val SPEC: ModConfigSpec = BUILDER.build()

        class AlbumCover(builder: ModConfigSpec.Builder) {
            val colorDivisions: ModConfigSpec.IntValue
            val coverResolution: ModConfigSpec.ConfigValue<String>

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
