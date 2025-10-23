package de.lostesburger.mySqlPlayerBridge.Managers;

import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 物品名称翻译管理器
 * 从配置文件中加载英文物品名称到中文的映射
 */
public class ItemTranslationManager {

    private final Main plugin;
    private File translationFile;
    private FileConfiguration translationConfig;
    private Map<String, String> translationMap;

    public ItemTranslationManager(Main plugin) {
        this.plugin = plugin;
        this.translationMap = new HashMap<>();
        loadTranslations();
    }

    /**
     * 加载物品翻译配置文件
     */
    private void loadTranslations() {
        // 创建配置文件
        translationFile = new File(plugin.getDataFolder(), "item_translations.yml");

        // 如果文件不存在，创建默认配置
        if (!translationFile.exists()) {
            try {
                // 尝试从资源中复制
                plugin.saveResource("item_translations.yml", false);
                plugin.getLogger().info("已从资源创建默认物品翻译配置文件: item_translations.yml");
            } catch (Exception e) {
                // 如果资源不存在，手动创建默认配置
                plugin.getLogger().info("资源文件不存在，正在创建默认物品翻译配置文件...");
                createDefaultTranslationFile();
            }
        }

        // 加载配置
        translationConfig = YamlConfiguration.loadConfiguration(translationFile);

        // 读取所有翻译映射
        loadTranslationMap();

        plugin.getLogger().info("已加载 " + translationMap.size() + " 个物品翻译");
    }

    /**
     * 手动创建默认翻译配置文件
     */
    private void createDefaultTranslationFile() {
        try {
            // 确保插件文件夹存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // 创建文件
            translationFile.createNewFile();

            // 创建配置对象
            YamlConfiguration config = new YamlConfiguration();

            // 添加注释和默认翻译
            config.set("translations.WHITE_BANNER", "白色旗帜");
            config.set("translations.GOLD_NUGGET", "金粒");
            config.set("translations.GOLD_INGOT", "金锭");
            config.set("translations.IRON_INGOT", "铁锭");
            config.set("translations.GOLDEN_HELMET", "金头盔");
            config.set("translations.GOLDEN_CHESTPLATE", "金胸甲");
            config.set("translations.GOLDEN_LEGGINGS", "金护腿");
            config.set("translations.GOLDEN_BOOTS", "金靴子");
            config.set("translations.IRON_HELMET", "铁头盔");
            config.set("translations.IRON_CHESTPLATE", "铁胸甲");
            config.set("translations.IRON_LEGGINGS", "铁护腿");
            config.set("translations.IRON_BOOTS", "铁靴子");
            config.set("translations.DIAMOND_HELMET", "钻石头盔");
            config.set("translations.DIAMOND_CHESTPLATE", "钻石胸甲");
            config.set("translations.DIAMOND_LEGGINGS", "钻石护腿");
            config.set("translations.DIAMOND_BOOTS", "钻石靴子");
            config.set("translations.WOODEN_SWORD", "木剑");
            config.set("translations.STONE_SWORD", "石剑");
            config.set("translations.IRON_SWORD", "铁剑");
            config.set("translations.GOLDEN_SWORD", "金剑");
            config.set("translations.DIAMOND_SWORD", "钻石剑");
            config.set("translations.NETHERITE_SWORD", "下界合金剑");
            config.set("translations.DARK_OAK_FENCE", "深色橡木栅栏");
            config.set("translations.EXPERIENCE_BOTTLE", "附魔之瓶");
            config.set("translations.NOTE_BLOCK", "音符盒");
            config.set("translations.ENCHANTING_TABLE", "附魔台");
            config.set("translations.DIAMOND", "钻石");
            config.set("translations.EMERALD", "绿宝石");
            config.set("translations.COAL", "煤炭");
            config.set("translations.STICK", "木棍");
            config.set("translations.STONE", "石头");
            config.set("translations.DIRT", "泥土");
            config.set("translations.GRASS_BLOCK", "草方块");
            config.set("translations.OAK_LOG", "橡木原木");
            config.set("translations.COBBLESTONE", "圆石");
            config.set("translations.SAND", "沙子");
            config.set("translations.GRAVEL", "沙砾");
            config.set("translations.GLASS", "玻璃");
            config.set("translations.BREAD", "面包");
            config.set("translations.APPLE", "苹果");
            config.set("translations.GOLDEN_APPLE", "金苹果");
            config.set("translations.ARROW", "箭");
            config.set("translations.BOW", "弓");
            config.set("translations.ENDER_PEARL", "末影珍珠");
            config.set("translations.ENDER_CHEST", "末影箱");

            // 保存配置
            config.save(translationFile);

            plugin.getLogger().info("已创建默认物品翻译配置文件，包含常用物品翻译");
            plugin.getLogger().info("您可以在 item_translations.yml 中添加更多自定义翻译");

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法创建默认物品翻译配置文件", e);
        }
    }

    /**
     * 从配置文件中读取翻译映射到内存
     */
    private void loadTranslationMap() {
        translationMap.clear();

        if (translationConfig.contains("translations")) {
            for (String key : translationConfig.getConfigurationSection("translations").getKeys(false)) {
                String chineseName = translationConfig.getString("translations." + key);
                if (chineseName != null && !chineseName.isEmpty()) {
                    translationMap.put(key.toUpperCase(), chineseName);
                }
            }
        }
    }

    /**
     * 获取物品的中文名称
     * @param materialName 物品的Material名称（英文）
     * @return 中文名称，如果没有找到映射则返回格式化后的英文名称
     */
    public String getChineseName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "未知物品";
        }

        // 转换为大写以进行匹配
        String upperName = materialName.toUpperCase();

        // 如果在映射中找到，返回中文名称
        if (translationMap.containsKey(upperName)) {
            return translationMap.get(upperName);
        }

        // 如果没有映射，返回格式化后的英文名称
        return formatMaterialName(materialName);
    }

    /**
     * 格式化物品名称
     * 将 DARK_OAK_FENCE 格式化为 Dark Oak Fence
     */
    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 0) {
                // 首字母大写
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }

                // 添加空格（除了最后一个单词）
                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }

    /**
     * 重新加载翻译配置
     */
    public void reload() {
        loadTranslations();
    }

    /**
     * 添加或更新翻译
     * @param materialName 物品Material名称
     * @param chineseName 中文名称
     */
    public void addTranslation(String materialName, String chineseName) {
        translationMap.put(materialName.toUpperCase(), chineseName);
        translationConfig.set("translations." + materialName.toUpperCase(), chineseName);
        saveConfig();
    }

    /**
     * 保存配置文件
     */
    private void saveConfig() {
        try {
            translationConfig.save(translationFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存物品翻译配置文件", e);
        }
    }

    /**
     * 获取翻译映射的数量
     */
    public int getTranslationCount() {
        return translationMap.size();
    }
}
