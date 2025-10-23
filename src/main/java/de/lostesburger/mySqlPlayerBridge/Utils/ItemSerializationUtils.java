package de.lostesburger.mySqlPlayerBridge.Utils;

import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Serialization.BukkitItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;
import java.util.logging.Level;

/**
 * 物品序列化工具类，用于检测和处理不同插件生成的物品数据格式
 */
public class ItemSerializationUtils {
    
    /**
     * 检测并反序列化Base64字符串为ItemStack数组
     * 自动识别数据格式并使用相应的反序列化方法
     * 
     * @param base64 Base64编码的物品数据
     * @return ItemStack数组
     * @throws Exception 如果反序列化失败
     */
    public static ItemStack[] detectAndDeserialize(String base64) throws Exception {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack[0];
        }
        
        try {
            // 首先尝试使用Bukkit标准方法反序列化
            if (Main.DEBUG) {
                Main.getInstance().getLogger().log(Level.INFO, "尝试使用Bukkit标准方法反序列化物品数据");
            }
            
            return BukkitItemSerializer.deserialize(base64);
        } catch (Exception e) {
            if (Main.DEBUG) {
                Main.getInstance().getLogger().log(Level.WARNING, "Bukkit标准方法反序列化失败: " + e.getMessage());
            }
            
            // 如果Bukkit方法失败，记录错误并重新抛出异常
            Main.getInstance().getLogger().log(Level.SEVERE, "无法识别物品数据格式，Base64长度: " + base64.length());
            throw new Exception("无法识别物品数据格式", e);
        }
    }
    
    /**
     * 检查Base64数据是否为有效的物品数据
     * 
     * @param base64 Base64编码的数据
     * @return 如果是有效的物品数据返回true，否则返回false
     */
    public static boolean isValidItemData(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return false;
        }
        
        try {
            // 尝试解码Base64
            byte[] data = Base64.getDecoder().decode(base64);
            
            // 检查数据长度
            if (data.length < 4) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}