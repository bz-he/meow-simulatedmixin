package cn.theherta;

import cn.theherta.command.AssemblyLimitCommand;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;          // 添加导入
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
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

        // 获取模组事件总线
        IEventBus modEventBus = modContainer.getEventBus();

        // 注册命令
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // 注册按键绑定（客户端事件，但通过模组总线注册即可）
        if (modEventBus != null) {
            modEventBus.addListener(this::registerKeyMappings);
        }
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(Submitchange_batchpacket.CONFIG_KEY);
        event.register(Submitchange_batchpacket.RESEND_LAST_PACKET_KEY);
        event.register(Submitchange_batchpacket.CUSTOM_STATE_EDITOR_KEY);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AssemblyLimitCommand.register(event.getDispatcher());
    }
}