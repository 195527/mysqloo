package de.lostesburger.mySqlPlayerBridge.Serialization.NBTSerialization;

import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.logging.Level;

public class NBTSerializer {

    // NBT API 相关字段
    private Method convertItemArrayToNBT;
    private Method convertNBTToItemArray;
    private Constructor<?> nbtContainerConstructor;
    private Method writeCompoundMethod;
    private Class<?> nbtContainerClass;
    private Class<?> nbtCompoundClass;

    public NBTSerializer() throws Exception {
        initNBTAPI();
        Main.getInstance().getLogger().info("NBTSerializer initialized with NBT-API (binary NBT format)");
    }

    private void initNBTAPI() throws Exception {
        Plugin nbtApiPlugin = Bukkit.getPluginManager().getPlugin("NBTAPI");
        if (nbtApiPlugin == null) {
            throw new IllegalStateException("NBTAPI Plugin is not loaded!");
        }

        ClassLoader nbtApiClassLoader = nbtApiPlugin.getClass().getClassLoader();

        try {
            // 尝试新版本的包名 (de.tr7zw.changeme.nbtapi)
            Class<?> nbtItemClass = Class.forName("de.tr7zw.changeme.nbtapi.NBTItem", true, nbtApiClassLoader);
            nbtCompoundClass = Class.forName("de.tr7zw.changeme.nbtapi.NBTCompound", true, nbtApiClassLoader);
            nbtContainerClass = Class.forName("de.tr7zw.changeme.nbtapi.NBTContainer", true, nbtApiClassLoader);

            Main.getInstance().getLogger().info("使用 NBT-API 新版本包名: de.tr7zw.changeme.nbtapi");

            // 获取方法
            convertItemArrayToNBT = nbtItemClass.getDeclaredMethod("convertItemArraytoNBT", ItemStack[].class);
            convertNBTToItemArray = nbtItemClass.getDeclaredMethod("convertNBTtoItemArray", nbtCompoundClass);

            // 获取写入二进制NBT的方法
            writeCompoundMethod = nbtContainerClass.getMethod("writeCompound", OutputStream.class);

            // 构造函数 - 从 InputStream 读取
            nbtContainerConstructor = nbtContainerClass.getConstructor(InputStream.class);

        } catch (ClassNotFoundException e) {
            // 回退到旧版本包名 (de.tr7zw.nbtapi)
            Main.getInstance().getLogger().info("尝试使用 NBT-API 旧版本包名: de.tr7zw.nbtapi");

            Class<?> nbtItemClass = Class.forName("de.tr7zw.nbtapi.NBTItem", true, nbtApiClassLoader);
            nbtCompoundClass = Class.forName("de.tr7zw.nbtapi.NBTCompound", true, nbtApiClassLoader);
            nbtContainerClass = Class.forName("de.tr7zw.nbtapi.NBTContainer", true, nbtApiClassLoader);

            convertItemArrayToNBT = nbtItemClass.getDeclaredMethod("convertItemArraytoNBT", ItemStack[].class);
            convertNBTToItemArray = nbtItemClass.getDeclaredMethod("convertNBTtoItemArray", nbtCompoundClass);
            writeCompoundMethod = nbtContainerClass.getMethod("writeCompound", OutputStream.class);
            nbtContainerConstructor = nbtContainerClass.getConstructor(InputStream.class);
        }

        Main.getInstance().getLogger().info("NBT-API methods loaded successfully");
    }

