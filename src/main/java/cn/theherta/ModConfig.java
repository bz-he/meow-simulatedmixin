//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package cn.theherta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.loading.FMLPaths;

public class ModConfig {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private static final Path CONFIG_PATH;
    private static final List<String> DEFAULT_WHITELIST_IDS;
    private static final int MIN_PROCESSING_SPEED = 1;
    private static final int MAX_PROCESSING_SPEED = 100;
    private static final ModConfig INSTANCE;
    private BatchMode batchMode;
    private int processingSpeed;
    private BlockState lastSavedPacketState;
    private String lastSavedPacketInfo;
    private AutoBreakMode autoBreakMode;
    private final Set<Block> autoBreakWhitelist;
    private boolean suppressSave;
    private String customStateBlockId = "";
    private Map<String, String> customStateProperties = new HashMap<>();
    private boolean useCustomState = false;

    private ModConfig() {
        this.batchMode = BatchMode.OFF;
        this.processingSpeed = 50;
        this.lastSavedPacketState = null;
        this.lastSavedPacketInfo = "";
        this.autoBreakMode = AutoBreakMode.OFF;
        this.autoBreakWhitelist = new LinkedHashSet();
        this.suppressSave = false;
        this.applyDefaultWhitelist();
        this.loadFromDisk();
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public BatchMode getBatchMode() {
        return this.batchMode;
    }

    public void setBatchMode(BatchMode mode) {
        this.batchMode = mode == null ? BatchMode.OFF : mode;
        this.saveIfAllowed();
    }

    public boolean isBatchModeEnabled() {
        return this.batchMode != BatchMode.OFF;
    }

    public int getPacketSendDelay() {
        double ratio = (double)(this.processingSpeed - 1) / (double)99.0F;
        return (int)Math.round((double)500.0F - ratio * (double)495.0F);
    }

    public void setPacketSendDelay(int delay) {
        int clampedDelay = Math.max(5, Math.min(500, delay));
        double ratio = ((double)500.0F - (double)clampedDelay) / (double)495.0F;
        this.processingSpeed = clampSpeed((int)Math.round((double)1.0F + ratio * (double)99.0F));
        this.saveIfAllowed();
    }

    public int getPacketsPerBatch() {
        double ratio = (double)(this.processingSpeed - 1) / (double)99.0F;
        return (int)Math.round((double)1.0F + ratio * (double)999.0F);
    }

    public void setPacketsPerBatch(int count) {
        int clampedCount = Math.max(1, Math.min(1000, count));
        double ratio = (double)(clampedCount - 1) / (double)999.0F;
        this.processingSpeed = clampSpeed((int)Math.round((double)1.0F + ratio * (double)99.0F));
        this.saveIfAllowed();
    }

    public int getProcessingSpeed() {
        return this.processingSpeed;
    }

    public void setProcessingSpeed(int speed) {
        this.processingSpeed = clampSpeed(speed);
        this.saveIfAllowed();
    }

    public BlockState getLastSavedPacketState() {
        return this.lastSavedPacketState;
    }

    public void setLastSavedPacketState(BlockState state) {
        this.lastSavedPacketState = state;
    }

    public String getLastSavedPacketInfo() {
        return this.lastSavedPacketInfo;
    }

    public void setLastSavedPacketInfo(String info) {
        this.lastSavedPacketInfo = info;
    }

    public AutoBreakMode getAutoBreakMode() {
        return this.autoBreakMode;
    }

    public void setAutoBreakMode(AutoBreakMode mode) {
        this.autoBreakMode = mode == null ? AutoBreakMode.OFF : mode;
        this.saveIfAllowed();
    }

    public Set<Block> getAutoBreakWhitelist() {
        return Collections.unmodifiableSet(this.autoBreakWhitelist);
    }

    public void setAutoBreakWhitelist(Set<Block> whitelist) {
        this.autoBreakWhitelist.clear();
        if (whitelist != null && !whitelist.isEmpty()) {
            this.autoBreakWhitelist.addAll(whitelist);
            this.saveIfAllowed();
        } else {
            this.applyDefaultWhitelist();
            this.saveIfAllowed();
        }
    }

    public List<String> getAutoBreakWhitelistIds() {
        List<String> ids = new ArrayList();

        for(Block block : this.autoBreakWhitelist) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key != null) {
                ids.add(key.toString());
            }
        }

