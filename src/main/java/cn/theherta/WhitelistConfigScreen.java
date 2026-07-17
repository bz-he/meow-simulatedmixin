//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class WhitelistConfigScreen extends Screen {
    private static final int MAX_VISIBLE_IDS = 6;
    private final Screen parent;
    private EditBox whitelistInput;
    private String statusText = "";
    private int scrollOffset = 0;

    protected WhitelistConfigScreen(Screen parent) {
        super(Component.literal("自动破方块白名单配置"));
        this.parent = parent;
    }

    public static WhitelistConfigScreen create(Screen parent) {
        return new WhitelistConfigScreen(parent);
    }

    protected void init() {
        this.whitelistInput = new EditBox(this.font, this.width / 2 - 100, 50, 200, 20, Component.literal("白名单方块ID"));
        this.addRenderableWidget(this.whitelistInput);
        this.addRenderableWidget(Button.builder(Component.literal("添加ID"), (button) -> this.addWhitelistByInput()).bounds(this.width / 2 - 100, 80, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("移除ID"), (button) -> this.removeWhitelistByInput()).bounds(this.width / 2 - 32, 80, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("准星加入"), (button) -> this.addWhitelistByCrosshair()).bounds(this.width / 2 + 36, 80, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("返回"), (button) -> this.onClose()).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawSharpCenteredString(guiGraphics, this.title.getString(), this.width / 2, 20, 16777215);
        this.drawSharpCenteredString(guiGraphics, "当前白名单（滚动查看全部）", this.width / 2, 112, 10551200);
        List<String> ids = ModConfig.getInstance().getAutoBreakWhitelistIds();
        this.clampScrollOffset(ids.size());
        if (ids.isEmpty()) {
            this.drawSharpCenteredString(guiGraphics, "(空)", this.width / 2, 126, 16744576);
        } else {
            int y = 126;
            int endExclusive = Math.min(ids.size(), this.scrollOffset + 6);

            for(int i = this.scrollOffset; i < endExclusive; ++i) {
                String id = (String)ids.get(i);
                this.drawSharpCenteredString(guiGraphics, this.getLocalizedBlockName(id), this.width / 2, y, 14737632);
                y += 12;
            }
        }

        if (!this.statusText.isEmpty()) {
            this.drawSharpCenteredString(guiGraphics, this.statusText, this.width / 2, this.height - 45, 16765056);
        }

    }

    private void drawSharpCenteredString(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int textWidth = this.font.width(text);
        int x = centerX - textWidth / 2;
        guiGraphics.drawString(this.font, text, x, y, color, false);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > (double)0.0F) {
            this.scrollBy(-1);
            return true;
        } else if (scrollY < (double)0.0F) {
            this.scrollBy(1);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        } else if (button != 0) {
            return false;
        } else if (this.whitelistInput == null) {
            return false;
        } else {
            List<String> ids = ModConfig.getInstance().getAutoBreakWhitelistIds();
            if (ids.isEmpty()) {
                return false;
            } else {
                this.clampScrollOffset(ids.size());
                int listStartY = 126;
                int rowHeight = 12;
                int listEndY = listStartY + 6 * rowHeight;
                int listMinX = this.width / 2 - 120;
                int listMaxX = this.width / 2 + 120;
                if (!(mouseX < (double)listMinX) && !(mouseX > (double)listMaxX) && !(mouseY < (double)listStartY) && !(mouseY >= (double)listEndY)) {
                    int row = (int)((mouseY - (double)listStartY) / (double)rowHeight);
                    int index = this.scrollOffset + row;
                    if (row >= 0 && row < 6 && index >= 0 && index < ids.size()) {
                        String selectedId = (String)ids.get(index);
                        this.whitelistInput.setValue(selectedId);
                        this.whitelistInput.setCursorPosition(selectedId.length());
                        this.statusText = "已填入: " + selectedId;
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private void scrollBy(int delta) {
        List<String> ids = ModConfig.getInstance().getAutoBreakWhitelistIds();
        if (ids.isEmpty()) {
            this.scrollOffset = 0;
        } else {
            this.scrollOffset += delta;
            this.clampScrollOffset(ids.size());
        }
    }

    private void clampScrollOffset(int size) {
        int maxOffset = Math.max(0, size - 6);
        if (this.scrollOffset < 0) {
            this.scrollOffset = 0;
        } else if (this.scrollOffset > maxOffset) {
            this.scrollOffset = maxOffset;
        }

    }

    private String getLocalizedBlockName(String blockId) {
        ResourceLocation key = ResourceLocation.tryParse(blockId);
        return key == null ? blockId : (String)BuiltInRegistries.BLOCK.getOptional(key).map((block) -> block.getName().getString()).orElse(blockId);
    }

    private void addWhitelistByInput() {
        String id = this.whitelistInput.getValue();
        if (id.trim().isEmpty()) {
            this.statusText = "请输入方块ID";
        } else {
            boolean changed = ModConfig.getInstance().addWhitelistBlockById(id);
            this.statusText = changed ? "已添加: " + id.trim() : "添加失败(无效ID或已存在): " + id.trim();
        }
    }

    private void removeWhitelistByInput() {
        String id = this.whitelistInput.getValue();
        if (id.trim().isEmpty()) {
            this.statusText = "请输入方块ID";
        } else {
            boolean changed = ModConfig.getInstance().removeWhitelistBlockById(id);
            this.statusText = changed ? "已移除: " + id.trim() : "移除失败(无效ID或不存在): " + id.trim();
        }
    }

    private void addWhitelistByCrosshair() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)mc.hitResult;
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            Block block = state.getBlock();
            boolean changed = ModConfig.getInstance().addWhitelistBlock(block);
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            String blockId = key.toString();
            this.statusText = changed ? "已从准星添加: " + blockId : "添加失败(空气或已存在): " + blockId;
            this.whitelistInput.setValue(blockId);
        } else {
            this.statusText = "准星未指向方块";
        }
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }
}