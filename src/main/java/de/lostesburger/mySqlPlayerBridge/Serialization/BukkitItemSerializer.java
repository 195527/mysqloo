package de.lostesburger.mySqlPlayerBridge.Serialization;

import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;

public class BukkitItemSerializer {
    
    /**
     * 将ItemStack数组序列化为Base64字符串
     * 使用Bukkit原生序列化方法确保与其他插件兼容
     * 
     * @param items ItemStack数组
     * @return Base64编码的字符串
     * @throws IOException 如果序列化过程中发生IO错误
     */
    public static String serialize(ItemStack[] items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream);
        
        try {
            // 写入数组长度
            bukkitOutputStream.writeInt(items.length);
            
            // 写入每个物品
            for (ItemStack item : items) {
                bukkitOutputStream.writeObject(item);
            }
            
            bukkitOutputStream.flush();
            byte[] data = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(data);
        } finally {
            bukkitOutputStream.close();
            outputStream.close();
        }
    }
    
    /**
     * 从Base64字符串反序列化为ItemStack数组
     * 使用Bukkit原生反序列化方法确保与其他插件兼容
     * 
     * @param base64 Base64编码的字符串
     * @return ItemStack数组
     * @throws IOException 如果反序列化过程中发生IO错误
     * @throws ClassNotFoundException 如果找不到相关类
     */
    public static ItemStack[] deserialize(String base64) throws IOException, ClassNotFoundException {
        // 添加调试日志
        if (Main.DEBUG) {
            Main.getInstance().getLogger().log(Level.INFO, "开始反序列化Base64数据，长度: " + base64.length());
        }
        
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream);
        
        try {
            // 读取数组长度
            int length = bukkitInputStream.readInt();
            
            if (Main.DEBUG) {
                Main.getInstance().getLogger().log(Level.INFO, "读取到数组长度: " + length);
            }
            
            ItemStack[] items = new ItemStack[length];
            
            // 读取每个物品
            for (int i = 0; i < length; i++) {
                try {
                    items[i] = (ItemStack) bukkitInputStream.readObject();
                    if (Main.DEBUG) {
                        Main.getInstance().getLogger().log(Level.INFO, "成功读取物品，索引: " + i + ", 物品: " + (items[i] != null ? items[i].getType() : "null"));
                    }
                } catch (Exception e) {
                    if (Main.DEBUG) {
                        Main.getInstance().getLogger().log(Level.WARNING, "读取物品时出错，索引: " + i + ", 错误: " + e.getMessage());
                    }
                    items[i] = null; // 出错时设置为null而不是中断整个过程
                }
            }
            
            return items;
        } finally {
            bukkitInputStream.close();
            inputStream.close();
        }
    }
}