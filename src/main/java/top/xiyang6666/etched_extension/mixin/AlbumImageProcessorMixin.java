package top.xiyang6666.etched_extension.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import gg.moonflower.etched.client.render.item.AlbumImageProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import top.xiyang6666.etched_extension.client.AlbumProcessor;

@Mixin(value = AlbumImageProcessor.class, remap = false)
public class AlbumImageProcessorMixin {
    /**
     * @author XiYang6666
     * @reason use custom album cover
     */
    @Overwrite
    public static NativeImage apply(NativeImage image, NativeImage overlay) {
        return AlbumProcessor.INSTANCE.apply(image);
    }
}
