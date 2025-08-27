package top.xiyang6666.etched_extension.mixin;

import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import top.xiyang6666.etched_extension.Utils;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = SoundDownloadSource.class, remap = false)
public interface SoundDownloadSourceMixin {
    /**
     * @author XiYang6666
     * @reason modify headers
     */
    @Overwrite
    static Map<String, String> getDownloadHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Utils.INSTANCE.getUserAgent());
        return headers;
    }
}