        return ids;
    }

    public boolean addWhitelistBlockById(String blockId) {
        Optional<Block> block = this.resolveBlock(blockId);
        return block.isEmpty() ? false : this.addWhitelistBlock((Block)block.get());
    }

    public boolean removeWhitelistBlockById(String blockId) {
        Optional<Block> block = this.resolveBlock(blockId);
        return block.isEmpty() ? false : this.removeWhitelistBlock((Block)block.get());
    }

    public boolean addWhitelistBlock(Block block) {
        if (block != null && block != Blocks.AIR) {
            boolean changed = this.autoBreakWhitelist.add(block);
            if (changed) {
                this.saveIfAllowed();
            }

            return changed;
        } else {
            return false;
        }
    }

    public boolean removeWhitelistBlock(Block block) {
        if (block == null) {
            return false;
        } else {
            boolean changed = this.autoBreakWhitelist.remove(block);
            if (this.autoBreakWhitelist.isEmpty()) {
                this.applyDefaultWhitelist();
                changed = true;
            }

            if (changed) {
                this.saveIfAllowed();
            }

            return changed;
        }
    }

    public boolean isWhitelistedAutoBreakBlock(Block block) {
        return block != null && this.autoBreakWhitelist.contains(block);
    }

    public String getAutoBreakWhitelistSummary() {
        return this.autoBreakWhitelist.isEmpty() ? "(空)" : (String)this.autoBreakWhitelist.stream().map((block) -> {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            return key == null ? "unknown" : key.toString();
        }).collect(Collectors.joining(", "));
    }

    public synchronized void loadFromDisk() {
        if (!Files.exists(CONFIG_PATH, new LinkOption[0])) {
            this.saveToDisk();
        } else {
            try {
                Reader reader;
                label207: {
                    reader = Files.newBufferedReader(CONFIG_PATH);

                    try {
                        PersistedConfig data = (PersistedConfig)GSON.fromJson(reader, PersistedConfig.class);
                        if (data == null) {
                            break label207;
                        }

                        this.suppressSave = true;
                        if (data.batchMode != null) {
                            this.batchMode = (BatchMode)parseEnum(data.batchMode, BatchMode.class, BatchMode.OFF);
                        }

                        if (data.processingSpeed != null) {
                            this.processingSpeed = clampSpeed(data.processingSpeed);
                        } else {
                            int delay = data.packetSendDelay == null ? 50 : Math.max(5, Math.min(500, data.packetSendDelay));
                            int batch = data.packetsPerBatch == null ? 10 : Math.max(1, Math.min(1000, data.packetsPerBatch));
                            int speedByDelay = speedFromDelay(delay);
                            int speedByBatch = speedFromBatchSize(batch);
                            this.processingSpeed = clampSpeed((speedByDelay + speedByBatch) / 2);
                        }

                        if (data.autoBreakMode != null) {
                            this.autoBreakMode = parseAutoBreakMode(data.autoBreakMode);
                        }

                        this.autoBreakWhitelist.clear();
                        if (data.autoBreakWhitelist != null) {
                            for(String id : data.autoBreakWhitelist) {
                                Optional var10000 = this.resolveBlock(id);
                                Set var10001 = this.autoBreakWhitelist;
                                Objects.requireNonNull(var10001);
                                var10000.ifPresent(var10001::add);
                            }
                        }

                        this.customStateBlockId = data.customStateBlockId != null ? data.customStateBlockId : "";
                        this.customStateProperties = data.customStateProperties != null ? new HashMap<>(data.customStateProperties) : new HashMap<>();
                        this.useCustomState = data.useCustomState != null ? data.useCustomState : false;

                        if (this.autoBreakWhitelist.isEmpty()) {
                            this.applyDefaultWhitelist();
                        }
                    } catch (Throwable var13) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Throwable var12) {
                                var13.addSuppressed(var12);
                            }
                        }

                        throw var13;
                    }

                    if (reader != null) {
                        reader.close();
                    }

                    return;
                }

                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                System.err.println("[submitChange_batchpacket] 加载配置失败: " + e.getMessage());
                return;
            } finally {
                this.suppressSave = false;
            }

        }
    }

    public synchronized void saveToDisk() {
        PersistedConfig data = new PersistedConfig();
        data.batchMode = this.batchMode.name();
        data.processingSpeed = this.processingSpeed;
        data.packetSendDelay = this.getPacketSendDelay();
        data.packetsPerBatch = this.getPacketsPerBatch();
        data.autoBreakMode = this.autoBreakMode.name();
        data.autoBreakWhitelist = this.getAutoBreakWhitelistIds();
        data.customStateBlockId = this.customStateBlockId;
        data.customStateProperties = new HashMap<>(this.customStateProperties);
        data.useCustomState = this.useCustomState;

        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("[submitChange_batchpacket] 保存配置失败: " + e.getMessage());
        }

    }

    private void saveIfAllowed() {
        if (!this.suppressSave) {
            this.saveToDisk();
        }

    }

    private void applyDefaultWhitelist() {
        this.autoBreakWhitelist.clear();

        for(String id : DEFAULT_WHITELIST_IDS) {
            Optional var10000 = this.resolveBlock(id);
            Set var10001 = this.autoBreakWhitelist;
            Objects.requireNonNull(var10001);
            var10000.ifPresent(var10001::add);
        }

        if (this.autoBreakWhitelist.isEmpty()) {
            this.autoBreakWhitelist.add(Blocks.BEDROCK);
            this.autoBreakWhitelist.add(Blocks.NETHERRACK);
            this.autoBreakWhitelist.add(Blocks.DEEPSLATE);
        }

    }

    private Optional<Block> resolveBlock(String blockId) {
        if (blockId == null) {
            return Optional.empty();
        } else {
            String normalized = blockId.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return Optional.empty();
            } else {
                ResourceLocation key = ResourceLocation.tryParse(normalized);
                return key == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(key).filter((block) -> block != Blocks.AIR);
            }
        }
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass, T fallback) {
        try {
            return (T)Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (Exception var4) {
            return fallback;
        }
    }

    private static int clampSpeed(int speed) {
        return Math.max(1, Math.min(100, speed));
    }

    private static int speedFromDelay(int delay) {
        double ratio = ((double)500.0F - (double)delay) / (double)495.0F;
        return clampSpeed((int)Math.round((double)1.0F + ratio * (double)99.0F));
    }

    private static int speedFromBatchSize(int batchSize) {
        double ratio = (double)(batchSize - 1) / (double)999.0F;
        return clampSpeed((int)Math.round((double)1.0F + ratio * (double)99.0F));
    }

    private static AutoBreakMode parseAutoBreakMode(String value) {
        if (value == null) {
            return AutoBreakMode.OFF;
        } else {
            AutoBreakMode var10000;
            switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "BEDROCK_ONLY":
                case "GENERIC_BLOCK":
                case "CLICK_WHITELIST":
                    var10000 = AutoBreakMode.CLICK_WHITELIST;
                    break;
                case "AREA_BEDROCK":
                case "AREA_WHITELIST":
                    var10000 = AutoBreakMode.AREA_WHITELIST;
                    break;
                default:
                    var10000 = AutoBreakMode.OFF;
            }

            return var10000;
        }
    }

    public boolean isAutoBreakBedrock() {
        return this.autoBreakMode != AutoBreakMode.OFF;
    }

    public void setAutoBreakBedrock(boolean enabled) {
        this.autoBreakMode = enabled ? AutoBreakMode.CLICK_WHITELIST : AutoBreakMode.OFF;
        this.saveIfAllowed();
    }


    public void setCustomBlockState(BlockState state) {
        if (state == null) {
            this.customStateBlockId = "";
            this.customStateProperties.clear();
        } else {
            this.customStateBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            this.customStateProperties.clear();
            for (var entry : state.getValues().entrySet()) {
                this.customStateProperties.put(entry.getKey().getName(), entry.getValue().toString());
            }
        }
        this.saveIfAllowed();
    }

    public BlockState getCustomBlockState() {
        if (this.customStateBlockId.isEmpty()) return null;
        ResourceLocation id = ResourceLocation.tryParse(this.customStateBlockId);
        if (id == null) return null;
        Optional<Block> optBlock = BuiltInRegistries.BLOCK.getOptional(id);
        if (optBlock.isEmpty()) return null;
        Block block = optBlock.get();
        BlockState state = block.defaultBlockState();
        for (var entry : this.customStateProperties.entrySet()) {
            Property<?> prop = block.getStateDefinition().getProperty(entry.getKey());
            if (prop != null) {
                Optional<?> val = prop.getValue(entry.getValue());
                if (val.isPresent()) {
                    state = state.setValue((Property) prop, (Comparable) val.get());
                }
            }
        }
        return state;
    }

    public boolean isUseCustomState() {
        return this.useCustomState;
    }

    public void setUseCustomState(boolean use) {
        this.useCustomState = use;
        this.saveIfAllowed();
    }

    static {
        CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("create_block_rotation_menu_wholesale-client.json");
        DEFAULT_WHITELIST_IDS = Arrays.asList("minecraft:bedrock", "minecraft:netherrack", "minecraft:deepslate");
        INSTANCE = new ModConfig();
    }

    public enum BatchMode {
        OFF,
        GENERAL,
        DRAIN_WATER,
        FILL,
        TEST;
    }

    public enum AutoBreakMode {
        OFF,
        CLICK_WHITELIST,
        AREA_WHITELIST;
    }

    private static class PersistedConfig {
        String batchMode;
        Integer processingSpeed;
        Integer packetSendDelay;
        Integer packetsPerBatch;
        String autoBreakMode;
        List<String> autoBreakWhitelist;
        String customStateBlockId;
        Map<String, String> customStateProperties;
        Boolean useCustomState;
    }
}