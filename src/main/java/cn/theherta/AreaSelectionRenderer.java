//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber({Dist.CLIENT})
public class AreaSelectionRenderer {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            AreaSelectionManager manager = AreaSelectionManager.INSTANCE;
            BlockPos start = manager.getSelectionStart();
            BlockPos end = manager.getSelectionEnd();
            if (start != null) {
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
                PoseStack poseStack = event.getPoseStack();
                Camera camera = event.getCamera();
                Vec3 camPos = camera.getPosition();
                poseStack.pushPose();
                poseStack.translate(-camPos.x(), -camPos.y(), -camPos.z());
                LevelRenderer.renderLineBox(poseStack, vertexConsumer, toSingleBlockAabb(start), 1.0F, 0.9F, 0.2F, 1.0F);
                if (end != null) {
                    LevelRenderer.renderLineBox(poseStack, vertexConsumer, toSelectionAabb(start, end), 1.0F, 0.0F, 0.0F, 1.0F);
                } else {
                    BlockPos previewEnd = getPreviewEndPos(mc);
                    if (previewEnd != null) {
                        LevelRenderer.renderLineBox(poseStack, vertexConsumer, toSelectionAabb(start, previewEnd), 0.55F, 0.85F, 1.0F, 1.0F);
                    }
                }

                poseStack.popPose();
                bufferSource.endBatch();
            }
        }
    }

    private static BlockPos getPreviewEndPos(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        } else {
            return null;
        }
    }

    private static AABB toSingleBlockAabb(BlockPos pos) {
        return new AABB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
    }

    private static AABB toSelectionAabb(BlockPos start, BlockPos end) {
        return new AABB((double)Math.min(start.getX(), end.getX()), (double)Math.min(start.getY(), end.getY()), (double)Math.min(start.getZ(), end.getZ()), (double)(Math.max(start.getX(), end.getX()) + 1), (double)(Math.max(start.getY(), end.getY()) + 1), (double)(Math.max(start.getZ(), end.getZ()) + 1));
    }
}