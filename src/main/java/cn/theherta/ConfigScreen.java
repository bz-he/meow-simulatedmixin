package cn.theherta;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import cn.theherta.ModConfig.AutoBreakMode;
import cn.theherta.ModConfig.BatchMode;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private Button batchModeButton;
    private Button autoBreakModeButton;
    private ProcessingSpeedSlider processingSpeedSlider;
    private Button showPacketButton;
    private Button useLastSelectionButton;
    private Button whitelistConfigButton;

    protected ConfigScreen(Screen parent) {
        super(Component.literal("机械动力:方块旋转菜单批发 配置"));
        this.parent = parent;
    }

    public static ConfigScreen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    protected void init() {
        ModConfig config = ModConfig.getInstance();

        // ---- 批量模式按钮 ----
        String batchModeText = getBatchModeText(config.getBatchMode());
        this.batchModeButton = Button.builder(Component.literal("批量方块旋转菜单: " + batchModeText), (button) -> {
            BatchMode currentMode = config.getBatchMode();
            BatchMode newMode = switch (currentMode) {
                case OFF -> BatchMode.GENERAL;
                case GENERAL -> BatchMode.DRAIN_WATER;
                case DRAIN_WATER -> BatchMode.FILL;
                case FILL -> BatchMode.OFF;
            };
            config.setBatchMode(newMode);
            button.setMessage(Component.literal("批量方块旋转菜单: " + getBatchModeText(newMode)));

            Minecraft mc = Minecraft.getInstance();
            String keyName = Submitchange_batchpacket.RESEND_LAST_PACKET_KEY.getTranslatedKeyMessage().getString();
            switch (newMode) {
                case OFF:
                    mc.player.sendSystemMessage(Component.literal("批量处理模式已关闭"));
                    AreaSelectionManager.INSTANCE.fullReset();
                    break;
                case GENERAL:
                    mc.player.sendSystemMessage(Component.literal("批量处理模式已开启（通用）！"));
                    mc.player.sendSystemMessage(Component.literal("步骤1：中键点击方块框选区域"));
                    mc.player.sendSystemMessage(Component.literal("步骤2：按" + keyName + "开始批量处理"));
                    break;
                case DRAIN_WATER:
                    mc.player.sendSystemMessage(Component.literal("批量处理模式已开启（区域排水）！"));
                    mc.player.sendSystemMessage(Component.literal("步骤1：中键点击方块框选区域"));
                    mc.player.sendSystemMessage(Component.literal("步骤2：按" + keyName + "开始排水"));
                case FILL:
                    mc.player.sendSystemMessage(Component.literal("批量处理模式已开启（区域填充）！"));
                    mc.player.sendSystemMessage(Component.literal("步骤1：中键点击方块框选区域"));
                    mc.player.sendSystemMessage(Component.literal("步骤2：按" + keyName + "开始填充流动流体为源"));
                    break;
            }
        }).bounds(this.width / 2 - 100, 40, 200, 20).build();
        this.addRenderableWidget(this.batchModeButton);

        // ---- 自动破方块模式按钮 ----
        String autoBreakText = getAutoBreakModeText(config.getAutoBreakMode());
        this.autoBreakModeButton = Button.builder(Component.literal("自动破方块: " + autoBreakText), (button) -> {
            AutoBreakMode currentMode = config.getAutoBreakMode();
            AutoBreakMode newMode = switch (currentMode) {
                case OFF -> AutoBreakMode.CLICK_WHITELIST;
                case CLICK_WHITELIST -> AutoBreakMode.AREA_WHITELIST;
                case AREA_WHITELIST -> AutoBreakMode.OFF;
            };
            config.setAutoBreakMode(newMode);
            button.setMessage(Component.literal("自动破方块: " + getAutoBreakModeText(newMode)));

            Minecraft mc = Minecraft.getInstance();
            switch (newMode) {
                case OFF:
                    mc.player.sendSystemMessage(Component.literal("自动破方块已关闭"));
                    break;
                case CLICK_WHITELIST:
                    mc.player.sendSystemMessage(Component.literal("自动破方块模式：点击破白名单"));
                    break;
                case AREA_WHITELIST:
                    mc.player.sendSystemMessage(Component.literal("自动破方块模式：区域跟随破白名单"));
                    mc.player.sendSystemMessage(Component.literal("中键框选区域，手持扳手，快捷栏放入活塞"));
            }
        }).bounds(this.width / 2 - 100, 70, 200, 20).build();
        this.addRenderableWidget(this.autoBreakModeButton);

        // ---- 处理速度滑块 ----
        this.processingSpeedSlider = new ProcessingSpeedSlider(this.width / 2 - 100, 105, 200, 20, config.getProcessingSpeed());
        this.addRenderableWidget(this.processingSpeedSlider);

        // ---- 白名单配置按钮 ----
        this.whitelistConfigButton = Button.builder(Component.literal("打开白名单配置"), (button) ->
                Minecraft.getInstance().setScreen(WhitelistConfigScreen.create(this))
        ).bounds(this.width / 2 - 100, 140, 200, 20).build();
        this.addRenderableWidget(this.whitelistConfigButton);

        // ---- 显示最近数据包按钮 ----
        this.showPacketButton = Button.builder(Component.literal("显示最近保存的数据包"), (button) -> {
            String packetInfo = config.getLastSavedPacketInfo();
            if (packetInfo.isEmpty()) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("暂无保存的数据包"));
            } else {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("最近保存的数据包: " + packetInfo));
            }
        }).bounds(this.width / 2 - 100, 170, 200, 20).build();
        this.addRenderableWidget(this.showPacketButton);

        // ---- 使用上次框选区域按钮 ----
        this.useLastSelectionButton = Button.builder(Component.literal("使用上次框选区域"), (button) -> {
            if (AreaSelectionManager.INSTANCE.restoreLastSelection()) {
                BlockPos start = AreaSelectionManager.INSTANCE.getSelectionStart();
                BlockPos end = AreaSelectionManager.INSTANCE.getSelectionEnd();
                Minecraft mc = Minecraft.getInstance();
                mc.player.sendSystemMessage(Component.literal("已恢复上次框选区域"));
                mc.player.sendSystemMessage(Component.literal("起点: " + start.getX() + "," + start.getY() + "," + start.getZ()));
                mc.player.sendSystemMessage(Component.literal("终点: " + end.getX() + "," + end.getY() + "," + end.getZ()));
            } else {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("暂无上次框选记录"));
            }
        }).bounds(this.width / 2 - 100, 200, 200, 20).build();
        this.addRenderableWidget(this.useLastSelectionButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtonStates();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateButtonStates() {
        if (this.useLastSelectionButton != null) {
            this.useLastSelectionButton.active = AreaSelectionManager.INSTANCE.hasLastSelection();
        }
    }

    private String getBatchModeText(BatchMode mode) {
        return switch (mode) {
            case OFF -> "关";
            case GENERAL -> "通用";
            case DRAIN_WATER -> "区域排水";
            case FILL -> "区域填充";
        };
    }

    private String getAutoBreakModeText(AutoBreakMode mode) {
        return switch (mode) {
            case OFF -> "关闭";
            case CLICK_WHITELIST -> "点击破白名单";
            case AREA_WHITELIST -> "区域跟随破白名单";
        };
    }

    // 若未使用可删除
    // private String getWhitelistCompactText() { ... }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    // 内部滑块类
    private static class ProcessingSpeedSlider extends AbstractSliderButton {
        public ProcessingSpeedSlider(int x, int y, int width, int height, int initialValue) {
            super(x, y, width, height, Component.literal(""), (double)(initialValue - 1) / 99.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int speed = (int)(1.0 + this.value * 99.0);
            setMessage(Component.literal("处理速度: " + speed));
        }

        @Override
        protected void applyValue() {
            int speed = (int)(1.0 + this.value * 99.0);
            ModConfig.getInstance().setProcessingSpeed(speed);
            updateMessage();
        }
    }
}