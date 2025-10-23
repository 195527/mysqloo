package de.lostesburger.mySqlPlayerBridge.Serialization;

/**
 * 序列化类型枚举
 * NBT_BASE64: 使用 Minecraft 原生 NBT 格式 + Base64 编码（推荐）
 * BUKKIT: 使用 Bukkit 原生序列化（已弃用）
 */
public enum SerializationType {
    /**
     * 使用 Minecraft NBT (Named Binary Tag) 格式的 Base64 编码
     * 这是最兼容和可靠的序列化方式，适用于背包、装备栏、末影箱
     */
    NBT_BASE64,

    /**
     * 使用 Bukkit 原生序列化
     * @deprecated 不推荐使用，可能在跨版本时出现兼容性问题
     */
    @Deprecated
    BUKKIT,

    /**
     * NBT_API 别名，指向 NBT_BASE64
     * @deprecated 使用 NBT_BASE64 代替
     */
    @Deprecated
    NBT_API
}
