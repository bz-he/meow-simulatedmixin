package cn.theherta.mixin;

import cn.theherta.Config;
import cn.theherta.meow;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

@Mixin(AssemblePacket.class)
public class AssemblePacketMixin {

    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/simulated_team/simulated/content/blocks/physics_assembler/PhysicsAssemblerBlockEntity;assembleOrDisassemble()V"
            ),
            cancellable = true
    )
    private void onAssemblePacket(ServerPacketContext context, CallbackInfo ci) {
        ServerPlayer player = context.player();
        // 安全检查：确保玩家所在世界是 ServerLevel
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = ((AssemblePacket) (Object) this).pos();
        // 防止空指针异常
        if (pos == null) {
            meow.LOGGER.warn("收到一个 pos 为 null 的 AssemblePacket，已忽略");
            ci.cancel(); // 阻止继续执行原方法
            return;
        }

        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (!(be instanceof PhysicsAssemblerBlockEntity assembler)) {
            return;
        }

        int maxSize = Config.MAX_STRUCTURE_SIZE.get();
        long size;

        // 使用 try-catch 保护结构大小计算，防止内部异常导致全部崩掉
        try {
            size = calculateStructureSize(assembler, serverLevel);
        } catch (Exception e) {
            meow.LOGGER.error("计算结构大小时发生异常", e);
            if (player.hasPermissions(2)) {
                // OP 玩家在计算失败时放行（但记录日志）
                return;
            } else {
                // 非 OP 玩家，无法确定大小，阻止操作并通知
                ci.cancel();
                player.sendSystemMessage(Component.literal("无法计算结构大小，操作已被阻止"));
                warnOps(serverLevel, player, -1, maxSize, pos);
                return;
            }
        }

        // 获取玩家所在的主世界坐标，并附上维度信息
        BlockPos playerPos = player.blockPosition();
        String dimensionName = serverLevel.dimension().location().toString();
        String locationDesc = String.format("%s 维度 [%d, %d, %d]", dimensionName, playerPos.getX(), playerPos.getY(), playerPos.getZ());

        // 所有操作都记录日志（带维度）
        meow.LOGGER.info("玩家 {} 在 {} 执行了组装/拆卸操作，结构大小 {} 方块（阈值 {}）",
                player.getName().getString(), locationDesc, size, maxSize);

        // OP 玩家不受限制（但仍然记录日志）
        if (player.hasPermissions(2)) {
            return;
        }

        // 非 OP 玩家检查大小
        if (size > maxSize) {
            ci.cancel();
            player.sendSystemMessage(
                    Component.literal("结构过大（约 " + size + " 方块），操作已被阻止")
            );
            warnOps(serverLevel, player, size, maxSize, playerPos);
        }
    }

    /**
     * 向所有在线 OP 发送警告消息
     */
    private void warnOps(ServerLevel level, ServerPlayer offender, long size, int maxSize, BlockPos pos) {
        MinecraftServer server = level.getServer();
        String dimensionName = level.dimension().location().toString();
        String locationDesc = String.format("%s 维度 [%d, %d, %d]", dimensionName, pos.getX(), pos.getY(), pos.getZ());
        Component warning = Component.literal(
                "[警告] " + offender.getName().getString()
                        + " 试图组装/拆卸超大结构（大小约 " + size
                        + "，上限 " + maxSize + "）于 " + locationDesc
        );
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (op.hasPermissions(2)) {
                op.sendSystemMessage(warning);
            }
        }
    }

    private static long calculateStructureSize(PhysicsAssemblerBlockEntity assembler, ServerLevel level) {
        SubLevel subLevel = Sable.HELPER.getContaining(assembler);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            return countNonAirBlocks(serverSubLevel);
        } else {
            BlockPos toAssemble = assembler.getBlockPos().relative(
                    PhysicsAssemblerBlock.getStickyFacing(assembler.getBlockState())
            );
            // 组装扫描
            try {
                return scanAssemblySize(level, toAssemble);
            } catch (Exception e) {
                // 扫描失败：记录错误，并抛出异常让上层根据角色处理
                meow.LOGGER.error("组装扫描时发生异常", e);
                throw new RuntimeException("组装扫描失败", e);
            }
        }
    }

    private static long countNonAirBlocks(ServerSubLevel subLevel) {
        Level level = subLevel.getLevel();
        var plot = subLevel.getPlot();
        if (plot == null) {
            meow.LOGGER.warn("子世界 plot 为 null，无法统计方块数");
            return 0;
        }
        BoundingBox3ic box = plot.getBoundingBox();
        long count = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static long scanAssemblySize(Level level, BlockPos start) throws Exception {
        Class<?> contraptionClass = Class.forName(
                "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption"
        );
        Constructor<?> ctor = contraptionClass.getDeclaredConstructor(BlockPos.class, boolean.class);
        ctor.setAccessible(true);
        Object contraption = ctor.newInstance(null, false);

        Method search = contraptionClass.getDeclaredMethod("searchMovedStructure", Level.class, BlockPos.class);
        search.setAccessible(true);
        search.invoke(contraption, level, start);

        Method getBlocks = contraptionClass.getDeclaredMethod("getBlocks");
        getBlocks.setAccessible(true);
        Collection<?> blocks = (Collection<?>) getBlocks.invoke(contraption);
        return blocks.size();
    }
}