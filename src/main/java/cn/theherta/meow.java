package cn.theherta;

import cn.theherta.command.AssemblyLimitCommand;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(meow.MODID)
public class meow {
    public static final String MODID = "meow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public meow(ModContainer modContainer) {
        // 注册服务端配置（文件位于服务端的 config/meow-server.toml）
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AssemblyLimitCommand.register(event.getDispatcher());
    }
}