package cn.theherta;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.Locale;

/**
 * 方块状态翻译工具，统一管理属性名和属性值的显示文本。
 * 翻译键格式：
 *   属性名：block_state.<property_name>
 *   属性值：block_state.value.<property_name>.<value>
 */
public class BlockStateTranslations {

    public static String getPropertyName(Property<?> property) {
        String key = "block_state." + property.getName();
        return I18n.exists(key) ? I18n.get(key) : property.getName();
    }

    public static String getValueName(Property<?> property, Comparable<?> value) {
        String propKey = "block_state.value." + property.getName() + "." + value.toString().toLowerCase(Locale.ROOT);
        if (I18n.exists(propKey)) {
            return I18n.get(propKey);
        }
        // 通用布尔值简写
        if (value instanceof Boolean b) {
            return b ? "是" : "否";
        }
        return value.toString();
    }
}