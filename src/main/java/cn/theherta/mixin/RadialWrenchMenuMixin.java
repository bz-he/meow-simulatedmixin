//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta.mixin;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = {RadialWrenchMenu.class},
        remap = false
)
public abstract class RadialWrenchMenuMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("BatchPacket");
    @Shadow
    @Final
    private BlockPos pos;
    @Shadow
    @Final
    private BlockState state;
    @Shadow
    private List<BlockState> allStates;
    @Shadow
    private int selectedStateIndex;

    @Inject(
            method = {"submitChange()V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void batchpacket$interceptSubmitChange(CallbackInfo ci) {
        if (this.pos != null && this.state != null && this.allStates != null) {
            if (this.selectedStateIndex >= 0 && this.selectedStateIndex < this.allStates.size()) {
                BlockState selectedState = (BlockState)this.allStates.get(this.selectedStateIndex);
                if (selectedState != this.state) {
                    PacketBatcherNew.INSTANCE.captureOriginalPacket(this.pos, selectedState);
                    ModConfig.getInstance().setLastSavedPacketState(selectedState);
                    StringBuilder props = new StringBuilder();
                    selectedState.getValues().forEach((property, value) -> {
                        if (!props.isEmpty()) {
                            props.append(", ");
                        }

                        props.append(property.getName()).append("=").append(value);
                    });
                    String var10000 = selectedState.getBlock().getName().getString();
                    String packetInfo = var10000 + " [" + String.valueOf(props) + "]";
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("保存的数据包: " + packetInfo));
                    ModConfig.getInstance().setLastSavedPacketInfo(packetInfo);
                }
            }
        }
    }
}