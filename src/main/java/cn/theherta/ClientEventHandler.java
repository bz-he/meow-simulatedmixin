//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import cn.theherta.ModConfig.BatchMode;
import net.minecraft.world.phys.BlockHitResult;

@EventBusSubscriber({Dist.CLIENT})
public class ClientEventHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 打开配置界面
        if (Submitchange_batchpacket.CONFIG_KEY.consumeClick()) {
            mc.setScreen(ConfigScreen.create(mc.screen));
            return;
        }

        // 自定义状态编辑器快捷键（任何模式下都能用，但仅测试模式有效？可灵活处理）
        if (Submitchange_batchpacket.CUSTOM_STATE_EDITOR_KEY.consumeClick()) {
            if (ModConfig.getInstance().getBatchMode() != BatchMode.TEST) {
                mc.player.sendSystemMessage(Component.literal("请在自定义状态模式下使用此快捷键"));
                return;
            }
            if (mc.hitResult instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState state = mc.level.getBlockState(pos);
                mc.setScreen(new StateEditorScreen(pos, state));
            } else {
                mc.player.sendSystemMessage(Component.literal("请看向一个方块"));
            }
            return;
        }

        // 批量应用快捷键
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
            } else if (batchMode == BatchMode.TEST) {
                if (ModConfig.getInstance().getCustomBlockState() == null) {
                    mc.player.sendSystemMessage(Component.literal("没有保存的自定义状态！请先用快捷键打开编辑器并保存。"));
                    return;
                }
                mc.player.sendSystemMessage(Component.literal("开始批量应用自定义状态..."));
                PacketBatcherNew.INSTANCE.sendBatchPackets(AreaSelectionManager.INSTANCE.getSelectionStart(), AreaSelectionManager.INSTANCE.getSelectionEnd(), null);
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