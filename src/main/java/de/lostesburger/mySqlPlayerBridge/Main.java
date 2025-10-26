package de.lostesburger.mySqlPlayerBridge;

import de.lostesburger.mySqlPlayerBridge.Config.SimpleConfig;
import de.lostesburger.mySqlPlayerBridge.Handlers.MySqlConnection.MySqlConnectionHandler;
import de.lostesburger.mySqlPlayerBridge.Managers.AutoBackup.AutoBackupManager;
import de.lostesburger.mySqlPlayerBridge.Managers.Command.CommandManager;
import de.lostesburger.mySqlPlayerBridge.Managers.ItemTranslationManager;
import de.lostesburger.mySqlPlayerBridge.Managers.Modules.ModulesManager;
import de.lostesburger.mySqlPlayerBridge.Managers.NbtAPI.NBTAPIManager;
import de.lostesburger.mySqlPlayerBridge.Managers.Player.PlayerManager;
import de.lostesburger.mySqlPlayerBridge.Managers.PlayerBridge.PlayerBridgeManager;
import de.lostesburger.mySqlPlayerBridge.Managers.Vault.VaultManager;
import de.lostesburger.mySqlPlayerBridge.Serialization.SerializationType;
import de.lostesburger.mySqlPlayerBridge.Storage.MySQLStorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.SQLiteStorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.StorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.YAMLStorageManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import de.lostesburger.mySqlPlayerBridge.Utils.Checks.DatabaseConfigCheck;
import de.lostesburger.mySqlPlayerBridge.Serialization.NBTSerialization.NBTSerializer;
import de.lostesburger.mySqlPlayerBridge.Serialization.YAMLSerialization.YAMLSerializer; // 添加YAML序列化器导入
import de.lostesburger.mySqlPlayerBridge.Utils.Utils;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager; // 添加MySqlDataManager导入
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    public static ArrayList<Runnable> schedulers = new ArrayList<Runnable>();
    public static FileConfiguration config;
    public static FileConfiguration mysqlConf;
    public static FileConfiguration messages;


    public static String serverType = "Unknown";
    private static Plugin instance;
    public static String version = "3.2.1";
    public static String pluginName = "MySqlPlayerBridge";
    public static String prefix;

    public static VaultManager vaultManager;
    public static ModulesManager modulesManager;
    public static ItemTranslationManager itemTranslationManager;
    public static PlayerManager playerManager;
    public static PlayerBridgeManager playerBridgeManager;
    public static CommandManager commandManager;
    public static MySqlConnectionHandler mySqlConnectionHandler;
    public static StorageManager storageManager; // 使用抽象存储管理器
    public static NBTSerializer nbtSerializer = null;
    public static YAMLSerializer yamlSerializer = null; // 添加YAML序列化器实例
    public static AutoBackupManager autoBackupManager = null;

    public static String INVENTORY_TABLE_NAME = "mpdb_inventory";
    public static String ENDERCHEST_TABLE_NAME = "mpdb_enderchest";
    public static String ECONOMY_TABLE_NAME = "mpdb_economy";
    public static String EXPERIENCE_TABLE_NAME = "mpdb_experience";
    public static String HEALTH_FOOD_AIR_TABLE_NAME = "mpdb_health_food_air";
    public static String POTION_EFFECTS_TABLE_NAME = "mpdb_potionEffects";

    /**
     * 序列化类型：仅使用 Minecraft NBT Base64 编码
     * 用于背包、装备栏、末影箱的数据编码
     */
    public static SerializationType serializationType = SerializationType.NBT_BASE64;

    public static boolean DEBUG = false;

    @Override
    public void onEnable() {
        instance = this;

        this.getLogger().log(Level.WARNING, "正在启动 MySqlPlayerBridge 插件 v"+version);
        serverType = Bukkit.getServer().getVersion();
        this.getLogger().log(Level.INFO, "检测到服务器类型: "+serverType);

        // 检测 Minecraft 版本
        String mcVersion = de.lostesburger.mySqlPlayerBridge.Utils.VersionUtils.getVersionString();
        this.getLogger().log(Level.INFO, "检测到 Minecraft 版本: " + mcVersion);

        // 检查版本支持范围 (1.8 - 1.21.10)
        if (!de.lostesburger.mySqlPlayerBridge.Utils.VersionUtils.isVersionOrHigher(1, 8)) {
            this.getLogger().severe("不支持的 Minecraft 版本: " + mcVersion);
            this.getLogger().severe("该插件支持 Minecraft 1.8.8 到 1.21.10");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getLogger().info("版本兼容性检查通过 ✓");

        // 检查是否是 Paper 服务器
        if (de.lostesburger.mySqlPlayerBridge.Utils.VersionUtils.isPaper()) {
            this.getLogger().info("检测到 Paper 服务器，性能优化已启用 ✓");
        }

        // 检查是否是 Folia 服务器
        if(de.lostesburger.mySqlPlayerBridge.Utils.VersionUtils.isFolia()){
            this.getLogger().warning("服务器正在运行 Folia，这是该插件支持的软件");
            this.getLogger().warning("Folia 本身可能会出现未知错误（包括重大安全漏洞）");
        }

        // 检查副手支持
        if (de.lostesburger.mySqlPlayerBridge.Utils.VersionUtils.supportsOffhand()) {
            this.getLogger().info("副手物品同步功能已启用 ✓");
        } else {
            this.getLogger().info("当前版本不支持副手物品（需要 1.9+）");
        }

        /**
         * Config file(s)
         */
        this.getLogger().log(Level.INFO, "正在加载/创建配置文件...");
        SimpleConfig ymlConfig = new SimpleConfig(this, "config.yml");
        config = ymlConfig.getConfig();
        prefix = config.getString("prefix");
        
        // 检查配置文件版本并更新注释
        checkAndUpdateConfigVersion(config, ymlConfig);
        
        config.set("version", version);

        // 确保自动备份配置项存在
        boolean configUpdated = false;
        if (!config.contains("auto-backup")) {
            config.set("auto-backup.enabled", false);
            config.set("auto-backup.interval", 60);
            config.set("auto-backup.keep-count", 10);
            this.getLogger().info("已添加自动备份配置项到config.yml");
            configUpdated = true;
        } else {
            if (!config.contains("auto-backup.enabled")) {
                config.set("auto-backup.enabled", false);
                configUpdated = true;
            }
            if (!config.contains("auto-backup.interval")) {
                config.set("auto-backup.interval", 60);
                configUpdated = true;
            }
            if (!config.contains("auto-backup.keep-count")) {
                config.set("auto-backup.keep-count", 10);
                configUpdated = true;
            }
        }

        // 确保gzip压缩配置项存在
        if (!config.contains("enable-gzip-compression")) {
            config.set("enable-gzip-compression", false);
            this.getLogger().info("已添加gzip压缩配置项到config.yml");
            configUpdated = true;
        }

        // 确保存储方式配置项存在
        if (!config.contains("storage-type")) {
            config.set("storage-type", "mysql");
            this.getLogger().info("已添加存储方式配置项到config.yml");
            configUpdated = true;
        }

        // 确保save-player-data-on-shutdown配置项存在
        if (!config.contains("settings.save-player-data-on-shutdown")) {
            config.set("settings.save-player-data-on-shutdown", true);
            this.getLogger().info("已添加save-player-data-on-shutdown配置项到config.yml");
            configUpdated = true;
        }

        if (configUpdated) {
            ymlConfig.save();
        }

        try {
            config.save(new File(getInstance().getDataFolder(), "config.yml"));
        } catch (IOException ignored) {}

        SimpleConfig ymlConfigMySQL = new SimpleConfig(this, "mysql.yml");
        mysqlConf = ymlConfigMySQL.getConfig();
        INVENTORY_TABLE_NAME = mysqlConf.getString("inventory-table-name");
        ENDERCHEST_TABLE_NAME = mysqlConf.getString("enderchest-table-name");
        ECONOMY_TABLE_NAME = mysqlConf.getString("economy-table-name");
        EXPERIENCE_TABLE_NAME = mysqlConf.getString("experience-table-name");
        HEALTH_FOOD_AIR_TABLE_NAME = mysqlConf.getString("health-food-air-table-name");
        POTION_EFFECTS_TABLE_NAME = mysqlConf.getString("potion-effects-table-name");

        SimpleConfig ymlConfigMessages = new SimpleConfig(this, "messages.yml");
        messages = ymlConfigMessages.getConfig();

        this.getLogger().log(Level.INFO, "正在检查配置更改...");

        /**
         * Checks
         */
        this.getLogger().log(Level.INFO, "正在检查更新...");

        this.getLogger().log(Level.INFO, "正在检查数据库配置...");

        // 初始化存储管理器
        initializeStorageManager();

        /**
         * Modules
         */
        modulesManager= new ModulesManager();

        /**
         * Item Translation Manager
         */
        this.getLogger().log(Level.INFO, "正在加载物品翻译管理器...");
        itemTranslationManager = new ItemTranslationManager(this);
        this.getLogger().log(Level.INFO, "物品翻译管理器加载完成");

        /**
         * NBT-API - 必须初始化
         */
        this.getLogger().log(Level.WARNING, "正在初始化NBT序列化器 (必需组件)...");
        if (!tryInitNBTSerializer()) {
            this.getLogger().severe("╔════════════════════════════════════════════════════════╗");
            this.getLogger().severe("║  NBT序列化器初始化失败 - 插件无法正常工作              ║");
            this.getLogger().severe("║                                                        ║");
            this.getLogger().severe("║  请确保已安装 NBTAPI 插件:                             ║");
            this.getLogger().severe("║  https://www.spigotmc.org/resources/nbt-api.7939/     ║");
            this.getLogger().severe("║                                                        ║");
            this.getLogger().severe("║  插件将被禁用                                          ║");
            this.getLogger().severe("╚════════════════════════════════════════════════════════╝");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getLogger().info("NBT序列化器初始化成功 ✓");
        this.getLogger().info("物品数据将使用 NBT Base64 格式进行编码");

        // 初始化YAML序列化器
        try {
            yamlSerializer = new YAMLSerializer();
            this.getLogger().info("YAML序列化器初始化成功 ✓");
        } catch (Exception e) {
            this.getLogger().severe("YAML序列化器初始化失败: " + e.getMessage());
            e.printStackTrace();
        }

        /**
         * VaultAPI
         */
        if(modulesManager.syncVaultEconomy){
            this.getLogger().log(Level.INFO, "正在加载 Vault API 模块...");
            vaultManager = new VaultManager();
            this.getLogger().log(Level.INFO, "Vault API 模块加载完成");
        }

        /**
         * Other Managers
         */
        playerManager = new PlayerManager();
        playerBridgeManager = new PlayerBridgeManager();
        commandManager = new CommandManager();

        /**
         * Auto Backup Manager
         */
        autoBackupManager = new AutoBackupManager(this);
        autoBackupManager.startAutoBackup();

        // 最终启动信息
        this.getLogger().info("╔════════════════════════════════════════════════════════╗");
        this.getLogger().info("║  MySqlPlayerBridge v" + version + " 启动完成               ║");
        this.getLogger().info("║  存储方式: " + config.getString("storage-type") + "                                 ║");
        this.getLogger().info("║  数据库已连接                                           ║");
        this.getLogger().info("╚════════════════════════════════════════════════════════╝");
    }

    /**
     * 初始化存储管理器
     */
    public void initializeStorageManager() {
        String storageType = config.getString("storage-type", "mysql").toLowerCase();
        
        switch (storageType) {
            case "mysql":
                this.getLogger().info("正在初始化MySQL存储管理器...");
                mySqlConnectionHandler = new MySqlConnectionHandler(
                        mysqlConf.getString("host"),
                        mysqlConf.getInt("port"),
                        mysqlConf.getString("database"),
                        mysqlConf.getString("user"),
                        mysqlConf.getString("password")
                );
                storageManager = new MySQLStorageManager(mySqlConnectionHandler.getManager());
                break;
                
            case "sqlite":
                this.getLogger().info("正在初始化SQLite存储管理器...");
                String dbPath = getDataFolder().getAbsolutePath() + File.separator + "database.db";
                storageManager = new SQLiteStorageManager(dbPath);
                break;
                
            case "yaml":
                this.getLogger().info("正在初始化YAML存储管理器...");
                storageManager = new YAMLStorageManager(getDataFolder());
                break;
                
            default:
                this.getLogger().severe("不支持的存储类型: " + storageType + "，使用默认的MySQL存储");
                mySqlConnectionHandler = new MySqlConnectionHandler(
                        mysqlConf.getString("host"),
                        mysqlConf.getInt("port"),
                        mysqlConf.getString("database"),
                        mysqlConf.getString("user"),
                        mysqlConf.getString("password")
                );
                storageManager = new MySQLStorageManager(mySqlConnectionHandler.getManager());
                break;
        }
        
        this.getLogger().info("存储管理器初始化完成: " + storageType);
    }

    public static Plugin getInstance(){return instance;}

    @Override
    public void onDisable() {
        this.getLogger().log(Level.WARNING, "正在停止 MySqlPlayerBridge 插件 v"+version);
        // 修复：检查mySqlConnectionHandler是否为null
        if(mySqlConnectionHandler != null){
            mySqlConnectionHandler.getMySqlDataManager().saveAllOnlinePlayers();
        } else if (storageManager != null) {
            // 对于非MySQL存储，创建临时DataManager来保存数据
            MySqlDataManager dataManager = new MySqlDataManager(storageManager);
            dataManager.saveAllOnlinePlayers();
        }

        // 关闭存储管理器连接
        if (storageManager != null) {
            storageManager.closeConnection();
        }

        // 停止自动备份任务
        if (autoBackupManager != null) {
            autoBackupManager.stopAutoBackup();
        }

        this.getLogger().log(Level.INFO, "正在关闭 MySql 连接...");
        if(mySqlConnectionHandler != null) {
            // mySqlConnectionHandler.getMySQL().closeConnection();
        }

        this.getLogger().log(Level.INFO, "正在停止运行中的计划任务...");
    }

    /**
     * 初始化NBT序列化器 (必需)
     * @return 是否成功初始化
     */
    private boolean tryInitNBTSerializer() {
        try {
            nbtSerializer = new NBTSerializer();
            getLogger().info("NBTSerializer 初始化成功");
            return true;
        } catch (Exception e) {
            getLogger().severe("NBTSerializer 初始化失败:");
            getLogger().severe(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public AutoBackupManager getAutoBackupManager() {
        return autoBackupManager;
    }
    
    /**
     * 检查并更新配置文件版本
     * @param config 当前配置
     * @param ymlConfig 配置管理器
     */
    private void checkAndUpdateConfigVersion(FileConfiguration config, SimpleConfig ymlConfig) {
        // 获取当前配置版本
        String currentVersion = config.getString("version", "0.0");
        
        // 如果是旧版本配置，更新注释和默认值
        // 检查版本是否低于3.2.1
        if (isOlderVersion(currentVersion, "3.2.1")) {
            this.getLogger().info("检测到旧版本配置文件 v" + currentVersion + "，正在更新配置注释至 v3.2.1...");
            
            // 备份旧配置
            File backupFile = new File(getDataFolder(), "config.yml.bak");
            try {
                Files.copy(new File(getDataFolder(), "config.yml").toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                this.getLogger().info("旧配置文件已备份为: " + backupFile.getName());
            } catch (IOException e) {
                this.getLogger().warning("无法创建配置文件备份: " + e.getMessage());
            }
            
            // 重新从资源文件复制默认配置（包含新注释）
            try {
                // 获取新的默认配置
                InputStream defaultConfigStream = getResource("config.yml");
                if (defaultConfigStream != null) {
                    // 读取默认配置
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
                    
                    // 保留用户自定义的值
                    preserveUserConfigValues(config, defaultConfig);
                    
                    // 保存更新后的配置
                    defaultConfig.save(new File(getDataFolder(), "config.yml"));
                    ymlConfig.reload();
                    
                    this.getLogger().info("配置文件已更新到最新版本 v3.2.1");
                }
            } catch (Exception e) {
                this.getLogger().warning("更新配置文件时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 比较两个版本号
     * @param currentVersion 当前版本
     * @param targetVersion 目标版本
     * @return 如果当前版本低于目标版本则返回true
     */
    private boolean isOlderVersion(String currentVersion, String targetVersion) {
        // 如果版本相同，则不是旧版本
        if (currentVersion.equals(targetVersion)) {
            return false;
        }
        
        // 如果当前版本是默认值(0.0)，则认为是旧版本
        if ("0.0".equals(currentVersion)) {
            return true;
        }
        
        // 简单的版本比较（假设版本格式为 X.Y.Z）
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] targetParts = targetVersion.split("\\.");
            
            for (int i = 0; i < Math.min(currentParts.length, targetParts.length); i++) {
                int currentNum = Integer.parseInt(currentParts[i]);
                int targetNum = Integer.parseInt(targetParts[i]);
                
                if (currentNum < targetNum) {
                    return true;
                } else if (currentNum > targetNum) {
                    return false;
                }
            }
            
            // 如果前面的部分都相等，但长度不同
            return currentParts.length < targetParts.length;
        } catch (NumberFormatException e) {
            // 如果解析失败，假设是旧版本
            return true;
        }
    }
    
    /**
     * 保留用户自定义的配置值
     * @param userConfig 用户配置
     * @param defaultConfig 默认配置
     */
    private void preserveUserConfigValues(FileConfiguration userConfig, YamlConfiguration defaultConfig) {
        // 复制用户自定义的值到新配置中
        for (String key : userConfig.getKeys(true)) {
            if (!key.equals("version")) {  // 版本号除外
                defaultConfig.set(key, userConfig.get(key));
            }
        }
    }
}