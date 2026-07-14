package cn.theherta;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_STRUCTURE_SIZE = BUILDER
            .comment("物理化拆装最大数量")
            .defineInRange("maxStructureSize", 1000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

}