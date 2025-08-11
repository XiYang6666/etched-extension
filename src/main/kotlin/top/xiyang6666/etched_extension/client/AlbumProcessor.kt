package top.xiyang6666.etched_extension.client

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import top.xiyang6666.etched_extension.Config
import top.xiyang6666.etched_extension.EtchedExtension
import top.xiyang6666.etched_extension.Utils.fromJsonTyped
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.pow


object AlbumProcessor {
    data class Properties(
        @SerializedName("template_texture")
        val templateTexture: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )


    fun getOverlay(): Pair<Properties, NativeImage> {
        val resourceManager: ResourceManager = Minecraft.getInstance().getResourceManager()
        val resolution = Config.Client.albumCover.coverResolution.get()
        val propertiesResource = resourceManager.getResourceOrThrow(
            ResourceLocation.fromNamespaceAndPath(
                EtchedExtension.MODID,
                "textures/item/album_cover_properties_$resolution.json"
            )
        )
        val properties: Properties = propertiesResource.open().use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                Gson().fromJsonTyped(reader)
            }
        }

        val imageResource = resourceManager.getResourceOrThrow(
            ResourceLocation.parse(properties.templateTexture)
        )
        return imageResource.open().use { stream ->
            properties to NativeImage.read(stream)
        }
    }

    /** 一个用于 sRGB 到线性色彩空间转换的查找表（伽马校正，2.2 次幂）。 */
    private val POW22 = FloatArray(256) { i ->
        (i / 255.0f).pow(2.2f)
    }

    fun apply(image: NativeImage): NativeImage {
        // 获取模板属性和模板图像，并使用 'use' 块确保模板图像资源在使用后被关闭
        val (properties, overlay) = getOverlay()
        return overlay.use { ov ->
            // 使用另一个 'use' 块确保输入的源图像资源也被关闭
            image.use { img ->
                // 1. 将模板图像(overlay)完整复制一份作为结果图像的底稿。
                //    这样可以保留模板上目标区域以外的所有内容。
                val resultImage = NativeImage(ov.width, ov.height, true)
                resultImage.copyFrom(ov)

                // 2. 计算缩放因子。将完整的源图像(img)缩放到模板上由properties定义的目标矩形区域。
                //    乘以2是因为我们每次从源图像中取一个2x2的像素块进行混合。
                val xFactor = img.width.toFloat() / (properties.width * 2)
                val yFactor = img.height.toFloat() / (properties.height * 2)

                // 3. 只遍历由properties定义的目标矩形区域。
                for (m in properties.x until (properties.x + properties.width)) {
                    for (n in properties.y until (properties.y + properties.height)) {
                        // 4. 计算当前像素在目标矩形内的相对坐标。
                        val relativeM = m - properties.x
                        val relativeN = n - properties.y

                        // 5. 根据相对坐标和缩放因子，计算在源图像(img)中对应的2x2像素区域。
                        val x1 = (xFactor * relativeM * 2).toInt()
                        val x2 = (xFactor * (relativeM * 2 + 1)).toInt()
                        val y1 = (yFactor * relativeN * 2).toInt()
                        val y2 = (yFactor * (relativeN * 2 + 1)).toInt()

                        // 使用伽马校正混合，将源图像的2x2区域缩减为一个像素颜色(baseColor)
                        val baseColor = blendPixelQuad(
                            img.getPixelRGBA(x1, y1),
                            img.getPixelRGBA(x2, y1),
                            img.getPixelRGBA(x1, y2),
                            img.getPixelRGBA(x2, y2)
                        )

                        // 获取模板上同一位置的像素颜色(overlayColor)
                        val overlayColor = ov.getPixelRGBA(m, n)

                        // 对每个颜色通道应用正片叠底混合与色阶控制
                        val r = multiplyAndQuantizeChannel(baseColor, overlayColor, 16)
                        val g = multiplyAndQuantizeChannel(baseColor, overlayColor, 8)
                        val b = multiplyAndQuantizeChannel(baseColor, overlayColor, 0)

                        // 将处理后的颜色通道与模板的Alpha通道合并，生成最终颜色
                        val finalColor = (overlayColor and 0xFF000000.toInt()) or r or g or b

                        // 在结果图像的相应位置设置最终颜色
                        resultImage.setPixelRGBA(m, n, finalColor)
                    }
                }
                resultImage
            }
        }
    }

    /**
     * 对单个颜色通道，组合执行正片叠底混合与颜色量化。
     */
    private fun multiplyAndQuantizeChannel(baseColor: Int, overlayColor: Int, bitOffset: Int): Int {
        val baseChannel = (baseColor shr bitOffset) and 0xFF
        val overlayChannel = (overlayColor shr bitOffset) and 0xFF

        val multiplied = multiplyBlendChannel(baseChannel, overlayChannel)
        val quantized = quantizeChannel(multiplied)

        return (quantized and 0xFF) shl bitOffset
    }

    /**
     * 对单个颜色通道应用正片叠底混合。
     * 计算公式：结果 = (基础色 * 叠加色) / 255
     */
    private fun multiplyBlendChannel(base: Int, overlay: Int): Int {
        return (base.toFloat() * (overlay.toFloat() / 255.0f)).toInt()
    }

    /**
     * 将一个通道的颜色数量减少到固定的阶数（色阶控制）。
     */
    private fun quantizeChannel(channel: Int): Int {
        val divisions = Config.Client.albumCover.colorDivisions.get()
        return (channel / divisions) * divisions
    }

    /**
     * 使用伽马校正平均法，将一个 2x2 的像素方块混合成单个像素。
     * Alpha 通道会被忽略，并设置为完全不透明 (0xFF)。
     */
    private fun blendPixelQuad(c1: Int, c2: Int, c3: Int, c4: Int): Int {
        val r = gammaBlend(c1, c2, c3, c4, 16)
        val g = gammaBlend(c1, c2, c3, c4, 8)
        val b = gammaBlend(c1, c2, c3, c4, 0)
        return (0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }

    /**
     * 以伽马校正的方式平均四个通道的值。
     * 该方法将 sRGB 值转换为线性空间，取平均值，然后再转换回 sRGB 空间。
     */
    private fun gammaBlend(c1: Int, c2: Int, c3: Int, c4: Int, bitOffset: Int): Int {
        // 从 sRGB 转换到线性空间
        val f = getPow22((c1 shr bitOffset) and 0xFF)
        val g = getPow22((c2 shr bitOffset) and 0xFF)
        val h = getPow22((c3 shr bitOffset) and 0xFF)
        val i = getPow22((c4 shr bitOffset) and 0xFF)

        // 在线性空间中取平均值
        val avgLinear = (f + g + h + i) * 0.25f

        // 转换回 sRGB 空间（1/2.2 次幂 ≈ 0.4545）
        val avgGamma = avgLinear.pow(0.45454547f)

        return (avgGamma * 255.0f).toInt()
    }

    /**
     * 查找一个 8 位 sRGB 值对应的预计算线性值。
     */
    private fun getPow22(value: Int): Float {
        return POW22[value and 0xFF]
    }
}