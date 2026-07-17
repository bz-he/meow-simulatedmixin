//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.network.PacketDistributor;
import cn.theherta.ModConfig.AutoBreakMode;

public class AutoBreakBedrock {
    private static final int AREA_SCAN_HORIZONTAL_RADIUS = 4;
    private static final int AREA_SCAN_VERTICAL_RADIUS = 4;
    private static final double RECYCLE_INTERACT_RANGE_SQR = (double)64.0F;
    private static final int RECYCLE_RETRY_INTERVAL_TICKS = 2;
    private static final int GHOST_PROBE_INTERVAL_TICKS = 2;
    private static final int MAX_GHOST_PROBE_ATTEMPTS = 8;
    private static final int RECYCLE_NON_PISTON_CONFIRM_TICKS = 20;
    private static final int PASSIVE_MONITOR_RADIUS = 8;
    private static final int PASSIVE_MONITOR_VERTICAL_RADIUS = 8;
    private static final int MIN_RECYCLE_DELAY_FOR_PISTON_A_TICKS = 4;
    private static final int MIN_PROCESSING_SPEED = 1;
    private static final int MAX_PROCESSING_SPEED = 100;
    private static final int MAX_BATCH_INTERVAL_TICKS = 10;
    private static final int MIN_BATCH_INTERVAL_TICKS = 1;
    private static final int TASK_STALL_RESET_TICKS = 5;
    private static boolean isLeftKeyPressed = false;
    private static final Map<BlockPos, BedrockCheckTask> bedrockTasks = new HashMap();
    private static final LinkedHashSet<BlockPos> pendingAreaTargets = new LinkedHashSet();
    private static int areaScanTickCounter = 0;
    private static int areaSelectionCursor = 0;
    private static boolean areaModeSelectionHintShown = false;
    private static final Set<BlockPos> pendingRecyclePistons = new HashSet();
    private static final Set<BlockPos> nearbyObservedNonPiston = new HashSet();
    private static final Set<BlockPos> nearbyObservedNonAirAroundSelection = new HashSet();
    private static final Map<BlockPos, Integer> pistonAPlacedTick = new HashMap();
    private static int globalTickCounter = 0;
    private static int bedrockTaskCursor = 0;
    private static final Map<BlockPos, RecycleTask> recycleTasks = new HashMap();

    private static void registerPlacedPiston(BlockPos pos, boolean isPistonA) {
        pendingRecyclePistons.add(pos);
        if (isPistonA) {
            pistonAPlacedTick.put(pos, globalTickCounter);
        } else {
            pistonAPlacedTick.remove(pos);
        }

    }

    private static void enqueueRecycle(BlockPos pos) {
        pendingRecyclePistons.add(pos);
        recycleTasks.computeIfAbsent(pos, RecycleTask::new);
    }

    private static int getPistonAAgeTicks(BlockPos pos) {
        Integer placedAt = (Integer)pistonAPlacedTick.get(pos);
        return placedAt == null ? 0 : Math.max(0, globalTickCounter - placedAt);
    }

    private static boolean isPistonAInDelayWindow(BlockPos pos) {
        return pistonAPlacedTick.containsKey(pos) && getPistonAAgeTicks(pos) < 4;
    }

