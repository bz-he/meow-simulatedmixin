package cn.theherta;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class StateEditorScreen extends Screen {
    private final BlockPos targetPos;
    private final BlockState originalState;
    private final Map<Property<?>, List<Comparable<?>>> propertyValues = new LinkedHashMap<>();
    private final Map<Property<?>, Integer> selectedIndex = new HashMap<>();
    // 记录每个属性行的 Y 坐标，用于渲染文字
    private final Map<Property<?>, Integer> propertyYPositions = new LinkedHashMap<>();

    public StateEditorScreen(BlockPos pos, BlockState initialState) {
        super(Component.literal("方块状态编辑器"));
        this.targetPos = pos;
        this.originalState = initialState;

        for (Property<?> prop : initialState.getProperties()) {
            List<Comparable<?>> values = new ArrayList<>(prop.getPossibleValues());
            propertyValues.put(prop, values);
            Comparable<?> current = initialState.getValue(prop);
            int idx = values.indexOf(current);
            selectedIndex.put(prop, Math.max(idx, 0));
        }
    }

    @Override
    protected void init() {
        int y = 35;
        for (Map.Entry<Property<?>, List<Comparable<?>>> entry : propertyValues.entrySet()) {
            Property<?> prop = entry.getKey();
            List<Comparable<?>> values = entry.getValue();

            // 记录该属性的渲染 Y 坐标
            propertyYPositions.put(prop, y);

            // 左箭头
            addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                int idx = selectedIndex.get(prop);
                idx = (idx - 1 + values.size()) % values.size();
                selectedIndex.put(prop, idx);
            }).bounds(this.width / 2 - 80, y, 20, 20).build());

            // 右箭头
            addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                int idx = selectedIndex.get(prop);
                idx = (idx + 1) % values.size();
                selectedIndex.put(prop, idx);
            }).bounds(this.width / 2 + 60, y, 20, 20).build());

            y += 25;
        }

        int buttonY = y + 10;
        // 保存并应用
        addRenderableWidget(Button.builder(Component.literal("保存并应用"), btn -> saveAndApply())
                .bounds(this.width / 2 - 100, buttonY, 60, 20).build());
        // 仅保存
        addRenderableWidget(Button.builder(Component.literal("仅保存"), btn -> saveOnly())
                .bounds(this.width / 2 - 35, buttonY, 60, 20).build());
        // 取消
        addRenderableWidget(Button.builder(Component.literal("取消"), btn -> onClose())
                .bounds(this.width / 2 + 30, buttonY, 60, 20).build());
    }

    private BlockState buildTargetState() {
        BlockState state = originalState;
        for (Map.Entry<Property<?>, Integer> entry : selectedIndex.entrySet()) {
            Property<?> prop = entry.getKey();
            int idx = entry.getValue();
            Comparable<?> value = propertyValues.get(prop).get(idx);
            state = state.setValue((Property) prop, (Comparable) value);
        }
        return state;
    }

    private void saveAndApply() {
        BlockState targetState = buildTargetState();
        ModConfig.getInstance().setCustomBlockState(targetState);
        ModConfig.getInstance().setUseCustomState(true);
        // 立即应用到当前方块
        RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(targetPos, targetState);
        PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("状态已保存并应用到目标方块。"));
        onClose();
    }

    private void saveOnly() {
        BlockState targetState = buildTargetState();
        ModConfig.getInstance().setCustomBlockState(targetState);
        ModConfig.getInstance().setUseCustomState(true);
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("状态已保存，可通过框选+批量应用。"));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 标题
        guiGraphics.drawCenteredString(this.font, "编辑方块状态", this.width / 2, 12, 0xFFFFFF);

        // 绘制每个属性的当前值（在两个箭头之间）
        for (Map.Entry<Property<?>, List<Comparable<?>>> entry : propertyValues.entrySet()) {
            Property<?> prop = entry.getKey();
            List<Comparable<?>> values = entry.getValue();
            int idx = selectedIndex.get(prop);
            String displayText = BlockStateTranslations.getPropertyName(prop) + ": " +
                    BlockStateTranslations.getValueName(prop, values.get(idx));
            int y = propertyYPositions.get(prop) + 5; // 与按钮垂直对齐
            guiGraphics.drawCenteredString(this.font, displayText, this.width / 2, y, 0xFFFFFF);
        }

        // 底部状态预览
        BlockState target = buildTargetState();
        String originalText = "原始: " + originalState.toString();
        String modifiedText = "修改后: " + target.toString();
        guiGraphics.drawString(this.font, originalText, 10, this.height - 40, 0xAAAAAA);
        guiGraphics.drawString(this.font, modifiedText, 10, this.height - 25, 0xFFFF55);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}