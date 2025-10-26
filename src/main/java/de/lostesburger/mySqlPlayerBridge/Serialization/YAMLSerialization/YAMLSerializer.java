package de.lostesburger.mySqlPlayerBridge.Serialization.YAMLSerialization;

import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class YAMLSerializer {

    /**
     * 使用YAML序列化ItemStack数组，并可选择使用GZIP压缩
     * @param items ItemStack数组
     * @return Base64编码的字符串
     * @throws IOException 当IO操作出错时抛出
     */
    public String serialize(ItemStack[] items) throws IOException {
        // 创建YamlConfiguration对象
        YamlConfiguration config = new YamlConfiguration();
        
        // 将物品数组存储到YAML配置中
        config.set("items", items);
        
        // 获取YAML字符串
        String yamlString = config.saveToString();
        
        // 检查是否启用gzip压缩
        boolean useGzip = Main.config.getBoolean("enable-gzip-compression", false);
        
        // 记录日志以便调试
        if (Main.DEBUG) {
            Main.getInstance().getLogger().info("序列化物品数组，大小: " + (items != null ? items.length : 0));
            Main.getInstance().getLogger().info("YAML内容: " + yamlString);
        }
        
        if (useGzip) {
            // 使用GZIP压缩
            ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedOutputStream)) {
                gzipOutputStream.write(yamlString.getBytes("UTF-8"));
                gzipOutputStream.finish();
            }
            
            // Base64编码
            return Base64.getEncoder().encodeToString(compressedOutputStream.toByteArray());
        } else {
            // 直接Base64编码
            return Base64.getEncoder().encodeToString(yamlString.getBytes("UTF-8"));
        }
    }

    /**
     * 反序列化Base64编码的字符串为ItemStack数组
     * @param base64 Base64编码的字符串
     * @return ItemStack数组
     * @throws IOException 当IO操作出错时抛出
     * @throws InvalidConfigurationException 当YAML配置无效时抛出
     */
    public ItemStack[] deserialize(String base64) throws IOException, InvalidConfigurationException {
        try {
            // Base64解码
            byte[] data = Base64.getDecoder().decode(base64);
            
            String yamlString;
            
            // 检查是否为GZIP压缩数据（通过前两个字节判断）
            if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
                // GZIP解压缩
                ByteArrayInputStream compressedInputStream = new ByteArrayInputStream(data);
                try (GZIPInputStream gzipInputStream = new GZIPInputStream(compressedInputStream)) {
                    ByteArrayOutputStream decompressedOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gzipInputStream.read(buffer)) > 0) {
                        decompressedOutputStream.write(buffer, 0, len);
                    }
                    yamlString = decompressedOutputStream.toString("UTF-8");
                }
            } else {
                // 直接转换为字符串
                yamlString = new String(data, "UTF-8");
            }
            
            // 检查YAML字符串是否为空或不包含items节点
            if (yamlString == null || yamlString.trim().isEmpty()) {
                Main.getInstance().getLogger().warning("YAML字符串为空");
                return new ItemStack[0];
            }
            
            // 从YAML字符串加载配置
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yamlString);
            
            // 检查是否存在items节点
            if (!config.contains("items")) {
                Main.getInstance().getLogger().warning("YAML配置中缺少'items'节点");
                return new ItemStack[0];
            }
            
            // 获取物品数组
            ItemStack[] items = config.getObject("items", ItemStack[].class, new ItemStack[0]);
            
            // 记录日志以便调试
            Main.getInstance().getLogger().info("反序列化物品数组，大小: " + (items != null ? items.length : 0));
            if (Main.DEBUG) {
                Main.getInstance().getLogger().info("YAML内容: " + yamlString);
            }
            
            // 确保返回的数组不为null
            return items != null ? items : new ItemStack[0];
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("YAML反序列化失败: " + e.getMessage());
            if (Main.DEBUG) {
                e.printStackTrace();
            }
            // 返回空的物品数组而不是抛出异常
            return new ItemStack[0];
        }
    }
}