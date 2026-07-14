package cn.theherta;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(meow.MODID)
public class meow {
    public static final String MODID = "meow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public meow(IEventBus modEventBus, ModContainer modContainer) {
        // 注册服务端配置（文件位于服务端的 config/meow-server.toml）
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}