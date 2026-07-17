package cn.theherta;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.PacketDistributor;
import cn.theherta.ModConfig.BatchMode;

public enum PacketBatcherNew {
    INSTANCE;

    private final Queue<BlockPos> taskQueue = new LinkedList<>();
    private Map<BlockPos, FluidType> taskFluidTypes = new HashMap<>();
    private BlockState originalTargetState;
    private int successCount = 0;
    private int skippedCount = 0;
    private Minecraft mc;
    private ScheduledExecutorService executor;
    private boolean isSending = false;

    private enum FluidType { WATER, LAVA, WATERLOGGED, TEST }

    private int getBatchSize() {
        return ModConfig.getInstance().getPacketsPerBatch();
    }

    private long getDelayPerBatch() {
        return (long) ModConfig.getInstance().getPacketSendDelay();
    }

    public void sendBatchPackets(BlockPos start, BlockPos end, Object originalData) {
        this.mc = Minecraft.getInstance();
        if (this.mc.player != null && this.mc.level != null) {
            ModConfig.BatchMode batchMode = ModConfig.getInstance().getBatchMode();
            if (batchMode == BatchMode.DRAIN_WATER) {
                this.drainFluidArea(start, end);
            } else if (batchMode == BatchMode.FILL) {
                this.fillArea(start, end);
            } else if (batchMode == BatchMode.TEST) {
                BlockState customState = ModConfig.getInstance().getCustomBlockState();
                if (customState == null) {
                    this.mc.player.sendSystemMessage(Component.literal("没有保存的自定义状态！请先编辑。"));
                    return;
                }
                this.originalTargetState = customState;
                this.successCount = 0;
                this.skippedCount = 0;
                this.taskQueue.clear();
                BlockPos.betweenClosedStream(
                        Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()), Math.min(start.getZ(), end.getZ()),
                        Math.max(start.getX(), end.getX()), Math.max(start.getY(), end.getY()), Math.max(start.getZ(), end.getZ())
                ).forEach((pos) -> {
                    BlockState currentState = this.mc.level.getBlockState(pos);
                    if (currentState.is(this.originalTargetState.getBlock())) {
                        this.taskQueue.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
                    }
                });
                this.mc.player.sendSystemMessage(Component.literal("已加入队列（" + this.taskQueue.size() + "个）"));
                this.startBatchSending(false, false, false);
            } else if (start != null && end != null && originalData != null && originalData instanceof BlockState) {
                BlockState state = (BlockState) originalData;
                if (this.isSending && this.executor != null) {
                    this.executor.shutdownNow();
                    try {
                        if (!this.executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                            this.executor.shutdownNow();
                        }
                    } catch (InterruptedException var7) {
                        Thread.currentThread().interrupt();
                    }
                }

                this.originalTargetState = state;
                this.successCount = 0;
                this.skippedCount = 0;
                this.taskQueue.clear();
                BlockPos.betweenClosedStream(Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()), Math.min(start.getZ(), end.getZ()), Math.max(start.getX(), end.getX()), Math.max(start.getY(), end.getY()), Math.max(start.getZ(), end.getZ())).forEach((pos) -> {
                    BlockState currentState = this.mc.level.getBlockState(pos);
                    if (currentState.is(this.originalTargetState.getBlock())) {
                        this.taskQueue.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
                    }
                });
                this.mc.player.sendSystemMessage(Component.literal("已加入队列（" + this.taskQueue.size() + "个）"));
                this.startBatchSending(false, false, false);
            }
        }
    }

    private void drainFluidArea(BlockPos start, BlockPos end) {
        if (this.isSending && this.executor != null) {
            this.executor.shutdownNow();
            try {
                if (!this.executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (InterruptedException var14) {
                Thread.currentThread().interrupt();
            }
        }

        this.successCount = 0;
        this.skippedCount = 0;
        this.taskQueue.clear();
        this.taskFluidTypes.clear();

        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int y = maxY; y >= minY; --y) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState currentState = this.mc.level.getBlockState(pos);
                    FluidType type = null;

                    if (currentState.is(Blocks.WATER)) {
                        type = FluidType.WATER;
                    } else if (currentState.is(Blocks.LAVA)) {
                        type = FluidType.LAVA;
                    } else if (currentState.hasProperty(BlockStateProperties.WATERLOGGED)
                            && currentState.getValue(BlockStateProperties.WATERLOGGED)) {
                        type = FluidType.WATERLOGGED;
                    }

                    if (type != null) {
                        this.taskQueue.add(pos);
                        this.taskFluidTypes.put(pos, type);
                    }
                }
            }
        }

        this.mc.player.sendSystemMessage(Component.literal("开始排水/排流体（" + this.taskQueue.size() + "个位置）"));
        this.startWaterDraining();
    }

    private void fillArea(BlockPos start, BlockPos end) {
        if (this.isSending && this.executor != null) {
            this.executor.shutdownNow();
            try {
                if (!this.executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (InterruptedException var14) {
                Thread.currentThread().interrupt();
            }
        }

        this.successCount = 0;
        this.skippedCount = 0;
        this.taskQueue.clear();
        this.taskFluidTypes.clear();

        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int y = maxY; y >= minY; --y) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState currentState = this.mc.level.getBlockState(pos);
                    FluidType type = null;

                    if (currentState.is(Blocks.WATER) && currentState.getValue(BlockStateProperties.LEVEL) > 0) {
                        type = FluidType.WATER;
                    } else if (currentState.is(Blocks.LAVA) && currentState.getValue(BlockStateProperties.LEVEL) > 0) {
                        type = FluidType.LAVA;
                    }

                    if (type != null) {
                        this.taskQueue.add(pos);
                        this.taskFluidTypes.put(pos, type);
                    }
                }
            }
        }

        this.mc.player.sendSystemMessage(Component.literal("开始填充（" + this.taskQueue.size() + "个流动流体）"));
        this.startFillSending();
    }

    private void startWaterDraining() {
        this.startBatchSending(true, false, false);
    }

    private void startFillSending() {
        this.startBatchSending(false, true, false);
    }

    private void startBatchSending(boolean isDrainWater, boolean isFill, boolean isTest) {
        this.isSending = true;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        int[] totalBatches = new int[]{0};
        int[] completedBatches = new int[]{0};
        int queueSize = this.taskQueue.size();
        int batchSize = this.getBatchSize();
        totalBatches[0] = (queueSize + batchSize - 1) / batchSize;
        int batchIndex = 0;

        while (!this.taskQueue.isEmpty()) {
            List<BlockPos> batch = new ArrayList<>(batchSize);

            for (int i = 0; i < batchSize && !this.taskQueue.isEmpty(); ++i) {
                BlockPos pos = this.taskQueue.poll();
                if (pos != null) {
                    batch.add(pos);
                }
            }

            if (!batch.isEmpty()) {
                this.executor.schedule(() -> {
                    if (this.mc.player != null && this.mc.level != null) {
                        this.mc.execute(() -> {
                            for (BlockPos pos : batch) {
                                if (isTest) {
                                    this.processTest(pos);
                                } else if (isFill) {
                                    this.processFill(pos);
                                } else if (isDrainWater) {
                                    this.processFluidDrain(pos);
                                } else {
                                    this.processSinglePacket(pos);
                                }
                            }

                            int var10002 = completedBatches[0]++;
                            if (completedBatches[0] >= totalBatches[0]) {
                                this.finishBatchSending();
                            }
                        });
                    } else {
                        int var10002 = completedBatches[0]++;
                    }
                }, (long) batchIndex * this.getDelayPerBatch(), TimeUnit.MILLISECONDS);
                ++batchIndex;
            }
        }
    }

    private void finishBatchSending() {
        this.isSending = false;
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }

        if (this.mc.player != null) {
            this.mc.player.sendSystemMessage(Component.literal("处理：" + this.successCount + " | 跳过：" + this.skippedCount));
        }
    }

    private void processSinglePacket(BlockPos pos) {
        try {
            BlockState currentState = this.mc.level.getBlockState(pos);
            if (!currentState.is(this.originalTargetState.getBlock())) {
                ++this.skippedCount;
                return;
            }

            RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(pos, this.originalTargetState);
            PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
            ++this.successCount;
        } catch (Exception var4) {
        }
    }

    private void processFluidDrain(BlockPos pos) {
        try {
            BlockState currentState = this.mc.level.getBlockState(pos);
            FluidType type = taskFluidTypes.get(pos);
            if (type == null) {
                ++this.skippedCount;
                return;
            }

            switch (type) {
                case WATER:
                    BlockState waterState = Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 7);
                    RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(pos, waterState);
                    PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
                    ++this.successCount;
                    System.out.println("Drain packet state: " + waterState);
                    break;

                case LAVA:
                    BlockState lavaState = Blocks.LAVA.defaultBlockState().setValue(BlockStateProperties.LEVEL, 3);
                    RadialWrenchMenuSubmitPacket lavaPacket = new RadialWrenchMenuSubmitPacket(pos, lavaState);
                    PacketDistributor.sendToServer(lavaPacket, new CustomPacketPayload[0]);
                    ++this.successCount;
                    System.out.println("Drain packet state (lava): " + lavaState);
                    break;

                case WATERLOGGED:
                    if (currentState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        BlockState dryState = currentState.setValue(BlockStateProperties.WATERLOGGED, false);
                        RadialWrenchMenuSubmitPacket dryPacket = new RadialWrenchMenuSubmitPacket(pos, dryState);
                        PacketDistributor.sendToServer(dryPacket, new CustomPacketPayload[0]);
                        ++this.successCount;
                        System.out.println("Drain packet state (waterlogged -> dry): " + dryState);
                    } else {
                        ++this.skippedCount;
                    }
                    break;
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    private void processFill(BlockPos pos) {
        try {
            BlockState currentState = this.mc.level.getBlockState(pos);
            FluidType type = taskFluidTypes.get(pos);
            if (type == null) {
                ++this.skippedCount;
                return;
            }

            BlockState targetState;
            if (type == FluidType.WATER) {
                targetState = Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 0);
            } else { // LAVA
                targetState = Blocks.LAVA.defaultBlockState().setValue(BlockStateProperties.LEVEL, 0);
            }

            RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(pos, targetState);
            PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
            ++this.successCount;
            System.out.println("Fill packet state: " + targetState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processTest(BlockPos pos) {
        try {
            BlockState currentState = this.mc.level.getBlockState(pos);
            if (!currentState.is(Blocks.COCOA)) {
                ++this.skippedCount;
                return;
            }

            // 设置 age = 2 (成熟)
            BlockState targetState = currentState.setValue(BlockStateProperties.AGE_2, 2);
            RadialWrenchMenuSubmitPacket packet = new RadialWrenchMenuSubmitPacket(pos, targetState);
            PacketDistributor.sendToServer(packet, new CustomPacketPayload[0]);
            ++this.successCount;
            System.out.println("Test packet state: " + targetState);
        } catch (Exception e) {
            e.printStackTrace();
            ++this.skippedCount;
        }
    }

    public void captureOriginalPacket(BlockPos originalPos, BlockState state) {
        AreaSelectionManager.INSTANCE.setOriginalPacketData(state);
    }
}