//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import cn.theherta.ModConfig.AutoBreakMode;

@EventBusSubscriber({Dist.CLIENT})
public enum AreaSelectionManager {
    INSTANCE;

    private boolean batchModeActive = false;
    private BlockPos selectionStart = null;
    private BlockPos selectionEnd = null;
    private BlockState originalPacketData;
    private long selectionStartTime = 0L;
    private BlockState lastPacketState = null;
    private BlockPos lastSelectionStart = null;
    private BlockPos lastSelectionEnd = null;

    public void reset() {
        this.batchModeActive = false;
        this.selectionStart = null;
        this.selectionEnd = null;
        this.originalPacketData = null;
        this.selectionStartTime = 0L;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("批量处理模式已关闭"));
        }

    }

    public void fullReset() {
        this.batchModeActive = false;
        this.selectionStart = null;
        this.selectionEnd = null;
        this.originalPacketData = null;
        this.selectionStartTime = 0L;
    }

    public void clearSelection() {
        this.selectionStart = null;
        this.selectionEnd = null;
        this.originalPacketData = null;
        this.selectionStartTime = 0L;
    }

    public boolean isBatchModeEnabled() {
        return ModConfig.getInstance().isBatchModeEnabled();
    }

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            if (mc.screen == null) {
                boolean areaBedrockModeEnabled = ModConfig.getInstance().getAutoBreakMode() == AutoBreakMode.AREA_WHITELIST;
                if (INSTANCE.isBatchModeEnabled() || areaBedrockModeEnabled) {
                    if (event.getButton() == 1 && event.getAction() == 1) {
                        if (INSTANCE.selectionStart != null && INSTANCE.selectionEnd == null) {
                            INSTANCE.clearSelection();
                            event.setCanceled(true);
                            mc.player.displayClientMessage(Component.literal("已取消框选"), true);
                        }

                    } else {
                        if (event.getButton() == 2) {
                            if (event.getAction() != 1) {
                                return;
                            }

                            HitResult hitResult = mc.hitResult;
                            if (hitResult instanceof BlockHitResult) {
                                BlockHitResult blockHit = (BlockHitResult)hitResult;
                                event.setCanceled(true);
                                BlockPos clickedPos = blockHit.getBlockPos();
                                if (INSTANCE.selectionStart != null && INSTANCE.selectionEnd != null) {
                                    INSTANCE.selectionStart = clickedPos;
                                    INSTANCE.selectionEnd = null;
                                    INSTANCE.selectionStartTime = System.currentTimeMillis();
                                    return;
                                }

                                if (INSTANCE.selectionStart == null) {
                                    INSTANCE.selectionStart = clickedPos;
                                    INSTANCE.selectionStartTime = System.currentTimeMillis();
                                } else {
                                    long elapsedTime = System.currentTimeMillis() - INSTANCE.selectionStartTime;
                                    if (elapsedTime < 1000L) {
                                        return;
                                    }

                                    INSTANCE.selectionEnd = clickedPos;
                                    INSTANCE.lastSelectionStart = INSTANCE.selectionStart;
                                    INSTANCE.lastSelectionEnd = INSTANCE.selectionEnd;
                                    if (INSTANCE.isBatchModeEnabled()) {
                                        String keyName = Submitchange_batchpacket.RESEND_LAST_PACKET_KEY.getTranslatedKeyMessage().getString();
                                        mc.player.displayClientMessage(Component.literal("区域已选择！按" + keyName + "开始批量处理"), true);
                                    } else if (areaBedrockModeEnabled) {
                                        mc.player.displayClientMessage(Component.literal("区域已选择！"), true);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public void setOriginalPacketData(BlockState data) {
        this.originalPacketData = data;
    }

    public void saveLastPacketData(BlockState state) {
        this.lastPacketState = state;
    }

    public boolean isBatchModeActive() {
        return this.batchModeActive;
    }

    public BlockPos getSelectionStart() {
        return this.selectionStart;
    }

    public BlockPos getSelectionEnd() {
        return this.selectionEnd;
    }

    public boolean hasActiveSelection() {
        return this.selectionStart != null && this.selectionEnd != null;
    }

    public boolean isWithinSelection(BlockPos pos) {
        if (this.hasActiveSelection() && pos != null) {
            int minX = Math.min(this.selectionStart.getX(), this.selectionEnd.getX());
            int minY = Math.min(this.selectionStart.getY(), this.selectionEnd.getY());
            int minZ = Math.min(this.selectionStart.getZ(), this.selectionEnd.getZ());
            int maxX = Math.max(this.selectionStart.getX(), this.selectionEnd.getX());
            int maxY = Math.max(this.selectionStart.getY(), this.selectionEnd.getY());
            int maxZ = Math.max(this.selectionStart.getZ(), this.selectionEnd.getZ());
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        } else {
            return false;
        }
    }

    public BlockState getOriginalPacketData() {
        return this.originalPacketData;
    }

    public boolean hasLastSelection() {
        return this.lastSelectionStart != null && this.lastSelectionEnd != null;
    }

    public boolean restoreLastSelection() {
        if (!this.hasLastSelection()) {
            return false;
        } else {
            this.selectionStart = new BlockPos(this.lastSelectionStart);
            this.selectionEnd = new BlockPos(this.lastSelectionEnd);
            this.selectionStartTime = System.currentTimeMillis();
            return true;
        }
    }

    public BlockPos getLastSelectionStart() {
        return this.lastSelectionStart;
    }

    public BlockPos getLastSelectionEnd() {
        return this.lastSelectionEnd;
    }
}