    private static void recyclePendingPistons(Minecraft mc) {
        if (!pendingRecyclePistons.isEmpty() && mc.level != null && mc.player != null) {
            for(BlockPos pos : pendingRecyclePistons) {
                if (isWithinRecycleRange(mc.player, pos)) {
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.getBlock() == Blocks.PISTON) {
                        enqueueRecycle(pos);
                    } else {
                        refreshClientBlock(mc, pos);
                        enqueueRecycle(pos);
                    }
                }
            }

        }
    }

    private static void scanPassiveNewPistons(Minecraft mc) {
        if (mc.level != null && mc.player != null) {
            if (ModConfig.getInstance().getAutoBreakMode() != AutoBreakMode.AREA_WHITELIST) {
                nearbyObservedNonPiston.clear();
            } else {
                AreaSelectionManager selectionManager = AreaSelectionManager.INSTANCE;
                if (!selectionManager.hasActiveSelection()) {
                    nearbyObservedNonPiston.clear();
                } else {
                    BlockPos start = selectionManager.getSelectionStart();
                    BlockPos end = selectionManager.getSelectionEnd();
                    if (start != null && end != null) {
                        int border = 3;
                        int minX = Math.min(start.getX(), end.getX()) - 3;
                        int minY = Math.min(start.getY(), end.getY()) - 3;
                        int minZ = Math.min(start.getZ(), end.getZ()) - 3;
                        int maxX = Math.max(start.getX(), end.getX()) + 3;
                        int maxY = Math.max(start.getY(), end.getY()) + 3;
                        int maxZ = Math.max(start.getZ(), end.getZ()) + 3;
                        BlockPos playerPos = mc.player.blockPosition();
                        nearbyObservedNonPiston.removeIf((posx) -> posx.getX() < minX || posx.getX() > maxX || posx.getY() < minY || posx.getY() > maxY || posx.getZ() < minZ || posx.getZ() > maxZ);

                        for(int dy = -8; dy <= 8; ++dy) {
                            for(int dx = -8; dx <= 8; ++dx) {
                                for(int dz = -8; dz <= 8; ++dz) {
                                    BlockPos pos = playerPos.offset(dx, dy, dz);
                                    if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                                        BlockState state = mc.level.getBlockState(pos);
                                        if (state.getBlock() == Blocks.PISTON) {
                                            if (nearbyObservedNonPiston.remove(pos)) {
                                                enqueueRecycle(pos);
                                            }
                                        } else {
                                            nearbyObservedNonPiston.add(pos);
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        nearbyObservedNonPiston.clear();
                    }
                }
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(AutoBreakBedrock::tick);
        } else {
            ++globalTickCounter;
            if (ModConfig.getInstance().getAutoBreakMode() == AutoBreakMode.OFF) {
                clearAutomationRuntimeState();
            } else {
                scanPassiveNewPistons(mc);
                probeGhostAirTransitionsNearSelection(mc);
                recyclePendingPistons(mc);
                processAreaBedrockMode(mc);
                processBedrockTasks(mc);
                processRecycleTasks(mc);
            }
        }
    }

    private static void probeGhostAirTransitionsNearSelection(Minecraft mc) {
        if (ModConfig.getInstance().getAutoBreakMode() == AutoBreakMode.AREA_WHITELIST && mc.level != null && mc.player != null) {
            AreaSelectionManager selectionManager = AreaSelectionManager.INSTANCE;
            if (!selectionManager.hasActiveSelection()) {
                nearbyObservedNonAirAroundSelection.clear();
            } else {
                BlockPos start = selectionManager.getSelectionStart();
                BlockPos end = selectionManager.getSelectionEnd();
                if (start != null && end != null) {
                    int border = 1;
                    int minX = Math.min(start.getX(), end.getX()) - 1;
                    int minY = Math.min(start.getY(), end.getY()) - 1;
                    int minZ = Math.min(start.getZ(), end.getZ()) - 1;
                    int maxX = Math.max(start.getX(), end.getX()) + 1;
                    int maxY = Math.max(start.getY(), end.getY()) + 1;
                    int maxZ = Math.max(start.getZ(), end.getZ()) + 1;
                    BlockPos playerPos = mc.player.blockPosition();
                    nearbyObservedNonAirAroundSelection.removeIf((posx) -> {
                        if (Math.abs(posx.getX() - playerPos.getX()) <= 4 && Math.abs(posx.getY() - playerPos.getY()) <= 4 && Math.abs(posx.getZ() - playerPos.getZ()) <= 4) {
                            return posx.getX() < minX || posx.getX() > maxX || posx.getY() < minY || posx.getY() > maxY || posx.getZ() < minZ || posx.getZ() > maxZ;
                        } else {
                            return true;
                        }
                    });

                    for(int dy = -4; dy <= 4; ++dy) {
                        for(int dx = -4; dx <= 4; ++dx) {
                            for(int dz = -4; dz <= 4; ++dz) {
                                BlockPos pos = playerPos.offset(dx, dy, dz);
                                if (pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                                    BlockState state = mc.level.getBlockState(pos);
                                    if (!state.isAir()) {
                                        nearbyObservedNonAirAroundSelection.add(pos);
                                    } else if (nearbyObservedNonAirAroundSelection.remove(pos)) {
                                        rightClickWithWrenchOnce(mc, pos);
                                    }
                                }
                            }
                        }
                    }

                } else {
                    nearbyObservedNonAirAroundSelection.clear();
                }
            }
        } else {
            nearbyObservedNonAirAroundSelection.clear();
        }
    }

    private static void processRecycleTasks(Minecraft mc) {
        if (mc.level != null && mc.player != null) {
            Set<RecycleTask> finished = new HashSet();

            for(RecycleTask task : recycleTasks.values()) {
                if (advanceRecycleTask(mc, task)) {
                    finished.add(task);
                }
            }

            for(RecycleTask task : finished) {
                recycleTasks.remove(task.pos);
                pendingRecyclePistons.remove(task.pos);
                pistonAPlacedTick.remove(task.pos);
            }

        }
    }

    private static boolean advanceRecycleTask(Minecraft mc, RecycleTask task) {
        if (!isWithinRecycleRange(mc.player, task.pos)) {
            return false;
        } else if (task.cooldownTicks > 0) {
            --task.cooldownTicks;
            return false;
        } else {
            BlockState state = mc.level.getBlockState(task.pos);
            return state.getBlock() == Blocks.PISTON ? advanceRecycleTaskWhenPistonPresent(mc, task) : advanceRecycleTaskWhenNonPiston(mc, task);
        }
    }

    private static boolean advanceRecycleTaskWhenPistonPresent(Minecraft mc, RecycleTask task) {
        if (!isPistonAInDelayWindow(task.pos) && consumeRecycleOperationBudget()) {
            boolean attempted = breakBlockWithEquippedWrench(mc, task.pos);
            task.cooldownTicks = 2;
            if (attempted) {
                ++task.attempts;
            }

            BlockState afterState = mc.level.getBlockState(task.pos);
            if (afterState.getBlock() == Blocks.PISTON) {
                task.nonPistonTicks = 0;
                return false;
            } else {
                task.nonPistonTicks = 1;
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean advanceRecycleTaskWhenNonPiston(Minecraft mc, RecycleTask task) {
        ++task.nonPistonTicks;
        refreshClientBlock(mc, task.pos);
        if (task.nonPistonTicks % 2 == 0 && task.attempts < 8 && consumeRecycleOperationBudget()) {
            boolean attempted = breakBlockWithEquippedWrench(mc, task.pos);
            task.cooldownTicks = 2;
            if (attempted) {
                ++task.attempts;
            }

            BlockState afterProbe = mc.level.getBlockState(task.pos);
            if (afterProbe.getBlock() == Blocks.PISTON) {
                task.nonPistonTicks = 0;
                return false;
            }
        }

        return task.nonPistonTicks >= 20;
    }

    private static void processBedrockTasks(Minecraft mc) {
        if (mc.level != null && mc.player != null) {
            Set<BlockPos> finished = new HashSet();
            if (bedrockTasks.isEmpty()) {
                bedrockTaskCursor = 0;
            } else {
                List<BlockPos> taskOrder = new ArrayList();

                for(Map.Entry<BlockPos, BedrockCheckTask> entry : bedrockTasks.entrySet()) {
                    BlockPos pos = (BlockPos)entry.getKey();
                    BedrockCheckTask task = (BedrockCheckTask)entry.getValue();
                    if (!task.isAreaTask() || isWithinPlayerAreaWindow(mc.player, pos)) {
                        taskOrder.add(pos);
                    }
                }

                if (taskOrder.isEmpty()) {
                    bedrockTaskCursor = 0;
                } else {
                    int taskCount = taskOrder.size();
                    int startIndex = Math.floorMod(bedrockTaskCursor, taskCount);
                    int checksThisTick = 0;

                    for(int offset = 0; offset < taskCount; ++offset) {
                        int index = (startIndex + offset) % taskCount;
                        BlockPos pos = (BlockPos)taskOrder.get(index);
                        BedrockCheckTask task = (BedrockCheckTask)bedrockTasks.get(pos);
                        if (task != null) {
                            if (task.check()) {
                                finished.add(pos);
                            }

                            ++checksThisTick;
                        }
                    }

                    bedrockTaskCursor = (startIndex + checksThisTick) % taskCount;

                    for(BlockPos pos : finished) {
                        bedrockTasks.remove(pos);
                    }

                    if (bedrockTasks.isEmpty()) {
                        bedrockTaskCursor = 0;
                    }

                }
            }
        }
    }

    private static void clearAutomationRuntimeState() {
        bedrockTasks.clear();
        pendingAreaTargets.clear();
        recycleTasks.clear();
        pendingRecyclePistons.clear();
        nearbyObservedNonPiston.clear();
        nearbyObservedNonAirAroundSelection.clear();
        pistonAPlacedTick.clear();
        areaScanTickCounter = 0;
        areaSelectionCursor = 0;
        areaModeSelectionHintShown = false;
        isLeftKeyPressed = false;
        globalTickCounter = 0;
        bedrockTaskCursor = 0;
    }

    private static boolean isWithinRecycleRange(Player player, BlockPos pos) {
        return player.distanceToSqr((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= (double)64.0F;
    }

    private static boolean isWithinPlayerAreaWindow(Player player, BlockPos pos) {
        BlockPos playerPos = player.blockPosition();
        return Math.abs(pos.getX() - playerPos.getX()) <= 4 && Math.abs(pos.getY() - playerPos.getY()) <= 4 && Math.abs(pos.getZ() - playerPos.getZ()) <= 4;
    }

    private static int getProcessingSpeed() {
        int speed = ModConfig.getInstance().getProcessingSpeed();
        return Math.max(1, Math.min(100, speed));
    }

    private static int getDynamicAreaScanIntervalTicks() {
        return getBatchIntervalTicks();
    }

    private static int getDynamicMaxActiveBreakTasks() {
        int speed = getProcessingSpeed();
        return Math.max(2, speed * 2);
    }

    private static int getDynamicNewTasksPerAreaScan() {
        return getBatchPacketCount();
    }

    private static int getBatchPacketCount() {
        return getProcessingSpeed();
    }

    private static int getBatchIntervalTicks() {
        int speed = getProcessingSpeed();
        double t = (double)(speed - 1) / (double)99.0F;
        int mapped = (int)Math.round((double)10.0F - (double)9.0F * t);
        return Math.max(1, Math.min(10, mapped));
    }

    private static boolean consumeOperationBudget() {
        return true;
    }

    private static boolean consumeRecycleOperationBudget() {
        return true;
    }

    private static void showActionBarMessage(Player player, String message) {
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
        }

    }

    public static boolean handleLeftClick(Minecraft mc, boolean isPressed) {
        recyclePendingPistons(mc);
        ModConfig.AutoBreakMode autoBreakMode = ModConfig.getInstance().getAutoBreakMode();
        if (autoBreakMode == AutoBreakMode.OFF) {
            return false;
        } else if (autoBreakMode == AutoBreakMode.AREA_WHITELIST) {
            return false;
        } else if (!isPressed) {
            isLeftKeyPressed = false;
            return false;
        } else if (isLeftKeyPressed) {
            return false;
        } else {
            isLeftKeyPressed = true;
            if (!isPlayerReadyForInteraction(mc)) {
                return false;
            } else {
                ItemStack mainHandItem = mc.player.getMainHandItem();
                if (!AllItems.WRENCH.isIn(mainHandItem)) {
                    return false;
                } else if (!hasPistonInHotbar(mc.player)) {
                    showActionBarMessage(mc.player, "快捷栏中没有活塞！");
                    return false;
                } else if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
                    BlockHitResult hitResult = (BlockHitResult)mc.hitResult;
                    BlockPos targetPos = hitResult.getBlockPos();
                    BlockState targetState = mc.level.getBlockState(targetPos);
                    boolean shouldHandle = autoBreakMode == AutoBreakMode.CLICK_WHITELIST && isWhitelistedBreakTarget(targetState);
                    if (shouldHandle) {
                        if (bedrockTasks.containsKey(targetPos)) {
                            showActionBarMessage(mc.player, "该方块正在处理中，请稍候！");
                            return true;
                        } else {
                            return tryCreateBreakTask(mc, targetPos, targetState, true, false);
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private static void processAreaBedrockMode(Minecraft mc) {
        if (ModConfig.getInstance().getAutoBreakMode() != AutoBreakMode.AREA_WHITELIST) {
            clearAreaSelectionAndCaches(mc);
        } else if (mc.level != null && mc.player != null) {
            AreaSelectionManager selectionManager = AreaSelectionManager.INSTANCE;
            if (AllItems.WRENCH.isIn(mc.player.getMainHandItem())) {
                if (hasPistonInHotbar(mc.player)) {
                    if (!selectionManager.hasActiveSelection()) {
                        pendingAreaTargets.clear();
                        areaSelectionCursor = 0;
                        if (!areaModeSelectionHintShown) {
                            showActionBarMessage(mc.player, "请先用中键框选区域");
                            areaModeSelectionHintShown = true;
                        }

                    } else {
                        areaModeSelectionHintShown = false;
                        if (!selectionContainsBreakTargetsInSelection(mc, selectionManager)) {
                            clearAreaSelectionAndCaches(mc);
                            showActionBarMessage(mc.player, "框选范围内已无白名单方块");
                        } else {
                            collectAreaTargetsToPending(mc, selectionManager);
                            int maxActiveBreakTasks = getDynamicMaxActiveBreakTasks();
                            int nearbyActiveAreaTasks = countNearbyActiveAreaTasks(mc.player);
                            if (nearbyActiveAreaTasks < maxActiveBreakTasks) {
                                ++areaScanTickCounter;
                                if (areaScanTickCounter >= getDynamicAreaScanIntervalTicks()) {
                                    areaScanTickCounter = 0;
                                    int availableSlots = maxActiveBreakTasks - nearbyActiveAreaTasks;
                                    int createBudget = Math.min(getDynamicNewTasksPerAreaScan(), availableSlots);
                                    if (createBudget > 0) {
                                        drainPendingAreaTargetsToTasks(mc, selectionManager, createBudget);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void collectAreaTargetsToPending(Minecraft mc, AreaSelectionManager selectionManager) {
        if (mc.level != null && mc.player != null && selectionManager.hasActiveSelection()) {
            int sizeX = 9;
            int sizeY = 9;
            int sizeZ = 9;
            int volume = sizeX * sizeY * sizeZ;
            BlockPos playerPos = mc.player.blockPosition();
            pendingAreaTargets.removeIf((pos) -> !isWithinPlayerAreaWindow(mc.player, pos) || !selectionManager.isWithinSelection(pos) || !isWhitelistedBreakTarget(mc.level.getBlockState(pos)));
            areaSelectionCursor = Math.floorMod(areaSelectionCursor, volume);
            int visitedThisScan = 0;

            for(int i = 0; i < volume; ++i) {
                int index = (areaSelectionCursor + i) % volume;
                int xOffset = index % sizeX;
                int yz = index / sizeX;
                int yOffset = yz % sizeY;
                int zOffset = yz / sizeY;
                int dx = xOffset - 4;
                int dy = yOffset - 4;
                int dz = zOffset - 4;
                BlockPos targetPos = playerPos.offset(dx, dy, dz);
                if (selectionManager.isWithinSelection(targetPos) && !bedrockTasks.containsKey(targetPos) && !pendingAreaTargets.contains(targetPos)) {
                    BlockState targetState = mc.level.getBlockState(targetPos);
                    if (isWhitelistedBreakTarget(targetState)) {
                        pendingAreaTargets.add(targetPos);
                    }

                    ++visitedThisScan;
                } else {
                    ++visitedThisScan;
                }
            }

            if (visitedThisScan > 0) {
                areaSelectionCursor = (areaSelectionCursor + visitedThisScan) % volume;
            }

        } else {
            pendingAreaTargets.clear();
        }
    }

    private static void drainPendingAreaTargetsToTasks(Minecraft mc, AreaSelectionManager selectionManager, int createBudget) {
        if (createBudget > 0 && !pendingAreaTargets.isEmpty() && mc.level != null && mc.player != null) {
            int created = 0;
            Iterator<BlockPos> iterator = pendingAreaTargets.iterator();

            while(iterator.hasNext() && created < createBudget) {
                BlockPos targetPos = (BlockPos)iterator.next();
                if (bedrockTasks.containsKey(targetPos)) {
                    iterator.remove();
                } else if (isWithinPlayerAreaWindow(mc.player, targetPos) && selectionManager.isWithinSelection(targetPos)) {
                    BlockState targetState = mc.level.getBlockState(targetPos);
                    if (!isWhitelistedBreakTarget(targetState)) {
                        iterator.remove();
                    } else if (tryCreateBreakTask(mc, targetPos, targetState, false, true)) {
                        iterator.remove();
                        ++created;
                    }
                } else {
                    iterator.remove();
                }
            }

        }
    }

    private static int countNearbyActiveAreaTasks(Player player) {
        int count = 0;

        for(Map.Entry<BlockPos, BedrockCheckTask> entry : bedrockTasks.entrySet()) {
            if (((BedrockCheckTask)entry.getValue()).isAreaTask() && isWithinPlayerAreaWindow(player, (BlockPos)entry.getKey())) {
                ++count;
            }
        }

        return count;
    }

    private static boolean selectionContainsBreakTargetsInSelection(Minecraft mc, AreaSelectionManager selectionManager) {
        if (mc.level != null && selectionManager.hasActiveSelection()) {
            BlockPos start = selectionManager.getSelectionStart();
            BlockPos end = selectionManager.getSelectionEnd();
            if (start != null && end != null) {
                int minX = Math.min(start.getX(), end.getX());
                int minY = Math.min(start.getY(), end.getY());
                int minZ = Math.min(start.getZ(), end.getZ());
                int maxX = Math.max(start.getX(), end.getX());
                int maxY = Math.max(start.getY(), end.getY());
                int maxZ = Math.max(start.getZ(), end.getZ());

                for(int y = minY; y <= maxY; ++y) {
                    for(int x = minX; x <= maxX; ++x) {
                        for(int z = minZ; z <= maxZ; ++z) {
                            if (isWhitelistedBreakTarget(mc.level.getBlockState(new BlockPos(x, y, z)))) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void clearAreaSelectionAndCaches(Minecraft mc) {
        AreaSelectionManager selectionManager = AreaSelectionManager.INSTANCE;
        if (selectionManager.hasActiveSelection()) {
            selectionManager.clearSelection();
        }

        pendingAreaTargets.clear();
        nearbyObservedNonAirAroundSelection.clear();
        bedrockTasks.entrySet().removeIf((entry) -> ((BedrockCheckTask)entry.getValue()).isAreaTask());
        areaScanTickCounter = 0;
        bedrockTaskCursor = 0;
        areaSelectionCursor = 0;
        areaModeSelectionHintShown = false;
    }

    private static boolean isWhitelistedBreakTarget(BlockState targetState) {
        return targetState != null && !targetState.isAir() && ModConfig.getInstance().isWhitelistedAutoBreakBlock(targetState.getBlock());
    }

    private static boolean tryCreateBreakTask(Minecraft mc, BlockPos targetPos, BlockState targetState, boolean notifyWhenNoSpace, boolean areaTask) {
        List<AWithB> validAList = new ArrayList();

        for(Direction dir : Direction.values()) {
            BlockPos pistonAPos = targetPos.relative(dir);
            BlockState pistonAState = mc.level.getBlockState(pistonAPos);
            if (pistonAState.isAir() || pistonAState.canBeReplaced()) {
                List<BlockPos> bList = new ArrayList();

                for(Direction bDir : Direction.values()) {
                    BlockPos bPos = pistonAPos.relative(bDir);
                    if (!bPos.equals(targetPos)) {
                        BlockState bState = mc.level.getBlockState(bPos);
                        if (bState.isAir() || bState.canBeReplaced()) {
                            bList.add(bPos);
                        }
                    }
                }

                if (!bList.isEmpty()) {
                    validAList.add(new AWithB(pistonAPos, bList));
                }
            }
        }

        if (validAList.isEmpty()) {
            if (notifyWhenNoSpace) {
                showActionBarMessage(mc.player, "没有足够的空间放置活塞！");
            }

            return false;
        } else {
            bedrockTasks.put(targetPos, new BedrockCheckTask(targetPos, targetState, validAList, areaTask));
            return true;
        }
    }

    private static boolean hasPistonInHotbar(Player player) {
        return findPistonInHotbar(player) != -1;
    }

    private static boolean placePistonA(Minecraft mc, BlockPos pos, BlockPos bedrockPos) {
        registerPlacedPiston(pos, true);
        Direction facing = Direction.getNearest((float)(bedrockPos.getX() - pos.getX()), (float)(bedrockPos.getY() - pos.getY()), (float)(bedrockPos.getZ() - pos.getZ()));
        int pistonSlot = findPistonInHotbar(mc.player);
        if (pistonSlot == -1) {
            return false;
        } else {
            InteractionResult placeResult = (InteractionResult)withHotbarSlot(mc.player, pistonSlot, () -> useMainHandOnBlock(mc, pos, facing.getOpposite()));
            BlockState state = mc.level.getBlockState(pos);
            boolean accepted = state.getBlock() == Blocks.PISTON || placeResult.consumesAction();
            if (!accepted) {
                return false;
            } else {
                refreshClientBlock(mc, pos);
                BlockState pistonState = (BlockState)((BlockState)Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, facing)).setValue(BlockStateProperties.EXTENDED, true);
                RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(pos, pistonState);
                PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
                return true;
            }
        }
    }

    private static boolean placePistonB(Minecraft mc, BlockPos pos) {
        registerPlacedPiston(pos, false);
        int pistonSlot = findPistonInHotbar(mc.player);
        if (pistonSlot == -1) {
            return false;
        } else {
            InteractionResult placeResult = (InteractionResult)withHotbarSlot(mc.player, pistonSlot, () -> useMainHandOnBlock(mc, pos, Direction.UP));
            BlockState state = mc.level.getBlockState(pos);
            boolean accepted = state.getBlock() == Blocks.PISTON || placeResult.consumesAction();
            if (accepted) {
                refreshClientBlock(mc, pos);
            }

            return accepted;
        }
    }

    private static InteractionResult breakBlockWithWrench(Minecraft mc, BlockPos pos) {
        ServerboundPlayerCommandPacket shiftPacket = new ServerboundPlayerCommandPacket(mc.player, Action.PRESS_SHIFT_KEY);
        mc.player.connection.send(shiftPacket);
        BlockHitResult hitResult = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        refreshClientBlock(mc, pos);
        ServerboundPlayerCommandPacket releaseShiftPacket = new ServerboundPlayerCommandPacket(mc.player, Action.RELEASE_SHIFT_KEY);
        mc.player.connection.send(releaseShiftPacket);
        return result;
    }

    private static boolean breakBlockWithEquippedWrench(Minecraft mc, BlockPos pos) {
        int wrenchSlot = findWrenchInHotbar(mc.player);
        return wrenchSlot == -1 ? false : (Boolean)withHotbarSlot(mc.player, wrenchSlot, () -> {
            InteractionResult result = breakBlockWithWrench(mc, pos);
            return result.consumesAction();
        });
    }

    private static boolean rightClickWithWrenchOnce(Minecraft mc, BlockPos pos) {
        int wrenchSlot = findWrenchInHotbar(mc.player);
        return wrenchSlot == -1 ? false : (Boolean)withHotbarSlot(mc.player, wrenchSlot, () -> useMainHandOnBlock(mc, pos, Direction.UP).consumesAction());
    }

    private static boolean isPlayerReadyForInteraction(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.screen == null;
    }

    private static InteractionResult useMainHandOnBlock(Minecraft mc, BlockPos pos, Direction face) {
        BlockHitResult hitResult = new BlockHitResult(pos.getCenter(), face, pos, false);
        return mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
    }

    private static <T> T withHotbarSlot(Player player, int slot, Supplier<T> action) {
        int previousSlot = selectHotbarSlot(player, slot);

        Object var4;
        try {
            var4 = action.get();
        } finally {
            restoreHotbarSlot(player, previousSlot);
        }

        return (T)var4;
    }

    private static void refreshClientBlock(Minecraft mc, BlockPos pos) {
        if (mc.level != null) {
            BlockState state = mc.level.getBlockState(pos);
            mc.level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static int selectHotbarSlot(Player player, int slot) {
        int previousSlot = player.getInventory().selected;
        player.getInventory().selected = slot;
        return previousSlot;
    }

    private static void restoreHotbarSlot(Player player, int previousSlot) {
        player.getInventory().selected = previousSlot;
    }

    private static int findWrenchInHotbar(Player player) {
        for(int i = 0; i < 9; ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (AllItems.WRENCH.isIn(stack)) {
                return i;
            }
        }

        return -1;
    }

    private static int findPistonInHotbar(Player player) {
        for(int i = 0; i < 9; ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.PISTON) {
                return i;
            }
        }

        return -1;
    }

    private static class RecycleTask {
        public final BlockPos pos;
        public int attempts = 0;
        public int cooldownTicks = 0;
        public int nonPistonTicks = 0;

        public RecycleTask(BlockPos pos) {
            this.pos = pos;
        }
    }

    private static class BedrockCheckTask {
        private final BlockPos targetPos;
        private final BlockState initialTargetState;
        private final List<AWithB> validAList;
        private final boolean areaTask;
        private final Set<BlockPos> placedPistons = new HashSet();
        private final Set<BlockPos> attemptedPistonPositions = new HashSet();
        private int tickCount = 0;
        private boolean pistonsPlaced = false;
        private boolean checkingBedrock = false;
        private boolean breakingPistons = false;
        private int tryAIndex = 0;
        private boolean waitingRecycleBeforeNextA = false;
        private BlockPos pendingFailedAPos = null;
        private int tryBIndex = 0;
        private BlockPos placedAPos = null;
        private BlockPos placedBPos = null;
        private int lastProgressTick;

        public BedrockCheckTask(BlockPos targetPos, BlockState initialTargetState, List<AWithB> validAList, boolean areaTask) {
            this.targetPos = targetPos;
            this.initialTargetState = initialTargetState;
            this.validAList = validAList;
            this.areaTask = areaTask;
            this.lastProgressTick = AutoBreakBedrock.globalTickCounter;
        }

        public boolean isAreaTask() {
            return this.areaTask;
        }

        public boolean check() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                if (!this.isCurrentTargetBlock(mc)) {
                    this.enqueueAllRecycleCandidates();
                    return true;
                } else if (AutoBreakBedrock.globalTickCounter - this.lastProgressTick >= 5) {
                    int nextAIndex = this.getNextAIndexAfterStall();
                    this.enqueueAllRecycleCandidates();
                    this.resetForRetry(nextAIndex);
                    this.touchProgress();
                    return false;
                } else if (!this.pistonsPlaced) {
                    return this.advancePlacementPhase(mc);
                } else if (this.checkingBedrock) {
                    return this.advanceBedrockCheckPhase(mc);
                } else {
                    return this.breakingPistons ? this.advanceOrderedRecyclePhase(mc) : false;
                }
            } else {
                this.enqueueAllRecycleCandidates();
                return true;
            }
        }

        private boolean isCurrentTargetBlock(Minecraft mc) {
            BlockState currentState = mc.level.getBlockState(this.targetPos);
            return currentState.getBlock() == this.initialTargetState.getBlock() && AutoBreakBedrock.isWhitelistedBreakTarget(currentState);
        }

        private boolean advancePlacementPhase(Minecraft mc) {
            if (this.waitingRecycleBeforeNextA && this.pendingFailedAPos != null) {
                return this.advanceWaitingFailedARecycle(mc);
            } else if (this.tryAIndex >= this.validAList.size()) {
                AutoBreakBedrock.showActionBarMessage(mc.player, "活塞放置失败");
                this.breakingPistons = true;
                this.pistonsPlaced = true;
                return false;
            } else {
                AWithB ab = (AWithB)this.validAList.get(this.tryAIndex);
                this.attemptedPistonPositions.add(ab.aPos);
                AutoBreakBedrock.registerPlacedPiston(ab.aPos, true);
                if (!AutoBreakBedrock.consumeOperationBudget()) {
                    return false;
                } else if (!AutoBreakBedrock.placePistonA(mc, ab.aPos, this.targetPos)) {
                    ++this.tryAIndex;
                    this.tryBIndex = 0;
                    this.touchProgress();
                    return false;
                } else {
                    this.placedPistons.add(ab.aPos);
                    this.placedAPos = ab.aPos;
                    this.touchProgress();
                    if (this.tryBIndex >= ab.bList.size()) {
                        AutoBreakBedrock.enqueueRecycle(ab.aPos);
                        this.waitingRecycleBeforeNextA = true;
                        this.pendingFailedAPos = ab.aPos;
                        this.tryBIndex = 0;
                        return false;
                    } else {
                        BlockPos bPos = (BlockPos)ab.bList.get(this.tryBIndex);
                        if (!this.isCurrentTargetBlock(mc)) {
                            this.enqueueAllRecycleCandidates();
                            return true;
                        } else {
                            this.attemptedPistonPositions.add(bPos);
                            AutoBreakBedrock.registerPlacedPiston(bPos, false);
                            if (!AutoBreakBedrock.consumeOperationBudget()) {
                                return false;
                            } else if (AutoBreakBedrock.placePistonB(mc, bPos)) {
                                this.placedPistons.add(bPos);
                                this.placedBPos = bPos;
                                this.pistonsPlaced = true;
                                this.checkingBedrock = true;
                                this.tickCount = 0;
                                this.tryBIndex = 0;
                                this.touchProgress();
                                return false;
                            } else {
                                ++this.tryBIndex;
                                this.touchProgress();
                                return false;
                            }
                        }
                    }
                }
            }
        }

        private boolean advanceOrderedRecyclePhase(Minecraft mc) {
            if (this.placedBPos != null) {
                AutoBreakBedrock.enqueueRecycle(this.placedBPos);
                if (this.isRecycleTargetPending(mc, this.placedBPos)) {
                    return false;
                }

                this.placedBPos = null;
                this.touchProgress();
            }

            if (this.placedAPos != null) {
                AutoBreakBedrock.enqueueRecycle(this.placedAPos);
                if (this.isRecycleTargetPending(mc, this.placedAPos)) {
                    return false;
                }

                this.placedAPos = null;
                this.touchProgress();
            }

            this.enqueueRemainingRecycleCandidates();
            this.touchProgress();
            return true;
        }

        private boolean isRecycleTargetPending(Minecraft mc, BlockPos pos) {
            if (AutoBreakBedrock.recycleTasks.containsKey(pos)) {
                return true;
            } else {
                return mc.level.getBlockState(pos).getBlock() == Blocks.PISTON;
            }
        }

        private void enqueueRemainingRecycleCandidates() {
            for(BlockPos pos : this.attemptedPistonPositions) {
                AutoBreakBedrock.enqueueRecycle(pos);
            }

            for(BlockPos pos : this.placedPistons) {
                AutoBreakBedrock.enqueueRecycle(pos);
            }

            if (this.pendingFailedAPos != null) {
                AutoBreakBedrock.enqueueRecycle(this.pendingFailedAPos);
            }

        }

        private boolean advanceWaitingFailedARecycle(Minecraft mc) {
            BlockState failedAState = mc.level.getBlockState(this.pendingFailedAPos);
            if (failedAState.getBlock() == Blocks.PISTON) {
                AutoBreakBedrock.enqueueRecycle(this.pendingFailedAPos);
                return false;
            } else {
                this.placedPistons.remove(this.pendingFailedAPos);
                if (this.pendingFailedAPos.equals(this.placedAPos)) {
                    this.placedAPos = null;
                }

                this.waitingRecycleBeforeNextA = false;
                this.pendingFailedAPos = null;
                ++this.tryAIndex;
                this.tryBIndex = 0;
                this.touchProgress();
                return false;
            }
        }

        private boolean advanceBedrockCheckPhase(Minecraft mc) {
            ++this.tickCount;
            BlockState currentState = mc.level.getBlockState(this.targetPos);
            boolean targetChanged = currentState.getBlock() != this.initialTargetState.getBlock();
            if (targetChanged || this.tickCount >= 10) {
                this.checkingBedrock = false;
                this.breakingPistons = true;
                this.touchProgress();
            }

            return false;
        }

        private int getNextAIndexAfterStall() {
            if (this.validAList.isEmpty()) {
                return 0;
            } else {
                int baseIndex = Math.min(this.tryAIndex, this.validAList.size() - 1);
                if (this.pendingFailedAPos != null) {
                    int failedIndex = this.findAIndexByPos(this.pendingFailedAPos);
                    if (failedIndex >= 0) {
                        baseIndex = failedIndex;
                    }
                }

                return Math.floorMod(baseIndex + 1, this.validAList.size());
            }
        }

        private int findAIndexByPos(BlockPos aPos) {
            for(int i = 0; i < this.validAList.size(); ++i) {
                if (((AWithB)this.validAList.get(i)).aPos.equals(aPos)) {
                    return i;
                }
            }

            return -1;
        }

        private void resetForRetry(int nextAIndex) {
            this.tickCount = 0;
            this.pistonsPlaced = false;
            this.checkingBedrock = false;
            this.breakingPistons = false;
            this.tryAIndex = this.validAList.isEmpty() ? 0 : Math.floorMod(nextAIndex, this.validAList.size());
            this.waitingRecycleBeforeNextA = false;
            this.pendingFailedAPos = null;
            this.tryBIndex = 0;
            this.placedAPos = null;
            this.placedBPos = null;
            this.placedPistons.clear();
            this.attemptedPistonPositions.clear();
        }

        private void touchProgress() {
            this.lastProgressTick = AutoBreakBedrock.globalTickCounter;
        }

        private void enqueueAllRecycleCandidates() {
            if (this.placedBPos != null) {
                AutoBreakBedrock.enqueueRecycle(this.placedBPos);
            }

            if (this.placedAPos != null) {
                AutoBreakBedrock.enqueueRecycle(this.placedAPos);
            }

            this.enqueueRemainingRecycleCandidates();
        }
    }

    private static class AWithB {
        public final BlockPos aPos;
        public final List<BlockPos> bList;

        public AWithB(BlockPos aPos, List<BlockPos> bList) {
            this.aPos = aPos;
            this.bList = bList;
        }
    }
}