    /**
     * 序列化为标准 NBT 二进制格式（第一种格式）
     */
    public String serialize(ItemStack[] items) throws Exception {
        try {
            // 日志：序列化开始
            Main.getInstance().getLogger().fine("开始序列化 " + items.length + " 个物品槽位");

            // 1. 将 ItemStack 数组转换为 NBT 对象
            Object nbtCompound = convertItemArrayToNBT.invoke(null, (Object) items);

            if (nbtCompound == null) {
                Main.getInstance().getLogger().warning("convertItemArrayToNBT 返回了 null");
                throw new IllegalStateException("NBT Compound is null");
            }

            // 2. 将 NBT 对象写入字节流（二进制格式）
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeCompoundMethod.invoke(nbtCompound, outputStream);

            // 3. Base64 编码
            byte[] nbtBytes = outputStream.toByteArray();

            // 日志：输出数据大小
            Main.getInstance().getLogger().fine("NBT 二进制数据大小: " + nbtBytes.length + " 字节");

            if (nbtBytes.length == 0) {
                Main.getInstance().getLogger().warning("NBT 二进制数据为空！");
            }

            String base64 = Base64.getEncoder().encodeToString(nbtBytes);

            // 日志：Base64 前缀（用于验证格式）
            String prefix = base64.length() > 20 ? base64.substring(0, 20) : base64;
            Main.getInstance().getLogger().fine("Base64 编码完成，前缀: " + prefix + "...");

            return base64;

        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "序列化失败", e);
            throw e;
        }
    }

    /**
     * 从标准 NBT 二进制格式反序列化（第一种格式）
     */
    public ItemStack[] deserialize(String base64) throws Exception {
        try {
            // 验证输入
            if (base64 == null || base64.isEmpty()) {
                Main.getInstance().getLogger().warning("尝试反序列化空字符串");
                throw new IllegalArgumentException("Base64 string is null or empty");
            }

            // 日志：Base64 前缀
            String prefix = base64.length() > 20 ? base64.substring(0, 20) : base64;
            Main.getInstance().getLogger().fine("开始反序列化，Base64 前缀: " + prefix + "...");
            Main.getInstance().getLogger().fine("Base64 总长度: " + base64.length());

            // 1. Base64 解码
            byte[] nbtBytes = Base64.getDecoder().decode(base64);
            Main.getInstance().getLogger().fine("Base64 解码成功，字节数: " + nbtBytes.length);

            // 检查字节数组前几个字节（NBT 标签）
            if (nbtBytes.length > 0) {
                StringBuilder hex = new StringBuilder("前10字节 (hex): ");
                for (int i = 0; i < Math.min(10, nbtBytes.length); i++) {
                    hex.append(String.format("%02X ", nbtBytes[i]));
                }
                Main.getInstance().getLogger().fine(hex.toString());
            }

            // 2. 从字节流读取 NBT 对象
            ByteArrayInputStream inputStream = new ByteArrayInputStream(nbtBytes);
            Object nbtContainer = nbtContainerConstructor.newInstance(inputStream);
            Main.getInstance().getLogger().fine("NBT Container 创建成功");

            if (nbtContainer == null) {
                Main.getInstance().getLogger().severe("NBT Container 为 null");
                throw new IllegalStateException("NBT Container is null");
            }

            // 3. 将 NBT 对象转换为 ItemStack 数组
            Object items = convertNBTToItemArray.invoke(null, nbtContainer);
            Main.getInstance().getLogger().fine("NBT 转换为 ItemStack 数组成功");

            if (items == null) {
                Main.getInstance().getLogger().warning("convertNBTToItemArray 返回了 null，使用空数组");
                return new ItemStack[0];
            }

            ItemStack[] result = (ItemStack[]) items;
            Main.getInstance().getLogger().fine("反序列化完成，共 " + result.length + " 个物品槽位");

            return result;

        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Base64 解码失败 - 数据可能损坏", e);
            throw new IllegalArgumentException("Invalid Base64 string", e);

        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            Main.getInstance().getLogger().log(Level.SEVERE, "NBT-API 方法调用失败: " + cause.getMessage(), cause);
            throw new Exception("Failed to invoke NBT-API method", cause);

        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "反序列化失败", e);
            throw e;
        }
    }

    /**
     * 序列化单个物品
     */
    public String serializeSingleItem(ItemStack item) throws Exception {
        if (item == null) {
            return null;
        }

        try {
            ClassLoader classLoader = Bukkit.getPluginManager().getPlugin("NBTAPI").getClass().getClassLoader();
            Class<?> nbtItemClass;

            try {
                nbtItemClass = Class.forName("de.tr7zw.changeme.nbtapi.NBTItem", true, classLoader);
            } catch (ClassNotFoundException e) {
                nbtItemClass = Class.forName("de.tr7zw.nbtapi.NBTItem", true, classLoader);
            }

            // 创建 NBTItem
            Constructor<?> nbtItemConstructor = nbtItemClass.getConstructor(ItemStack.class);
            Object nbtItem = nbtItemConstructor.newInstance(item);

            // 获取 compound
            Method getCompoundMethod = nbtItemClass.getMethod("getCompound");
            Object compound = getCompoundMethod.invoke(nbtItem);

            // 写入二进制
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeCompoundMethod.invoke(compound, outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "序列化单个物品失败", e);
            throw e;
        }
    }

    /**
     * 反序列化单个物品
     */
    public ItemStack deserializeSingleItem(String base64) throws Exception {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }

        try {
            ClassLoader classLoader = Bukkit.getPluginManager().getPlugin("NBTAPI").getClass().getClassLoader();
            Class<?> nbtItemClass;

            try {
                nbtItemClass = Class.forName("de.tr7zw.changeme.nbtapi.NBTItem", true, classLoader);
            } catch (ClassNotFoundException e) {
                nbtItemClass = Class.forName("de.tr7zw.nbtapi.NBTItem", true, classLoader);
            }

            // 从二进制读取
            byte[] nbtBytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(nbtBytes);
            Object nbtContainer = nbtContainerConstructor.newInstance(inputStream);

            // 转换为 ItemStack
            Method convertMethod = nbtItemClass.getDeclaredMethod("convertNBTtoItem", nbtContainerClass);
            return (ItemStack) convertMethod.invoke(null, nbtContainer);

        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "反序列化单个物品失败", e);
            throw e;
        }
    }

    /**
     * 转换：从第二种格式（Bukkit序列化）转换为第一种格式（NBT二进制）
     */
    public String convertBukkitToNBT(String bukkitBase64) throws Exception {
        try {
            Main.getInstance().getLogger().info("尝试从 Bukkit 格式转换为 NBT 格式");

            // 1. 使用 Bukkit 原生方式反序列化
            byte[] data = Base64.getDecoder().decode(bukkitBase64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

            org.bukkit.util.io.BukkitObjectInputStream dataInput =
                    new org.bukkit.util.io.BukkitObjectInputStream(inputStream);

            Object obj = dataInput.readObject();
            dataInput.close();

            if (!(obj instanceof ItemStack[])) {
                throw new IllegalArgumentException("反序列化的对象不是 ItemStack 数组");
            }

            ItemStack[] items = (ItemStack[]) obj;
            Main.getInstance().getLogger().info("成功从 Bukkit 格式读取 " + items.length + " 个物品槽位");

            // 2. 使用 NBT 方式重新序列化
            String result = serialize(items);
            Main.getInstance().getLogger().info("成功转换为 NBT 格式");

            return result;

        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "从 Bukkit 格式转换为 NBT 格式失败", e);
            throw e;
        }
    }

    /**
     * 验证是否为 NBT 格式（第一种格式）
     */
    public boolean isNBTFormat(String base64) {
        try {
            if (base64 == null || base64.isEmpty()) {
                return false;
            }

            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            nbtContainerConstructor.newInstance(inputStream);
            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().fine("不是有效的 NBT 格式: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证是否为 Bukkit 格式（第二种格式）
     */
    public boolean isBukkitFormat(String base64) {
        try {
            if (base64 == null || base64.isEmpty()) {
                return false;
            }

            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            org.bukkit.util.io.BukkitObjectInputStream dataInput =
                    new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
            Object obj = dataInput.readObject();
            dataInput.close();

            return obj instanceof ItemStack[];

        } catch (Exception e) {
            Main.getInstance().getLogger().fine("不是有效的 Bukkit 格式: " + e.getMessage());
            return false;
        }
    }

    /**
     * 智能反序列化 - 自动检测格式并反序列化
     */
    public ItemStack[] smartDeserialize(String base64) throws Exception {
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalArgumentException("Base64 string is null or empty");
        }

        // 首先尝试 NBT 格式
        if (isNBTFormat(base64)) {
            Main.getInstance().getLogger().fine("检测到 NBT 格式，使用 NBT 反序列化");
            return deserialize(base64);
        }

        // 然后尝试 Bukkit 格式
        if (isBukkitFormat(base64)) {
            Main.getInstance().getLogger().info("检测到 Bukkit 格式，将自动转换为 NBT 格式");
            String nbtFormat = convertBukkitToNBT(base64);
            return deserialize(nbtFormat);
        }

        // 都不是，抛出异常
        throw new IllegalArgumentException("无法识别的数据格式（既不是 NBT 也不是 Bukkit 格式）");
    }
}