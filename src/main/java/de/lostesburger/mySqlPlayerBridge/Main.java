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
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import de.lostesburger.mySqlPlayerBridge.Utils.Checks.DatabaseConfigCheck;
import de.lostesburger.mySqlPlayerBridge.Serialization.NBTSerialization.NBTSerializer;
import de.lostesburger.mySqlPlayerBridge.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    public static ArrayList<Runnable> schedulers = new ArrayList<Runnable>();
    public static FileConfiguration config;
    public static FileConfiguration mysqlConf;
    public static FileConfiguration messages;


    public static String serverType = "Unknown";
    private static Plugin instance;
    public static String version = "3.2";
    public static String pluginName = "MySqlPlayerBridge";
    public static String prefix;

    public static VaultManager vaultManager;
    public static ModulesManager modulesManager;
    public static ItemTranslationManager itemTranslationManager;
    public static PlayerManager playerManager;
    public static PlayerBridgeManager playerBridgeManager;
    public static CommandManager commandManager;
    public static MySqlConnectionHandler mySqlConnectionHandler;
    public static NBTSerializer nbtSerializer = null;
    public static AutoBackupManager autoBackupManager = null;

    public static String INVENTORY_TABLE_NAME = "mpdb_inventory";
    public static String ENDERCHEST_TABLE_NAME = "mpdb_enderchest";
    public static String ECONOMY_TABLE_NAME = "mpdb_economy";
    public static String EXPERIENCE_TABLE_NAME = "mpdb_experience";
    public static String HEALTH_FOOD_AIR_TABLE_NAME = "mpdb_health_food_air";
    public static String POTION_EFFECTS_TABLE_NAME = "mpdb_potionEffects";

    /**
     * 序列化类型：使用 Minecraft NBT Base64 编码
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
            // 确保所有自动备份子项都存在
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
        
        // 如果配置已更新，则保存到文件
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

        // 移除了对配置更改检查的依赖，因为 SimpleConfig 没有这个功能

        /**
         * Checks
         */
        this.getLogger().log(Level.INFO, "正在检查更新...");
        // 移除了 PluginSmithsUpdateCheck，因为这是 CoreLib 的一部分
        
        this.getLogger().log(Level.INFO, "正在检查数据库配置...");

        if(!new DatabaseConfigCheck(mysqlConf).isSetup()) {
            Bukkit.broadcastMessage(Chat.getMessage("no-database-config-error"));
            return;
        }

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
         * NBT-API
         */
        // 总是尝试初始化NBT序列化器，无论NBTAPI插件是否存在
        tryInitNBTSerializer();


        /**
         * VaultAPI
         */
        if(modulesManager.syncVaultEconomy){
            this.getLogger().log(Level.INFO, "正在加载 Vault API 模块...");
            vaultManager = new VaultManager();
            this.getLogger().log(Level.INFO, "Vault API 模块加载完成");
        }

        /**
         * Database
         */
        mySqlConnectionHandler = new MySqlConnectionHandler(
                mysqlConf.getString("host"),
                mysqlConf.getInt("port"),
                mysqlConf.getString("database"),
                mysqlConf.getString("user"),
                mysqlConf.getString("password")
        );

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
    }



    public static Plugin getInstance(){return instance;}

    @Override
    public void onDisable() {
        this.getLogger().log(Level.WARNING, "正在停止 MySqlPlayerBridge 插件 v"+version);
        if(mySqlConnectionHandler != null){
            mySqlConnectionHandler.getMySqlDataManager().saveAllOnlinePlayers();
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
        // 移除了对 schedulers.forEach(Scheduler.Task::cancel) 的依赖
    }

    private void tryInitNBTSerializer() {
        try {
            nbtSerializer = new NBTSerializer();
            getLogger().info("NBTSerializer 初始化成功 - 使用 Minecraft NBT Base64 编码格式");
            getLogger().info("背包、装备栏、末影箱数据将使用 NBT Base64 格式进行编码");
        } catch (Exception e) {
            getLogger().severe("NBTSerializer 无法初始化:");
            getLogger().severe("背包、装备栏、末影箱数据编码功能不可用");
            e.printStackTrace();
            // 即使NBT序列化器初始化失败，也不要禁用插件
            // getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    public AutoBackupManager getAutoBackupManager() {
        return autoBackupManager;
    }
}