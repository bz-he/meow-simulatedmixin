package cn.theherta;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;

// 注意：去掉了 @Mod 注解和 implements IEventBus 等
public class Submitchange_batchpacket {
    // 这两个静态字段保持不变，供其他类引用
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.submitchange_batchpacket.config",
            Type.KEYSYM,
            298,
            "category.submitchange_batchpacket"
    );

    public static final KeyMapping RESEND_LAST_PACKET_KEY = new KeyMapping(
            "key.submitchange_batchpacket.resend",
            Type.KEYSYM,
            299,
            "category.submitchange_batchpacket"
    );

    // 如果有其他静态方法或字段也保留，但不要有 @Mod 注解
}