//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import cn.theherta.ModConfig.BatchMode;

@EventBusSubscriber({Dist.CLIENT})
public class ClientEventHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (Submitchange_batchpacket.CONFIG_KEY.consumeClick()) {
                mc.setScreen(ConfigScreen.create(mc.screen));
            }

            if (Submitchange_batchpacket.RESEND_LAST_PACKET_KEY.consumeClick()) {
                if (!ModConfig.getInstance().isBatchModeEnabled()) {
                    mc.player.sendSystemMessage(Component.literal("请先在配置界面中开启批量处理模式！"));
                    return;
                }

                if (AreaSelectionManager.INSTANCE.getSelectionStart() == null || AreaSelectionManager.INSTANCE.getSelectionEnd() == null) {
                    mc.player.sendSystemMessage(Component.literal("请先完成区域选择！"));
                    return;
                }

                ModConfig.BatchMode batchMode = ModConfig.getInstance().getBatchMode();
                if (batchMode == BatchMode.DRAIN_WATER) {
                    mc.player.sendSystemMessage(Component.literal("开始区域排水..."));
                    PacketBatcherNew.INSTANCE.sendBatchPackets(AreaSelectionManager.INSTANCE.getSelectionStart(), AreaSelectionManager.INSTANCE.getSelectionEnd(), (Object)null);
                } else if (batchMode == BatchMode.FILL) {
                    mc.player.sendSystemMessage(Component.literal("开始区域填充..."));
                    PacketBatcherNew.INSTANCE.sendBatchPackets(AreaSelectionManager.INSTANCE.getSelectionStart(), AreaSelectionManager.INSTANCE.getSelectionEnd(), (Object)null);
                } else {
                    if (ModConfig.getInstance().getLastSavedPacketState() == null) {
                        mc.player.sendSystemMessage(Component.literal("没有保存的数据包！请先使用方块旋转菜单保存数据包。"));
                        return;
                    }

                    mc.player.sendSystemMessage(Component.literal("应用上次保存的数据包..."));
                    PacketBatcherNew.INSTANCE.sendBatchPackets(AreaSelectionManager.INSTANCE.getSelectionStart(), AreaSelectionManager.INSTANCE.getSelectionEnd(), ModConfig.getInstance().getLastSavedPacketState());
                }

                AreaSelectionManager.INSTANCE.clearSelection();
            }

        }
    }

    @SubscribeEvent
    public static void onMouseLeftClick(InputEvent.MouseButton.Pre event) {
        if (event.getButton() == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                if (AutoBreakBedrock.handleLeftClick(mc, event.getAction() == 1)) {
                    event.setCanceled(true);
                }

            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && event.getEntity() == mc.player) {
                AutoBreakBedrock.tick();
            }
        }
    }
}